package com.example.myapplication;

import static android.hardware.Sensor.TYPE_GYROSCOPE;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;


import android.media.ExifInterface;
import android.widget.ToggleButton;

import org.json.JSONObject;

public class SixDOF extends AppCompatActivity implements SensorEventListener, ZMQ_Interface{
    SensorManager mSensorManager=null;
    Sensor mAccelerometer;
    Sensor mGyroscope;

    String[] data= {"hello", "world"};

    private long lastTimestamp = 0;
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private float[] translation = {0, 0, 0};

    int send_freq=50;

    float sensor_x=0, sensor_y=0;
    String button_name="";
    boolean button_pressed=false;

    // Complementary filter alpha value (adjust as needed for filtering)
    private static final float ALPHA = 0.1f;
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            // Get the accelerometer values and update the rotation matrix
            System.arraycopy(event.values, 0, orientationAngles, 0, 3);
        } else if (event.sensor == mGyroscope) {
            if (lastTimestamp != 0) {
                // Calculate the rotation angles using gyroscope data
                float dt = (event.timestamp - lastTimestamp) * 1.0e-9f; // Convert timestamp to seconds
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
                if (omegaMagnitude > 1e-5) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                float thetaOverTwo = omegaMagnitude * dt / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                float deltaRotationX = sinThetaOverTwo * axisX;
                float deltaRotationY = sinThetaOverTwo * axisY;
                float deltaRotationZ = sinThetaOverTwo * axisZ;
                float deltaRotationW = cosThetaOverTwo;

                float[] deltaRotationMatrix = new float[9];
                deltaRotationMatrix[0] = 1 - 2 * deltaRotationY * deltaRotationY - 2 * deltaRotationZ * deltaRotationZ;
                deltaRotationMatrix[1] = 2 * deltaRotationX * deltaRotationY - 2 * deltaRotationW * deltaRotationZ;
                deltaRotationMatrix[2] = 2 * deltaRotationX * deltaRotationZ + 2 * deltaRotationW * deltaRotationY;
                deltaRotationMatrix[3] = 2 * deltaRotationX * deltaRotationY + 2 * deltaRotationW * deltaRotationZ;
                deltaRotationMatrix[4] = 1 - 2 * deltaRotationX * deltaRotationX - 2 * deltaRotationZ * deltaRotationZ;
                deltaRotationMatrix[5] = 2 * deltaRotationY * deltaRotationZ - 2 * deltaRotationW * deltaRotationX;
                deltaRotationMatrix[6] = 2 * deltaRotationX * deltaRotationZ - 2 * deltaRotationW * deltaRotationY;
                deltaRotationMatrix[7] = 2 * deltaRotationY * deltaRotationZ + 2 * deltaRotationW * deltaRotationX;
                deltaRotationMatrix[8] = 1 - 2 * deltaRotationX * deltaRotationX - 2 * deltaRotationY * deltaRotationY;

                // Update the rotation matrix using a complementary filter
                for (int i = 0; i < 9; i++) {
                    rotationMatrix[i] = ALPHA * deltaRotationMatrix[i] + (1 - ALPHA) * rotationMatrix[i];
                }

                // Update the translation using the rotation matrix and accelerometer data
                float x = rotationMatrix[0] * orientationAngles[0] + rotationMatrix[1] * orientationAngles[1] + rotationMatrix[2] * orientationAngles[2];
                float y = rotationMatrix[3] * orientationAngles[0] + rotationMatrix[4] * orientationAngles[1] + rotationMatrix[5] * orientationAngles[2];
                float z = rotationMatrix[6] * orientationAngles[0] + rotationMatrix[7] * orientationAngles[1] + rotationMatrix[8] * orientationAngles[2];

                // Update the translation values
                translation[0] += x * dt;
                translation[1] += y * dt;
                translation[2] += z * dt;

//                float x=translation[0], y=translation[1], z=translation[2];
                data[0]="X: " + String.format("%.2f", x)  + "; Y: " +String.format("%.2f", y)+ "; Z: " + String.format("%.2f", z);

                String txt=data[0]+"\n"+data[1];
                show_txt(txt);

                sensor_x=y;
                sensor_y=x;
            }
            lastTimestamp = event.timestamp;
        }
    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public static String TAG=SixDOF.class.getSimpleName();

    String connection_mode=null;  //server or client
    String serverIP=null;
    String localIP=null;



    TextView display;
    TextView tv_ip;

    Button bxp, bxm, byp, bym, bgripper;
    Button bup, bdown;
    Button T1,T2;

    CheckBox cb, cbg;
    long event_no=0;

    int setting_fq=100;
    ZMQ_Pub  zmq_publisher;
    ToggleButton toggleButton;
    boolean gripper_status=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_six_dof);
        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
        connection_mode=getIntent().getStringExtra("mode");
        serverIP=getIntent().getStringExtra("serverIP");
        setting_fq=getIntent().getIntExtra("fq", 100);
        if(setting_fq<1){
            setting_fq=1;
        }
        System.out.println("received setting_fq="+setting_fq);

//        Toast.makeText(this, "connecting ...\n"+serverIP, Toast.LENGTH_SHORT).show();

        display=(TextView) findViewById(R.id.textView2);
        tv_ip=(TextView) findViewById(R.id.textView3);

        localIP=getIP();
        tv_ip.setText("local ip="+localIP);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);

        bup=(Button) findViewById(R.id.button2);
        bdown=(Button) findViewById(R.id.button3);

        bxp=(Button) findViewById(R.id.button8);
        bxm=(Button) findViewById(R.id.button9);
        byp=(Button) findViewById(R.id.button6);
        bym=(Button) findViewById(R.id.button7);
        T1=(Button) findViewById(R.id.button14);
        T2=(Button) findViewById(R.id.button15);
        bgripper=(Button) findViewById(R.id.button5);
        toggleButton=(ToggleButton) findViewById(R.id.toggleButton2);


        cb = (CheckBox) findViewById(R.id.checkBox);
        cbg=(CheckBox) findViewById(R.id.checkBox3);


        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

               @Override
               public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {

               }
           }
        );


        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Button b=(Button)view;
                String name=b.getText().toString();

                ++event_no;
                button_name=name;

//                cnt++;
//                Log.i(TAG, "ontouch="+name+" "+cnt+" "+motionEvent.getAction());

                boolean pressed=true; //down and move
                if (motionEvent.getAction()==MotionEvent.ACTION_DOWN) {
                    button_pressed=true;
                }else if(motionEvent.getAction()==MotionEvent.ACTION_UP) {
                    button_pressed=false;
                }

                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN || motionEvent.getAction()==MotionEvent.ACTION_UP) {
                    SixDOF.this.is_pressed = motionEvent.getAction()==MotionEvent.ACTION_DOWN;
                    SixDOF.this.button = name;
                }
                return false;
            }
        };

        bup.setOnTouchListener(touchListener);
        bdown.setOnTouchListener(touchListener);
        bxp.setOnTouchListener(touchListener);
        bxm.setOnTouchListener(touchListener);
        byp.setOnTouchListener(touchListener);
        bym.setOnTouchListener(touchListener);
        T1.setOnTouchListener(touchListener);
        T2.setOnTouchListener(touchListener);
//        bgripper.setOnTouchListener(touchListener);

        bgripper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gripper_status = !gripper_status;
            }
        });

        new Thread(){
            public void run(){
                zmq_publisher=new ZMQ_Pub(SixDOF.this, setting_fq);
                zmq_publisher.start();
            }
        }.start();

    }

    private String getIP(){
        Context context = this.getApplicationContext();
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }


    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            zmq_publisher.close();
        }catch (Exception ex){

        }
    }


    public void show_txt(final String txt){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                display.setText(txt);
            }
        });
    }



    String button="";
    boolean is_pressed;
    public String generate_zmq_pub_msg(){
        JSONObject jsr = new JSONObject();
        try {
            jsr.put("mode", "button");
            jsr.put("button", this.button);
            jsr.put("pressed", this.is_pressed);
            jsr.put("x", sensor_x);
            jsr.put("y", sensor_y);
            jsr.put("ctrl", cb.isChecked());
            jsr.put("gyro", cbg.isChecked());
//            jsr.put("f", sending_f);
//            jsr.put("spinner_angle", spinner_angle);
//            jsr.put("spinner_strength", spinner_strength);
//            jsr.put("spinner_x", 50-spinner_x);
//            jsr.put("spinner_y", 50-spinner_y);
            jsr.put("gripper", gripper_status);
            jsr.put("rec",toggleButton.getText().toString().contains("ON"));
        }catch (Exception ex){

        }
//        String toret=this.button+","+this.is_pressed+","+sensor_x+","+sensor_y+","+cb.isChecked()
//                +","+cbg.isChecked()+","+spinner_angle+","+spinner_strength+","+(50-spinner_x)
//                +","+(50-spinner_y);
////        return toret;

        return jsr.toString();
    }



}