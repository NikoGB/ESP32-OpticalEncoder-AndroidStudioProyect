package com.example.finalencoder_controller;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.example.finalencoder_controller.databinding.FragmentShowDataPointsBinding;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.text.DecimalFormat;


public class ShowDataPointsFragment extends Fragment {
    private FragmentShowDataPointsBinding binding;
    String toSaveData = "";
    String dvName = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = FragmentShowDataPointsBinding.inflate(inflater, container, false);
        ControlCenter.getInstance().showDataPointFrag = this;
        if(getArguments() != null){
            setDataToShow(getArguments().getString("dataP"));
            dvName = getArguments().getString("devName");
        }

        binding.ExportDataButton.setOnClickListener(
            view -> {
                exportDataTxt();
            }
        );
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    void setDataToShow(String dataP){//START;NAME;FECHAINI;INTERVAL;MESUAREUNIT;....STOP;FECHAFIN; <-
        // verifica que exitan datos
        if(dataP.equals("")){
            binding.dataInfoNameTextView.setText("No se encontro data");
            binding.ExportDataButton.setEnabled(false);
            return;
        }
        // si el archivo contiene un error va a tener el # al final
        if (dataP.contains("#")){
            // en ese caso se elimina para que no afecte al resto de la funcion
            dataP= dataP.replace("#","");
        }
        // divide los datos en 2 partes, la primera es una session de muestreo (formato de arriba), la otra el resto
        String[] sSplit = dataP.split("STOP");
        // separa por ; los valores del muestreo
        String[] startSplit = sSplit[0].split(";");

        // muestra el nombre del muestreo en el textView
        binding.dataInfoNameTextView.setText(startSplit[1]);

        // obtiene la fecha formateada yyyy-mm-dd hh:mm:ss.ms
        String formatDate = startSplit[2].replace("T", "  ");
        formatDate =  formatDate.substring(0, formatDate.lastIndexOf(":")) + "." + formatDate.substring(formatDate.lastIndexOf(":") + 1);
        // muestra la fecha
        binding.dataInfoStartTimeTextView.setText(formatDate);

        // obtine el tiempo de termino que se encuentra en la sSplit[1] y formatea la fecha yyyy-mm-dd hh:mm:ss.ms
        formatDate = sSplit[1].substring(sSplit[1].indexOf(";") + 1, sSplit[1].length() - 1).replace("T", "  ");
        formatDate =  formatDate.substring(0, formatDate.lastIndexOf(":")) + "." + formatDate.substring(formatDate.lastIndexOf(":") + 1);
        // muestra la fecha
        binding.dataInfoEndTimeTextView.setText(formatDate);

        // define la magnitud de los datos (cm,m,mm)
        binding.dataGraphView.getGridLabelRenderer().setVerticalAxisTitle("Distancia(" + (startSplit[4])+")");

        // define el intervalo entre los datos
        float intervalo = Integer.parseInt(startSplit[3]) * 0.001f;

        // Crea un nuevo label para los datos del grafico
        LabelFormatter labfor = new LabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(isValueX){
                    DecimalFormat df = new DecimalFormat("#.##");
                    String str = df.format(value * intervalo) + "\n";
                    return str;
                }else{
                    return "" + value;
                }
            }
            @Override
            public void setViewport(Viewport viewport) {}
        };
        // asigna el label para el formateo de los datos
        binding.dataGraphView.getGridLabelRenderer().setLabelFormatter(labfor);
        // crea series para los graficos de linea y los datapoints
        LineGraphSeries<DataPoint> mDataSeries = new LineGraphSeries<>();
        PointsGraphSeries<DataPoint> mPointDataSeries = new PointsGraphSeries<>();

        // declara un double para almacenar la ultima posicion de x
        double dataGraphLastX = 0d;
        // divide los datos en una array basandose en el formato de !distance1\n!distance2...
        String[] dats = startSplit[5].replace("\n", "").split("!");
        // para cada dato en datoS
        for (String dat : dats) {
            // verifica que los datos no sean vacios
            if (dat.equals("")) {
                continue;
            }
            
            // transforma el valor del dato (distancia) a un float
            float dist = Float.parseFloat(dat);
            // agrega 1 a la ultima posicion de X
            dataGraphLastX += 1d;
            // agrega los los datos al grafico
            mPointDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
            mDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
        }

        mPointDataSeries.appendData(new DataPoint(dataGraphLastX + 1, 0), false, 360000);
        mDataSeries.setDrawDataPoints(true);
        mDataSeries.setDataPointsRadius(6);

        GraphView dataGraph = binding.dataGraphView;

        // agrega los datos al grafico
        dataGraph.addSeries(mDataSeries);
        dataGraph.addSeries(mPointDataSeries);

        // define el tamaño de los datos
        dataGraph.getViewport().setXAxisBoundsManual(true);
        dataGraph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Reason reason) {
            if((amountDataX = maxX - minX) > 4){
                dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
            }else{
                dataGraph.getGridLabelRenderer().setNumHorizontalLabels((int)Math.ceil(amountDataX) + 1);
            }
            amountDataX = (int)Math.floor(Math.max(1, amountDataX / 8));
            }
        });

        // define la forma de los datos en el grafico
        mPointDataSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                if (((int) dataPoint.getX() % (int) amountDataX) != 0 || dataPoint.getX() == mPointDataSeries.getHighestValueX()) {
                    return;
                }
                paint.setColor(getResources().getColor(android.R.color.holo_red_dark));
                paint.setStrokeWidth(2);
                paint.setTextSize(36);
                DecimalFormat df = new DecimalFormat("#.##");
                String str = df.format(dataPoint.getY());
                canvas.drawText(str, x - (18 * (str.length() - 1) - 9), y - 30, paint);
            }
        });
        // define el tamaño del grafico y el tamaño de los datos
        dataGraph.getViewport().setMinX(mDataSeries.getLowestValueX());
        dataGraph.getViewport().setMaxX(mDataSeries.getHighestValueX() - 1);
        dataGraph.getViewport().scrollToEnd();

        dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        dataGraph.getGridLabelRenderer().setLabelVerticalWidth(100);

        dataGraph.getViewport().setScalable(true);
        dataGraph.getViewport().setScrollable(true);
        dataGraph.setNestedScrollingEnabled(true);
        dataGraph.getGridLabelRenderer().setHorizontalAxisTitle(" \nIntervalo muestreo(seg)");

        toSaveData = dataP;

    }

    // funcion para exportar los datos a un archivo txt
    void exportDataTxt(){
        String toSaveTxt = "";

        // divide los datos en 2 partes, la primera es una session de muestreo (formato de arriba), la otra el resto
        String[] sSplit = toSaveData.split("STOP");

        // separa por ; los valores del muestreo
        String[] startSplit = sSplit[0].split(";");

        toSaveTxt += "Nombre del agenamiento:\t" + startSplit[1] + "\n";

        // obtiene la fecha formateada yyyy-mm-dd hh:mm:ss.ms
        String formatDate = startSplit[2].replace("T", "  ");
        formatDate =  formatDate.substring(0, formatDate.lastIndexOf(":")) + "." + formatDate.substring(formatDate.lastIndexOf(":") + 1);
        // muestra la fecha
        toSaveTxt += "Tiempo de inicio:\t \t" + formatDate + "\n";


        String fName = startSplit[1];

        if (fName.isEmpty()){

        }

        // obtine el tiempo de termino que se encuentra en la sSplit[1] y formatea la fecha yyyy-mm-dd hh:mm:ss.ms
        formatDate = sSplit[1].substring(sSplit[1].indexOf(";") + 1, sSplit[1].length() - 1).replace("T", "  ");
        formatDate =  formatDate.substring(0, formatDate.lastIndexOf(":")) + "." + formatDate.substring(formatDate.lastIndexOf(":") + 1);
        // muestra la fecha
        toSaveTxt += "Tiempo de Termino:\t" + formatDate + "\n";
        toSaveTxt += "Intervalo de muestreo:\t" + startSplit[3] + " ms\n";

        toSaveTxt += "Tiempo(Seg)\t";
        toSaveTxt += "Distancia(" + (startSplit[4])+")\n";

        String[] dats = startSplit[5].replace("\n", "").split("!");

        float intervalo = Integer.parseInt(startSplit[3]) * 0.001f;
        int muestreoIdx = 0;

        // para cada dato en datoS
        for (String dat : dats) {
            // verifica que los datos no sean vacios
            if (dat.equals("")) {
                continue;
            }

            toSaveTxt += (intervalo * muestreoIdx) + "\t \t" + dat + "\n";
            muestreoIdx++;
        }
        ControlCenter.getInstance().saveDataOnStorage(toSaveTxt, fName, dvName);
    }


    // Declare a new double to store the amount of data points
    double amountDataX = 1, amountFilteredDataX = 1;

    // Override the on destroy view method to set the binding to null
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
