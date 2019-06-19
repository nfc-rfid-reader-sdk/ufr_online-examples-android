package com.dlogic.ufronlinenfcreader;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.client.methods.HttpPost;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import si.inova.neatle.Neatle;
import si.inova.neatle.monitor.ConnectionMonitor;
import si.inova.neatle.operation.Operation;
import si.inova.neatle.operation.OperationResults;
import si.inova.neatle.operation.SimpleOperationObserver;
import si.inova.neatle.source.ByteArrayInputSource;
import static android.widget.Toast.makeText;


public class MainActivity extends Activity {

    private static int write_bytes_done = 0;
    public boolean btSerialIsConnected = false;
    public boolean btBLEIsConnected = false;
    public String BLE_DEVICE_NAME = "";
    public String BLE_MAC_ADDRESS = "";
    private static BluetoothDevice device;
    private static ConnectionMonitor connectionMonitor = null;
    public static Context global_context;
    private static boolean finishedWrite = false;
    private static final UUID serviceToWrite = UUID.fromString("e7f9840b-d767-4169-a3d0-a83b083669df");
    private static final UUID characteristicToWrite = UUID.fromString("8bdc835c-10fe-407f-afb0-b21926f068a7");
    private static boolean finishedRead = false;
    private static byte data[];
    //-------------------------------------------------------------------
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
    List<String> listDevices;
    private BluetoothAdapter mBTAdapter;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static boolean BT_GET_UID = false;
    public static boolean BT_SEND_CMD = false;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;
    private ConnectedThread mConnectedThread;
    private BluetoothSocket mBTSocket = null;
    //--------------------------------------------------------------------

    Button btnScan;

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

        global_context = getApplicationContext();
        context = this;
        btnScan = findViewById(R.id.btnScan);
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
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        checkBTPermissions();

        btnScan.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
                btnCONNECT.setText("CONNECT");
                btnCONNECT.setTextColor(Color.BLACK);
                btnCONNECT.setEnabled(true);

                RadioButton bt = findViewById(R.id.radioButtonBluetooth);
                RadioButton ble = findViewById(R.id.radioButtonBLE);

                if(bt.isChecked())
                {
                    try {
                        if(mBTSocket.isConnected())
                        {
                            mBTSocket.close();
                            btSerialIsConnected = false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if(global_context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                       global_context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(global_context, "You have to allow access to device's location for BT scan", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!mBTAdapter.isEnabled()) {
                        Toast.makeText(getApplicationContext(), "You have to turn Bluetooth ON", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else
                    {
                        discoverBluetoothDevices("_BT");
                    }
                }
                else if(ble.isChecked())
                {
                    ble_port_close();
                    btBLEIsConnected = false;

                    if(global_context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        global_context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(global_context, "You have to allow access to device's location for BLE scan", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!mBTAdapter.isEnabled()) {
                        Toast.makeText(getApplicationContext(), "You have to turn Bluetooth ON", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else
                    {
                        discoverBluetoothDevices("_BLE");
                    }
                }
                else
                {
                    scanProgress.setVisibility(View.VISIBLE);

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
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            RadioButton bt = findViewById(R.id.radioButtonBluetooth);
            RadioButton ble = findViewById(R.id.radioButtonBLE);

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                if(bt.isChecked())
                {
                    try {
                        mBTSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String info = parent.getItemAtPosition(pos).toString();
                    final String address = info.substring(info.length() - 17);
                    final String name = info.substring(0,info.length() - 17);
                    ip_text.setText(address);
                    btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
                    btnCONNECT.setText("CONNECT");
                    btnCONNECT.setTextColor(Color.BLACK);
                    btnCONNECT.setEnabled(true);
                }
                else if(ble.isChecked())
                {
                    ble_port_close();
                    btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
                    btnCONNECT.setText("CONNECT");
                    btnCONNECT.setTextColor(Color.BLACK);
                    btnCONNECT.setEnabled(true);
                    BLE_MAC_ADDRESS = parent.getItemAtPosition(pos).toString();
                    BLE_DEVICE_NAME = BLE_MAC_ADDRESS.substring(0 , BLE_MAC_ADDRESS.indexOf(' ')).trim();
                    BLE_MAC_ADDRESS = BLE_MAC_ADDRESS.substring(BLE_MAC_ADDRESS.indexOf(' ')).trim();
                    ip_text.setText(BLE_MAC_ADDRESS);
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
                        btnCONNECT.setEnabled(true);
                        btSerialIsConnected = true;
                        scanProgress.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Connection failed, please try again", Toast.LENGTH_SHORT).show();
                        btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
                        btnCONNECT.setText("CONNECT");
                        btnCONNECT.setTextColor(Color.BLACK);
                        btnCONNECT.setEnabled(true);
                        btSerialIsConnected = false;
                        scanProgress.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
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
        try
        {
            StringBuilder sBuilder = new StringBuilder(byteArray.length * 2);
            for(byte b: byteArray)
                sBuilder.append(String.format("%02X", b & 0xff));
            return sBuilder.toString();
        }
        catch (Exception ex)
        {
            return "";
        }
    }

    public void OnRadioButtonTCPClicked(View view)
    {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton ble = findViewById(R.id.radioButtonBLE);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(ble.isChecked())
        {
            ble_port_close();
        }

        if(bt.isChecked() || ble.isChecked())
        {
            ip_text.setText("");
            spinner.setAdapter(null);
            listDevices.clear();
            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
            btnCONNECT.setEnabled(true);
        }

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        udp.setChecked(false);
        http.setChecked(false);
        bt.setChecked(false);
        ble.setChecked(false);
    }

    public void OnRadioButtonUDPClicked(View view)
    {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton ble = findViewById(R.id.radioButtonBLE);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(ble.isChecked())
        {
            ble_port_close();
        }

        if(bt.isChecked() || ble.isChecked())
        {
            ip_text.setText("");
            spinner.setAdapter(null);
            listDevices.clear();
            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
            btnCONNECT.setEnabled(true);
        }

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        ble.setChecked(false);
        http.setChecked(false);
        tcp.setChecked(false);
        bt.setChecked(false);
    }

    public void OnRadioButtonHTTPClicked(View view)
    {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton ble = findViewById(R.id.radioButtonBLE);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(ble.isChecked())
        {
            ble_port_close();
        }

        if(bt.isChecked() || ble.isChecked())
        {
            ip_text.setText("");
            spinner.setAdapter(null);
            listDevices.clear();
            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
            btnCONNECT.setEnabled(true);
        }

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("80");
        portNumber.setInputType(0);

        udp.setChecked(false);
        tcp.setChecked(false);
        bt.setChecked(false);
        ble.setChecked(false);
    }

    public void OnRadioButtonBluetoothClicked(View view)
    {
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton ble = findViewById(R.id.radioButtonBLE);

        if(ble.isChecked())
        {
            ble_port_close();
            listDevices.clear();
        }

        spinner.setAdapter(null);

        btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
        btnCONNECT.setText("CONNECT");
        btnCONNECT.setTextColor(Color.BLACK);
        btnCONNECT.setEnabled(true);
        ip_text.setText("");

        http.setChecked(false);
        udp.setChecked(false);
        tcp.setChecked(false);
        ble.setChecked(false);

        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
    }

    public void OnRadioButtonBLEClicked(View view)
    {
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(http.isChecked() || udp.isChecked() || tcp. isChecked() || bt.isChecked())
        {
            ble_port_close();
            listDevices.clear();
        }

        http.setChecked(false);
        udp.setChecked(false);
        tcp.setChecked(false);
        bt.setChecked(false);

        spinner.setAdapter(null);

        btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
        btnCONNECT.setText("CONNECT");
        btnCONNECT.setTextColor(Color.BLACK);
        btnCONNECT.setEnabled(true);
        ip_text.setText("");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }

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
        RadioButton ble = findViewById(R.id.radioButtonBLE);

        if(bt.isChecked())
        {
            BT_GET_UID = true;
            byte[] byteArray1 = {0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
            if(mConnectedThread != null)
                mConnectedThread.write(byteArray1);
        }
        else if(ble.isChecked())
        {
            byte[] GET_UID = {0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};

            ble_port_write(GET_UID);

            try
            {

                byte[] resp = ble_port_read(18);
                String readMessage = "";

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
            }
            catch (Exception e){};
        }
        else
        {
            DoOperation();
        }
    }

    public void onLightClicked(View view)
    {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);
        RadioButton ble = findViewById(R.id.radioButtonBLE);

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
        else if(ble.isChecked())
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

            ble_port_write(byteArray1);
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
        RadioButton ble = findViewById(R.id.radioButtonBLE);

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
        else if(ble.isChecked())
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

                BT_CMDbuffer = hexStringToByteArray(bytesToHex(calculated_crc));
            }
            else
            {
                BT_CMDbuffer = hexStringToByteArray(cmdStr);
            }

            ble_port_write(BT_CMDbuffer);

            CmdResponse.setText(bytesToHex(ble_port_read(256)));
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
        if(btSerialIsConnected == true)
        {
            try
            {
                if(mBTSocket.isConnected())
                {
                    try {
                        mBTSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (Exception ex){}

            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
            btnCONNECT.setEnabled(true);
            btSerialIsConnected = false;
            return;
        }
        else if(btBLEIsConnected == true)
        {
            ble_port_close();

            btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
            btnCONNECT.setText("CONNECT");
            btnCONNECT.setTextColor(Color.BLACK);
            btnCONNECT.setEnabled(true);
            btBLEIsConnected = false;
            return;
        }

        scanProgress.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        RadioButton bt = findViewById(R.id.radioButtonBluetooth);
        RadioButton ble = findViewById(R.id.radioButtonBLE);

        if(bt.isChecked()) {

            if (!mBTAdapter.isEnabled()) {
                makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                mBTSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            toastForConnect = makeText(this, "Connecting ... Please wait", Toast.LENGTH_SHORT);

            CountDownTimer toastCountDown;
            toastCountDown = new CountDownTimer(12000, 1000 /*Tick duration*/) {
                public void onTick(long millisUntilFinished) {
                    if (isBTConnected) {
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

            try {
                info = spinner.getSelectedItem().toString();
            } catch (Exception ex) {
            }

            String name_info = "";
            try {
                name_info = info.substring(0, info.length() - 17);
            } catch (Exception ex) {
            }

            final String name = name_info;

            new Thread() {
                public void run() {
                    boolean fail = false;
                    BluetoothDevice device = null;

                    try {
                        device = mBTAdapter.getRemoteDevice(ip_text.getText().toString().trim());
                    } catch (Exception ex) {
                        return;
                    }

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
                    if (fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();

        }
        else if(ble.isChecked())
        {
            ble_port_close();

            if(ble_port_open(BLE_MAC_ADDRESS) == 1)
            {
                Toast.makeText(this, "Connected to uFR Online " + BLE_DEVICE_NAME, Toast.LENGTH_SHORT).show();
                btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pressed));
                btnCONNECT.setText("Connected");
                btnCONNECT.setTextColor(Color.WHITE);
                btnCONNECT.setEnabled(true);
                btBLEIsConnected = true;
            }
            else
            {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(ble_port_open(BLE_MAC_ADDRESS) == 1)
                {
                    Toast.makeText(this, "Connected to uFR Online " + BLE_DEVICE_NAME, Toast.LENGTH_SHORT).show();
                    btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pressed));
                    btnCONNECT.setText("Connected");
                    btnCONNECT.setTextColor(Color.WHITE);
                    btnCONNECT.setEnabled(true);
                    btBLEIsConnected = true;
                }
                else {
                    Toast.makeText(this, "Connection failed, please try again", Toast.LENGTH_SHORT).show();
                    btnCONNECT.setBackground(ContextCompat.getDrawable(context, R.drawable.button_pattern));
                    btnCONNECT.setText("CONNECT");
                    btnCONNECT.setTextColor(Color.BLACK);
                    btnCONNECT.setEnabled(true);
                    btBLEIsConnected = false;
                }
            }

            scanProgress.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
    }

    public void discoverBluetoothDevices(final String suffix)
    {
        String OS_VERSION = Build.VERSION.RELEASE;
        int major = 0;
        int minor = 0;

        try
        {
            major = Integer.parseInt(OS_VERSION.substring(0, OS_VERSION.indexOf('.')));
            minor = Integer.parseInt(OS_VERSION.substring(OS_VERSION.indexOf('.') + 1, OS_VERSION.indexOf('.') + 2));
        }
        catch (Exception ex){minor = 0;}

        spinner.setAdapter(null);
        listDevices.clear();

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        for(BluetoothDevice bt : pairedDevices)
        {
            try
            {
                if(bt.getName().startsWith("ON") && bt.getName().endsWith(suffix))
                {
                    listDevices.add(bt.getName() + " " + bt.getAddress());
                }
            }
            catch (Exception ex){}
        }

        if(major == 9 && minor > 0)
        {
            scanProgress.setVisibility(View.VISIBLE);

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            mBTAdapter.startDiscovery();

            new Thread() {
                public void run() {
                    int counter = 0;
                    while(true)
                    {
                        counter++;

                        if(counter == 10)
                        {
                            mBTAdapter.cancelDiscovery();
                            break;
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

            BroadcastReceiver mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_FOUND.equals(action))
                    {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                        try
                        {
                            if(device.getName().startsWith("ON") && device.getName().endsWith(suffix))
                            {
                                listDevices.add(device.getName() + " " + device.getAddress());
                            }
                        }
                        catch (Exception ex){}
                    }
                    else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                    {
                        LinkedHashSet<String> hashSet = new LinkedHashSet<>(listDevices);
                        listWithoutDuplicates = new ArrayList<>(hashSet);

                        if(listWithoutDuplicates.size() > 0)
                        {
                            spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_dropdown_item_1line,listWithoutDuplicates));
                        }
                        else
                        {
                            spinner.setAdapter(null);
                            Toast.makeText(getApplicationContext(), "No devices found, please try again", Toast.LENGTH_SHORT).show();
                        }

                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        scanProgress.setVisibility(View.GONE);
                    }
                }
            };

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
        }
        else
        {
            if(listDevices.size() > 0)
            {
                spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_dropdown_item_1line,listDevices));
            }
            else
            {
                Toast.makeText(getApplicationContext(), "No paired devices found", Toast.LENGTH_SHORT).show();
            }
        }
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

    //------------------------------------------------------------------------
    //Bluetooth Low Energy
    //------------------------------------------------------------------------

    public static int ble_port_open(String mac) {

        if (Neatle.isMacValid(mac)) {
            if (connectionMonitor != null) {
                if (connectionMonitor.getConnection().isConnecting()) {
                    connectionMonitor.getConnection().disconnect();

                }
            }

            connectionMonitor = Neatle.createConnectionMonitor(global_context, Neatle.getDevice(mac));

            connectionMonitor.setKeepAlive(true);
            connectionMonitor.start();

            connectionMonitor.getConnection().connect();

            int state = connectionMonitor.getConnection().getState();
            long startTime = System.currentTimeMillis();

            if (state == 2) {
                device = connectionMonitor.getDevice();
                return 1;
            }
            else
            {
                return 0;
            }
        } else {

            return 0;
        }

    }

    public static int ble_port_close() {

        if (connectionMonitor != null) {
            connectionMonitor.getConnection().disconnect();
            connectionMonitor.stop();
            connectionMonitor=null;
        }
        return 1;
    }

    public static int ble_port_write(final byte data[]) {
        if(connectionMonitor !=null) {
            if (connectionMonitor.getConnection().getState() == 2) {
                write_bytes_done = 0;
                HandlerThread handlerThread1 = new HandlerThread("MyHandlerThread1");
                handlerThread1.start();
                Looper looper1 = handlerThread1.getLooper();
                Handler handler1 = new Handler(looper1);

                handler1.post(new Runnable() {

                    @Override
                    public void run() {
                        ByteArrayInputSource inputSource = new ByteArrayInputSource(data);

                        Operation operation = Neatle.createOperationBuilder(global_context)
                                .write(serviceToWrite, characteristicToWrite, inputSource)
                                .onFinished(new SimpleOperationObserver() {
                                    @Override
                                    public void onOperationFinished(Operation op, OperationResults results) {
                                        if (results.wasSuccessful()) {
                                            finishedWrite = true;
                                            write_bytes_done = data.length;

                                        } else {
                                            finishedWrite = true;
                                        }
                                    }
                                })
                                .build(device);
                        operation.execute();
                    }
                });

                while (finishedWrite == false) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                finishedWrite = false;
                return data.length;
            } else {
                return 0;
            }
        }
        else
        {
            return 0;
        }
    }


    public static byte[] ble_port_read(int number_of_bytes) {
        if(connectionMonitor!=null) {
            if (connectionMonitor.getConnection().getState() == 2) {

                byte len[] = new byte[6];
                len[0] = 'L';
                len[1] = 'E';
                len[2] = 'N';
                len[3] = '=';
                len[5] = (byte) (number_of_bytes & 0xFF);
                len[4] = (byte) ((number_of_bytes >> 8) & 0xFF);
                ble_port_write(len);

                data = null;
                HandlerThread handlerThread1 = new HandlerThread("MyHandlerThread1");
                handlerThread1.start();
                Looper looper1 = handlerThread1.getLooper();
                Handler handler1 = new Handler(looper1);

                handler1.post(new Runnable() {

                    @Override
                    public void run() {
                    si.inova.neatle.operation.Operation operation = Neatle.createOperationBuilder(global_context)
                                .read(serviceToWrite, characteristicToWrite)
                                .onFinished(new SimpleOperationObserver() {
                                    @Override
                                    public void onOperationFinished( si.inova.neatle.operation.Operation op, OperationResults results) {
                                        if (results.wasSuccessful()) {
                                            //Log.e(TAG, "READ onOperationFinished: " +results.getResult(characteristicToWrite).getValueAsString());

                                            data = results.getResult(characteristicToWrite).getValue();

                                            finishedRead = true;
                                        } else {

                                            //Log.e(TAG, "onOperationFinished: READFING ERROR");
                                            finishedRead = true;
                                        }
                                    }
                                })
                                .build(device);
                        operation.execute();
                    }
                });

                while (finishedRead == false) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                finishedRead = false;


                return data;
            } else {
                data = null;
                return data;
            }
        }
        else
        {
            data = null;
            return data;
        }
    }
}
