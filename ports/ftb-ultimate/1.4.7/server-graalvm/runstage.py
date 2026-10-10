#!/usr/bin/env python3
"""Isolated startup/login/shutdown checks for the Ultimate native port."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import time

D = Path(__file__).resolve().parent
p = argparse.ArgumentParser()
p.add_argument('mode', choices=['base', 'record', 'aot', 'agent', 'native'])
p.add_argument('--name')
p.add_argument('--seconds', type=int, default=30)
p.add_argument('--flight', action='store_true')
p.add_argument('--world-from', type=Path)
p.add_argument('--bundle', type=Path)
a = p.parse_args()
if 'GRAALVM_HOME' not in os.environ:
    p.error('Set GRAALVM_HOME to a GraalVM installation')
if a.mode in ('base', 'record') and 'JDK8_HOME' not in os.environ:
    p.error('Set JDK8_HOME to a JDK 8 installation')
r = D / 'checks' / (a.name or a.mode)
r.mkdir(parents=True, exist_ok=False)
if a.bundle:
    if a.mode != 'native':
        p.error('--bundle requires native mode')
    shutil.copytree(a.bundle, r, dirs_exist_ok=True)
else:
    for name in ['mods', 'coremods', 'lib', 'config']:
        shutil.copytree(D / 'base' / name, r / name)
shutil.copy2(D / 'base' / 'server.properties', r / 'server.properties')
if a.world_from:
    shutil.copytree(a.world_from, r / 'world')
java8 = str(Path(os.environ.get('JDK8_HOME', '')) / 'bin/java')
java21 = str(Path(os.environ['GRAALVM_HOME']) / 'bin/java')
cp = f'{D}/glue/bin:{D}/aotwork/out/classes.jar:{D}/aotwork/out/resources.jar'
if a.mode == 'base':
    shutil.copy2(D / 'base-server.jar', r / 'server.jar')
    cmd = [java8, '-Xmx4g', f'-Djava.security.properties={D}/jar-policy.properties', '-jar', 'server.jar', 'nogui']
elif a.mode == 'native':
    cmd = [str(r / 'start.sh')] if a.bundle else [str(D / 'native' / 'ultimate-server'), '-Xmx4g', 'nogui']
else:
    cmd = [java8 if a.mode == 'record' else java21, '-Xmx4g', f'-Djava.security.properties={D}/jar-policy.properties']
    if a.mode == 'record':
        cp = cp.replace('/out/', '/out-rec/')
        cmd += [f'-Dretro.enums.out={D}/enum-additions.txt']
    if a.mode == 'agent':
        (D / 'agent-cfg').mkdir(exist_ok=True)
        cmd += [f'-agentlib:native-image-agent=config-merge-dir={D}/agent-cfg,config-write-period-secs=20']
    cmd += ['-cp', cp, 'cpw.mods.fml.relauncher.ServerBoot', 'nogui']
print('RUN', a.mode, r, flush=True)
ok = False
started = time.monotonic()
with (r / 'run.log').open('w') as log:
    proc = subprocess.Popen(cmd, cwd=r, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
    try:
        deadline = time.monotonic() + 360
        while time.monotonic() < deadline:
            text = (r / 'run.log').read_text(errors='replace')
            if 'Done (' in text:
                ok = True
                print('READY pid=', proc.pid, 'boot_seconds=', round(time.monotonic() - started, 2), flush=True)
                status = Path(f'/proc/{proc.pid}/status').read_text()
                print('\n'.join(x for x in status.splitlines() if x.startswith(('VmRSS:', 'Threads:'))), flush=True)
                break
            if proc.poll() is not None:
                break
            time.sleep(2)
        if ok:
            ping = subprocess.run(['python3', str(D / 'scripts/ping.py'), '127.0.0.1', '25591'], capture_output=True, text=True, timeout=30)
            print('PING', ping.returncode, ping.stdout, ping.stderr, flush=True)
            bot_cmd = [java21, '-cp', str(D / 'base/lib/guava-12.0.1.jar'), str(D / 'scripts/Bot.java'), '127.0.0.1', '25591', 'UltimateTest', str(a.seconds), str(r / 'ForgeModLoader-server-0.log')]
            if a.flight:
                bot_cmd += [str(r / 'run.log')]
            bot = subprocess.run(bot_cmd, capture_output=True, text=True, timeout=a.seconds + 120)
            (r / 'bot.log').write_text(bot.stdout + bot.stderr)
            print('BOT', bot.returncode, bot.stdout[-2500:], bot.stderr[-1000:], flush=True)
            server_text = (r / 'run.log').read_text(errors='replace')
            ok = (bot.returncode == 0 and ping.returncode == 0
                  and 'logged in with entity id' in server_text
                  and 'answered mod list request:' in bot.stdout
                  and 'reader alive' in bot.stdout)
            if a.flight:
                ok = ok and 'final position x=' in bot.stdout and 'echoing spawn:' in bot.stdout
            proc.stdin.write('save-all\nstop\n')
            proc.stdin.flush()
            proc.wait(timeout=90)
            ok = ok and proc.returncode == 0
        print('RESULT', a.mode, 'OK' if ok else 'FAILED', 'exit', proc.poll(), flush=True)
    finally:
        if proc.poll() is None:
            proc.terminate()
            try:
                proc.wait(timeout=30)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait()
print('\n'.join((r / 'run.log').read_text(errors='replace').splitlines()[-25:]), flush=True)
raise SystemExit(0 if ok else 1)
