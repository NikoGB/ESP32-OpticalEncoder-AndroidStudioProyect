package com.example.finalencoder_controller;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.example.finalencoder_controller.databinding.DebuggingConsoleBinding;

public class DebuggingConsoleFragment extends Fragment {
    DebuggingConsoleBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = DebuggingConsoleBinding.inflate(inflater, container, false);
        // get the debug fragment instance
        ControlCenter.getInstance().debugFrag = this;

        // check if there are any messages to be displayed
        if(ControlCenter.getInstance().logMessages != ""){
            // update the console log with the messages
            updateConsoleLog(ControlCenter.getInstance().logMessages, false);
        }

        // set the send button listener
        binding.debugComsSend.setOnClickListener(view-> {
            // send the command to the ESP32 and clear the text box
            ControlCenter.getInstance().connectionFrag.sendCommand(String.valueOf(binding.comsSendText.getText()) );
            binding.comsSendText.setText("");
        });
        // set the scroll view to the bottom
        binding.debugScrollView.fullScroll(View.FOCUS_DOWN);

        return binding.getRoot();
    }
    // update the console log with the message received
    public void updateConsoleLog(String log, boolean isReceived){
        // get the current text and add the new message
        String toAdd = binding.comsTextView.getText().toString();
        // add the ">" if the message is received from the ESP32
        toAdd  = toAdd + (isReceived ? ">" : "") + log+ "\n";
        // update the text view
        binding.comsTextView.setText(toAdd);
        // set the scroll view to the bottom
        binding.debugScrollView.fullScroll(View.FOCUS_DOWN);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // check if the fragment is destroyed
    public boolean isDestroyed(){
        if(binding == null){ return true;
        }else{ return false; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
