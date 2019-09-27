package com.lxy.sockettest;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer implements Runnable{
    private static final String TAG = "TcpServer";

    private ServerSocketChannel mServerChannel;
    private Selector mSelector;
    private Thread mServerThread;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private ExecutorService executorService;//线程池
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private int POOL_SIZE = 4;//单个cpu时线程池中的工作线程数目
    @SuppressLint("NewApi")
    private static ConcurrentLinkedDeque<SocketChannel> socketDeque = new ConcurrentLinkedDeque<>();

    @RequiresApi(api = Build.VERSION_CODES.N)
    public TcpServer(){
        try {
            mSelector = Selector.open();
            mServerChannel = ServerSocketChannel.open();
            mServerChannel.socket().bind(new InetSocketAddress(8080));
            mServerChannel.configureBlocking(false);
            mServerChannel.register(mSelector, SelectionKey.OP_ACCEPT);

            executorService = Executors.newFixedThreadPool(CPU_COUNT * POOL_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        mServerThread = new Thread(this);
        mServerThread.start();
    }

    public void service(){
        while (true){
            SocketChannel channel = null;
            if(channel == null){
                continue;
            }else {
                try {
                    channel = (SocketChannel) mServerChannel.accept();
                    channel.configureBlocking(false);
                    executorService.execute(new Handler(channel));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        try{
            while (true){
                int select = mSelector.select();
                if(select < 0){
                    continue;
                }
                Iterator<SelectionKey> it = mSelector.selectedKeys().iterator();
                while (it.hasNext()){
                    SelectionKey key = it.next();
                    if(key.isValid()){
                        if(key.isAcceptable()){
                            SocketChannel channel = mServerChannel.accept();
                            channel.configureBlocking(false);
                            executorService.execute(new Handler(channel));
                        }
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    class Handler extends Thread{
        private SocketChannel channel;

        public Handler(SocketChannel channel){
            this.channel = channel;
        }

        @Override
        public void run() {
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
                                key.cancel();
                                Log.d(TAG,"reading!!!");
                            }else if(key.isWritable()){
                                handleWrite(key);
                                key.cancel();
                                Log.d(TAG,"writing!!!");
                            }
                        }
                    }
                }
            }catch (IOException e){
                stop();
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
