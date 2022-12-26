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
    
    // Variables to store the time selected by the user
    public int hour, minute, second, millisecond;

    // variables to store the max and min time to show
    int minTime, maxTime;
    // Runnable to execute when the user selects a time
    public Runnable onAccept;

    // Constructor to set the max and min time to show
    public  ExtendedTimePickerFragment(int maxTimes, int minTime){
        this.minTime = minTime; this.maxTime = maxTimes;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(layout.extended_time_picker, null);

        // Get the number pickers from the layout
        NumberPicker hourNP = dialogView.findViewById(id.hour_picker);
        NumberPicker minuteNP = dialogView.findViewById(id.minute_picker);
        NumberPicker secondNP = dialogView.findViewById(id.second_picker);
        NumberPicker millisecondNP = dialogView.findViewById(id.millisecond_picker);


        // check the max and min time to show and set the number pickers
        // if the max time is 3, show the hour number picker
        if(maxTime == 3){
            // set the hour number picker visible and set the min and max values
            hourNP.setVisibility(View.VISIBLE);
            hourNP.setMinValue(0);
            hourNP.setMaxValue(23);
            // set the separation between the hour and minute number picker visible
            dialogView.findViewById(id.hourMin_separation).setVisibility(View.VISIBLE);
        }

        // check the max and min time to show and set the number pickers (same as above)
        // if the max time is 2 and the min time is 2, show the minute number picker
        if(maxTime >= 2 && minTime <= 2){
            minuteNP.setVisibility(View.VISIBLE);
            minuteNP.setMinValue(0);
            minuteNP.setMaxValue(59);

            dialogView.findViewById(id.minSecond_separation).setVisibility(View.VISIBLE);
        }

        // check the max and min time to show and set the number pickers (same as above)
        // if the max time is 1 and the min time is 1, show the second number picker
        if(maxTime >= 1 && minTime <= 1){
            secondNP.setVisibility(View.VISIBLE);
            secondNP.setMinValue(0);
            secondNP.setMaxValue(59);
            dialogView.findViewById(id.secondMill_separation).setVisibility(View.VISIBLE);
        }

        // check the max and min time to show and set the number pickers (same as above)
        // if the max time is 0 and the min time is 0, show the millisecond number picker
        if(minTime == 0){
            // set the millisecond number picker visible and set the min and max values
            millisecondNP.setVisibility(View.VISIBLE);
            millisecondNP.setMinValue(0);
            millisecondNP.setMaxValue(9);
            // set the displayed values of the millisecond number picker
            millisecondNP.setDisplayedValues(new String[]{ "000", "100", "200", "300", "400", "500", "600", "700", "800", "900" });
        }

        // set the title of the dialog and the positive button
        builder.setView(dialogView).setPositiveButton("Ok", (dialogInterface, i) -> {
            // set the time selected by the user
            setTime(dialogView);
            // execute the runnable when the user selects a time (in this case, the runnable is in the MainActivity)
            onAccept.run(); }).setTitle("Seleccionar tiempo");

        // create the dialog and return it
        return builder.create();
    }

    // method to set the time selected by the user
    public void setTime(View tDialogView){
        // get the number pickers from the layout
        NumberPicker hourNP = tDialogView.findViewById(id.hour_picker);
        NumberPicker minuteNP = tDialogView.findViewById(id.minute_picker);
        NumberPicker secondNP = tDialogView.findViewById(id.second_picker);
        NumberPicker millisecondNP = tDialogView.findViewById(id.millisecond_picker);

        // set the time selected by the user
        hour = hourNP.getValue();
        minute = minuteNP.getValue();
        second = secondNP.getValue();
        millisecond = millisecondNP.getValue() * 100;
    }
}
