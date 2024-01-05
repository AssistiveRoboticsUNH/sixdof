package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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

import io.github.controlwear.virtual.joystick.android.JoystickView;
import android.media.ExifInterface;
import android.widget.ToggleButton;

import org.json.JSONObject;
public class SixDOFSpinner extends AppCompatActivity implements SensorEventListener, ZMQ_Interface{
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

    int spinner_angle, spinner_strength, spinner_x=50, spinner_y=50;

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

                data[0]="X: " + String.format("%.2f", x)  + "; Y: " +String.format("%.2f", y)+ "; Z: " + String.format("%.2f", z);
                data[1] = String.format(" , x %03d :y %03d",spinner_x,spinner_y);

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

    Button T1,T2;
    ToggleButton toggleButton;

    boolean gripper_status=false;
    CheckBox cb, cbg;

    long event_no=0;

    long cnt=0;

    int setting_fq=100;
    ZMQ_Pub  zmq_publisher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_six_dofspinner);
        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
        connection_mode=getIntent().getStringExtra("mode");
        serverIP=getIntent().getStringExtra("serverIP");
        setting_fq=getIntent().getIntExtra("fq", 100);
        if(setting_fq<10){
            setting_fq=10;
        }
        System.out.println("received setting_fq="+setting_fq);



        display=(TextView) findViewById(R.id.textView2);
        tv_ip=(TextView) findViewById(R.id.textView3);

        localIP=getIP();
        tv_ip.setText("local ip="+localIP);

        connection_mode="server";


        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);

        T1=(Button) findViewById(R.id.button14);
        T2=(Button) findViewById(R.id.button15);
        toggleButton=(ToggleButton) findViewById(R.id.toggleButton);


        cb = (CheckBox) findViewById(R.id.checkBox);
        cbg=(CheckBox) findViewById(R.id.checkBox3);


        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                  @Override
                  public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
//                      String msg=generateMsg();
//                      sayHello(msg);
                  }
              }
        );
        cbg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                          @Override
                                          public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
//                                              String msg=generateMsg();
//                                              sayHello(msg);
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
                    SixDOFSpinner.this.is_pressed = motionEvent.getAction()==MotionEvent.ACTION_DOWN;
                    SixDOFSpinner.this.button = name;
//                    String msg=generateMsg();
//                    sayHello(msg);
                }
                return false;
            }
        };

//        bup.setOnTouchListener(touchListener);
//        bdown.setOnTouchListener(touchListener);
        T1.setOnTouchListener(touchListener);
        T2.setOnTouchListener(touchListener);
//        bgripper.setOnTouchListener(touchListener);

//        bgripper.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                gripper_status=!gripper_status;
//            }
//        });


        final JoystickView jv= (JoystickView) findViewById(R.id.joyv);
        jv.setOnMoveListener(new JoystickView.OnMoveListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onMove(int angle, int strength) {
                int x=jv.getNormalizedX();
                int y=jv.getNormalizedY();
                spinner_angle=angle;
                spinner_strength=strength;
                spinner_x=jv.getNormalizedX();
                spinner_y=jv.getNormalizedY();;

//                String msg=generateMsg();
//                sayHello(msg);
            }
        });



        jv.setOnTouchListener(new View.OnTouchListener() {

            long jv_down, jv_up;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (view instanceof Button) {
                    Toast.makeText(SixDOFSpinner.this, "spinner button onTouch", Toast.LENGTH_SHORT).show();
                }

                if (motionEvent.getAction()==MotionEvent.ACTION_DOWN) {
                    jv_down=System.currentTimeMillis();
//                    Toast.makeText(SixDOFSpinner.this, "spinner onTouch", Toast.LENGTH_SHORT).show();
                    long d=jv_down-jv_up;
                    if(d<200){
//                        Toast.makeText(SixDOFSpinner.this, "soft click", Toast.LENGTH_SHORT).show();
                        SixDOFSpinner.this.is_pressed =true;
                        SixDOFSpinner.this.button = "gripper";

                        gripper_status = !gripper_status;
                    }
                }else if(motionEvent.getAction()==MotionEvent.ACTION_UP){
                    jv_up=System.currentTimeMillis();
                }

                return false;
            }
        });




        new Thread(){
            public void run(){
                zmq_publisher=new ZMQ_Pub(SixDOFSpinner.this, setting_fq);
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


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            zmq_publisher.close();
        }catch (Exception ex){

        }
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
        zmq_publisher.close();
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
            jsr.put("button", this.button);
            jsr.put("pressed", this.is_pressed);
            jsr.put("x", sensor_x);
            jsr.put("y", sensor_y);
            jsr.put("ctrl", cb.isChecked());
            jsr.put("gyro", cbg.isChecked());
//            jsr.put("f", sending_f);
            jsr.put("spinner_angle", spinner_angle);
            jsr.put("spinner_strength", spinner_strength);
            jsr.put("spinner_x", 50-spinner_x);
            jsr.put("spinner_y", 50-spinner_y);
            jsr.put("gripper", gripper_status);
            jsr.put("rec",toggleButton.getText().toString().contains("ON"));
        }catch (Exception ex){

        }
        String toret=this.button+","+this.is_pressed+","+sensor_x+","+sensor_y+","+cb.isChecked()
                +","+cbg.isChecked()+","+spinner_angle+","+spinner_strength+","+(50-spinner_x)
                +","+(50-spinner_y);
//        return toret;

        return jsr.toString();
    }

}