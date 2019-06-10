package com.dlogic.ufronlinenfcreader;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.client.methods.HttpPost;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import static android.widget.Toast.makeText;
import static org.apache.http.protocol.HTTP.USER_AGENT;
import static org.apache.http.protocol.HTTP.UTF_8;

public class MainActivity extends Activity {

    static Context context;
    public static String resp = "";
    public static String server_address = "192.168.0.101";
    public static Integer server_port = 8881;
    public static Spinner spinner;
    public static TextView response;
    public static TextView CmdResponse;
    public static TextView num_of_bytes;
    public static EditText port_text;
    public static EditText ip_text;
    public static EditText cmdText;
    public static Boolean Abort = false;
    public static boolean isLight = false;
    public static boolean isCommand = false;
    public static byte[] cmdBuffer = new byte[4096];
    public static String cmdStr = "552CAA000000DA";
    public static HttpPost httppost = null;

    public static Button btnCONNECT;
    public static Broadcast broadcastOperation = null;
    public static UDP udpCmd = null;
    public static HTTP httpCmd = null;
    public static TCP tcpCmd = null;

    public static Spinner beepSpinner;
    public static Spinner lightSpinner;
    public static byte beepByte = 1;
    public static byte lightByte = 1;
    //--------------------------------------------------------------------
    ArrayList<String> listWithoutDuplicates;
    public static boolean isBTConnected = false;
    Toast toastForConnect;
    public static ProgressBar scanProgress;
    private Handler mHandler;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    List<String> listDevices;
    private BluetoothAdapter mBTAdapter;
    private ArrayAdapter<String> mBTArrayAdapter;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static boolean BT_GET_UID = false;
    public static boolean BT_SEND_CMD = false;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;
    private ConnectedThread mConnectedThread;
    private BluetoothSocket mBTSocket = null;
    //--------------------------------------------------------------------
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String toHexadecimal(byte[] bytes, int len) {
        char[] hexChars = new char[len * 2];
        for ( int j = 0; j < len; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        spinner = findViewById(R.id.IpSpinner);
        response = findViewById(R.id.txtUID);
        port_text = findViewById(R.id.portText);
        cmdText = findViewById(R.id.cmdEditText);
        CmdResponse = findViewById(R.id.textViewCmdResponse);
        ip_text = findViewById(R.id.ipText);
        num_of_bytes = findViewById(R.id.labelResponse);
        btnCONNECT = findViewById(R.id.btnConnect);
        beepSpinner = findViewById(R.id.beepSignalSpinner);
        lightSpinner = findViewById(R.id.lightSignalSpinner);
        scanProgress = findViewById(R.id.progressBar);

        ArrayAdapter<CharSequence> spnBeepAdapter = ArrayAdapter.createFromResource(context,
                R.array.beep_signal_modes,
                R.layout.dl_spinner_textview);
        spnBeepAdapter.setDropDownViewResource(R.layout.dl_spinner_textview);
        beepSpinner.setAdapter(spnBeepAdapter);
        beepSpinner.setSelection(0);

        ArrayAdapter<CharSequence> spnLightAdapter = ArrayAdapter.createFromResource(context,
                R.array.light_signal_modes,
                R.layout.dl_spinner_textview);
        spnBeepAdapter.setDropDownViewResource(R.layout.dl_spinner_textview);
        lightSpinner.setAdapter(spnLightAdapter);
        lightSpinner.setSelection(0);

        listDevices = new ArrayList<String>();

        mBTDevices = new ArrayList<>();

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        spinner.setAdapter(mBTArrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            RadioButton bt = findViewById(R.id.radioButtonBluetooth);

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                if(bt.isChecked())
                {
                    String info = parent.getItemAtPosition(pos).toString();
                    final String address = info.substring(info.length() - 17);
                    final String name = info.substring(0,info.length() - 17);
                    ip_text.setText(address);
                    btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
                    btnCONNECT.setText("CONNECT");
                    btnCONNECT.setTextColor(Color.BLACK);
                    btnCONNECT.setEnabled(true);
                }
                else
                {
                    String temp_ip = parent.getItemAtPosition(pos).toString();
                    int whitespace = temp_ip.indexOf(' ');
                    server_address = temp_ip.substring(0, whitespace).trim();
                    ip_text.setText(server_address);
                    btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pressed));
                    btnCONNECT.setText("Connected");
                    btnCONNECT.setTextColor(Color.WHITE);
                    btnCONNECT.setEnabled(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){

                    String readMessage = "";
                    byte resp[] = (byte[])msg.obj;

                    if(BT_GET_UID == true)
                    {
                        if(resp[0] == (byte)0xDE)
                        {
                            if(resp[1]==0x2C)
                            {
                                byte uid[] = new byte[10];

                                System.arraycopy(resp, 7, uid, 0, resp[5]);
                                readMessage += toHexadecimal(uid, (int) resp[5]);
                            }
                        }
                        else {
                            if(resp[1]==0x08)
                            {
                                readMessage = "NO CARD";
                            }
                        }

                        response.setText(readMessage);
                        BT_GET_UID = false;
                    }
                    else if(BT_SEND_CMD == true)
                    {
                        Log.d("cmd response BT", bytesToHex(resp));
                        CmdResponse.setText(bytesToHex(resp));
                        BT_SEND_CMD = false;
                    }
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1) {
                        isBTConnected = true;
                        makeText(getApplicationContext(), "Connected to uFR Online : " + (String)msg.obj, Toast.LENGTH_SHORT).show();
                        btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pressed));
                        btnCONNECT.setText("Connected");
                        btnCONNECT.setTextColor(Color.WHITE);
                        btnCONNECT.setEnabled(false);
                        //mBTArrayAdapter.clear();
                    }
                    else {
                        btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
                        btnCONNECT.setText("CONNECT");
                        btnCONNECT.setTextColor(Color.BLACK);
                        //makeText(getApplicationContext(), "Connection failed, please try again", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

    }

    public static boolean isHexChar(char c)
    {
        char hexChars[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'x',
            'A', 'B', 'C', 'D', 'E', 'F', 'X'};

        for(int i = 0; i < 24; i++)
        {
            if(c == hexChars[i])
            {
                return true;
            }
        }

        return false;
    }

    public static String eraseDelimiters(String hex_str)
    {
        for(int i = 0; i < hex_str.length(); i++)
        {
            if(!isHexChar(hex_str.charAt(i)))
            {
                hex_str = hex_str.substring(0, i) + hex_str.substring(i + 1);
            }
        }

        return hex_str;
    }

    public static byte[] hexStringToByteArray(String paramString) throws IllegalArgumentException {
        int j = paramString.length();

        if (j % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }

        byte[] arrayOfByte = new byte[j / 2];
        int hiNibble, loNibble;

        for (int i = 0; i < j; i += 2) {
            hiNibble = Character.digit(paramString.charAt(i), 16);
            loNibble = Character.digit(paramString.charAt(i + 1), 16);
            if (hiNibble < 0) {
                throw new IllegalArgumentException("Illegal hex digit at position " + i);
            }
            if (loNibble < 0) {
                throw new IllegalArgumentException("Illegal hex digit at position " + (i + 1));
            }
            arrayOfByte[(i / 2)] = ((byte) ((hiNibble << 4) + loNibble));
        }
        return arrayOfByte;
    }

    public static String bytesToHex(byte[] byteArray)
    {
        StringBuilder sBuilder = new StringBuilder(byteArray.length * 2);
        for(byte b: byteArray)
            sBuilder.append(String.format("%02X", b & 0xff));
        return sBuilder.toString();
    }

    public void OnScanClicked(View view){

        scanProgress.setVisibility(view.VISIBLE);

        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
        {
            spinner.setAdapter(null);
            discoverBluetoothDevices();
            scanProgress.setVisibility(View.GONE);
        }
        else
        {
            try
            {
                if(Abort == true)
                {
                    broadcastOperation.cancel(false);
                }
                else
                {
                    broadcastOperation = new Broadcast();
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

    public void OnRadioButtonTCPClicked(View view)
    {
        mBTAdapter.disable();

        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
        {
            ip_text.setText("");
            spinner.setAdapter(null);
            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
        }

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        udp.setChecked(false);
        http.setChecked(false);
        tcp.setChecked(true);
        bt.setChecked(false);
    }

    public void OnRadioButtonUDPClicked(View view)
    {
        mBTAdapter.disable();

        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
        {
            ip_text.setText("");
            spinner.setAdapter(null);
            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
        }

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        udp.setChecked(true);
        http.setChecked(false);
        tcp.setChecked(false);
        bt.setChecked(false);
    }

    public void OnRadioButtonHTTPClicked(View view)
    {
        mBTAdapter.disable();

        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
        {
            ip_text.setText("");
            spinner.setAdapter(null);
            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
        }

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("80");
        portNumber.setInputType(0);

        udp.setChecked(false);
        http.setChecked(true);
        tcp.setChecked(false);
        bt.setChecked(false);
    }

    public void OnRadioButtonBluetoothClicked(View view)
    {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);

        if(http.isChecked() || udp.isChecked() || tcp.isChecked())
        {
            spinner.setAdapter(null);
            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
            ip_text.setText("");
        }

        http.setChecked(false);
        udp.setChecked(false);
        tcp.setChecked(false);
    }

    public void DoOperation()
    {
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);

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
                    udpCmd = new UDP();
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
                    httpCmd = new HTTP();
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
                    tcpCmd = new TCP();
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

    public void OnButtonGetUIDClick(View view)
    {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
        {
            BT_GET_UID = true;
            byte[] byteArray1 = {0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
            if(mConnectedThread != null)
                mConnectedThread.write(byteArray1);
        }
        else
        {
            DoOperation();
        }
    }

    public void onLightClicked(View view)
    {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
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

            byte[] byteArray1 = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, lightByte, beepByte, 0x00};
            byte checksum = 0;

            for(int i = 0; i < 6; i++)
            {
                checksum ^= byteArray1[i];
            }
            checksum += 0x07;

            byteArray1 = new byte[]{0x55, 0x26, (byte)0xAA, 0x00, lightByte, beepByte, checksum};
            if(mConnectedThread != null)
                mConnectedThread.write(byteArray1);
        }
        else {
            isLight = true;
            DoOperation();
            isLight = false;
        }
    }

    public void onSendCommandClicked(View view)
    {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);
        byte[] BT_CMDbuffer;

        if(bt.isChecked())
        {
            BT_SEND_CMD = true;

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

                BT_CMDbuffer = hexStringToByteArray(bytesToHex(calculated_crc));
            }
            else
            {
                BT_CMDbuffer = hexStringToByteArray(cmdStr);
            }

            if(mConnectedThread != null)
                mConnectedThread.write(BT_CMDbuffer);
        }
        else
        {
            isCommand = true;
            DoOperation();
        }
    }

    //--------------------------------------------------
    //Bluetooth
    //--------------------------------------------------

    public void OnConnectClicked(View view)
    {
        btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_not_pressed));
        btnCONNECT.setText("CONNECT");
        btnCONNECT.setTextColor(Color.BLACK);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(!bt.isChecked())
            return;

        if(!mBTAdapter.isEnabled()) {
            makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            return;
        }

        toastForConnect = makeText(this, "Connecting ... Please wait", Toast.LENGTH_SHORT);

        CountDownTimer toastCountDown;
        toastCountDown = new CountDownTimer(12000, 1000 /*Tick duration*/) {
            public void onTick(long millisUntilFinished) {
                if(isBTConnected)
                {
                    toastForConnect.cancel();
                }
                toastForConnect.show();
            }
            public void onFinish() {
                toastForConnect.cancel();
            }
        };

        // Show the toast and starts the countdown
        toastForConnect.show();
        toastCountDown.start();
        String info = "";

        try
        {
            info = spinner.getSelectedItem().toString();
        }
        catch (Exception ex){}

        String name_info = "";
        try
        {
            name_info = info.substring(0,info.length() - 17);
        }
        catch (Exception ex){}

        final String name = name_info;

        new Thread()
        {
            public void run() {
                boolean fail = false;
                BluetoothDevice device = null;

                try
                {
                    device = mBTAdapter.getRemoteDevice(ip_text.getText().toString().trim());
                }
                catch (Exception ex){return;}

                try {
                    mBTSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                    fail = true;
                    makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }
                try {
                    mBTSocket.connect();
                } catch (Exception e) {
                    try {
                        fail = true;
                        mBTSocket.close();
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                    } catch (Exception e2) {
                        return;
                        //makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
                if(fail == false) {
                    mConnectedThread = new ConnectedThread(mBTSocket);
                    mConnectedThread.start();

                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                            .sendToTarget();
                }
            }
        }.start();
    }

    private BroadcastReceiver broadcastReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);

                try
                {
                    if(device.getName().startsWith("ON"))
                    {
                        listDevices.add(device.getName() + " " + device.getAddress());
                        mBTDevices.add(device);
                    }
                }
                catch (Exception ex){}
            }

            LinkedHashSet<String> hashSet = new LinkedHashSet<>(listDevices);
            listWithoutDuplicates = new ArrayList<>(hashSet);
            spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_dropdown_item_1line,listWithoutDuplicates));
        }
    };


    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
    }

    public void discoverBluetoothDevices() {

        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();

            //check BT permissions in manifest
            checkBTPermissions();

            mBTAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceive, discoverDevicesIntent);
        }
        if(!mBTAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            mBTAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceive, discoverDevicesIntent);
        }
    }

    @Override
    protected void onDestroy() {
        try
        {
            super.onDestroy();
            unregisterReceiver(broadcastReceive);
        }
        catch (Exception ex){};
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
        }
        try
        {
            return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        }
        catch (Exception ex){return null;}
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        buffer = new byte[1024];
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        buffer = new byte[bytes];
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        public void write(byte[] input) {

            try {
                mmOutStream.write(input);
            } catch (IOException e) { }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
