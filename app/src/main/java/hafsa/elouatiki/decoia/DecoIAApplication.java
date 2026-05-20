package hafsa.elouatiki.decoia;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class DecoIAApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("1:934539577124:android:0af099749d8384cdc555b1")
                .setApiKey("AIzaSyAOKv3FTO1912MyyCR1lHBmmIFM-rF95zA")
                .setProjectId("decoia-6a5e3")
                .setStorageBucket("decoia-6a5e3.firebasestorage.app")
                .build();

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options);
        }
    }
}
