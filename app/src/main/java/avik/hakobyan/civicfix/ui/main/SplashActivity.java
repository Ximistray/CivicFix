package avik.hakobyan.civicfix.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.ui.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private static final int SPLASH_DELAY = 2500; // 2.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        // 1. Initialize views and animations
        ImageView ivLogo = findViewById(R.id.ivLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);

        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1500);

        if (ivLogo != null) ivLogo.startAnimation(fadeIn);
        if (tvAppName != null) tvAppName.startAnimation(fadeIn);

        // 2. Delay and navigate
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, SPLASH_DELAY);
    }

    private void checkUserStatus() {
        // Safety check to ensure we don't navigate if the activity was closed during the delay
        if (isFinishing() || isDestroyed()) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        Intent intent;
        if (currentUser != null && currentUser.isEmailVerified()) {
            // User is logged in and verified
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // No user or user not verified
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        
        startActivity(intent);
        finish();
    }
}
