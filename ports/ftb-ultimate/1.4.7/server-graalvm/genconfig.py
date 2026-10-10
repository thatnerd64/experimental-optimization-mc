#!/usr/bin/env python3 -I
"""Builds the native-image config dir: agent output + every pack class registered for reflection + non-class resources."""
import json, sys, zipfile, os, shutil
classes_jar, agent_dir, out = sys.argv[1], sys.argv[2], sys.argv[3]
os.makedirs(out, exist_ok=True)
FULL = dict(allDeclaredConstructors=True, allPublicConstructors=True, allDeclaredMethods=True, allPublicMethods=True,
            allDeclaredFields=True, allPublicFields=True)
ent = {}
for e in json.load(open(os.path.join(agent_dir, 'reflect-config.json'))):
    ent[e['name']] = e
n = 0
for name in zipfile.ZipFile(classes_jar).namelist():
    if not name.endswith('.class') or name.startswith('META-INF/'): continue
    cn = name[:-6].replace('/', '.')
    e = ent.setdefault(cn, {'name': cn})
    e.update(FULL)
    # explicit member lists are redundant once the "all*" flags are on
    n += 1
json.dump(list(ent.values()), open(os.path.join(out, 'reflect-config.json'), 'w'))
res = json.load(open(os.path.join(agent_dir, 'resource-config.json')))
res.setdefault('resources', {}).setdefault('includes', []).append({'pattern': '^(?!.*\\.class$).*'})
json.dump(res, open(os.path.join(out, 'resource-config.json'), 'w'))
for f in ('jni-config.json', 'proxy-config.json', 'serialization-config.json'):
    p = os.path.join(agent_dir, f)
    if os.path.exists(p): shutil.copy(p, out)
print('registered', n, 'pack classes; total reflect entries', len(ent))
