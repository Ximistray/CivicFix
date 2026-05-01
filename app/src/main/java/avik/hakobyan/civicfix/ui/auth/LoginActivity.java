package avik.hakobyan.civicfix.ui.auth;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import avik.hakobyan.civicfix.ui.main.MainActivity;
import avik.hakobyan.civicfix.R;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginButton;
    TextView registerText;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.etEmail);
        passwordInput = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        registerText = findViewById(R.id.tvGoRegister);

        loginButton.setOnClickListener(v -> loginUser());

        registerText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            Log.d(TAG, "loginUser: Empty fields");
            Toast.makeText(this,"Fill all fields",Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                Log.d(TAG, "loginUser: Success - Email Verified");
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.d(TAG, "loginUser: Email not verified");
                                mAuth.signOut();
                                Toast.makeText(this, "Please verify your email address. Check your inbox.", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Log.d(TAG, "loginUser: Failed, user not found or wrong credentials");
                        Toast.makeText(this,"Login failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"),Toast.LENGTH_SHORT).show();
                    }
                });
    }
}