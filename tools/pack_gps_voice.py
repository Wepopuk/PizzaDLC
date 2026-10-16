"""Build a Minecraft 26.2 GPS voice pack from local OGG Vorbis recordings."""
import argparse
import json
from pathlib import Path
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('folder', type=Path)
parser.add_argument('--output', type=Path, default=Path(__file__).resolve().parents[1] / 'run/resourcepacks/GPS-Voice.zip')
args = parser.parse_args()
required = ('straight', 'left', 'right', 'uturn', 'arrived')
files = {}
for cue in (*required, 'start'):
    path = args.folder / (cue + '.ogg')
    if not path.is_file():
        if cue in required:
            parser.error(f'Missing {path}')
        continue
    data = path.read_bytes()
    if not data.startswith(b'OggS') or b'\x01vorbis' not in data[:256]:
        parser.error(f'{path} must be OGG Vorbis (Minecraft cannot play OGG Opus)')
    files[cue] = data
args.output.parent.mkdir(parents=True, exist_ok=True)
sounds = {'gps.' + cue: {'sounds': [{'name': 'pizzadlc:gps/' + cue, 'stream': True}]} for cue in files}
with zipfile.ZipFile(args.output, 'w', zipfile.ZIP_DEFLATED) as archive:
    archive.writestr('pack.mcmeta', json.dumps({'pack': {'min_format': [88, 0], 'max_format': [88, 0], 'description': 'GPS voice guidance'}}, ensure_ascii=False))
    archive.writestr('assets/pizzadlc/sounds.json', json.dumps(sounds))
    for cue, data in files.items():
        archive.writestr('assets/pizzadlc/sounds/gps/' + cue + '.ogg', data)
print(args.output)
