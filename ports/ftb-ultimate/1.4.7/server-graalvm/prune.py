#!/usr/bin/env python3 -I
"""prune.py classes.jar unloadable.txt out.jar : drop enums whose signatures reference missing (client-only) types.
GraalVM's builder calls Class.getEnumConstants() on every enum in the image and aborts when that throws."""
import sys, zipfile
src, lst, out = sys.argv[1:4]
drop = set()
for line in open(lst):
    if '\t' not in line: continue   # multi-line exception message continuation
    name, why = line.rstrip('\n').split('\t', 1)
    if '[ENUM]' in why: drop.add(name.replace('.', '/') + '.class')
zin = zipfile.ZipFile(src)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
    for i in zin.infolist():
        if i.filename in drop: continue
        zout.writestr(i, zin.read(i.filename))
print('dropped', sorted(drop))
