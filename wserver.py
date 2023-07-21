# import asyncio
# from websockets.server import serve


# server_ip="localhost"
# server_ip="10.0.0.118"

# async def echo(websocket):
#     async for message in websocket:
#         print('new message: ', message)
#         await websocket.send(message)

# async def main():
#     # async with serve(echo, server_ip, 8080):
#     async with serve(echo, 8080):
#         await asyncio.Future()  # run forever

# asyncio.run(main())


import asyncio
import websockets

async def echo(websocket, path):
    # This function will handle incoming WebSocket connections and messages.
    # The `websocket` parameter represents the WebSocket connection, and `path` represents the URL path.

    try:
        # When a new connection is made, send a welcome message
        await websocket.send("Welcome to the WebSocket server!")

        while True:
            # Wait for incoming message from the client
            message = await websocket.recv()

            # Process the received message (you can customize this part)
            print(f"Received message: {message}")

            # Send a response back to the client
            response = f"Server received: {message}"
            await websocket.send(response)

    except websockets.ConnectionClosedError:
        print("Connection with client closed.")
    except Exception as e:
        print(f"Error: {e}")

# Create and run the WebSocket server
start_server = websockets.serve(echo, "10.0.0.118", 8080)

asyncio.get_event_loop().run_until_complete(start_server)
asyncio.get_event_loop().run_forever()
