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
        ControlCenter.getInstance().dataSchFrag = this;

        ArrayList<ScheduleItem> scAwItems = new ArrayList<ScheduleItem>();
        awaitScAdapterListView = new ScheduleItemAdapter(getContext(), scAwItems);
        ListView awListView = (ListView) binding.deviceViewAwaitScheduleListView;
        awListView.setAdapter(awaitScAdapterListView);

        ArrayList<ScheduleItem> scFinItems = new ArrayList<ScheduleItem>();
        finishedAdapterListView = new ScheduleItemAdapter(getContext(), scFinItems);
        ListView finListView = (ListView) binding.deviceViewDueScheduleListView;
        finListView.setAdapter(finishedAdapterListView);

        if(getArguments() != null){
            setDataViewFor(getArguments().getBoolean("isSchedules"), getArguments().getString("devName"));
        }


        return binding.getRoot();

    }


    static class ScheduleItem {
        public String scName;
        public DateType stTime, edTime;
        public int sType;

        public void setsTypeT(int t){
            sType = t;
        }

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
                String[] aInfo = dat.split("T");
                String[] dInfo = aInfo[0].split("/");
                String[] tInfo = aInfo[1].split(":");

                data = new int[]{ Integer.parseInt(dInfo[0]), Integer.parseInt(dInfo[1]), Integer.parseInt(dInfo[2]),
                        Integer.parseInt(tInfo[0]), Integer.parseInt(tInfo[1]), Integer.parseInt(tInfo[2]), (tInfo.length < 4 ? 0 : Integer.parseInt(tInfo[3])) };
            }

            @Override
            public String toString() {
                DecimalFormat dF = new DecimalFormat("00");
                return year() + "/" + dF.format(month()) + "/" + dF.format(day()) + "\n" + dF.format(hour()) + ":" + dF.format(minute()) + ":" + dF.format(second()) + "." + millisecond();
            }

            public  String toParse(){
                return year() + "/" + month() + "/" + day() + "T" + hour() + ":" + minute() + ":" + second() + ":" + millisecond();
            }

            public int compare(int[] toComp){
                for(int i = 0; i < data.length; i++){
                    if(data[i] == toComp[i]){ continue;
                    } else if(data[i] > toComp[i]){ return 1;
                    } else { return -1; }
                }

                return 0;
            }
        }

        public ScheduleItem(String scheduleName, String startTime, String endTime, int sT){
            scName = scheduleName; stTime = new DateType(startTime); edTime = new DateType(endTime);
            sType = sT;
        }

        @Override
        public String toString() {
            return  scName + ";" + stTime.toParse() + ";" + edTime.toParse() + ";" + sType;
        }
    }

    static class ScheduleItemAdapter extends ArrayAdapter<ScheduleItem>{
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

            final boolean isSchedule = ControlCenter.getInstance().dataSchFrag.isS;

            if(schedule.sType == 0){
                intButt.setOnClickListener(view -> {
                    Bundle bun = new Bundle();
                    if(isSchedule){
                        bun.putString("dataP", ControlCenter.getInstance().dataSchFrag.GetScDataPointInfo(schedule.scName));
                    }else{
                        bun.putString("dataP", ControlCenter.getInstance().dataSchFrag.GetDataPointInfo(schedule.stTime.toParse()));

                    }
                    ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_dataSchedulerFragment_to_showDataPointsFragment, bun, "Informacion de muestreo");

                });
            }else if(schedule.sType == 1){
                intButt.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }
    }

    public boolean isS = false;
    void setDataViewFor(boolean isSchedules, String devName){
        isS= isSchedules;
        deviceName = devName;
        if(isSchedules){

            CreateSchItems(ControlCenter.getInstance().getData("schedules_", devName));
        }else{
            CreateSchItems(ConvertPointsToSchedules(ControlCenter.getInstance().getData("data_", devName)));
            binding.deviceViewAwaitScheduleListView.setVisibility(View.GONE);
            binding.textViewDv.setVisibility(View.GONE);
        }

    }

    String GetDataPointInfo(String stDate){
        String dat = ControlCenter.getInstance().getData("data_", deviceName);
        int i = dat.indexOf(";"+ stDate +";");
        return dat.substring( dat.lastIndexOf("START", i), dat.indexOf(";", dat.indexOf("STOP", i) + 6) + 1);
    }

    String GetScDataPointInfo(String scName){
        String dat = ControlCenter.getInstance().getData("data_", deviceName);

        int scPos = dat.indexOf(";"+ scName + ";");
        if(scPos < 0){ return ""; }

        int scLastPos = dat.indexOf("START", scPos);
        if(scLastPos < 0){ scLastPos = dat.length(); }

        return  dat.substring(dat.lastIndexOf("START", scPos), scLastPos);
    }


    String ConvertPointsToSchedules(String dataP){
        if(Objects.equals(dataP, "")){ return ""; }

        String[] pSplit = dataP.substring(1).split("START");

        String dataS = "";

        for(int i = 0; i < pSplit.length; i++){

            String[] sSplit = pSplit[i].split("STOP");
            String[] auxS = sSplit[0].split(";");
            String auxSch = "";

            auxSch += (auxS[1].equals("") ? "#"+i : auxS[1]) + ";";
            auxSch += auxS[2] + ";";
            auxSch += sSplit[1].split(";")[1] + "-";
            dataS += auxSch;
        }

        return dataS;
    }

    void CreateSchItems(String dataS){

        if(dataS == null || Objects.equals(dataS, "") || !dataS.contains("-")){
            return;
        }
        String aux = dataS.replaceAll("\n", "");
        String[] split = aux.split("-");

        List<ScheduleItem> awaScheduleItems = new ArrayList<ScheduleItem>();
        List<ScheduleItem> finiScheduleItems = new ArrayList<ScheduleItem>();

        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH) + 1;
        ScheduleItem.DateType actD = new ScheduleItem.DateType(
                cal.get(Calendar.YEAR)  +"/" + m + "/" + cal.get(Calendar.DAY_OF_MONTH) + "T" +
                        cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ":" + cal.get(Calendar.MILLISECOND));

        for (String sc : split) {
            if(sc.equals("")){ continue; }
            String[] sInfo = sc.split(";");
            ScheduleItem toS = new ScheduleItem(sInfo[0], sInfo[1], sInfo[2], 1);
            if (toS.edTime.compare(actD.data) >= 0) {
                awaScheduleItems.add(toS);
            } else {
                toS.setsTypeT(0);
                finiScheduleItems.add(toS);
            }
        }

        if(awaScheduleItems.size() > 0){
            ScheduleItem[] auxArray = new  ScheduleItem[awaScheduleItems.size()];
            auxArray =  awaScheduleItems.toArray(auxArray);
            CreateAllSchedules(1, auxArray);
        }
        if(finiScheduleItems.size() > 0){
            ScheduleItem[] auxArrayD = new ScheduleItem[finiScheduleItems.size()];
            auxArrayD =  finiScheduleItems.toArray(auxArrayD);

            CreateAllSchedules(0, auxArrayD);
        }

    }

    void CreateAllSchedules(int sType, ScheduleItem[] scs){
        ScheduleItemAdapter toMod = (sType == 1 ? awaitScAdapterListView : finishedAdapterListView);

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
        toMod.notifyDataSetChanged();

        ListView lv = (sType == 1 ? binding.deviceViewAwaitScheduleListView : binding.deviceViewDueScheduleListView );
        updateListHeight(lv);
    }

    void updateListHeight(ListView lv){
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