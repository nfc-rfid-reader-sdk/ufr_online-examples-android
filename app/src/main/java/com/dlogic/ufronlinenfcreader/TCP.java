package com.dlogic.ufronlinenfcreader;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import static com.dlogic.ufronlinenfcreader.MainActivity.Abort;
import static com.dlogic.ufronlinenfcreader.MainActivity.CmdResponse;
import static com.dlogic.ufronlinenfcreader.MainActivity.bytesToHex;
import static com.dlogic.ufronlinenfcreader.MainActivity.cmdText;
import static com.dlogic.ufronlinenfcreader.MainActivity.hexStringToByteArray;
import static com.dlogic.ufronlinenfcreader.MainActivity.isBeep;
import static com.dlogic.ufronlinenfcreader.MainActivity.isCommand;
import static com.dlogic.ufronlinenfcreader.MainActivity.isLight;
import static com.dlogic.ufronlinenfcreader.MainActivity.port_text;
import static com.dlogic.ufronlinenfcreader.MainActivity.resp;
import static com.dlogic.ufronlinenfcreader.MainActivity.response;
import static com.dlogic.ufronlinenfcreader.MainActivity.server_address;
import static com.dlogic.ufronlinenfcreader.MainActivity.server_port;
import static com.dlogic.ufronlinenfcreader.MainActivity.spinner;
import static com.dlogic.ufronlinenfcreader.MainActivity.cmdBuffer;

public class TCP extends AsyncTask<String, Void, String> {

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected String doInBackground(String... params) {

        String str = "";

        Socket socket = new Socket();
        SocketAddress address = new InetSocketAddress(server_address, server_port);

        try
        {
            socket.connect(address, 3000);

            OutputStream out = null;
            try {
                out = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            DataOutputStream dos = new DataOutputStream(out);
            dos.write(cmdBuffer, 0, cmdBuffer.length);
            dos.flush();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            socket.setSoTimeout(3000);
        }
        catch (SocketException e)
        {
            e.printStackTrace();
            return "";
        }

        InputStream stream = null;

        try
        {
            stream = socket.getInputStream();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        byte[] data = new byte[4096];

        try
        {
            int count = stream.read(data);

            Log.d("COUNT TCP ", Integer.toString(count));

            byte[] responseBytes = new byte[count];

            System.arraycopy(data, 0, responseBytes, 0, count);

            str = bytesToHex(responseBytes);

            if(isCommand == true)
            {
                //return str;
            }
            else
            {
                try
                {
                    if((str.substring(0, 2).equals("DE")))
                    {
                        String tempStr = "";

                        if((str.substring(11,12)).equals("4"))
                        {
                            tempStr = str.substring(14, str.length()-14);
                        }
                        else if((str.substring(11,12)).equals("7"))
                        {
                            tempStr = str.substring(14, str.length()-8);
                        }

                        str = tempStr;
                    }
                    else if ((str.substring(0, 4).equals("EC08")))
                    {
                        str = "NO CARD";
                    }
                    else
                    {
                        str = "COMMUNICATION ERROR";
                    }
                }
                catch (Exception e)
                {
                    str = "";
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return str;
    }

    @Override
    protected void onPostExecute(String result)
    {
        isLight = false;
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

        if(isBeep == true)
        {
            cmdBuffer = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, 0x01, 0x01, (byte)0xE0};
        }
        else if(isLight == true)
        {
            cmdBuffer = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, 0x03, 0x00, (byte)0xE1};
        }
        else if(isCommand == true)
        {
            cmdBuffer = hexStringToByteArray(cmdText.getText().toString().trim());
        }
        else
        {
            cmdBuffer = new byte[]{0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
        }

        server_port = Integer.parseInt(port_text.getText().toString().trim());
    }

    @Override
    protected void onProgressUpdate(Void... values) {}


    protected void onCancelled()
    {
        isLight = false;
        isCommand = false;
        isBeep = false;
        Abort = false;
    }

}