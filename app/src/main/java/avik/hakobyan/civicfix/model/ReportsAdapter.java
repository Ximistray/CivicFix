package avik.hakobyan.civicfix.model;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.ui.report.ReportDetailActivity;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ViewHolder> {

    private final Context context;
    private final List<Problem> list;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final String currentUid;
    private final DatabaseReference dbRef;

    public ReportsAdapter(Context context, List<Problem> list) {
        this.context = context;
        this.list = list;
        this.currentUid = FirebaseAuth.getInstance().getUid();
        this.dbRef = FirebaseDatabase.getInstance().getReference("problems");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Problem report = list.get(position);

        holder.textType.setText(context.getString(getResIdForType(report.getType())));
        holder.textDescription.setText(report.getDescription());
        holder.textDate.setText(dateFormat.format(new Date(report.getTimestamp())));

        setStatusChip(holder.chipStatus, report.getStatus());

        Glide.with(context)
                .load(report.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.background_gradient)
                .error(R.drawable.background_gradient)
                .into(holder.imageReport);

        // Voting Logic
        holder.tvVoteCount.setText(String.valueOf(report.getVoteCount()));
        boolean hasVoted = currentUid != null && report.getVotedUsers() != null && report.getVotedUsers().containsKey(currentUid);
        
        if (hasVoted) {
            holder.ivVote.setImageResource(R.drawable.ic_heart_filled);
            holder.ivVote.setColorFilter(ContextCompat.getColor(context, R.color.like_red));
            holder.tvVoteCount.setTextColor(ContextCompat.getColor(context, R.color.like_red));
        } else {
            holder.ivVote.setImageResource(R.drawable.ic_heart_outline);
            holder.ivVote.setColorFilter(ContextCompat.getColor(context, R.color.slate_grey));
            holder.tvVoteCount.setTextColor(ContextCompat.getColor(context, R.color.slate_grey));
        }

        holder.llVote.setOnClickListener(v -> {
            animateHeart(holder.ivVote);
            toggleVote(report);
        });

        // Solved Confirmation Logic
        if ("solved".equalsIgnoreCase(report.getStatus())) {
            holder.btnMarkSolved.setVisibility(View.GONE);
        } else {
            holder.btnMarkSolved.setVisibility(View.VISIBLE);
            boolean hasConfirmedSolved = currentUid != null && report.getSolvedConfirmations() != null && report.getSolvedConfirmations().containsKey(currentUid);
            if (hasConfirmedSolved) {
                holder.btnMarkSolved.setText(R.string.confirmed_solved);
                holder.btnMarkSolved.setEnabled(false);
                holder.btnMarkSolved.setAlpha(0.6f);
            } else {
                holder.btnMarkSolved.setText(R.string.mark_as_solved);
                holder.btnMarkSolved.setEnabled(true);
                holder.btnMarkSolved.setAlpha(1.0f);
            }
        }

        holder.btnMarkSolved.setOnClickListener(v -> confirmSolved(report));

        // Show edit button only for user's own reports
        if (currentUid != null && currentUid.equals(report.getUserId())) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> showEditDialog(report));
        } else {
            holder.btnEdit.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReportDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            context.startActivity(intent);
        });
    }

    private void confirmSolved(Problem report) {
        if (currentUid == null) {
            Toast.makeText(context, R.string.login_to_like, Toast.LENGTH_SHORT).show();
            return;
        }

        dbRef.child(report.getId()).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Problem p = mutableData.getValue(Problem.class);
                if (p == null) return Transaction.success(mutableData);

                if (p.solvedConfirmations == null) p.solvedConfirmations = new HashMap<>();
                
                if (!p.solvedConfirmations.containsKey(currentUid)) {
                    p.solvedConfirmations.put(currentUid, true);
                    
                    if (p.solvedConfirmations.size() > 3) {
                        p.setStatus("solved");
                    }
                }

                mutableData.setValue(p);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed) {
                    Toast.makeText(context, R.string.confirmed_solved, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void animateHeart(View view) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(0.7f, 1.0f, 0.7f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(200);
        view.startAnimation(scaleAnimation);
    }

    private void toggleVote(Problem report) {
        if (currentUid == null) {
            Toast.makeText(context, R.string.login_to_like, Toast.LENGTH_SHORT).show();
            return;
        }

        dbRef.child(report.getId()).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
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
                context.getString(R.string.type_graffiti),
                context.getString(R.string.type_water),
                context.getString(R.string.type_parking),
                context.getString(R.string.type_sidewalk),
                context.getString(R.string.type_sign),
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
        if (localizedType.equals(context.getString(R.string.type_graffiti))) return "Graffiti";
        if (localizedType.equals(context.getString(R.string.type_water))) return "Water Leak";
        if (localizedType.equals(context.getString(R.string.type_parking))) return "Illegal Parking";
        if (localizedType.equals(context.getString(R.string.type_sidewalk))) return "Sidewalk Damage";
        if (localizedType.equals(context.getString(R.string.type_sign))) return "Damaged Sign";
        return "Other";
    }

    private int getResIdForType(String type) {
        if (type == null) return R.string.type_other;
        switch (type) {
            case "Pothole": return R.string.type_pothole;
            case "Trash": return R.string.type_trash;
            case "Broken streetlight": return R.string.type_streetlight;
            case "Damaged road": return R.string.type_road;
            case "Graffiti": return R.string.type_graffiti;
            case "Water Leak": return R.string.type_water;
            case "Illegal Parking": return R.string.type_parking;
            case "Sidewalk Damage": return R.string.type_sidewalk;
            case "Damaged Sign": return R.string.type_sign;
            default: return R.string.type_other;
        }
    }

    private void setStatusChip(Chip chip, String status) {
        if (status == null) status = "pending";
        
        int statusResId;
        int color;
        
        switch (status.toLowerCase()) {
            case "solved":
                statusResId = R.string.status_solved;
                color = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                break;
            case "resolved":
                statusResId = R.string.status_resolved;
                color = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                break;
            case "in_progress":
                statusResId = R.string.status_in_progress;
                color = ContextCompat.getColor(context, android.R.color.holo_blue_dark);
                break;
            default:
                statusResId = R.string.status_pending;
                color = ContextCompat.getColor(context, android.R.color.holo_orange_dark);
                break;
        }
        
        chip.setText(context.getString(statusResId));
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageReport, ivVote;
        TextView textType, textDescription, textDate, tvVoteCount;
        Chip chipStatus;
        ImageButton btnEdit;
        LinearLayout llVote;
        Button btnMarkSolved;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageReport = itemView.findViewById(R.id.imageReport);
            textType = itemView.findViewById(R.id.textType);
            textDescription = itemView.findViewById(R.id.textDescription);
            textDate = itemView.findViewById(R.id.textDate);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnEdit = itemView.findViewById(R.id.btnEditReportItem);
            llVote = itemView.findViewById(R.id.llVote);
            ivVote = itemView.findViewById(R.id.ivVote);
            tvVoteCount = itemView.findViewById(R.id.tvVoteCount);
            btnMarkSolved = itemView.findViewById(R.id.btnMarkSolved);
        }
    }
}
