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
        ControlCenter.getInstance().debugFrag = this;

        if(ControlCenter.getInstance().logMessages != ""){
            updateConsoleLog(ControlCenter.getInstance().logMessages, false);
        }


        binding.debugComsSend.setOnClickListener(view-> {
            ControlCenter.getInstance().connectionFrag.sendCommand(String.valueOf(binding.comsSendText.getText()) );
            binding.comsSendText.setText("");
        });

        binding.debugScrollView.fullScroll(View.FOCUS_DOWN);

        return binding.getRoot();
    }

    public void updateConsoleLog(String log, boolean isReceived){
        String toAdd = binding.comsTextView.getText().toString();
        toAdd  = toAdd + (isReceived ? ">" : "") + log+ "\n";

        binding.comsTextView.setText(toAdd);

        binding.debugScrollView.fullScroll(View.FOCUS_DOWN);

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

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
