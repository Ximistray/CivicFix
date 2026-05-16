package avik.hakobyan.civicfix.ui.report;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Account;
import avik.hakobyan.civicfix.model.Problem;

public class ReportDetailActivity extends AppCompatActivity {

    private ImageView detailImage, ivVoteDetail;
    private TextView detailType, detailDescription, detailLocation, detailDate, tvVoteCountDetail;
    private Chip detailStatusChip;
    private View layoutActions, btnVoteDetail;
    private DatabaseReference reportRef;
    private DatabaseReference userRef;
    private String reportId, currentUid;
    private Problem currentReport;
    private boolean isUserAdmin = false;
    private String lastImageUrl = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        reportId = getIntent().getStringExtra("reportId");
        if (reportId == null) {
            Toast.makeText(this, "Error: Report ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.report_details);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        reportRef = FirebaseDatabase.getInstance().getReference("problems").child(reportId);
        
        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
            checkAdminStatus();
        }

        loadReportDetails();
    }

    private void checkAdminStatus() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Account account = snapshot.getValue(Account.class);
                if (account != null) {
                    isUserAdmin = account.isAdmin();
                    updateActionVisibility();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initViews() {
        detailImage = findViewById(R.id.detailImage);
        detailType = findViewById(R.id.detailType);
        detailDescription = findViewById(R.id.detailDescription);
        detailLocation = findViewById(R.id.detailLocation);
        detailDate = findViewById(R.id.detailDate);
        detailStatusChip = findViewById(R.id.detailStatusChip);
        layoutActions = findViewById(R.id.layoutActions);
        
        btnVoteDetail = findViewById(R.id.btnVoteDetail);
        ivVoteDetail = findViewById(R.id.ivVoteDetail);
        tvVoteCountDetail = findViewById(R.id.tvVoteCountDetail);

        View btnDelete = findViewById(R.id.btnDelete);
        View btnEdit = findViewById(R.id.btnEdit);

        if (btnDelete != null) btnDelete.setOnClickListener(v -> confirmDelete());
        if (btnEdit != null) btnEdit.setOnClickListener(v -> showEditDialog());
        
        if (btnVoteDetail != null) {
            btnVoteDetail.setOnClickListener(v -> toggleVote());
        }
    }

    private void toggleVote() {
        if (currentUid == null) {
            Toast.makeText(this, R.string.login_to_like, Toast.LENGTH_SHORT).show();
            return;
        }

        // Use a surgical transaction to avoid wiping data
        reportRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                // IMPORTANT: Abort if data is null to prevent accidental deletion during local transaction sync
                if (mutableData.getValue() == null) {
                    return Transaction.abort();
                }

                Integer count = mutableData.child("voteCount").getValue(Integer.class);
                if (count == null) count = 0;

                Map<String, Object> votedUsers = (Map<String, Object>) mutableData.child("votedUsers").getValue();
                if (votedUsers == null) votedUsers = new HashMap<>();

                if (votedUsers.containsKey(currentUid)) {
                    mutableData.child("voteCount").setValue(count - 1);
                    votedUsers.remove(currentUid);
                } else {
                    mutableData.child("voteCount").setValue(count + 1);
                    votedUsers.put(currentUid, true);
                }
                
                mutableData.child("votedUsers").setValue(votedUsers);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
            }
        });
    }

    private void loadReportDetails() {
        reportRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Problem report = snapshot.getValue(Problem.class);
                if (report != null) {
                    currentReport = report;
                    currentReport.setId(snapshot.getKey());
                    updateUI(currentReport);
                } else if (currentReport != null) {
                    // Only finish if the report was actually there and is now gone (not during transaction flickering)
                    // If snapshot is null but we had a report, it might be a temporary state or actual deletion.
                    // We check if it's verified on server usually, but for now we just handle null gracefully.
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportDetailActivity.this, "Error loading details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(Problem report) {
        detailType.setText(getString(getResIdForType(report.getType())));
        detailDescription.setText(report.getDescription());
        detailLocation.setText(getString(R.string.location_lat_lon, report.getLatitude(), report.getLongitude()));
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        detailDate.setText(dateFormat.format(new Date(report.getTimestamp())));

        setStatusChip(detailStatusChip, report.getStatus());

        // Prevent reloading image if URL is the same to avoid flickering
        if (report.getImageUrl() != null && !report.getImageUrl().equals(lastImageUrl)) {
            lastImageUrl = report.getImageUrl();
            Glide.with(this)
                    .load(report.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.background_gradient)
                    .error(R.drawable.ic_launcher_background)
                    .into(detailImage);
        }

        // Update Liking UI
        tvVoteCountDetail.setText(String.valueOf(report.getVoteCount()));
        boolean hasVoted = currentUid != null && report.getVotedUsers() != null && report.getVotedUsers().containsKey(currentUid);
        if (hasVoted) {
            ivVoteDetail.setImageResource(R.drawable.ic_heart_filled);
            ivVoteDetail.setColorFilter(ContextCompat.getColor(this, R.color.like_red));
            tvVoteCountDetail.setTextColor(ContextCompat.getColor(this, R.color.like_red));
        } else {
            ivVoteDetail.setImageResource(R.drawable.ic_heart_outline);
            ivVoteDetail.setColorFilter(ContextCompat.getColor(this, R.color.slate_grey));
            tvVoteCountDetail.setTextColor(ContextCompat.getColor(this, R.color.slate_grey));
        }

        updateActionVisibility();
    }

    private void updateActionVisibility() {
        if (currentReport == null) return;
        String userId = FirebaseAuth.getInstance().getUid();
        
        if (isUserAdmin || (userId != null && userId.equals(currentReport.getUserId()))) {
            layoutActions.setVisibility(View.VISIBLE);
        } else {
            layoutActions.setVisibility(View.GONE);
        }
    }

    private int getResIdForType(String type) {
        if (type == null) return R.string.type_other;
        switch (type) {
            case "Pothole": return R.string.type_pothole;
            case "Trash": return R.string.type_trash;
            case "Broken streetlight": return R.string.type_streetlight;
            case "Damaged road": return R.string.type_road;
            default: return R.string.type_other;
        }
    }

    private void setStatusChip(Chip chip, String status) {
        if (status == null) status = "pending";
        
        int statusResId;
        int color;
        
        switch (status.toLowerCase()) {
            case "resolved":
                statusResId = R.string.status_resolved;
                color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case "in_progress":
                statusResId = R.string.status_in_progress;
                color = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
                break;
            default:
                statusResId = R.string.status_pending;
                color = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
                break;
        }
        
        chip.setText(getString(statusResId));
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_report)
                .setMessage(R.string.are_you_sure)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    reportRef.removeValue().addOnSuccessListener(aVoid -> finish());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditDialog() {
        if (currentReport == null) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_report, null);
        EditText etDesc = view.findViewById(R.id.etEditDescription);
        AutoCompleteTextView autoCompleteTextView = view.findViewById(R.id.spinnerEditType);

        etDesc.setText(currentReport.getDescription());
        
        String[] types = {
                getString(R.string.type_pothole),
                getString(R.string.type_trash),
                getString(R.string.type_streetlight),
                getString(R.string.type_road),
                getString(R.string.type_other)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types);
        autoCompleteTextView.setAdapter(adapter);
        
        for (String type : types) {
            if (getEnglishType(type).equals(currentReport.getType())) {
                autoCompleteTextView.setText(type, false);
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_report)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("description", etDesc.getText().toString());
                    updates.put("type", getEnglishType(autoCompleteTextView.getText().toString()));
                    reportRef.updateChildren(updates);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getEnglishType(String localizedType) {
        if (localizedType.equals(getString(R.string.type_pothole))) return "Pothole";
        if (localizedType.equals(getString(R.string.type_trash))) return "Trash";
        if (localizedType.equals(getString(R.string.type_streetlight))) return "Broken streetlight";
        if (localizedType.equals(getString(R.string.type_road))) return "Damaged road";
        return "Other";
    }
}
