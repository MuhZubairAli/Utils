package pk.gov.pbs.utils;
import android.content.SharedPreferences;

public class Application extends android.app.Application {
    private static Application INSTANCE;
    private static SharedPreferences mSharedPreferences;

    public Application(){
        super();
        INSTANCE = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SystemUtils.createNotificationChannel(
                this
                , Constants.Notification_Channel_Name
                , Constants.Notification_Channel_ID
        );
    }

    public static Application getInstance() {
        return INSTANCE;
    }

    public static SharedPreferences getSharedPreferencesManager(){
        if (mSharedPreferences == null && INSTANCE != null) {
            synchronized (Application.class) {
                mSharedPreferences = getInstance().getSharedPreferences(Constants.SHARE_PREFERENCES_CONTAINER, MODE_PRIVATE);
                return mSharedPreferences;
            }
        }
        return mSharedPreferences;
    }
}
