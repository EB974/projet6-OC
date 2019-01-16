package com.eric_b.go4lunch.controller.activity;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;

import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.eric_b.go4lunch.LogInActivity;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.SPAdapter;
import com.eric_b.go4lunch.api.CompagnyHelper;
import com.eric_b.go4lunch.api.GetUserInfo;
import com.eric_b.go4lunch.controller.fragment.ListViewFragment;
import com.eric_b.go4lunch.modele.Compagny;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.EasyPermissions;

import static com.google.android.gms.common.api.internal.LifecycleCallback.getFragment;

public class LunchActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, EasyPermissions.PermissionCallbacks {

    private static final java.lang.Object Object = 12 ;
    // DESIGN
    @BindView(R.id.lunch_activity_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.lunch_activity_drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.lunch_activity_viewpager)
    ViewPager mPager;
    @BindView(R.id.activity_lunch_tabs)
    TabLayout mTab;
    private ActionBar mActionBar;
    private ActionBarDrawerToggle mToggle;
    private EditText mSearchEditText;
    View mView;
    private TextView mUserMail;
    private TextView mUserNameTextView;
    private ImageView mUserPhotoImageView;
    private String mUserName;
    private String mUserPhoto;
    private Compagny mCurrentWorkmate;
    private boolean doubleBackToExitPressedOnce;
    private Handler mHandler = new Handler();


    // FRAGMENTS
    private Fragment fragmentMap;
    private Fragment fragmentListView;
    private Fragment fragmentWorkmates;

    // DATAS
    private static final String PERM_FINE = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String PERM_COARSE = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int RC_LOCATION_PERMS = 120;
    private static final int RC_ALL_PERMS = 130;
    private static final String PERM_TEL = Manifest.permission.CALL_PHONE;
    private static final int RC_CALL_PERMS = 110;
    private static final int FRAGMENT_MAP = 0;
    //private static final int FRAGMENT_LISTVIEW = 1;
    private static final int FRAGMENT_WORKMATE = 2;
    public static final String FRAGMENT_LISTVIEW = "ListViewFragment";
    static final int SETTING_RESULT = 1;
    public static final String BUNDLE_EXTRA ="EXTRA";
    private static final String RESTAURANT_ID = "placeId";
    private static final String USER_NAME = "UserName";
    private static final String USER_PHOTO = "UserPhoto";
    private static final int SIGN_OUT_TASK = 10;
    private static final int DELETE_USER_TASK = 20;
    private static final int UPDATE_USERNAME = 30;

    // COLORS ITEMS TABS
    String mUnselectedItemColor;
    String mSelectedItemColor;
    private String mUserRestaurant;
    private String mUserRestaurantName;
    private java.lang.Object ListViewFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lunch);
        ButterKnife.bind(this);
        final String uid = this.getCurrentUser().getUid();

        CompagnyHelper.getWorkmate(uid).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                mCurrentWorkmate = documentSnapshot.toObject(Compagny.class);
                if(mCurrentWorkmate == null) startLoginActivity();
                assert mCurrentWorkmate != null;
                try {
                    mUserName = mCurrentWorkmate.getUserName();
                    mUserPhoto = mCurrentWorkmate.getUserPhoto();
                    mUserRestaurant = mCurrentWorkmate.getReservedRestaurant();
                    mUserRestaurantName = mCurrentWorkmate.getReservedRestaurantName();
                }catch(Throwable e) {
                    e.printStackTrace();
                    startLoginActivity();
                }
                SPAdapter spAdapter = new  SPAdapter(getApplicationContext());
                spAdapter.setUserId(uid);
                spAdapter.setUserName(mUserName);
                spAdapter.setUserPhoto(mUserPhoto);
                spAdapter.setRestaurantReserved(mUserRestaurant);
                spAdapter.setRestaurantNameReserved(mUserRestaurantName);
                checkPermissions();
            }

        });

        configureToolBar();
        configureDrawerLayout();
        configureNavigationView();
    }

    private void checkPermissions() {
        String[] locationPerms = {PERM_FINE, PERM_COARSE};
        String callPerms = PERM_TEL;
        String[] allPerms = {PERM_FINE, PERM_COARSE,PERM_TEL};

        if (!EasyPermissions.hasPermissions(this, locationPerms) && !EasyPermissions.hasPermissions(this, callPerms)) {
            EasyPermissions.requestPermissions(this, getResources().getString(R.string.all_perm_request), RC_ALL_PERMS, allPerms);
        } else if (!EasyPermissions.hasPermissions(this, locationPerms)) {
            EasyPermissions.requestPermissions(this, getResources().getString(R.string.localisation_request), RC_LOCATION_PERMS, locationPerms);
        } else if (!EasyPermissions.hasPermissions(this, callPerms)) {
            EasyPermissions.requestPermissions(this, getResources().getString(R.string.call_request), RC_CALL_PERMS, callPerms);
        }

        if (EasyPermissions.hasPermissions(this,locationPerms)){
            configureViewPagerAndTabs();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Inflate menu to add items to action bar if it is present.
        inflater.inflate(R.menu.menu_activity_lunch, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_activity_lunch_search).getActionView();
        assert searchManager != null;
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
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getResources().getText(R.string.pressBack), Toast.LENGTH_SHORT).show();
        mHandler.postDelayed(mRunnable, 2000);
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doubleBackToExitPressedOnce = false;
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mHandler != null) { mHandler.removeCallbacks(mRunnable); }
    }

    // ---------------------
    // CONFIGURATION
    // ---------------------

    // Configure Toolbar
    private void configureToolBar() {
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        //mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        assert mActionBar != null;
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
        NavigationView navigationView = findViewById(R.id.lunch_activity_nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View header=navigationView.getHeaderView(0);
        mUserMail =  header.findViewById(R.id.user_email);
        mUserNameTextView = header.findViewById(R.id.user_name);
        mUserPhotoImageView = header.findViewById(R.id.user_photo);
        mUserMail.setText(Objects.requireNonNull(this.getCurrentUser()).getEmail());

                mUserNameTextView.setText(mUserName);
                try {
                    Glide.with(LunchActivity.this)
                            .load(mUserPhoto)
                            .apply(RequestOptions.circleCropTransform())
                            .into(mUserPhotoImageView);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
    }


    private void configureViewPagerAndTabs() {
        // Set Adapter PageAdapter and glue it together
        mPager.setAdapter(new PageAdapter(getSupportFragmentManager(),mUserName, mUserPhoto,mUserRestaurant));
        // Glue TabLayout and ViewPager together
        mTab.setupWithViewPager(mPager);
        // Design purpose. Tabs have the same width
        mTab.setTabMode(TabLayout.MODE_FIXED);
        setupTabIcons();
        TabLayout.Tab tab = mTab.getTabAt(0);

        assert tab != null;
        tab.select();

    }


    private void setupTabIcons() {
        Objects.requireNonNull(mTab.getTabAt(0)).setIcon(R.drawable.baseline_map_white);
        Objects.requireNonNull(mTab.getTabAt(1)).setIcon(R.drawable.baseline_view_list_white);
        Objects.requireNonNull(mTab.getTabAt(2)).setIcon(R.drawable.baseline_people_white);
        mUnselectedItemColor = getResources().getString(R.string.unSelectedItemColor);
        mSelectedItemColor = getResources().getString(R.string.selectedItemColor);
        Objects.requireNonNull(Objects.requireNonNull(mTab.getTabAt(0)).getIcon()).setColorFilter(Color.parseColor(mSelectedItemColor), PorterDuff.Mode.SRC_IN);
        Objects.requireNonNull(Objects.requireNonNull(mTab.getTabAt(1)).getIcon()).setColorFilter(Color.parseColor(mUnselectedItemColor), PorterDuff.Mode.SRC_IN);
        Objects.requireNonNull(Objects.requireNonNull(mTab.getTabAt(2)).getIcon()).setColorFilter(Color.parseColor(mUnselectedItemColor), PorterDuff.Mode.SRC_IN);

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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        GetUserInfo getUserInfo = new GetUserInfo(this.getCurrentUser().getUid());
        switch (item.getItemId()){
            case R.id.activity_display_restaurant :
                CompagnyHelper.getWorkmate(this.getCurrentUser().getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Compagny currentWorkmate = documentSnapshot.toObject(Compagny.class);
                        assert currentWorkmate != null;
                        mUserNameTextView.setText(currentWorkmate.getUserName());
                        if (currentWorkmate.getReservedRestaurant()!=null || currentWorkmate.getReservedRestaurant()!=""){
                            startRestaurantDisplayActivity(currentWorkmate.getReservedRestaurant(), currentWorkmate.getUserName(),currentWorkmate.getUserPhoto());
                        }
                        else
                            Toast.makeText(LunchActivity.this, getResources().getText(R.string.no_restaurant),Toast.LENGTH_LONG).show();
                    }});
                break;
            case R.id.activity_settings:
                startSettingActivity();
                ///////////////////////////
                // Reload current fragment


                ///////////////////////////
                break;
            case R.id.logout:
                logout();
                break;
            default:
                break;
        }
        this.mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnSuccessListener(this, this.updateUIAfterRESTRequestsCompleted(SIGN_OUT_TASK));
    }

    private OnSuccessListener<Void> updateUIAfterRESTRequestsCompleted(final int origin){
        return new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                switch (origin){
                    case UPDATE_USERNAME:
                        //progressBar.setVisibility(View.INVISIBLE);
                        break;
                    case SIGN_OUT_TASK:
                        Toast.makeText(LunchActivity.this, getResources().getText(R.string.disconnected),Toast.LENGTH_LONG).show();
                        finish();
                        startLoginActivity();
                        break;
                    case DELETE_USER_TASK:
                        finish();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void startRestaurantDisplayActivity(String id, String userName, String userPhoto){
        Intent intent = new Intent(this, RestaurantDisplay.class);
        intent.putExtra(RESTAURANT_ID,id);
        intent.putExtra(USER_NAME,userName);
        intent.putExtra(USER_PHOTO,userPhoto);
        startActivity(intent);
    }

    private void startLoginActivity(){
        Intent intent = new Intent(this, LogInActivity.class);
        startActivity(intent);
    }

    private void startSettingActivity(){
        Intent intent = new Intent(this, SettingActivity.class);
        //startActivity(intent);
        startActivityForResult(intent,SETTING_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SETTING_RESULT && RESULT_OK == resultCode) {
            refreshlistViewFrag(); //refresh listViewFragment
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, perms, grantResults, this);

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode >= 120) configureViewPagerAndTabs();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
    }

    private void refreshlistViewFrag(){
        // Create new fragment and transaction
        Fragment newFragment = new ListViewFragment();
        FrameLayout fl = findViewById(R.id.listViewFragment);
        fl.removeAllViews();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.listViewFragment, newFragment,"list").commitAllowingStateLoss();
    }

}
