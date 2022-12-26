package com.example.finalencoder_controller;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    // Variables to store the time selected by the user
    public int hour, minute;
    // Runnable to execute when the user selects a time
    public Runnable onTPick;

    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        // Get the current hour and minute
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
    }

    // When the user selects a time, store it in the variables and execute the runnable
    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
        this.hour = hour; this.minute = minute;
        onTPick.run();
    }
}
