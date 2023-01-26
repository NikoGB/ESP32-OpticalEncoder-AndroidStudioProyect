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
        // obtitene la direccion de la carpeta de archivos
        File t = ControlCenter.getInstance().mainActivity.getFilesDir();
        File[] allF = t.listFiles();
        // si hay archivos en la carpeta
        if(allF != null && allF.length > 0){
            // crea una lista de dispositivos
            ArrayList<DeviceItem> deviceItems = new ArrayList<DeviceItem>();
            // para cada archivo en la carpeta 
            for(int i = 0; i < allF.length; i++){
                // si el archivo es un directorio (carpeta) se agrega a la lista, si no se ignora
                deviceItems.add(new DeviceItem(allF[i].getName(), "no encontrado"));
            }
            // crea un adaptador para la lista de dispositivos
            devicesAdapterListView = new DeviceItemAdapter(getContext(), deviceItems);
            ListView debListView = (ListView) binding.dataDeviceListView;
            debListView.setAdapter(devicesAdapterListView);
            devicesAdapterListView.notifyDataSetChanged();
            // actualiza el tamaño de la lista
            updateListHeight(debListView);
        }
        return binding.getRoot();
    }

    // clase para almacenar los datos de un dispositivo
    public static class DeviceItem {
        public String dName, dMac;
        public DeviceItem(String devName, String devMac){
            dName = devName; dMac = devMac;
        }
    }

    // adaptador para la lista de dispositivos
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

            // boton para ver los datos
            dataButton.setOnClickListener(
                    view -> {
                        Bundle bun = new Bundle();
                        bun.putBoolean("isSchedules", false);
                        bun.putString("devName", device.dName);
                        ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_dataSelectorFragment_to_dataSchedulerFragment, bun,"Muestreos realizados");
                    }
            );
            // boton para ver los agendamientos
            scButton.setOnClickListener(
                    view -> {
                        Bundle bun = new Bundle();
                        bun.putBoolean("isSchedules", true);
                        bun.putString("devName", device.dName);
                        ControlCenter.getInstance().mainActivity.navigateTo(R.id.action_dataSelectorFragment_to_dataSchedulerFragment, bun, "Muestreos agendados");
                    }
            );
            return convertView;
        }
    }

    /**
     * actualiza el tamaño de la lista
     * @param lv: lista a actualizar
     */
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

