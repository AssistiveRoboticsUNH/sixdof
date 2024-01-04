package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class home extends AppCompatActivity {


    EditText etf;
    String KEY="serverIP";
    String KEY_F="FQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_home);
        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }


        etf= (EditText) findViewById(R.id.editTextNumber);

        etf.setText(""+getFq());
    }
    private int getFq() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int savedValue = sharedPref.getInt(KEY_F, 100);

        return savedValue;
    }
    private String getValue() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String savedValue = sharedPref.getString(KEY, "192.168.0.10"); //the 2 argument return default value

        return savedValue;
    }

    private void saveFromEditText(String text) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY, text);
        editor.apply();
    }
    private void saveFqFromEditText(int fq) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_F, fq);
        editor.apply();
    }
    public void button_connect_clicked(View v){

        int fq=Integer.parseInt(etf.getText().toString());
        saveFqFromEditText(fq);

        Intent intent=new Intent(home.this, SixDOFSpinner.class);
        intent.putExtra("fq", fq);
        startActivity(intent);

    }

    public void button_server_clicked(View v){
//        String serverIP=et.getText().toString();
        String serverIP="";
        int fq=Integer.parseInt(etf.getText().toString());
//        Toast.makeText(this, "fq="+fq, Toast.LENGTH_LONG).show();
        saveFromEditText(serverIP);
        saveFqFromEditText(fq);

        Intent intent=new Intent(home.this, SixDOF.class);
        intent.putExtra("serverIP", serverIP);
        intent.putExtra("fq", fq);
        intent.putExtra("mode", "server");
        startActivity(intent);
    }

    public void button_voice_clicked(View v){
        Intent intent=new Intent(home.this, VoiceCommand.class);
        startActivity(intent);
    }
    public void button_6dof_clicked(View v){
//        Intent intent=new Intent(home.this, SFusion.class);
        Intent intent=new Intent(home.this, SixDOFSpinner.class);
        startActivity(intent);
    }
}