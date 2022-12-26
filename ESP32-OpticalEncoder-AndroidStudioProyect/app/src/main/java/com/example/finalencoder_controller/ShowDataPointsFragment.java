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
        //Get the instance of the control center
        ControlCenter.getInstance().showDataPointFrag = this;
        //Check to see if the bundle has any data
        if(getArguments() != null){
            //Get the data from the bundle
            setDataToShow(getArguments().getString("dataP"));
        }

        //Return the view
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    void setDataToShow(String dataP){//START;NAME;FECHAINI;INTERVAL;MESUAREUNIT;....STOP;FECHAFIN; <-
        // Check if the data is empty
        if(dataP.equals("")){
            // Set the text to no data found
            binding.dataInfoNameTextView.setText("No se encontro data");
            return;
        }
        // Split the data into two parts the first part is the name, start time, interval, and measure unit and the second part is the end time
        String[] sSplit = dataP.split("STOP");
        // Split the start data into the name, start time, interval, and measure unit and store it in an array of strings
        String[] startSplit = sSplit[0].split(";");

        // Set the name of the data to the name of the data in the array
        binding.dataInfoNameTextView.setText(startSplit[1]);

        // Set the start time of the data to the start time of the data in the array
        String formatDate = startSplit[2].replace("T", "  ");
        // Remove the seconds from the time and replace it with milliseconds and set the text to the new time format because the time format is yyyy-mm-dd hh:mm:ss.ms
        formatDate =  formatDate.substring(0, formatDate.lastIndexOf(":")) + "." + formatDate.substring(formatDate.lastIndexOf(":") + 1);
        // Set the text to the new time format
        binding.dataInfoStartTimeTextView.setText(formatDate);

        // Set the end time of the data to the end time of the data in the array (sSplit[1] is the second part of the data)
        formatDate = sSplit[1].substring(sSplit[1].indexOf(";") + 1, sSplit[1].length() - 1).replace("T", "  ");
        // Remove the seconds from the time and replace it with milliseconds and set the text to the new time format because the time format is yyyy-mm-dd hh:mm:ss.ms
        formatDate =  formatDate.substring(0, formatDate.lastIndexOf(":")) + "." + formatDate.substring(formatDate.lastIndexOf(":") + 1);
        // Set the text to the new time format
        binding.dataInfoEndTimeTextView.setText(formatDate);

        // Set the interval of the data to the interval of the data in the array
        binding.dataGraphView.getGridLabelRenderer().setVerticalAxisTitle("Distancia(" + (startSplit[4])+")");

        // Set the measure unit of the data to the measure unit of the data in the array
        float intervalo = Integer.parseInt(startSplit[3]) * 0.001f;

        // Create a new label formatter to format the labels on the graph to show the distance instead of the time 
        LabelFormatter labfor = new LabelFormatter() {
            // Override the format label method to format the labels on the graph to show the distance instead of the time
            @Override
            public String formatLabel(double value, boolean isValueX) {
                // Check if the value is the x value (the time)
                if(isValueX){
                    // Create a new decimal format to format the value to two decimal places
                    DecimalFormat df = new DecimalFormat("#.##");
                    // Create a new string to store the formatted value and return it
                    String str = df.format(value * intervalo) + "\n";
                    // Return the formatted value
                    return str;
                }else{
                    // Return the value as a string
                    return "" + value;
                }
            }

            @Override
            public void setViewport(Viewport viewport) {

            }
        };
        // Set the label formatter to the new label formatter
        binding.dataGraphView.getGridLabelRenderer().setLabelFormatter(labfor);
        // Create a new line graph series to store the data 
        LineGraphSeries<DataPoint> mDataSeries = new LineGraphSeries<>();
        // Create a new points graph series to store the data 
        PointsGraphSeries<DataPoint> mPointDataSeries = new PointsGraphSeries<>();

        // Declare a new double to store the last x value (the number of data points)
        double dataGraphLastX = 0d;
        // Split the data into an array of strings the data is split by the new line character and the exclamation point character (!distance1\n!distance2...)
        String[] dats = startSplit[5].replace("\n", "").split("!");

        // Loop through the array of strings
        for (String dat : dats) {
            // Check if the string is empty
            if (dat.equals("")) {
                // Continue the loop
                continue;
            }
            
            // Parse the string to a float and add it to the data series
            float dist = Float.parseFloat(dat);
            // add one to the last x value
            dataGraphLastX += 1d;
            // Add the data to the data series and set the last x value and set the y value to the distance
            // TODO: Check the false and 36000
            // the false means that the data should not be animated and the 36000 means that the data should be shown for 36000 seconds
            mPointDataSeries.appendData(new DataPoint(dataGraphLastX, dist), false, 36000);
            // Add the data to the data series and set the last x value to the new last x value and set the y value to the distance
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
