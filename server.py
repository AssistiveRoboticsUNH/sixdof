import socket

host = socket.gethostname()
port = 8080  # initiate port no above 1024

server_socket = socket.socket()  # get instance
# look closely. The bind() function takes tuple as argument
server_socket.bind((host, port))  # bind host address and port together

# configure how many client the server can listen simultaneously
server_socket.listen(2)
conn, address = server_socket.accept()  # accept new connection
print("Connection from: " + str(address))
while True:
    # receive data stream. it won't accept data packet greater than 1024 bytes
    print('waiting for data')
    try:
        data = conn.recv(1024).decode()
        if not data:
            # if data is not received break
            break
        print(data)
        data = "Hello from server"
        conn.send(data.encode())  # send data to the client
    except:
        break 

conn.close()  # close the connection

