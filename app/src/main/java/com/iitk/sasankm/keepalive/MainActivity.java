package com.iitk.sasankm.keepalive;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    public static Activity activity = null;
    public static Handler mHandler;
    public static TextView connstatus;


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        mHandler = new Handler();
        connstatus = (TextView) findViewById(R.id.textView3);

        
        if(isMyServiceRunning(keepaliveservice.class))
        {

            setstatus(keepaliveservice.status);
            EditText editText = (EditText) findViewById(R.id.username);
            editText.setEnabled(false);
            editText = (EditText) findViewById(R.id.password);
            editText.setEnabled(false);

        }
        else{

            connstatus.setText("Not Running", TextView.BufferType.NORMAL);
            connstatus.setTextColor(Color.RED);
            connstatus.invalidate();
            EditText editText = (EditText) findViewById(R.id.username);
            editText.setEnabled(true);
            editText = (EditText) findViewById(R.id.password);
            editText.setEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();

        connstatus = (TextView) findViewById(R.id.textView3);


        if(isMyServiceRunning(keepaliveservice.class))
        {

            setstatus(keepaliveservice.status);
            EditText editText = (EditText) findViewById(R.id.username);
            editText.setEnabled(false);
            editText = (EditText) findViewById(R.id.password);
            editText.setEnabled(false);

        }
        else{
            connstatus.setText("Not Running", TextView.BufferType.NORMAL);
            connstatus.setTextColor(Color.RED);
            EditText editText = (EditText) findViewById(R.id.username);
            editText.setEnabled(true);
            editText = (EditText) findViewById(R.id.password);
            editText.setEnabled(true);
        }

    }




    public void getURL(View view)
    {
        Intent mServiceIntent = new Intent(this, keepaliveservice.class);
        File u_file = new File(this.getFilesDir(),"username");
        File p_file = new File(this.getFilesDir(),"password");
        BufferedReader br=null ;
        BufferedReader p_br=null;
        EditText editText = (EditText) findViewById(R.id.username), editText1 = (EditText) findViewById(R.id.password);

        try {


             br = new BufferedReader(new FileReader(u_file));
             p_br = new BufferedReader(new FileReader(p_file));

            if(br.readLine().isEmpty() || p_br.readLine().isEmpty())
                throw new IOException();




        }
        catch (IOException s)
        {
            Toast.makeText(this, "Could not read login details", Toast.LENGTH_SHORT).show();
            s.printStackTrace();
            return;
        }
        finally {
            try {
                if (br != null) br.close();
                if (p_br != null) p_br.close();
            }
            catch (IOException f)
            {
                f.printStackTrace();
            }
        }

        editText.setEnabled(false);
        editText1.setEnabled(false);

        this.startService(mServiceIntent);

        setstatus(-1);


    }

    public static void setstatus(int i)
    {

        if(i==2)
        {
            connstatus.setTextColor(Color.GRAY);
            connstatus.setText("Already logged in", TextView.BufferType.NORMAL);
        }
        else if(i==1)
        {
            connstatus.setTextColor(Color.argb(255,0,150,0));
            connstatus.setText("Keeping alive URL: " + keepaliveservice.urlstring, TextView.BufferType.NORMAL);
        }

        else if(i==0) {
            connstatus.setTextColor(Color.MAGENTA);
            connstatus.setText("Disconnected", TextView.BufferType.NORMAL);

        }
        else if(i==-1){
            connstatus.setTextColor(Color.argb(255,0,150,150));
            connstatus.setText("Waiting for status...", TextView.BufferType.NORMAL);

        }
        else if(i==3){

            connstatus.setTextColor(Color.argb(255,0,150,0));
            connstatus.setText("Connected to Cisco Gateway", TextView.BufferType.NORMAL);
        }


        connstatus.invalidate();
    }

    public void stopservice(View view)
    {
        Intent mServiceIntent = new Intent(this, keepaliveservice.class);
        this.stopService(mServiceIntent);
        connstatus.setText("Not Running", TextView.BufferType.NORMAL);
        connstatus.setTextColor(Color.RED);
        connstatus.invalidate();

        EditText editText = (EditText) findViewById(R.id.username);
        editText.setEnabled(true);

        editText = (EditText) findViewById(R.id.password);
        editText.setEnabled(true);

    }

    public void deletelogin(View view){

        File u_file = new File(this.getFilesDir(),"username");
        File p_file = new File(this.getFilesDir(),"password");

        if(u_file.exists())
            u_file.delete();
        if(p_file.exists())
            p_file.delete();

        Toast.makeText(this, "Login details deleted", Toast.LENGTH_SHORT).show();


    }
    public void savelogin(View view)
    {

        File u_file = new File(this.getFilesDir(),"username");
        File p_file = new File(this.getFilesDir(),"password");



        EditText editText = (EditText) findViewById(R.id.username), editText1 = (EditText) findViewById(R.id.password);

        if(editText.getText().toString().isEmpty() || editText1.getText().toString().isEmpty())
        {

            Toast.makeText(this, "Invalid login details", Toast.LENGTH_SHORT).show();


            return;
        }

        try {
            if (!u_file.exists())
                u_file.createNewFile();
            if (!p_file.exists())
                p_file.createNewFile();

            FileWriter fw = new FileWriter(u_file.getAbsoluteFile(), false);
            BufferedWriter bw = new BufferedWriter(fw);
            FileWriter p_fw = new FileWriter(p_file.getAbsoluteFile(), false);
            BufferedWriter p_bw = new BufferedWriter(p_fw);

            bw.write(editText.getText().toString());
            p_bw.write(editText1.getText().toString());

            bw.close();
            p_bw.close();
            fw.close();
            p_fw.close();
            Toast.makeText(this, "Login saved succesfully", Toast.LENGTH_SHORT).show();

        }
        catch (IOException a)
        {
            Toast.makeText(this, "Could not save login", Toast.LENGTH_SHORT).show();
            a.printStackTrace();
        }

    }






}
