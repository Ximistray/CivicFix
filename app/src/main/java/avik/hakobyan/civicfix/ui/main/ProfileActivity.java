package avik.hakobyan.civicfix.ui.main;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Account;
import avik.hakobyan.civicfix.ui.auth.LoginActivity;
import avik.hakobyan.civicfix.ui.map.MapActivity;
import avik.hakobyan.civicfix.ui.report.MyReportsActivity;
import avik.hakobyan.civicfix.ui.report.ReportProblemActivity;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private TextView tvUserName, tvUserEmail;
    private EditText etProfileName;
    private ImageView profileImage;
    private Button btnAdminPanel;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    uploadProfileImage();
                }
            }
    );

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();
        FirebaseUser user = mAuth.getCurrentUser();

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        etProfileName = findViewById(R.id.etProfileName);
        profileImage = findViewById(R.id.profileImage);
        btnAdminPanel = findViewById(R.id.btnAdminPanel);

        if (user != null) {
            loadUserProfile(user.getUid());
        }

        findViewById(R.id.btnUpdateProfile).setOnClickListener(v -> updateProfile());
        
        View btnChangePhoto = findViewById(R.id.btnChangePhoto);
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }

        findViewById(R.id.btnSecurity).setOnClickListener(v -> 
                startActivity(new Intent(this, SecuritySettingsActivity.class)));

        findViewById(R.id.btnNotifications).setOnClickListener(v -> 
                startActivity(new Intent(this, NotificationSettingsActivity.class)));

        findViewById(R.id.btnLanguage).setOnClickListener(v -> 
                startActivity(new Intent(this, LanguageSettingsActivity.class)));

        findViewById(R.id.btnAbout).setOnClickListener(v -> 
                Toast.makeText(this, "CivicFix v1.0\nDeveloped by Avik Hakobyan", Toast.LENGTH_LONG).show());

        findViewById(R.id.btnPrivacy).setOnClickListener(v -> 
                Toast.makeText(this, "Privacy Policy coming soon!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnLogoutProfile).setOnClickListener(v -> logout());
        
        if (btnAdminPanel != null) {
            btnAdminPanel.setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));
        }

        // Setup Floating Action Button
        FloatingActionButton fabAdd = findViewById(R.id.fabAddReport);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> startActivity(new Intent(this, ReportProblemActivity.class)));
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                } else if (itemId == R.id.nav_map) {
                    startActivity(new Intent(this, MapActivity.class));
                    return true;
                } else if (itemId == R.id.nav_reports) {
                    startActivity(new Intent(this, MyReportsActivity.class));
                    return true;
                }
                return itemId == R.id.nav_profile;
            });
        }
    }

    private void loadUserProfile(String uid) {
        mDatabase.child("users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Account account = snapshot.getValue(Account.class);
                if (account != null) {
                    tvUserName.setText(account.getName());
                    tvUserEmail.setText(account.getEmail());
                    etProfileName.setText(account.getName());
                    
                    if (profileImage != null && account.getProfileImageUrl() != null) {
                        Glide.with(ProfileActivity.this)
                                .load(account.getProfileImageUrl())
                                .circleCrop()
                                .placeholder(android.R.drawable.ic_menu_myplaces)
                                .into(profileImage);
                    }
                    
                    if (btnAdminPanel != null) {
                        btnAdminPanel.setVisibility(account.isAdmin() ? View.VISIBLE : View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || selectedImageUri == null) return;

        Toast.makeText(this, R.string.uploading_photo, Toast.LENGTH_SHORT).show();

        StorageReference fileRef = mStorage.child("profile_images/" + user.getUid() + ".jpg");
        fileRef.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot -> 
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                
                // Update Firebase Database
                mDatabase.child("users").child(user.getUid()).child("profileImageUrl").setValue(downloadUrl);
                
                // Update Firebase Auth profile
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(uri)
                    .build();
                
                user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, R.string.photo_updated, Toast.LENGTH_SHORT).show();
                    }
                });
            })
        ).addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateProfile() {
        String newName = etProfileName.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user != null) {
            mDatabase.child("users").child(user.getUid()).child("name").setValue(newName)
                .addOnSuccessListener(aVoid -> {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build();
                    
                    user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
