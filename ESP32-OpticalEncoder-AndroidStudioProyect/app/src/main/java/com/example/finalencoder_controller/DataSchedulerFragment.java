package com.example.finalencoder_controller;

import android.content.Context;
import android.os.Bundle;
import android.service.controls.Control;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.example.finalencoder_controller.databinding.FragmentDeviceviewSchedulerBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class DataSchedulerFragment extends Fragment {

    private FragmentDeviceviewSchedulerBinding binding;
    ScheduleItemAdapter awaitScAdapterListView, finishedAdapterListView;
    String deviceName;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = FragmentDeviceviewSchedulerBinding.inflate(inflater, container, false);
        // set the fragment to the control center instance to be able to access it from the main activity
        ControlCenter.getInstance().dataSchFrag = this;
        
        // Create an empty ArrayList of await ScheduleItem objects
        ArrayList<ScheduleItem> scAwItems = new ArrayList<ScheduleItem>();
        // Create a new instance of ScheduleItemAdapter and pass it the context and ArrayList
        awaitScAdapterListView = new ScheduleItemAdapter(getContext(), scAwItems);
        // Get the ListView from layout 
        ListView awListView = (ListView) binding.deviceViewAwaitScheduleListView;
        // Set the ListView to use the ScheduleItemAdapter we created above
        awListView.setAdapter(awaitScAdapterListView);
        
        
        // Create an empty list of finished ScheduleItems to be displayed
        ArrayList<ScheduleItem> scFinItems = new ArrayList<ScheduleItem>();
        // Create a new ScheduleItemAdapter to display the list of ScheduleItems
        finishedAdapterListView = new ScheduleItemAdapter(getContext(), scFinItems);
        // Get the ListView from the layout
        ListView finListView = (ListView) binding.deviceViewDueScheduleListView;
        // Set the ListView to use the ScheduleItemAdapter
        finListView.setAdapter(finishedAdapterListView);

        // if the fragment is created with arguments, set the data view for the device
        if(getArguments() != null){
            // set the data view for the device with the arguments passed
            setDataViewFor(getArguments().getBoolean("isSchedules"), getArguments().getString("devName"));
        }
        return binding.getRoot();
    }

    // class to create a ScheduleItem object
    static class ScheduleItem {
        // ScheduleItem attributes (name, start time, end time, schedule type)
        public String scName;
        public DateType stTime, edTime;
        public int sType;

        // Setter for the schedule type
        public void setsTypeT(int t){
            sType = t;
        }

        // class to create a DateType object
        public static class DateType{
            // DateType attributes (year, month, day, hour, minute, second, millisecond) (format: yyyy/mm/ddThh:mm:ss:ms)
            public int year(){  return data[0]; }
            public int month(){ return data[1]; }
            public int day(){   return data[2]; }
            public int hour(){ return data[3]; }
            public int minute(){ return data[4]; }
            public int second(){ return data[5]; }
            public int millisecond(){ return data[6]; }

            // array to store the date and time
            public int[] data;

            // constructor to create a DateType object from a string
            public DateType(String dat){
                // split the string to get the date and time separately (format: yyyy/mm/ddThh:mm:ss:ms to: yyyy/mm/dd hh:mm:ss:ms)
                String[] aInfo = dat.split("T");
                String[] dInfo = aInfo[0].split("/");
                String[] tInfo = aInfo[1].split(":");
                // if the time is not specified, set it to 0 (00:00:00:000)
                data = new int[]{ Integer.parseInt(dInfo[0]), Integer.parseInt(dInfo[1]), Integer.parseInt(dInfo[2]),
                        Integer.parseInt(tInfo[0]), Integer.parseInt(tInfo[1]), Integer.parseInt(tInfo[2]), (tInfo.length < 4 ? 0 : Integer.parseInt(tInfo[3])) };
            }

            // toString method to return the date and time in a string format (format: yyyy/mm/dd\nhh:mm:ss.ms)
            @Override
            public String toString() {
                DecimalFormat dF = new DecimalFormat("00");
                return year() + "/" + dF.format(month()) + "/" + dF.format(day()) + "\n" + dF.format(hour()) + ":" + dF.format(minute()) + ":" + dF.format(second()) + "." + millisecond();
            }

            // method to return the date and time in a string format to be parsed (format: yyyy/mm/ddThh:mm:ss:ms)
            public  String toParse(){
                return year() + "/" + month() + "/" + day() + "T" + hour() + ":" + minute() + ":" + second() + ":" + millisecond();
            }

            // method to compare the date and time of two DateType objects
            public int compare(int[] toComp){
                for(int i = 0; i < data.length; i++){
                    if(data[i] == toComp[i]){ 
                        continue;
                    } else if(data[i] > toComp[i]){
                        // if the date and time of the current object is greater than the date and time of the object to compare, return 1
                        return 1;
                    } else {
                        // if the date and time of the current object is less than the date and time of the object to compare, return -1
                        return -1;
                    }
                }
                // if the date and time of the current object is equal to the date and time of the object to compare, return 0
                return 0;
            }
        }

        // constructor to create a ScheduleItem object from a string
        public ScheduleItem(String scheduleName, String startTime, String endTime, int sT){
            // set the attributes of the ScheduleItem object to the values passed
            scName = scheduleName; stTime = new DateType(startTime); edTime = new DateType(endTime);
            sType = sT;
        }

        // toString method to return the ScheduleItem object in a string format (format: scheduleName;startTime;endTime;scheduleType)
        @Override
        public String toString() {
            return  scName + ";" + stTime.toParse() + ";" + edTime.toParse() + ";" + sType;
        }
    }

    // class to create a ScheduleItemAdapter object
    static class ScheduleItemAdapter extends ArrayAdapter<ScheduleItem>{
        public ScheduleItemAdapter(@NonNull Context context, ArrayList<ScheduleItem> schedules) {
            super(context, 0, schedules);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            // get the ScheduleItem object at the position
            ScheduleItem schedule = getItem(position);
            // if the view is null, create a new view
            if(convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.schedule_list_item_1, parent, false);
            }

            // set the attributes of the view to the values of the ScheduleItem object
            TextView tvTStart = (TextView) convertView.findViewById(R.id.schedule_st_data);
            TextView tvTEnd = (TextView) convertView.findViewById(R.id.schedule_ed_data);
            TextView tvScName = (TextView) convertView.findViewById(R.id.schedule_name_data);
            ImageButton intButt = (ImageButton) convertView.findViewById(R.id.schedule_interactItem_button);
            // set the text of the TextViews to the values of the ScheduleItem object
            tvTStart.setText(schedule.stTime.toString());
            tvTEnd.setText(schedule.edTime.toString());
            tvScName.setText(schedule.scName);
            // get the value of the isSchedule attribute of the DataSchedulerFragment object
            final boolean isSchedule = ControlCenter.getInstance().dataSchFrag.isS;

            // if the schedule type is 0
            if(schedule.sType == 0){
                // set the onClickListener of the interact button to navigate to the ShowDataPointsFragment object
                intButt.setOnClickListener(view -> {
                    // create a new bundle to pass the data point information to the ShowDataPointsFragment object
                    Bundle bun = new Bundle();
                    // if the schedule is a schedule, get the data point information of the schedule (it may be a schedule or task)
                    if(isSchedule){
                        bun.putString("dataP", ControlCenter.getInstance().dataSchFrag.GetScDataPointInfo(schedule.scName));
                    }else{
                        // if the schedule is a task, get the data point information of the task
                        bun.putString("dataP", ControlCenter.getInstance().dataSchFrag.GetDataPointInfo(schedule.stTime.toParse()));
                    }
                    // navigate to the ShowDataPointsFragment object and pass the bundle with the data point information and the title of the fragment
                    ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_dataSchedulerFragment_to_showDataPointsFragment, bun, "Informacion de muestreo");
                });
            }else if(schedule.sType == 1){
                // if the schedule type is 1, do not show the interact button
                intButt.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }
    }

    // boolean to determine if the schedule is a schedule or a task
    public boolean isS = false;

    // method to set the data view for the schedule or task
    void setDataViewFor(boolean isSchedules, String devName){
        // set the isSchedule attribute to the value passed
        isS= isSchedules;
        // set the deviceName attribute to the value passed
        deviceName = devName;
        // if the schedule is a schedule, create the schedule items from the schedules of the device
        if(isSchedules){
            CreateSchItems(ControlCenter.getInstance().getData("schedules_", devName));
        }else{
            // if the schedule is a task, create the schedule items from the tasks points of the device
            CreateSchItems(ConvertPointsToSchedules(ControlCenter.getInstance().getData("data_", devName)));
            binding.deviceViewAwaitScheduleListView.setVisibility(View.GONE);
            binding.textViewDv.setVisibility(View.GONE);
        }
    }

    // method to get the data point information of a task
    String GetDataPointInfo(String stDate){
        // get the data points of the device 
        String dat = ControlCenter.getInstance().getData("data_", deviceName);
        // get the position of the data point with the start date passed (format: START;startDate; ... ;STOP;endDate;)
        int i = dat.indexOf(";"+ stDate +";");
        // return the data points
        return dat.substring( dat.lastIndexOf("START", i), dat.indexOf(";", dat.indexOf("STOP", i) + 6) + 1);
    }

    // method to get the data point information of a schedule
    String GetScDataPointInfo(String scName){
        // get the data points of the device
        String dat = ControlCenter.getInstance().getData("data_", deviceName);
        // get the position of the schedule with the name passed
        int scPos = dat.indexOf(";"+ scName + ";");
        // if the schedule is not found, return an empty string
        if(scPos < 0){ return ""; }
        // get the position of the next schedule
        int scLastPos = dat.indexOf("START", scPos);
        // if the next schedule is not found, set the position to the end of the data points
        if(scLastPos < 0){ scLastPos = dat.length(); }
        // return the data points of the schedule
        return  dat.substring(dat.lastIndexOf("START", scPos), scLastPos);
    }


    // TODO: check if the documentation is correct
    // method to convert the data points of a task to the data points of a schedule
    String ConvertPointsToSchedules(String dataP){
        // if the data points are empty, return an empty string
        if(Objects.equals(dataP, "")){ return ""; }
        // split the data points by the START string
        String[] pSplit = dataP.substring(1).split("START");
        // create a string to store the data points of the schedule
        String dataS = "";
        // for each data point
        for(int i = 0; i < pSplit.length; i++){
            // split the data point by the STOP string
            String[] sSplit = pSplit[i].split("STOP");
            // split the first part of the data point by the ; string
            String[] auxS = sSplit[0].split(";");
            // create a string to store the data point of the schedule
            String auxSch = "";
            // if the first part of the data point is empty, add the index of the data point to the string
            // else, add the first part of the data name to the string
            auxSch += (auxS[1].equals("") ? "#"+i : auxS[1]) + ";";
            // add the start date of the data point to the string
            auxSch += auxS[2] + ";";
            // add the end date of the data point to the string
            auxSch += sSplit[1].split(";")[1] + "-";
            // add the string to the data points of the schedule
            dataS += auxSch;
        }
        // return the schedule 
        return dataS;
    }

    // method to create the schedule items from the data points of a schedule or task
    void CreateSchItems(String dataS){
        // if the data points are empty, return
        if(dataS == null || Objects.equals(dataS, "") || !dataS.contains("-")){
            return;
        }
        // remove the new line characters from the data points
        String aux = dataS.replaceAll("\n", "");
        // split the data points by the - string
        String[] split = aux.split("-");

        // create a list to store the await schedule items
        List<ScheduleItem> awaScheduleItems = new ArrayList<ScheduleItem>();
        // create a list to store the finished schedule items
        List<ScheduleItem> finiScheduleItems = new ArrayList<ScheduleItem>();

        // get the actual date
        Calendar cal = Calendar.getInstance();
        // get the actual month
        int m = cal.get(Calendar.MONTH) + 1;
        // create a date type with the actual date and time (format: yyyy/MM/ddThh:mm:ss:ms)
        ScheduleItem.DateType actD = new ScheduleItem.DateType(
                cal.get(Calendar.YEAR)  +"/" + m + "/" + cal.get(Calendar.DAY_OF_MONTH) + "T" +
                        cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ":" + cal.get(Calendar.MILLISECOND));

        // for each data point
        for (String sc : split) {
            // if the data point is empty, continue
            if(sc.equals("")){ continue; }
            // split the data point by the ; string
            String[] sInfo = sc.split(";");
            // create a schedule item with the data point information
            ScheduleItem toS = new ScheduleItem(sInfo[0], sInfo[1], sInfo[2], 1);
            // if the end date of the data point is greater than the actual date, add the schedule item to the await schedule items list
            if (toS.edTime.compare(actD.data) >= 0) {
                awaScheduleItems.add(toS);
            } else {
                // else, add the schedule item to the finished schedule items list
                toS.setsTypeT(0);
                finiScheduleItems.add(toS);
            }
        }
        // if the await schedule items list is not empty, create the await schedule items
        if(awaScheduleItems.size() > 0){
            // create an array of schedule items with the size of the await schedule items list
            ScheduleItem[] auxArray = new  ScheduleItem[awaScheduleItems.size()];
            // convert the await schedule items list to an array
            auxArray =  awaScheduleItems.toArray(auxArray);
            // create the await schedule items
            CreateAllSchedules(1, auxArray);
        }
        // if the finished schedule items list is not empty, create the finished schedule items
        if(finiScheduleItems.size() > 0){
            // create an array of schedule items with the size of the finished schedule items list
            ScheduleItem[] auxArrayD = new ScheduleItem[finiScheduleItems.size()];
            // convert the finished schedule items list to an array
            auxArrayD =  finiScheduleItems.toArray(auxArrayD);
            // create the finished schedule items
            CreateAllSchedules(0, auxArrayD);
        }

    }

    // method to create the schedules
    void CreateAllSchedules(int sType, ScheduleItem[] scs){
        // check if the schedules are await schedules or finished schedules
        // if the schedules are await schedules, it will create the await schedule items
        // else, it will create the finished schedule items
        ScheduleItemAdapter toMod = (sType == 1 ? awaitScAdapterListView : finishedAdapterListView);

        // for each schedule item
        for (ScheduleItem scheduleIt : scs) {
            // create a variable to store the index of the schedule item
            int nIdx = 0;
            // for each schedule item in the schedules
            for (; nIdx < toMod.getCount(); nIdx++) {
                // get the schedule item
                ScheduleItem sC = (ScheduleItem) toMod.getItem(nIdx);
                // if the start date of the schedule item is greater than the start date of the schedule item to add, break
                if (sC.stTime.compare(scheduleIt.stTime.data) >= 0) {
                    break;
                }
            }
            // add the schedule item to the schedules
            toMod.insert(scheduleIt, nIdx);
        }
        // notify the schedules that the data has changed
        toMod.notifyDataSetChanged();
        // update the height of the schedules list view to show all the schedules
        ListView lv = (sType == 1 ? binding.deviceViewAwaitScheduleListView : binding.deviceViewDueScheduleListView );
        updateListHeight(lv);
    }

    // method to update the height of a list view
    void updateListHeight(ListView lv){
        // get the first item of the list view and measure it
        View li = lv.getAdapter().getView(0, null, lv);
        li.measure(0,0);
        // Get the ListView layout parameters
        ViewGroup.LayoutParams params = lv.getLayoutParams();
        // Set the height of the ListView to the sum of the heights of the items, plus the divider height
        params.height = (li.getMeasuredHeight() + lv.getDividerHeight()) * (lv.getAdapter().getCount());
        // Set the new layout parameters
        lv.setLayoutParams(params);
        // Request a layout pass
        lv.requestLayout();
    }

    // on view created method
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // on destroy view method
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}