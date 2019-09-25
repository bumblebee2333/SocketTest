package com.lxy.sockettest;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ClientServer implements Runnable{
    private static final String TAG = "ClientServer";
    private SocketChannel mSocketChannel;
    private Selector mSelector;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private Thread mClientThread;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ClientServer(){
        try {
            if(mSelector == null){
                mSelector = Selector.open();
            }
            createSocketChannel();
            createSocketChannel();
//            mSocketChannel = SocketChannel.open();
//            mSocketChannel.configureBlocking(false);
//            mSocketChannel.connect(new InetSocketAddress("127.0.0.1",8080));
//            mSocketChannel.register(mSelector,SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SocketChannel createSocketChannel() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("127.0.0.1",8080));
        socketChannel.register(mSelector,SelectionKey.OP_CONNECT);
        return socketChannel;
    }

    public void start(){
        mClientThread = new Thread(this);
        mClientThread.setName("ClientServerThread");
        mClientThread.start();
    }

    @Override
    public void run() {
        try{
            while (true){
                //mSelector.wakeup();
                int select = mSelector.select();
                if(select < 0){
                    continue;
                }
                Set<SelectionKey> keys = mSelector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()){
                    SelectionKey key = it.next();
                    if(key.isValid()){
                        if(key.isConnectable()){
                            connect(key);
                            Log.d(TAG,"connecting!!!");
                        }else if(key.isReadable()){
                            handleRead(key);
                            Log.d(TAG,"reading!!!");
                        }else if(key.isWritable()){
                            handleWrite(key);
                            Log.d(TAG,"writing!!!");
                        }
                    }
                    it.remove();
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

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if(channel != null){
            if(channel.isConnectionPending()){
                channel.finishConnect();
                channel.register(mSelector,SelectionKey.OP_WRITE);
            }
            if(channel.isBlocking()){
                channel.configureBlocking(false);
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if(channel != null && channel.isConnected()){
            buffer.clear();
            int bytesRead = channel.read(buffer);
            if(bytesRead > 0){
                buffer.flip();
                String message = Utils.bytesToHex(buffer.array());
                Log.d(TAG,"收到服务器的来信："+message);
            }
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if(channel != null && channel.isConnected()){
            String sendMessage = "Hello TcpServer";
            byte[] packet = sendMessage.getBytes();
            buffer.clear();
            channel.write(ByteBuffer.wrap(packet));
            channel.register(mSelector,SelectionKey.OP_READ);
        }
    }

    private void stop() throws IOException {
        if(mSelector != null){
            mSelector.close();
        }

        if(mSocketChannel != null){
            mSocketChannel.close();
        }
    }
}
