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
    ScheduleCreateMenuBinding binding;
    String stDate, edDate, stTime, edTime, nSc;
    SchedulerFragment.ScheduleItem scToSave;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = ScheduleCreateMenuBinding.inflate(inflater, container, false);
        binding.scheduleStartEditText2.setShowSoftInputOnFocus(false);
        binding.scheduleEndEditText2.setShowSoftInputOnFocus(false);
        binding.scheduleStartEditText.setShowSoftInputOnFocus(false);
        binding.scheduleEndEditText.setShowSoftInputOnFocus(false);
        Calendar cal = Calendar.getInstance();

        setDate(cal.get(Calendar.YEAR) ,  (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH), true);
        setDate(cal.get(Calendar.YEAR) ,  (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH), false);
        setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0, true);
        setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0, false);

        class getDateTime implements Runnable{
            boolean isTime, isStart;
            View view;
            public getDateTime(boolean iT, boolean iS, View v){
                isTime = iT; isStart = iS; view = v;
            }

            @Override
            public void run() {

                InputMethodManager imm = (InputMethodManager)requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                if(isTime){
                    TimePickerFragment tPick = new TimePickerFragment();
                    tPick.onTPick = ()->  {
                        setTime( tPick.hour, tPick.minute, 0, isStart);
                    };
                    tPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select time");
                }else{
                    DatePickerFragment dPick = new DatePickerFragment();
                    dPick.onDPick = ()->  {
                        setDate( dPick.year, dPick.month, dPick.day, isStart);
                    };
                    dPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select date");
                }
            }
        }


        binding.scheduleStartEditText.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(false, true, view).run(); } });
        binding.scheduleEndEditText.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(false, false, view).run(); } } );
        binding.scheduleStartEditText2.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(true, true, view).run(); } });
        binding.scheduleEndEditText2.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(true, false, view).run(); } });

        binding.scheduleStartEditText.setOnClickListener(view ->  new getDateTime(false, true, view).run());
        binding.scheduleEndEditText.setOnClickListener(view -> new getDateTime(false, false, view).run());
        binding.scheduleStartEditText2.setOnClickListener(view -> new getDateTime(true, true, view).run());
        binding.scheduleEndEditText2.setOnClickListener(view ->  new getDateTime(true, false, view).run());

        binding.addScheduleButton.setOnClickListener(
                view -> {
                    if(saveSchedule()){
                        ControlCenter.getInstance().connectionFrag.sendCommand(
                            ("SCHEDULE;ADD;"+ nSc + "," + stDate + "T" + stTime + "," + edDate + "T" + edTime),
                            ()-> {
                                ControlCenter.getInstance().schedulerFrag.addSchedule(scToSave);
                                //final Handler handler = new Handler(Looper.getMainLooper());
                                //handler.postDelayed(() -> ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_scheduleCreateFragment_to_ContentMainFragment, "Experimentos de muestreo de distancia"), 300);

                            }, 10000);
                    }
                });

        return binding.getRoot();
    }

    boolean saveSchedule(){
        nSc = binding.scheduleNameEditText.getText().toString();

        if(Objects.equals(nSc, "")){
            ControlCenter.getInstance().mainActivity.makeSnackB("Ingrese un nombre valido");
            return false;
        }

        scToSave = new SchedulerFragment.ScheduleItem(nSc, stDate + "T" + stTime, edDate + "T" + edTime, 1);

        if(scToSave.stTime.compare(scToSave.edTime.data) >= 0){
            ControlCenter.getInstance().mainActivity.makeSnackB("El TIEMPO de inicio debe ser menor que el de termino");
        }else{
            int res = ControlCenter.getInstance().schedulerFrag.isScheduleAvailable(scToSave);
            if(res == 1){ return true;
            }else if(res == -1){ ControlCenter.getInstance().mainActivity.makeSnackB("El TIEMPO seleccionado ya esta en uso para otro muestreo");
            }else{ ControlCenter.getInstance().mainActivity.makeSnackB("El NOMBRE seleccionado ya esta en uso para otro muestreo"); }

        }
        return false;
    }

    String setDate(int y, int m, int d, boolean isStart){

        DecimalFormat df = new DecimalFormat("00");
        String fmt = y + "/" + df.format(m) + "/" + df.format(d);
        String dt = y +"/" + m + "/"+ d;

        if(isStart){
            stDate = dt;
            binding.scheduleStartEditText.setText(fmt);
        }else{
            edDate = dt;
            binding.scheduleEndEditText.setText(fmt);
        }
        return fmt;
    }

    String setTime(int h, int m, int s, boolean isStart) {
        DecimalFormat df = new DecimalFormat("00");
        String fmt = h + ":" + df.format(m) + ":" + df.format(s);
        String dt = h +":" + m + ":"+ s;

        if(isStart){
            stTime = dt;
            binding.scheduleStartEditText2.setText(fmt);
        }else{
            edTime = dt;
            binding.scheduleEndEditText2.setText(fmt);
        }
        return fmt;
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
