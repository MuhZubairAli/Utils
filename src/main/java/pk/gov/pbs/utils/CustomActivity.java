package pk.gov.pbs.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import pk.gov.pbs.utils.exceptions.InvalidIndexException;
import pk.gov.pbs.utils.location.ILocationChangeCallback;
import pk.gov.pbs.utils.location.LocationService;

public abstract class CustomActivity extends AppCompatActivity {
    private static final String TAG = ":Utils] CustomActivity";
    private static final int PERMISSIONS_REQUEST_FIRST = 100;
    private static final int PERMISSIONS_REQUEST_SECOND = 101;
    private static final int mSystemControlsHideFlags =
            View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    private boolean IS_LOCATION_SERVICE_BOUND = false;
    private boolean USING_LOCATION_SERVICE = false;
    private ActionBar actionBar;
    private AlertDialog dialogLocationSettings;

    private Runnable mAfterLocationServiceStartCallback;
    private LocationService mLocationService = null;
    private ServiceConnection mLocationServiceConnection = null;
    private BroadcastReceiver GPS_PROVIDER_ACCESS = null;
    private static byte mLocationAttachAttempts = 0;

    private final List<String> mPermissions = new ArrayList<>();
    private final List<String> mSpecialPermissions = new ArrayList<>(5);

    private LayoutInflater mLayoutInflater;
    protected UXToolkit mUXToolkit;
    protected FileManager mFileManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        //checkAllPermissions();
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
        if (USING_LOCATION_SERVICE) {
            if (getLocationService() != null)
                getLocationService().
                        clearLocalCallbacks(this);
            stopLocationService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        List<String> askAgain = new ArrayList<>();
        boolean missingPermission = false;
        for (int i = 0; i < grantResults.length; i++) {
            missingPermission = missingPermission | grantResults[i] == PackageManager.PERMISSION_DENIED;
            if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                askAgain.add(permissions[i]);
        }

        if (requestCode == PERMISSIONS_REQUEST_FIRST && missingPermission) {
            boolean showRationale = false;
            for (String perm : askAgain)
                showRationale = showRationale | ActivityCompat.shouldShowRequestPermissionRationale(this, perm);

            if (showRationale) {
                mUXToolkit.buildAlertDialogue(
                        getString(R.string.alert_dialog_permission_require_all_title)
                        , getString(R.string.alert_dialog_permission_require_all_message)
                        , getString(R.string.label_btn_request_again),
                        () -> requestPermissions(PERMISSIONS_REQUEST_SECOND, askAgain.toArray(new String[0]))).show();
            } else {
                // No explanation needed, we can request the permissions..
                requestPermissions(PERMISSIONS_REQUEST_SECOND, askAgain.toArray(new String[0]));
            }
        } else if (requestCode == PERMISSIONS_REQUEST_SECOND && missingPermission) {
            showAlertAppPermissionsSetting();
        }

        if (!mSpecialPermissions.isEmpty()){
            requestSpecialPermissions();
        }
    }

    private void initialize(){
        mUXToolkit = new UXToolkit(this);
        mFileManager = new FileManager(this.getApplicationContext());

        GPS_PROVIDER_ACCESS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showAlertLocationSettings();
            }
        };

        mPermissions.addAll(Arrays.asList(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE)
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mSpecialPermissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        }
    }

    private String[] getAllPermissions(){
        String[] permissions = new String[
                mPermissions.size() +
                LocationService.getPermissionsRequired().length +
                FileManager.getPermissionsRequired().length
        ];
        int i = 0;
        for (String perm : mPermissions)
            permissions[i++] = perm;

        for (String perm : LocationService.getPermissionsRequired())
            permissions[i++] = perm;

        for (String perm : FileManager.getPermissionsRequired())
            permissions[i++] = perm;

        return permissions;
    }

    protected String[] getSpecialPermissions(){
        return mSpecialPermissions.toArray(new String[0]);
    }

    protected String[] getDeniedPermissions(){
        List<String> denied = new ArrayList<>();

        for (String perm : getAllPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_DENIED)
                denied.add(perm);
        }

        if (!denied.isEmpty()){
            return denied.toArray(new String[0]);
        }

        return null;
    }

    private String[] getAskablePermissions(){
        ArrayList<String> permission = new ArrayList<>(
                Arrays.asList(getDeniedPermissions())
        );
        permission.removeAll(mSpecialPermissions);
        return permission.toArray(new String[0]);
    }

    protected boolean hasAllPermissions(){
        boolean has = true;
        for (String perm : getAllPermissions())
            has &= ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
        return has;
    }

    protected void checkAllPermissions() {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED))
            throw new RuntimeException("This method must be called from onCreate method only, other wise it will cause infinite recursion");

        String[] perms = getAskablePermissions();
        if (perms.length > 0)
            requestPermissions(
                    CustomActivity.PERMISSIONS_REQUEST_FIRST,
                    perms
            );
        else if (!hasAllPermissions())
            requestSpecialPermissions();
    }

    protected void requestPermissions(int requestCode, String[] permissions){
        ActivityCompat.requestPermissions(
                this,
                permissions,
                requestCode
        );
    }

    protected void requestSpecialPermissions(){
        for (String perm : mSpecialPermissions){
            if (perm.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    StaticUtils.getHandler().post(() -> {
                        mUXToolkit.showToast("On API 30 and above permission to manage all files is required, Please enable the option of 'Allow access to manage all files'.");
                    });
                    Uri uri = Uri.parse("package:" + this.getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                }
            } else { // else if(other special permission) handle requesting of other special permission (if any)
            }
        }
    }

    //only call this from Activity construction
    //because mPermission is consumed in onCreate()
    protected void includePermission(String permission){
        if(this.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED))
            throw new RuntimeException("In order to include new permission ["+permission+"], call includePermission(String) from activity constructor.");
        mPermissions.add(permission);
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
            ExceptionReporter.handle(npe);
        }
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
            ExceptionReporter.handle(npe);
        }
    }

    protected void showSystemControls(){
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        showActionBar();
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


    @NonNull
    public LayoutInflater getLayoutInflater(){
        if (mLayoutInflater == null)
            mLayoutInflater = LayoutInflater.from(this);
        return mLayoutInflater;
    }

    protected LocationService getLocationService(){
        return mLocationService;
    }
    public UXToolkit getUXToolkit(){
        return mUXToolkit;
    }

    public void addLocationChangeGlobalCallback(String index, ILocationChangeCallback callback) {
        StaticUtils.getHandler().postDelayed(()-> {
            if (getLocationService() != null) {
                try {
                    getLocationService().addLocationChangeGlobalCallback(index, callback);
                } catch (InvalidIndexException e) {
                    ExceptionReporter.handle(e);
                }
            } else {
                if (++mLocationAttachAttempts >= 5) {
                    Exception e =  new Exception("addLocationChangeCallback] - Attempt to add location listener to LocationService failed after 5 tries, Location service has not started, make sure startLocationService() is called before adding listener");
                    ExceptionReporter.handle(e);
                    mLocationAttachAttempts = 0;
                } else
                    addLocationChangeGlobalCallback(index, callback);
            }
        },1000);
    }

    /**
     * this helper method attaches LocationChange callback with location service
     * it will attempt 5 times with 1 sec gap if locationService is not available then throws exception
     * it will not start location service (only waits 5 secs if location service is starting)
     * @param callback OTC to be attached with the service
     */
    public void addLocationChangeCallback(ILocationChangeCallback callback) {
        StaticUtils.getHandler().postDelayed(()-> {
            if (getLocationService() != null) {
                getLocationService().addLocalLocationChangedCallback(this, callback);
            } else {
                if (++mLocationAttachAttempts >= 5) {
                    Exception e =  new Exception("addLocationChangeCallback] - Attempt to add location listener to LocationService failed after 5 tries, Location service has not started, make sure startLocationService() is called before adding listener");
                    ExceptionReporter.handle(e);
                    mLocationAttachAttempts = 0;
                } else
                    addLocationChangeCallback(callback);
            }
        }, 1000);
    }

    /**
     * this helper method attaches OTC with location service
     * it will attempt 5 times with 1 sec gap if locationService is not available then throws exception
     * it will not start location service (only waits 5 secs if location service is starting)
     * @param callback OTC to be attached with the service
     */
    public void addLocationChangedOneTimeCallback(ILocationChangeCallback callback) {
        StaticUtils.getHandler().postDelayed(()-> {
            if (getLocationService() != null) {
                getLocationService().addLocationChangedOTC(callback);
            } else {
                if (++mLocationAttachAttempts >= 5) {
                    Exception e =  new Exception("addLocationChangeCallback] - Attempt to add location listener to LocationService failed after 5 tries, Location service has not started, make sure startLocationService() is called before adding listener");
                    ExceptionReporter.handle(e);
                    mLocationAttachAttempts = 0;
                } else
                    addLocationChangedOneTimeCallback(callback);
            }
        },1000);
    }

    protected void startLocationService(){
        Log.d(TAG, "startLocationService: Starting location service");
        if (mLocationServiceConnection == null) {
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
        }

        if (mLocationService == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(new Intent(this, LocationService.class));

            IS_LOCATION_SERVICE_BOUND = bindService( new Intent(this, LocationService.class),
                    mLocationServiceConnection, Context.BIND_AUTO_CREATE);
        }

        IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_RECEIVER_ACTION_PROVIDER_DISABLED);
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
                stopService(new Intent(this, LocationService.class));
            }
            USING_LOCATION_SERVICE = false;
        }
    }

    public void verifyCurrentLocation(@Nullable ILocationChangeCallback callback){
        if(Constants.DEBUG_MODE) {
            if (callback != null) {
                Location location = null;
                if (mLocationService != null)
                    location = mLocationService.getLocation();

                if (location == null)
                    location = mLocationService.getLastKnownLocation();

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

    private void checkLocationAndRunCallback(@NonNull ILocationChangeCallback callback){
        if (mLocationService.getLocation() == null) {
            mUXToolkit.showProgressDialogue("Getting current location, please wait...");
            mLocationService.addLocationChangedOTC((location -> {
                mUXToolkit.dismissProgressDialogue();
                callback.onLocationChange(location);
            }));
        } else {
            callback.onLocationChange(mLocationService.getLocation());
        }
    }

    protected void showAlertAppPermissionsSetting(){
        try {
            if (!isDestroyed() && !isFinishing()) {
                if(dialogLocationSettings == null) {
                    dialogLocationSettings = mUXToolkit.buildAlertDialogue(
                            getString(R.string.alert_dialog_all_permissions_title)
                            ,getString(R.string.alert_dialog_all_permissions_message)
                            ,getString(R.string.label_btn_permissions_settings)
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
            ExceptionReporter.handle(e);
        }
    }

    private void showAlertLocationSettings(){
        try {
            if (!isDestroyed() && !isFinishing()) {
                if(dialogLocationSettings == null) {
                    dialogLocationSettings = mUXToolkit.buildAlertDialogue(
                            getString(R.string.alert_dialog_gps_title)
                            ,getString(R.string.alert_dialog_gps_message)
                            ,getString(R.string.label_btn_location_settings)
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
            ExceptionReporter.handle(e);
        }
    }

}
