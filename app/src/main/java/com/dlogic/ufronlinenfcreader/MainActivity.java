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
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.client.methods.HttpPost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class MainActivity extends Activity {

    //Bluetooth
    //---------------------------------------
    BluetoothAdapter mBluetoothAdapter;
    List<String> listDevices;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    private BluetoothSocket mBTSocket = null;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream mmInStream = null;
    private OutputStream mmOutStream = null;
    byte[] BTbuffer = new byte[256];
    //---------------------------------------

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

    public static Broadcast broadcastOperation = null;
    public static UDP udpCmd = null;
    public static HTTP httpCmd = null;
    public static TCP tcpCmd = null;

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
        listDevices = new ArrayList<String>();

        mBTDevices = new ArrayList<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            RadioButton bt = findViewById(R.id.radioButtonBluetooth);

            public void onItemSelected(AdapterView<?> parent, View view, int i, long id) {
                if(bt.isChecked())
                {
                    mBluetoothAdapter.cancelDiscovery();

                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                        mBTDevices.get(i).createBond();
                    }

                    connectToBluetoothDevice(mBTDevices.get(i));
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
                if(bt.isChecked())
                {
                    mBluetoothAdapter.cancelDiscovery();

                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                        mBTDevices.get(0).createBond();
                    }

                    connectToBluetoothDevice(mBTDevices.get(0));
                }
            }
        });
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

    public void OnScanClicked(View view)
    {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if(bt.isChecked())
        {
            discoverBluetoothDevices();
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
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        if(bt.isChecked())
        {
            spinner.setAdapter(null);
        }

        udp.setChecked(false);
        http.setChecked(false);
        tcp.setChecked(true);
        bt.setChecked(false);

        if(mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

        try {
            if(mBTSocket.isConnected())
            {
                mBTSocket.close();
            }
        } catch (IOException e) { }
    }

    public void OnRadioButtonUDPClicked(View view)
    {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        if(bt.isChecked())
        {
            spinner.setAdapter(null);
        }

        udp.setChecked(true);
        http.setChecked(false);
        tcp.setChecked(false);
        bt.setChecked(false);

        if(mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

        try {
            if(mBTSocket.isConnected())
            {
                mBTSocket.close();
            }
        } catch (IOException e) { }
    }

    public void OnRadioButtonHTTPClicked(View view)
    {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("80");
        portNumber.setInputType(0);

        if(bt.isChecked())
        {
            spinner.setAdapter(null);
        }

        udp.setChecked(false);
        http.setChecked(true);
        tcp.setChecked(false);
        bt.setChecked(false);

        if(mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

        try {
            if(mBTSocket.isConnected())
            {
                mBTSocket.close();
            }
        } catch (IOException e) { }
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
            byte[] input = new byte[]{0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
            writeBluetoothSerial(input);
            readBluetoothSerial();

            if(BTbuffer[1] == 0x08)
            {
                response.setText("NO CARD");
            }
            else
            {
                String BT_UID = bytesToHex(BTbuffer);
                response.setText(BT_UID.substring(14, 14 + BTbuffer[5] * 2));
            }
        }
        else
        {
            DoOperation();
        }
    }

    public void onLightClicked(View view) {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if (bt.isChecked())
        {
            byte[] UISignal = new byte[]{0x55, 0x26, (byte) 0xAA, 0x00, 0x03, 0x05, (byte) 0xE6};
            writeBluetoothSerial(UISignal);
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
        byte[] BT_CMD = new byte[300];

        if(bt.isChecked())
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

                BT_CMD = hexStringToByteArray(bytesToHex(calculated_crc));
            }
            else
            {
                BT_CMD = hexStringToByteArray(cmdStr);
            }

            writeBluetoothSerial(BT_CMD);
            readBluetoothSerial();

            CmdResponse.setText(bytesToHex(BTbuffer));
        }
        else
        {
            isCommand = true;
            DoOperation();
        }
    }


    //--------------------------------------------------------------
    //Bluetooth implementation
    //--------------------------------------------------------------

    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                }
            }
        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        //mBluetoothAdapter.cancelDiscovery();
    }

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    public void OnRadioButtonBluetoothClicked(View view)
    {
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);

        if(http.isChecked() || udp.isChecked() || tcp.isChecked())
        {
            spinner.setAdapter(null);
        }

        http.setChecked(false);
        udp.setChecked(false);
        tcp.setChecked(false);

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
    }

    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);

                if(device.getName().startsWith("ON"))
                {
                    listDevices.add(device.getName() + " " + device.getAddress());
                    mBTDevices.add(device);
                }

                spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_dropdown_item_1line,listDevices));
            }
        }
    };

    public void discoverBluetoothDevices() {

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
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

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
        }
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    public void connectToBluetoothDevice(BluetoothDevice device)
    {
        try {
            mBTSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
        }

        try {

            if(mBTSocket.isConnected())
            {
                mBTSocket.close();
            }
            mBTSocket.connect();
        } catch (IOException e) {
            try {
                mBTSocket.close();
            } catch (IOException e2) {
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
            }
        }

        try {
            mmInStream = mBTSocket.getInputStream();
            mmOutStream = mBTSocket.getOutputStream();

            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();

        } catch (IOException e) { }
    }

    public void readBluetoothSerial()
    {
        int bytes;
        while (true) {
            try {
                bytes = mmInStream.available();
                if(bytes != 0) {
                    SystemClock.sleep(100);
                    bytes = mmInStream.available();
                    BTbuffer = new byte[bytes];
                    mmInStream.read(BTbuffer, 0, bytes);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void writeBluetoothSerial(byte[] input)
    {
        try {
            mmOutStream.write(input);
        } catch (IOException e) { }
    }
}
