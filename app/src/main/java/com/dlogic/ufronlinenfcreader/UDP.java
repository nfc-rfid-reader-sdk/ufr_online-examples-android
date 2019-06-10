package com.dlogic.ufronlinenfcreader;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static com.dlogic.ufronlinenfcreader.MainActivity.Abort;
import static com.dlogic.ufronlinenfcreader.MainActivity.CmdResponse;
import static com.dlogic.ufronlinenfcreader.MainActivity.beepSpinner;
import static com.dlogic.ufronlinenfcreader.MainActivity.bytesToHex;
import static com.dlogic.ufronlinenfcreader.MainActivity.cmdText;
import static com.dlogic.ufronlinenfcreader.MainActivity.eraseDelimiters;
import static com.dlogic.ufronlinenfcreader.MainActivity.hexStringToByteArray;
import static com.dlogic.ufronlinenfcreader.MainActivity.ip_text;
import static com.dlogic.ufronlinenfcreader.MainActivity.isCommand;
import static com.dlogic.ufronlinenfcreader.MainActivity.isLight;
import static com.dlogic.ufronlinenfcreader.MainActivity.lightSpinner;
import static com.dlogic.ufronlinenfcreader.MainActivity.port_text;
import static com.dlogic.ufronlinenfcreader.MainActivity.resp;
import static com.dlogic.ufronlinenfcreader.MainActivity.response;
import static com.dlogic.ufronlinenfcreader.MainActivity.server_address;
import static com.dlogic.ufronlinenfcreader.MainActivity.server_port;
import static com.dlogic.ufronlinenfcreader.MainActivity.spinner;
import static com.dlogic.ufronlinenfcreader.MainActivity.cmdBuffer;
import static com.dlogic.ufronlinenfcreader.MainActivity.beepByte;
import static com.dlogic.ufronlinenfcreader.MainActivity.lightByte;

public class UDP extends AsyncTask<String, Void, String> {

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected String doInBackground(String... params) {

        String str = "";

        DatagramSocket socket = null;
        InetAddress address;

        try
        {
            socket = new DatagramSocket();
            address = InetAddress.getByName(server_address);

            DatagramPacket packet = new DatagramPacket(cmdBuffer, cmdBuffer.length, address, server_port);
            socket.send(packet);

            socket.setSoTimeout(3000);
            byte[] receivedResponse = new byte[4096];
            DatagramPacket rcv_packet = new DatagramPacket(receivedResponse, 4096);

            socket.receive(rcv_packet);

            receivedResponse = rcv_packet.getData();

            byte[] receivedBytes = new byte[rcv_packet.getLength()];
            System.arraycopy(receivedResponse, 0, receivedBytes, 0, rcv_packet.getLength());

            str = bytesToHex(receivedBytes);

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
                catch (Exception e)
                {
                    str = "";
                    e.printStackTrace();
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
            }
        }

        return str;
    }

    @Override
    protected void onPostExecute(String result)
    {
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
        isCommand = false;
        Abort = false;
    }
}