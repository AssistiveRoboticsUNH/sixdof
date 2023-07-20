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

    EditText et;
    String KEY="serverIP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_home);
        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }

        et=(EditText) findViewById(R.id.editTextText);

        et.setText(getValue());
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
    public void button_connect_clicked(View v){
        String serverIP=et.getText().toString();

//        Toast.makeText(this, "connecting ...\n"+serverIP, Toast.LENGTH_SHORT).show();

        saveFromEditText(serverIP);
        Intent intent=new Intent(home.this, SixDOF.class);
        intent.putExtra("serverIP", serverIP);
        startActivity(intent);

    }


}