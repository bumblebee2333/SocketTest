package com.lxy.sockettest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolServer {
    private int port = 8000;
    private ServerSocket serverSocket;
    private ExecutorService executorService; //线程池
    private final int POOL_SIZE = 4; //单个CPU时线程池中的工作线程数目

    public ThreadPoolServer() throws IOException{
        serverSocket = new ServerSocket(port);
        //创建线程池
        //Runtime 的availableProcessors()方法返回当前系统CPU的数目
        //系统CPU越多，线程池中的工作线程数目越多
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*POOL_SIZE);
        System.out.println("服务器已启动！！");
    }

    public void service(){
        while(true){
            Socket socket = null;
            try{
                socket = serverSocket.accept();
                executorService.execute(new Handler(socket));
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            new ThreadPoolServer().service();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

    class Handler implements Runnable {
        private Socket socket;
        public Handler(Socket socket) {
            this.socket = socket;
        }
        private PrintWriter getWriter(Socket socket) throws IOException {
            OutputStream socketOut = socket.getOutputStream();
            return new PrintWriter(socketOut, true);
        }
        private BufferedReader getReader(Socket socket) throws IOException {
            InputStream socketIn = socket.getInputStream();
            return new BufferedReader(new InputStreamReader(socketIn));
        }
        public String echo(String msg) {
            return "echo:" + msg;
        }
        @Override
        public void run() {
            try {
                System.out.println("New connection accepted:" + socket.getInetAddress() + ":" + socket.getPort());
                BufferedReader br = getReader(socket);
                PrintWriter pw = getWriter(socket);
                String msg = null;
                while ((msg = br.readLine()) != null) {
                    System.out.println(msg);
                    pw.println(echo(msg));
                    if (msg.equals("bye")) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
}
