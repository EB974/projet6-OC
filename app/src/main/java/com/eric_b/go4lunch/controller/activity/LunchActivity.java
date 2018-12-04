package com.eric_b.go4lunch.controller.activity;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Constraints;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;

import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.controller.activity.PageAdapter;
import com.eric_b.go4lunch.controller.fragment.MapFragment;
import com.google.common.io.Resources;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LunchActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // DESIGN
    @BindView(R.id.lunch_activity_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.lunch_activity_drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.lunch_activity_nav_view)
    NavigationView mNavigationView;
    @BindView(R.id.lunch_activity_viewpager)
    ViewPager mPager;
    @BindView(R.id.activity_lunch_tabs)
    TabLayout mTab;
    ActionBar mActionBar;
    ActionBarDrawerToggle mToggle;
    EditText mSearchEditText;
    View mView;

    // FRAGMENTS
    private Fragment fragmentMap;
    private Fragment fragmentListView;
    private Fragment fragmentWorkmates;

    // DATAS
    private static final int FRAGMENT_MAP = 0;
    private static final int FRAGMENT_LISTVIEW = 1;
    private static final int FRAGMENT_WORKMATE = 2;

    // COLORS ITEMS TABS
    String mUnselectedItemColor;
    String mSelectedItemColor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lunch);
        ButterKnife.bind(this);
        configureToolBar();

        configureDrawerLayout();
        configureViewPagerAndTabs(0);
        configureNavigationView();

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Inflate menu to add items to action bar if it is present.
        inflater.inflate(R.menu.menu_activity_lunch, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_activity_lunch_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        //searchView.setMaxWidth(mActionBar.getHeight());

        mSearchEditText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            /**
             * Called when the focus state of a view has changed.
             *
             * @param v        The view whose state has changed.
             * @param hasFocus The new focus state of v.
             */
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mToggle.setDrawerIndicatorEnabled(false);
                    mActionBar.setTitle(null);

                    // Set searchbox text and background
                    mSearchEditText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.searchTextColor));
                    mSearchEditText.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.hintSearchTextColor));
                    searchView.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.search_edittext_background));
                    searchView.setX(-10);
                    searchView.setMaxWidth(1000);
                    searchView.setScaleY(0.9f);
                    searchView.setScaleX(0.97f);
                    //searchView.setLayoutParams(new ActionBar.LayoutParams(Gravity.CENTER));
                } else {
                    // Set searchbox text and background
                    mToggle.setDrawerIndicatorEnabled(true);
                    mActionBar.setTitle(R.string.lunch_title);
                    mSearchEditText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.textColor));
                    searchView.setIconified(true);
                    searchView.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.color.colorPrimary));

                }
            }
        });
        return true;
    }

    @Override
    public void onBackPressed() {
        // 5 - Handle back click to close menu
        if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ---------------------
    // CONFIGURATION
    // ---------------------

    // Configure Toolbar
    private void configureToolBar() {
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        //mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        mActionBar.setTitle(R.string.lunch_title);
    }

    // Configure Drawer Layout
    private void configureDrawerLayout() {
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
    }

    // Configure NavigationView
    public void configureNavigationView() {
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    private void configureViewPagerAndTabs(int setTabs) {

        // Set Adapter PageAdapter and glue it together
        mPager.setAdapter(new PageAdapter(getSupportFragmentManager()));

        // Glue TabLayout and ViewPager together
        mTab.setupWithViewPager(mPager);
        // Design purpose. Tabs have the same width
        mTab.setTabMode(TabLayout.MODE_FIXED);
        setupTabIcons();
        TabLayout.Tab tab = mTab.getTabAt(setTabs);

        assert tab != null;
        tab.select();
    }


    private void setupTabIcons() {
        mTab.getTabAt(0).setIcon(R.drawable.baseline_map_white);
        mTab.getTabAt(1).setIcon(R.drawable.baseline_view_list_white);
        mTab.getTabAt(2).setIcon(R.drawable.baseline_people_white);
        mUnselectedItemColor = getResources().getString(R.string.unSelectedItemColor);
        mSelectedItemColor = getResources().getString(R.string.selectedItemColor);
        mTab.getTabAt(0).getIcon().setColorFilter(Color.parseColor(mSelectedItemColor), PorterDuff.Mode.SRC_IN);
        mTab.getTabAt(1).getIcon().setColorFilter(Color.parseColor(mUnselectedItemColor), PorterDuff.Mode.SRC_IN);
        mTab.getTabAt(2).getIcon().setColorFilter(Color.parseColor(mUnselectedItemColor), PorterDuff.Mode.SRC_IN);

        mTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(Color.parseColor(mSelectedItemColor), PorterDuff.Mode.SRC_IN);

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(Color.parseColor(mUnselectedItemColor), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }


    /**
     * Called when an item in the navigation menu is selected.
     *
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }


}
