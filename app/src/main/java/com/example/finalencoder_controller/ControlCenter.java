package com.example.finalencoder_controller;

import android.content.Context;
import android.util.JsonReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class ControlCenter {
    private static volatile ControlCenter current = null;

    public  static ControlCenter getInstance(){
        if(current == null){
            synchronized (ControlCenter.class){
                if(current == null){
                    current = new ControlCenter();
                }
            }
        }
        return current;
    }

    public MainActivity mainActivity = null;
    public MainContentFragment mainContent = null;
    public GeneralFragment generalFrag = null;
    public SchedulerFragment schedulerFrag = null;
    public ConnectionFragment connectionFrag = null;
    public DebuggingConsoleFragment debugFrag = null;
    public DataSchedulerFragment dataSchFrag = null;

    public ShowDataPointsFragment showDataPointFrag = null;


    public String lastSentMsg = "", lastReceivedMsg = "";
    public String logMessages = "";
    public boolean connectedDevice = false, bluetoothConnected = false;
    public boolean isSendingScan = false;

    public float scanInterval = 100;

    public void setBluetoothConnected(boolean state){
        bluetoothConnected = state;

        if(!state){
            mainActivity.makeSnackB("Se desconecto el Bluetooth" );
        }

        if(!state && connectedDevice){ connectionFrag.disconnectDevice(); }
    }

    public void setDeviceConnected(boolean state, String name){
        connectedDevice = state;
        if(state){
            generalFrag.deviceConnected(name);

            connectionFrag.requestInfo("SCAN;GET;",
                ()->{ saveData(getDateCheckedDataPoints(lastReceivedMsg), "data_", true);

                    connectionFrag.requestInfo("SCHEDULE;GET;",
                        ()-> saveData(getCheckedCreatedSchedules(lastReceivedMsg), "schedules_", false),
                            ()-> connectionFrag.disconnectDevice(), 10000);

                },()-> connectionFrag.disconnectDevice(), 10000);

        }else{
            lastSentMsg = ""; lastReceivedMsg = "";
            generalFrag.deviceDisconnected();
        }
    }

    public void askForDataPoints(Runnable onSucces, Runnable onFail){
        connectionFrag.requestInfo("SCAN;GET;",
                ()->{ saveData(getDateCheckedDataPoints(lastReceivedMsg), "data_", true);
                    if(onSucces != null){ onSucces.run(); }
                }, onFail, 10000);
    }

    void checkOperation(String msg){
        if(isSendingScan){
            if(msg.charAt(0) == '!'){
                float dist = Float.parseFloat(msg.substring(1));
                generalFrag.showCapturedData(dist);
            }else if(Objects.equals(msg, "sAbort")){
                generalFrag.turnOffScanning();
            }
        }
    }

    public void setReceivedMessage(String msg){
        logMessages += ">" +  msg + "\n";
        lastReceivedMsg = msg;
        if(debugFrag != null && !debugFrag.isDestroyed()){
            debugFrag.updateConsoleLog(lastReceivedMsg, true);
        }

        checkOperation(msg);
    }

    public void setSentMessage(String msg){
        logMessages += msg + "\n";
        lastSentMsg = msg;
        if(debugFrag != null && !debugFrag.isDestroyed()){
            debugFrag.updateConsoleLog(lastSentMsg, true);
        }
    }

    public String getDateCheckedDataPoints(String dataP){

        String read = getData("data_");

        int i = read.length() - 1;
        if(i < 0){ return dataP; }

        i = read.lastIndexOf("STOP;", i);
        if(i < 0){ return dataP; }

        i = read.indexOf(";", i);
        SchedulerFragment.ScheduleItem.DateType dat = new SchedulerFragment.ScheduleItem.DateType(read.substring(i + 1, read.indexOf(";", i + 1)));

        //it saves it a txt file with the name of the device
        //to save points could be like, when ON; is sended if accepted, there should first add a START;scheduleIfExist;startDate;intervalMs;measureUnit
        //And at the end there should be a STOP;endDate and saved on every line a diferent one maybe
        //check the last date of point saved and verify that the new ones are not repeating meaning it always should be
        //a later date



        i = 0;
        int j;
        boolean findNew = false;
        while((j = dataP.indexOf("START;", i)) >= 0){

            i = dataP.indexOf(";", dataP.indexOf(";", j) + 1) + 1;

            SchedulerFragment.ScheduleItem.DateType iDat = new SchedulerFragment.ScheduleItem.DateType(dataP.substring(i, dataP.indexOf(";", i)));

            if(dat.compare(iDat.data) < 0){
                dataP = dataP.substring(j);
                findNew = true;
                break;
            }
        }
        if(!findNew){ return ""; }
        setReceivedMessage("All saved data: \n" + read + dataP + "\n");

        return dataP;
    }

    public String getCheckedCreatedSchedules(String dataS){
        //String read = getData("schedules_");

        /*String[] sSplited = getData("schedules_").split("\n");

        for(int i = 0; i < sSplited.length; i++){
            sSplited[i] = sSplited[i].split(";")[0];
        }*/

        /*
        if(!Objects.equals(read, "")){
            for(int i = 0; i < dataS.length(); ){
                if(read.contains(dataS.substring(i, dataS.indexOf(";", i)))){
                    dataS = dataS.replace(dataS.substring(i, dataS.indexOf("-", i)), "");

                /*int lIdx = 0;
                String temp = "";
                if((lIdx = dataS.indexOf("\n", i)) < dataS.length() - 1){
                    temp = dataS.substring(lIdx);
                }

                dataS = dataS.substring(0, i - (i > 0 ? 1 : 0));

                dataS = */
               /* }

                i = dataS.indexOf("-", i+1);
                if(i < 0){ break; }
            }
            if(dataS.equals("")){ return dataS; }
        }

        String[] scs = dataS.split("-");
        */

        //dataS.replaceAll("-", "-\n");
        if(dataS == null || Objects.equals(dataS, "") || !dataS.contains("-")){
            return "";
        }

        String aux = dataS.replaceAll("\n", "");
        String[] split = aux.split("-");

        List<SchedulerFragment.ScheduleItem> awaScheduleItems = new ArrayList<SchedulerFragment.ScheduleItem>();
        List<SchedulerFragment.ScheduleItem> finiScheduleItems = new ArrayList<SchedulerFragment.ScheduleItem>();

        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH) + 1;
        SchedulerFragment.ScheduleItem.DateType actD = new SchedulerFragment.ScheduleItem.DateType(
                cal.get(Calendar.YEAR)  +"/" + m + "/" + cal.get(Calendar.DAY_OF_MONTH) + "T" +
                cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ":" + cal.get(Calendar.MILLISECOND));

        for (String sc : split) {
            if(sc.equals("")){ continue; }
            String[] sInfo = sc.split(";");
            SchedulerFragment.ScheduleItem toS = new SchedulerFragment.ScheduleItem(sInfo[0], sInfo[1], sInfo[2], 1);
            if (toS.edTime.compare(actD.data) >= 0) {
                awaScheduleItems.add(toS);
            } else {
                toS.setsTypeT(0);
                finiScheduleItems.add(toS);
            }
        }

        if(awaScheduleItems.size() > 0){
            SchedulerFragment.ScheduleItem[] auxArray = new  SchedulerFragment.ScheduleItem[awaScheduleItems.size()];
            auxArray =  awaScheduleItems.toArray(auxArray);

            schedulerFrag.recreateAllSchedules(1, auxArray);
        }
        if(finiScheduleItems.size() > 0){
            SchedulerFragment.ScheduleItem[] auxArrayD = new SchedulerFragment.ScheduleItem[finiScheduleItems.size()];
            auxArrayD =  finiScheduleItems.toArray(auxArrayD);

            schedulerFrag.recreateAllSchedules(0, auxArrayD);
        }

        return dataS;
    }



    public boolean deleteData(String dInfo, String onFile){
        String actD = getData(onFile);

        if(actD == null || actD.equals("") || !actD.contains(dInfo)){
            return false;
        }

        actD = actD.replace(dInfo, "");
        saveData(actD, onFile, false);

        return true;
    }

    public void saveData(String sInfo, String onFile, boolean append){
        File t = new File(mainActivity.getFilesDir(), connectionFrag.devName);
        if(!t.exists()){
            t.mkdir();
        }

        try{
            File fos = new File(t, onFile + connectionFrag.devName + ".txt");
            FileWriter writer = new FileWriter(fos, append);
            writer.write(sInfo);

            /*
            if(append){
                writer.append(sInfo);
            }else{
            }*/

            writer.flush();
            writer.close();

        }catch (IOException e){
            mainActivity.makeSnackB("No se ha podido guardar (" + e + ")");
        }


    }

    public String getData(String fName){
        return getData(fName, connectionFrag.devName);
    }

    public String getData(String fName, String devName){
        File t = new File(mainActivity.getFilesDir(), devName);
        if(!t.exists()){
            t.mkdir();
            return "";
        }


        String read = "";

        try{
            File fos = new File(t,fName + devName + ".txt");
            if(!fos.exists()){
                return "";
            }

            FileInputStream reader = new FileInputStream(fos);
            BufferedReader bfr = new BufferedReader(new InputStreamReader(reader));
            String out = "";
            while ((out = bfr.readLine()) != null){
                read += out;
            }

            reader.close();
            bfr.close();
        }catch (IOException e){
            mainActivity.makeSnackB("No se ha podido leer (" + e + ")");
        }

        return read;

    }
}

