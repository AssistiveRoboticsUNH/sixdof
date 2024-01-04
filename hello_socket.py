# PORT=8855

import socket
import json 
import time 
import numpy as np

def read_lines_from_socket(sock):
    buffer_size = 1024  # You can adjust the buffer size according to your needs
    data = b""
    while True:
        chunk = sock.recv(buffer_size)
        if not chunk:
            break
        data += chunk
        while b'\n' in data:
            line, data = data.split(b'\n', 1)
            yield line.decode('utf-8')

# Replace 'SERVER_IP' and 'PORT' with the actual IP and port of your server
server_ip = '10.0.0.12'
port = 8855

# Create a socket
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Connect to the server
client_socket.connect((server_ip, port))

max_dt=0
last_time=time.time()
dts=[]
for line in read_lines_from_socket(client_socket):
    if line[-2]==',':
        line=line[:-2]+line[-1]
    line = json.loads(line)
    # print("Received:", line)
    ct=time.time()
    dt=ct-last_time
    last_time=ct
    
    dts.append(dt)
    
    if dt>max_dt:
        max_dt=dt 
        
    print(f'fps={1/dt:0.2f} min fps={1/max_dt:0.2f} mean fps={1/np.mean(dts):0.2f} dt={np.mean(dts):0.2f}/{np.max(dts):0.2f}')
    

# Close the socket when done
client_socket.close()
