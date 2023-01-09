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

    // singleton used to have a global access to the data of the app
    private static volatile ControlCenter current = null;

    // Function to get the instance of the singleton
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

    // singleton classes, it is used to have a global access to the data of the app
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
        // If the bluetooth is disconnected, disconnect the device
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
        // send request to device
        connectionFrag.requestInfo("SCAN;GET;",
        // if the request is successful, save the data
        ()->{ saveData(getDateCheckedDataPoints(lastReceivedMsg), "data_", true);
        // if the user specified a callback, call it
        if(onSucces != null){ onSucces.run(); }
        },
        // if the request failed, call the user specified callback
        onFail, 10000);
    }

    void checkOperation(String msg){
        if(isSendingScan){
            // ! indicates the start of a distance value
            if(msg.charAt(0) == '!'){
                // get the distance value
                float dist = Float.parseFloat(msg.substring(1));
                // show the captured data in the GeneralFragment
                generalFrag.showCapturedData(dist);
            }else if(Objects.equals(msg, "sAbort")){
                // stop scanning if sAbort is received
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
        // 1. Saves the message to the logMessages variable
        logMessages += msg + "\n";
        // 2. Saves the message to the lastSentMsg variable
        lastSentMsg = msg;
        // 3. Check to see if the debug fragment is still available
        if(debugFrag != null && !debugFrag.isDestroyed()){
            // If it is, send the message to the debug fragment
            debugFrag.updateConsoleLog(lastSentMsg, true);
        }
    }

    public String getDateCheckedDataPoints(String dataP){
        // Get data from the file data_ as a string
        String read = getData("data_");

        // Set the value of i to the length of the string read
        int i = read.length() - 1;
        // If i is less than 0, return dataP
        if(i < 0){ return dataP; }

        // Set the value of i to the last index of STOP; in the string read
        i = read.lastIndexOf("STOP;", i);
        // If i is less than 0, return dataP
        if(i < 0){ return dataP; }

        // Set the value of i to the index of ; in the string read
        i = read.indexOf(";", i);
        // Create a new DateType object and set it to dat
        SchedulerFragment.ScheduleItem.DateType dat = new SchedulerFragment.ScheduleItem.DateType(read.substring(i + 1, read.indexOf(";", i + 1)));

        //it saves it a txt file with the name of the device
        //to save points could be like, when ON; is sended if accepted, there should first add a START;scheduleNameIfExist;startDate;intervalMs;measureUnit
        //And at the end there should be a STOP;endDate and saved on every line a diferent one maybe
        //check the last date of point saved and verify that the new ones are not repeating meaning it always should be
        //a later date

        //find the first data that is after the date dat
        i = 0;
        int j;
        boolean findNew = false;
        // While the index of START; in the string dataP is greater than or equal to 0
        while((j = dataP.indexOf("START;", i)) >= 0){
            // Set the value of i to the index of ; in the string dataP
            i = dataP.indexOf(";", dataP.indexOf(";", j) + 1) + 1;
            // Create a new DateType object and set it to iDat
            SchedulerFragment.ScheduleItem.DateType iDat = new SchedulerFragment.ScheduleItem.DateType(dataP.substring(i, dataP.indexOf(";", i)));
            // If the date of the data point is less than the date of the data point in the file
            if(dat.compare(iDat.data) < 0){
                // Set the value of dataP to the substring of dataP starting at the index of START; in the string dataP
                dataP = dataP.substring(j);
                // Set the value of findNew to true
                findNew = true;
                // Break out of the loop
                break;
            }
        }
        // If findNew is false, return an empty string
        if(!findNew){ return ""; }
        // Set the value of i to the index of STOP; in the string dataP
        setReceivedMessage("All saved data: \n" + read + dataP + "\n");
        
        return dataP;
    }

    public String getCheckedCreatedSchedules(String dataS){
        // If the data is null, return an empty string
        if(dataS == null || Objects.equals(dataS, "") || !dataS.contains("-")){
            return "";
        }
        // Remove all new line characters from the data
        String aux = dataS.replaceAll("\n", "");
        // Split the data into an array of strings
        String[] split = aux.split("-");

        // Create a new ArrayList of ScheduleItems awaiting to be executed
        List<SchedulerFragment.ScheduleItem> awaScheduleItems = new ArrayList<SchedulerFragment.ScheduleItem>();
        // Create a new ArrayList of ScheduleItems that are done executing
        List<SchedulerFragment.ScheduleItem> finiScheduleItems = new ArrayList<SchedulerFragment.ScheduleItem>();

        // Create a new Calendar object
        Calendar cal = Calendar.getInstance();
        // Get the current year
        int m = cal.get(Calendar.MONTH) + 1;
        // Create a new DateType object and set it to actD (actual date) with the current date
        SchedulerFragment.ScheduleItem.DateType actD = new SchedulerFragment.ScheduleItem.DateType(
                cal.get(Calendar.YEAR)  +"/" + m + "/" + cal.get(Calendar.DAY_OF_MONTH) + "T" +
                cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ":" + cal.get(Calendar.MILLISECOND));

        // For each string in the split array
        for (String sc : split) {
            // If the string is empty, continue
            if(sc.equals("")){ continue; }
            // Split the string into an array of strings
            String[] sInfo = sc.split(";");
            // Create a new ScheduleItem object and set it to toS (to Schedule)
            SchedulerFragment.ScheduleItem toS = new SchedulerFragment.ScheduleItem(sInfo[0], sInfo[1], sInfo[2], 1);
            // If the ScheduleItem's end time is greater than or equal to the actual date
            if (toS.edTime.compare(actD.data) >= 0) {
                // Set the ScheduleItem's type to 1
                awaScheduleItems.add(toS);
            } else {
                // Set the ScheduleItem's type to 0
                toS.setsTypeT(0);
                // Add the ScheduleItem to the finiScheduleItems list
                finiScheduleItems.add(toS);
            }
        }

        // If the awaScheduleItems list is not empty (there is scheduleItmes awaiting to be executed)
        if(awaScheduleItems.size() > 0){
            // Create a new ScheduleItem array and set it to auxArray
            SchedulerFragment.ScheduleItem[] auxArray = new  SchedulerFragment.ScheduleItem[awaScheduleItems.size()];
            // Set the auxArray to the awaScheduleItems list
            auxArray =  awaScheduleItems.toArray(auxArray);
            // Call the recreateAllSchedules method of the schedulerFrag object with the auxArray
            schedulerFrag.recreateAllSchedules(1, auxArray);
        }
        // If the finiScheduleItems list is not empty (there is scheduleItmes that are done executing)
        if(finiScheduleItems.size() > 0){
            // Create a new ScheduleItem array and set it to auxArrayD
            SchedulerFragment.ScheduleItem[] auxArrayD = new SchedulerFragment.ScheduleItem[finiScheduleItems.size()];
            // Set the auxArrayD to the finiScheduleItems list
            auxArrayD =  finiScheduleItems.toArray(auxArrayD);
            // Call the recreateAllSchedules method of the schedulerFrag object with the auxArrayD 
            schedulerFrag.recreateAllSchedules(0, auxArrayD);
        }
        // Return the dataS string (Schedule data) 
        return dataS;
    }



    public boolean deleteData(String dInfo, String onFile){
        String actD = getData(onFile);
        // If the actual data is null, return false 
        if(actD == null || actD.equals("") || !actD.contains(dInfo)){
            return false;
        }
        // Remove the data from the actual data 
        actD = actD.replace(dInfo, "");
        // Save the actual data
        saveData(actD, onFile, false);

        return true;
    }

    public void saveData(String sInfo, String onFile, boolean append){
        // Create a new File object and set it to t (temp) with the mainActivity's files directory and the device name
        File t = new File(mainActivity.getFilesDir(), connectionFrag.devName);
        // If the t file does not exist
        if(!t.exists()){
            // Create the t file
            t.mkdir();
        }
        // Create a new File object and set it to fos (file on save) with the t file and the onFile string
        try{
            File fos = new File(t, onFile + connectionFrag.devName + ".txt");
            // Create a new FileWriter object and set it to writer with the fos file and the append boolean
            FileWriter writer = new FileWriter(fos, append);
            // Write the sInfo string to the writer
            writer.write(sInfo);
            // Flush the writer
            writer.flush();
            // Close the writer
            writer.close();

        }catch (IOException e){
            // If an error occurs, make a snackB with the error
            mainActivity.makeSnackB("No se ha podido guardar (" + e + ")");
        }


    }

    public String getData(String fName){
        // Return the getData method with the fName string and the device name
        return getData(fName, connectionFrag.devName);
    }

    public String getData(String fName, String devName){
        // Create a new File object and set it to t (temp) with the mainActivity's files directory and the device name
        File t = new File(mainActivity.getFilesDir(), devName);
        // If the t file does not exist
        if(!t.exists()){
            // Create the t file
            t.mkdir();
            // Return an empty string
            return "";
        }

        // Create a new String object and set it to read
        String read = "";
        // Create a new File object and set it to fos (file on save) with the t file and the fName string
        try{
            File fos = new File(t,fName + devName + ".txt");
            // If the fos file does not exist
            if(!fos.exists()){
                // return an empty string
                return "";
            }
            // Create a new FileInputStream object and set it to reader with the fos file
            FileInputStream reader = new FileInputStream(fos);
            // Create a new BufferedReader object and set it to bfr with the reader
            BufferedReader bfr = new BufferedReader(new InputStreamReader(reader));
            // Create a new String object and set it to out
            String out = "";
            // While the out string is not null
            while ((out = bfr.readLine()) != null){
                // Add the out string to the read string
                read += out;
            }
            // Close the reader and the bfr
            reader.close();
            bfr.close();
        }catch (IOException e){
            // If an error occurs, make a snackB with the error
            mainActivity.makeSnackB("No se ha podido leer (" + e + ")");
        }
        // Return the read string
        return read;

    }
}

