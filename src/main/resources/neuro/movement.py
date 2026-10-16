"""Optional learned keyboard policy. No scripted pursuit or jump timer."""
import collections, copy, random
import numpy as np
import torch
from torch import nn
from torch.nn import functional as F

METADATA=dict(architecture='causal-controls-v2',sequence=16,channels=16,dilations=[1,2,4],
              features=37,outputs=['direction_9','jump_logit','sprint_logit'])

class MovementNet(nn.Module):
    def __init__(self):
        super().__init__()
        self.layers=nn.ModuleList([nn.Conv1d(37 if i==0 else 16,16,3,dilation=d) for i,d in enumerate([1,2,4])])
        self.head=nn.Linear(16,11)
        # Keep a trainable path from the current state to the controls. It helps
        # sparse labels such as jumps converge without replacing learning with rules.
        self.skip=nn.Linear(37,11)
        nn.init.zeros_(self.head.weight);nn.init.zeros_(self.head.bias)
        nn.init.zeros_(self.skip.weight);nn.init.zeros_(self.skip.bias)
    def forward(self,x):
        current=x[:,-1,:]
        x=x.transpose(1,2)
        for layer,d in zip(self.layers,[1,2,4]):x=F.relu(layer(F.pad(x,(2*d,0))))
        return self.head(x[:,:,-1])+self.skip(current)

class MovementTrainer:
    def __init__(self,device,capacity,config):
        self.device=device;self.config=config;self.model=MovementNet().to(device)
        self.optimizer=torch.optim.AdamW(self.model.parameters(),lr=float(config.get('lr',.001)),weight_decay=1e-5)
        self.train=collections.deque(maxlen=capacity);self.valid=collections.deque(maxlen=max(128,capacity//4))
        self.steps=0;self.samples=0;self.jump_samples=0;self.loss=None;self.metrics={};self.weight_delta=0.;self.gradient=0.
        self.last_good=(copy.deepcopy(self.model.state_dict()),copy.deepcopy(self.optimizer.state_dict()))
    def add(self,window,controls,validation):
        a=np.asarray(controls,dtype=np.float32)
        if a.shape!=(4,) or not np.isfinite(a).all() or not all(v in (-1,0,1) for v in a[:2]) or not all(v in (0,1) for v in a[2:]):raise ValueError('Invalid manual movement labels')
        (self.valid if validation else self.train).append((window.copy(),a.copy()))
        self.samples+=1;self.jump_samples+=int(a[2]>0)
    def batch(self,source,count):
        rows=random.sample(list(source),min(count,len(source)))
        return tuple(torch.from_numpy(np.stack([row[i] for row in rows])).to(self.device) for i in (0,1))
    @staticmethod
    def loss_for(logits,y):
        direction=((y[:,0]+1)*3+y[:,1]+1).long()
        # Rare jump presses must contribute gradients instead of disappearing behind idle ticks.
        positive=y[:,2:].sum(0);weight=((len(y)-positive)/positive.clamp_min(1)).clamp(1,8)
        return F.cross_entropy(logits[:,:9],direction)+F.binary_cross_entropy_with_logits(logits[:,9:],y[:,2:],pos_weight=weight)
    def update(self):
        if len(self.train)<max(64,int(self.config.get('minimum',256))) or len(self.valid)<32:return
        self.model.train()
        for _ in range(max(1,min(8,int(self.config.get('updates',2))*3))):
            x,y=self.batch(self.train,int(self.config.get('batch',64)));self.optimizer.zero_grad(set_to_none=True)
            logits=self.model(x);loss=self.loss_for(logits,y)
            if not torch.isfinite(logits).all() or not torch.isfinite(loss):raise FloatingPointError('Non-finite movement loss/output')
            loss.backward()
            if any(p.grad is not None and not torch.isfinite(p.grad).all() for p in self.model.parameters()):raise FloatingPointError('Non-finite movement gradients')
            self.gradient=float(torch.nn.utils.clip_grad_norm_(self.model.parameters(),10).item())
            before=[p.detach().clone() for p in self.model.parameters()];self.optimizer.step()
            if not all(torch.isfinite(p).all() for p in self.model.parameters()):
                self.recover();raise FloatingPointError('Non-finite movement update; restored good weights')
            self.weight_delta=float(sum((p.detach()-old).square().sum() for p,old in zip(self.model.parameters(),before)).sqrt().item())
            self.loss=float(loss.item());self.steps+=1
            self.last_good=(copy.deepcopy(self.model.state_dict()),copy.deepcopy(self.optimizer.state_dict()))
    def recover(self):
        self.model.load_state_dict(self.last_good[0]);self.optimizer.load_state_dict(self.last_good[1])
    def validate(self):
        if not self.valid or not self.steps:return
        self.model.eval()
        with torch.no_grad():
            x,y=self.batch(self.valid,512);logits=self.model(x)
            if not torch.isfinite(logits).all():raise FloatingPointError('Non-finite movement validation')
            labels=((y[:,0]+1)*3+y[:,1]+1).long();directions=logits[:,:9].argmax(1)
            jumps=logits[:,9].sigmoid()>=.55;truth=y[:,2]>=.5
            positives=(jumps&truth).sum();predicted=jumps.sum();actual=truth.sum()
            self.metrics=dict(movement_validation_loss=float(self.loss_for(logits,y).item()),
                movement_direction_accuracy=float((directions==labels).float().mean().item()),
                movement_idle_baseline=float((labels==4).float().mean().item()),
                movement_jump_precision=float((positives/predicted.clamp_min(1)).item()),
                movement_jump_recall=float((positives/actual.clamp_min(1)).item()))
    def status(self):
        return dict(movement_steps=self.steps,movement_samples=self.samples,movement_jump_samples=self.jump_samples,
                    movement_train_loss=self.loss,movement_weight_delta_l2=self.weight_delta,movement_gradient_l2=self.gradient,**self.metrics)
    def save(self):
        self.validate()
        weights={k:v.detach().cpu() for k,v in self.model.state_dict().items()}
        data=dict(metadata=METADATA,weights=weights,optimizer=self.optimizer.state_dict(),steps=self.steps,samples=self.samples,
                  jump_samples=self.jump_samples,loss=self.loss,metrics=self.metrics)
        for split,rows in [('train',self.train),('valid',self.valid)]:
            if rows:
                data[split+'_x']=torch.from_numpy(np.stack([row[0] for row in rows]));data[split+'_y']=torch.from_numpy(np.stack([row[1] for row in rows]))
        inference=dict(**METADATA,steps=self.steps,samples=self.samples,jump_samples=self.jump_samples,
                       weights={k:v.flatten().tolist() for k,v in weights.items()},metrics=self.metrics)
        return data,inference
    def restore(self,data):
        metadata=data['metadata'];legacy=dict(METADATA,architecture='causal-controls-v1')
        if metadata not in (METADATA,legacy):raise ValueError('Incompatible movement schema')
        weights=data['weights']
        if not all(torch.isfinite(v).all() for v in weights.values()):raise ValueError('Non-finite movement checkpoint')
        current=self.model.state_dict()
        required={key for key in current if not key.startswith('skip.')}
        if not required.issubset(weights):raise ValueError('Incomplete movement checkpoint')
        for key,value in weights.items():
            if key not in current or current[key].shape!=value.shape:raise ValueError('Incompatible movement weight: '+key)
            current[key]=value
        self.model.load_state_dict(current,strict=True)
        if metadata==METADATA:
            self.optimizer.load_state_dict(data['optimizer'])
            for state in self.optimizer.state.values():
                for key,value in state.items():
                    if isinstance(value,torch.Tensor):
                        if not torch.isfinite(value).all():raise ValueError('Non-finite movement optimizer')
                        state[key]=value.to(self.device)
        else:
            # The v1 forward path could fail before its optimizer step. Keep compatible
            # weights and replay, then resume with a fresh optimizer for the v2 graph.
            self.optimizer=torch.optim.AdamW(self.model.parameters(),lr=float(self.config.get('lr',.001)),weight_decay=1e-5)
        for group in self.optimizer.param_groups:group['lr']=float(self.config.get('lr',.001))
        self.steps=int(data['steps']);self.samples=int(data['samples']);self.jump_samples=int(data.get('jump_samples',0));self.loss=data.get('loss');self.metrics=data.get('metrics',{})
        if min(self.steps,self.samples,self.jump_samples)<0 or self.jump_samples>self.samples:raise ValueError('Invalid movement counters')
        self.train.clear();self.valid.clear()
        for split,destination in [('train',self.train),('valid',self.valid)]:
            x=data.get(split+'_x');y=data.get(split+'_y')
            if x is not None:
                if x.ndim!=3 or x.shape[1:]!=(16,37) or y.shape!=(len(x),4) or not torch.isfinite(x).all() or not torch.isfinite(y).all():raise ValueError('Invalid movement replay')
                for a,b in zip(x.numpy(),y.numpy()):
                    if not all(v in (-1,0,1) for v in b[:2]) or not all(v in (0,1) for v in b[2:]):raise ValueError('Invalid movement replay labels')
                    destination.append((a.copy(),b.copy()))
        self.last_good=(copy.deepcopy(self.model.state_dict()),copy.deepcopy(self.optimizer.state_dict()))
