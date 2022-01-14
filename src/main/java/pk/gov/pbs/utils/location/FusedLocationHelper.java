package pk.gov.pbs.utils.location;

public class FusedLocationHelper {
    // This class requires "compile 'com.google.android.gms:play-services:11.0.0'" in build.gradle file

    /**
    private static final String TAG = FusedLocationHelper.class.getSimpleName();
    private static FusedLocationHelper INSTANCE;

    private final FusedLocationProviderClient mFusedLocationClient;
    private final LocationCallback locationCallback;
    private final LocationSettingsRequest locationSettingsRequest;

    private ILocationChangeCallback mLocationChangeCallback;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    public FusedLocationHelper getInstance(AppCompatActivity context) {
        if (INSTANCE == null)
            INSTANCE = new FusedLocationHelper(context);
        return INSTANCE;
    }

    private FusedLocationHelper(AppCompatActivity context) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location currentLocation = locationResult.getLastLocation();

                if (null != mLocationChangeCallback)
                    mLocationChangeCallback.onLocationChange(currentLocation);
            }
        };

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        if (
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
            );
        }
    }

    public void onChange(ILocationChangeCallback callback) {
        mLocationChangeCallback = callback;
    }

    public LocationSettingsRequest getLocationSettingsRequest() {
        return locationSettingsRequest;
    }

    public void stop() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }
    */
}