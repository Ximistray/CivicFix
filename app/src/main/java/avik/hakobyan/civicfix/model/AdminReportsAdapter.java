package avik.hakobyan.civicfix.model;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.ui.report.ReportDetailActivity;

public class AdminReportsAdapter extends RecyclerView.Adapter<AdminReportsAdapter.ViewHolder> {

    private Context context;
    private List<Problem> reports;
    private DatabaseReference dbRef;
    private String currentUid;

    public AdminReportsAdapter(Context context, List<Problem> reports) {
        this.context = context;
        this.reports = reports;
        this.dbRef = FirebaseDatabase.getInstance().getReference("problems");
        this.currentUid = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Problem report = reports.get(position);

        holder.tvType.setText(report.getType());
        holder.tvDescription.setText(report.getDescription());
        holder.tvVoteCount.setText(String.valueOf(report.getVoteCount()));
        
        // Show liked state even in admin panel for consistency
        boolean hasLiked = currentUid != null && report.getVotedUsers() != null && report.getVotedUsers().containsKey(currentUid);
        if (hasLiked) {
            holder.ivHeart.setColorFilter(0xFFEF4444);
            holder.tvVoteCount.setTextColor(0xFFEF4444);
        } else {
            holder.ivHeart.setColorFilter(0xFF64748B);
            holder.tvVoteCount.setTextColor(0xFF64748B);
        }

        // Format timestamp
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(report.getTimestamp());
        String date = DateFormat.format("dd MMM yyyy, HH:mm", cal).toString();
        holder.tvTimestamp.setText(date);

        Glide.with(context)
                .load(report.getImageUrl())
                .placeholder(R.drawable.background_gradient)
                .into(holder.ivImage);

        if (report.isVerified()) {
            holder.btnApprove.setVisibility(View.GONE);
            holder.tvStatusBadge.setText(context.getString(R.string.admin_verified));
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        } else {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(context.getString(R.string.status_pending));
            holder.tvStatusBadge.setTextColor(0xFFF59E0B); // Amber/Orange
        }

        holder.btnApprove.setOnClickListener(v -> {
            dbRef.child(report.getId()).child("verified").setValue(true)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, context.getString(R.string.admin_report_approved), Toast.LENGTH_SHORT).show());
        });

        holder.btnEdit.setOnClickListener(v -> showEditDialog(report));

        holder.btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.delete_report)
                    .setMessage(R.string.are_you_sure)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        dbRef.child(report.getId()).removeValue()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, context.getString(R.string.admin_report_deleted), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReportDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            context.startActivity(intent);
        });
    }

    private void showEditDialog(Problem report) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_report, null);
        EditText etDesc = view.findViewById(R.id.etEditDescription);
        AutoCompleteTextView autoCompleteTextView = view.findViewById(R.id.spinnerEditType);

        etDesc.setText(report.getDescription());
        
        String[] types = {
                context.getString(R.string.type_pothole),
                context.getString(R.string.type_trash),
                context.getString(R.string.type_streetlight),
                context.getString(R.string.type_road),
                context.getString(R.string.type_other)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, types);
        autoCompleteTextView.setAdapter(adapter);
        
        for (String type : types) {
            if (getEnglishType(type).equals(report.getType())) {
                autoCompleteTextView.setText(type, false);
                break;
            }
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.edit_report)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("description", etDesc.getText().toString());
                    updates.put("type", getEnglishType(autoCompleteTextView.getText().toString()));
                    dbRef.child(report.getId()).updateChildren(updates);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getEnglishType(String localizedType) {
        if (localizedType.equals(context.getString(R.string.type_pothole))) return "Pothole";
        if (localizedType.equals(context.getString(R.string.type_trash))) return "Trash";
        if (localizedType.equals(context.getString(R.string.type_streetlight))) return "Broken streetlight";
        if (localizedType.equals(context.getString(R.string.type_road))) return "Damaged road";
        return "Other";
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivHeart;
        TextView tvType, tvDescription, tvStatusBadge, tvTimestamp, tvVoteCount;
        Button btnApprove, btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivAdminReportImage);
            ivHeart = itemView.findViewById(R.id.ivAdminHeart);
            tvType = itemView.findViewById(R.id.tvAdminReportType);
            tvDescription = itemView.findViewById(R.id.tvAdminReportDesc);
            tvStatusBadge = itemView.findViewById(R.id.tvAdminStatusBadge);
            tvTimestamp = itemView.findViewById(R.id.tvAdminTimestamp);
            tvVoteCount = itemView.findViewById(R.id.tvAdminVoteCount);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnEdit = itemView.findViewById(R.id.btnEditAdmin);
            btnDelete = itemView.findViewById(R.id.btnDeleteAdmin);
        }
    }
}
