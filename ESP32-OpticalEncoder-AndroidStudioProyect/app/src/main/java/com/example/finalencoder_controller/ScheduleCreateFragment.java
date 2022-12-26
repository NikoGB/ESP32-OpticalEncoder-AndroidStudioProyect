package com.example.finalencoder_controller;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.example.finalencoder_controller.databinding.ScheduleCreateMenuBinding;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Objects;


public class ScheduleCreateFragment extends Fragment {
    // Variables to store the date and time selected by the user
    ScheduleCreateMenuBinding binding;
    String stDate, edDate, stTime, edTime, nSc;
    // schedule to save
    SchedulerFragment.ScheduleItem scToSave;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = ScheduleCreateMenuBinding.inflate(inflater, container, false);
        // Inflate the layout for this fragment
        binding.scheduleStartEditText2.setShowSoftInputOnFocus(false);
        binding.scheduleEndEditText2.setShowSoftInputOnFocus(false);
        binding.scheduleStartEditText.setShowSoftInputOnFocus(false);
        binding.scheduleEndEditText.setShowSoftInputOnFocus(false);
        // get the calendar instance
        Calendar cal = Calendar.getInstance();

        // set the date and time to the current date and time
        setDate(cal.get(Calendar.YEAR) ,  (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH), true);
        setDate(cal.get(Calendar.YEAR) ,  (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH), false);
        setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0, true);
        setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0, false);

        // Class to get the date and time from the user
        class getDateTime implements Runnable{
            // variables to pick the date and time
            boolean isTime, isStart;
            View view;

            // Constructor to set the variables
            public getDateTime(boolean iT, boolean iS, View v){
                isTime = iT; isStart = iS; view = v;
            }

            @Override
            public void run() {
                //we are going to use the InputMethodManager class to hide the soft keyboard
                //getSystemService(Context.INPUT_METHOD_SERVICE); gets us the InputMethodManager
                //hideSoftInputFromWindow(view.getWindowToken(), 0); hides the keyboard from the window
                InputMethodManager imm = (InputMethodManager)requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                //if we are picking a time, we want to create a new time picker fragment
                //we are going to use the onTPick field to set the time that we pick
                if(isTime){
                    TimePickerFragment tPick = new TimePickerFragment();
                    tPick.onTPick = ()->  {
                        setTime( tPick.hour, tPick.minute, 0, isStart);
                    }; 
                    //show the time picker fragment that we just created
                    tPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select time");
                }else{ //we are picking a date, so we are going to create a new date picker fragment
                    DatePickerFragment dPick = new DatePickerFragment();
                    dPick.onDPick = ()->  {
                        setDate( dPick.year, dPick.month, dPick.day, isStart);
                    };
                    //show the date picker fragment that we just created
                    dPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select date");
                }
            }
        }

        // set the on focus change listeners for the date and time edit texts
        binding.scheduleStartEditText.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(false, true, view).run(); } });
        binding.scheduleEndEditText.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(false, false, view).run(); } } );
        binding.scheduleStartEditText2.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(true, true, view).run(); } });
        binding.scheduleEndEditText2.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(true, false, view).run(); } });

        // set the on click listeners for the date and time edit texts
        binding.scheduleStartEditText.setOnClickListener(view ->  new getDateTime(false, true, view).run());
        binding.scheduleEndEditText.setOnClickListener(view -> new getDateTime(false, false, view).run());
        binding.scheduleStartEditText2.setOnClickListener(view -> new getDateTime(true, true, view).run());
        binding.scheduleEndEditText2.setOnClickListener(view ->  new getDateTime(true, false, view).run());

        // set the on click listener for the add schedule button
        binding.addScheduleButton.setOnClickListener(
                view -> {
                    // Execute the save schedule function and if it returns true, send the command to the controller
                    if(saveSchedule()){
                        // send the command to the controller to add the schedule (format: SCHEDULE;ADD;name,start,end)
                        ControlCenter.getInstance().connectionFrag.sendCommand(
                            ("SCHEDULE;ADD;"+ nSc + "," + stDate + "T" + stTime + "," + edDate + "T" + edTime),
                            ()-> {
                                ControlCenter.getInstance().schedulerFrag.addSchedule(scToSave);
                            }, 10000);
                    }
                });

        return binding.getRoot();
    }

    // function to save the schedule
    boolean saveSchedule(){
        // get the schedule name from the edit text
        nSc = binding.scheduleNameEditText.getText().toString();

        // check if the schedule name is valid
        if(Objects.equals(nSc, "")){
            // if the schedule name is not valid, show a snackbar with the error message
            ControlCenter.getInstance().mainActivity.makeSnackB("Ingrese un nombre valido");
            return false;
        }

        // get the start date and time from the edit texts
        scToSave = new SchedulerFragment.ScheduleItem(nSc, stDate + "T" + stTime, edDate + "T" + edTime, 1);

        // check if the start date and time are valid
        if(scToSave.stTime.compare(scToSave.edTime.data) >= 0){
            // if the start date and time are not valid, show a snackbar with the error message (the start time must be less than the end time)
            ControlCenter.getInstance().mainActivity.makeSnackB("El TIEMPO de inicio debe ser menor que el de termino");
        }else{
            // if the start date and time are valid, check if the schedule name and time are available
            int res = ControlCenter.getInstance().schedulerFrag.isScheduleAvailable(scToSave);
            // if the schedule name and time are available, return true
            if(res == 1){ 
                return true;
            }else if(res == -1){ 
                // if the schedule time is not available, show a snackbar with the error message
                ControlCenter.getInstance().mainActivity.makeSnackB("El TIEMPO seleccionado ya esta en uso para otro muestreo");
            }else{ 
                // if the schedule name is not available, show a snackbar with the error message
                ControlCenter.getInstance().mainActivity.makeSnackB("El NOMBRE seleccionado ya esta en uso para otro muestreo");
            }

        }
        // return false by default
        return false;
    }

    // function to set the date
    String setDate(int y, int m, int d, boolean isStart){

        // format the date to a string with the format: yyyy/mm/dd
        DecimalFormat df = new DecimalFormat("00");
        String fmt = y + "/" + df.format(m) + "/" + df.format(d);
        String dt = y +"/" + m + "/"+ d;

        // if the date is the start date, set the start date string and the start date edit text with the formatted date
        // set the date and time strings and the edit texts with the formatted date
        if(isStart){
            stDate = dt;
            binding.scheduleStartEditText.setText(fmt);
        }else{
            // if the date is the end date, set the end date string and the end date edit text with the formatted date
            edDate = dt;
            binding.scheduleEndEditText.setText(fmt);
        }
        // return the formatted date
        return fmt;
    }

    // function to set the time
    String setTime(int h, int m, int s, boolean isStart) {
        // format the time to a string with the format: hh:mm:ss
        DecimalFormat df = new DecimalFormat("00");
        String fmt = h + ":" + df.format(m) + ":" + df.format(s);
        String dt = h +":" + m + ":"+ s;

        // if the time is the start time, set the start time string and the start time edit text with the formatted time
        if(isStart){
            stTime = dt;
            binding.scheduleStartEditText2.setText(fmt);
        }else{
            // if the time is the end time, set the end time string and the end time edit text with the formatted time
            edTime = dt;
            binding.scheduleEndEditText2.setText(fmt);
        }
        // return the formatted time
        return fmt;
    }

    // On view created method
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // On destroy view method (to avoid memory leaks) 
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
