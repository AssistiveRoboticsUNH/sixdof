package com.example.myapplication;

import static android.hardware.Sensor.TYPE_GYROSCOPE;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

public class SixDOF extends AppCompatActivity implements SensorEventListener {
    SensorManager mSensorManager=null;
    Sensor mAccelerometer;
    Sensor mGyroscope;



    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        net_connect();
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

        net_close();
    }
    String[] data= {"hello", "world"};
    public void onSensorChanged(SensorEvent sensorEvent) {
        String sensorName = sensorEvent.sensor.getName();
//        String txt=sensorName + ": X: " + sensorEvent.values[0] + "; Y: " + sensorEvent.values[1] + "; Z: " + sensorEvent.values[2] + ";";

        float x=sensorEvent.values[0];
        float y=sensorEvent.values[1];
        float z=sensorEvent.values[2];

        if(sensorName.contains("Gyroscope")) {
            data[0]="Gyro: " + ": X: " + String.format("%.4f", x)  + "; Y: " +String.format("%.4f", y)+ "; Z: " + String.format("%.4f", z);
        }else{
            data[1]="Acc: " + ": X: " + String.format("%.4f", x)  + "; Y: " + String.format("%.4f", y)+ "; Z: " + String.format("%.4f", z);
        }

        String txt=data[0]+"\n"+data[1];
        Log.d(TAG, txt);
        show_txt(txt);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public static String TAG=SixDOF.class.getSimpleName();

    String serverIP=null;
    TextView tv=null;
    boolean isconnected=false;

    Socket client;
    PrintWriter writer;

    TextView display;

    Button bxp, bxm, byp, bym;

    CheckBox cb;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_six_dof);
        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
        serverIP=getIntent().getStringExtra("serverIP");
//        Toast.makeText(this, "connecting ...\n"+serverIP, Toast.LENGTH_SHORT).show();

        tv=(TextView) findViewById(R.id.textView);
        display=(TextView) findViewById(R.id.textView2);


//        net_connect();


        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);

//        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_POSE_6DOF);

        bxp=(Button) findViewById(R.id.button8);
        bxm=(Button) findViewById(R.id.button9);
        byp=(Button) findViewById(R.id.button6);
        bym=(Button) findViewById(R.id.button7);

        cb = (CheckBox) findViewById(R.id.checkBox);

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                Button b=(Button)view;
                String name=b.getText().toString();
                doSend(name);

                return false;
            }
        };

        bxp.setOnTouchListener(touchListener);
        bxm.setOnTouchListener(touchListener);
        byp.setOnTouchListener(touchListener);
        bym.setOnTouchListener(touchListener);
    }



    public void net_connect(){
        new Thread(){
            public void run(){
                try{
                    client=new Socket(serverIP, 8080);
                    writer=new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                    isconnected=true;
                }catch (Exception ex){
                    ex.printStackTrace();
                    isconnected=false;
                }
//                Toast.makeText(SixDOF.this, "connecting status="+isconnected, Toast.LENGTH_SHORT).show();
                show_connection_status();
            }
        }.start();
    }
    public void net_close(){
        try{
            client.close();
        }catch (Exception ex){

        }
    }

    public void show_connection_status(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText("connection:"+isconnected);
            }
        });
    }

    public void show_txt(final String txt){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                display.setText(txt);
            }
        });
    }


    public void clicked_up(View v){
        doSend("up");
    }
    public void clicked_down(View v){
        doSend("down");
    }
    public void clicked_open(View v){
        doSend("open");
    }
    public void clicked_close(View v){
        doSend("close");
    }

    public void clicked_xplus(View v){
//        doSend("x+");
    }
    public void clicked_xminus(View v){
//        doSend("x-");
    }
    public void clicked_yplus(View v){
//        doSend("y+");
    }
    public void clicked_yminus(View v){
//        doSend("y-");
    }

    public void doSend(final String msg){

        if(!cb.isChecked()){
            return;
        }

        if(!isconnected){
            return;
        }

        new Thread(){
            public void run(){
                try{
                    writer.println(msg);
                    writer.flush();
                }catch (Exception ex){
                    isconnected=false;
                    show_connection_status();
                    ex.printStackTrace();
                }
            }
        }.start();

    }



}