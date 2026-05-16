package avik.hakobyan.civicfix;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class CivicFixApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        FirebaseApp.initializeApp(this);
        
        // Enable Firebase offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Initialize App Check for Debugging
        // This helps bypass Recaptcha errors on emulators and debug builds
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
    }
}
