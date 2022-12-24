package com.example.finalencoder_controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;

public class BluetoothReceiver extends BroadcastReceiver {

    @Override public void onReceive(Context context, Intent intent){
        String action = intent.getAction();
        int state;
        switch (action){
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(state == BluetoothAdapter.STATE_ON){
                    ControlCenter.getInstance().setBluetoothConnected(true);
                    ControlCenter.getInstance().bluetoothConnected = true;
                }else{
                    ControlCenter.getInstance().setBluetoothConnected(false);
                    ControlCenter.getInstance().bluetoothConnected = false;
                }
        }

    }

}
