package com.example.finalencoder_controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;

public class BluetoothReceiver extends BroadcastReceiver {

    @Override public void onReceive(Context context, Intent intent){
        String action = intent.getAction();
        int state;
        // se verifica si el estado del bluetooth cambio
        switch (action){
            // si el estado cambio a encendido
            case BluetoothAdapter.ACTION_STATE_CHANGED:
            // se obtiene el estado actual
                state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(state == BluetoothAdapter.STATE_ON){
                    // se actualiza el estado del bluetooth en el control center
                    ControlCenter.getInstance().setBluetoothConnected(true);
                    ControlCenter.getInstance().bluetoothConnected = true;
                }else{
                    // se actualiza el estado del bluetooth en el control center
                    ControlCenter.getInstance().setBluetoothConnected(false);
                    ControlCenter.getInstance().bluetoothConnected = false;
                }
        }

    }

}
