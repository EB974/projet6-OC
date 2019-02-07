package com.eric_b.go4lunch.controller.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.eric_b.go4lunch.Auth.LogInActivity;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.utils.SPAdapter;
import com.eric_b.go4lunch.api.CompagnyHelper;
import com.eric_b.go4lunch.controller.fragment.ListViewFragment;
import com.eric_b.go4lunch.controller.fragment.MapFragment;
import com.eric_b.go4lunch.controller.fragment.WorkmateFragment;
import com.eric_b.go4lunch.modele.Compagny;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.Objects;
import butterknife.BindView;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.EasyPermissions;

public class LunchActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, EasyPermissions.PermissionCallbacks {

    private final String TAG=this.getClass().getSimpleName() ;
    // DESIGN
    @BindView(R.id.lunch_activity_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.lunch_activity_drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.lunch_activity_viewpager)
    ViewPager mPager;
    @BindView(R.id.activity_lunch_tabs)
    TabLayout mTab;
    private TextView mUserNameTextView;
    private String mUserName;
    private String mUserPhoto;
    private Compagny mCurrentWorkmate;
    private boolean doubleBackToExitPressedOnce;
    private Handler mHandler = new Handler();

    // DATAS
    private static final String PERM_FINE = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String PERM_COARSE = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int RC_LOCATION_PERMS = 120;
    private static final int RC_ALL_PERMS = 130;
    private static final String PERM_TEL = Manifest.permission.CALL_PHONE;
    private static final int RC_CALL_PERMS = 110;
    static final int SETTING_RESULT = 1;
    private static final String RESTAURANT_ID = "placeId";
    private static final String USER_NAME = "UserName";
    private static final String USER_PHOTO = "UserPhoto";
    private static final int SIGN_OUT_TASK = 10;
    private static final int DELETE_USER_TASK = 20;
    private static final int UPDATE_USERNAME = 30;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 10;


    // COLORS ITEMS TABS
    String mUnselectedItemColor;
    String mSelectedItemColor;
    private String mUserRestaurant;
    private String mUserRestaurantName;
    private Location mLastLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lunch);
        ButterKnife.bind(this);
        final String uid = Objects.requireNonNull(this.getCurrentUser()).getUid();

        //get the id of the user
        CompagnyHelper.getWorkmate(uid).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                mCurrentWorkmate = documentSnapshot.toObject(Compagny.class);
                if(mCurrentWorkmate == null) startLoginActivity();
                assert mCurrentWorkmate != null;
                //get the user info
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

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        String provider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = locationManager.getLastKnownLocation(provider);

        configureToolBar();
        configureDrawerLayout();
        configureNavigationView();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

// check call and gps permissions
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
        inflater.inflate(R.menu.menu_activity_search, menu);
        menu.findItem(R.id.menu_cancel).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_search:
                searchScreen();
              break;
            case R.id.menu_cancel:
                refreshWorkmateFrag(null);
              break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    public void searchScreen(){
        LatLngBounds latLngBounds = new LatLngBounds(
                new LatLng(mLastLocation.getLatitude()-.01, mLastLocation.getLongitude()-.01),
                new LatLng(mLastLocation.getLatitude()+.01, mLastLocation.getLongitude()+.01));
        try {
            Intent intent =
                    new PlaceAutocomplete
                            .IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .setBoundsBias(latLngBounds)
                            .build(LunchActivity.this);

            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            Log.i(TAG, "Google PlayServices Repairable Exception "+e);
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.i(TAG, "Google PlayServices NotAvailable Exception "+e);
        }
    }


    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
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
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.lunch_title);
    }




    // Configure Drawer Layout
    private void configureDrawerLayout() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    // Configure NavigationView
    public void configureNavigationView() {
        NavigationView navigationView = findViewById(R.id.lunch_activity_nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View header=navigationView.getHeaderView(0);
        TextView userMail = header.findViewById(R.id.user_email);
        mUserNameTextView = header.findViewById(R.id.user_name);
        ImageView userPhotoImageView = header.findViewById(R.id.user_photo);
        userMail.setText(Objects.requireNonNull(this.getCurrentUser()).getEmail());

                mUserNameTextView.setText(mUserName);
                try {
                    Glide.with(LunchActivity.this)
                            .load(mUserPhoto)
                            .apply(RequestOptions.circleCropTransform())
                            .into(userPhotoImageView);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
    }


    private void configureViewPagerAndTabs() {
        // Set Adapter PageAdapter and glue it together
        mPager.setAdapter(new PageAdapter(getSupportFragmentManager()));
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

        mUnselectedItemColor = getResources().getString(R.string.unSelectedItemColor);
        mSelectedItemColor = getResources().getString(R.string.selectedItemColor);

        //setup the color of tab icon while booting
        Objects.requireNonNull(mTab.getTabAt(0)).setIcon(R.drawable.baseline_map_white);
        Objects.requireNonNull(mTab.getTabAt(1)).setIcon(R.drawable.baseline_view_list_white);
        Objects.requireNonNull(mTab.getTabAt(2)).setIcon(R.drawable.baseline_people_white);

        Objects.requireNonNull(Objects.requireNonNull(mTab.getTabAt(0)).getIcon()).setColorFilter(Color.parseColor(mSelectedItemColor), PorterDuff.Mode.SRC_IN);
        Objects.requireNonNull(Objects.requireNonNull(mTab.getTabAt(1)).getIcon()).setColorFilter(Color.parseColor(mUnselectedItemColor), PorterDuff.Mode.SRC_IN);
        Objects.requireNonNull(Objects.requireNonNull(mTab.getTabAt(2)).getIcon()).setColorFilter(Color.parseColor(mUnselectedItemColor), PorterDuff.Mode.SRC_IN);

        mTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            //set orange color icon when the tab is selected
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                assert tab.getIcon() != null;
                tab.getIcon().setColorFilter(Color.parseColor(mSelectedItemColor), PorterDuff.Mode.SRC_IN);

            }

            //set black color icon when the tab is selected
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                assert tab.getIcon() != null;
                tab.getIcon().setColorFilter(Color.parseColor(mUnselectedItemColor), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.activity_display_restaurant :
                CompagnyHelper.getWorkmate(Objects.requireNonNull(this.getCurrentUser()).getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Compagny currentWorkmate = documentSnapshot.toObject(Compagny.class);
                        assert currentWorkmate != null;
                        mUserNameTextView.setText(currentWorkmate.getUserName());
                        startRestaurantDisplayActivity(currentWorkmate.getReservedRestaurant(), currentWorkmate.getUserName(),currentWorkmate.getUserPhoto());
                    }});
                break;
            case R.id.activity_settings:
                startSettingActivity();
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
                .addOnSuccessListener(this, this.updateUIAfterRESTRequestsCompleted());
    }

    private OnSuccessListener<Void> updateUIAfterRESTRequestsCompleted(){
        return new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                switch (LunchActivity.SIGN_OUT_TASK){
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
        startActivityForResult(intent,SETTING_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Check which request we're responding to

        //responding to Setting activity
        if (requestCode == SETTING_RESULT){
            if(resultCode == RESULT_OK) {
                refreshListViewFrag(null); //refresh listViewFragment
            }
        }

        // responding to place autocomplete search
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                Place place = PlaceAutocomplete.getPlace(this, data);
               switch (mPager.getCurrentItem()){
                   case 0 : refreshMapFrag(place.getLatLng());
                    break;
                   case 1 : refreshListViewFrag(place.getId());
                    break;
                   case 2 : refreshWorkmateFrag((String) place.getName());
                    break;
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.i(TAG, status.getStatusMessage());
            }
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
        if (requestCode >= 120) {
            configureViewPagerAndTabs();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
    }

    private void refreshListViewFrag(String id){
        // Create new fragment and transaction
        Fragment newFragment =ListViewFragment.newInstance(id);
        FrameLayout fl = findViewById(R.id.listViewFragment);
        fl.removeAllViews();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.listViewFragment, newFragment,"list").commitAllowingStateLoss();
    }

    private void refreshMapFrag(LatLng latLng){
        // Create new fragment and transaction
        Fragment newFragment = MapFragment.newInstance(latLng);
        RelativeLayout fl = findViewById(R.id.mapFragment);
        fl.removeAllViews();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapFragment, newFragment,"map").commitAllowingStateLoss();
    }

    private void refreshWorkmateFrag(String name){
        // Create new fragment and transaction
        Fragment newFragment = WorkmateFragment.newInstance(name);
        FrameLayout fl = findViewById(R.id.workmateFragment);
        fl.removeAllViews();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.workmateFragment, newFragment,"workmate").commitAllowingStateLoss();
    }

}
