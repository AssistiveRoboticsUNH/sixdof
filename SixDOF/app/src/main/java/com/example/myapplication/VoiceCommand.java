package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class VoiceCommand extends AppCompatActivity {

    public static final String TAG=VoiceCommand.class.getSimpleName();
    TextView tv_ip, tv_console;
    String localIP;

    Button button_speech;

    private SpeechRecognizer speech = null;
    boolean performingSpeechSetup = true;
    private Intent recognizerIntent;

    boolean listening_button_status=false;

    int setting_fq=50;
    ZMQ_Pub  zmq_publisher;

    String msg_to_send="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_command);

        tv_ip=(TextView) findViewById(R.id.textView5);
        tv_console=(TextView) findViewById(R.id.textView7);

        button_speech=(Button) findViewById(R.id.button13);
        button_speech.setText("start listening");

        localIP=getIP();
        tv_ip.setText("local ip="+localIP);


        requestRecordAudioPermission();
        speech_init();

        new Thread(){
            public void run(){
                zmq_publisher=new ZMQ_Pub(null, setting_fq);
//                zmq_publisher.start();
            }
        }.start();
    }

    void stop_listening() {
        listening_button_status=false;
        System.out.println("dostop request");
        speech.stopListening();
    }
    void start_listening() {
        System.out.println("dostart request");


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    speech.startListening(recognizerIntent);
                }catch (Exception ex) {
//            et.setText("Can't start recog");
                    ex.printStackTrace();
                }
            }
        });

    }


    private static final int SPEECH_REQUEST_CODE = 0;

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
// This starts the activity and populates the intent with the speech text.
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            addMsg(spokenText);

            setMsg(spokenText);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void button_recog_once_clicked(View v){
        showMsg("Recognizing speech ...");
        displaySpeechRecognizer();
    }
    public void button_recog_cont_clicked(View v){
//        Toast.makeText(this, "not implemented yet", Toast.LENGTH_SHORT).show();
        if(button_speech.getText().toString().equals("start listening")) {
            button_speech.setText("stop listening");
            button_speech.setBackgroundColor(Color.RED);
            start_listening();
            listening_button_status=true;
        }else {
            button_speech.setText("start listening");
            stop_listening();
            listening_button_status=false;
            button_speech.setBackgroundColor(Color.GREEN);
        }

    }


    @Override
    protected void onDestroy() {

        try {
            zmq_publisher.close();
        }catch (Exception ex){

        }
        stop_listening();

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        stop_listening();
        super.onPause();
    }


    private String getIP(){
        Context context = this.getApplicationContext();
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    public void showMsg(String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_console.setText(msg);
            }
        });
    }
    /*
    add as a new line.
     */
    public void addMsg(String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                tv_console.append("\n"+msg);
                tv_console.setText(msg);
            }
        });
    }

    public void setMsg(String msg){
        JSONObject jsr = new JSONObject();
        try {
            jsr.put("mode", "voice");
            jsr.put("speech", msg);
            msg=jsr.toString();
        }catch (Exception ex){}

        zmq_publisher.publish(msg);
    }

    private void requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String requiredPermission = Manifest.permission.RECORD_AUDIO;

            // If the user previously denied this permission then show a message explaining why
            // this permission is needed
            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{requiredPermission}, 101);
            }
        }
    }
    void speech_init() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(new listener());
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    /*
    call  from RecognitionListener
     */
    private void onRecog(boolean success, String spokenText){

        addMsg(spokenText);
        if(success){
            setMsg(spokenText);
        }

        if(listening_button_status==true) {
            //start again
            start_listening();
        }
    }

    class listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "onReadyForSpeech");
            performingSpeechSetup = false;
        }
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginningOfSpeech");
//            addMsg("[");
        }
        public void onRmsChanged(float rmsdB)
        {
            Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndofSpeech");
//            addMsg("]");
        }

        public void onError(int error)
        {
            Log.d(TAG,  "error " +  error);
            if (performingSpeechSetup && error == SpeechRecognizer.ERROR_NO_MATCH) return;
//            addMsg("error="+error);
//            doSend("_recog_error_", null);
            onRecog(false, "error="+error);
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            Log.d(TAG, "onResults " + results);
            String resp="_none_";
            try {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = "";
                for (String result : matches)
                    text += result + "::";

                Log.i(TAG, text);
                String q=matches.get(0);
//                doSend(q, matches);
//                addMsg(q);
                onRecog(true, q);

            }catch(Exception ex) {
                ex.printStackTrace();
            }

        }
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
        }
    }

}