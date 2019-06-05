package com.dlogic.ufronlinenfcreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
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
import java.util.List;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {

    public static Context context;
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
    public static byte[] BT_cmdBuffer = new byte[4096];
    public static String cmdStr = "552CAA000000DA";
    public static HttpPost httppost = null;

    public static Broadcast broadcastOperation = null;
    public static UDP udpCmd = null;
    public static HTTP httpCmd = null;
    public static TCP tcpCmd = null;

    private ConnectedThread mConnectedThread;
    private Handler mHandler;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBTAdapter;
    private BluetoothSocket mBTSocket = null;
    List<String> listBluetoothDevices = new ArrayList<String>();

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;
    Button blButton;
    boolean BT_GET_UID = false;
    boolean BT_SEND_COMMAND = false;

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
        blButton = findViewById(R.id.radioButtonBluetooth);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        spinner.setOnItemSelectedListener(spinnerSelect);

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String readMessage = null;
                    byte resp[] = (byte[]) msg.obj;

                    if (BT_GET_UID) {
                        if (resp[0] == (byte) 0xDE) {
                            if (resp[1] == 0x2C) {
                                byte uid[] = new byte[resp[5]];

                                System.arraycopy(resp, 7, uid, 0, resp[5]);
                                readMessage = bytesToHex(uid);
                            }
                        } else {
                            if (resp[1] == 0x08) {
                                readMessage = "NO CARD";
                            }
                        }

                        response.setText(readMessage);

                        BT_GET_UID = false;
                    } else if (BT_SEND_COMMAND) {
                        /*if(resp[0] == 0xAC)
                        {
                            byte[] tempResp = new byte[7];
                            System.arraycopy(resp, 0, tempResp, 0, 7);
                            CmdResponse.setText(bytesToHex(tempResp));
                        }
                        else if(resp[3] > 0)
                        {
                            byte[] tempResp = new byte[resp[3]];
                            System.arraycopy(resp, 0, tempResp, 0, resp[3]);
                            CmdResponse.setText(bytesToHex(tempResp));
                        }
                        else
                        {
                            byte[] tempResp = new byte[7];
                            System.arraycopy(resp, 0, tempResp, 0, 7);
                            CmdResponse.setText(bytesToHex(tempResp));
                        }*/
                        CmdResponse.setText(bytesToHex(resp));
                        BT_SEND_COMMAND = false;
                    }
                }

                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1) {
                        Toast.makeText(getBaseContext(), "Connected to uFR Online: " + (String) (msg.obj), Toast.LENGTH_SHORT).show();
                        // mBTArrayAdapter.clear();
                    } else {
                        Toast.makeText(getBaseContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }

    public static boolean isHexChar(char c) {
        char hexChars[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'x',
                'A', 'B', 'C', 'D', 'E', 'F', 'X'};

        for (int i = 0; i < 24; i++) {
            if (c == hexChars[i]) {
                return true;
            }
        }

        return false;
    }

    public static String eraseDelimiters(String hex_str) {
        for (int i = 0; i < hex_str.length(); i++) {
            if (!isHexChar(hex_str.charAt(i))) {
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

    public static String bytesToHex(byte[] byteArray) {
        StringBuilder sBuilder = new StringBuilder(byteArray.length * 2);
        for (byte b : byteArray)
            sBuilder.append(String.format("%02X", b & 0xff));
        return sBuilder.toString();
    }

    public void OnScanClicked(View view) {
        spinner.setAdapter(null);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if (bt.isChecked()) {
            scanForBluetoothDevices();
        } else {
            try {
                if (Abort == true) {
                    broadcastOperation.cancel(false);
                } else {
                    broadcastOperation = new Broadcast();
                    broadcastOperation.execute();
                }
                Abort = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void OnRadioButtonTCPClicked(View view) {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        if (bt.isChecked()) {
            spinner.setAdapter(null);
        }

        udp.setChecked(false);
        http.setChecked(false);
        tcp.setChecked(true);
        bt.setChecked(false);
    }

    public void OnRadioButtonUDPClicked(View view) {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        if (bt.isChecked()) {
            spinner.setAdapter(null);
        }

        udp.setChecked(true);
        http.setChecked(false);
        tcp.setChecked(false);
        bt.setChecked(false);
    }

    public void OnRadioButtonHTTPClicked(View view) {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("80");
        portNumber.setInputType(0);

        if (bt.isChecked()) {
            spinner.setAdapter(null);
        }

        udp.setChecked(false);
        http.setChecked(true);
        tcp.setChecked(false);
        bt.setChecked(false);
    }

    public void DoOperation() {
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);

        if (udp.isChecked()) {
            try {
                if (Abort == true) {
                    udpCmd.cancel(false);
                } else {
                    udpCmd = new UDP();
                    udpCmd.execute();
                }
                Abort = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (http.isChecked()) {
            try {
                if (Abort == true) {
                    httpCmd.cancel(false);
                } else {
                    httpCmd = new HTTP();
                    httpCmd.execute();
                }
                Abort = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (tcp.isChecked()) {
            try {
                if (Abort == true) {
                    tcpCmd.cancel(false);
                } else {
                    tcpCmd = new TCP();
                    tcpCmd.execute();
                }
                Abort = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public AdapterView.OnItemSelectedListener spinnerSelect = new AdapterView.OnItemSelectedListener() {

        String address = "";
        String name = "";

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            RadioButton bt = findViewById(R.id.radioButtonBluetooth);

            if (bt.isChecked()) {
                if (!mBTAdapter.isEnabled()) {
                    Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                    return;
                }

                String info = spinner.getItemAtPosition(position).toString();
                address = info.substring(info.length() - 17);
                name = info.substring(0, info.length() - 17);

                new Thread() {
                    public void run() {
                        boolean fail = false;

                        BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                        unpairDevice(device);

                        try {
                            mBTSocket = createBluetoothSocket(device);
                        } catch (IOException e) {
                            fail = true;
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                        try {
                            mBTSocket.connect();
                        } catch (IOException e) {
                            try {
                                fail = true;
                                mBTSocket.close();
                                mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                        .sendToTarget();
                            } catch (IOException e2) {
                                //insert code to deal with this
                                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
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
        }

        public void onNothingSelected(AdapterView<?> parent) {
            RadioButton bt = findViewById(R.id.radioButtonBluetooth);

            if (bt.isChecked()) {
                if (!mBTAdapter.isEnabled()) {
                    Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                    return;
                }

                String info = spinner.getItemAtPosition(0).toString();
                address = info.substring(info.length() - 17);
                name = info.substring(0, info.length() - 17);

                new Thread() {
                    public void run() {
                        boolean fail = false;

                        BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                        unpairDevice(device);

                        try {
                            mBTSocket = createBluetoothSocket(device);
                        } catch (IOException e) {
                            fail = true;
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                        try {
                            mBTSocket.connect();
                        } catch (IOException e) {
                            try {
                                fail = true;
                                mBTSocket.close();
                                mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                        .sendToTarget();
                            } catch (IOException e2) {
                                //insert code to deal with this
                                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
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
        }
    };

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) { }
    }

    public void OnButtonGetUIDClick(View view)
    {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
        {
            BT_GET_UID = true;

            byte[] byteArray1 = { 0x55, 0x2C, (byte)0xAA, 0x00, 0x00, 0x00, (byte)0xDA};
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
            byte[] byteArray1 = { 0x55, 0x26, (byte)0xAA, 0x00, 0x01, 0x01, (byte)0xE0};
            if(mConnectedThread != null)
                mConnectedThread.write(byteArray1);
        }
        else
        {
            isLight = true;
            DoOperation();
            isLight = false;
        }
    }

    public void onSendCommandClicked(View view)
    {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
        {
            BT_SEND_COMMAND = true;

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

                BT_cmdBuffer = hexStringToByteArray(bytesToHex(calculated_crc));
            }
            else
            {
                BT_cmdBuffer = hexStringToByteArray(cmdStr);
            }

            if(mConnectedThread != null)
                mConnectedThread.write(BT_cmdBuffer);
        }
        else
        {
            isCommand = true;
            DoOperation();
        }
    }

    //-----------------------------------------------------------------------------------------------
    //Bluetooth
    //-----------------------------------------------------------------------------------------------

    public void onRadioButtonBluetoothClicked(View view)
    {
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(udp.isChecked() || http.isChecked() || tcp.isChecked())
        {
            spinner.setAdapter(null);
        }

        udp.setChecked(false);
        http.setChecked(false);
        tcp.setChecked(false);
        bt.setChecked(true);

        if (!mBTAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {

                Toast.makeText(getApplicationContext(),"Bluetooth enabled",Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(getApplicationContext(),"Bluetooth disabled",Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
        }
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
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
                        buffer = new byte[256];
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
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

    //----------------------------------------------------------------------------------
    //Bluetooth discover
    //----------------------------------------------------------------------------------

    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                if(device.getName().startsWith("ON"))
                {
                    listBluetoothDevices.add(device.getName() + " " + device.getAddress());
                }
                spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_dropdown_item_1line,listBluetoothDevices));
            }
        }
    };

    public void scanForBluetoothDevices() {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBTAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBTAdapter.isDiscovering()){

            checkBTPermissions();

            mBTAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
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
}
