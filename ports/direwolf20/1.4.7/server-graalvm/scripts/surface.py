import sys, zipfile, struct, collections, glob, os
mods = sorted(glob.glob(sys.argv[1] + '/*.jar') + glob.glob(sys.argv[1] + '/*.zip'))
def parse(data):
    # minimal constant-pool parser
    p = 10; n = struct.unpack('>H', data[8:10])[0]; cp = [None]*n; i = 1
    while i < n:
        t = data[p]
        if t == 1:
            l = struct.unpack('>H', data[p+1:p+3])[0]; cp[i] = ('u', data[p+3:p+3+l].decode('utf8','replace')); p += 3+l
        elif t in (3,4): p += 5
        elif t in (5,6): p += 9; i += 1
        elif t == 7: cp[i] = ('c', struct.unpack('>H', data[p+1:p+3])[0]); p += 3
        elif t == 8: p += 3
        elif t in (9,10,11): cp[i] = ('m', t, *struct.unpack('>HH', data[p+1:p+5])); p += 5
        elif t == 12: cp[i] = ('nt', *struct.unpack('>HH', data[p+1:p+5])); p += 5
        elif t == 15: p += 4
        elif t == 16: p += 3
        elif t == 18: p += 5
        else: raise ValueError(t)
        i += 1
    u = lambda k: cp[k][1]
    cname = lambda k: u(cp[k][1])
    this = struct.unpack('>H', data[p+2:p+4])[0]
    refs = []
    for e in cp:
        if e and e[0] == 'm':
            c = cname(e[2]); nt = cp[e[3]]
            refs.append((c, u(nt[1]), u(nt[2]), e[1]))
    return cname(this), refs
allcls = {}; modrefs = {}
for m in mods:
    z = zipfile.ZipFile(m); names = set()
    for n in z.namelist():
        if n.endswith('.class'):
            try: c, refs = parse(z.read(n)); allcls[c] = m; modrefs.setdefault(m, []).extend(refs); names.add(c)
            except Exception as e: pass
ext = collections.Counter(); extcls = collections.Counter()
for m, refs in modrefs.items():
    for c, name, desc, t in refs:
        if c.startswith('[') or c.startswith('java/') or c.startswith('javax/') or c in allcls: continue
        if c.startswith(('org/lwjgl','org/objectweb','org/luaj','com/google','org/bouncy','argo','paulscode','sun/','org/w3c','org/xml','com/sun')): continue
        ext[(c, name, desc)] += 1; extcls[c] += 1
print('mod jars', len(mods), 'mod classes', len(allcls))
print('distinct external (MC/Forge/FML/obf) classes referenced:', len(extcls))
print('distinct external members referenced:', len(ext))
fm = collections.Counter()
for (c,n,d) in ext:
    fm['forge/fml' if c.startswith(('net/minecraftforge','cpw/')) else ('named-mc' if c.startswith('net/minecraft') else 'obfuscated/other')] += 1
print(dict(fm))
print('top external classes:'); [print(' ', k, v) for k, v in extcls.most_common(15)]
# per-mod
print('per-mod distinct external members:')
for m, refs in sorted(modrefs.items(), key=lambda kv: -len({(c,n,d) for c,n,d,t in kv[1] if (c,n,d) in ext})):
    s = {(c,n,d) for c,n,d,t in refs if (c,n,d) in ext}
    print(f'  {len(s):5} {os.path.basename(m)}')
