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
import java.util.Objects;

public class SchedulerFragment extends Fragment {

    // Variables to store the binding and the adapters for the list views
    private FragmentSchedulerBinding binding;
    ScheduleItemAdapter awaitScAdapterListView, executingAdapterListView, finishedAdapterListView;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = FragmentSchedulerBinding.inflate(inflater, container, false);
        
        // Create a new ArrayList of ScheduleItem objects
        ArrayList<ScheduleItem> scAwItems = new ArrayList<ScheduleItem>();
        // Create a new adapter with the current context and the ArrayList
        awaitScAdapterListView = new ScheduleItemAdapter(getContext(), scAwItems);
        // Get a reference to the ListView, and attach the adapter to the listView
        ListView awListView = (ListView) binding.awaitScheduleListView;
        awListView.setAdapter(awaitScAdapterListView);

        // Create an ArrayList to hold the ScheduleItem objects that get passed to the adapter
        ArrayList<ScheduleItem> scFinItems = new ArrayList<ScheduleItem>();
        // Create a ScheduleItemAdapter object to handle the ArrayList
        finishedAdapterListView = new ScheduleItemAdapter(getContext(), scFinItems);
        // Get a reference to the ListView in the activity
        ListView finListView = (ListView) binding.dueScheduleListView;
        // Set the adapter to the ListView in the activity
        finListView.setAdapter(finishedAdapterListView);

        // Create an empty ArrayList of ScheduleItems
        ArrayList<ScheduleItem> scExItems = new ArrayList<ScheduleItem>();
        // Create a new Adapter for executing ScheduleItems
        executingAdapterListView = new ScheduleItemAdapter(getContext(), scExItems);
        // Find the ListView in the layout
        ListView exListView = (ListView) binding.executingScheduleListView;
        // Set the ListView to use the Adapter
        exListView.setAdapter(executingAdapterListView);


        // Create a new OnClickListener for the create schedule button
        binding.scheduleCreateScheduleButton.setOnClickListener( view -> {
            // Navigate to the create schedule fragment
            ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_ContentMainFragment_to_scheduleCreateFragment, "Agendar");
        });

        // return the root view of the fragment
        return binding.getRoot();
    }

    // Class to handle the ScheduleItemAdapter 
    public static class ScheduleItem {
        // Variables to store the information of the ScheduleItem
        public String scName;
        public DateType stTime, edTime;
        public int sType;

        // Class to handle the date and time of the ScheduleItem
        public static class DateType{

            // Getters for the date and time
            public int year(){  return data[0]; }
            public int month(){ return data[1]; }
            public int day(){   return data[2]; }
            public int hour(){ return data[3]; }
            public int minute(){ return data[4]; }
            public int second(){ return data[5]; }
            public int millisecond(){ return data[6]; }

            // Array to store the date and time
            public int[] data;

            // Constructor to create a new DateType object
            public DateType(String dat){
                // Split the date and time into an array ( from format: yyyy/mm/ddThh:mm:ss:ms to: yyyy/mm/dd hh:mm:ss.ms)
                String[] aInfo = dat.split("T");
                String[] dInfo = aInfo[0].split("/");
                String[] tInfo = aInfo[1].split(":");

                // Store the date and time in the data array (format: yyyy, mm, dd, hh, mm, ss, ms)
                data = new int[]{ Integer.parseInt(dInfo[0]), Integer.parseInt(dInfo[1]), Integer.parseInt(dInfo[2]),
                        Integer.parseInt(tInfo[0]), Integer.parseInt(tInfo[1]), Integer.parseInt(tInfo[2]),  (tInfo.length < 4 ? 0 : Integer.parseInt(tInfo[3]))};
            }

            // Method to convert the date and time to a string
            @Override
            public String toString() {
                // Create a new DecimalFormat object to format the date and time
                DecimalFormat dF = new DecimalFormat("00");
                // Return the date and time in the format: yyyy/mm/dd hh:mm:ss.ms
                return year() + "/" + dF.format(month()) + "/" + dF.format(day()) + "\n" + dF.format(hour()) + ":" + dF.format(minute()) + ":" + dF.format(second()) + "." + millisecond();
            }

            // Method to convert the date and time to a string to parse
            public  String toParse(){
                // Return the date and time in the format: yyyy/mm/ddThh:mm:ss:ms
                return year() + "/" + month() + "/" + day() + "T" + hour() + ":" + minute() + ":" + second() + ":" + millisecond();
            }

            // Method to compare the date and time of two DateType objects
            public int compare(int[] toComp){
                // Loop through the data array
                for(int i = 0; i < data.length; i++){
                    // Compare the data of the two DateType objects
                    if(data[i] == toComp[i]){ 
                        // If the data is the same, continue to the next data
                        continue;
                    } else if(data[i] > toComp[i]){ 
                        // If the data of the current DateType object is greater than the data of the other DateType object, return 1
                        return 1;
                    } else { 
                        // If the data of the current DateType object is less than the data of the other DateType object, return -1
                        return -1; 
                    }
                }
                // If the data of the two DateType objects is the same, return 0
                return 0;
            }
        }

        // Setters for the ScheduleItem variables
        public void setsTypeT(int t){
            sType = t;
        }

        // Constructor to create a new ScheduleItem object
        public ScheduleItem(String scheduleName, String startTime, String endTime, int sT){
            // Set the ScheduleItem variables
            scName = scheduleName; stTime = new DateType(startTime); edTime = new DateType(endTime);
            sType = sT;
        }

        // Method to convert the ScheduleItem to a string to parse
        @Override
        public String toString() {
            // Return the ScheduleItem in the format: scheduleName;startTime;endTime;scheduleType
            return  scName + ";" + stTime.toParse() + ";" + edTime.toParse() + ";" + sType;
        }
    }

    // Class to handle the ScheduleItemAdapter
    public class ScheduleItemAdapter extends ArrayAdapter<ScheduleItem>{
        // Constructor to create a new ScheduleItemAdapter object
        public ScheduleItemAdapter(@NonNull Context context, ArrayList<ScheduleItem> schedules) {
            super(context, 0, schedules);
        }

        // Method to get the view of the ScheduleItemAdapter
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            // Get the ScheduleItem object from the position of the ScheduleItemAdapter 
            ScheduleItem schedule = getItem(position);
            // Check if the view is null
            if(convertView == null){
                // Inflate the view
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.schedule_list_item_1, parent, false);
            }

            // Get the views from the layout
            TextView tvTStart = (TextView) convertView.findViewById(R.id.schedule_st_data);
            TextView tvTEnd = (TextView) convertView.findViewById(R.id.schedule_ed_data);
            TextView tvScName = (TextView) convertView.findViewById(R.id.schedule_name_data);
            ImageButton intButt = (ImageButton) convertView.findViewById(R.id.schedule_interactItem_button);

            // Set the views to the data of the ScheduleItem object
            tvTStart.setText(schedule.stTime.toString());
            tvTEnd.setText(schedule.edTime.toString());
            tvScName.setText(schedule.scName);

            // Check the schedule type
            if(schedule.sType == 0){
               try {
                   // Set the Image tint to green
                   intButt.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.green));
                   // Crate a new event listener for the IntButton
                   intButt.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View view) {
                           // Create a new Bundle object to pass the data to the ShowDataPointsFragment
                           Bundle bun = new Bundle();
                           // Put the data to the Bundle object
                           bun.putString("dataP", ControlCenter.getInstance().schedulerFrag.GetDataPointInfo(schedule.scName));
                           // Navigate to the ShowDataPointsFragment with the Bundle object
                           ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_ContentMainFragment_to_showDataPointsFragment, bun, "Informacion de muestreo");
                       }
                   });
               }catch (Exception e){
                   // error al mostrar la informacion del muestreo
                   ControlCenter.getInstance().mainActivity.makeSnackB("Error al mostrar los datos");
               }
            }else if(schedule.sType == 1){
                try {
                    // Set the Image tint to red if the schedule is a delete schedule
                    intButt.setImageResource(android.R.drawable.ic_menu_delete);
                    intButt.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.red));
                    // Crate a new event listener for the IntButton
                    intButt.setOnClickListener(view -> {
                        // Send the command to delete the schedule to the device
                        ControlCenter.getInstance().connectionFrag.sendCommand("SCHEDULE;DELETE;" + schedule.scName + ";",
                                () -> deleteAwaitSchedule(position), 20000);
                    });
                } catch (Exception e){
                    // error al borrar una schedule
                    ControlCenter.getInstance().mainActivity.makeSnackB("Error al borrar el agendamiento");
                }
            }else if(schedule.sType == 2){
                try {
                    // Set the Image tint to blue if the schedule is a update schedule
                    intButt.setImageResource(android.R.drawable.ic_menu_search);
                    intButt.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.purple_700));
                    // Crate a new event listener for the IntButton
                    intButt.setOnClickListener(view -> ControlCenter.getInstance().mainContent.navigateViewPag(0) );
                } catch (Exception e){
                    // error al navegar al menu
                    ControlCenter.getInstance().mainActivity.makeSnackB("Error al cambiar de pestaña");
                }
            }
            // Return the view 
            return convertView;
        }
    }


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

    // Method to delete a schedule from the await schedule list
    void deleteAwaitSchedule(int pos){
        // Get the schedule from the position of the await schedule list
        ScheduleItem sc = (ScheduleItem) awaitScAdapterListView.getItem(pos);
        // Get the string of the schedule
        String scString = sc.toString();
        // Remove the last 3 characters of the string
        scString = scString.substring(0,scString.length()-3);

        // Delete the schedule from the device
        if(!ControlCenter.getInstance().deleteData(scString+"-\n", "schedules_")){ //VA DEPENDDER DE COMO FUNCIONE EL ENCODER
            ControlCenter.getInstance().deleteData(scString+"-", "schedules_");
        }

        // Remove the schedule from the await schedule list
        awaitScAdapterListView.remove(sc);
        // Notify the adapter that the data has changed
        awaitScAdapterListView.notifyDataSetChanged();

        // Update the height of the list
        updateListHeight(binding.awaitScheduleListView);
    }

    // Method to check if a schedule is available
    public int isScheduleAvailable(ScheduleItem sc){
        
        boolean dCheck = false;

        // Check if the schedule is in the await schedule list
        for(int i = 0; i < awaitScAdapterListView.getCount(); i++){
            // Get the schedule from the position of the await schedule list
            ScheduleItem sC = (ScheduleItem)awaitScAdapterListView.getItem(i);

            // Check if the schedule name is the same
            if(Objects.equals(sC.scName, sc.scName)){
                return -2;
            }

            // if dCheck is true continue the loop
            if(dCheck){ continue; }
            // compare the start and end time of the schedules to check if the schedules are available
            if(sC.edTime.compare(sc.stTime.data) <= 0 || sC.stTime.compare(sc.edTime.data) >= 0){
                // if the schedules are available set dCheck to true to continue the loop
                dCheck = true;
            }else if((sC.stTime.compare(sc.stTime.data) >= 0 && sC.edTime.compare(sc.stTime.data) <= 0)
                    || (sC.stTime.compare(sc.edTime.data) >= 0 && sC.edTime.compare(sc.edTime.data) <= 0)){
                // if the schedules are not available return -1 (error code)
                return -1; 
            }
        }
        // return 1 if the schedule is available
        return 1;
    }

    // Method to check if the schedule state has changed
    void scheduleStateChanged(int state){ //SET STYPE TO FINISH
        // Check if the schedule state is 0 (finished)
        if(state == 0){
            // Get the schedule from the position 0 of the executing schedule list
            ScheduleItem sC = (ScheduleItem)executingAdapterListView.getItem(0);
            // Set the schedule state to finished (0)
            sC.sType = state;

            // Add the schedule to the finished schedule list
            finishedAdapterListView.add(sC);
            // Remove the schedule from the executing schedule list
            executingAdapterListView.remove(executingAdapterListView.getItem(0));

            // Notify the adapter that the data has changed
            executingAdapterListView.notifyDataSetChanged();
            finishedAdapterListView.notifyDataSetChanged();

            // Update the height of the list
            updateListHeight(binding.dueScheduleListView);

        }else{ //2 ejecutando
            // if the schedule state is not 0 (finished) then the schedule state is 2 (executing)
            // Get the schedule from the position 0 of the await schedule list
            ScheduleItem sC = (ScheduleItem)awaitScAdapterListView.getItem(0);
            // Set the schedule state to executing (2)
            sC.sType = state;

            // Add the schedule to the executing schedule list
            executingAdapterListView.add(sC);
            // Remove the schedule from the await schedule list
            awaitScAdapterListView.remove(awaitScAdapterListView.getItem(0));

            // Notify the adapter that the data has changed
            executingAdapterListView.notifyDataSetChanged();
            awaitScAdapterListView.notifyDataSetChanged();

            // Update the height of the list
            updateListHeight(binding.awaitScheduleListView);
        }

        // Update the height of the list
        updateListHeight(binding.executingScheduleListView);
    }

    // Method to recreate all the schedules
    public void recreateAllSchedules(int sType, ScheduleItem[] scs){
        // Check if the schedule type is 1 (await) if not check if the schedule type is 0 (finished) if not set the schedule type to executing (2)
        ScheduleItemAdapter toMod = (sType == 1 ? awaitScAdapterListView : (sType == 0 ? finishedAdapterListView : executingAdapterListView));
        // Clear the schedule list
        toMod.clear();

        // Add all the schedules to the schedule list
        for (ScheduleItem scheduleIt : scs) {
            // declare a variable to store the position of the schedule
            int nIdx = 0;
            // loop through the schedule list
            for (; nIdx < toMod.getCount(); nIdx++) {
                // Get the schedule from the position of the schedule list
                ScheduleItem sC = (ScheduleItem) toMod.getItem(nIdx);
                // Compare the start time of the schedule to the start time of the schedule in the schedule list
                if (sC.stTime.compare(scheduleIt.stTime.data) >= 0) {
                    // if the start time of the schedule is greater than the start time of the schedule in the schedule list then break the loop
                    break;
                }
            }
            // Add the schedule to the schedule list
            toMod.insert(scheduleIt, nIdx);
        }
        // Notify the adapter that the data has changed
        toMod.notifyDataSetChanged();

        // check if the schedule type is 1 (await) if not check if the schedule type is 0 (finished) if not set the schedule type to executing (2)
        // if the schedule type is 1 (await) then update the height of the await schedule list
        // if the schedule type is 0 (finished) then update the height of the finished schedule list
        // if the schedule type is 2 (executing) then update the height of the executing schedule list
        ListView lv = (sType == 1 ? binding.awaitScheduleListView : (sType == 0 ? binding.dueScheduleListView : binding.executingScheduleListView));
        // Update the height of the list
        updateListHeight(lv);
    }

    // Method to add a schedule to the schedule list
    public void addSchedule(ScheduleItem scheduleIt){
        // declare a variable to store the position of the schedule
        int nIdx = 0;
        // check if the schedule type is 1 (await) if not check if the schedule type is 0 (finished) if not set the schedule type to executing (2)
        ScheduleItemAdapter toMod = (scheduleIt.sType == 1 ? awaitScAdapterListView : (scheduleIt.sType == 0 ? finishedAdapterListView : executingAdapterListView));

        // loop through the schedule list
        for(; nIdx < toMod.getCount(); nIdx++){
            // Get the schedule from the position of the schedule list
            ScheduleItem sC = (ScheduleItem)toMod.getItem(nIdx);
            // Compare the start time of the schedule to the start time of the schedule in the schedule list
            // if the start time of the schedule is greater than the start time of the schedule in the schedule list then break the loop
            if( sC.stTime.compare(scheduleIt.stTime.data) >= 0){ break; }
        }

        // Add the schedule to the schedule list and save the schedule list
        ControlCenter.getInstance().saveData(scheduleIt.toString().substring(0, scheduleIt.toString().length() - 2) + "-", "schedules_", true);
        // Set the received message to the schedule list
        ControlCenter.getInstance().setReceivedMessage(ControlCenter.getInstance().getData("schedules_"));

        // Add the schedule to the schedule list
        toMod.insert(scheduleIt, nIdx);
        
        // Notify the adapter that the data has changed
        toMod.notifyDataSetChanged();

        // check if the schedule type is 1 (await) if not check if the schedule type is 0 (finished) if not set the schedule type to executing (2)
        // if the schedule type is 1 (await) then update the height of the await schedule list
        // if the schedule type is 0 (finished) then update the height of the finished schedule list
        // if the schedule type is 2 (executing) then update the height of the executing schedule list
        ListView lv = (scheduleIt.sType == 1 ? binding.awaitScheduleListView : (scheduleIt.sType == 0 ? binding.dueScheduleListView : binding.executingScheduleListView));
        // Update the height of the list
        updateListHeight(lv);
    }

    // Method to update the height of the list
    void updateListHeight(ListView lv){
        // Get the first item of the list
        View li = lv.getAdapter().getView(0, null, lv);
        // Measure the height of the first item of the list
        li.measure(0,0);

        // get the layout parameters of the list
        ViewGroup.LayoutParams params = lv.getLayoutParams();
        // set the height of the list to the height of the first item of the list multiplied by the number of items in the list
        params.height = (li.getMeasuredHeight() + lv.getDividerHeight()) * (lv.getAdapter().getCount());
        // set the layout parameters of the list
        lv.setLayoutParams(params);
        // request the layout of the list
        lv.requestLayout();
    }

    // on View Created method
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // on Destroy View method to set the binding to null
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}