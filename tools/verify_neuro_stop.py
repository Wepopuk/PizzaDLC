"""Regression: stop drains accepted demonstrations without extra optimizer work."""
import hashlib,json,os,subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def run():
    env=dict(os.environ,OMP_NUM_THREADS='1',MKL_NUM_THREADS='1',PYTHONIOENCODING='utf-8')
    with tempfile.TemporaryDirectory(prefix='stop-',dir=ROOT/'build/neuro-check') as folder:
        def session(name,count,drain):
            rows=[dict(cmd='drain')] if drain else []
            for i in range(count):
                rows.append(dict(cmd='sample',seq=i,segment=0,x=[0.0]*37,y=[1.0,-1.0],controls=[1,0,1,1]))
            rows.append(dict(cmd='stop'))
            command=[sys.executable,'-u',str(ROOT/'src/main/resources/neuro/trainer.py'),'--root',folder,'--name',name,'--device','cpu','--config',json.dumps(dict(minimum=64,updates=1,capacity=1024,checkpoint_seconds=999999))]
            result=subprocess.run(command,input=''.join(json.dumps(row)+'\n' for row in rows),capture_output=True,text=True,encoding='utf-8',env=env,timeout=90)
            assert result.returncode==0,result.stderr+result.stdout
            output=[json.loads(line) for line in result.stdout.splitlines()]
            assert output[-1]['type']=='stopped',output[-1]
            assert any(row['type']=='saving' for row in output)
            model=Path(folder)/'models'/name
            pointer=json.loads((model/'current.json').read_text())
            for key in ('checkpoint','inference'):
                assert hashlib.sha256((model/pointer[key]).read_bytes()).hexdigest()==pointer[key+'_sha256']
            return output[-1],pointer
        first,_=session('drained',1000,True)
        assert first['samples']==1000 and first['steps']==0,first
        resumed,_=session('drained',500,True)
        assert resumed['samples']==1500 and resumed['steps']==0,resumed
        trained,_=session('trained',400,False)
        assert trained['steps']>0 and trained['movement_steps']>0,trained
        saved,_=session('trained',200,True)
        assert saved['samples']==600 and saved['steps']==trained['steps'] and saved['movement_steps']==trained['movement_steps'],saved
        print('PASS: drain preserves 1000 queued samples, atomic checksums, resume, trained weights and optimizer step counts; no final forced update')
if __name__=='__main__':run()
