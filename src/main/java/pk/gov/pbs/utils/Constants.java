package pk.gov.pbs.utils;

import pk.gov.pbs.utils.location.LocationService;

public class Constants {
    public static final String TAG = "[:Utils]";
    public static final boolean DEBUG_MODE = true;
    public static final String INTEGRITY_CHECK_KEY = "!s^&ks_=(a$";

    public static final String Notification_Channel_ID = "TDS_Notification_Channel";
    public static final String Notification_Channel_Name = "TDS Notifications";
    public static final String SHARE_PREFERENCES_CONTAINER = "TDS_Shared_Preferences";

    public static final String WEB_API_ROOT = "localhost:8080/api";

    public static class Location {
        public static final String BROADCAST_RECEIVER_ACTION_PROVIDER_DISABLED = LocationService.class.getCanonicalName() + ".ProviderDisabled";
        public static final String BROADCAST_RECEIVER_ACTION_LOCATION_CHANGED = LocationService.class.getCanonicalName() + ".LocationChanged";
        public static final String BROADCAST_EXTRA_LOCATION_DATA = Location.class.getCanonicalName() + ".CurrentLocation";
    }
}
