package com.dlogic.ufronlinenfcreader;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.http.client.methods.HttpPost;

public class MainActivity extends Activity {

    static Context context;
    public static String resp = "";
    public static String server_address = "192.168.0.101";
    public static Integer server_port = 8881;
    public static Spinner spinner;
    public static TextView response;
    public static TextView CmdResponse;
    public static EditText port_text;
    public static EditText cmdText;
    public static Boolean Abort = false;
    public static boolean isBeep = false;
    public static boolean isLight = false;
    public static boolean isCommand = false;
    public static byte[] cmdBuffer = {0x55, 0x2C, (byte) 0xAA, 0x00, 0x00, 0x00, (byte) 0xDA};
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

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        udp.setChecked(false);
        http.setChecked(false);
        tcp.setChecked(true);
    }

    public void OnRadioButtonUDPClicked(View view)
    {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("8881");
        portNumber.setInputType(1);

        udp.setChecked(true);
        http.setChecked(false);
        tcp.setChecked(false);
    }

    public void OnRadioButtonHTTPClicked(View view)
    {
        TextView uidTV = findViewById(R.id.txtUID);
        uidTV.setText("");
        RadioButton http = findViewById(R.id.radioButtonHTTP);
        RadioButton udp = findViewById(R.id.radioButtonUDP);
        RadioButton tcp = findViewById(R.id.radioButtonTCP);

        EditText portNumber = findViewById(R.id.portText);

        portNumber.setText("80");
        portNumber.setInputType(0);

        udp.setChecked(false);
        http.setChecked(true);
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
        DoOperation();
    }

    public void onBeepClicked(View view)
    {
        isBeep = true;
        DoOperation();
        isBeep = false;
    }

    public void onLightClicked(View view)
    {
        isLight = true;
        DoOperation();
        isLight = false;
    }

    public void onSendCommandClicked(View view)
    {
        isCommand = true;
        DoOperation();
    }
}