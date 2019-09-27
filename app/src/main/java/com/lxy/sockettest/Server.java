package com.lxy.sockettest;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 多线程好迷 但是有感觉很有趣 清楚了原理感觉很厉害的亚子
 */

public class Server implements Runnable{
    private static final String TAG = "Server";
    private ServerSocketChannel mServerChannel;
    private Selector mSelector;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    public Server(){
        try {
            mSelector = Selector.open();
            mServerChannel = ServerSocketChannel.open();
            mServerChannel.configureBlocking(false);
            mServerChannel.socket().bind(new InetSocketAddress(8080));
            mServerChannel.register(mSelector, SelectionKey.OP_ACCEPT);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void run() {
        try{
            while (true){
                int select = mSelector.select();
                if(select <= 0){
                    continue;
                }
                Set<SelectionKey> keys = mSelector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()){
                    SelectionKey key = it.next();
                    it.remove();
                    if(key.isValid()){
                        if(key.isAcceptable()){
                            handleAccept();
                            Log.d(TAG,"accepting!!!");
                        }else if(key.isReadable()){
                            handleRead(key);
                            Log.d(TAG,"reading!!!");
                        }else if(key.isWritable()){
                            handleWrite(key);
                            Log.d(TAG,"writing!!!");
                        }
                    }
                }
            }
        }catch (IOException e){
            try {
                stop();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void handleAccept(){
        SocketChannel channel;
        try {
            channel = mServerChannel.accept();
            channel.configureBlocking(false);
            channel.register(mSelector,SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if(channel != null){
            int bytesRead = channel.read(buffer);
            if(bytesRead > 0){
                buffer.flip();
                byte[] packet = buffer.array();
                String message = Utils.bytesToHex(packet);
                Log.d(TAG,"收到来自客户端的消息："+message);
                channel.register(mSelector,SelectionKey.OP_WRITE);
            }
        }
    }

    private void handleWrite(SelectionKey key){
        SocketChannel channel = (SocketChannel) key.channel();
        if(channel != null){
            String sendMsg = "Hello ClientServer";
            buffer.clear();
            buffer.put(buffer.array());
            buffer.flip();
            while (buffer.hasRemaining()){
                try {
                    channel.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            key.cancel();
        }else {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stop() throws IOException {
        if(mSelector != null){
            mSelector.close();
        }

        if(mServerChannel != null){
            mServerChannel.close();
        }
    }
}
