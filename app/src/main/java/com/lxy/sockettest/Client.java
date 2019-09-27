package com.lxy.sockettest;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 尝试加入线程池的多进程
 *
 */

public class Client implements Runnable{
    private static Selector mSelector;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private static final String TAG = "Client";

    static {
        try {
            mSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Client(){
        try {
            SocketChannel channel = null;
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress("127.0.0.1",8080));
            channel.register(mSelector,SelectionKey.OP_CONNECT);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void run() {
        try {
            while (true){
                int select = mSelector.select();
                if(select < 0){
                    continue;
                }
                Set<SelectionKey> keys = mSelector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()){
                    SelectionKey key = it.next();
                    it.remove();
                    if(key.isValid()){
                        if(key.isConnectable()){
                            SocketChannel channel = (SocketChannel) key.channel();
                            if(channel.isConnectionPending()){
                                channel.finishConnect();
                            }
                            channel.register(mSelector,SelectionKey.OP_WRITE);
                        }else if(key.isWritable()){
                            SocketChannel socket = (SocketChannel) key.channel();
                            int x = 1+(int)Math.random()*50;
                            String msg = "服务器你好，我是第"+x+"个客户端";
                            if(socket.isConnected()){
                                socket.write(ByteBuffer.wrap(msg.getBytes()));
                            }
                            socket.register(mSelector,SelectionKey.OP_READ);
                        }else if(key.isReadable()){
                            SocketChannel socket = (SocketChannel) key.channel();
                            buffer.clear();
                            int bytesRead = socket.read(buffer);
                            String msg;
                            if(bytesRead > 0){
                                msg = buffer.array().toString();
                                Log.d(TAG,"收到服务器的来信："+msg);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


