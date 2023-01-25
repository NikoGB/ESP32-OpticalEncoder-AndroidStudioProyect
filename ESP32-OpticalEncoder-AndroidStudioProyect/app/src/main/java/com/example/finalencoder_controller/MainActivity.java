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
import android.service.controls.Control;
import android.view.View;

import androidx.core.app.ActivityCompat;
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
        // The binding object will be used to access UI elements in the layout.
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // Set the content view to the layout defined in the binding object.
        setContentView(binding.getRoot());
        // set the toolbar as the app bar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        // add the main activity to the control center
        ControlCenter.getInstance().mainActivity = this;
        // set the title of the toolbar
        tittle = "Experimento de muestreo";
        // ocultamos la action bar para tener mas espacio en pantalla
        //actionBar.hide();

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

        // Add an OnDestinationChangedListener to the NavController to keep track of the current destination
        // and to update the action bar.
        navController.addOnDestinationChangedListener(
                new NavController.OnDestinationChangedListener() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onDestinationChanged(@NonNull NavController navController,
                                                     @NonNull NavDestination navDestination,
                                                     @Nullable Bundle bundle) {
                        // Keep track of the current destination and update the action bar.
                        isMain = (R.id.ContentMainFragment == navDestination.getId());
                        // Get the Action Bar for the activity
                        ActionBar actionBar = getSupportActionBar();
                        // Invalidate the menu to force a redraw
                        actionBar.invalidateOptionsMenu();
                        // Set the display options to show the back button if we are not in the main activity
                        actionBar.setDisplayHomeAsUpEnabled(!isMain);
                        // If the user is navigating forward, add the title to the stack.
                        if (isForward) {
                            actionBar.setTitle(tittle);
                        } else {
                            // If the user is navigating backward, update the action bar title and remove the
                            // previous title from the stack.
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

    // Returns the FragmentManager for interacting with fragments associated with this activity.
    // This is the correct way to interact with fragments when using the support library.
    // @return FragmentManager
    @NonNull
    @Override
    public FragmentManager getSupportFragmentManager() {
        return super.getSupportFragmentManager();
    }

    public File getExternalStorage(){
        return Environment.getExternalStorageDirectory();
    }
    // method to run a runnable on the UI thread
    public void onUIThread(Runnable toRun){
        runOnUiThread(toRun);
    }

    // method to navigate to a new fragment
    public void navigateTo(int toID, Bundle bundle, String tittle) {
        // get the navHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        // get the NavController
        NavController navController = navHostFragment.getNavController();
        // add the current tittle to the back stack
        prevTitles.add(this.tittle);
        // update the current tittle to the new tittle
        this.tittle = tittle;
        // set the isForward flag to true
        isForward = true;
        // navigate to the new fragment
        navController.navigate(toID, bundle);
        // set the isForward flag to false
        isForward = false;
    }

    public void navigateTo(int toID, String tittle){
        // get the navHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        // get the NavController
        NavController navController = navHostFragment.getNavController();
        // add the current tittle to the back stack
        prevTitles.add(this.tittle);
        // update the current tittle to the new tittle
        this.tittle = tittle;

        // set the isForward flag to true
        isForward = true;
        // navigate to the new fragment
        navController.navigate(toID);
        // set the isForward flag to false
        isForward = false;
    }

    // method to create a snackbar with a message
    public void makeSnackB(String snackBarMsg){
        onUIThread( ()-> Snackbar.make(binding.coordinatorLayoutMain, snackBarMsg, Snackbar.LENGTH_LONG).show());
    }

    // TODO: check if the documentation is correct
    // method to wait for a response from the device
    public void setOnWaitForResponse(int state){
        onUIThread( ()-> {
            // set the visibility of the waiting answer layout
            binding.waitingAnswerLayout.setVisibility(state);
            // set the visibility of the main content
            ControlCenter.getInstance().mainContent.setViewPagTouch(state == View.GONE);
            // set the enabled state of the navHostFragment
            binding.navHostFragmentContentMain.setEnabled(state == View.GONE);

        });
    }

    boolean isMain = true;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if(!isMain){ return false; }

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }else if (id == R.id.action_allDevices) {
            navigateTo(R.id.action_ContentMainFragment_to_dataSelectorFragment, "Dispositivos muestreados");
        }else if (id == R.id.action_about) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // This code is used to create the navigation bar at the top of the screen
    // The code is taken from the android studio tutorial and has been modified
    // to fit this project
    // The code is used to navigate between the different fragments in the project
    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp()//navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}

