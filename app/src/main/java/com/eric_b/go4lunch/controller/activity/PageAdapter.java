package com.eric_b.go4lunch.controller.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.eric_b.go4lunch.controller.fragment.ListViewFragment;
import com.eric_b.go4lunch.controller.fragment.MapFragment;
import com.eric_b.go4lunch.controller.fragment.WorkmateFragment;

public class PageAdapter extends FragmentStatePagerAdapter {

    PageAdapter(FragmentManager mgr) {
        super(mgr);
    }

    @Override
    public int getCount() {
        return(3);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0: //Page number 1
                return MapFragment.newInstance(null);
            case 1: //Page number 2
                return ListViewFragment.newInstance(null);
                //return ListViewFragment.newInstance(null);
            case 2: //Page number 3
                return WorkmateFragment.newInstance(null);
            default:
                return null;
        }

    }

    @Override
    public CharSequence getPageTitle(int position) {
        //title of tabs
        String[]  namePage = {"Map View","List View","Workmates"};
        return namePage[position];
    }
}
