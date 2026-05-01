package avik.hakobyan.civicfix;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class CivicFixApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Feature 14: Enable Firebase offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}