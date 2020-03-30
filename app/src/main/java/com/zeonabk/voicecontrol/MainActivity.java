package com.zeonabk.voicecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextToSpeech textToSpeech;
    private TextView txvResult;
    Button listDevices;
    ListView listView;
    TextView msg_box,status;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;
    String address;
    int a = 0;
    String text = "Connected";
    SendReceive sendReceive;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;

    int REQUEST_ENABLE_BLUETOOTH=1;

    private static final String APP_NAME = "BTChat";
    private static final UUID MY_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewByIdes();
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        address = bluetoothAdapter.getAddress();
        BluetoothDevice hc05 = bluetoothAdapter.getRemoteDevice(address);

        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
        }

        implementListeners();

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int lang = textToSpeech.setLanguage(Locale.getDefault());
                }

            }
        });
    }

    public void implementListeners() {

        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textToSpeech.speak("list devices", TextToSpeech.QUEUE_FLUSH, null);
                Set<BluetoothDevice> bt=bluetoothAdapter.getBondedDevices();
                String[] strings=new String[bt.size()];
                btArray=new BluetoothDevice[bt.size()];
                int index=0;

                if( bt.size()>0)
                {
                    for(BluetoothDevice device : bt)
                    {
                        btArray[index]= device;
                        strings[index]=device.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ServerClass serverClass=new ServerClass();
                serverClass.start();
                ClientClass clientClass=new ClientClass(btArray[i]);
                clientClass.start();

                status.setText("Connecting");
            }
        });
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_LISTENING:
                    status.setText("Listening");
                    textToSpeech.speak("bluetooth starts listening", TextToSpeech.QUEUE_FLUSH, null);
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    status.setTextColor(Color.parseColor("#FFA500"));
                    textToSpeech.speak("bluetooth starts connecting", TextToSpeech.QUEUE_FLUSH, null);
                    a = 0 ;
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    status.setTextColor(Color.parseColor("#8FBC8F"));
                     textToSpeech.speak("bluetooth is now connected", TextToSpeech.QUEUE_FLUSH, null);
                     a = 1 ;
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    status.setTextColor(Color.parseColor("#FF4081"));
                    textToSpeech.speak("connect bluetooth again", TextToSpeech.QUEUE_FLUSH, null);
                    a = 0 ;
                break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    msg_box.setText(tempMsg);
                    textToSpeech.speak(tempMsg, TextToSpeech.QUEUE_FLUSH, null);

                    break;
            }
            return true;
        }
    });

    private void findViewByIdes() {
        listView=(ListView) findViewById(R.id.listview);
        msg_box =(TextView) findViewById(R.id.msg);
        status=(TextView) findViewById(R.id.status);
        listDevices=(Button) findViewById(R.id.listDevices);
        txvResult = (TextView) findViewById(R.id.txvResult);
    }
    public void getSpeechInput(View view) {
        if(a == 0)
        {
            textToSpeech.speak("please connect bluetooth then click here", TextToSpeech.QUEUE_FLUSH, null);
            }

        if (a == 1) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, 10);
            } else {
                Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txvResult.setText(result.get(0));
                        String string= String.valueOf(txvResult.getText());
                        sendReceive.write(string.getBytes());
                    }

                break;
        }
    }

    private class ServerClass extends Thread
    {
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            try {
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            BluetoothSocket socket=null;

            while (socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive=new SendReceive(socket);
                    sendReceive.start();

                    break;
                }
            }
        }
    }
    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1)
        {
            device=device1;

            try {
                socket=device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            try {
                socket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive=new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run()
        {
            byte[] buffer=new byte[1024];
            int bytes;

            while (true)
            {
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
