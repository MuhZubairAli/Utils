package pk.gov.pbs.utils.location;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;

import pk.gov.pbs.utils.Constants;
import pk.gov.pbs.utils.R;
import pk.gov.pbs.utils.exceptions.InvalidIndexException;

public class LocationService extends Service implements LocationListener {
    private static final String TAG = "LocationService";
    public static final String BROADCAST_ACTION_PROVIDER_DISABLED = LocationService.class.getCanonicalName() + ".ProviderDisabled";
    public static final String BROADCAST_ACTION_LOCATION_CHANGED = LocationService.class.getCanonicalName() + ".LocationChanged";
    public static final String BROADCAST_EXTRA_LOCATION_PARCEL = "currentLocation";

    private static final int SERVICE_NOTIFICATION_ID = 1;
    private HashMap<String, ILocationChangeCallback> mOnLocationChangedCallbacks;
    private ILocationChangeCallback mSingleRunCallback;
    private final LocationServiceBinder mBinder = new LocationServiceBinder();
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 10;

    protected boolean isGPSEnabled = false;
    protected boolean isNetworkEnabled = false;

    protected LocationManager mLocationManager;
    protected Location mLocation;

    public Location getLocation() {
        return mLocation;
    }

    public Location getLastKnownLocation(){
        if (
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            return mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null
                    ? mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    : mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        return null;
    }

    public void addLocationChangeListener(String index, ILocationChangeCallback callback) throws InvalidIndexException {
        if (mOnLocationChangedCallbacks == null)
            mOnLocationChangedCallbacks = new HashMap<>();

        if (mOnLocationChangedCallbacks.containsKey(index))
            throw new InvalidIndexException(index, "it already exists");

        mOnLocationChangedCallbacks.put(index, callback);

        if (mLocation != null)
            callback.onLocationChange(mLocation);
    }

    public void removeLocationChangeListener(String index){
        if (mOnLocationChangedCallbacks != null)
            mOnLocationChangedCallbacks.remove(index);
    }

    public void setLocationChangedCallback(ILocationChangeCallback changedCallback){
        mSingleRunCallback = changedCallback;
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        requestLocationUpdates();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "onStatusChanged: Change in GPS detected");
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION_PROVIDER_DISABLED);
        sendBroadcast(intent);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "onLocationChanged: Location changed : " + location.toString());
        if (((int) location.getLongitude() != 0 && (int) location.getLatitude() != 0)) {
            mLocation = location;

            Intent intent = new Intent();
            intent.setAction(BROADCAST_ACTION_LOCATION_CHANGED);
            intent.putExtra(BROADCAST_EXTRA_LOCATION_PARCEL, location);
            sendBroadcast(intent);
        }

        if (mSingleRunCallback != null) {
            mSingleRunCallback.onLocationChange(location);
            mSingleRunCallback = null;
        }

        if (mOnLocationChangedCallbacks != null && mOnLocationChangedCallbacks.size() > 0){
            for (String callbackIndex : mOnLocationChangedCallbacks.keySet()){
                mOnLocationChangedCallbacks.get(callbackIndex).onLocationChange(location);
            }
        }
    }

    public boolean isGPSEnabled() {
        isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isGPSEnabled;
    }

    public boolean isNetworkEnabled() {
        isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return isNetworkEnabled;
    }

    protected boolean requestLocationUpdates() {
        if (
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            if (isGPSEnabled || isNetworkEnabled) {
                if (isNetworkEnabled) {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            this);
                }

                if (isGPSEnabled){
                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            this);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate() {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "onCreate: Location service created");
        super.onCreate();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        isGPSEnabled = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = mLocationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "onStartCommand: requesting location updates");
        requestLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, Constants.Notification_Channel_ID)
                .setContentTitle("Location Service")
                .setContentText("Current location is being observed")
                .setSmallIcon(R.drawable.ic_location)
                .build();

        startForeground(SERVICE_NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocationServiceBinder extends Binder {
        public LocationService getService(){
            return LocationService.this;
        }
    }

}
