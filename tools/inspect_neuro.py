"""Read-only diagnostics for a local checkpoint; prints axis statistics, never raw demonstrations."""
import argparse, importlib.util, json
from pathlib import Path
import numpy as np
import torch

def stats(values):
    a=np.asarray(values)
    return dict(mean=float(a.mean()),mean_abs=float(np.abs(a).mean()),std=float(a.std()),
                p90_abs=float(np.quantile(np.abs(a),.9)),small_percent=float((np.abs(a)<.1).mean()*100))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('name');parser.add_argument('--root',default='run/pizzadlc/neuro');args=parser.parse_args()
    project=Path(__file__).resolve().parents[1];folder=Path(args.root)/'models'/args.name
    pointer=json.loads((folder/'current.json').read_text('utf-8'))
    saved=torch.load(folder/pointer['checkpoint'],map_location='cpu',weights_only=True)
    spec=importlib.util.spec_from_file_location('neuro_inspect_backend',project/'src/main/resources/neuro/trainer.py')
    backend=importlib.util.module_from_spec(spec);spec.loader.exec_module(backend)
    torch.set_num_threads(1);model=backend.TCN();model.load_state_dict(saved['weights']);model.eval()
    report=dict(name=args.name,steps=saved['steps'],samples=saved['samples'],partitions={})
    with torch.no_grad():
        for split in ('train','valid'):
            x=saved.get(split+'_x');y=saved.get(split+'_y')
            if x is None:continue
            pred=torch.cat([model(batch) for batch in x.split(128)]).numpy()*30;actual=y.numpy()*30
            axes={}
            for i,name in enumerate(('yaw','pitch')):
                moving=np.abs(actual[:,i])>=.5
                axes[name]=dict(manual=stats(actual[:,i]),predicted=stats(pred[:,i]),
                    mae=float(np.abs(actual[:,i]-pred[:,i]).mean()),
                    moving_mae=float(np.abs(actual[moving,i]-pred[moving,i]).mean()) if moving.any() else None,
                    moving_count=int(moving.sum()),
                    last_mae=float(np.abs(actual[:,i]-x[:,-1,20+i].numpy()*30).mean()))
            report['partitions'][split]=axes
    text=json.dumps(report,ensure_ascii=False,indent=2);print(text)
    destination=project/'build/neuro-check'/('inspection-'+args.name+'.json');destination.parent.mkdir(parents=True,exist_ok=True);destination.write_text(text,encoding='utf-8')

if __name__=='__main__':main()
