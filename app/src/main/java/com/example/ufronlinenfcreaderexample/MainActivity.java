package com.example.ufronlinenfcreaderexample;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    public static String msg = "default";
    public static String resp = "";
    public static String server_address = "192.168.1.101";
    public static Integer server_port = 9999;
    public static Boolean Abort = false;
    public static LongOperation lo = null;
    HttpPost httppost = null;

    public static final String MY_PREFS_NAME = "MyPrefsFile";
    TextView response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String nserver_address = prefs.getString("server", null);

        if (nserver_address!= null){
            Log.d("load server",nserver_address);
            TextView server_text = (TextView) findViewById(R.id.serverText);
            server_text.setText(nserver_address);
        }

        response = (TextView) findViewById(R.id.textView2);
        Log.d("create",msg);

    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected String doInBackground(String... params) {

            HttpClient httpclient = new DefaultHttpClient();
            httppost = new HttpPost("http://"+ server_address +"/uart1");
            HttpParams httpParameters = new BasicHttpParams();

            String str = "00";
            try {

                HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
                HttpConnectionParams.setSoTimeout(httpParameters, 5000);
                ((DefaultHttpClient) httpclient).setParams(httpParameters);
                httppost.setEntity(new StringEntity("552CAA000000DA"));
                HttpResponse response = httpclient.execute(httppost);
                httppost.abort();
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();

                str = convertStreamToString(is);
                Log.d("rsp", convertStreamToString(is));

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            if((str.substring(0, 2).equals("DE")))
            {
                String uidDefault = "UID : ";
                String tempStr = "";

                if((str.substring(11,12)).equals("4"))
                {
                    tempStr = str.substring(14, str.length()-15);
                }
                else if((str.substring(11,12)).equals("7"))
                {
                    tempStr = str.substring(14, str.length()-9);
                }

                str = uidDefault + tempStr;
            }
            else
            {
                str = "UID : ";
            }

            try {
                Log.d("port", InetAddress.getLocalHost().toString());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            return str;
        }

        private String convertStreamToString(InputStream is) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append((line + "\n"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String result) {

            Abort = false;
            Log.d("Set Abort",Abort.toString());
            Log.d("tag","post ex");
            resp = result;
            response.setText(resp);

        }

        @Override
        protected void onPreExecute() {
            TextView server_text = (TextView) findViewById(R.id.serverText);
            server_address = server_text.getText().toString();
            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("server", server_address);
            editor.putInt("port", server_port);
            editor.commit();
        }

        @Override
        protected void onProgressUpdate(Void... values) {}


        protected void onCancelled(){
            Log.d("cancel","ca");
            Abort = false;
        }

    }

    public void sendMessage(View view) {

        try
        {
            TextView textView = (TextView) findViewById(R.id.textView2);
            textView.setText("UID : Loading...");
            Log.d("msg",msg);
            Log.d("Check Abort",Abort.toString());
            if(Abort==true) {
                lo.cancel(false);
                Log.d("Aborting",Abort.toString());
            }
            else {
                lo = new LongOperation();
                lo.execute();
            }
            Abort = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
