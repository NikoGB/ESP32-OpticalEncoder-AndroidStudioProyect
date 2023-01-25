package com.example.finalencoder_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finalencoder_controller.databinding.FragmentGeneralBinding;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.text.DecimalFormat;

public class GeneralFragment extends Fragment {

    // since we use data binding, we don't need to use findViewById to get the views from the layout file anymore (see onCreate method)
    private FragmentGeneralBinding binding;

    // Variables for the graph
    LineGraphSeries<DataPoint> mDataSeries = new LineGraphSeries<>();
    PointsGraphSeries<DataPoint> mPointDataSeries = new PointsGraphSeries<>();
    PointsGraphSeries<DataPoint> auxPointSeries = new PointsGraphSeries<>();

    // Create a new String object to hold the time
    String rsTime;

    // Class to handle the countdown timer for the experiment
    public static class tempRoutine implements Runnable {
        
        String[] vals;
        //declare a Runnable object, onFinish, to be used later
        Runnable onFinish;
        TextView tempT;

        public tempRoutine(String rsTime, Runnable onFinish, TextView tempText){
            // split the time into its components
            vals = rsTime.split(":");

            // set the onFinish event
            this.onFinish = onFinish;

            // set the temp text for the countdown
            tempT = tempText;
        }

        @Override
        public void run() {
            try{
                int minutes = Integer.parseInt(vals[0]);
                int seconds = Integer.parseInt(vals[1]);
                int milliseconds = Integer.parseInt(vals[2]);

                while (true){
                    Thread.sleep(100);

                    milliseconds-=100;
                    if(milliseconds == -100){
                        milliseconds = 900;
                        seconds -= 1;

                        if(seconds == -1){
                            seconds = 59;
                            minutes -= 1;

                            if(minutes == -1){
                                onFinish.run();
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                    DecimalFormat dF = new DecimalFormat("00");
                    String tDisp = dF.format(minutes) + ":" + dF.format(seconds) + "." + new DecimalFormat("000").format(milliseconds);
                    ControlCenter.getInstance().mainActivity.onUIThread(()-> tempT.setText(tDisp));
                }

            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Create a new thread object to hold the countdown timer
    Thread countDown;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {

        binding = FragmentGeneralBinding.inflate(inflater, container, false);

        // set the edit text to not show the keyboard when clicked
        binding.editTextTime.setShowSoftInputOnFocus(false);
        // set the setOnFocusChangeListener to show the time picker when the edit text is clicked
        binding.editTextTime.setOnFocusChangeListener((view, b) -> {
                    if(b){
                        // hide the keyboard
                        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                        // create a new time picker fragment
                        ExtendedTimePickerFragment tPick = new ExtendedTimePickerFragment(2, 0);
                        // set the onAccept event to set the time in the edit text
                        tPick.onAccept = ()->  {
                            // set the time in the edit text
                            rsTime = tPick.minute + ":" + tPick.second + ":" + tPick.millisecond;
                            // format the time to be displayed properly
                            DecimalFormat df = new DecimalFormat("00");
                            // create a new string to hold the formatted time
                            String toPut = df.format(tPick.minute)  + ":" + df.format(tPick.second) + "." + new DecimalFormat("000").format(tPick.millisecond);
                            // set the edit text to the formatted time
                            binding.editTextTime.setText(toPut);
                        };
                        // show the time picker
                        tPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select amount time");
                    }
                }
        );

        // define la accion del editText que almacena el tiempo
        binding.editTextTime.setOnClickListener((view) -> {
                        // hide the keyboard
                        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        // create a new time picker fragment
                        ExtendedTimePickerFragment tPick = new ExtendedTimePickerFragment(2, 0);
                        tPick.onAccept = ()->  {
                            // guarda el valor seleccionado en rsTime con formato mm:ss:ms
                            rsTime = tPick.minute + ":" + tPick.second + ":" + tPick.millisecond;
                            // format the time to be displayed properly
                            DecimalFormat df = new DecimalFormat("00");
                            // create a new string to hold the formatted time
                            String toPut = df.format(tPick.minute)  + ":" + df.format(tPick.second) + "." + new DecimalFormat("000").format(tPick.millisecond);
                            // set the edit text to the formatted time
                            binding.editTextTime.setText(toPut);
                        };
                        // show the time picker
                        tPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select amount time");
                }
        );

        // set the onClickListener for the start button
        binding.temporizadorButtonStart.setOnClickListener( view -> {
            // si no esta conectado mandar un mensaje solicitando la conexion
            if(!ControlCenter.getInstance().connectedDevice){
                Toast.makeText(getContext(),"Debes estar conectado para usar esta funcion",Toast.LENGTH_LONG).show();
                return;
            }
            // si no se ha ingresado una cantidad de tiempo solicitar el ingreso de una
            if(rsTime == null || rsTime.isEmpty() || rsTime.compareTo("0:0:0")==0){
                Toast.makeText(getContext(),"Debe ingresar la duracion del temporizador (mayor a 00:00.000)",Toast.LENGTH_LONG).show();
                return;
            }
                // Envia el commando al ESP32 formato TIMER;mm:ss:ms;
                ControlCenter.getInstance().connectionFrag.sendCommand("TIMER;" + rsTime+";", () -> {
                    // set the temp state to true
                    setTempState(true);
                    // create a new thread to hold the countdown timer
                    countDown = new Thread(
                            // create a new tempRoutine object to hold the countdown timer
                            new tempRoutine(rsTime, () -> {
                                // set the temp state to false
                                setTempState(false);
                                // interrupt the thread holding the countdown timer to stop it from running in the background after the timer is done
                                ControlCenter.getInstance().mainActivity.onUIThread(() -> binding.scanStateSwitch.setChecked(false));
                            }, binding.timeLeftScanTextView)
                    );
                    // start the countdown timer
                    countDown.start();
                    // set the isSendingScan to true
                    ControlCenter.getInstance().isSendingScan = true;
                    // disable the switch
                    binding.scanStateSwitch.setChecked(true);
                    // set the switch to be disabled
                    binding.scanStateSwitch.setEnabled(false);
                    // !123.3221*

                }, 10000);

        });
        // set the onClickListener for the stop button
        binding.temporizadorButtonStop.setOnClickListener( view -> {
            // send the command to stop the timer and turn off the scan
            ControlCenter.getInstance().connectionFrag.sendCommand("OFF;", () -> {
                // set the temp state to false
                setTempState(false);
                // interrupt the thread holding the countdown timer to stop it from running in the background after the timer is done
                countDown.interrupt();
                // set the isSendingScan to false
                ControlCenter.getInstance().isSendingScan = false;
                // set the switch to be enabled
                binding.scanStateSwitch.setChecked(false);
                // set the switch to be enabled
                binding.scanStateSwitch.setEnabled(true);
            }, 10000);
        } );

        // set the onCheckedChangedListener for the switch
        binding.scanStateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // check if the switch is being checked
                if(b == ControlCenter.getInstance().isSendingScan){ return; }

                if(b){
                    // send the command to turn on the scan
                    ControlCenter.getInstance().connectionFrag.sendCommand("ON;", () -> {
                        // set the isSendingScan to true
                        ControlCenter.getInstance().isSendingScan = true;
                        // set the switch to be enabled
                        binding.scanStateSwitch.setChecked(true);
                        // set the switch to be enabled
                        binding.scanStateSwitch.setEnabled(true);

                    }, ()->{ 
                        // set the switch to be enabled
                        binding.scanStateSwitch.setEnabled(true); 
                    }, ()->{  
                        // set the switch to be enabled
                        binding.scanStateSwitch.setEnabled(true);
                    },10000);
                    // disable the switch
                    binding.scanStateSwitch.setChecked(false);
                }else{
                    // if the switch is sending a scan, send the command to turn off the scan
                    if(ControlCenter.getInstance().isSendingScan){
                        // send the command to turn off the scan
                        ControlCenter.getInstance().connectionFrag.sendCommand("OFF;", () -> {
                            // set the isSendingScan to false
                            ControlCenter.getInstance().isSendingScan = false;
                            // set the switch to be enabled
                            binding.scanStateSwitch.setChecked(false);
                            // set the switch to be enabled
                            binding.scanStateSwitch.setEnabled(true);
                            // ask for the data points
                            // if the data points are successfully retrieved, make a snack bar saying that the data points were successfully retrieved
                            // if the data points are not successfully retrieved, make a snack bar saying that the data points were not successfully retrieved
                            ControlCenter.getInstance().askForDataPoints(
                                    ()->ControlCenter.getInstance().mainActivity.makeSnackB("Muestreo almacenado exitosamente"),
                                    ()->ControlCenter.getInstance().mainActivity.makeSnackB("No se pudo guardar el muestreo"));
                        }, ()->{ 
                            // set the switch to be enabled
                            binding.scanStateSwitch.setEnabled(true);
                        }, ()->{ 
                            // set the switch to be enabled
                            binding.scanStateSwitch.setEnabled(true);
                        },100000);
                        binding.scanStateSwitch.setChecked(true);
                    }
                }
                // disable the switch
                binding.scanStateSwitch.setEnabled(false);
            }
        });
        // set the onClickListener for the connection image view
        binding.connectionImageView.setOnClickListener( view -> { ControlCenter.getInstance().mainContent.navigateViewPag(2); } );


        // set the interval between two values on x-axis
        float inter = ControlCenter.getInstance().scanInterval * 0.001f;

        // define the formatter
        LabelFormatter labfor = new LabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {

                // if it is x axis
                if(isValueX){

                    // format the value
                    DecimalFormat df = new DecimalFormat("#.##");
                    String str = df.format(value * inter) + "\n";

                    return str;
                }else{
                    return "" + value;
                }
            }

            @Override
            public void setViewport(Viewport viewport) {

            }
        };
        // set the formatter to the graph view
        binding.spikesGraphView.getGridLabelRenderer().setLabelFormatter(labfor);
        // set the number of labels on the x axis
        mDataSeries.setDrawDataPoints(true);
        mDataSeries.setDataPointsRadius(6);
        // set the number of labels on the x axis
        mPointDataSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                // create a new paint object with the color of the text

                // set the color to the color of the text
                paint.setColor(getResources().getColor(android.R.color.holo_red_dark));
                // set the stroke width
                paint.setStrokeWidth(2);
                // set the text size
                paint.setTextSize(36);
                // create a DecimalFormat object to format the text
                DecimalFormat df = new DecimalFormat("#.##");
                // get the value of the y coordinate of the data point
                String str = df.format(dataPoint.getY());
                // draw the text
                canvas.drawText(str, x - (18 * (str.length() - 1) - 9), y - 30, paint);

            }
        });
        // set custom shape for the aux point series
        auxPointSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                return;
            }
        });
        // set the data graph view to the spikes graph view
        GraphView dataGraph = binding.spikesGraphView;
        
        // set the data series to the data graph view
        dataGraph.addSeries(mDataSeries);
        dataGraph.addSeries(mPointDataSeries);
        dataGraph.addSeries(auxPointSeries);
        // set the viewport of the data graph view
        dataGraph.getViewport().setXAxisBoundsManual(true);
        dataGraph.getViewport().setMinX(0);
        dataGraph.getViewport().setMaxX(4);
        // OnXAxisBoundsChangedListener for the data graph view
        dataGraph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Reason reason) {
                // TODO: check if the reason is zoom or scroll and set the number of labels to 4 if the difference between the max and min x is greater than 4 is correct
                // if the reason is zoom or scroll and the difference between the max and min x is greater than 4 set the number of labels to 4
                if(maxX - minX > 4){
                    dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
                }else{
                    // if the difference between the max and min x is less than 4 set the number of labels to the difference between the max and min x
                    dataGraph.getGridLabelRenderer().setNumHorizontalLabels((int)Math.ceil(maxX - minX) + 1);
                }
            }
        });
        // set the number of labels on the x axis to 4 and the width of the labels on the y axis to 100
        dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        dataGraph.getGridLabelRenderer().setLabelVerticalWidth(100);
        // set the viewport to be scalable and scrollable and set the nested scrolling to be enabled for the data graph view
        dataGraph.getViewport().setScalable(true);
        dataGraph.getViewport().setScrollable(true);
        dataGraph.setNestedScrollingEnabled(true);
        // set the title of the x axis and the y axis of the data graph view
        dataGraph.getGridLabelRenderer().setHorizontalAxisTitle(" \nIntervalo muestreo(seg)");
        binding.spikesGraphView.getGridLabelRenderer().setVerticalAxisTitle("Distancia(cm)");


        return binding.getRoot();
    }

    double amountDataX = 1;
    // on view created method
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // set the visibility of the views of the temporizador
    void setTempState(boolean started){
        // check if the temporizador is started or not and set the visibility of the views of the temporizador
        ControlCenter.getInstance().mainActivity.onUIThread(()->{
            binding.temporizadorRow1.setVisibility((started ? View.GONE : View.VISIBLE));
            binding.temporizadorRow2.setVisibility((started ? View.GONE : View.VISIBLE));
            binding.temporizadorRow3.setVisibility((started ? View.VISIBLE : View.GONE));
            binding.temporizadorRow4.setVisibility((started ? View.VISIBLE : View.GONE));
        });
    }


    // variable to store the last value of the x-axis of the graph
    double dataGraphLastX = 0d;
    public void showCapturedData(float dist){
        //ControlCenter.getInstance().mainActivity.makeSnackB("New dist: " + dist + " ,ON: " + dataGraphLastX);
        // dataGraphLastX is the last value of the x-axis of the graph
        dataGraphLastX += 1d; // add 1d to dataGraphLastX

        // add the new data to the graph
        mPointDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
        mDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
        binding.spikesGraphView.getViewport().scrollToEnd();
        // add a point to the auxiliar graph
        auxPointSeries.appendData(new DataPoint(dataGraphLastX + 2, mPointDataSeries.getHighestValueY()), true, 2);
    }

    public void deviceConnected(String name){
        // Set the color of the connection status to green.
        binding.connectionImageView.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.green));
        // Set the text of the connection status to "Conectado".
        binding.connectionTextView.setText("Conectado");
        // Set the name of the connected device.
        binding.connectionDeviceNameTextView.setText(name);
        // Enable the scan button.
        binding.scanStateSwitch.setEnabled(true);

        //dataGraphLastX = 0d;
        // Clear the graph.

    }

    public void deviceDisconnected(){
        // Set the image to a red color and the text to the disconnected state.
        binding.connectionImageView.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.red));
        binding.connectionTextView.setText("Desconectado");
        binding.connectionDeviceNameTextView.setText("ninguno");
        // Stop scanning for devices.
        turnOffScanning();
    }

    public void turnOffScanning(){

        // if the connected device is not connected
        if(!ControlCenter.getInstance().connectedDevice){
            // set the scan state to false
            ControlCenter.getInstance().isSendingScan = false;
            // set the scan state switch to false
            binding.scanStateSwitch.setChecked(false);

            // disable the scan state switch
            binding.scanStateSwitch.setEnabled(false);
        }else{
            // set the scan state switch to false
            binding.scanStateSwitch.setChecked(false);
            // set the scan state to false
            ControlCenter.getInstance().isSendingScan = false;
        }
    }

    public void turnOnScanning(){
        // set the scan state switch to false
        ControlCenter.getInstance().isSendingScan = true;
        binding.scanStateSwitch.setChecked(true);
        binding.scanStateSwitch.setEnabled(false);
        //binding.scanStateSwitch.setChecked(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
