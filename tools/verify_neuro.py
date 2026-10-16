"""Executable integration checks on SYNTHETIC signals, not evidence of a user's aiming style."""
import importlib.util,json,sys,time,subprocess,tempfile,hashlib,queue,threading
from pathlib import Path
import numpy as np
import torch

PROJECT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('neuro_trainer',PROJECT/'src/main/resources/neuro/trainer.py')
backend=importlib.util.module_from_spec(spec);spec.loader.exec_module(backend)

def check(ok,text):
    if not ok: raise AssertionError(text)

def synthetic(i,segment=0):
    # A stationary exogenous context with two independent angular outputs. No future targets enter x.
    phase=i*.113; x=np.zeros(len(backend.SCHEMA),np.float32)
    x[0]=1; x[7]=np.sin(phase)*.3; x[8]=np.cos(phase*.77)*.2
    x[13]=np.cos(phase)*.2;x[4]=.4;x[24]=1;x[26]=.8
    x[20]=np.sin((i-1)*.113)*.18; x[21]=np.cos((i-1)*.113*.77)*.08
    y=[float(18*x[7]+2*x[13]),float(12*x[8])]
    forward=1 if x[7]>.05 else -1 if x[7]<-.05 else 0
    strafe=1 if x[13]>.05 else -1 if x[13]<-.05 else 0
    return dict(cmd='sample',seq=i,segment=segment,x=x.tolist(),y=y,controls=[forward,strafe,int(x[8]>.12),int(forward>0)])

def run():
    started=time.time(); root=PROJECT/'build/neuro-check';root.mkdir(parents=True,exist_ok=True)
    # Each run owns a new model name; never touch real user checkpoints.
    name='synthetic_'+str(time.time_ns())[-12:]
    t=backend.Trainer(root,name,dict(minimum=64,batch=64,capacity=1024,updates=2,checkpoint_seconds=999999),'auto')
    check(t.steps==0 and not t.update(True),'Empty session updated weights')
    for i in range(40):t.add(synthetic(i))
    check(not t.update(True),'Too-short session updated weights')
    # Fresh segment ensures temporal test has explicit identity in every input.
    for i in range(1800):
        row=synthetic(i,1);t.add(row)
        if i%8==0:t.update()
    check(t.steps>100 and t.weight_delta>0 and t.gradient>0,'No genuine backprop/optimizer update')
    check(t.movement.steps>100 and t.movement.weight_delta>0 and t.movement.gradient>0,'Movement policy did not learn')
    t.movement.validate();check(t.movement.metrics['movement_direction_accuracy']>.7,'Movement policy did not learn directions')
    print('Movement holdout:',json.dumps(t.movement.status()),flush=True)
    check(t.movement.metrics['movement_jump_recall']>.55 and t.movement.metrics['movement_jump_precision']>.55,'Movement policy did not learn jumps above the balanced baseline: '+str(t.movement.metrics))
    t.validate();metrics=t.metrics.copy()
    check(metrics['mae_degrees']<metrics['baseline_zero_mae']*.5,'Model did not beat zero baseline')
    check(metrics['mae_degrees']<metrics['baseline_persistence_mae'],'Model did not beat persistence baseline')
    check(len(t.train)<=1024 and len(t.valid)<=256,'Unbounded replay')
    # All contexts in one window must stay within its own temporal block, incl. purged derivative margin.
    splits={}
    for i in range(2048):
        group=i//128
        frames=set(range(i-15-2,i+2))
        if i%128>=19:splits.setdefault('valid' if group%5==1 else 'train',set()).update(frames)
    check(not splits['train'].intersection(splits['valid']),'Adjacent temporal partitions overlap')
    split=backend.Trainer(root,'split_'+str(time.time_ns())[-12:],dict(capacity=1024),'cpu')
    for i in range(700):
        row=synthetic(i);row['x'][0]=i;split.add(row)
    actual_train={int(v) for x,y in split.train for v in x[:,0]}
    actual_valid={int(v) for x,y in split.valid for v in x[:,0]}
    check(actual_train and actual_valid and not actual_train.intersection(actual_valid),'Actual replay partitions share input frames')
    check(all(abs(a-b)>3 for a in actual_train for b in actual_valid),'No purge margin in actual replay')
    invalid=synthetic(1801);invalid['x'][0]=float('nan')
    try:t.add(invalid);raise AssertionError('NaN accepted')
    except ValueError:pass
    t.save(); saved=t.steps
    restored=backend.Trainer(root,name,t.config,'auto')
    check(restored.steps==saved and len(restored.optimizer.state)>0,'Optimizer/steps were not restored')
    check(restored.movement.steps==t.movement.steps and len(restored.movement.optimizer.state)>0,'Movement weights/optimizer were not restored')
    legacy_movement=restored.movement.save()[0];legacy_movement['metadata']=dict(legacy_movement['metadata'],architecture='causal-controls-v1')
    migrated=backend._movement.MovementTrainer(restored.device,restored.capacity,restored.config);migrated.restore(legacy_movement)
    check(migrated.steps==restored.movement.steps and migrated.samples==restored.movement.samples and not migrated.optimizer.state,'Legacy movement weights/replay did not migrate safely')
    before=[v.detach().clone() for v in restored.model.parameters()];restored.update(True)
    check(restored.steps>saved and any(not torch.equal(a,b) for a,b in zip(before,restored.model.parameters())),'Resume did not update weights')
    correct=[p.detach().clone() for p in restored.model.parameters()];steps=restored.steps
    hook=restored.model.head.weight.register_hook(lambda g:g*float('nan'))
    try:
        try:restored.update(True);raise AssertionError('Non-finite gradient accepted')
        except FloatingPointError:pass
        check(restored.steps==steps and all(torch.equal(a,b) for a,b in zip(correct,restored.model.parameters())),'Bad gradient changed model')
    finally:hook.remove()
    restored.save()
    # Corrupt newest checkpoint: previous generation must survive in BOTH Python and Java.
    pointer=json.loads((restored.folder/'current.json').read_text())
    newest=restored.folder/pointer['checkpoint']; content=newest.read_bytes();newest.write_bytes(b'corrupt')
    fallback=backend.Trainer(root,name,t.config,'auto');check(fallback.steps==saved,'Previous generation recovery failed')
    newest.write_bytes(content)
    current=json.loads((restored.folder/pointer['inference']).read_text())
    sequences=[];restored.model.eval()
    with torch.no_grad():
        for end in (31,150,700):
            x=np.stack([np.asarray(synthetic(i)['x'],np.float32) for i in range(end-15,end+1)])
            y=(restored.model(torch.from_numpy(x[None]).to(restored.device))[0]*30).cpu().tolist()
            movement=restored.movement.model(torch.from_numpy(x[None]).to(restored.device))[0].cpu().tolist()
            sequences.append(dict(x=x.tolist(),y=y,movement_logits=movement))
    (root/'parity.json').write_text(json.dumps(dict(model=current,sequences=sequences,name=name)),encoding='utf-8')
    # Separate subprocess protocol: readiness, data streaming, atomic stop/save and restart.
    protocol_name='protocol_'+str(time.time_ns())[-12:]
    command=[sys.executable,'-u',str(PROJECT/'src/main/resources/neuro/trainer.py'),'--root',str(root),'--name',protocol_name,'--device','cpu','--config',json.dumps(dict(minimum=64,updates=1))]
    proc=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,encoding='utf-8')
    output=[]
    reader=threading.Thread(target=lambda:output.extend(json.loads(line) for line in proc.stdout),daemon=True);reader.start()
    for i in range(700):proc.stdin.write(json.dumps(synthetic(i))+'\n')
    proc.stdin.write('{"cmd":"stop"}\n');proc.stdin.flush();proc.stdin.close()
    try: proc.wait(timeout=90)
    finally:
        if proc.poll() is None: proc.kill();proc.wait(timeout=10)
        reader.join(5)
    check(proc.returncode==0,proc.stderr.read());check(output[0]['type']=='starting' and any(row['type']=='ready' for row in output) and output[-1]['type']=='stopped','Process lifecycle protocol failed')
    check(output[-1]['steps']>0 and output[-1]['weight_delta_l2']>0,'Subprocess did not learn')
    # A protocol/data failure must exit with a concrete error and preserve a resumable correct checkpoint.
    failed=subprocess.run(command,input=json.dumps(dict(cmd='sample',seq=0,segment=0,x=[],y=[]))+'\n',capture_output=True,text=True,encoding='utf-8',timeout=90)
    check(failed.returncode!=0 and 'Invalid demonstration' in failed.stdout,'Trainer error was not reported')
    recovered=backend.Trainer(root,protocol_name,dict(minimum=64,updates=1),'cpu')
    check(recovered.steps==output[-1]['steps'] and all(torch.isfinite(p).all() for p in recovered.model.parameters()),'Process error destroyed good model')
    # Forced CPU selection is explicit; unavailable GPU path is tested by a real mocked CUDA-unavailable probe.
    original=torch.cuda.is_available;torch.cuda.is_available=lambda:False
    try:
        device,_,reason=backend.select_device('auto');check(device.type=='cpu' and reason,'Silent GPU fallback')
        try:backend.select_device('gpu');raise AssertionError('GPU-required silently fell back')
        except RuntimeError:pass
    finally:torch.cuda.is_available=original
    result=dict(device=str(t.device),device_name=t.device_name,backend=torch.__version__,steps=t.steps,weight_delta_l2=t.weight_delta,gradient_l2=t.gradient,
                metrics=metrics,movement=t.movement.status(),seconds=time.time()-started,protocol_steps=output[-1]['steps'],synthetic_only=True)
    (root/'result.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result,indent=2))
    print('PASS: online training, holdout, baselines, bounded replay, finite guards, save/resume, recovery, subprocess, CPU fallback; Java parity fixture written')

if __name__=='__main__':run()
