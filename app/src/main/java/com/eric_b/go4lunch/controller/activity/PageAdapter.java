package com.eric_b.go4lunch.controller.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.eric_b.go4lunch.controller.fragment.ListViewFragment;
import com.eric_b.go4lunch.controller.fragment.MapFragment;
import com.eric_b.go4lunch.controller.fragment.WorkmateFragment;

public class PageAdapter extends FragmentPagerAdapter{

    private String userName;
    private String userPhoto;

    public PageAdapter(FragmentManager mgr, String userName,String userPhoto) {
        super(mgr);
        this.userName = userName;
        this.userPhoto = userPhoto;
    }

    @Override
    public int getCount() {
        return(3);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0: //Page number 1
                return MapFragment.newInstance(userName, userPhoto);
            case 1: //Page number 2
                return new ListViewFragment();
            case 2: //Page number 3
                return new WorkmateFragment();
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

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserPhoto(String userPhoto) {
        this.userPhoto = userPhoto;
    }
}
