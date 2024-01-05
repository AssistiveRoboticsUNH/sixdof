package com.example.myapplication;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ZMQ_Pub extends Thread{
    int fq=10;
    boolean is_alive=false;
    private ZContext context;
    private ZMQ.Socket socket;
    ZMQ_Interface zmq_interface;
    public ZMQ_Pub(ZMQ_Interface zmq_interface, int fq){
        this.zmq_interface=zmq_interface;
        this.fq=fq;

        context = new ZContext();
        socket = context.createSocket(SocketType.PUB);
        socket.bind("tcp://*:5555");
    }

    public void run(){
        is_alive=true;
        while (is_alive){
            String response = zmq_interface.generate_zmq_pub_msg();
            socket.send(response.getBytes(ZMQ.CHARSET), 0);

            try{
                Thread.sleep( (int)(1000.0/this.fq) ) ;
            }catch (Exception ex){}
        }
    }
    public void publish(String txt){
        socket.send(txt.getBytes(ZMQ.CHARSET), 0);
    }

    public void close(){
        is_alive=false;
        socket.close();
        context.close();
    }


}
