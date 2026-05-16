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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Account;

public class RegistrationActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.provePassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.tvGoLogin).setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "Password too short (min 6 chars)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                saveUserToDatabase(name, email);
            } else {
                showError(task.getException().getMessage());
            }
        });
    }

    private void saveUserToDatabase(String name, String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // 1. Update Auth Profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            // 2. Save to Realtime Database
            Account account = new Account(user.getUid(), name, email, System.currentTimeMillis());
            mDatabase.child(user.getUid()).setValue(account).addOnCompleteListener(dbTask -> {
                if (dbTask.isSuccessful()) {
                    sendVerificationEmail(user);
                } else {
                    showError("Database error: " + dbTask.getException().getMessage());
                }
            });
        });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Verification email sent! Please check your inbox.", Toast.LENGTH_LONG).show();
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        btnRegister.setEnabled(true);
        Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
    }
}
