package com.example.bltlampu;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    private TextView mBluetoothStatus;
    private Button mBluetoothOnButton;
    private Button mBluetoothOffButton;
    private Button gedung_A_on,gedung_A_off,gedung_B_on,gedung_B_off,gedung_C_on,gedung_C_off,
            gedung_D_on,gedung_D_off,gedung_All_on,gedung_All_off;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private Switch mPowerLock;

    private final String TAG = MainActivity.class.getSimpleName();
    private Handler mHandler; // Our main handler that will receive callback notifications
    private BluetoothConnection mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private final static String G_A_ON = "a";
    private final static String G_A_OFF = "b";
    private final static String G_B_ON = "c";
    private final static String G_B_OFF = "d";
    private final static String G_C_ON = "e";
    private final static String G_C_OFF="f";
    private final static String G_D_ON = "g";
    private final static String G_D_OFF = "h";
    private final static String All_G_ON = "i";
    private final static String All_G_OFF = "j";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = findViewById(R.id.bluetoothStatus);
        mBluetoothOnButton = findViewById(R.id.btnBluetoothOn);
        mBluetoothOffButton = findViewById(R.id.btnBluetoothOff);
        gedung_A_on = findViewById(R.id.btnAon);
        gedung_A_off = findViewById(R.id.btnAoff);
        gedung_B_on = findViewById(R.id.btnBon);
        gedung_B_off = findViewById(R.id.btnBoff);
        gedung_C_on = findViewById(R.id.btnCon);
        gedung_C_off = findViewById(R.id.btnCoff);
        gedung_D_on = findViewById(R.id.btnDon);
        gedung_D_off = findViewById(R.id.btnDoff);
        gedung_All_on = findViewById(R.id.btnAllon);
        gedung_All_off = findViewById(R.id.btnAlloff);
        mPowerLock = findViewById(R.id.switch1);

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);


        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }
        };

        // Door lock/unlock event handlers and validations
        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            mPowerLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(mConnectedThread != null) //First check to make sure thread created
                    {
                        if(mPowerLock.isChecked())
                            mConnectedThread.write(G_A_ON);
                        else
                            mConnectedThread.write(G_A_OFF);
                    }
                }
            });

            // Bluetooth adapter power on listener
            mBluetoothOnButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            // bluetooth adapter power off listener
            mBluetoothOffButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff(v);
                }
            });

            // Blink car light event listener
            gedung_A_on.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    A_on(v);
                }
            });

            gedung_A_off.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    A_off(v);
                }
            });

            gedung_B_on.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    B_on(v);
                }
            });

            gedung_B_off.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    B_off(v);
                }
            });

            gedung_C_on.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    C_on(v);
                }
            });

            gedung_C_off.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    C_off(v);
                }
            });

            gedung_D_on.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    D_on(v);
                }
            });

            gedung_D_off.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    D_off(v);
                }
            });

            gedung_All_on.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    allOn(v);
                }
            });

            gedung_All_off.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    allOff(v);
                }
            });
        }
    }

    private void A_on(View v) {

        if(mConnectedThread != null) //First check to make sure thread created
        {
            mConnectedThread.write(G_A_ON);
        }
    }
    private void A_off(View v){
        if (mConnectedThread !=null)
        {
            mConnectedThread.write(G_A_OFF);
        }
    }
    private void B_on(View v) {

        if(mConnectedThread != null) //First check to make sure thread created
        {
            mConnectedThread.write(G_B_ON);
        }
    }
    private void B_off(View v){
        if (mConnectedThread !=null)
        {
            mConnectedThread.write(G_B_OFF);
        }
    }
    private void C_on(View v) {

        if(mConnectedThread != null) //First check to make sure thread created
        {
            mConnectedThread.write(G_C_ON);
        }
    }
    private void C_off(View v){
        if (mConnectedThread !=null)
        {
            mConnectedThread.write(G_C_OFF);
        }
    }
    private void D_on(View v) {

        if(mConnectedThread != null) //First check to make sure thread created
        {
            mConnectedThread.write(G_D_ON);
        }
    }
    private void D_off(View v){
        if (mConnectedThread !=null)
        {
            mConnectedThread.write(G_D_OFF);
        }
    }
    private void allOn(View v) {

        if(mConnectedThread != null) //First check to make sure thread created
        {
            mConnectedThread.write(All_G_ON);
        }
    }
    private void allOff(View v){
        if (mConnectedThread !=null)
        {
            mConnectedThread.write(All_G_OFF);
        }
    }

    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

            listPairedDevices(view);

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
            listPairedDevices(view);
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Enabled");
            } else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };


    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
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
                    if(!fail) {
                        mConnectedThread = new BluetoothConnection(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    // Bluetooth Communication class
    // Responsible for establishing connection, error control and transmission
    private class BluetoothConnection extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BluetoothConnection(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        buffer = new byte[1024];
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
