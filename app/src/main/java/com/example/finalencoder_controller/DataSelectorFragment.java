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

    private FragmentDataSelectionBinding binding;
    DataSelectorFragment.DeviceItemAdapter devicesAdapterListView;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = FragmentDataSelectionBinding.inflate(inflater, container, false);


        File t = ControlCenter.getInstance().mainActivity.getFilesDir();
        File[] allF = t.listFiles();

        if(allF != null && allF.length > 0){
            ArrayList<DeviceItem> deviceItems = new ArrayList<DeviceItem>();

            for(int i = 0; i < allF.length; i++){
                deviceItems.add(new DeviceItem(allF[i].getName(), "no encontrado"));
            }

            devicesAdapterListView = new DeviceItemAdapter(getContext(), deviceItems);
            ListView debListView = (ListView) binding.dataDeviceListView;
            debListView.setAdapter(devicesAdapterListView);

            devicesAdapterListView.notifyDataSetChanged();

            updateListHeight(debListView);
        }
        return binding.getRoot();
    }


    public static class DeviceItem {
        public String dName, dMac;
        public DeviceItem(String devName, String devMac){
            dName = devName; dMac = devMac;
        }
    }

    public class DeviceItemAdapter extends ArrayAdapter<DataSelectorFragment.DeviceItem> {
        public DeviceItemAdapter(@NonNull Context context, ArrayList<DataSelectorFragment.DeviceItem> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            DataSelectorFragment.DeviceItem device = getItem(position);

            if(convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.data_device_list_item_1, parent, false);
            }

            TextView tvDName = (TextView) convertView.findViewById(R.id.dataDevice_name);
            TextView tvDMac = (TextView) convertView.findViewById(R.id.dataDevice_mac);
            ImageButton dataButton = (ImageButton) convertView.findViewById(R.id.dataDevice_interactItem_viewData);
            ImageButton scButton = (ImageButton) convertView.findViewById(R.id.dataDevice_interactItem_viewSchedules);

            tvDName.setText(device.dName);
            tvDMac.setText(device.dMac);

            dataButton.setOnClickListener(
                    view -> {
                        Bundle bun = new Bundle();
                        bun.putBoolean("isSchedules", false);
                        bun.putString("devName", device.dName);
                        ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_dataSelectorFragment_to_dataSchedulerFragment, bun,"Muestreos realizados");
                    }
            );

            scButton.setOnClickListener(
                    view -> { Bundle bun = new Bundle();
                        bun.putBoolean("isSchedules", true);
                        bun.putString("devName", device.dName);
                        ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_dataSelectorFragment_to_dataSchedulerFragment, bun, "Muestreos agendados");
                    }
            );

            return convertView;
        }
    }

    void updateListHeight(ListView lv){
        View li = lv.getAdapter().getView(0, null, lv);
        li.measure(0,0);

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

