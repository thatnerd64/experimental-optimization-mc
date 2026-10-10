import sys, zipfile, glob, os, re, collections
pat = {
 'getCodeSource': rb'getCodeSource', 'getProtectionDomain': rb'getProtectionDomain',
 'JarFile': rb'java/util/jar/JarFile', 'ZipFile': rb'java/util/zip/ZipFile',
 'ZipInputStream': rb'java/util/zip/ZipInputStream',
 'getResource(url)': rb'\x00\x0agetResource\x01', 'getResources': rb'getResources\x01',
 'getURLs': rb'getURLs', 'java.class.path': rb'java\.class\.path',
}
res = collections.defaultdict(list)
for f in sorted(glob.glob(sys.argv[1]+'/*.jar')+glob.glob(sys.argv[1]+'/*.zip')+glob.glob(sys.argv[2]+'/*.jar')):
    z = zipfile.ZipFile(f)
    for n in z.namelist():
        if not n.endswith('.class'): continue
        d = z.read(n)
        for k, p in pat.items():
            if re.search(p, d): res[k].append((os.path.basename(f), n[:-6].replace('/', '.')))
for k, v in res.items():
    byjar = collections.Counter(a for a, b in v)
    print(f'{k:20} {len(v):3} classes  ' + ', '.join(f'{a}({c})' for a, c in byjar.most_common(8)))
print()
for k in ('getCodeSource', 'getResource(url)', 'JarFile', 'ZipFile'):
    print(k, ':', ', '.join(b for a, b in res[k]))
