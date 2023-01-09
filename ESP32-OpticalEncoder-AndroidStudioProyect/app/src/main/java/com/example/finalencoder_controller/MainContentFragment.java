package com.example.finalencoder_controller;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.finalencoder_controller.databinding.ContentMainBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainContentFragment extends Fragment {

    // The binding object will be used to access UI elements in the layout.
    private ContentMainBinding binding;
    // The view pager that will be used to change between fragments
    ViewPager2 mainViewPag;

    
    // 1. Inflate the layout for this fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = ContentMainBinding.inflate(inflater, container, false);

        // 2. Get the instance of the Control Center class and set the main content property to this class
        ControlCenter.getInstance().mainContent = this;
        TabLayout tabLayout = binding.mainTabLayout;
        mainViewPag = binding.mainViewPager;

        // 3. Set the adapter of the mainViewPager and disable the user input to avoid the swipe action
        mainViewPag.setAdapter(new FragmentAdapter(ControlCenter.getInstance().mainActivity));
        mainViewPag.setUserInputEnabled(false);

        // 4. Create a tab for each fragment and set the title of each tab with the corresponding fragment name
        new TabLayoutMediator(tabLayout, mainViewPag, (tab, position) -> {
            tab.setText((position == 0 ? "General" : position == 1 ? "Agendar" : "Conexion"));
        }).attach();

        return binding.getRoot();
    }




    // Class that will be used to create the fragments
    class FragmentAdapter extends FragmentStateAdapter {

        public FragmentAdapter(@NonNull FragmentActivity fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Return the fragment that will be used in the corresponding position
            switch(position){
                case 0 :
                    // If the general fragment is null, create a new instance of the general fragment and return it 
                    if(ControlCenter.getInstance().generalFrag == null){ ControlCenter.getInstance().generalFrag = new GeneralFragment(); }
                    return ControlCenter.getInstance().generalFrag;

                case 1 :
                    // If the scheduler fragment is null, create a new instance of the scheduler fragment and return it
                    if(ControlCenter.getInstance().schedulerFrag == null){ ControlCenter.getInstance().schedulerFrag = new SchedulerFragment(); }
                    return ControlCenter.getInstance().schedulerFrag;

                case 2 :
                    // If the connection fragment is null, create a new instance of the connection fragment and return it
                    if(ControlCenter.getInstance().connectionFrag == null){ ControlCenter.getInstance().connectionFrag = new ConnectionFragment(); }
                    return ControlCenter.getInstance().connectionFrag;

                default:
                    // If the general fragment is null, create a new instance of the general fragment and return it
                    if(ControlCenter.getInstance().generalFrag == null){ ControlCenter.getInstance().generalFrag = new GeneralFragment(); }
                    return ControlCenter.getInstance().generalFrag;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
    // Method that will be used to change the current tab
    public  void navigateViewPag(int tab){
        mainViewPag.setCurrentItem(tab);
    }
    // Method that will be used to enable or disable the swipe action
    public void setViewPagTouch(boolean state){
        mainViewPag.setEnabled(state);
    }
    // on view created method
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    // on destroy view method to avoid memory leaks
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
