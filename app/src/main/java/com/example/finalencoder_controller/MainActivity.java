package com.example.finalencoder_controller;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.example.finalencoder_controller.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.service.controls.Control;
import android.view.View;

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

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ControlCenter.getInstance().mainActivity = this;
        tittle = "Experimento de muestreo";

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();

        navController.addOnDestinationChangedListener(
                new NavController.OnDestinationChangedListener() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                        isMain = (R.id.ContentMainFragment == navDestination.getId());
                        ActionBar actionBar = getSupportActionBar();
                        actionBar.invalidateOptionsMenu();
                        actionBar.setDisplayHomeAsUpEnabled(!isMain);
                        if(isForward){
                            actionBar.setTitle(tittle);
                        }else{
                            if(isMain){
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

    @NonNull
    @Override
    public FragmentManager getSupportFragmentManager() {
        return super.getSupportFragmentManager();
    }

    public void onUIThread(Runnable toRun){
        runOnUiThread(toRun);
    }

    public void navigateTo(int toID, Bundle bundle, String tittle) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        prevTitles.add(this.tittle);
        this.tittle = tittle;

        isForward = true;
        navController.navigate(toID, bundle);
        isForward = false;
    }

    public void navigateTo(int toID, String tittle){

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        prevTitles.add(this.tittle);
        this.tittle = tittle;

        isForward = true;
        navController.navigate(toID);
        isForward = false;

    }

    public void makeSnackB(String snackBarMsg){
        onUIThread( ()-> Snackbar.make(binding.coordinatorLayoutMain, snackBarMsg, Snackbar.LENGTH_LONG).show());
    }

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

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp()//navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}

