package com.example.finalencoder_controller;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.example.finalencoder_controller.databinding.FragmentDataSelectionBinding;

public class DataSelectorFragment extends Fragment {

    // variables to store de binding and the adapter for the list view
    private FragmentDataSelectionBinding binding;
    DataSelectorFragment.DeviceItemAdapter devicesAdapterListView;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = FragmentDataSelectionBinding.inflate(inflater, container, false);
        // get the files directory and list all the files
        File t = ControlCenter.getInstance().mainActivity.getFilesDir();
        File[] allF = t.listFiles();
        // check if there are any files
        if(allF != null && allF.length > 0){
            // create the list of devices
            ArrayList<DeviceItem> deviceItems = new ArrayList<DeviceItem>();
            // add all the files to the list
            for(int i = 0; i < allF.length; i++){
                deviceItems.add(new DeviceItem(allF[i].getName(), "no encontrado"));
            }
            // create the adapter and set it to the list view and update the list view
            devicesAdapterListView = new DeviceItemAdapter(getContext(), deviceItems);
            ListView debListView = (ListView) binding.dataDeviceListView;
            debListView.setAdapter(devicesAdapterListView);
            // update the list view height to fit all the items
            devicesAdapterListView.notifyDataSetChanged();
            updateListHeight(debListView);
        }
        return binding.getRoot();
    }

    // class to store the device name and mac address
    public static class DeviceItem {
        // variables to store the device name and mac address
        public String dName, dMac;
        // constructor to create a new device item
        public DeviceItem(String devName, String devMac){
            dName = devName; dMac = devMac;
        }
    }

    // class to create the adapter for the list view
    public class DeviceItemAdapter extends ArrayAdapter<DataSelectorFragment.DeviceItem> {
        // constructor to create a new adapter
        public DeviceItemAdapter(@NonNull Context context, ArrayList<DataSelectorFragment.DeviceItem> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            // get the device item and check if the view is null
            DataSelectorFragment.DeviceItem device = getItem(position);
            if(convertView == null){
                // inflate the view
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.data_device_list_item_1, parent, false);
            }
            // get the text views and buttons and set the text 
            TextView tvDName = (TextView) convertView.findViewById(R.id.dataDevice_name);
            TextView tvDMac = (TextView) convertView.findViewById(R.id.dataDevice_mac);
            ImageButton dataButton = (ImageButton) convertView.findViewById(R.id.dataDevice_interactItem_viewData);
            ImageButton scButton = (ImageButton) convertView.findViewById(R.id.dataDevice_interactItem_viewSchedules);

            tvDName.setText(device.dName);
            tvDMac.setText(device.dMac);

            // set the buttons on click listeners
            dataButton.setOnClickListener(
                    view -> {
                        // create a bundle to pass the device name and if the data is schedules or not
                        Bundle bun = new Bundle();
                        bun.putBoolean("isSchedules", false);
                        bun.putString("devName", device.dName);
                        // navigate to the data scheduler fragment and pass the bundle and the title
                        ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_dataSelectorFragment_to_dataSchedulerFragment, bun,"Muestreos realizados");
                    }
            );
            scButton.setOnClickListener(
                    view -> {
                        // create a bundle to pass the device name and if the data is schedules or not
                        Bundle bun = new Bundle();
                        bun.putBoolean("isSchedules", true);
                        bun.putString("devName", device.dName);
                        // navigate to the data scheduler fragment and pass the bundle and the title
                        ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_dataSelectorFragment_to_dataSchedulerFragment, bun, "Muestreos agendados");
                    }
            );
            // return the view
            return convertView;
        }
    }

    // function to update the list view height
    void updateListHeight(ListView lv){
        // get the first item and measure it
        View li = lv.getAdapter().getView(0, null, lv);
        li.measure(0,0);
        // set the list view height to fit all the items
        ViewGroup.LayoutParams params = lv.getLayoutParams();
        params.height = (li.getMeasuredHeight() + lv.getDividerHeight()) * (lv.getAdapter().getCount());
        lv.setLayoutParams(params);
        lv.requestLayout();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

