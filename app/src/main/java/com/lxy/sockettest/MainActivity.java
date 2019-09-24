package com.lxy.sockettest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity implements Runnable{
    private ClientServer mClientServer;
    private TcpServer mTcpServer;
    private Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start();
    }

    private void start(){
        mThread = new Thread(this);
        mThread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void run() {
        mTcpServer = new TcpServer();
        mTcpServer.start();

        mClientServer = new ClientServer();
        mClientServer.start();
    }
}
