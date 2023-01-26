package com.example.finalencoder_controller;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finalencoder_controller.databinding.FragmentSchedulerBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class SchedulerFragment extends Fragment {

    private FragmentSchedulerBinding binding;
    ScheduleItemAdapter awaitScAdapterListView, executingAdapterListView, finishedAdapterListView;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = FragmentSchedulerBinding.inflate(inflater, container, false);
        
        ArrayList<ScheduleItem> scAwItems = new ArrayList<ScheduleItem>();
        awaitScAdapterListView = new ScheduleItemAdapter(getContext(), scAwItems);
        ListView awListView = (ListView) binding.awaitScheduleListView;
        awListView.setAdapter(awaitScAdapterListView);

        ArrayList<ScheduleItem> scFinItems = new ArrayList<ScheduleItem>();
        finishedAdapterListView = new ScheduleItemAdapter(getContext(), scFinItems);
        ListView finListView = (ListView) binding.dueScheduleListView;
        finListView.setAdapter(finishedAdapterListView);

        ArrayList<ScheduleItem> scExItems = new ArrayList<ScheduleItem>();
        executingAdapterListView = new ScheduleItemAdapter(getContext(), scExItems);
        ListView exListView = (ListView) binding.executingScheduleListView;
        exListView.setAdapter(executingAdapterListView);


        binding.scheduleCreateScheduleButton.setOnClickListener( view -> {
            ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_ContentMainFragment_to_scheduleCreateFragment, "Agendar");
        });

        return binding.getRoot();
    }

    public static class ScheduleItem {
        public String scName;
        public DateType stTime, edTime;
        public int sType;

        public static class DateType{

            public int year(){  return data[0]; }
            public int month(){ return data[1]; }
            public int day(){   return data[2]; }
            public int hour(){ return data[3]; }
            public int minute(){ return data[4]; }
            public int second(){ return data[5]; }
            public int millisecond(){ return data[6]; }

            public int[] data;

            public DateType(String dat){
                // separa la fecha de: yyyy/mm/ddThh:mm:ss:ms a: yyyy/mm/dd hh:mm:ss.ms
                String[] aInfo = dat.split("T");
                String[] dInfo = aInfo[0].split("/");
                String[] tInfo = aInfo[1].split(":");

                data = new int[]{ Integer.parseInt(dInfo[0]), Integer.parseInt(dInfo[1]), Integer.parseInt(dInfo[2]),
                        Integer.parseInt(tInfo[0]), Integer.parseInt(tInfo[1]), Integer.parseInt(tInfo[2]),  (tInfo.length < 4 ? 0 : Integer.parseInt(tInfo[3]))};
            }

            @Override
            public String toString() {
                DecimalFormat dF = new DecimalFormat("00");
                //  format: yyyy/mm/dd hh:mm:ss.ms
                return year() + "/" + dF.format(month()) + "/" + dF.format(day()) + "\n" + dF.format(hour()) + ":" + dF.format(minute()) + ":" + dF.format(second()) + "." + millisecond();
            }

            public  String toParse(){
                // Return the date and time in the format: yyyy/mm/ddThh:mm:ss:ms
                return year() + "/" + month() + "/" + day() + "T" + hour() + ":" + minute() + ":" + second() + ":" + millisecond();
            }

            // compara dos fechas
            // @param toComp: fecha a comparar
            // @return: 0: iguales, 1: mayor, -1: menor
            public int compare(int[] toComp){
                for(int i = 0; i < data.length; i++){
                    if(data[i] == toComp[i]){ 
                        continue;
                    } else if(data[i] > toComp[i]){ 
                        return 1;
                    } else { 
                        return -1; 
                    }
                }
                return 0;
            }
        }

        // define el tipo de la tarea 
        public void setsTypeT(int t){
            sType = t;
        }

        // define un scheduleItem
        public ScheduleItem(String scheduleName, String startTime, String endTime, int sT){
            scName = scheduleName; stTime = new DateType(startTime); edTime = new DateType(endTime);
            sType = sT;
        }

        // retorna el scheduleItem en formato de string
        @Override
        public String toString() {
            return  scName + ";" + stTime.toParse() + ";" + edTime.toParse() + ";" + sType;
        }
    }

    // clase para adaptar los datos de los scheduleItems a la vista
    public class ScheduleItemAdapter extends ArrayAdapter<ScheduleItem>{
        public ScheduleItemAdapter(@NonNull Context context, ArrayList<ScheduleItem> schedules) {
            super(context, 0, schedules);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            ScheduleItem schedule = getItem(position);
            if(convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.schedule_list_item_1, parent, false);
            }

            TextView tvTStart = (TextView) convertView.findViewById(R.id.schedule_st_data);
            TextView tvTEnd = (TextView) convertView.findViewById(R.id.schedule_ed_data);
            TextView tvScName = (TextView) convertView.findViewById(R.id.schedule_name_data);
            ImageButton intButt = (ImageButton) convertView.findViewById(R.id.schedule_interactItem_button);

            tvTStart.setText(schedule.stTime.toString());
            tvTEnd.setText(schedule.edTime.toString());
            tvScName.setText(schedule.scName);

            // si el schedule es de tipo 0, o finalizado, se muestra el boton de informacion
            if(schedule.sType == 0){
               try {
                   intButt.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.green));
                   intButt.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           Bundle bun = new Bundle();
                           bun.putString("dataP", ControlCenter.getInstance().schedulerFrag.GetDataPointInfo(schedule.scName));
                           bun.putString("devName", ControlCenter.getInstance().schedulerFrag.GetDataPointInfo(ControlCenter.getInstance().connectionFrag.devName));

                           ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_ContentMainFragment_to_showDataPointsFragment, bun, "Informacion de muestreo");
                       }
                   });
               }catch (Exception e){
                   ControlCenter.getInstance().mainActivity.makeSnackB("Error al mostrar los datos");
               }
               // si el schedule es de tipo 1, o en espera, se muestra el boton de borrar
            }else if(schedule.sType == 1){
                try {
                    intButt.setImageResource(android.R.drawable.ic_menu_delete);
                    intButt.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.red));
                    intButt.setOnClickListener(view -> {
                        ControlCenter.getInstance().connectionFrag.sendCommand("SCHEDULE;DELETE;" + schedule.scName + ";",
                                () -> deleteAwaitSchedule(position), 20000);
                    });
                } catch (Exception e){
                    // error al borrar una schedule
                    ControlCenter.getInstance().mainActivity.makeSnackB("Error al borrar el agendamiento");
                }
            }else if(schedule.sType == 2){
                try {
                    intButt.setImageResource(android.R.drawable.ic_menu_search);
                    intButt.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.purple_700));
                    intButt.setOnClickListener(view -> ControlCenter.getInstance().mainContent.navigateViewPag(0) );
                } catch (Exception e){
                    ControlCenter.getInstance().mainActivity.makeSnackB("Error al cambiar de pestaña");
                }
            }
            return convertView;
        }
    }


    // obtiene la informacion de un agendamiento en especifico
    // @param scName: nombre del agendamiento
    // @return: informacion del agendamiento
    String GetDataPointInfo(String scName){
        // obtine los datos almacenados en el archivo "data_"
        String dat = ControlCenter.getInstance().getData("data_", ControlCenter.getInstance().connectionFrag.devName);
        // si el archivo contiene un error va a tener el # al final
        if (dat.contains("#")){
            // en ese caso se elimina para que no afecte al resto de la funcion
            dat= dat.replace("#","");
        }
        // obtiene la posicion del agendemiento
        int scPos = dat.indexOf(";"+ scName + ";");
        // verifica que sea una posicion valida si no retorna vacio
        if(scPos < 0){ return ""; }
        // obtiene la ultima posicion del agendamiento (busca el siguiente START) (format: START;NAME;FECHAINI;INTERVAL;MESUAREUNIT;....STOP;FECHAFIN;)
        int scLastPos = dat.indexOf("START", scPos);
        // verifica que la posicion sea valida, si no asigna el tamaño completo del archivo
        if(scLastPos < 0){ scLastPos = dat.length(); }
        // devuelve todos los datos del schedule
        return  dat.substring(dat.lastIndexOf("START", scPos), scLastPos);
    }

    // elimina un agendamiento de la lista de espera y del archivo schedules_
    // @param pos: posicion del agendamiento en la lista
    void deleteAwaitSchedule(int pos){
        ScheduleItem sc = (ScheduleItem) awaitScAdapterListView.getItem(pos);
        String scString = sc.toString();
        scString = scString.substring(0,scString.length()-3);

        if(!ControlCenter.getInstance().deleteData(scString+"-\n", "schedules_")){ //VA DEPENDDER DE COMO FUNCIONE EL ENCODER
            ControlCenter.getInstance().deleteData(scString+"-", "schedules_");
        }

        awaitScAdapterListView.remove(sc);
        awaitScAdapterListView.notifyDataSetChanged();

        updateListHeight(binding.awaitScheduleListView);
    }

    // funcion para determinar si un agendamiento esta disponible
    // @param sc: agendamiento a verificar
    // @return: 0 si esta disponible, -1 si no esta disponible, -2 si ya paso, -3 si es menor a la hora actual
    public int isScheduleAvailable(ScheduleItem sc){
        
        boolean dCheck = false;

        Calendar cal = Calendar.getInstance();
        String dt = cal.get(Calendar.YEAR) +"/" + (cal.get(Calendar.MONTH) + 1) + "/"+ cal.get(Calendar.DAY_OF_MONTH) + "T" + cal.get(Calendar.HOUR_OF_DAY) + ":" +  cal.get(Calendar.MINUTE) + ":0";
        ScheduleItem.DateType actDate = new ScheduleItem.DateType(dt);
        if(actDate.compare(sc.stTime.data) >= 0){
            return -3;
        }

        for(int i = 0; i < awaitScAdapterListView.getCount(); i++){
            ScheduleItem sC = (ScheduleItem)awaitScAdapterListView.getItem(i);

            if(Objects.equals(sC.scName, sc.scName)){
                return -2;
            }

            if(dCheck){ continue; }
            if((sC.stTime.compare(sc.stTime.data) <= 0 && sC.edTime.compare(sc.stTime.data) >= 0)
                    || (sC.stTime.compare(sc.edTime.data) <= 0 && sC.edTime.compare(sc.edTime.data) >= 0)){
                return -1;
            } 
            else if(sC.edTime.compare(sc.stTime.data) <= 0 || sC.stTime.compare(sc.edTime.data) >= 0){
                dCheck = true;
            }
        }
        return 1;
    }

    // funcion actualizar el estado de un agendamiento
    // @param state: estado del agendamiento
    void scheduleStateChanged(int state){
        // 0: finalizado
        if(state == 0){
            ScheduleItem sC = (ScheduleItem)executingAdapterListView.getItem(0);
            sC.sType = state;

            finishedAdapterListView.add(sC);
            executingAdapterListView.remove(executingAdapterListView.getItem(0));

            executingAdapterListView.notifyDataSetChanged();
            finishedAdapterListView.notifyDataSetChanged();

            updateListHeight(binding.executingScheduleListView);
            updateListHeight(binding.dueScheduleListView);

        }else{ //2 ejecutando
            ScheduleItem sC = (ScheduleItem)awaitScAdapterListView.getItem(0);
            sC.sType = state;

            executingAdapterListView.add(sC);
            awaitScAdapterListView.remove(awaitScAdapterListView.getItem(0));

            executingAdapterListView.notifyDataSetChanged();
            awaitScAdapterListView.notifyDataSetChanged();

            updateListHeight(binding.executingScheduleListView);
            updateListHeight(binding.awaitScheduleListView);
        }
        //  se actualiza la lista de agendamientos
        updateListHeight(binding.executingScheduleListView);
    }

    // funcion para eliminar todos los agendamientos
    public void cleanAllSchedules(){
        awaitScAdapterListView.clear();
        awaitScAdapterListView.notifyDataSetChanged();
        finishedAdapterListView.clear();
        finishedAdapterListView.notifyDataSetChanged();
        executingAdapterListView.clear();
        executingAdapterListView.notifyDataSetChanged();
        updateListHeight(binding.awaitScheduleListView);
        updateListHeight(binding.dueScheduleListView);
        updateListHeight(binding.executingScheduleListView);
    }

    // funcion para recrear todos los agendamientos
    // @param sType: tipo de agendamiento
    // @param scs: lista de agendamientos
    public void recreateAllSchedules(int sType, ScheduleItem[] scs){
        ScheduleItemAdapter toMod = (sType == 1 ? awaitScAdapterListView : (sType == 0 ? finishedAdapterListView : executingAdapterListView));
        toMod.clear();

        for (ScheduleItem scheduleIt : scs) {
            int nIdx = 0;
            for (; nIdx < toMod.getCount(); nIdx++) {
                ScheduleItem sC = (ScheduleItem) toMod.getItem(nIdx);
                if (sC.stTime.compare(scheduleIt.stTime.data) >= 0) {
                    break;
                }
            }
            toMod.insert(scheduleIt, nIdx);
        }

       // asigna list view de agendamientos a modificar
        ListView lv = (sType == 1 ? binding.awaitScheduleListView : (sType == 0 ? binding.dueScheduleListView : binding.executingScheduleListView));
        updateListHeight(lv);
    }

    // funcion para agregar un agendamiento
    // @param scheduleIt: agendamiento a agregar
    public void addSchedule(ScheduleItem scheduleIt){
        int nIdx = 0;
        ScheduleItemAdapter toMod = (scheduleIt.sType == 1 ? awaitScAdapterListView : (scheduleIt.sType == 0 ? finishedAdapterListView : executingAdapterListView));

        for(; nIdx < toMod.getCount(); nIdx++){
            ScheduleItem sC = (ScheduleItem)toMod.getItem(nIdx);
            if( sC.stTime.compare(scheduleIt.stTime.data) >= 0){ break; }
        }

        ControlCenter.getInstance().saveData(scheduleIt.toString().substring(0, scheduleIt.toString().length() - 2) + "-", "schedules_", true);
        ControlCenter.getInstance().setReceivedMessage(ControlCenter.getInstance().getData("schedules_"));

        toMod.insert(scheduleIt, nIdx);
        
        toMod.notifyDataSetChanged();

        ListView lv = (scheduleIt.sType == 1 ? binding.awaitScheduleListView : (scheduleIt.sType == 0 ? binding.dueScheduleListView : binding.executingScheduleListView));
        updateListHeight(lv);
    }

    // funcion para actualizar la altura de una lista
    // @param lv: lista a actualizar
    void updateListHeight(ListView lv){
        if(lv.getAdapter().getCount() == 0){
            ViewGroup.LayoutParams params = lv.getLayoutParams();
            params.height = 0;
            lv.setLayoutParams(params);
            lv.requestLayout();

            return;
        }
        View li = lv.getAdapter().getView(0, null, lv);
        li.measure(0,0);

        ViewGroup.LayoutParams params = lv.getLayoutParams();
        params.height = (li.getMeasuredHeight() + lv.getDividerHeight()) * (lv.getAdapter().getCount());
        lv.setLayoutParams(params);
        lv.requestLayout();
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
