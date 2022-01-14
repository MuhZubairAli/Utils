package pk.gov.pbs.utils;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import pk.gov.pbs.utils.exceptions.InvalidIndexException;
import pk.gov.pbs.utils.location.ILocationChangeCallback;
import pk.gov.pbs.utils.location.LocationService;

public abstract class CustomActivity extends AppCompatActivity {
    private static final String TAG = "[:Utils] CustomActivity";
    private static final int mSystemControlsHideFlags =
            View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    private LayoutInflater mLayoutInflater;
    private static final int PERMISSIONS_REQUEST_CODE_LOCATION = 100;
    private boolean IS_LOCATION_SERVICE_BOUND = false;
    private boolean USING_LOCATION_SERVICE = false;
    private ActionBar actionBar;
    private AlertDialog dialogLocationSettings;

    private Runnable mAfterLocationServiceStartCallback;
    private Intent locationServiceIntent = null;
    private LocationService mLocationService = null;
    private ServiceConnection mLocationServiceConnection = null;
    private BroadcastReceiver GPS_PROVIDER_ACCESS = null;
    private static byte mLocationAttachAttempts = 0;

    private HashMap<String, String[]> mPermissions;
    protected UXToolkit mUXToolkit;
    private static final String permissionKeyLocation = "location";
    private static final String permissionKeyStorage = "storage";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (USING_LOCATION_SERVICE) {
            if (mLocationService != null) {
                if (!mLocationService.isNetworkEnabled() && !mLocationService.isGPSEnabled())
                    showAlertLocationSettings();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (USING_LOCATION_SERVICE)
            stopLocationService();
    }

    private void initialize(){
        mUXToolkit = new UXToolkit(this);

        mPermissions = new HashMap<>();
        mPermissions.put(permissionKeyLocation, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION });
        mPermissions.put(permissionKeyStorage, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE });

        GPS_PROVIDER_ACCESS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showAlertLocationSettings();
            }
        };

        mLocationServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationService.LocationServiceBinder binder = (LocationService.LocationServiceBinder) service;
                mLocationService = binder.getService();

                if (!mLocationService.isNetworkEnabled() && !mLocationService.isGPSEnabled())
                    showAlertLocationSettings();

                if (mAfterLocationServiceStartCallback != null)
                    mAfterLocationServiceStartCallback.run();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mLocationService = null;
                USING_LOCATION_SERVICE = false;
            }
        };

        checkAllPermissions();
    }

    protected void showActionBar(){
        try {
            actionBar = getSupportActionBar();
            if (actionBar != null) {
                if (!actionBar.isShowing())
                    actionBar.show();
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                actionBar.setCustomView(R.layout.custom_action_bar);
            }
        } catch (NullPointerException npe) {
            ExceptionReporter.printStackTrace(npe);
        }
    }

    protected void showSystemControls(){
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        showActionBar();
    }

    protected void hideActionBar(){
        try {
            actionBar = getSupportActionBar();
            if (actionBar != null) {
                if (actionBar.isShowing())
                    actionBar.hide();
            }
            actionBar = null; //so that setActivityTitle() would not proceed
        } catch (NullPointerException npe) {
            ExceptionReporter.printStackTrace(npe);
        }
    }

    protected void hideSystemControls(){
        if (getWindow().getDecorView().getSystemUiVisibility() != mSystemControlsHideFlags)
            getWindow().getDecorView().setSystemUiVisibility(mSystemControlsHideFlags);
        hideActionBar();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            getWindow().getDecorView().postDelayed(()->{
                getWindow().getDecorView().setSystemUiVisibility(mSystemControlsHideFlags);
            }, 3000);
        });
    }

    public UXToolkit getUXToolkit(){
        return mUXToolkit;
    }

    public LayoutInflater getLayoutInflater(){
        if (mLayoutInflater == null)
            mLayoutInflater = LayoutInflater.from(this);
        return mLayoutInflater;
    }

    protected LocationService getLocationService(){
        return mLocationService;
    }

    public void addLocationChangeCallback(String index, ILocationChangeCallback callback) {
        StaticUtils.getHandler().postDelayed(()-> {
            if (getLocationService() != null) {
                try {
                    getLocationService().addLocationChangeListener(index, callback);
                } catch (InvalidIndexException e) {
                    ExceptionReporter.printStackTrace(e);
                    Log.e(TAG, e.getMessage(), e);
                }
            } else {
                if (++mLocationAttachAttempts >= 5) {
                    Exception e =  new Exception("addLocationChangeCallback] - Attempt to add location listener to LocationService failed after 5 tries, Location service has not started, make sure startLocationService() is called before adding listener");
                    ExceptionReporter.printStackTrace(e);
                    Log.e(TAG, e.getMessage(), e);
                    mLocationAttachAttempts = 0;
                } else
                    addLocationChangeCallback(index, callback);
            }
        },1000);
    }

    protected void startLocationService(){
        Log.d(TAG, "startLocationService: Starting location service");
        if (mLocationService == null) {
            if (locationServiceIntent == null)
                locationServiceIntent = new Intent(this, LocationService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(locationServiceIntent);

            IS_LOCATION_SERVICE_BOUND = bindService(locationServiceIntent, mLocationServiceConnection, Context.BIND_AUTO_CREATE);
        }

        IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_ACTION_PROVIDER_DISABLED);
        registerReceiver(GPS_PROVIDER_ACCESS, intentFilter);
        USING_LOCATION_SERVICE = true;
    }

    protected void stopLocationService(){
        if (USING_LOCATION_SERVICE) {
            if (mLocationService != null) {
                if (GPS_PROVIDER_ACCESS.isOrderedBroadcast())
                    unregisterReceiver(GPS_PROVIDER_ACCESS);
                if (IS_LOCATION_SERVICE_BOUND) {
                    unbindService(mLocationServiceConnection);
                    IS_LOCATION_SERVICE_BOUND = false;
                }
                stopService(locationServiceIntent);
            }
            USING_LOCATION_SERVICE = false;
        }
    }

    protected Location getLocation(){
        if (mLocationService == null) {
            if (checkPermissionByKey(permissionKeyLocation)) {
                startLocationService();
            } else
                showAlertLocationSettings();
        }
        return mLocationService.getLocation();
    }

    public void verifyCurrentLocation(@Nullable ILocationChangeCallback callback){
        if(Constants.DEBUG_MODE) {
            if (callback != null) {
                Location location = null;
                if (mLocationService != null)
                    location = mLocationService.getLocation();

                if (location == null)
                    location = new Location(LocationManager.GPS_PROVIDER);

                callback.onLocationChange(location);
            }
            return;
        }
        if (mLocationService != null) {
            checkLocationAndRunCallback(callback);
        } else {
            startLocationService();
            mAfterLocationServiceStartCallback = () -> {
                checkLocationAndRunCallback(callback);
            };
        }
    }

    private void checkLocationAndRunCallback(ILocationChangeCallback callback){
        if (mLocationService.getLocation() == null) {
            mUXToolkit.showProgressDialogue("Getting current location, please wait...");
            if (callback != null)
                mLocationService.setLocationChangedCallback((location -> {
                    mUXToolkit.dismissProgressDialogue();
                    callback.onLocationChange(location);
                }));
        } else {
            if (callback != null)
                callback.onLocationChange(mLocationService.getLocation());
        }
    }

    protected void setActivityTitle(@NonNull String title, @Nullable String subtitle){
        if(actionBar != null){
            Spanned htm = Html.fromHtml(title);
            ((TextView) actionBar.getCustomView().findViewById(R.id.tv_1)).setText(htm);
            if(subtitle != null)
                ((TextView) actionBar.getCustomView().findViewById(R.id.tv_2)).setText(subtitle);
            else
                ((TextView) actionBar.getCustomView().findViewById(R.id.tv_2)).setVisibility(View.INVISIBLE);
        }{
            Objects.requireNonNull(getSupportActionBar())
                    .setTitle(title);
        }
    }

    protected void setActivityTitle(int title, int subtitle){
        setActivityTitle(getString(title),getString(subtitle));
    }

    protected void setActivityTitle(@NonNull String subtitle){
        setActivityTitle(getString(R.string.app_name),subtitle);
    }

    protected void setActivityTitle(int subtitle){
        setActivityTitle(getString(R.string.app_name),getString(subtitle));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE_LOCATION) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showAlertAppPermissionsSetting();
            }
        }
    }

    private List<String> getAllPermissions(){
        List<String> allPermissions = new ArrayList<>();
        for (String key : mPermissions.keySet()){
            allPermissions.addAll(Arrays.asList(mPermissions.get(key)));
        }
        return allPermissions;
    }

    protected boolean hasAllPermissions(){
        List<String> permissions = getAllPermissions();
        boolean has = true;
        for (String perm : permissions)
            has = has & ContextCompat.checkSelfPermission(this, perm)
                    == PackageManager.PERMISSION_GRANTED;
        return has;
    }

    protected void checkAllPermissions() {
        List<String> permissions = getAllPermissions();
        boolean proceed = hasAllPermissions();
        if (!proceed) {
            proceed = false;
            for (String perm : permissions)
                proceed = proceed | ActivityCompat.shouldShowRequestPermissionRationale(this, perm);
            if (proceed) {
                mUXToolkit.buildAlertDialogue(
                        getString(R.string.alert_dialog_location_storage_title)
                        , getString(R.string.alert_dialog_location_storage_message)
                        , getString(R.string.label_btn_ok),
                        this::askForAllPermissions)
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                askForAllPermissions();
            }
        }
    }

    protected boolean checkPermissionByKey(String key){
        boolean proceed = true;
        for (String perm : mPermissions.get(key))
            proceed = proceed & ContextCompat.checkSelfPermission(this, perm)
                    == PackageManager.PERMISSION_GRANTED;
        return proceed;
    }

    private void askForAllPermissions(){
        List<String> permissionsList = getAllPermissions();
        String[] permissionsArray = new String[permissionsList.size()];
        permissionsList.toArray(permissionsArray);
        ActivityCompat.requestPermissions(
                this,
                permissionsArray,
                PERMISSIONS_REQUEST_CODE_LOCATION
        );
    }

    protected void showAlertAppPermissionsSetting(){
        try {
            if (!isDestroyed() && !isFinishing()) {
                if(dialogLocationSettings == null) {
                    dialogLocationSettings = mUXToolkit.buildAlertDialogue(
                            getString(R.string.alert_dialog_all_permissions_title)
                            ,getString(R.string.alert_dialog_all_permissions_message)
                            ,getString(R.string.label_settings)
                            , () -> {
                                final Intent i = new Intent();
                                i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                i.addCategory(Intent.CATEGORY_DEFAULT);
                                i.setData(Uri.parse("package:" + getPackageName()));
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                startActivity(i);
                            }
                    );
                }

                if(!dialogLocationSettings.isShowing())
                    dialogLocationSettings.show();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void showAlertLocationSettings(){
        try {
            if (!isDestroyed() && !isFinishing()) {
                if(dialogLocationSettings == null) {
                    dialogLocationSettings = mUXToolkit.buildAlertDialogue(
                            getString(R.string.alert_dialog_gps_title)
                            ,getString(R.string.alert_dialog_gps_message)
                            ,getString(R.string.label_btn_settings)
                            , () -> {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                    );
                }

                if(!dialogLocationSettings.isShowing())
                    dialogLocationSettings.show();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}