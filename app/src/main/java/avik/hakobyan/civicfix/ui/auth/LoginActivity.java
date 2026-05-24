package avik.hakobyan.civicfix.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnDemoUser;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnDemoUser = findViewById(R.id.btnDemoUser);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> handleLogin());
        
        if (btnDemoUser != null) {
            btnDemoUser.setOnClickListener(v -> handleDemoLogin());
        }

        findViewById(R.id.tvGoRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, RegistrationActivity.class));
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        performLogin(email, pass, false);
    }

    private void handleDemoLogin() {
        performLogin("innovationcampus26@gmail.com", "123456", true);
    }

    private void performLogin(String email, String pass, boolean isDemo) {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        if (btnDemoUser != null) btnDemoUser.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (isDemo) {
                    // Demo user doesn't need email verification check for convenience
                    progressBar.setVisibility(View.GONE);
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    checkEmailVerification();
                }
            } else {
                showError(task.getException().getMessage());
            }
        });
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.isEmailVerified()) {
                progressBar.setVisibility(View.GONE);
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                if (btnDemoUser != null) btnDemoUser.setEnabled(true);
                mAuth.signOut();
                Toast.makeText(this, "Please verify your email address before logging in.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        if (btnDemoUser != null) btnDemoUser.setEnabled(true);
        Toast.makeText(this, "Login failed: " + message, Toast.LENGTH_LONG).show();
    }
}
