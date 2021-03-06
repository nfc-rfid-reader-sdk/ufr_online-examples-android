package com.dlogic.ufronlinenfcreader;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static com.dlogic.ufronlinenfcreader.MainActivity.Abort;
import static com.dlogic.ufronlinenfcreader.MainActivity.bytesToHex;
import static com.dlogic.ufronlinenfcreader.MainActivity.context;
import static com.dlogic.ufronlinenfcreader.MainActivity.resp;
import static com.dlogic.ufronlinenfcreader.MainActivity.scanProgress;
import static com.dlogic.ufronlinenfcreader.MainActivity.server_address;
import static com.dlogic.ufronlinenfcreader.MainActivity.spinner;

public class Broadcast extends AsyncTask<String, Void, String> {

    private Activity g_activity;
    public Broadcast(Activity activity) {
        g_activity = activity;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected String doInBackground(String... params) {

        String str = "";

        DatagramSocket socket = null;
        InetAddress address;

        try
        {
            socket = new DatagramSocket();
            address = InetAddress.getByName(getBroadcastAddress());

            byte[] bcBuffer = {0x01, 0x01};

            DatagramPacket packet = new DatagramPacket(bcBuffer, bcBuffer.length, address, 8880);
            socket.send(packet);

            socket.setSoTimeout(5000);
            byte[] receivedResponse = new byte[28];

            long t= System.currentTimeMillis();
            long end = t+500;

            while(System.currentTimeMillis() < end)
            {
                DatagramPacket rcv_packet = new DatagramPacket(receivedResponse, 28);

                socket.receive(rcv_packet);

                if(rcv_packet.getLength() == 28)
                {
                    receivedResponse = rcv_packet.getData();
                    str += bytesToHex(receivedResponse);
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
    protected void onPostExecute(String result) {

        Abort = false;
        resp = result;
        String temp = "";
        String temp_serialNum = "";

        try {
            for (int i = 0; i < resp.length(); i = i + 56) {
                temp += resp.substring(i, i + 8) + ",";
                temp_serialNum += resp.substring(i + 38, i + 16 + 38) + ",";
            }

            temp = temp.substring(0, temp.length() - 1);
            temp_serialNum = temp_serialNum.substring(0, temp_serialNum.length() - 1);

        } catch (Exception ex) { }

        String[] ipAddresses = temp.split(",");
        final String[] serialNumbers = temp_serialNum.split(",");
        temp = "";
        temp_serialNum = "";
        List<String> list = new ArrayList<String>();

        try {
            for (int i = 0; i < ipAddresses.length; i++) {

                for (int j = 0; j < ipAddresses[i].length(); j = j + 2) {
                    int dec = Integer.parseInt(ipAddresses[i].substring(j, j + 2), 16);
                    temp += Integer.toString(dec) + ".";
                }

                for (int k = 0; k < serialNumbers[i].length(); k = k + 2) {
                    int dec = Integer.parseInt(serialNumbers[i].substring(k, k + 2), 16);
                    temp_serialNum += (char)dec;
                }

                temp = temp.substring(0, temp.length() - 1);
                list.add(temp + " / " + temp_serialNum);
                temp = "";
                temp_serialNum = "";
            }
        } catch (Exception ex) { }

        try
        {
            if(list.size() > 0)
            {
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, list);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(dataAdapter);
                spinner.setSelection(0);

                String temp_ip = spinner.getSelectedItem().toString();
                int whitespace = temp_ip.indexOf(' ');
                server_address = temp_ip.substring(0, whitespace);
            }
            else
            {
                Toast.makeText(context, "No devices found", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception ex){}

        g_activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        scanProgress.setVisibility(View.GONE);
    }

    @Override
    protected void onPreExecute()
    {

    }

    @Override
    protected void onProgressUpdate(Void... values) {}

    protected void onCancelled()
    {
        Abort = false;
    }

    public String getBroadcastAddress() {
        InetAddress broadcastAddress = null;
        try {
            Enumeration<NetworkInterface> networkInterface = NetworkInterface
                    .getNetworkInterfaces();

            while (broadcastAddress == null
                    && networkInterface.hasMoreElements()) {
                NetworkInterface singleInterface = networkInterface
                        .nextElement();
                String interfaceName = singleInterface.getName();
                if (interfaceName.contains("wlan0")
                        || interfaceName.contains("eth0")) {
                    for (InterfaceAddress infaceAddress : singleInterface
                            .getInterfaceAddresses()) {
                        broadcastAddress = infaceAddress.getBroadcast();
                        if (broadcastAddress != null) {
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        String return_ip = "";

        try
        {
            return_ip = broadcastAddress.toString().substring(1);
        }
        catch (Exception ex){}

        return return_ip;
    }

}