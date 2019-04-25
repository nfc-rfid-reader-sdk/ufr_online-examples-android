package com.example.ufronlinenfcreaderexample;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static Context context;
    public static String resp = "";
    public static String server_address = "192.168.0.101";
    public static Integer server_port = 8881;
    public static Boolean Abort = false;
    public static HttpOperation httpCmd = null;
    public static UDPOperation udpCmd = null;
    public static TCPOperation tcpCmd = null;
    public static BroadcastOperation broadcastOperation = null;
    HttpPost httppost = null;
    byte[] cmdBuffer = {0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
    public boolean isBeep = false;
    String cmdStr = "552CAA000000DA";

    public static final String MY_PREFS_NAME = "MyPrefsFile";
    TextView response;
    Spinner spinner;

    public static String bytesToHex(byte[] byteArray)
    {
        StringBuilder sBuilder = new StringBuilder(byteArray.length * 2);
        for(byte b: byteArray)
            sBuilder.append(String.format("%02X", b & 0xff));
        return sBuilder.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);

        RadioButton http = findViewById(R.id.httpRB);
        EditText portNumber = findViewById(R.id.portText);

        if(http.isChecked())
        {
            portNumber.setText("80");
            portNumber.setInputType(0);
        }

        Integer nserver_port = prefs.getInt("port", 8881);

        if (nserver_port!= 8881)
        {
            TextView port_text =  findViewById(R.id.portText);
            port_text.setText(nserver_port.toString());
        }

        response = findViewById(R.id.textView2);
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

        } catch (SocketException e) {
            e.printStackTrace();
        }

        return broadcastAddress.toString().substring(1);
    }

    private class BroadcastOperation extends AsyncTask<String, Void, String> {

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
                byte[] receivedResponse = new byte[18];

                long t= System.currentTimeMillis();
                long end = t+150;

                while(System.currentTimeMillis() < end)
                {
                    DatagramPacket rcv_packet = new DatagramPacket(receivedResponse, 18);

                    socket.receive(rcv_packet);

                    if(rcv_packet.getLength() == 18)
                    {
                        Log.d("Len : ", Integer.toString(rcv_packet.getLength()));
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
            isBeep = false;
            Abort = false;
            resp = result;
            String temp = "";

            spinner = findViewById(R.id.spinnerIP);

            try {
                for (int i = 0; i < resp.length(); i = i + 36) {
                    temp += resp.substring(i, i + 8) + ",";
                }

                temp = temp.substring(0, temp.length() - 1);
            } catch (Exception ex) { }

            String[] ipAddresses = temp.split(",");
            temp = "";
            List<String> list = new ArrayList<String>();

            try {
                for (int i = 0; i < ipAddresses.length; i++) {
                    for (int j = 0; j < ipAddresses[i].length(); j = j + 2) {
                        int dec = Integer.parseInt(ipAddresses[i].substring(j, j + 2), 16);
                        temp += Integer.toString(dec) + ".";
                    }

                    temp = temp.substring(0, temp.length() - 1);
                    list.add(temp);
                    temp = "";
                }
            } catch (Exception ex) { }

            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, list);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(dataAdapter);
            spinner.setSelection(0);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    server_address = parent.getItemAtPosition(pos).toString();
                }

                public void onNothingSelected(AdapterView<?> parent) {
                    server_address = parent.getItemAtPosition(0).toString();
                }
            });
        }

        @Override
        protected void onPreExecute()
        {

            if(isBeep == true)
            {
                cmdBuffer = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, 0x01, 0x01, (byte)0xE0};
            }
            else
            {
                cmdBuffer = new byte[]{0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
            }

            TextView port_text =  findViewById(R.id.portText);
            server_port = Integer.parseInt(port_text.getText().toString().trim());
            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("server", server_address);
            editor.putInt("port", server_port);
            editor.commit();

        }

        @Override
        protected void onProgressUpdate(Void... values) {}

        protected void onCancelled()
        {
            isBeep = false;
            Abort = false;
        }
    }

    private class UDPOperation extends AsyncTask<String, Void, String> {

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

                socket.setSoTimeout(5000);
                byte[] receivedResponse = new byte[18];
                DatagramPacket rcv_packet = new DatagramPacket(receivedResponse, 18);

                socket.receive(rcv_packet);

                receivedResponse = rcv_packet.getData();
                str = bytesToHex(receivedResponse);

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
                        str = "COMMUNICATION ERROR";
                    }
                }
                catch (Exception e)
                {
                    str = "";
                    e.printStackTrace();
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
            isBeep = false;
            Abort = false;
            resp = result;
            response.setText(resp);
        }

        @Override
        protected void onPreExecute()
        {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    server_address = parent.getItemAtPosition(pos).toString();
                }

                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            if(isBeep == true)
            {
                cmdBuffer = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, 0x01, 0x01, (byte)0xE0};
            }
            else
            {
                cmdBuffer = new byte[]{0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
            }

            TextView port_text =  findViewById(R.id.portText);
            server_port = Integer.parseInt(port_text.getText().toString().trim());
            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("server", server_address);
            editor.putInt("port", server_port);
            editor.commit();

        }

        @Override
        protected void onProgressUpdate(Void... values) {}

        protected void onCancelled()
        {
            isBeep = false;
            Abort = false;
        }
    }

    private class HttpOperation extends AsyncTask<String, Void, String> {

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
                    str = "COMMUNICATION ERROR";
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
            response.setText(resp);
        }

        @Override
        protected void onPreExecute()
        {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    server_address = parent.getItemAtPosition(pos).toString();
                }

                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            if(isBeep == true)
            {
                cmdStr = "5526AA000101E0";
            }
            else
            {
                cmdStr = "552CAA000000DA";
            }

            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("server", server_address);
            editor.putInt("port", server_port);
            editor.commit();
        }

        @Override
        protected void onProgressUpdate(Void... values) {}


        protected void onCancelled()
        {
            isBeep = false;
            Abort = false;
        }

    }

    private class TCPOperation extends AsyncTask<String, Void, String>
    {

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
                dos.write(cmdBuffer, 0, 7);
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

            byte[] data = new byte[18];

            try
            {
                int count = stream.read(data);

                str = bytesToHex(data);

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
                        str = "COMMUNICATION ERROE";
                    }
                }
                catch (Exception e)
                {
                    str = "";
                    e.printStackTrace();
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
            isBeep = false;
            Abort = false;
            resp = result;
            response.setText(resp);
        }

        @Override
        protected void onPreExecute()
        {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    server_address = parent.getItemAtPosition(pos).toString();
                }

                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            if(isBeep == true)
            {
                cmdBuffer = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, 0x01, 0x01, (byte)0xE0};
            }
            else
            {
                cmdBuffer = new byte[]{0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
            }

            TextView port_text =  findViewById(R.id.portText);
            server_port = Integer.parseInt(port_text.getText().toString().trim());
            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("server", server_address);
            editor.putInt("port", server_port);
            editor.commit();
        }

        @Override
        protected void onProgressUpdate(Void... values) {}


        protected void onCancelled()
        {
            isBeep = false;
            Abort = false;
        }
    }

    public void onBeepClicked(View view)
    {
        isBeep = true;
        RadioButton udp = findViewById(R.id.udpRB);
        RadioButton http = findViewById(R.id.httpRB);
        RadioButton tcp = findViewById(R.id.tcpRB);

        if(udp.isChecked())
        {
            try
            {
                if(Abort == true)
                {
                    udpCmd.cancel(false);
                }
                else
                {
                    udpCmd = new UDPOperation();
                    udpCmd.execute();
                }
                Abort = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else if(http.isChecked())
        {
            try
            {
                if(Abort == true)
                {
                    httpCmd.cancel(false);
                }
                else
                {
                    httpCmd = new HttpOperation();
                    httpCmd.execute();
                }
                Abort = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else if(tcp.isChecked())
        {
            try
            {
                if(Abort == true)
                {
                    tcpCmd.cancel(false);
                }
                else
                {
                    tcpCmd = new TCPOperation();
                    tcpCmd.execute();
                }
                Abort = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        isBeep = false;
    }

    public void onUdpRadioButtonClicked(View view)
    {
        TextView uidTV = findViewById(R.id.textView2);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.httpRB);
        RadioButton udp = findViewById(R.id.udpRB);
        RadioButton tcp = findViewById(R.id.tcpRB);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        if(http.isChecked())
        {
            http.setChecked(false);
        }

        if(tcp.isChecked())
        {
            tcp.setChecked(false);
        }

        udp.setChecked(true);
    }

    public void onHttpRadioButtonClicked(View view)
    {
        TextView uidTV = findViewById(R.id.textView2);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.httpRB);
        RadioButton udp = findViewById(R.id.udpRB);
        RadioButton tcp = findViewById(R.id.tcpRB);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("80");
        portNumber.setInputType(0);

        if(udp.isChecked())
        {
            udp.setChecked(false);
        }

        if(tcp.isChecked())
        {
            tcp.setChecked(false);
        }

        http.setChecked(true);
    }

    public void onTcpRadioButtonClicked(View view)
    {
        TextView uidTV = findViewById(R.id.textView2);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.httpRB);
        RadioButton udp = findViewById(R.id.udpRB);
        RadioButton tcp = findViewById(R.id.tcpRB);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        if(udp.isChecked())
        {
            udp.setChecked(false);
        }

        if(http.isChecked())
        {
            http.setChecked(false);
        }

        tcp.setChecked(true);
    }

    public void sendMessage(View view) {

        RadioButton udp = findViewById(R.id.udpRB);
        RadioButton http = findViewById(R.id.httpRB);
        RadioButton tcp = findViewById(R.id.tcpRB);

        if(udp.isChecked())
        {
            try
            {
                if(Abort == true)
                {
                    udpCmd.cancel(false);
                }
                else
                {
                    udpCmd = new UDPOperation();
                    udpCmd.execute();
                }
                Abort = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else if(http.isChecked())
        {
            try
            {
                if(Abort == true)
                {
                    httpCmd.cancel(false);
                }
                else
                {
                    httpCmd = new HttpOperation();
                    httpCmd.execute();
                }
                Abort = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else if(tcp.isChecked())
        {
            try
            {
                if(Abort == true)
                {
                    tcpCmd.cancel(false);
                }
                else
                {
                    tcpCmd = new TCPOperation();
                    tcpCmd.execute();
                }
                Abort = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    public void RefreshBroadcast(View view)
    {
        try
        {
            if(Abort == true)
            {
                broadcastOperation.cancel(false);
            }
            else
            {
                broadcastOperation = new BroadcastOperation();
                broadcastOperation.execute();
            }
            Abort = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

