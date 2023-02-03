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

    private FragmentGeneralBinding binding;

    LineGraphSeries<DataPoint> mDataSeries = new LineGraphSeries<>();
    PointsGraphSeries<DataPoint> mPointDataSeries = new PointsGraphSeries<>();
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
        // seteo de la grafica 
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

        // define la accion del editText que almacena el tiempo
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

        // set the onClickListener for the start button
        binding.temporizadorButtonStart.setOnClickListener( view -> {
            // si no esta conectado mandar un mensaje solicitando la conexion
            if(!ControlCenter.getInstance().connectedDevice){
                ControlCenter.getInstance().mainActivity.makeSnackB("Debes estar conectado para usar esta funcion");
                return;
            }
            // si no se ha ingresado una cantidad de tiempo solicitar el ingreso de una
            if(rsTime == null || rsTime.isEmpty() || rsTime.compareTo("0:0:0")==0){
                ControlCenter.getInstance().mainActivity.makeSnackB("Debe ingresar la duracion del temporizador (mayor a 00:00.000)");
                return;
            }
                // Envia el commando al ESP32 formato TIMER;mm:ss:ms;
                ControlCenter.getInstance().connectionFrag.sendCommand("TIMER;" + rsTime+";", () -> {
                    setTempState(true);
                    countDown = new Thread(
                            new tempRoutine(rsTime, () -> {
                                setTempState(false);
                                ControlCenter.getInstance().mainActivity.onUIThread(() -> binding.scanStateSwitch.setChecked(false));
                            }, binding.timeLeftScanTextView)
                    );
                    countDown.start();
                    ControlCenter.getInstance().isSendingScan = true;
                    binding.scanStateSwitch.setChecked(true);
                    binding.scanStateSwitch.setEnabled(false);

                }, 10000);

        });
        // al pulsar el boton de stop se manda el OFF;
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
                    // si se activa el muestreo se manda el ON; al esp32
                    ControlCenter.getInstance().connectionFrag.sendCommand("ON;", () -> {
                        ControlCenter.getInstance().isSendingScan = true;
                        binding.scanStateSwitch.setChecked(true);
                        binding.scanStateSwitch.setEnabled(true);

                    }, ()->{ 
                        binding.scanStateSwitch.setEnabled(true);
                    }, ()->{  
                        binding.scanStateSwitch.setEnabled(true);
                    },10000);
                    binding.scanStateSwitch.setChecked(false);
                }else{
                    // si no se apaga
                    if(ControlCenter.getInstance().isSendingScan){
                        ControlCenter.getInstance().connectionFrag.sendCommand("OFF;", () -> {
                            ControlCenter.getInstance().isSendingScan = false;
                            binding.scanStateSwitch.setChecked(false);
                            binding.scanStateSwitch.setEnabled(true);
                            ControlCenter.getInstance().askForDataPoints(
                                    ()->ControlCenter.getInstance().mainActivity.makeSnackB("Muestreo almacenado exitosamente"),
                                    ()->ControlCenter.getInstance().mainActivity.makeSnackB("No se pudo guardar el muestreo"));
                        }, ()->{ 
                            binding.scanStateSwitch.setEnabled(true);
                        }, ()->{ 
                            binding.scanStateSwitch.setEnabled(true);
                        },100000);
                        binding.scanStateSwitch.setChecked(true);
                    }
                }
                binding.scanStateSwitch.setEnabled(false);
            }
        });
        binding.connectionImageView.setOnClickListener( view -> { ControlCenter.getInstance().mainContent.navigateViewPag(2); } );


        float inter = ControlCenter.getInstance().scanInterval * 0.001f;

        // label formatter para el grafico
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
        mDataSeries.setDrawDataPoints(true);
        mDataSeries.setDataPointsRadius(6);
        
        mPointDataSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
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
        // se agregan las series al grafico
        dataGraph.addSeries(mDataSeries);
        dataGraph.addSeries(mPointDataSeries);
        dataGraph.addSeries(auxPointSeries);
        dataGraph.getViewport().setXAxisBoundsManual(true);
        dataGraph.getViewport().setMinX(0);
        dataGraph.getViewport().setMaxX(4);
        // se configura el viewport para que se pueda hacer zoom y scroll
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
        // se configura el viewport para que se pueda hacer zoom y scroll
        dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        dataGraph.getGridLabelRenderer().setLabelVerticalWidth(100);
        dataGraph.getViewport().setScalable(true);
        dataGraph.getViewport().setScrollable(true);
        dataGraph.setNestedScrollingEnabled(true);
        dataGraph.getGridLabelRenderer().setHorizontalAxisTitle(" \nIntervalo muestreo(seg)");
        binding.spikesGraphView.getGridLabelRenderer().setVerticalAxisTitle("Distancia(cm)");

        // funcion del boton de actualizar el tiempo manda el comando CONFIG;TIEMPOMUESTREO;tiempo; al dispositivo
        binding.actualizarTiempoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!ControlCenter.getInstance().connectedDevice){
                    ControlCenter.getInstance().mainActivity.makeSnackB("Debes estar conectado para usar esta funcion");
                    return;
                }
                int tiempoMuestreo = Integer.parseInt(String.valueOf(binding.editTextMuestreo.getText()));
                if(tiempoMuestreo<100){
                    ControlCenter.getInstance().mainActivity.makeSnackB("El tiempo de muestreo no puede ser menor a 100 ms");
                    return;
                }
                ControlCenter.getInstance().connectionFrag.sendCommand("CONFIG;TIEMPOMUESTREO;"+tiempoMuestreo+";",()->{ ControlCenter.getInstance().scanInterval = tiempoMuestreo; },10000);
            }
        });

        // funcion del boton de reiniciar el tiempo manda el comando RESET; al dispositivo
        binding.reiniciarTiempoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!ControlCenter.getInstance().connectedDevice){
                    ControlCenter.getInstance().mainActivity.makeSnackB("Debes estar conectado para usar esta funcion");
                    return;
                }
                ControlCenter.getInstance().connectionFrag.sendCommand("RESET;",()->{
                    binding.editTextMuestreo.setText("");
                    ControlCenter.getInstance().scanInterval = 100;
                }  ,10000);
            }
        });



        return binding.getRoot();
    }

    double amountDataX = 1;
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // funcion que se llama cuando se presiona el boton de iniciar el temporizador
    void setTempState(boolean started){
        ControlCenter.getInstance().mainActivity.onUIThread(()->{
            binding.temporizadorRow1.setVisibility((started ? View.GONE : View.VISIBLE));
            binding.temporizadorRow2.setVisibility((started ? View.GONE : View.VISIBLE));
            binding.temporizadorRow3.setVisibility((started ? View.VISIBLE : View.GONE));
            binding.temporizadorRow4.setVisibility((started ? View.VISIBLE : View.GONE));
        });
    }


    double dataGraphLastX = 0d;

    // funcion que se llama cuando se recibe un dato del dispositivo
    public void showCapturedData(float dist){
        dataGraphLastX += 1d;

        mPointDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
        mDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
        binding.spikesGraphView.getViewport().scrollToEnd();
        auxPointSeries.appendData(new DataPoint(dataGraphLastX + 2, mPointDataSeries.getHighestValueY()), true, 2);
    }

    // funcion que se llama cuando se conecta un dispositivo 
    public void deviceConnected(String name){
        binding.connectionImageView.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.green));
        binding.connectionTextView.setText("Conectado");
        binding.connectionDeviceNameTextView.setText(name);
        binding.scanStateSwitch.setEnabled(true);
    }

    // funcion que se llama cuando se desconecta un dispositivo
    public void deviceDisconnected(){
        binding.connectionImageView.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.red));
        binding.connectionTextView.setText("Desconectado");
        binding.connectionDeviceNameTextView.setText("ninguno");
        turnOffScanning();
    }

    // funcion que se llama cuando se apaga el escaneo
    public void turnOffScanning(){

        if(!ControlCenter.getInstance().connectedDevice){
            ControlCenter.getInstance().isSendingScan = false;
            binding.scanStateSwitch.setChecked(false);

            binding.scanStateSwitch.setEnabled(false);
        }else{
            binding.scanStateSwitch.setChecked(false);
            ControlCenter.getInstance().isSendingScan = false;
        }
    }

    // funcion que se llama cuando se enciende el escaneo
    public void turnOnScanning(){
        ControlCenter.getInstance().isSendingScan = true;
        binding.scanStateSwitch.setChecked(true);
        binding.scanStateSwitch.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
