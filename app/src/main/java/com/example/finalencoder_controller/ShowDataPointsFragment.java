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
        if(dataP.equals("")){
            binding.dataInfoNameTextView.setText("No se encontro data");
            return;
        }
        String[] sSplit = dataP.split("STOP");
        String[] startSplit = sSplit[0].split(";");

        binding.dataInfoNameTextView.setText(startSplit[1]);

        String formatDate = startSplit[2].replace("T", "  ");
        formatDate =  formatDate.substring(0, formatDate.lastIndexOf(":")) + "." + formatDate.substring(formatDate.lastIndexOf(":") + 1);
        binding.dataInfoStartTimeTextView.setText(formatDate);

        formatDate = sSplit[1].substring(sSplit[1].indexOf(";") + 1, sSplit[1].length() - 1).replace("T", "  ");
        formatDate =  formatDate.substring(0, formatDate.lastIndexOf(":")) + "." + formatDate.substring(formatDate.lastIndexOf(":") + 1);
        binding.dataInfoEndTimeTextView.setText(formatDate);


        binding.dataGraphView.getGridLabelRenderer().setVerticalAxisTitle("Distancia(" + (startSplit[4])+")");
        //binding.filteredGraphView.getGridLabelRenderer().setVerticalAxisTitle("Distancia total(" + (startSplit[4])+")");

        float intervalo = Integer.parseInt(startSplit[3]) * 0.001f;

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
            public void setViewport(Viewport viewport) {

            }
        };

        /*binding.filteredGraphView.getGridLabelRenderer().setLabelFormatter(labfor);
        LineGraphSeries<DataPoint> mFilteredDataSeries = new LineGraphSeries<>();
        PointsGraphSeries<DataPoint> mPointFilteredDataSeries = new PointsGraphSeries<>(); */


        binding.dataGraphView.getGridLabelRenderer().setLabelFormatter(labfor);
        LineGraphSeries<DataPoint> mDataSeries = new LineGraphSeries<>();
        PointsGraphSeries<DataPoint> mPointDataSeries = new PointsGraphSeries<>();


        //float actDist = 0;
        double dataGraphLastX = 0d;//, filteredDataGraphLastX = 0d;
        String[] dats = startSplit[5].replace("\n", "").split("!");

        for (String dat : dats) {
            if (dat.equals("")) {
                continue;
            }

            float dist = Float.parseFloat(dat);
            dataGraphLastX += 1d;
            mPointDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
            mDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);

            /*
            actDist += dist;
            filteredDataGraphLastX += 1d;
            mPointFilteredDataSeries.appendData(new DataPoint(filteredDataGraphLastX, actDist), false, 36000);
            mFilteredDataSeries.appendData(new DataPoint(filteredDataGraphLastX, actDist), false, 36000); */
        }

        /*
        mPointFilteredDataSeries.appendData(new DataPoint(filteredDataGraphLastX + 1, 0), false, 360000);
        mFilteredDataSeries.setDrawDataPoints(true);
        mFilteredDataSeries.setDataPointsRadius(6);
        */

        mPointDataSeries.appendData(new DataPoint(dataGraphLastX + 1, 0), false, 360000);
        mDataSeries.setDrawDataPoints(true);
        mDataSeries.setDataPointsRadius(6);


        GraphView dataGraph = binding.dataGraphView;

        dataGraph.addSeries(mDataSeries);
        dataGraph.addSeries(mPointDataSeries);

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

        dataGraph.getViewport().setMinX(mDataSeries.getLowestValueX());
        dataGraph.getViewport().setMaxX(mDataSeries.getHighestValueX() - 1);
        dataGraph.getViewport().scrollToEnd();

        dataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        dataGraph.getGridLabelRenderer().setLabelVerticalWidth(100);

        dataGraph.getViewport().setScalable(true);
        dataGraph.getViewport().setScrollable(true);
        dataGraph.setNestedScrollingEnabled(true);
        dataGraph.getGridLabelRenderer().setHorizontalAxisTitle(" \nIntervalo muestreo(seg)");
        /*
        GraphView filteredDataGraph = binding.filteredGraphView;

        filteredDataGraph.addSeries(mFilteredDataSeries);
        filteredDataGraph.addSeries(mPointFilteredDataSeries);

        filteredDataGraph.getViewport().setXAxisBoundsManual(true);
        filteredDataGraph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Reason reason) {
            if((amountFilteredDataX = maxX - minX) > 4){
                filteredDataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
            }else{
                filteredDataGraph.getGridLabelRenderer().setNumHorizontalLabels((int)Math.ceil(amountFilteredDataX) + 1);
            }

            amountFilteredDataX = (int)Math.floor(Math.max(1, amountFilteredDataX / 8));
            }
        });


        mPointFilteredDataSeries.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                if (((int) dataPoint.getX() % (int) amountFilteredDataX) != 0 || dataPoint.getX() == mPointFilteredDataSeries.getHighestValueX()) {
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
        filteredDataGraph.getViewport().setMinX(mDataSeries.getLowestValueX());
        filteredDataGraph.getViewport().setMaxX(mDataSeries.getHighestValueX() - 1);
        filteredDataGraph.getViewport().scrollToEnd();

        filteredDataGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        filteredDataGraph.getGridLabelRenderer().setLabelVerticalWidth(100);

        filteredDataGraph.getViewport().setScalable(true);
        filteredDataGraph.getViewport().setScrollable(true);
        filteredDataGraph.setNestedScrollingEnabled(true);
        filteredDataGraph.getGridLabelRenderer().setHorizontalAxisTitle(" \nIntervalo muestreo(seg)"); */
    }

    double amountDataX = 1, amountFilteredDataX = 1;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
