package pk.gov.pbs.utils.location;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pk.gov.pbs.utils.Constants;
import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.R;
import pk.gov.pbs.utils.StaticUtils;
import pk.gov.pbs.utils.exceptions.InvalidIndexException;

public class LocationService extends Service implements LocationListener {
    private static final String TAG = ":Utils] LocationService";
    public static final String BROADCAST_RECEIVER_ACTION_PROVIDER_DISABLED = Constants.Location.BROADCAST_RECEIVER_ACTION_PROVIDER_DISABLED;
    public static final String BROADCAST_RECEIVER_ACTION_LOCATION_CHANGED = Constants.Location.BROADCAST_RECEIVER_ACTION_LOCATION_CHANGED;
    public static final String BROADCAST_EXTRA_LOCATION_DATA = Constants.Location.BROADCAST_EXTRA_LOCATION_DATA;

    private static final int SERVICE_NOTIFICATION_ID = 1;
    private HashMap<String, List<ILocationChangeCallback>> mOnLocationChangedLocalCallbacks;
    private HashMap<String, ILocationChangeCallback> mOnLocationChangedGlobalCallbacks;
    private List<ILocationChangeCallback> mListOTCs;
    private final LocationServiceBinder mBinder = new LocationServiceBinder();
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 10;

    protected boolean isGPSEnabled = false;
    protected boolean isNetworkEnabled = false;

    protected LocationManager mLocationManager;
    protected Location mLocation;

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
                .setContentText("Device location is being observed")
                .setSmallIcon(R.drawable.ic_location)
//                .setContentIntent(
//                        PendingIntent.getActivity(
//                                this, 0,
//                                new Intent(this, CustomActivity.class),
//                                0
//                        )
//                )
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

    public void addLocationChangeGlobalCallback(String index, ILocationChangeCallback callback) throws InvalidIndexException {
        if (mOnLocationChangedGlobalCallbacks == null)
            mOnLocationChangedGlobalCallbacks = new HashMap<>();

        if (mOnLocationChangedGlobalCallbacks.containsKey(index))
            throw new InvalidIndexException(index, "it already exists");

        mOnLocationChangedGlobalCallbacks.put(index, callback);

        if (mLocation != null)
            callback.onLocationChange(mLocation);
    }

    public void removeLocationChangeListener(String index){
        if (mOnLocationChangedGlobalCallbacks != null)
            mOnLocationChangedGlobalCallbacks.remove(index);
    }

    /**
     * adds One Time Callback to the service
     * on receiving location it will execute once and then remove it
     * @param otc one time callback
     */

    public void addLocationChangedOTC(ILocationChangeCallback otc){
        if (mListOTCs == null)
            mListOTCs = new ArrayList<>();

        mListOTCs.add(otc);
    }

    public void addLocalLocationChangedCallback(Context context, ILocationChangeCallback changedCallback){
        if (mOnLocationChangedLocalCallbacks == null)
            mOnLocationChangedLocalCallbacks = new HashMap<>();

        if (mOnLocationChangedLocalCallbacks.containsKey(context.getClass().getSimpleName()))
            mOnLocationChangedLocalCallbacks.get(context.getClass().getSimpleName()).add(changedCallback);
        else {
            List<ILocationChangeCallback> list = new ArrayList<>();
            list.add(changedCallback);
            mOnLocationChangedLocalCallbacks.put(context.getClass().getSimpleName(), list);
        }
    }

    public void clearLocalCallbacks(CustomActivity context){
        mOnLocationChangedLocalCallbacks.remove(context.getLocalClassName());
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
        intent.setAction(BROADCAST_RECEIVER_ACTION_PROVIDER_DISABLED);
        sendBroadcast(intent);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "onLocationChanged: Location changed : " + location.toString());
        if (((int) location.getLongitude() != 0 && (int) location.getLatitude() != 0)) {
            mLocation = location;

            Intent intent = new Intent();
            intent.setAction(BROADCAST_RECEIVER_ACTION_LOCATION_CHANGED);
            intent.putExtra(BROADCAST_EXTRA_LOCATION_DATA, location);
            sendBroadcast(intent);
        }

        // Executing OTC
        if (mListOTCs != null && mListOTCs.size() > 0) {
            StaticUtils.getHandler().post(()-> {
                for (int i=0; i < mListOTCs.size(); i++) {
                    mListOTCs.get(i).onLocationChange(location);
                }
                mListOTCs.clear();
            });
        }

        //Executing Local Callbacks
        if (mOnLocationChangedLocalCallbacks != null && mOnLocationChangedLocalCallbacks.size() > 0) {
            StaticUtils.getHandler().post(()-> {
                    for (String groupId : mOnLocationChangedLocalCallbacks.keySet()) {
                        if (mOnLocationChangedLocalCallbacks.get(groupId).size() > 0) {
                            for (ILocationChangeCallback callback : mOnLocationChangedLocalCallbacks.get(groupId)) {
                                callback.onLocationChange(location);
                            }
                        }
                    }
            });
        }

        //Executing Global Callbacks
        if (mOnLocationChangedGlobalCallbacks != null && mOnLocationChangedGlobalCallbacks.size() > 0){
            StaticUtils.getHandler().post(()-> {
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                            for (String callbackIndex : mOnLocationChangedGlobalCallbacks.keySet()){
                                StaticUtils.getHandler().post(()-> {
                                    mOnLocationChangedGlobalCallbacks.get(callbackIndex).onLocationChange(location);
                                });
                            }
                        }
                })).start();
            });
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

    public class LocationServiceBinder extends Binder {
        public LocationService getService(){
            return LocationService.this;
        }
    }

}
