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

        // define los valores iniciales de los campos de fecha y hora
        setDate(cal.get(Calendar.YEAR) ,  (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH), true);
        setDate(cal.get(Calendar.YEAR) ,  (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH), false);
        setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0, true);
        setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0, false);

        // clase para obtener la fecha
        class getDateTime implements Runnable{
            boolean isTime, isStart;
            View view;

            // constructor
            public getDateTime(boolean iT, boolean iS, View v){
                isTime = iT; isStart = iS; view = v;
            }

            @Override
            public void run() {
                // usamos el InputMethodManager para ocultar el teclado 
                InputMethodManager imm = (InputMethodManager)requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                // si estamos seleccionando una hora, creamos un nuevo fragmento de seleccion de hora
                // usamos el campo onTPick para establecer la hora que seleccionamos
                if(isTime){
                    TimePickerFragment tPick = new TimePickerFragment();
                    tPick.onTPick = ()->  {
                        setTime( tPick.hour, tPick.minute, 0, isStart);
                    }; 
                    // mostramos el fragmento de seleccion de hora que acabamos de crear
                    tPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select time");
                }else{
                    // si estamos seleccionando un dia, creamos un nuevo fragmento de seleccion de dia
                    DatePickerFragment dPick = new DatePickerFragment();
                    dPick.onDPick = ()->  {
                        setDate( dPick.year, dPick.month, dPick.day, isStart);
                    };
                    // mostramos el fragmento de seleccion de dia que acabamos de crear
                    dPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select date");
                }
            }
        }

        // define los listeners para los campos de fecha y hora
        binding.scheduleStartEditText.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(false, true, view).run(); } });
        binding.scheduleEndEditText.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(false, false, view).run(); } } );
        binding.scheduleStartEditText2.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(true, true, view).run(); } });
        binding.scheduleEndEditText2.setOnFocusChangeListener((view, b) -> { if(b){ new getDateTime(true, false, view).run(); } });

        // define los listeners para los campos de fecha y hora
        binding.scheduleStartEditText.setOnClickListener(view ->  new getDateTime(false, true, view).run());
        binding.scheduleEndEditText.setOnClickListener(view -> new getDateTime(false, false, view).run());
        binding.scheduleStartEditText2.setOnClickListener(view -> new getDateTime(true, true, view).run());
        binding.scheduleEndEditText2.setOnClickListener(view ->  new getDateTime(true, false, view).run());

        binding.addScheduleButton.setOnClickListener(
                view -> {
                    if(saveSchedule()){
                        // envia el comando de crear shcedule (formato: SCHEDULE;ADD;name,start,end)
                        ControlCenter.getInstance().connectionFrag.sendCommand(
                            ("SCHEDULE;ADD;"+ nSc + "," + stDate + "T" + stTime + "," + edDate + "T" + edTime),
                            ()-> {
                                ControlCenter.getInstance().schedulerFrag.addSchedule(scToSave);
                            }, 10000);
                    }
                });

        return binding.getRoot();
    }

    // funcion para guardar el schedule y comprueba que los datos sean validos y no esten siendo ocupados por otro schedule
    boolean saveSchedule(){
        nSc = binding.scheduleNameEditText.getText().toString();

        if(Objects.equals(nSc, "")){
            ControlCenter.getInstance().mainActivity.makeSnackB("Ingrese un nombre valido");
            return false;
        }else{
            for(int i = 0; i < nSc.length; i++){
                if(!Character.isLetter(nSc.charAt(i)){
                    ControlCenter.getInstance().mainActivity.makeSnackB("No se permiten CARACTERES especiales en el NOMBRE");
                    return false;
                }
            }
        }

        scToSave = new SchedulerFragment.ScheduleItem(nSc, stDate + "T" + stTime, edDate + "T" + edTime, 1);

        if(scToSave.stTime.compare(scToSave.edTime.data) >= 0){
            ControlCenter.getInstance().mainActivity.makeSnackB("El TIEMPO de inicio debe ser menor que el de termino");
        }else{
            int res = ControlCenter.getInstance().schedulerFrag.isScheduleAvailable(scToSave);
            if(res == 1){ 
                return true;
            }else if(res == -1){ 
                ControlCenter.getInstance().mainActivity.makeSnackB("El TIEMPO seleccionado ya esta en uso para otro muestreo");
            }else if(res == -2){ 
                ControlCenter.getInstance().mainActivity.makeSnackB("El NOMBRE seleccionado ya esta en uso para otro muestreo");
            }else{
                ControlCenter.getInstance().mainActivity.makeSnackB("El TIEMPO de inicio es anterior a la hora actual real");
            }

        }
        return false;
    }

    // funcion para establecer la fecha seleccionada en el formato yyyy/mm/dd
    // @param y: año
    // @param m: mes
    // @param d: dia
    // @param isStart: si es la fecha de inicio o no
    String setDate(int y, int m, int d, boolean isStart){
        // formato de fecha: yyyy/mm/dd
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

    // funcion para establecer la hora seleccionada en el formato hh:mm:ss
    // @param h: hora
    // @param m: minuto
    // @param s: segundo
    // @param isStart: si es la hora de inicio o no
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
