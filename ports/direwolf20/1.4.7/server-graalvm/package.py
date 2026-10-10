#!/usr/bin/env python3
"""Package the native binary with its immutable discovery jars and pack config."""
from pathlib import Path
import hashlib
import shutil
import tarfile

d = Path(__file__).resolve().parent
out = d / 'dist' / 'dw20-native'
out.mkdir(parents=True, exist_ok=False)
for name in ['mods', 'coremods', 'lib', 'config']:
    shutil.copytree(d / 'base' / name, out / name)
shutil.copy2(d / 'native' / 'dw20-server', out)
for lib in (d / 'native').glob('*.so'):
    shutil.copy2(lib, out)
shutil.copy2(d / 'start.sh', out)
(out / 'start.sh').chmod(0o755)
shutil.copy2(d / 'RUNTIME.md', out / 'README.md')
shutil.copy2(d / 'VALIDATION.md', out)
(out / 'server.properties').write_text('''server-port=25565
online-mode=true
level-name=world
motd=Direwolf20 1.4.7 - GraalVM
allow-flight=false
max-players=10
view-distance=10
''')
archive = d / 'dw20-native-linux-x86_64.tar.gz'
with tarfile.open(archive, 'w:gz') as tar:
    tar.add(out, arcname=out.name)
sha = hashlib.file_digest(archive.open('rb'), 'sha256').hexdigest()
archive.with_suffix(archive.suffix + '.sha256').write_text(f'{sha}  {archive.name}\n')
print(archive, archive.stat().st_size, sha)
