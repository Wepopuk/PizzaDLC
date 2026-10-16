"""Import Alice and Oksana GPS clips from the user's Yandex Navigator SCS archive."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source', type=Path, default=ROOT / 'pasting/yandex_navigator_all_voices_250421.scs')
parser.add_argument('--decoder', type=Path, default=ROOT / 'build/gps-voice-source/vgmstream/vgmstream-cli.exe')
parser.add_argument('--ffmpeg', type=Path, default=Path('C:/ffmpeg/bin/ffmpeg.exe'))
args = parser.parse_args()
for path in (args.source, args.decoder, args.ffmpeg):
    if not path.is_file():
        parser.error(f'Missing {path}')

clips = {
    'alice': {'start': 'start_1', 'straight': 'go_straight', 'left': 'turn_left', 'right': 'turn_right', 'uturn': 'u_turn', 'arrived': 'finish_1'},
    'oksana': {'start': 'start', 'straight': 'go_straight', 'left': 'turn_left', 'right': 'turn_right', 'uturn': 'u_turn', 'arrived': 'finish'},
}
scratch = ROOT / 'build/gps-voice-source'
scratch.mkdir(parents=True, exist_ok=True)
assets = ROOT / 'src/main/resources/assets/pizzadlc'
manifest = {'source': args.source.name, 'sourceSha256': hashlib.sha256(args.source.read_bytes()).hexdigest(), 'voices': {}}
with zipfile.ZipFile(args.source) as archive:
    for voice, mappings in clips.items():
        bank_path = f'sound/navigation/yandex_russian_{voice}.bank'
        bank = archive.read(bank_path)
        offset = bank.index(b'FSB5')
        version, count, headers_size, names_size, data_size, codec = struct.unpack_from('<6I', bank, offset + 4)
        if version != 1 or codec != 15 or count > 1000:
            raise ValueError('Unexpected bank format')
        fsb = bank[offset:offset + 60 + headers_size + names_size + data_size]
        fsb_path = scratch / (voice + '.fsb')
        fsb_path.write_bytes(fsb)
        names_start = 60 + headers_size
        samples = {}
        for index in range(count):
            start = names_start + struct.unpack_from('<I', fsb, names_start + index * 4)[0]
            end = fsb.index(b'\0', start)
            samples[fsb[start:end].decode('utf-8')] = index + 1
        output = assets / 'sounds/gps' / voice
        output.mkdir(parents=True, exist_ok=True)
        manifest['voices'][voice] = {'bank': bank_path, 'bankSha256': hashlib.sha256(bank).hexdigest(), 'clips': {}}
        for cue, sample in mappings.items():
            wav = scratch / (voice + '-' + cue + '.wav')
            ogg = output / (cue + '.ogg')
            subprocess.run([str(args.decoder), '-s', str(samples[sample]), '-o', str(wav), str(fsb_path)], check=True, stdout=subprocess.DEVNULL)
            subprocess.run([str(args.ffmpeg), '-hide_banner', '-loglevel', 'error', '-y', '-i', str(wav), '-ac', '1', '-ar', '48000', '-c:a', 'libvorbis', '-q:a', '5', str(ogg)], check=True)
            manifest['voices'][voice]['clips'][cue] = {'sample': sample, 'stream': samples[sample], 'sha256': hashlib.sha256(ogg.read_bytes()).hexdigest()}
            print(f'{voice}: {cue} <- {sample}')
sounds_file = assets / 'sounds.json'
sounds = json.loads(sounds_file.read_text(encoding='utf-8'))
for voice, cues in clips.items():
    for cue in cues:
        sounds[f'gps.{voice}.{cue}'] = {'sounds': [{'name': f'pizzadlc:gps/{voice}/{cue}', 'stream': True}]}
sounds_file.write_text(json.dumps(sounds, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
(assets / 'sounds/gps/source.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
