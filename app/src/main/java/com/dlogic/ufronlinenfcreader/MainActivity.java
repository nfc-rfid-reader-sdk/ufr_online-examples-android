package com.dlogic.ufronlinenfcreader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import com.dlogic.ufronlinenfcreader.BluetoothActivity.*;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dlogic.ufronlinenfcreader.BluetoothActivity.mConnectedThread;
import static com.dlogic.ufronlinenfcreader.BluetoothActivity.mmOutStream;

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

    public static Broadcast broadcastOperation = null;
    public static UDP udpCmd = null;
    public static HTTP httpCmd = null;
    public static TCP tcpCmd = null;

    public static boolean BT_GET_UID = false;
    public static boolean BT_SEND_CMD = false;

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

        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        List<String> list = new ArrayList<String>();
        list.add("");
        spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_dropdown_item_1line,list));
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

        udp.setChecked(false);
        http.setChecked(false);
        tcp.setChecked(true);
        bt.setChecked(false);
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

        udp.setChecked(true);
        http.setChecked(false);
        tcp.setChecked(false);
        bt.setChecked(false);
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

        udp.setChecked(false);
        http.setChecked(true);
        tcp.setChecked(false);
        bt.setChecked(false);
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
            byte[] byteArray1 = { 0x55, 0x2C, (byte)0xAA, 0x00, 0x00, 0x00, (byte)0xDA};
            if(mConnectedThread != null)
                try {
                    mmOutStream.write(byteArray1);
                } catch (IOException e) { }
        }
        else {
            DoOperation();
        }
    }

    public void onLightClicked(View view) {
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        if (bt.isChecked())
        {
            byte[] byteArray1 = {0x55, 0x26, (byte)0xAA, 0x00, 0x03, 0x05, (byte)0xE6};
            if(mConnectedThread != null)
                try {
                    mmOutStream.write(byteArray1);
                } catch (IOException e) { }
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
            BT_SEND_CMD = true;
            byte[] BT_Cmd;
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

                BT_Cmd = hexStringToByteArray(bytesToHex(calculated_crc));
            }
            else
            {
                BT_Cmd = hexStringToByteArray(cmdStr);
            }

            if(mConnectedThread != null)
                try {
                    mmOutStream.write(BT_Cmd);
                } catch (IOException e) { }
        }
        else
        {
            isCommand = true;
            DoOperation();
        }
    }

    public void OnRadioButtonBluetoothClicked(View view)
    {
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);
        RadioButton bt = findViewById(R.id.radioButtonBluetooth);

        http.setChecked(false);
        udp.setChecked(false);
        tcp.setChecked(false);

        startActivity(new Intent(getApplicationContext(), BluetoothActivity.class));
    }

}
