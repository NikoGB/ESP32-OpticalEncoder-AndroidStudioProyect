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

    public String devName = "", devMac = "";
    private FragmentConnectionBinding binding;
    private BluetoothManager bluetoothManager = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentConnectionBinding.inflate(inflater, container, false);

        binding.findDeviceButton.setOnClickListener(view -> {
            ControlCenter.getInstance().bluetoothConnected = android.bluetooth.BluetoothAdapter.getDefaultAdapter().isEnabled();

            if(ControlCenter.getInstance().bluetoothConnected && bluetoothManager != null){
                showDevices(); return; }

            bluetoothManager = BluetoothManager.getInstance();

            if (bluetoothManager == null) {
                Toast.makeText(getContext(), "El Bluetooth no esta abilitado.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ControlCenter.getInstance().bluetoothConnected) {
                Intent enableBtIntent = new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
                int REQUEST_ENABLE_BT = 0;
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

                if (!ControlCenter.getInstance().bluetoothConnected) {
                    ControlCenter.getInstance().mainActivity.makeSnackB("Vuelva a intentar");
                    return;
                }
            }

            showDevices();
        });
        binding.devConsoleButton.setOnClickListener(view -> {
            ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_ContentMainFragment_to_debuggingConsoleFragment, "Consola serial");
        });

        //ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_connectionFragment_to_debuggingConsoleFragment);

        //ControlCenter.getInstance().mainActivity.createdebbug();
/*
        ControlCenter.getInstance().debugFrag.setSendButton();

        binding.devConsoleButton.setOnClickListener(view -> {
            ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_connectionFragment_to_debuggingConsoleFragment);
        });*/

        return binding.getRoot();

    }

    private SimpleBluetoothDeviceInterface deviceInterface;

    String[][] devInfo;

    @SuppressLint("MissingPermission")
    void showDevices(){
        ListView deviceList = binding.deviceList;
        Collection<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevicesList();
        List<Map<String, String>> data = new ArrayList<>();

        devInfo = new String[pairedDevices.size()][];

        int idx = 0;
        for (BluetoothDevice device : pairedDevices) {
            Map<String, String> datum = new HashMap<>(2);
            devInfo[idx] = new String[]{ device.getName(), device.getAddress() };

            datum.put("", device.getName());
            datum.put("Mac", device.getAddress());
            data.add(datum);
            idx++;
        }

        SimpleAdapter devAdapter = new SimpleAdapter(getContext(), data, android.R.layout.simple_list_item_2,
                new String[] {"", "Mac"}, new int[] {android.R.id.text1, android.R.id.text2});

        deviceList.setAdapter(devAdapter);
        deviceList.setOnItemClickListener((adapterView, view, position, id) -> connectDevice(position));
        //(android.R.layout.simple_list_item_2)devAdapter.getItem(0) ;

        devAdapter.notifyDataSetChanged();

    }



    private void receiveMsg(String s) {
        ControlCenter.getInstance().setReceivedMessage(s);

    }
    private void sentMsg(String s) {
        ControlCenter.getInstance().setSentMessage(s);
    }

    public void sendCommand(String cmd){
        if(!ControlCenter.getInstance().connectedDevice){ return; } //show message of not connected;

        deviceInterface.sendMessage(cmd);
    }

    public void sendCommand(String cmd, Runnable onSuccessfulResponse, int wms){
        sendCommand(cmd, onSuccessfulResponse, null, null, wms);
    }

    public void sendCommand(String cmd, Runnable onResponse, boolean repeatForCancel, int wms){
        sendCommand(cmd, onResponse, (repeatForCancel ? onResponse : null), onResponse, wms);
    }

    public void sendCommand(String cmd, Runnable onSuccessfulResponse, Runnable onFailedResponse, int wms){
        sendCommand(cmd, onSuccessfulResponse, onFailedResponse, null, wms);
    }

    public void sendCommand(String cmd, Runnable onSuccessfulResponse, Runnable onFailedResponse, Runnable onNullResponse, int wms){

        if(!ControlCenter.getInstance().connectedDevice){ return; } //show message of not connected;

        sendCommand(cmd);

        ControlCenter.getInstance().lastReceivedMsg = "(Waiting For response...)";
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.VISIBLE);

        runWait runnerWaiter = new runWait(wms, onSuccessfulResponse, onFailedResponse, onNullResponse);
        Thread waitResponse = new Thread(runnerWaiter);

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

    public static class runWait implements  Runnable {

        int msW;
        Runnable onSuccessfulResponse, onFailedResponse, onNullResponse;
        public runWait(int ms, Runnable onSucc, Runnable onFail, Runnable onNull){
            msW = ms;
            onSuccessfulResponse = onSucc;
            onFailedResponse = onFail;
            onNullResponse = onNull;
        }

        public void run(){
            try{
                String lastMsg = ControlCenter.getInstance().lastReceivedMsg;

                while (true){
                    Thread.sleep(100);
                    msW -= 100;

                    String lastR = ControlCenter.getInstance().lastReceivedMsg;
                    if(lastR.charAt(0) != '*' && !Objects.equals(lastR, lastMsg)){
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);

                        if(Objects.equals(lastR, "1")){
                            ControlCenter.getInstance().mainActivity.onUIThread(onSuccessfulResponse);
                            ControlCenter.getInstance().mainActivity.makeSnackB("Cambios efectuados exitosamente");

                        }else if(Objects.equals(lastR, "0")){
                            ControlCenter.getInstance().mainActivity.makeSnackB("No hubo cambios");

                            if(onNullResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onNullResponse); }
                        }else{
                            if(Objects.equals(lastR, "-1")){
                                ControlCenter.getInstance().mainActivity.makeSnackB("Fallo en efectuar los cambios");
                            }else{
                                ControlCenter.getInstance().mainActivity.makeSnackB("Comando no ejecutado correctamente (Respuesta: "+ lastR + " )" );
                            }

                            if(onFailedResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onFailedResponse); }
                        }

                        Thread.currentThread().interrupt();
                        return;
                    }else if(msW <= 0){
                        if(onNullResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onNullResponse); }
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
                        ControlCenter.getInstance().mainActivity.makeSnackB("Tiempo de espera agotado");

                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                return;
            }
        }

    }

    public static class runRequestWait implements  Runnable {

        int msW;
        Runnable onSuccessfulResponse, onFailedResponse;
        public runRequestWait(int ms, Runnable onSucc, Runnable onFail){
            msW = ms;
            onSuccessfulResponse = onSucc;
            onFailedResponse = onFail;
        }

        public void run(){
            try{
                String lastMsg = ControlCenter.getInstance().lastReceivedMsg;

                while (true){
                    Thread.sleep(100);
                    msW -= 100;

                    String lastR = ControlCenter.getInstance().lastReceivedMsg;
                    if(lastR.charAt(0) != '*' && !Objects.equals(lastR, lastMsg)){
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
                        if(Objects.equals(lastR, "-1")){
                            ControlCenter.getInstance().mainActivity.makeSnackB("No ha sido posible obtener informacion");
                            if(onFailedResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onFailedResponse); }
                        }else{
                            ControlCenter.getInstance().mainActivity.onUIThread(onSuccessfulResponse);
                            ControlCenter.getInstance().mainActivity.makeSnackB("Informacion encontrada");

                        }

                        Thread.currentThread().interrupt();
                        return;
                    }else if(msW <= 0){
                        if(onFailedResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onFailedResponse); }
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
                        ControlCenter.getInstance().mainActivity.makeSnackB("Tiempo de espera agotado");

                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                return;
            }
        }

    }


    int tryConn = -1;

    @SuppressLint("CheckResult")
    private void connectDevice(int pos){

        if(!Objects.equals(devMac, "")){
            String dM = devMac;
            disconnectDevice();

            if(Objects.equals(dM, devInfo[pos][1])){ return; }
        }

        tryConn = pos;
        bluetoothManager.openSerialDevice(devInfo[pos][1]).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(this::onConnected, this::onError);

        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.VISIBLE);
    }

    public void disconnectDevice(){
        bluetoothManager.closeDevice(deviceInterface);
        devMac = ""; devName = "";
        tryConn = -1;

        binding.deviceNameTextView.setText("Ninguno");
        binding.deviceMACTextView.setText("Ninguno");

        ControlCenter.getInstance().mainActivity.makeSnackB("Dispositivo desconectado");
        ControlCenter.getInstance().setDeviceConnected(false, "");
    }

    private void onConnected(BluetoothSerialDevice connectDevice){

        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
        devName = devInfo[tryConn][0];
        devMac = devInfo[tryConn][1];

        binding.deviceNameTextView.setText(devName);
        binding.deviceMACTextView.setText(devMac);


        deviceInterface = connectDevice.toSimpleDeviceInterface();
        deviceInterface.setListeners(this::receiveMsg, this::sentMsg, this::onError);
        ControlCenter.getInstance().mainActivity.makeSnackB("Dispositivo Conectado");

        ControlCenter.getInstance().setDeviceConnected(true, devName);
    }

    private void onError(Throwable throwable) {

        if(!Objects.equals(devMac, "")){
            disconnectDevice();
        }

        tryConn = -1;
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
        ControlCenter.getInstance().mainActivity.makeSnackB("Error al intentar conectar dispositivo");
    }
    
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
