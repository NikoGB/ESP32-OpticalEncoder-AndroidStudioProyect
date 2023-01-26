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

    
    // 1. el metodo onCreateView se ejecuta cuando se crea la vista
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        binding = ContentMainBinding.inflate(inflater, container, false);

        // 2. el metodo mainContent de la clase ControlCenter se usa para acceder a la vista desde otras clases
        ControlCenter.getInstance().mainContent = this;
        TabLayout tabLayout = binding.mainTabLayout;
        mainViewPag = binding.mainViewPager;

        // 3. se crea el adaptador que se encargara de crear los fragmentos y se le asigna al view pager
        mainViewPag.setAdapter(new FragmentAdapter(ControlCenter.getInstance().mainActivity));
        mainViewPag.setUserInputEnabled(false);

        // 4. se crea el tab layout y se le asigna al view pager
        new TabLayoutMediator(tabLayout, mainViewPag, (tab, position) -> {
            tab.setText((position == 0 ? "General" : position == 1 ? "Agendar" : "Conexion"));
        }).attach();

        return binding.getRoot();
    }




    // clase que se encarga de crear los fragmentos
    class FragmentAdapter extends FragmentStateAdapter {

        public FragmentAdapter(@NonNull FragmentActivity fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // dependiendo de la posicion se crea un fragmento u otro
            switch(position){
                case 0 :
                    if(ControlCenter.getInstance().generalFrag == null){ ControlCenter.getInstance().generalFrag = new GeneralFragment(); }
                    return ControlCenter.getInstance().generalFrag;

                case 1 :
                    if(ControlCenter.getInstance().schedulerFrag == null){ ControlCenter.getInstance().schedulerFrag = new SchedulerFragment(); }
                    return ControlCenter.getInstance().schedulerFrag;

                case 2 :
                    if(ControlCenter.getInstance().connectionFrag == null){ ControlCenter.getInstance().connectionFrag = new ConnectionFragment(); }
                    return ControlCenter.getInstance().connectionFrag;

                default:
                    if(ControlCenter.getInstance().generalFrag == null){ ControlCenter.getInstance().generalFrag = new GeneralFragment(); }
                    return ControlCenter.getInstance().generalFrag;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    public  void navigateViewPag(int tab){
        mainViewPag.setCurrentItem(tab);
    }

    public void setViewPagTouch(boolean state){
        mainViewPag.setEnabled(state);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
