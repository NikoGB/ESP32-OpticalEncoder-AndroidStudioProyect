package com.example.finalencoder_controller;

import android.app.AlertDialog;
import android.app.Dialog;
//import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.finalencoder_controller.databinding.ExtendedTimePickerBinding;

import static com.example.finalencoder_controller.R.*;

public class ExtendedTimePickerFragment extends DialogFragment {
    /*
    public interface ExtendedTimePickerListener{
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    ExtendedTimePickerListener listener;

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        try{
            listener = (ExtendedTimePickerListener) context;
        }catch (ClassCastException e){
            throw  new ClassCastException("No implementada")
        }

    }
    */

    public int hour, minute, second, millisecond;

    int minTime, maxTime;
    public Runnable onAccept;

    public  ExtendedTimePickerFragment(int maxTimes, int minTime){
        this.minTime = minTime; this.maxTime = maxTimes;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(layout.extended_time_picker, null);

        NumberPicker hourNP = dialogView.findViewById(id.hour_picker);
        NumberPicker minuteNP = dialogView.findViewById(id.minute_picker);
        NumberPicker secondNP = dialogView.findViewById(id.second_picker);
        NumberPicker millisecondNP = dialogView.findViewById(id.millisecond_picker);


        if(maxTime == 3){
            hourNP.setVisibility(View.VISIBLE);
            hourNP.setMinValue(0);
            hourNP.setMaxValue(23);

            dialogView.findViewById(id.hourMin_separation).setVisibility(View.VISIBLE);
        }

        if(maxTime >= 2 && minTime <= 2){
            minuteNP.setVisibility(View.VISIBLE);
            minuteNP.setMinValue(0);
            minuteNP.setMaxValue(59);

            dialogView.findViewById(id.minSecond_separation).setVisibility(View.VISIBLE);
        }

        if(maxTime >= 1 && minTime <= 1){
            secondNP.setVisibility(View.VISIBLE);
            secondNP.setMinValue(0);
            secondNP.setMaxValue(59);
            dialogView.findViewById(id.secondMill_separation).setVisibility(View.VISIBLE);
        }


        if(minTime == 0){
            millisecondNP.setVisibility(View.VISIBLE);
            millisecondNP.setMinValue(0);
            millisecondNP.setMaxValue(9);
            millisecondNP.setDisplayedValues(new String[]{ "000", "100", "200", "300", "400", "500", "600", "700", "800", "900" });
        }

        builder.setView(dialogView).setPositiveButton("Ok", (dialogInterface, i) -> {
            setTime(dialogView);
            onAccept.run(); }).setTitle("Seleccionar tiempo");


        return builder.create();
    }

    public void setTime(View tDialogView){

        NumberPicker hourNP = tDialogView.findViewById(id.hour_picker);
        NumberPicker minuteNP = tDialogView.findViewById(id.minute_picker);
        NumberPicker secondNP = tDialogView.findViewById(id.second_picker);
        NumberPicker millisecondNP = tDialogView.findViewById(id.millisecond_picker);

        hour = hourNP.getValue();
        minute = minuteNP.getValue();
        second = secondNP.getValue();
        millisecond = millisecondNP.getValue() * 100;
    }
}
