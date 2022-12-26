package com.example.finalencoder_controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;

public class BluetoothReceiver extends BroadcastReceiver {

    @Override public void onReceive(Context context, Intent intent){
        // Get the action of the intent
        String action = intent.getAction();
        // Get the state of the bluetooth
        int state;
        // Check if the action is the state of the bluetooth
        switch (action){
            // If the action is the state of the bluetooth changed
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                // Get the state of the bluetooth
                state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                // Check if the bluetooth is on
                if(state == BluetoothAdapter.STATE_ON){
                    // Set the bluetooth connected to true
                    ControlCenter.getInstance().setBluetoothConnected(true);
                    ControlCenter.getInstance().bluetoothConnected = true;
                }else{
                    // Set the bluetooth connected to false
                    ControlCenter.getInstance().setBluetoothConnected(false);
                    ControlCenter.getInstance().bluetoothConnected = false;
                }
        }

    }

}
