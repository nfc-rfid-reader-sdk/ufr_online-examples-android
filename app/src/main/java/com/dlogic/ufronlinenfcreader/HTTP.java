package com.dlogic.ufronlinenfcreader;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

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
import java.lang.reflect.Array;

import static com.dlogic.ufronlinenfcreader.MainActivity.Abort;
import static com.dlogic.ufronlinenfcreader.MainActivity.CmdResponse;
import static com.dlogic.ufronlinenfcreader.MainActivity.bytesToHex;
import static com.dlogic.ufronlinenfcreader.MainActivity.cmdText;
import static com.dlogic.ufronlinenfcreader.MainActivity.eraseDelimiters;
import static com.dlogic.ufronlinenfcreader.MainActivity.hexStringToByteArray;
import static com.dlogic.ufronlinenfcreader.MainActivity.isBeep;
import static com.dlogic.ufronlinenfcreader.MainActivity.isCommand;
import static com.dlogic.ufronlinenfcreader.MainActivity.isLight;
import static com.dlogic.ufronlinenfcreader.MainActivity.resp;
import static com.dlogic.ufronlinenfcreader.MainActivity.response;
import static com.dlogic.ufronlinenfcreader.MainActivity.server_address;
import static com.dlogic.ufronlinenfcreader.MainActivity.spinner;
import static com.dlogic.ufronlinenfcreader.MainActivity.cmdStr;
import static com.dlogic.ufronlinenfcreader.MainActivity.httppost;

public class HTTP extends AsyncTask<String, Void, String> {

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected String doInBackground(String... params)
    {

        String str = "";

        HttpClient httpclient = new DefaultHttpClient();
        httppost = new HttpPost("http://"+ server_address +"/uart1");
        HttpParams httpParameters = new BasicHttpParams();

        try
        {
            HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
            HttpConnectionParams.setSoTimeout(httpParameters, 5000);
            ((DefaultHttpClient) httpclient).setParams(httpParameters);
            httppost.setEntity(new StringEntity(cmdStr));
            HttpResponse response = httpclient.execute(httppost);
            httppost.abort();
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();

            str = convertStreamToString(is);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        try
        {

            if(isCommand == true)
            {

            }
            else
            {
                if((str.substring(0, 2).equals("DE")))
                {
                    String tempStr = "";

                    if((str.substring(11,12)).equals("4"))
                    {
                        tempStr = str.substring(14, str.length()-15);
                    }
                    else if((str.substring(11,12)).equals("7"))
                    {
                        tempStr = str.substring(14, str.length()-9);
                    }

                    str =  tempStr;
                }
                else if ((str.substring(0, 4).equals("EC08")))
                {
                    str = "NO CARD";
                }
                else
                {
                    str = "";
                }
            }
        }
        catch (Exception e)
        {
            str = "";
            e.printStackTrace();
        }

        return str;
    }

    private String convertStreamToString(InputStream is)
    {

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
    protected void onPostExecute(String result)
    {
        isBeep = false;
        Abort = false;
        resp = result;

        if(isCommand == true)
        {
            CmdResponse.setText(resp);
        }
        else
        {
            response.setText(resp);
        }

        isCommand = false;
    }

    @Override
    protected void onPreExecute()
    {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                server_address = parent.getItemAtPosition(pos).toString();
            }

            public void onNothingSelected(AdapterView<?> parent) {
                server_address = parent.getItemAtPosition(0).toString();
            }
        });

        if(server_address.isEmpty())
        {
            return;
        }

        if(isBeep == true)
        {
            cmdStr = "5526AA000101E0";
        }
        else if(isLight == true)
        {
            cmdStr = "5526AA000300E1";
        }
        else if(isCommand == true)
        {
            cmdStr = cmdText.getText().toString().trim();
            cmdStr = eraseDelimiters(cmdStr);

            if(cmdStr.contains("xx") || cmdStr.contains("xX") || cmdStr.contains("Xx") || cmdStr.contains("XX"))
            {
                byte[] hex_command = hexStringToByteArray(cmdStr.substring(0, cmdStr.length() - 2));
                byte crc = 0;

                for(int i = 0; i < hex_command.length; i++)
                {
                    crc ^= hex_command[i];
                }
                crc += 0x07;

                byte[] calculated_crc = new byte[hex_command.length + 1];
                System.arraycopy(hex_command,0,calculated_crc,0,hex_command.length);
                calculated_crc[hex_command.length] = crc;

                cmdStr = bytesToHex(calculated_crc);
            }
        }
        else
        {
            cmdStr = "552CAA000000DA";
        }

    }

    @Override
    protected void onProgressUpdate(Void... values) {}


    protected void onCancelled()
    {
        isCommand = false;
        isBeep = false;
        Abort = false;
    }

}