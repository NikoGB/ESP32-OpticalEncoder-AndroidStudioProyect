package com.example.finalencoder_controller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.harrysoft.androidbluetoothserial.BluetoothManager;

import com.example.finalencoder_controller.databinding.FragmentConnectionBinding;

import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ConnectionFragment extends Fragment {

    // variables for the bluetooth connection and the device interface
    public String devName = "", devMac = "";
    private FragmentConnectionBinding binding;
    private BluetoothManager bluetoothManager = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConnectionBinding.inflate(inflater, container, false);

        // set the onclick listener for the find device button
        binding.findDeviceButton.setOnClickListener(view -> {
            // check if the bluetooth is connected
            ControlCenter.getInstance().bluetoothConnected = android.bluetooth.BluetoothAdapter.getDefaultAdapter().isEnabled();
            // check if the bluetooth is connected and the bluetooth manager is not null
            if(ControlCenter.getInstance().bluetoothConnected && bluetoothManager != null){
                // show the devices
                showDevices();
                return;
            }

            // get the bluetooth manager instance
            bluetoothManager = BluetoothManager.getInstance();
            // check if the bluetooth manager is null
            if (bluetoothManager == null) {
                // show a toast message and return
                Toast.makeText(getContext(), "El Bluetooth no esta abilitado.", Toast.LENGTH_SHORT).show();
                return;
            }

            // check if the bluetooth is connected
            if (!ControlCenter.getInstance().bluetoothConnected) {
                // request the bluetooth to be enabled
                Intent enableBtIntent = new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
                int REQUEST_ENABLE_BT = 0;
                // start the activity for result
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                // check if the bluetooth is connected
                if (!ControlCenter.getInstance().bluetoothConnected) {
                    // show a toast message and return if the bluetooth is not connected
                    ControlCenter.getInstance().mainActivity.makeSnackB("Vuelva a intentar");
                    return;
                }
            }
            // show the devices
            showDevices();
        });

        // set the onclick listener for the connect button
        binding.devConsoleButton.setOnClickListener(view -> {
            // navigate to the console fragment
            ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_ContentMainFragment_to_debuggingConsoleFragment, "Consola serial");
        });

        return binding.getRoot();
    }

    // variables for the device interface
    private SimpleBluetoothDeviceInterface deviceInterface;

    // variables for the device info
    String[][] devInfo;

    // supress the missing permission warning
    @SuppressLint("MissingPermission")
    // method to show the device
    void showDevices(){
        // get the device list
        ListView deviceList = binding.deviceList;
        // get the paired devices
        Collection<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevicesList();
        // create the list to use in the adapter
        List<Map<String, String>> data = new ArrayList<>();
        // create the array to store the device info
        devInfo = new String[pairedDevices.size()][];

        // variables for the index
        int idx = 0;
        // loop through the paired devices
        for (BluetoothDevice device : pairedDevices) {
            // create the map to store the device info
            Map<String, String> datum = new HashMap<>(2);
            // store the device info
            devInfo[idx] = new String[]{ device.getName(), device.getAddress() };
            // add the device info to the map
            datum.put("", device.getName());
            datum.put("Mac", device.getAddress());
            // add the map to the list
            data.add(datum);
            // increment the index
            idx++;
        }

        // create the adapter
        SimpleAdapter devAdapter = new SimpleAdapter(getContext(), data, android.R.layout.simple_list_item_2,
                new String[] {"", "Mac"}, new int[] {android.R.id.text1, android.R.id.text2});

        // set the adapter to the list view
        deviceList.setAdapter(devAdapter);
        // set the onclick listener for the list view to connect to the device when clicked
        deviceList.setOnItemClickListener((adapterView, view, position, id) -> connectDevice(position));
        // notify the adapter that the data has changed
        devAdapter.notifyDataSetChanged();
    }

    // method called when a message is received
    private void receiveMsg(String s) {
        ControlCenter.getInstance().setReceivedMessage(s);

    }
    // method called when a message is sent
    private void sentMsg(String s) {
        ControlCenter.getInstance().setSentMessage(s);
    }
    // method to send a command to the device
    public void sendCommand(String cmd){
        if(!ControlCenter.getInstance().connectedDevice){ return; }
        deviceInterface.sendMessage(cmd);
    }
    // method to send a command to the device
    public void sendCommand(String cmd, Runnable onSuccessfulResponse, int wms){
        sendCommand(cmd, onSuccessfulResponse, null, null, wms);
    }
    // method to send a command to the device
    public void sendCommand(String cmd, Runnable onResponse, boolean repeatForCancel, int wms){
        sendCommand(cmd, onResponse, (repeatForCancel ? onResponse : null), onResponse, wms);
    }
    // method to send a command to the device
    public void sendCommand(String cmd, Runnable onSuccessfulResponse, Runnable onFailedResponse, int wms){
        sendCommand(cmd, onSuccessfulResponse, onFailedResponse, null, wms);
    }

    // method to send a command to the device and wait for a response for a certain amount of time and then execute the runnable if the response is successful
    // if the response is not successful, the onFailedResponse runnable is executed
    // if the response is null, the onNullResponse runnable is executed
    // this method is called by the rest of overloads of this method
    public void sendCommand(String cmd, Runnable onSuccessfulResponse, Runnable onFailedResponse, Runnable onNullResponse, int wms){
        // check if the device is connected
        if(!ControlCenter.getInstance().connectedDevice){ return; } 
        // send the command to the device (simple method)
        sendCommand(cmd);
        // set the last received message to the waiting for response message and show the waiting for response message
        ControlCenter.getInstance().lastReceivedMsg = "(Waiting For response...)";
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.VISIBLE);
        // create the runnable to wait for the response and execute the runnable if the response is successful or not
        runWait runnerWaiter = new runWait(wms, onSuccessfulResponse, onFailedResponse, onNullResponse);
        // create the thread to execute the runnable and wait for the response 
        Thread waitResponse = new Thread(runnerWaiter);
        // start the thread
        waitResponse.start();
    }

    public void requestInfo(String cmd, Runnable onSuccessfulResponse, Runnable onFailedResponse, int wms){
        if(!ControlCenter.getInstance().connectedDevice){ return; } //show message of not connected;

        sendCommand(cmd);

        ControlCenter.getInstance().lastReceivedMsg = "(Waiting For response...)";
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.VISIBLE);

        runRequestWait runnerWaiter = new runRequestWait(wms, onSuccessfulResponse, onFailedResponse);
        Thread waitResponse = new Thread(runnerWaiter);

        waitResponse.start();
    }

    // class to wait for the response and execute the runnable if the response is successful or not
    public static class runWait implements  Runnable {

        // variables for the time to wait and the runnables to execute
        int msW;
        Runnable onSuccessfulResponse, onFailedResponse, onNullResponse;
        // constructor to set the variables
        public runWait(int ms, Runnable onSucc, Runnable onFail, Runnable onNull){
            msW = ms;
            onSuccessfulResponse = onSucc;
            onFailedResponse = onFail;
            onNullResponse = onNull;
        }

        // method to execute the runnable
        public void run(){
            try{
                // get the last received message
                String lastMsg = ControlCenter.getInstance().lastReceivedMsg;
                // loop until the time to wait is over
                while (true){
                    Thread.sleep(100);
                    msW -= 100;
                    // while the time to wait is over, get the last received message (possible response)
                    String lastR = ControlCenter.getInstance().lastReceivedMsg;
                    // if the last received message is not the same as the last message, the response has been received
                    if(lastR.charAt(0) != '*' && !Objects.equals(lastR, lastMsg)){
                        // hide the waiting for response message
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
                        // if the response is successful, execute the onSuccessfulResponse runnable
                        if(Objects.equals(lastR, "1")){
                            ControlCenter.getInstance().mainActivity.onUIThread(onSuccessfulResponse);
                            ControlCenter.getInstance().mainActivity.makeSnackB("Cambios efectuados exitosamente");
                        }else if(Objects.equals(lastR, "0")){
                            // if the response is null, execute the onNullResponse runnable
                            ControlCenter.getInstance().mainActivity.makeSnackB("No hubo cambios");
                            if(onNullResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onNullResponse); }
                        }else{
                            // if the response is not successful, execute the onFailedResponse runnable (Error code -1)
                            if(Objects.equals(lastR, "-1")){
                                ControlCenter.getInstance().mainActivity.makeSnackB("Fallo en efectuar los cambios");
                            }else{
                                // in case of an unknown error, show the error code
                                ControlCenter.getInstance().mainActivity.makeSnackB("Comando no ejecutado correctamente (Respuesta: "+ lastR + " )" );
                            }
                            // if the onFailedResponse runnable is not null, execute it
                            if(onFailedResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onFailedResponse); }
                        }
                        // stop the thread
                        Thread.currentThread().interrupt();
                        return;
                    }else if(msW <= 0){
                        // if the time to wait is over, execute the onNullResponse runnable
                        if(onNullResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onNullResponse); }
                        // hide the waiting for response message
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
                        // show the time out message
                        ControlCenter.getInstance().mainActivity.makeSnackB("Tiempo de espera agotado");
                        // stop the thread
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }catch(InterruptedException e){
                // if an error occurs, stop the thread
                Thread.currentThread().interrupt();
                return;
            }
        }

    }

    // class to wait for the response and execute the runnable if the response is successful or not
    public static class runRequestWait implements  Runnable {

        // variables for the time to wait and the runnables to execute
        int msW;
        Runnable onSuccessfulResponse, onFailedResponse;
        // constructor to set the variables
        public runRequestWait(int ms, Runnable onSucc, Runnable onFail){
            msW = ms;
            onSuccessfulResponse = onSucc;
            onFailedResponse = onFail;
        }

        // method to execute the runnable
        public void run(){
            try{
                // get the last received message
                String lastMsg = ControlCenter.getInstance().lastReceivedMsg;
                // loop until the time to wait is over
                while (true){
                    Thread.sleep(100);
                    msW -= 100;
                    // while the time to wait is over, get the last received message (possible response)
                    String lastR = ControlCenter.getInstance().lastReceivedMsg;
                    // if the last received message is not the same as the last message, the response has been received
                    if(lastR.charAt(0) != '*' && !Objects.equals(lastR, lastMsg)){
                        // hide the waiting for response message
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
                        if(Objects.equals(lastR, "-1")){
                            // if the response is not successful, execute the onFailedResponse runnable (Error code -1)
                            ControlCenter.getInstance().mainActivity.makeSnackB("No ha sido posible obtener informacion");
                            if(onFailedResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onFailedResponse); }
                        }else{
                            // if the response is successful, execute the onSuccessfulResponse runnable
                            ControlCenter.getInstance().mainActivity.onUIThread(onSuccessfulResponse);
                            ControlCenter.getInstance().mainActivity.makeSnackB("Informacion encontrada");
                        }
                        // stop the thread
                        Thread.currentThread().interrupt();
                        return;
                    }else if(msW <= 0){
                        // if the time to wait is over, execute the onNullResponse runnable
                        if(onFailedResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onFailedResponse); }
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
                        ControlCenter.getInstance().mainActivity.makeSnackB("Tiempo de espera agotado");
                        // stop the thread
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }catch(InterruptedException e){
                // if an error occurs, stop the thread
                Thread.currentThread().interrupt();
                return;
            }
        }
    }


    // variable to store the device interface
    int tryConn = -1;
    // suppress the lint warning
    @SuppressLint("CheckResult")
    // method to connect to a device
    private void connectDevice(int pos){
        // if the device is already connected, disconnect it
        if(!Objects.equals(devMac, "")){
            String dM = devMac;
            disconnectDevice();
            // if the device to connect is the same as the one that was connected, stop the method
            if(Objects.equals(dM, devInfo[pos][1])){ return; }
        }
        // store the device info
        tryConn = pos;
        // connect to the device and execute the onConnected method if the connection is successful or the onError method if not
        bluetoothManager.openSerialDevice(devInfo[pos][1]).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(this::onConnected, this::onError);
        // show the waiting for response message
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.VISIBLE);
    }

    // method to disconnect the device
    public void disconnectDevice(){
        // close the device and execute
        bluetoothManager.closeDevice(deviceInterface);
        // clear the variables
        devMac = ""; devName = "";
        tryConn = -1;
        // set the text views to "None"
        binding.deviceNameTextView.setText("Ninguno");
        binding.deviceMACTextView.setText("Ninguno");
        // show the disconnected message and set the device connected variable to false
        ControlCenter.getInstance().mainActivity.makeSnackB("Dispositivo desconectado");
        ControlCenter.getInstance().setDeviceConnected(false, "");
    }

    // method called when the device is connected
    private void onConnected(BluetoothSerialDevice connectDevice){
        // hide the waiting for response message
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
        // store the device info
        devName = devInfo[tryConn][0];
        devMac = devInfo[tryConn][1];
        // set the text views to the device info
        binding.deviceNameTextView.setText(devName);
        binding.deviceMACTextView.setText(devMac);
        // set the device interface and the listeners to the device interface
        deviceInterface = connectDevice.toSimpleDeviceInterface();
        // set the listeners to the device interface and show the connected message
        deviceInterface.setListeners(this::receiveMsg, this::sentMsg, this::onError);
        ControlCenter.getInstance().mainActivity.makeSnackB("Dispositivo Conectado");
        // set the device connected variable to true
        ControlCenter.getInstance().setDeviceConnected(true, devName);
    }

    // method called when an error occurs
    private void onError(Throwable throwable) {
        // if the device is empty, stop disconnecting the device
        if(!Objects.equals(devMac, "")){
            disconnectDevice();
        }
        // set the device interface to -1 and show the error message
        tryConn = -1;
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
        ControlCenter.getInstance().mainActivity.makeSnackB("Error al intentar conectar dispositivo");
    }
    
    // on view created method
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // on destroy view method
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
