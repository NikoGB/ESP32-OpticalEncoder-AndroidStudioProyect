package com.example.finalencoder_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
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

    // variables para el bluetooth
    private String completeMsg = "";
    public String devName = "", devMac = "";
    private FragmentConnectionBinding binding;
    private BluetoothManager bluetoothManager = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConnectionBinding.inflate(inflater, container, false);

        // evento para el boton de buscar dispositivos
        binding.findDeviceButton.setOnClickListener(view -> {
            // se verifica si el bluetooth esta encendido
            ControlCenter.getInstance().bluetoothConnected = android.bluetooth.BluetoothAdapter.getDefaultAdapter().isEnabled();
            if(ControlCenter.getInstance().bluetoothConnected && bluetoothManager != null){
                // si esta encendido se muestran los dispositivos
                showDevices();
                return;
            }
            // si no esta encendido se enciende
            bluetoothManager = BluetoothManager.getInstance();
            if (bluetoothManager == null) {
                Toast.makeText(getContext(), "El Bluetooth no esta habilitado.", Toast.LENGTH_SHORT).show();
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
            // show the devices
            showDevices();
        });

        // evento para el boton debug console
        binding.devConsoleButton.setOnClickListener(view -> {
            ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_ContentMainFragment_to_debuggingConsoleFragment, "Consola serial");
        });

        return binding.getRoot();
    }

    // variables para el bluetooth serial
    private SimpleBluetoothDeviceInterface deviceInterface;

    // almacena la informacion de los dispositivos
    String[][] devInfo;

    @SuppressLint("MissingPermission")
    // muestra los dispositivos enlazados al celular y permite seleccionar uno
    void showDevices(){
        // se obtienen los dispositivos enlazados
        ListView deviceList = binding.deviceList;
        Collection<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevicesList();
        List<Map<String, String>> data = new ArrayList<>();
        devInfo = new String[pairedDevices.size()][];

        // se almacena la informacion de los dispositivos
        int idx = 0;
        for (BluetoothDevice device : pairedDevices) {
            Map<String, String> datum = new HashMap<>(2);
            devInfo[idx] = new String[]{ device.getName(), device.getAddress() };
            datum.put("", device.getName());
            datum.put("Mac", device.getAddress());
            data.add(datum);
            idx++;
        }

        // se crea el adaptador para mostrar los dispositivos en el listview
        SimpleAdapter devAdapter = new SimpleAdapter(getContext(), data, android.R.layout.simple_list_item_2, new String[] {"", "Mac"}, new int[] {android.R.id.text1, android.R.id.text2});

        // se crea el evento para cuando se selecciona un dispositivo
        deviceList.setAdapter(devAdapter);
        deviceList.setOnItemClickListener((adapterView, view, position, id) -> connectDevice(position));
        devAdapter.notifyDataSetChanged();
    }

    // metodo usado para definir el mensaje
    private void receiveMsg(String s) {
        // si encuentra el '*' quiere decir que es la ultima parte del mensaje
        if(s.contains("*")){
            completeMsg+=s;
            // si existe un error en los datos se el mensaje contendra un #
            if (completeMsg.contains("#")){
                // si lo tiene se borra para que no interfiera con el resto de las funciones y se notifica al usuario
                completeMsg=completeMsg.replace("#","");
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("ADVERTENCIA: ");
                builder.setMessage("Se interrumpio indebidamente el muestreo, los contenidos de estos no pueden estar completos.");
                builder.setNeutralButton("Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
            ControlCenter.getInstance().setReceivedMessage(completeMsg.substring(0,completeMsg.lastIndexOf('*')));
            // se limpia el completeMsg para recibir el sgt
            completeMsg="";

            return;
        }
        // si no es porque el mensaje esta incompleto y sumamos el contenido al anterior
        completeMsg+=s;
    }

    // metodo usado para enviar un mensaje
    private void sentMsg(String s) {
        ControlCenter.getInstance().setSentMessage(s);
    }

    // metodo usado para enviar un comando
    public void sendCommand(String cmd){
        if(!ControlCenter.getInstance().connectedDevice){ return; }
        deviceInterface.sendMessage(cmd);
    }
    
    // metodo usado para enviar un comando y esperar una respuesta
    public void sendCommand(String cmd, Runnable onSuccessfulResponse, int wms){
        sendCommand(cmd, onSuccessfulResponse, null, null, wms);
    }
    
    // metodo usado para enviar un comando y esperar una respuesta
    public void sendCommand(String cmd, Runnable onResponse, boolean repeatForCancel, int wms){
        sendCommand(cmd, onResponse, (repeatForCancel ? onResponse : null), onResponse, wms);
    }
    
    // metodo usado para enviar un comando y esperar una respuesta
    public void sendCommand(String cmd, Runnable onSuccessfulResponse, Runnable onFailedResponse, int wms){
        sendCommand(cmd, onSuccessfulResponse, onFailedResponse, null, wms);
    }

    /**
     * metodo usado para enviar un comando y esperar una respuesta por un tiempo determinado
     * @param cmd: comando a enviar
     * @param onSuccessfulResponse: accion a realizar si la respuesta es exitosa
     * @param onFailedResponse: accion a realizar si la respuesta es erronea
     * @param onNullResponse: accion a realizar si no se recibe respuesta
     */
    public void sendCommand(String cmd, Runnable onSuccessfulResponse, Runnable onFailedResponse, Runnable onNullResponse, int wms){
        if(!ControlCenter.getInstance().connectedDevice){ return; } 
        sendCommand(cmd);
        ControlCenter.getInstance().lastReceivedMsg = "(Waiting For response...)";
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.VISIBLE);
        runWait runnerWaiter = new runWait(wms, onSuccessfulResponse, onFailedResponse, onNullResponse);
        Thread waitResponse = new Thread(runnerWaiter);
        waitResponse.start();
    }

    /**
     * metodo usado para enviar un comando y esperar una respuesta por un tiempo determinado, este se usa para solicitar informacion (data.txt y schedule.txt)
     * @param cmd: comando a enviar
     * @param onSuccessfulResponse: accion a realizar si la respuesta es exitosa
     * @param onFailedResponse: accion a realizar si la respuesta es erronea
     */
    public void requestInfo(String cmd, Runnable onSuccessfulResponse, Runnable onFailedResponse, int wms){
        if(!ControlCenter.getInstance().connectedDevice){ return; } 

        sendCommand(cmd);

        ControlCenter.getInstance().lastReceivedMsg = "(Waiting For response...)";
        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.VISIBLE);

        runRequestWait runnerWaiter = new runRequestWait(wms, onSuccessfulResponse, onFailedResponse);
        Thread waitResponse = new Thread(runnerWaiter);

        waitResponse.start();
    }

    // clase usada para esperar una respuesta 
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
                    // si se recibe respuesta y es diferente a la anterior
                    if(lastR.charAt(0) != '*' && lastR.charAt(0) != '!' && !Objects.equals(lastR, lastMsg)){
                        // se elimina el mensaje de espera de respuesta
                        ControlCenter.getInstance().mainActivity.setOnWaitForResponse(View.GONE);
                        if(Objects.equals(lastR, "1")){
                            // si la respuesta es exitosa se recibe un 1 y se ejecuta la accion de exito
                            ControlCenter.getInstance().mainActivity.onUIThread(onSuccessfulResponse);
                            ControlCenter.getInstance().mainActivity.makeSnackB("Cambios efectuados exitosamente");
                        }else if(Objects.equals(lastR, "0")){
                            // si la respuesta no surje efecto se recibe un 0 y se ejecuta la accion de error
                            ControlCenter.getInstance().mainActivity.makeSnackB("No hubo cambios");
                            if(onNullResponse != null){ ControlCenter.getInstance().mainActivity.onUIThread(onNullResponse); }
                        }else{
                            if(Objects.equals(lastR, "-1")){
                                // si la respuesta es erronea se recibe un -1 y se ejecuta la accion de error
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
