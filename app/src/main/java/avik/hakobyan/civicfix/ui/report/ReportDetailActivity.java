package avik.hakobyan.civicfix.ui.report;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Problem;

public class ReportDetailActivity extends AppCompatActivity {

    private ImageView detailImage;
    private TextView detailType, detailDescription, detailLocation, detailDate;
    private Chip detailStatusChip;
    private View layoutActions;
    private DatabaseReference reportRef;
    private String reportId;
    private Problem currentReport;

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
        loadReportDetails();
    }

    private void initViews() {
        detailImage = findViewById(R.id.detailImage);
        detailType = findViewById(R.id.detailType);
        detailDescription = findViewById(R.id.detailDescription);
        detailLocation = findViewById(R.id.detailLocation);
        detailDate = findViewById(R.id.detailDate);
        detailStatusChip = findViewById(R.id.detailStatusChip);
        layoutActions = findViewById(R.id.layoutActions);

        View btnDelete = findViewById(R.id.btnDelete);
        View btnEdit = findViewById(R.id.btnEdit);

        if (btnDelete != null) btnDelete.setOnClickListener(v -> confirmDelete());
        if (btnEdit != null) btnEdit.setOnClickListener(v -> showEditDialog());
    }

    private void loadReportDetails() {
        reportRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentReport = snapshot.getValue(Problem.class);
                if (currentReport != null) {
                    updateUI(currentReport);
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

        Glide.with(this)
                .load(report.getImageUrl())
                .placeholder(R.drawable.background_gradient)
                .error(R.drawable.ic_launcher_background)
                .into(detailImage);

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null && userId.equals(report.getUserId())) {
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
        Spinner spinner = view.findViewById(R.id.spinnerEditType);

        etDesc.setText(currentReport.getDescription());
        
        String[] types = {
                getString(R.string.type_pothole),
                getString(R.string.type_trash),
                getString(R.string.type_streetlight),
                getString(R.string.type_road),
                getString(R.string.type_other)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinner.setAdapter(adapter);
        
        // Set selection based on English value
        for (int i = 0; i < types.length; i++) {
            if (getEnglishType(types[i]).equals(currentReport.getType())) {
                spinner.setSelection(i);
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_report)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("description", etDesc.getText().toString());
                    updates.put("type", getEnglishType(spinner.getSelectedItem().toString()));
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
