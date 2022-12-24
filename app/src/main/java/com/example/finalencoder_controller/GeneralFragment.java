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

    private FragmentGeneralBinding binding;

    LineGraphSeries<DataPoint> mDataSeries = new LineGraphSeries<>();
    PointsGraphSeries<DataPoint> mPointDataSeries = new PointsGraphSeries<>();

    //LineGraphSeries<DataPoint> mFilteredDataSeries = new LineGraphSeries<>();
    //PointsGraphSeries<DataPoint> mPointFilteredDataSeries = new PointsGraphSeries<>();

    PointsGraphSeries<DataPoint> auxPointSeries = new PointsGraphSeries<>();
    String rsTime;

    public static class tempRoutine implements Runnable {
        String[] vals;
        Runnable onFinish;
        TextView tempT;

        public tempRoutine(String rsTime, Runnable onFinish, TextView tempText){
            vals = rsTime.split(":");
            this.onFinish = onFinish;
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

    Thread countDown;
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {

        binding = FragmentGeneralBinding.inflate(inflater, container, false);

        binding.editTextTime.setShowSoftInputOnFocus(false);
        binding.editTextTime.setOnFocusChangeListener((view, b) -> {
                    if(b){
                        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                        ExtendedTimePickerFragment tPick = new ExtendedTimePickerFragment(2, 0);
                        tPick.onAccept = ()->  {
                            rsTime = tPick.minute + ":" + tPick.second + ":" + tPick.millisecond;
                            DecimalFormat df = new DecimalFormat("00");
                            String toPut = df.format(tPick.minute)  + ":" + df.format(tPick.second) + "." + new DecimalFormat("000").format(tPick.millisecond);
                            binding.editTextTime.setText(toPut);
                        };

                        tPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select amount time");
                    }
                }
        );

        binding.editTextTime.setOnClickListener((view) -> {
                        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                        ExtendedTimePickerFragment tPick = new ExtendedTimePickerFragment(2, 0);
                        tPick.onAccept = ()->  {
                            rsTime = tPick.minute + ":" + tPick.second + ":" + tPick.millisecond;
                            DecimalFormat df = new DecimalFormat("00");
                            String toPut = df.format(tPick.minute)  + ":" + df.format(tPick.second) + "." + new DecimalFormat("000").format(tPick.millisecond);
                            binding.editTextTime.setText(toPut);
                        };

                        tPick.show(ControlCenter.getInstance().mainActivity.getSupportFragmentManager(), "select amount time");

                }
        );


        binding.temporizadorButtonStart.setOnClickListener( view -> {
            ControlCenter.getInstance().connectionFrag.sendCommand("ON;" + rsTime, () -> {
                setTempState(true);
                countDown = new Thread(
                        new tempRoutine(rsTime, ()-> {
                            setTempState(false);
                            ControlCenter.getInstance().mainActivity.onUIThread(()-> binding.scanStateSwitch.setChecked(false));
                        }, binding.timeLeftScanTextView)
                );
                countDown.start();
                ControlCenter.getInstance().isSendingScan = true;
                binding.scanStateSwitch.setChecked(true);
                binding.scanStateSwitch.setEnabled(false);
            }, 10000);

        } );

        binding.temporizadorButtonStop.setOnClickListener( view -> {
            ControlCenter.getInstance().connectionFrag.sendCommand("OFF;", () -> {
                setTempState(false);
                countDown.interrupt();
                ControlCenter.getInstance().isSendingScan = false;
                binding.scanStateSwitch.setChecked(false);
                binding.scanStateSwitch.setEnabled(true);
            }, 10000);

        } );

        binding.scanStateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b == ControlCenter.getInstance().isSendingScan){ return; }

                if(b){
                    ControlCenter.getInstance().connectionFrag.sendCommand("ON;", () -> {

                        ControlCenter.getInstance().isSendingScan = true;
                        binding.scanStateSwitch.setChecked(true);
                        binding.scanStateSwitch.setEnabled(true);

                    }, ()->{  binding.scanStateSwitch.setEnabled(true); }, ()->{  binding.scanStateSwitch.setEnabled(true); },10000);
                    binding.scanStateSwitch.setChecked(false);

                }else{
                    if(ControlCenter.getInstance().isSendingScan){
                        ControlCenter.getInstance().connectionFrag.sendCommand("OFF;", () -> {
                            ControlCenter.getInstance().isSendingScan = false;
                            binding.scanStateSwitch.setChecked(false);
                            binding.scanStateSwitch.setEnabled(true);

                            ControlCenter.getInstance().askForDataPoints(
                                    ()->ControlCenter.getInstance().mainActivity.makeSnackB("Muestreo almacenado exitosamente"),
                                    ()->ControlCenter.getInstance().mainActivity.makeSnackB("No se pudo guardar el muestreo"));

                        }, ()->{  binding.scanStateSwitch.setEnabled(true); }, ()->{  binding.scanStateSwitch.setEnabled(true); },10000);
                        binding.scanStateSwitch.setChecked(true);
                    }
                }

                binding.scanStateSwitch.setEnabled(false);
            }
        });
    
        binding.connectionImageView.setOnClickListener( view -> { ControlCenter.getInstance().mainContent.navigateViewPag(2); } );



        float inter = ControlCenter.getInstance().scanInterval * 0.001f;
        LabelFormatter labfor = new LabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(isValueX){

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

        binding.spikesGraphView.getGridLabelRenderer().setLabelFormatter(labfor);
        //binding.distanceGraphView.getGridLabelRenderer().setLabelFormatter(labfor);

        /*
        mFilteredDataSeries.setDrawDataPoints(true);
        mFilteredDataSeries.setDataPointsRadius(6);
        mPointFilteredDataSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                //if(dataPoint.getX() == mPointFilteredDataSeries.getHighestValueX()){ return; }

                paint.setColor(getResources().getColor(android.R.color.holo_red_dark));
                paint.setStrokeWidth(2);
                paint.setTextSize(36);
                DecimalFormat df = new DecimalFormat("#.##");
                String str = df.format(dataPoint.getY());

                canvas.drawText(str, x - (18 * (str.length() - 1) - 9), y - 30, paint);

            }
        });
        */

        mDataSeries.setDrawDataPoints(true);
        mDataSeries.setDataPointsRadius(6);

        mPointDataSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                //if(dataPoint.getX() == mPointDataSeries.getHighestValueX()){ return; }

                paint.setColor(getResources().getColor(android.R.color.holo_red_dark));
                paint.setStrokeWidth(2);
                paint.setTextSize(36);
                DecimalFormat df = new DecimalFormat("#.##");
                String str = df.format(dataPoint.getY());

                canvas.drawText(str, x - (18 * (str.length() - 1) - 9), y - 30, paint);

            }
        });



        auxPointSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                return;
            }
        });

        GraphView dataGraph = binding.spikesGraphView;

        dataGraph.addSeries(mDataSeries);
        dataGraph.addSeries(mPointDataSeries);
        dataGraph.addSeries(auxPointSeries);

        dataGraph.getViewport().setXAxisBoundsManual(true);
        dataGraph.getViewport().setMinX(0);
        dataGraph.getViewport().setMaxX(4);

        dataGraph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Reason reason) {
                if(maxX - minX > 4){
                    dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
                }else{
                    dataGraph.getGridLabelRenderer().setNumHorizontalLabels((int)Math.ceil(maxX - minX) + 1);
                }
            }
        });

        dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        dataGraph.getGridLabelRenderer().setLabelVerticalWidth(100);

        dataGraph.getViewport().setScalable(true);
        dataGraph.getViewport().setScrollable(true);
        dataGraph.setNestedScrollingEnabled(true);
        dataGraph.getGridLabelRenderer().setHorizontalAxisTitle(" \nIntervalo muestreo(seg)");



        /*GraphView filteredDataGraph = binding.distanceGraphView;

        filteredDataGraph.addSeries(mFilteredDataSeries);
        filteredDataGraph.addSeries(mPointFilteredDataSeries);
        filteredDataGraph.addSeries(auxPointSeries);

        filteredDataGraph.getViewport().setXAxisBoundsManual(true);
        filteredDataGraph.getViewport().setMinX(0);
        filteredDataGraph.getViewport().setMaxX(4);


        filteredDataGraph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Reason reason) {
                if(maxX - minX > 4){
                    filteredDataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
                }else{
                    filteredDataGraph.getGridLabelRenderer().setNumHorizontalLabels((int)Math.ceil(maxX - minX) + 1);
                }
            }
        });

        filteredDataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        filteredDataGraph.getGridLabelRenderer().setLabelVerticalWidth(100);

        filteredDataGraph.getViewport().setScalable(true);
        filteredDataGraph.getViewport().setScrollable(true);
        filteredDataGraph.setNestedScrollingEnabled(true);
        filteredDataGraph.getGridLabelRenderer().setHorizontalAxisTitle(" \nIntervalo muestreo(seg)"); */

        binding.spikesGraphView.getGridLabelRenderer().setVerticalAxisTitle("Distancia(cm)");
        //binding.distanceGraphView.getGridLabelRenderer().setVerticalAxisTitle("Distancia total(cm)");


        return binding.getRoot();

    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    void setTempState(boolean started){
        binding.temporizadorRow1.setVisibility((started ? View.GONE : View.VISIBLE));
        binding.temporizadorRow2.setVisibility((started ? View.GONE : View.VISIBLE));
        binding.temporizadorRow3.setVisibility((started ? View.VISIBLE : View.GONE));
        binding.temporizadorRow4.setVisibility((started ? View.VISIBLE : View.GONE));
    }


    //float actDist = 0;
    double dataGraphLastX = 0d;//, filteredDataGraphLastX = 0d;
    public void showCapturedData(float dist){

        dataGraphLastX += 1d;
        mPointDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
        mDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);

        /*
        actDist += dist;
        filteredDataGraphLastX += 1d;
        mPointFilteredDataSeries.appendData(new DataPoint(filteredDataGraphLastX, actDist), false, 36000);
        mFilteredDataSeries.appendData(new DataPoint(filteredDataGraphLastX, actDist), false, 36000);
        */

        auxPointSeries.appendData(new DataPoint(dataGraphLastX + 2, mPointDataSeries.getHighestValueY()), true, 2);

        //mPointFilteredDataSeries.appendData(new DataPoint(filteredDataGraphLastX + 1, 0), false, 360000);
        //mPointDataSeries.appendData(new DataPoint(dataGraphLastX + 1, 0), false, 360000);

    }

    public void deviceConnected(String name){
        binding.connectionImageView.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.green));
        binding.connectionTextView.setText("Conectado");
        binding.connectionDeviceNameTextView.setText(name);
        binding.scanStateSwitch.setEnabled(true);
        binding.spikesGraphView.getSeries().clear();
    }

    public void deviceDisconnected(){
        binding.connectionImageView.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.red));
        binding.connectionTextView.setText("Desconectado");
        binding.connectionDeviceNameTextView.setText("ninguno");

        turnOffScanning();
    }

    public void turnOffScanning(){
        ControlCenter.getInstance().isSendingScan = false;
        binding.scanStateSwitch.setChecked(false);
        if(!ControlCenter.getInstance().connectedDevice){
            binding.scanStateSwitch.setEnabled(false);
        }


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}