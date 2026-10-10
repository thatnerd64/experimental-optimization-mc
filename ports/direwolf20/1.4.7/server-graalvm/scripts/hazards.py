import sys, zipfile, glob, os, re, collections
pat = {
 'asm': rb'org/objectweb/asm/',
 'proxy': rb'java/lang/reflect/Proxy',
 'defineClass': rb'defineClass',
 'EnumHelper': rb'EnumHelper',
 'Unsafe': rb'sun/misc/Unsafe|sun/reflect/',
 'serialization': rb'java/io/ObjectInputStream|java/io/ObjectOutputStream',
 'ScriptEngine': rb'javax/script/',
 'setAccessible': rb'setAccessible',
 'loadLibrary': rb'loadLibrary',
 'ServiceLoader': rb'java/util/ServiceLoader',
 'URLClassLoader': rb'java/net/URLClassLoader',
 'getResourceAsStream': rb'getResourceAsStream',
 'javax.tools': rb'javax/tools/',
 'Class.forName': rb'forName',
}
res = collections.defaultdict(lambda: collections.defaultdict(int))
for f in sorted(glob.glob(sys.argv[1]+'/*.jar')+glob.glob(sys.argv[1]+'/*.zip')+glob.glob(sys.argv[2]+'/*.jar')):
    z = zipfile.ZipFile(f)
    for n in z.namelist():
        if not n.endswith('.class'): continue
        d = z.read(n)
        for k, p in pat.items():
            if re.search(p, d): res[k][os.path.basename(f)] += 1
for k, v in res.items():
    print(f'{k:20} classes={sum(v.values()):5} jars={len(v):2}  top: ' + ', '.join(f'{a}({b})' for a, b in sorted(v.items(), key=lambda x: -x[1])[:5]))
