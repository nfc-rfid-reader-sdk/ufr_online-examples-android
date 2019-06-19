package com.dlogic.ufronlinenfcreader;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
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
import static com.dlogic.ufronlinenfcreader.MainActivity.beepByte;
import static com.dlogic.ufronlinenfcreader.MainActivity.beepSpinner;
import static com.dlogic.ufronlinenfcreader.MainActivity.bytesToHex;
import static com.dlogic.ufronlinenfcreader.MainActivity.cmdText;
import static com.dlogic.ufronlinenfcreader.MainActivity.eraseDelimiters;
import static com.dlogic.ufronlinenfcreader.MainActivity.hexStringToByteArray;
import static com.dlogic.ufronlinenfcreader.MainActivity.ip_text;
import static com.dlogic.ufronlinenfcreader.MainActivity.isCommand;
import static com.dlogic.ufronlinenfcreader.MainActivity.isLight;
import static com.dlogic.ufronlinenfcreader.MainActivity.lightByte;
import static com.dlogic.ufronlinenfcreader.MainActivity.lightSpinner;
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

            byte[] responseBytes = new byte[count];

            System.arraycopy(data, 0, responseBytes, 0, count);

            str = bytesToHex(responseBytes);

            if(isCommand == true)
            {

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
                        str = "";
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
        String temp_ip = spinner.getSelectedItem().toString();
        int whitespace = temp_ip.indexOf(' ');
        server_address = temp_ip.substring(0, whitespace);

        String manual_ip = ip_text.getText().toString().trim();

        if(!manual_ip.isEmpty())
        {
            server_address = manual_ip;
        }

        if(server_address.isEmpty())
        {
            return;
        }

        if(isLight == true)
        {
            beepSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    beepByte = (byte) ((byte)pos + (byte)1);
                }

                public void onNothingSelected(AdapterView<?> parent) {
                    beepByte = 1;
                }
            });

            lightSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    lightByte = (byte) ((byte)pos + (byte)1);
                }

                public void onNothingSelected(AdapterView<?> parent) {
                    lightByte = 1;
                }
            });

            cmdBuffer = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, lightByte, beepByte, 0x00};
            byte checksum = 0;

            for(int i = 0; i < 6; i++)
            {
                checksum ^= cmdBuffer[i];
            }
            checksum += 0x07;

            cmdBuffer = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, lightByte, beepByte, checksum};
        }
        else if(isCommand == true)
        {
            String cmdStr = cmdText.getText().toString().trim();
            cmdStr = eraseDelimiters(cmdStr);

            if(cmdStr.contains("xx") || cmdStr.contains("xX") || cmdStr.contains("Xx") || cmdStr.contains("XX"))
            {
                byte crc = 0;
                int cmd_length = cmdStr.length() / 2;
                byte[] calculated_crc = new byte[cmd_length];

                byte[] temp_buffer = hexStringToByteArray(cmdStr.substring(0, cmdStr.length() - 2));

                for(int i = 0; i < temp_buffer.length; i++)
                {
                    crc ^= temp_buffer[i];
                }

                crc += 0x07;
                calculated_crc[temp_buffer.length] = crc;
                System.arraycopy(temp_buffer,0,calculated_crc,0, temp_buffer.length);

                cmdBuffer = hexStringToByteArray(bytesToHex(calculated_crc));
            }
            else
            {
                cmdBuffer = hexStringToByteArray(cmdStr);
            }
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
        Abort = false;
    }

}