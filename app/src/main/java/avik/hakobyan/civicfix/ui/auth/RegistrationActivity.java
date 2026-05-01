package avik.hakobyan.civicfix.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import avik.hakobyan.civicfix.model.Account;
import avik.hakobyan.civicfix.ui.main.MainActivity;
import avik.hakobyan.civicfix.R;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";
    
    private EditText nameInput, emailInput, passwordInput, provePasswordInput;
    private Button registerButton;
    private TextView loginText;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        nameInput = findViewById(R.id.etName);
        emailInput = findViewById(R.id.etEmail);
        passwordInput = findViewById(R.id.etPassword);
        provePasswordInput = findViewById(R.id.provePassword);
        registerButton = findViewById(R.id.btnRegister);
        loginText = findViewById(R.id.tvGoLogin);

        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser(){
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String provePassword = provePasswordInput.getText().toString().trim();

        if(name.isEmpty() || email.isEmpty() || password.isEmpty() || provePassword.isEmpty()){
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        } 
        
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        } 
        
        if (!password.equals(provePassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Log.d(TAG, "registerUser: Success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            
                            // Send verification email
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(RegistrationActivity.this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_LONG).show();
                                        } else {
                                            Log.e(TAG, "sendEmailVerification", verifyTask.getException());
                                            Toast.makeText(RegistrationActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                            // Update Firebase Auth Profile
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates);

                            // Create and save Account object to Realtime Database
                            Account account = new Account(uid, name, email, System.currentTimeMillis());
                            mDatabase.child("users").child(uid).setValue(account)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            // Sign out after registration so they have to verify and log in
                                            mAuth.signOut();
                                            Toast.makeText(RegistrationActivity.this, "Registration successful. Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                                            
                                            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(RegistrationActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Log.e(TAG, "registerUser: Failed -> " + error);
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}