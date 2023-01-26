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

    // singleton para tener acceso global a los datos de la app
    private static volatile ControlCenter current = null;

    // funcion para obtener la instancia del singleton
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

    // singleton classes, para tener acceso global a los datos de la app
    public MainActivity mainActivity = null;
    public MainContentFragment mainContent = null;
    public GeneralFragment generalFrag = null;
    public SchedulerFragment schedulerFrag = null;
    public ConnectionFragment connectionFrag = null;
    public DebuggingConsoleFragment debugFrag = null;
    public DataSchedulerFragment dataSchFrag = null;

    public ShowDataPointsFragment showDataPointFrag = null;

    // variables para guardar los datos de la app
    public String lastSentMsg = "", lastReceivedMsg = "";
    public String logMessages = "";
    public boolean connectedDevice = false, bluetoothConnected = false;
    public boolean isSendingScan = false;

    public float scanInterval = 100;

    // funcion para desconectar el dispositivo
    public void setBluetoothConnected(boolean state){
        bluetoothConnected = state;
        if(!state){
            mainActivity.makeSnackB("Se desconecto el Bluetooth" );
        }
        
        if(!state && connectedDevice){ connectionFrag.disconnectDevice(); }
    }
    // funcion que se llama cuando se conecta el dispositivo y se piden los datos (data.txt y schedules.txt)
    // se guardan los datos en el archivo correspondiente (data_ y schedules_)
    // @param state: estado de la conexion
    // @param name: nombre del dispositivo conectado
    public void setDeviceConnected(boolean state, String name){
        connectedDevice = state;
        if(state){
            generalFrag.deviceConnected(name);

            connectionFrag.requestInfo("SCAN;GET;",
                    ()->{ saveData(getDateCheckedDataPoints(lastReceivedMsg), "data_", true);

                        connectionFrag.requestInfo("SCHEDULE;GET;",
                                ()-> saveData(getCheckedCreatedSchedules(lastReceivedMsg), "schedules_", false),
                                ()-> connectionFrag.disconnectDevice(), 100000);

                    },()-> connectionFrag.disconnectDevice(), 100000);

        }else{
            lastSentMsg = ""; lastReceivedMsg = "";
            generalFrag.deviceDisconnected();
        }
    }

    // funcion para obtener los datos del archivo data.txt t los guarda en el archivo data_
    // @param onSucces: funcion que se ejecuta cuando se termina de guardar los datos
    // @param onFail: funcion que se ejecuta cuando no se puede guardar los datos
    public void askForDataPoints(Runnable onSucces, Runnable onFail){
        connectionFrag.requestInfo("SCAN;GET;",
        ()->{ saveData(getDateCheckedDataPoints(lastReceivedMsg), "data_", true);
        if(onSucces != null){ onSucces.run(); }
        },
        onFail, 100000);
    }

    // funcion que verifica la accion que esta realizando el dispositivo
    // @param msg: mensaje recibido del dispositivo
    void checkOperation(String msg){
        // si el mensaje es scheduleStart, se inicia el escaneo
        if(msg.equals("scheduleStart")){
            mainActivity.onUIThread(() -> schedulerFrag.scheduleStateChanged(2));
            isSendingScan = true;
            generalFrag.turnOnScanning();

        // si el mensaje es scheduleStop, se detiene el escaneo
        }else if(msg.equals("scheduleStop")){
            mainActivity.onUIThread(() -> {
                schedulerFrag.scheduleStateChanged(0);
                generalFrag.turnOffScanning();
            } );


        // si se esta escaneando y el mensaje es !distancia, se muestra la distancia en el fragmento general
        } else if(isSendingScan){
            if(msg.charAt(0) == '!'){
                float dist = Float.parseFloat(msg.substring(1));
                generalFrag.showCapturedData(dist);
            }else if(Objects.equals(msg, "sAbort")){
                generalFrag.turnOffScanning();
            }
        }
    }

    // funcion que se llama cuando se recibe un mensaje del dispositivo y lo muestra en la consola de debug
    // @param msg: mensaje recibido del dispositivo
    public void setReceivedMessage(String msg){
        logMessages += ">" +  msg + "\n";
        lastReceivedMsg = msg;
        if(debugFrag != null && !debugFrag.isDestroyed()){
            debugFrag.updateConsoleLog(lastReceivedMsg, true);
        }

        checkOperation(msg);
    }

    // funcion que se llama cuando se envia un mensaje al dispositivo y lo muestra en la consola de debug
    // @param msg: mensaje enviado al dispositivo
    public void setSentMessage(String msg){
        logMessages += msg + "\n";
        lastSentMsg = msg;
        if(debugFrag != null && !debugFrag.isDestroyed()){
            debugFrag.updateConsoleLog(lastSentMsg, true);
        }
    }

    // funcion que guarda como txt los datos recibidos del dispositivo y los guarda en el archivo con el nombre del dispositivo conectado
    // para guardar los datos que se reciben del dispositivo estos deben seguir el siguiente formato "START;scheduleNameIfExist;startDate;intervalMs;measureUnit"
    // @param data: datos recibidos del dispositivo
    // @return: datos recibidos del dispositivo con el formato "START;scheduleNameIfExist;startDate;intervalMs;measureUnit"
    public String getDateCheckedDataPoints(String dataP){
        String read = getData("data_");
        int i = read.length() - 1;
        if(i < 0){ return dataP; }

        i = read.lastIndexOf("STOP;", i);
        if(i < 0){ return dataP; }

        i = read.indexOf(";", i);
        SchedulerFragment.ScheduleItem.DateType dat = new SchedulerFragment.ScheduleItem.DateType(read.substring(i + 1, read.indexOf(";", i + 1)));

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

    // funcion que obtiene los schedules que se han creado en el dispositivo y los guarda en las respectivas listas
    // @param dataS: datos recibidos del dispositivo
    // @return: los schedules que se han creado en el dispositivo
    public String getCheckedCreatedSchedules(String dataS){
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

    /**
     * @return un booleano que indica si se elimino o no los datos
     * @param dInfo datos a eliminar
     * @param onFile en el archivo
     */
    public boolean deleteData(String dInfo, String onFile){
        String actD = getData(onFile);
        if(actD == null || actD.equals("") || !actD.contains(dInfo)){
            return false;
        }
        actD = actD.replace(dInfo, "");
        saveData(actD, onFile, false);
        return true;
    }

    /**
     * funcion que guarda los datos en el almacenamiento externo (SD del telefono)
     * @param data: datos a guardar
     * @param nFile: nombre del archivo
     * @param devName: nombre del dispositivo
     */
    public void saveDataOnStorage(String data, String nFile, String devName){
        File dir = new File(mainActivity.getExternalStorage(), "Experimentos_Distancia");
        if (!dir.exists()) {
            dir.mkdir();
        }

        File t = new File(dir,"DataDispositvo_" + devName);
        if(!t.exists()){
            t.mkdir();
        }

        try{
            File fos = new File(t, nFile + ".txt");
            FileWriter writer = new FileWriter(fos, false);
            writer.write(data);
            writer.flush();
            writer.close();
            mainActivity.makeSnackB("Exportado correctamente en: "+ t.getPath());
        }catch (IOException e){
            mainActivity.makeSnackB("Intento "+ t.getPath());
            mainActivity.makeSnackB("No se ha podido guardar (" + e + ")");
        }
    }

    /**
     * funcion que guarda los datos en el almacenamiento interno de la app
     * @param sInfo: datos a guardar
     * @param onFile: nombre del archivo
     * @param append: bool que determina si agregar los datos o sobreescribir
     */
    public void saveData(String sInfo, String onFile, boolean append){
        File t = new File(mainActivity.getFilesDir(), connectionFrag.devName);
        if(!t.exists()){
            t.mkdir();
        }
        try{
            File fos = new File(t, onFile + connectionFrag.devName + ".txt");
            FileWriter writer = new FileWriter(fos, append);
            writer.write(sInfo);
            writer.flush();
            writer.close();

        }catch (IOException e){
            mainActivity.makeSnackB("No se ha podido guardar (" + e + ")");
        }


    }

    /**
     * funcion que recupera los datos del dispositivo conectado
     * @param fName: nombre del archivo
     */
    public String getData(String fName){
        return getData(fName, connectionFrag.devName);
    }

    /**
     * funcion que recupera los datos del dispositivo conectado
     * @param fName: nombre del archivo
     * @param devName: nombre del dispositivo
     */
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

