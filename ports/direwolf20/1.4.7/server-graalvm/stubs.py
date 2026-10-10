#!/usr/bin/env python3 -I
"""stubs.py classes.jar outdir : writes empty Java stubs for every type the pack's classes reference (supers, interfaces,
member signatures, constant-pool class refs) that is neither in classes.jar nor part of the JDK. Interfaces stay interfaces."""
import sys, zipfile, struct, os, re
src, outdir = sys.argv[1:3]
JDK = ('java/', 'javax/', 'sun/', 'jdk/', 'com/sun/', 'org/w3c/', 'org/xml/', 'org/ietf/', 'org/omg/')

def parse(d):
    n = struct.unpack('>H', d[8:10])[0]; p = 10; cp = [None] * n; i = 1
    while i < n:
        t = d[p]
        if t == 1:
            l = struct.unpack('>H', d[p+1:p+3])[0]; cp[i] = ('u', d[p+3:p+3+l].decode('utf8', 'replace')); p += 3 + l
        elif t in (3, 4): p += 5
        elif t in (5, 6): p += 9; i += 1
        elif t == 7: cp[i] = ('c', struct.unpack('>H', d[p+1:p+3])[0]); p += 3
        elif t == 8: p += 3
        elif t in (9, 10, 11): p += 5
        elif t == 12: cp[i] = ('nt',) + struct.unpack('>HH', d[p+1:p+5]); p += 5
        elif t == 15: p += 4
        elif t == 16: p += 3
        elif t == 18: p += 5
        else: raise ValueError(t)
        i += 1
    u = lambda k: cp[k][1]
    acc, this, sup = struct.unpack('>HHH', d[p:p+6]); p += 6
    ni = struct.unpack('>H', d[p:p+2])[0]; p += 2
    ifaces = [u(cp[struct.unpack('>H', d[p+2*k:p+2*k+2])[0]][1]) for k in range(ni)]; p += 2 * ni
    descs = []
    for _ in range(2):  # fields, methods
        cnt = struct.unpack('>H', d[p:p+2])[0]; p += 2
        for _ in range(cnt):
            _, ni_, di, ac = struct.unpack('>HHHH', d[p:p+8]); descs.append(u(di)); p += 8
            for _ in range(ac):
                p += 2; l = struct.unpack('>I', d[p:p+4])[0]; p += 4 + l
    classrefs = [u(e[1]) for e in cp if e and e[0] == 'c']
    ntdescs = [u(e[2]) for e in cp if e and e[0] == 'nt']
    return u(cp[this][1]), (u(cp[sup][1]) if sup else None), ifaces, classrefs, descs + ntdescs

have, refs, ifaceset = set(), set(), set()
z = zipfile.ZipFile(src)
for n in z.namelist():
    if not n.endswith('.class'): continue
    name, sup, ifs, crefs, descs = parse(z.read(n))
    have.add(name)
    if sup: refs.add(sup)
    for i in ifs: refs.add(i); ifaceset.add(i)
    for c in crefs: refs.add(c)
    for ds in descs:
        for m in re.findall(r'L([^;()\[<>]+);', ds): refs.add(m)
miss = set()
for r in refs:
    if r.startswith('['):
        r = r.lstrip('[')
        if r.startswith('L') and r.endswith(';'): r = r[1:-1]
        else: continue   # primitive array
    if not r or r in have or r.startswith(JDK) or '/' in r and r.split('/')[0] in ('java', 'javax'): continue
    # only vanilla types (obfuscated default-package names, net/minecraft/*): mods probe optional *mod* classes with
    # Class.forName and must keep seeing them absent
    if '/' in r and not r.startswith('net/minecraft/'): continue
    miss.add(r)
os.makedirs(outdir, exist_ok=True)
for m in sorted(miss):
    pkg, _, simple = m.rpartition('/')
    d = os.path.join(outdir, pkg) if pkg else outdir
    os.makedirs(d, exist_ok=True)
    kind = 'interface' if m in ifaceset else 'class'
    with open(os.path.join(d, simple + '.java'), 'w') as f:
        if pkg: f.write('package %s;\n' % pkg.replace('/', '.'))
        f.write('public %s %s {}\n' % (kind, simple))
print('stubbed', len(miss), 'missing types (%d interfaces)' % len([m for m in miss if m in ifaceset]))
