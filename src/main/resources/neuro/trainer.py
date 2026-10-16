"""Local-only online imitation trainer. Protocol: bounded NDJSON stdin/stdout; no network."""
import argparse, collections, copy, hashlib, json, math, os, queue, random, sys, threading, time, uuid
from pathlib import Path

def startup(stage):
    if __name__=='__main__':
        print(json.dumps(dict(type='starting',stage=stage)),flush=True)
        print(time.strftime('%Y-%m-%d %H:%M:%S')+' '+stage,file=sys.stderr,flush=True)

if __name__=='__main__':
    sys.stdout.reconfigure(encoding='utf-8',line_buffering=True)
    sys.stderr.reconfigure(encoding='utf-8',line_buffering=True)
    import faulthandler
    faulthandler.enable()
    faulthandler.dump_traceback_later(60,repeat=True)
startup('Загрузка NumPy')
import numpy as np
startup('Загрузка PyTorch')
import torch
from torch import nn
from torch.nn import functional as F
startup('PyTorch загружен: '+torch.__version__)
import importlib.util
_movement_spec=importlib.util.spec_from_file_location('neuro_movement',Path(__file__).with_name('movement.py'))
_movement=importlib.util.module_from_spec(_movement_spec);_movement_spec.loader.exec_module(_movement)

SCHEMA = [
    ('dt_ms',50),('rel_forward',12),('rel_right',12),('rel_up',6),('distance',12),
    ('target_width',2),('target_height',3),('error_yaw',180),('error_pitch',90),
    ('player_forward_bps',10),('player_right_bps',10),('player_up_bps',10),
    ('target_forward_bps',10),('target_right_bps',10),('target_up_bps',10),
    ('radial_bps',15),('relative_strafe_bps',15),('yaw_sin',1),('yaw_cos',1),('pitch',90),
    ('last_dyaw',30),('last_dpitch',30),('prev_dyaw',30),('prev_dpitch',30),
    ('visible',1),('blocked',1),('attack_cooldown',1),('since_attack_s',5),
    ('previous_attack',1),('previous_local_hit',1),('previous_miss',1),('target_changed',1),
    ('ray_u',1),('ray_v',1),('ray_depth',1),('on_ground',1),('sprint',1)]
FORMAT=1; SEQUENCE=16; CHANNELS=24; DILATIONS=[1,2,4]; OUTPUT_SCALE=[30.,30.]

def emit(kind, **data):
    print(json.dumps(dict(type=kind, **data), allow_nan=False, separators=(',',':')), flush=True)

def atomic_json(path, data):
    tmp=path.with_suffix(path.suffix+'.tmp')
    with tmp.open('w',encoding='utf-8') as f:
        json.dump(data,f,allow_nan=False,separators=(',',':')); f.flush(); os.fsync(f.fileno())
    os.replace(tmp,path)

def digest(path):
    hasher=hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda:f.read(65536),b''): hasher.update(chunk)
    return hasher.hexdigest()

class TCN(nn.Module):
    def __init__(self):
        super().__init__()
        self.layers=nn.ModuleList([nn.Conv1d(len(SCHEMA) if i==0 else CHANNELS, CHANNELS,3,dilation=d) for i,d in enumerate(DILATIONS)])
        self.head=nn.Linear(CHANNELS,2)
        nn.init.zeros_(self.head.weight); nn.init.zeros_(self.head.bias)  # neutral untrained output; no procedural aim
    def forward(self,x):
        x=x.transpose(1,2)
        for layer,dilation in zip(self.layers,DILATIONS): x=F.relu(layer(F.pad(x,(2*dilation,0))))
        return self.head(x[:,:,-1])

def select_device(mode):
    reason=''
    if mode!='cpu':
        try:
            startup('Проверка CUDA и обратного распространения')
            if not torch.cuda.is_available():
                raise RuntimeError('PyTorch built without CUDA' if torch.version.cuda is None else 'CUDA device unavailable')
            probe=torch.randn(64,64,device='cuda',requires_grad=True)
            probe.square().mean().backward(); torch.cuda.synchronize()
            if probe.grad is None or probe.grad.device.type!='cuda' or not torch.isfinite(probe.grad).all(): raise RuntimeError('CUDA backward probe failed')
            return torch.device('cuda:0'), torch.cuda.get_device_name(0), ''
        except Exception as e:
            reason=str(e)
            if mode=='gpu': raise RuntimeError('GPU requested but unavailable: '+reason+'; select CPU in NeuroTrainer settings')
    return torch.device('cpu'), 'CPU', reason or 'CPU explicitly selected'

class Trainer:
    def __init__(self,root,name,config,mode='auto'):
        if not name or len(name)>32 or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-' for c in name): raise ValueError('Invalid model name')
        self.folder=Path(root)/'models'/name; self.folder.mkdir(parents=True,exist_ok=True)
        self.name=name; self.config=config; self.device,self.device_name,self.fallback=select_device(mode)
        startup('Создание модели на '+str(self.device))
        torch.set_num_threads(1); torch.manual_seed(731); random.seed(731)
        self.model=TCN().to(self.device)
        startup('Создание оптимизатора AdamW')
        self.optimizer=torch.optim.AdamW(self.model.parameters(),lr=float(config.get('lr',.001)),weight_decay=1e-5)
        self.capacity=max(256,min(20000,int(config.get('capacity',4096))))
        self.movement=_movement.MovementTrainer(self.device,self.capacity,config)
        if int(config.get('minimum',256))>self.capacity: raise ValueError('Minimum examples exceeds replay capacity')
        self.train=collections.deque(maxlen=self.capacity); self.valid=collections.deque(maxlen=max(128,self.capacity//4))
        self.history=collections.deque(maxlen=SEQUENCE); self.samples=0; self.steps=0; self.loss=None; self.metrics={}
        self.last_save=0; self.last_emit=0; self.last_update_samples=0; self.weight_delta=0.; self.gradient=0.; self.last_group=None
        self.session=uuid.uuid4().hex; self.local_row=0; self.last_good=None; self.group_rows=0
        self.log=None; self.log_folder=Path(root)/'demonstrations'
        if (self.folder/'current.json').exists():
            startup('Загрузка checkpoint и оптимизатора'); self.restore()
        self.last_good=(copy.deepcopy(self.model.state_dict()),copy.deepcopy(self.optimizer.state_dict()))

    def metadata(self):
        return dict(format=FORMAT,architecture='causal-tcn-v2',sequence=SEQUENCE,channels=CHANNELS,dilations=DILATIONS,
                    features=[n for n,s in SCHEMA],scales=[s for n,s in SCHEMA],output_scale=OUTPUT_SCALE)

    def restore(self):
        failures=[]
        for pointer in ['current.json','previous.json']:
            try:
                self.train.clear(); self.valid.clear()
                p=json.loads((self.folder/pointer).read_text('utf-8'))
                file=self.folder/p['checkpoint']; inference=self.folder/p['inference']
                if p.get('format')!=FORMAT or file.parent!=self.folder or inference.parent!=self.folder or not file.name.startswith('g-') or not inference.name.startswith('g-') or digest(file)!=p['checkpoint_sha256'] or digest(inference)!=p['inference_sha256']: raise ValueError('Checkpoint integrity mismatch')
                if file.stat().st_size>128*1024*1024 or inference.stat().st_size>2*1024*1024: raise ValueError('Oversized checkpoint')
                saved=torch.load(file,map_location='cpu',weights_only=True)
                if saved['metadata']!=self.metadata(): raise ValueError('Incompatible architecture/feature schema')
                if not all(torch.isfinite(t).all() for t in saved['weights'].values()): raise ValueError('Non-finite checkpoint weights')
                self.model.load_state_dict(saved['weights'],strict=True); self.optimizer.load_state_dict(saved['optimizer'])
                if 'movement' in saved:self.movement.restore(saved['movement'])
                else:self.movement=_movement.MovementTrainer(self.device,self.capacity,self.config)
                for state in self.optimizer.state.values():
                    for key,value in state.items():
                        if isinstance(value,torch.Tensor):
                            if not torch.isfinite(value).all(): raise ValueError('Non-finite optimizer state')
                            state[key]=value.to(self.device)
                for group in self.optimizer.param_groups: group['lr']=float(self.config.get('lr',.001))
                self.steps=int(saved['steps']); self.samples=int(saved['samples']); self.metrics=saved.get('metrics',{}); self.loss=saved.get('train_loss'); self.last_save=float(saved.get('saved_at',0))
                for partition,dest in [('train',self.train),('valid',self.valid)]:
                    xs=saved.get(partition+'_x'); ys=saved.get(partition+'_y')
                    if xs is not None:
                        if xs.ndim!=3 or xs.shape[1:]!=(SEQUENCE,len(SCHEMA)) or ys.shape!=(len(xs),2) or not torch.isfinite(xs).all() or not torch.isfinite(ys).all(): raise ValueError('Invalid replay buffer')
                        for x,y in zip(xs.numpy(),ys.numpy()): dest.append((x.copy(),y.copy()))
                if pointer=='previous.json': self.fallback+='; restored previous checkpoint: '+str(failures)
                return
            except Exception as e: failures.append(str(e))
        raise ValueError('Cannot restore model: '+'; '.join(failures))

    def add(self,row):
        x=np.asarray(row['x'],dtype=np.float32); y=np.asarray(row['y'],dtype=np.float32)
        if x.shape!=(len(SCHEMA),) or y.shape!=(2,) or not np.isfinite(x).all() or not np.isfinite(y).all(): raise ValueError('Invalid demonstration')
        self.local_row+=1; group=(self.session,int(row['segment']),int(row['seq'])//128)
        if group!=self.last_group: self.history.clear(); self.last_group=group; self.group_rows=0
        self.group_rows+=1; self.samples+=1
        if int(row['seq'])%128<4: return  # purge derivative/label context at temporal partition boundaries
        if not self.history: self.history.extend(x.copy() for _ in range(SEQUENCE-1))
        self.history.append(x)
        if len(self.history)==SEQUENCE:
            # Entire windows belong to a single disjoint time block. No neighboring train/validation overlap.
            partition=self.valid if group[2]%5==1 else self.train
            partition.append((np.stack(self.history).copy(),y/np.asarray(OUTPUT_SCALE,dtype=np.float32)))
            if row.get('controls') is not None:self.movement.add(np.stack(self.history),row['controls'],group[2]%5==1)

    def log_row(self,row):
        self.log_folder.mkdir(parents=True,exist_ok=True)
        if self.log is None or self.log.tell()>25_000_000:
            if self.log: self.log.close()
            path=self.log_folder/(str(time.time_ns())+'-'+self.session+'.jsonl')
            self.log=path.open('w',encoding='utf-8')
            self.log.write(json.dumps(dict(type='schema',metadata=self.metadata(),name=self.name))+'\n')
            for old in sorted(self.log_folder.glob('*.jsonl'))[:-8]: old.unlink()
        self.log.write(json.dumps(row,allow_nan=False,separators=(',',':'))+'\n')

    def close_log(self):
        if self.log: self.log.flush(); self.log.close(); self.log=None

    def batch(self,source,count):
        rows=random.sample(list(source),min(count,len(source)))
        return (torch.from_numpy(np.stack([r[0] for r in rows])).to(self.device),torch.from_numpy(np.stack([r[1] for r in rows])).to(self.device))

    def update(self,force=False):
        minimum=max(64,int(self.config.get('minimum',256))); batch=int(self.config.get('batch',64))
        if len(self.train)<minimum or len(self.valid)<32 or (not force and self.samples-self.last_update_samples<8): return False
        self.last_update_samples=self.samples; self.model.train()
        for _ in range(max(1,min(8,int(self.config.get('updates',2))))):
            x,y=self.batch(self.train,batch); self.optimizer.zero_grad(set_to_none=True)
            output=self.model(x); loss=F.smooth_l1_loss(output,y,beta=.1)
            if not torch.isfinite(loss) or not torch.isfinite(output).all(): raise FloatingPointError('Non-finite forward/loss')
            loss.backward()
            if any(p.grad is not None and not torch.isfinite(p.grad).all() for p in self.model.parameters()): raise FloatingPointError('Non-finite gradients')
            self.gradient=float(torch.nn.utils.clip_grad_norm_(self.model.parameters(),10).item())
            before=[p.detach().clone() for p in self.model.parameters()]
            self.optimizer.step()
            if not all(torch.isfinite(p).all() for p in self.model.parameters()):
                self.model.load_state_dict(self.last_good[0]); self.optimizer.load_state_dict(self.last_good[1]); raise FloatingPointError('Non-finite optimizer update; restored good weights')
            self.weight_delta=float(sum((p.detach()-old).square().sum() for p,old in zip(self.model.parameters(),before)).sqrt().item())
            self.steps+=1; self.loss=float(loss.item())
            self.last_good=(copy.deepcopy(self.model.state_dict()),copy.deepcopy(self.optimizer.state_dict()))
        self.movement.update()
        if self.steps%20<8: self.validate()
        if time.time()-self.last_save>=float(self.config.get('checkpoint_seconds',30)): self.save()
        return True

    def validate(self):
        if not self.valid: return
        self.model.eval()
        with torch.no_grad():
            x,y=self.batch(self.valid,512); pred=self.model(x)
            if not torch.isfinite(pred).all(): raise FloatingPointError('Non-finite validation prediction')
            persistence=x[:,-1,[20,21]]
            errors=(pred-y)*30
            self.metrics=dict(validation_loss=float(F.smooth_l1_loss(pred,y,beta=.1).item()),mae_degrees=float(errors.abs().mean().item()),
               baseline_zero_mae=float((y*30).abs().mean().item()),baseline_persistence_mae=float(((persistence-y)*30).abs().mean().item()),
               validation_windows=len(self.valid),train_windows=len(self.train))

    def save(self):
        self.validate(); generation='g-'+str(self.steps)+'-'+uuid.uuid4().hex[:10]
        movement_data,movement_inference=self.movement.save()
        saved_at=time.time(); weights={k:v.detach().cpu() for k,v in self.model.state_dict().items()}
        data=dict(metadata=self.metadata(),weights=weights,optimizer=self.optimizer.state_dict(),steps=self.steps,samples=self.samples,metrics=self.metrics,saved_at=saved_at,train_loss=self.loss,training_device=str(self.device),training_backend='PyTorch '+torch.__version__)
        data['movement']=movement_data
        for key,source in [('train',self.train),('valid',self.valid)]:
            if source:
                data[key+'_x']=torch.from_numpy(np.stack([r[0] for r in source])); data[key+'_y']=torch.from_numpy(np.stack([r[1] for r in source]))
        file=self.folder/(generation+'.pt'); tmp=file.with_suffix('.pt.tmp')
        with tmp.open('wb') as f: torch.save(data,f); f.flush(); os.fsync(f.fileno())
        os.replace(tmp,file)
        inference=dict(**self.metadata(),steps=self.steps,samples=self.samples,weights={k:v.flatten().tolist() for k,v in weights.items()},saved_at=saved_at,metrics=self.metrics,train_loss=self.loss,training_device=str(self.device),training_backend='PyTorch '+torch.__version__)
        inference['movement']=movement_inference
        inference_file=self.folder/(generation+'.json'); atomic_json(inference_file,inference)
        pointer=self.folder/'current.json'
        if pointer.exists():
            try:
                previous=json.loads(pointer.read_text('utf-8'))
                if digest(self.folder/previous['checkpoint'])==previous['checkpoint_sha256'] and digest(self.folder/previous['inference'])==previous['inference_sha256']: atomic_json(self.folder/'previous.json',previous)
            except Exception: pass
        atomic_json(pointer,dict(format=FORMAT,name=self.name,steps=self.steps,saved_at=saved_at,checkpoint=file.name,inference=inference_file.name,
           checkpoint_sha256=digest(file),inference_sha256=digest(inference_file)))
        self.last_save=saved_at; self.saved_samples=self.samples
        keep={generation}
        if (self.folder/'previous.json').exists(): keep.add(Path(json.loads((self.folder/'previous.json').read_text('utf-8'))['checkpoint']).stem)
        for old in self.folder.glob('g-*'):
            if old.suffix in ('.pt','.json') and old.stem not in keep: old.unlink()

    def status(self,kind='status'):
        emit(kind,name=self.name,device=str(self.device),device_name=self.device_name,backend='PyTorch '+torch.__version__,fallback=self.fallback,
             samples=self.samples,steps=self.steps,train_loss=self.loss,saved_at=self.last_save,weight_delta_l2=self.weight_delta,gradient_l2=self.gradient,
             train_windows=len(self.train),validation_windows=len(self.valid),**self.movement.status(),**{k:v for k,v in self.metrics.items() if k not in ('train_windows','validation_windows')})

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--root',required=True);parser.add_argument('--name',required=True);parser.add_argument('--device',choices=['auto','cpu','gpu'],default='auto');parser.add_argument('--config',default='{}');parser.add_argument('--config-file')
    args=parser.parse_args(); trainer=None
    try:
        trainer=Trainer(args.root,args.name,json.loads(Path(args.config_file).read_text('utf-8') if args.config_file else args.config),args.device)
        faulthandler.cancel_dump_traceback_later(); trainer.status('ready')
        incoming=queue.Queue(maxsize=1024)
        # The control message bypasses training even while samples remain queued.
        # Samples are still added to replay/log before the final atomic checkpoint.
        draining=threading.Event()
        def reader():
            try:
                for line in sys.stdin:
                    if len(line)>65536: raise ValueError('Oversized protocol message')
                    message=json.loads(line)
                    if message.get('cmd')=='drain':
                        draining.set()
                        continue
                    incoming.put(message)
            except Exception as e: incoming.put(dict(cmd='error',message=str(e)))
            finally: incoming.put(dict(cmd='stop'))
        threading.Thread(target=reader,daemon=True).start()
        while True:
            try: message=incoming.get(timeout=1)
            except queue.Empty:
                if not draining.is_set() and trainer.steps>0 and time.time()-trainer.last_save>=float(trainer.config.get('checkpoint_seconds',30)) and trainer.samples!=getattr(trainer,'saved_samples',-1):
                    trainer.save(); trainer.status()
                continue
            if message['cmd']=='stop':
                trainer.status('saving'); trainer.save(); trainer.close_log(); trainer.status('stopped'); return
            if message['cmd']=='error': raise ValueError(message['message'])
            if message['cmd'] in ('sample','event'): trainer.log_row(message)
            if message['cmd']=='sample':
                trainer.add(message)
                if not draining.is_set(): trainer.update()
            if time.time()-trainer.last_emit>1:
                trainer.status('draining' if draining.is_set() else 'status'); trainer.last_emit=time.time()
    except Exception as e:
        if trainer is not None:
            try:
                if trainer.last_good:
                    trainer.model.load_state_dict(trainer.last_good[0]);trainer.optimizer.load_state_dict(trainer.last_good[1])
                trainer.save(); trainer.close_log()
            except Exception: pass
        emit('error',message=type(e).__name__+': '+str(e)); sys.exit(1)

if __name__=='__main__':
    sys.stdout.reconfigure(encoding='utf-8',line_buffering=True); main()
