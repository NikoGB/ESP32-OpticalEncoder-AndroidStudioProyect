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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = FragmentShowDataPointsBinding.inflate(inflater, container, false);
        ControlCenter.getInstance().showDataPointFrag = this;
        if(getArguments() != null){
            setDataToShow(getArguments().getString("dataP"));
        }
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    void setDataToShow(String dataP){//START;NAME;FECHAINI;INTERVAL;MESUAREUNIT;....STOP;FECHAFIN; <-
        // verifica que exitan datos
        if(dataP.equals("")){
            binding.dataInfoNameTextView.setText("No se encontro data");
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

        // Add the last data point to the data series and set the last x value to the new last x value and set the y value to 0
        mPointDataSeries.appendData(new DataPoint(dataGraphLastX + 1, 0), false, 360000);
        // Set the draw data points to true and set the data points radius to 6
        mDataSeries.setDrawDataPoints(true);
        mDataSeries.setDataPointsRadius(6);

        // Set the data series to the data graph
        GraphView dataGraph = binding.dataGraphView;

        // Set the data series to the data graph
        dataGraph.addSeries(mDataSeries);
        dataGraph.addSeries(mPointDataSeries);

        // Set the viewport to the data graph and set the x axis bounds to manual
        dataGraph.getViewport().setXAxisBoundsManual(true);
        // Set a event listener to the data graph to set the number of horizontal labels to the number of data points
        dataGraph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Reason reason) {
            // if the amount of data points is greater than 4 set the number of horizontal labels to 4
            if((amountDataX = maxX - minX) > 4){
                dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
            }else{
                // Set the number of horizontal labels to the amount of data points
                dataGraph.getGridLabelRenderer().setNumHorizontalLabels((int)Math.ceil(amountDataX) + 1);
            }
            // Set the amount of data points to the amount of data points divided by 8
            amountDataX = (int)Math.floor(Math.max(1, amountDataX / 8));
            }
        });

        // Set the viewport to the data graph and set the y axis bounds to manual
        mPointDataSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                // Check if the data point is the first data point or if the data point is a multiple of the amount of data points
                if (((int) dataPoint.getX() % (int) amountDataX) != 0 || dataPoint.getX() == mPointDataSeries.getHighestValueX()) {
                    return;
                }
                // Set the paint color to red and set the stroke width to 2 and set the text size to 36
                paint.setColor(getResources().getColor(android.R.color.holo_red_dark));
                paint.setStrokeWidth(2);
                paint.setTextSize(36);
                // Create a new decimal format to format the value to two decimal places
                DecimalFormat df = new DecimalFormat("#.##");
                // Create a new string to store the formatted value and return it
                String str = df.format(dataPoint.getY());
                // Draw the text to the canvas and set the x and y position to the data point x and y position and set the paint
                canvas.drawText(str, x - (18 * (str.length() - 1) - 9), y - 30, paint);
            }
        });
        // Set the min and max x values to the lowest and highest x values of the data series
        dataGraph.getViewport().setMinX(mDataSeries.getLowestValueX());
        dataGraph.getViewport().setMaxX(mDataSeries.getHighestValueX() - 1);
        // Scroll to the end of the data series
        dataGraph.getViewport().scrollToEnd();

        // Set the number of horizontal labels to 4 and set the vertical label width to 100
        dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        dataGraph.getGridLabelRenderer().setLabelVerticalWidth(100);

        // Set the scalable and scrollable to true and set the nested scrolling enabled to true
        dataGraph.getViewport().setScalable(true);
        dataGraph.getViewport().setScrollable(true);
        dataGraph.setNestedScrollingEnabled(true);
        // Set the horizontal axis title to the string "Intervalo muestreo(seg)"
        dataGraph.getGridLabelRenderer().setHorizontalAxisTitle(" \nIntervalo muestreo(seg)");
        
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
