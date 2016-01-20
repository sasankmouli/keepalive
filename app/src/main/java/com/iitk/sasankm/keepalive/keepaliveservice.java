package com.iitk.sasankm.keepalive;

import android.app.Service;
import android.app.NotificationManager;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class keepaliveservice extends Service {



   // public boolean stop=false;

    private static long INTERVAL_LONG = 180*1000;
    private static long INTERVAL_SHORT = 5*1000;

    public static int status  = -1;

    private static long UPDATE_INTERVAL = INTERVAL_SHORT;  //default

    private static Timer timer ;
    private final String USER_AGENT = "Mozilla/5.0";

     private File file ;
    private File t_file ;
   private  BufferedReader br = null;
    private  BufferedReader t_br = null;
    private FileWriter fw=null;
    private FileWriter t_fw=null;
    private BufferedWriter bw=null;
    private BufferedWriter t_bw=null;

    private Intent wIntent;
    String username;
    String password;



    InputStream in = null;
    byte[] b = new byte[21000];

    URL url = null;
    public static String urlstring = null;

    int mId = 2365;
    @Override
    public void onCreate(){
        super.onCreate();
        _startService();

    }
    private void _startService()
    {
        timer = new Timer();
        timer.scheduleAtFixedRate(

                new TimerTask() {

                    public void run() {

                        Refresh();

                    }
                }, 1000, UPDATE_INTERVAL);
        //Log.d("Keepalive", "Timer started....");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public int onStartCommand(Intent workIntent, int flags, int startId) {
        // Gets data from the incoming Intent

        //Log.d("Keepalive", "Service started");

        File u_file = new File(this.getFilesDir(),"username");
        File p_file = new File(this.getFilesDir(),"password");


        try {


            BufferedReader u_br = new BufferedReader(new FileReader(u_file));
            BufferedReader p_br = new BufferedReader(new FileReader(p_file));

            username = u_br.readLine();
            password = p_br.readLine();

            u_br.close();
            p_br.close();

        }
        catch (IOException s)
        {
            s.printStackTrace();

        }


        return START_STICKY;
    }

    public void Refresh(){


        String dataString = "http://www.google.com";
        String tString;
        HttpURLConnection urlConnection = null;
        String inputLine;
        StringBuffer response;
        BufferedReader ini;
        Pattern magic;
        Matcher m;

        Authenticator.setDefault (new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });


        //Log.d("Keepalive", "Entered main");


        try {
            url = new URL(dataString);
        }
        catch(MalformedURLException mlf)
        {
            mlf.printStackTrace();
        }
        file = new File(this.getFilesDir(),"url.txt");
        t_file = new File(this.getFilesDir(),"time.txt");


        try {


            if(!file.exists() || !t_file.exists()) {

                //Log.d("Keepalive", "URL files don't exist");
                throw new NullPointerException();

            }

            br = new BufferedReader(new FileReader(file));
            t_br = new BufferedReader(new FileReader(t_file));
        //    fw = new FileWriter(file.getAbsoluteFile(), false);
          //  bw = new BufferedWriter(fw);
          //  t_fw = new FileWriter(t_file.getAbsoluteFile(), false);
          //  t_bw = new BufferedWriter(t_fw);

            dataString = br.readLine();
            tString = t_br.readLine();
            urlstring = dataString;

            br.close();
            t_br.close();

            //Log.d("Keepalive", "TimeStamp: " + tString);

           try{
                Long.parseLong(tString);
            }
            catch(NumberFormatException asdas)
            {
                //Log.d("Keepalive", "Bad timestamp: " + tString);
                t_file.delete();
                asdas.printStackTrace();
                Refresh();
                return;
            }

            if(SystemClock.elapsedRealtime() > Long.parseLong(tString)+1800*1000)
            {
                //Log.d("Keepalive", "URL too old");
                file.delete();
                t_file.delete();
                Refresh();
                return;
            }


            try {
                url = new URL(dataString);
                //Log.d("Keepalive", "URL: "+dataString);
            } catch (MalformedURLException | NullPointerException e) {

                // stopForeground(true);
                //Log.d("Keepalive", "Bad URL: "+ dataString);

                file.delete();
                Refresh();
                return;
            }

            try {

                //Log.d("Keepalive", "Attempting to refresh ... ");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", USER_AGENT);
                urlConnection.setConnectTimeout(5000);
                urlConnection.connect();
            }
            catch(java.net.SocketTimeoutException ste){
                //Log.d("Keepalive", "URL Timed out, will try switching to Ironport");
                file.delete();
                t_file.delete();
                Refresh();
                ste.printStackTrace();
                return;
            }
            catch(IOException a) {

                //Log.d("Keepalive","No connection");
                if(status !=0) {
                    status = 0;
                    try {
                        MainActivity.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.setstatus(0);

                            }
                        });
                    } catch (NullPointerException h) {
                        h.printStackTrace();
                    }

                        //changeTimer(INTERVAL_SHORT);
                }



                if(urlConnection!=null)
                    urlConnection.disconnect();
                return;

            }
            try {



               // in = new BufferedInputStream(urlConnection.getInputStream());

                 //in.read(b,0,2000);
                //Log.d("Keepalive", "Refresh Successful at " + Long.toString(SystemClock.elapsedRealtime()));
               // in.close();

                if(urlConnection.getResponseCode()!=200)
                    throw new IOException();

                if(status!=1) {

                    status = 1;


                    //if (UPDATE_INTERVAL == INTERVAL_SHORT)
                        //changeTimer(INTERVAL_LONG);


                    try {
                        MainActivity.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.setstatus(1);

                            }
                        });
                    } catch (NullPointerException h) {
                        h.printStackTrace();
                    }
                }

            }
            catch(IOException a) {
                //Log.d("Keepalive", "Keepalive URL is dead");

                if(status!=0) {
                    status = 0;
                    //if (UPDATE_INTERVAL == INTERVAL_LONG)
                        //changeTimer(INTERVAL_SHORT);

                    try {
                        MainActivity.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.setstatus(0);

                            }
                        });
                    } catch (NullPointerException h) {
                        h.printStackTrace();
                    }
                }

                    file.delete();
                    t_file.delete();
                    if (urlConnection != null)
                        urlConnection.disconnect();
                    Refresh();
                    return;


            }finally {
                if(urlConnection!=null)
                    urlConnection.disconnect();

            }

               try{

                t_fw = new FileWriter(t_file.getAbsoluteFile(), false);
                t_bw = new BufferedWriter(t_fw);


                t_bw.write(Long.toString(SystemClock.elapsedRealtime()));

                t_bw.close();
                t_fw.close();


            }
            catch(IOException | NullPointerException ee)
            {
                //Log.d("Keepalive", "File I/O Exception");
            }


        } catch (IOException | NullPointerException e) {
            //Log.d("Keepalive", "File not found");

            try {

                int code;

                try{
                //Log.d("Keepalive", "Attempting to connect ... ");
                    url = new URL("http://google.com");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(5000);
                    urlConnection.setReadTimeout(5000);
                    //Log.d("Keepalive","About to Connect");
                urlConnection.connect();
                 code = urlConnection.getResponseCode();
                    //Log.d("Keepalive","Status Code"+Integer.toString(code));
                urlConnection.disconnect();
            }
                catch(java.net.SocketTimeoutException ste){
                    //Log.d("Keepalive", "URL Timed out, will try switching to Fortinet");

                    Refresh();
                    ste.printStackTrace();
                    return;
                }

                if(code!=303 && code!=307) {

                    //Log.d("Keepalive",Long.toString(SystemClock.elapsedRealtime()) +  ": Already Logged In");


                    try {

                        url = new URL("http://authenticate.iitk.ac.in/netaccess/connstatus.html");
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.setConnectTimeout(5000);
                        urlConnection.connect();

                         ini = new BufferedReader(
                                new InputStreamReader(urlConnection.getInputStream()));

                         response = new StringBuffer();

                        while ((inputLine = ini.readLine()) != null) {
                            response.append(inputLine);
                        }
                        ini.close();


                         magic = Pattern.compile("You are logged in.");
                         m = magic.matcher(response.toString());
                        if (m.find()) {

                            //Log.d("Keepalive", "Logged in to Cisco");
                            urlConnection.disconnect();

                            if(status!=3) {

                                status = 3;

                                //if (UPDATE_INTERVAL == INTERVAL_LONG)
                                    //changeTimer(INTERVAL_SHORT);


                                try {
                                    MainActivity.mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainActivity.setstatus(3);

                                        }
                                    });
                                } catch (NullPointerException h) {
                                    h.printStackTrace();
                                }

                            }


                            return;

                        }

                    }
                    catch(IOException a) {
                        //Log.d("Keepalive","Could not reach Cisco server");

                        try {



                            if(!t_file.exists())
                                t_file.createNewFile();

                            // t_file.setReadable(true,false);
                            // t_file.setWritable(true, false);

                            t_fw = new FileWriter(t_file);
                            t_bw = new BufferedWriter(t_fw);


                            t_bw.write(Long.toString(SystemClock.elapsedRealtime()));
                            t_bw.flush();
                            t_bw.close();

                            if(status!=2) {

                                status = 2;

                                //if (UPDATE_INTERVAL == INTERVAL_LONG)
                                    //changeTimer(INTERVAL_SHORT);


                                try {
                                    MainActivity.mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainActivity.setstatus(2);

                                        }
                                    });
                                } catch (NullPointerException h) {
                                    h.printStackTrace();
                                }

                            }
                            // file.setReadable(true,false);
                            // file.setWritable(true,false);




                        }
                        catch(IOException ggl)
                        {

                            ggl.printStackTrace();
                        }

                        return;

                    }

                    try{


                        //--------------//


                        url = new URL("http://authenticate.iitk.ac.in/netaccess/connstatus.html");
                        urlConnection  = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("POST");
                        urlConnection.setRequestProperty("Content-Type",
                                "application/x-www-form-urlencoded");
                        urlConnection.setRequestProperty("Accept",
                                "text/plain");

                        String urlParameters =
                                "login=" + URLEncoder.encode("Log In Now","UTF-8") +
                                        "&sid=0" ;

                        urlConnection.setRequestProperty("Content-Length", "" +
                                Integer.toString(urlParameters.getBytes().length));
                        urlConnection.setRequestProperty("Content-Language", "en-US");

                        urlConnection.setUseCaches(false);
                        urlConnection.setDoInput(true);
                        urlConnection.setDoOutput(true);

                        urlConnection.connect();


                        BufferedOutputStream wr = new BufferedOutputStream (
                                urlConnection.getOutputStream ());
                        wr.write(urlParameters.getBytes("UTF-8"), 0, urlParameters.length());
                        wr.flush();
                        wr.close();



                         ini = new BufferedReader(
                                new InputStreamReader(urlConnection.getInputStream()));
                         response = new StringBuffer();

                        while ((inputLine = ini.readLine()) != null) {
                            response.append(inputLine);
                        }
                        ini.close();

                     urlConnection.disconnect();


                        //-------//

                        url = new URL("http://authenticate.iitk.ac.in/netaccess/loginuser.html");
                        urlConnection  = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("POST");
                        urlConnection.setRequestProperty("Content-Type",
                                "application/x-www-form-urlencoded");
                        urlConnection.setRequestProperty("Accept",
                                "text/plain");

                         urlParameters =
                                "username=" + URLEncoder.encode(username,"UTF-8") +
                                        "&password=" + URLEncoder.encode(password,"UTF-8")+"&sid=0" ;

                        urlConnection.setRequestProperty("Content-Length", "" +
                                Integer.toString(urlParameters.getBytes().length));
                        urlConnection.setRequestProperty("Content-Language", "en-US");

                        urlConnection.setUseCaches(false);
                        urlConnection.setDoInput(true);
                        urlConnection.setDoOutput(true);

                        urlConnection.connect();


                         wr = new BufferedOutputStream (
                                urlConnection.getOutputStream ());
                        wr.write(urlParameters.getBytes("UTF-8"), 0, urlParameters.length());
                        wr.flush();
                        wr.close();



                         ini = new BufferedReader(
                                new InputStreamReader(urlConnection.getInputStream()));

                         response = new StringBuffer();

                        while ((inputLine = ini.readLine()) != null) {
                            response.append(inputLine);
                        }
                        ini.close();


                        String str = response.toString();

                        //Log.d("Keepalive", "Server Response: " + str);

                        urlConnection.disconnect();

                    /*    url = new URL("http://authenticate.iitk.ac.in/netaccess/connstatus.html");

                        urlConnection  = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.connect();

                        urlConnection.disconnect();*/


                         magic = Pattern.compile("Credentials Rejected");
                         m = magic.matcher(str);
                        if(m.find()) {

                            //Log.d("Keepalive", "Wrong Credentials");

                            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


                            NotificationCompat.Builder mBuilder =
                                    new NotificationCompat.Builder(this)
                                            .setSmallIcon(R.drawable.wifi)
                                            .setContentTitle("Keepalive")
                                            .setContentText("Login credentials might be wrong")
                                            .setSound(alarmSound);

                            // Sets an ID for the notification
                            int mNotificationId = 001;
// Gets an instance of the NotificationManager service
                            NotificationManager mNotifyMgr =
                                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
                            mNotifyMgr.notify(mNotificationId, mBuilder.build());


                            stopSelf();

                            return;
                        }

                        magic = Pattern.compile("You are logged in");
                        m = magic.matcher(str);

                        if(m.find()){
                            //Log.d("Keepalive", "Cisco: Logged in successfully");
                        }



                        if(file.exists())
                            file.delete();

                        if(t_file.exists())
                            t_file.delete();

                        return;



                    }
                    catch(IOException aa)
                    {

                        //Log.d("Keepalive","I/O Exception at Cisco Gateway");
                        aa.printStackTrace();
                        return;
                    }




                    //--------------------//




                }


                if(code == 307)
                {
                    String str = urlConnection.getHeaderField("Location");

                    //Log.d("Keepalive", "Auth location: " + str);
                    urlConnection.disconnect();

                    url = new URL(str);
                    //Log.d("Keepalive", "Insecure URL: "+ "http://"+url.getHost()+url.getPath());
                    url = new URL("http://"+url.getHost()+url.getPath());

                    try {

                        urlConnection = (HttpURLConnection) url.openConnection();

                        urlConnection.setRequestMethod("GET");


                       urlConnection.connect();
                        //Log.d("Keepalive", "Status Code: " + Integer.toString(urlConnection.getResponseCode()));

                        in = new BufferedInputStream(urlConnection.getInputStream());

                        in.read(b, 0, 2000);
                        in.close();
                        urlConnection.disconnect();
                    }
                    catch (IOException bmw)
                    {
                        //Log.d("Keepalive", "I/O Exception at Ironport Login");

                        bmw.printStackTrace();
                    }

                    Refresh();
                    return;
                }

                if(code==303) {

                    String str = urlConnection.getHeaderField("Location");
                    String s = "hull";
                    //Log.d("Keepalive", "Auth location: " + str);
                    urlConnection.disconnect();

                    url = new URL(str);

                    urlConnection = (HttpURLConnection) url.openConnection();

                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    in = new BufferedInputStream(urlConnection.getInputStream());

                    in.read(b, 0, 2000);
                    in.close();

                    str = new String(b, "UTF-8");

                    //Log.d("Keepalive", str);

                    magic = Pattern.compile("value=\"([0-9a-f]+)\"");
                    m = magic.matcher(str);

                    if (m.find()) {
                        // //Log.d("Keepalive", "Found magic string");
                        s = m.group(1);
                    }


                    //Log.d("Keepalive", "Magic string: " + s);

                    urlConnection.disconnect();

                    //Log.d("Keepalive", url.getProtocol() + "://" + url.getHost() + ":" + url.getPort());

                    url = new URL(url.getProtocol() + "://" + url.getHost() + ":" + url.getPort());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type",
                            "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Accept",
                            "text/plain");

                    String urlParameters =
                            "username=" + URLEncoder.encode(username, "UTF-8") +
                                    "&password=" + URLEncoder.encode(password, "UTF-8") + "&magic=" + URLEncoder.encode(s, "UTF-8") + "&4Tredir=" + URLEncoder.encode("/", "UTF-8");

                    urlConnection.setRequestProperty("Content-Length", "" +
                            Integer.toString(urlParameters.getBytes().length));
                    urlConnection.setRequestProperty("Content-Language", "en-US");

                    urlConnection.setUseCaches(false);
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);

                    urlConnection.connect();


                    BufferedOutputStream wr = new BufferedOutputStream(
                            urlConnection.getOutputStream());
                    wr.write(urlParameters.getBytes("UTF-8"), 0, urlParameters.length());
                    wr.flush();
                    wr.close();


                    try {
                        in = new BufferedInputStream(urlConnection.getInputStream());

                        in.read(b, 0, 2000);
                        in.close();
                    } catch (IOException aa) {
                        //Log.d("Keepalive", "Wrong Credentials");

                        aa.printStackTrace();
                    }

                    str = new String(b, "UTF-8");

                    //Log.d("Keepalive", str);

                    magic = Pattern.compile("location.href=\"(.+?)\"");
                    m = magic.matcher(str);
                    if (!m.find()) {

                        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(this)
                                        .setSmallIcon(R.drawable.wifi)
                                        .setContentTitle("Keepalive")
                                        .setContentText("Login credentials might be wrong")
                                        .setSound(alarmSound);

                        // Sets an ID for the notification
                        int mNotificationId = 001;
// Gets an instance of the NotificationManager service
                        NotificationManager mNotifyMgr =
                                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
                        mNotifyMgr.notify(mNotificationId, mBuilder.build());


                        stopSelf();

                        return;
                    }
                    s = m.group(1);

                    //Log.d("Keepalive", "Keepalive URL: " + s);

                    try {


                        if (!file.exists())
                            file.createNewFile();


                        if (!t_file.exists())
                            t_file.createNewFile();


                        fw = new FileWriter(file.getAbsoluteFile(), false);
                        bw = new BufferedWriter(fw);
                        t_fw = new FileWriter(t_file.getAbsoluteFile(), false);
                        t_bw = new BufferedWriter(t_fw);


                        bw.write(s);
                        t_bw.write(Long.toString(SystemClock.elapsedRealtime()));

                        t_bw.flush();
                        t_bw.close();
                        bw.flush();
                        bw.close();

                        //Log.d("Keepalive", "Files written successfully at " + Long.toString(SystemClock.elapsedRealtime()));
                        status = 1;
                        urlstring = s;

                        //if (UPDATE_INTERVAL == INTERVAL_SHORT)
                            //changeTimer(INTERVAL_LONG);


                        try {
                            MainActivity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    MainActivity.setstatus(1);

                                }
                            });
                        } catch (NullPointerException h) {
                            h.printStackTrace();
                        }
                    } catch (IOException bla) {
                        //Log.d("Keepalive", "Files could not be created");
                        bla.printStackTrace();
                    }

                }




            }
            catch(IOException oo)
            {
                //Log.d("Keepalive", "Could not connect");

                if(status!=0) {
                    status = 0;
                    //if (UPDATE_INTERVAL == INTERVAL_LONG)
                        //changeTimer(INTERVAL_SHORT);

                    try {
                        MainActivity.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.setstatus(0);

                            }
                        });
                    } catch (NullPointerException h) {
                        h.printStackTrace();
                    }
                }



                try {
                    t_br = new BufferedReader(new FileReader(t_file));
                    tString = t_br.readLine();




                }
                catch(IOException | NullPointerException z) {
                    //Log.d("Keepalive", "Can't read timestamp, writing current timestamp");

                    try {
                        t_fw = new FileWriter(t_file.getAbsoluteFile(), false);
                        t_bw = new BufferedWriter(t_fw);
                        t_bw.write(Long.toString(SystemClock.elapsedRealtime()));



                    } catch (IOException la) {
                        //Log.d("Keepalive", "***FATAL***: File I/O failed");
                        stopSelf();

                    }

                    z.printStackTrace();

                }
                oo.printStackTrace();
            }
            finally {
                try {
                    if (br != null) br.close();
                    if (bw != null) bw.close();
                    if (t_br != null) t_br.close();
                    if (t_bw != null) t_bw.close();
                }
                catch(IOException askl)
                {
                    askl.printStackTrace();
                }
            }



        }
        finally {
            try {
                if (br != null) br.close();
                if (bw != null) bw.close();
                if (t_br != null) t_br.close();
                if (t_bw != null) t_bw.close();
            }
            catch(IOException askl)
            {
                askl.printStackTrace();
            }
        }



        // notificationIntent = new Intent(this, MainActivity.class);


       /* pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

         notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.wifi)
                .setContentText(urlstatus[0])
                .setContentIntent(pendingIntent).build();

        startForeground(mId, notification);*/



    }

    private void _shutdownService()
    {
        if (timer != null) timer.cancel();
        //Log.d("Keepalive", "Timer stopped...");
    }

   /* private void changeTimer(long time)
    {
        _shutdownService();
        UPDATE_INTERVAL = time;
        _startService();
    }*/

    @Override
    public void onDestroy() {
       // stopForeground(true);
        status = -1;
        _shutdownService();
       // stop=true;



    }

}
