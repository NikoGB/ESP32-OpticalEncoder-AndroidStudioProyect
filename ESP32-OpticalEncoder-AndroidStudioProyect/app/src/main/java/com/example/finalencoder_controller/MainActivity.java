package com.example.finalencoder_controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.example.finalencoder_controller.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.storage.StorageManager;
import android.service.controls.Control;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    boolean isForward = false;
    String tittle = "";
    List<String> prevTitles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // rellena el binding con el layout de la actividad
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        ControlCenter.getInstance().mainActivity = this;
        tittle = "Experimento de muestreo";

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();

        // Obteniendo el acceso al bluetooth a penas se carga la app para evitar crasheos
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)!= PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT>=31){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH_CONNECT},100);
            }
        }
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH)!= PackageManager.PERMISSION_GRANTED){
            if(Build.VERSION.SDK_INT>=31){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH},100);
            }

        }
        // ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH_CONNECT},100);
        startActivity(enableBtIntent);

        // obteniendo permisos de lectura y escritura de archivos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }


        // Se agrega un listener para saber cuando se cambia de fragmento
        navController.addOnDestinationChangedListener(
                new NavController.OnDestinationChangedListener() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onDestinationChanged(@NonNull NavController navController,
                                                     @NonNull NavDestination navDestination,
                                                     @Nullable Bundle bundle) {
                        // Se obtiene el id del fragmento actual
                        isMain = (R.id.ContentMainFragment == navDestination.getId());
                        // obtiene el action bar de la actividad
                        ActionBar actionBar = getSupportActionBar();
                        // Invalida el menu para que se actualice
                        actionBar.invalidateOptionsMenu();
                        // Si el fragmento actual no es el principal, se muestra el boton de regreso
                        actionBar.setDisplayHomeAsUpEnabled(!isMain);
                        // Si el fragmento actual es el principal, se cambia el titulo del action bar
                        if (isForward) {
                            actionBar.setTitle(tittle);
                        } else {
                            // si no, se elimina el titulo del action bar y se agrega el titulo del fragmento anterior
                            if (isMain) {
                                prevTitles.clear();
                                prevTitles.add("Experimento de muestreo");
                            }
                            actionBar.setTitle(prevTitles.get(prevTitles.size() - 1));
                            prevTitles.remove(prevTitles.size() - 1);
                        }
                    }
                }
        );

    }

    // metodo para obtener el fragmento actual
    @NonNull
    @Override
    public FragmentManager getSupportFragmentManager() {
        return super.getSupportFragmentManager();
    }

    // metodo para obtener el directorio de almacenamiento externo
    public File getExternalStorage(){
        return Environment.getExternalStorageDirectory();
    }

    // metodo para modificar la interfaz de usuario desde un hilo secundario 
    public void onUIThread(Runnable toRun){
        runOnUiThread(toRun);
    }

    // metodo para navegar a un fragmento
    // @param toID: id del fragmento al que se quiere navegar
    // @param bundle: datos que se quieren pasar al fragmento
    // @param tittle: titulo del fragmento
    public void navigateTo(int toID, Bundle bundle, String tittle) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        prevTitles.add(this.tittle);
        this.tittle = tittle;
        isForward = true;
        navController.navigate(toID, bundle);
        isForward = false;
    }

    // metodo para navegar a un fragmento
    // @param toID: id del fragmento al que se quiere navegar
    // @param tittle: titulo del fragmento
    public void navigateTo(int toID, String tittle){
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        prevTitles.add(this.tittle);
        this.tittle = tittle;
        isForward = true;
        navController.navigate(toID);
        isForward = false;
    }

    // metodo para crear un snack bar
    // @param snackBarMsg: mensaje que se quiere mostrar en el snack bar
    public void makeSnackB(String snackBarMsg){
        onUIThread( ()-> Snackbar.make(binding.coordinatorLayoutMain, snackBarMsg, Snackbar.LENGTH_LONG).show());
    }

    // metodo para esperar una respuesta del dispositivo (esp32)
    // @param state: estado del layout de espera
    public void setOnWaitForResponse(int state){
        onUIThread( ()-> {
            binding.waitingAnswerLayout.setVisibility(state);
            ControlCenter.getInstance().mainContent.setViewPagTouch(state == View.GONE);
            binding.navHostFragmentContentMain.setEnabled(state == View.GONE);
        });
    }

    boolean isMain = true;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(isMain){
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    // modifica el comportamiento de los botones del action bar
    // @param item: boton del action bar que se presiono
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // maneja el item del action bar que se presiono
        // el action bar maneja automaticamente los botones de regreso
        // siempre y cuando se especifique una actividad padre en AndroidManifest.xml
        if(!isMain){ return false; }
        int id = item.getItemId();
        // realiza la accion correspondiente al boton presionado
        if (id == R.id.action_allDevices) {
            navigateTo(R.id.action_ContentMainFragment_to_dataSelectorFragment, "Dispositivos muestreados");
        }else if (id == R.id.action_about) {
            navigateTo(R.id.action_ContentMainFragment_to_aboutFragment,"Acerca de");
        }

        return super.onOptionsItemSelected(item);
    }

    // metodo para crear el menu de navegacion de la aplicacion
    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp()//navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}

