import zmq

context = zmq.Context()

#  Socket to talk to server
print("Connecting to hello world server…")
socket = context.socket(zmq.SUB)

 
serverIP="10.0.0.12"
socket.connect(f"tcp://{serverIP}:5555")

# Subscribe to all messages (empty string means subscribe to all topics)
socket.setsockopt_string(zmq.SUBSCRIBE, "")

print("Subscriber is ready to receive messages...")

while True:
    # Receive a message
    message = socket.recv_string()
    print(f"Received: {message}")

 
# request = 0
# while True:
#     request += 1
#     # print("Sending request %s …" % request)
#     # socket.send(b"Hello")

#     #  Get the reply.
#     message = socket.recv()
#     print(f'Received reply {request} [ {message} ]')
