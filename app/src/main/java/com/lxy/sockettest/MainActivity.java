package com.lxy.sockettest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable{
    private static final String TAG = "MainActivity";
    private ClientServer mClientServer;
    private TcpServer mTcpServer;
    private Thread mThread;
    private Client mClient;
    private Server mServer;
    public static List<Client> clients = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //开启server
        new Thread(new Server()).start();
        Thread thread = new Thread(this);
        thread.start();

    }


    //客户端开启一个线程
    @Override
    public void run() {

        //开启三个客户端 建立三个tcp连接
        clients.add(new Client());
        clients.add(new Client());
        clients.add(new Client());

        for (Client cl : clients) {
            new Thread(cl).start();
            Log.d(TAG,"activity");
        }
    }
}
