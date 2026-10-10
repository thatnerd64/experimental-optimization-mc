#!/usr/bin/env python3
# Minecraft 1.4.x server-list ping (0xFE 0x01). Prints protocol, version, motd, players. Exit 0 on success.
import socket, sys, time
host, port = sys.argv[1], int(sys.argv[2])
t = time.time()
s = socket.create_connection((host, port), timeout=10)
s.sendall(b'\xfe\x01')
data = b''
while True:
    c = s.recv(4096)
    if not c: break
    data += c
    if len(data) > 3 and len(data) >= 3 + 2 * int.from_bytes(data[1:3], 'big'): break
assert data[0] == 0xFF, data[:8]
txt = data[3:].decode('utf-16-be')
f = txt.split('\0')
print('ping ok in %.0f ms:' % ((time.time() - t) * 1000), f)
