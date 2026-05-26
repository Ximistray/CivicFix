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
import androidx.recyclerview.widget.LinearLayoutManager;
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
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Account;
import avik.hakobyan.civicfix.model.Comment;
import avik.hakobyan.civicfix.model.Notification;
import avik.hakobyan.civicfix.model.Problem;

public class ReportDetailActivity extends AppCompatActivity {

    private ImageView detailImage, ivVoteDetail;
    private TextView detailType, detailDescription, detailLocation, detailDate, tvVoteCountDetail;
    private Chip detailStatusChip;
    private View layoutActions;
    private DatabaseReference reportRef, commentsRef, userRef;
    private String reportId, currentUid;
    private Problem currentReport;
    private boolean isUserAdmin = false;
    private String lastImageUrl = "";

    // Comment variables
    private CommentsAdapter commentsAdapter;
    private List<Comment> commentList;
    private EditText etComment;
    private View replyIndicatorLayout;
    private TextView tvReplyTo;
    private Comment selectedReplyToComment = null;
    private Comment editingComment = null;
    private Account currentUserAccount;

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
        commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(reportId);
        
        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
            checkAdminStatus();
            loadCurrentUserAccount();
        }

        loadReportDetails();
        loadComments();
    }

    private void loadCurrentUserAccount() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserAccount = snapshot.getValue(Account.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkAdminStatus() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Account account = snapshot.getValue(Account.class);
                if (account != null) {
                    isUserAdmin = account.isAdmin();
                    updateActionVisibility();
                    setupCommentsSection(); // Refresh adapter with admin status
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
        
        View btnVoteDetail = findViewById(R.id.btnVoteDetail);
        ivVoteDetail = findViewById(R.id.ivVoteDetail);
        tvVoteCountDetail = findViewById(R.id.tvVoteCountDetail);

        View btnDelete = findViewById(R.id.btnDelete);
        View btnEdit = findViewById(R.id.btnEdit);

        if (btnDelete != null) btnDelete.setOnClickListener(v -> confirmDelete());
        if (btnEdit != null) btnEdit.setOnClickListener(v -> showEditDialog());
        
        if (btnVoteDetail != null) {
            btnVoteDetail.setOnClickListener(v -> toggleVote());
        }

        etComment = findViewById(R.id.etComment);
        View btnSendComment = findViewById(R.id.btnSendComment);
        replyIndicatorLayout = findViewById(R.id.replyIndicatorLayout);
        tvReplyTo = findViewById(R.id.tvReplyTo);
        findViewById(R.id.btnCancelReply).setOnClickListener(v -> cancelReplyOrEdit());

        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> sendOrUpdateComment());
        }

        setupCommentsSection();
    }

    private void setupCommentsSection() {
        RecyclerView rvComments = findViewById(R.id.rvComments);
        if (commentList == null) commentList = new ArrayList<>();
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter(this, commentList, isUserAdmin, new CommentsAdapter.OnCommentActionListener() {
            @Override
            public void onReplyClick(Comment comment) {
                cancelReplyOrEdit();
                selectedReplyToComment = comment;
                replyIndicatorLayout.setVisibility(View.VISIBLE);
                tvReplyTo.setText(getString(R.string.replying_to, comment.getUserName()));
                etComment.requestFocus();
            }

            @Override
            public void onEditClick(Comment comment) {
                cancelReplyOrEdit();
                editingComment = comment;
                replyIndicatorLayout.setVisibility(View.VISIBLE);
                tvReplyTo.setText(getString(R.string.edit_comment));
                etComment.setText(comment.getText());
                etComment.requestFocus();
                etComment.setSelection(etComment.getText().length());
            }

            @Override
            public void onDeleteClick(Comment comment) {
                confirmDeleteComment(comment);
            }

            @Override
            public void onLikeClick(Comment comment) {
                toggleCommentLike(comment);
            }
        });
        rvComments.setAdapter(commentsAdapter);
    }

    private void toggleCommentLike(Comment comment) {
        if (currentUid == null) {
            Toast.makeText(this, R.string.login_to_like, Toast.LENGTH_SHORT).show();
            return;
        }

        commentsRef.child(comment.getId()).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Comment c = mutableData.getValue(Comment.class);
                if (c == null) return Transaction.abort();

                Map<String, Boolean> likedUsers = c.getLikedUsers();
                if (likedUsers == null) likedUsers = new HashMap<>();

                if (likedUsers.containsKey(currentUid)) {
                    c.setLikeCount(c.getLikeCount() - 1);
                    likedUsers.remove(currentUid);
                } else {
                    c.setLikeCount(c.getLikeCount() + 1);
                    likedUsers.put(currentUid, true);
                }

                c.setLikedUsers(likedUsers);
                mutableData.setValue(c);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
            }
        });
    }

    private void confirmDeleteComment(Comment comment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_comment)
                .setMessage(R.string.delete_comment_msg)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    commentsRef.child(comment.getId()).removeValue();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void sendOrUpdateComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) return;
        if (currentUid == null || currentUserAccount == null) {
            Toast.makeText(this, "Please log in to comment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingComment != null) {
            commentsRef.child(editingComment.getId()).child("text").setValue(text)
                    .addOnSuccessListener(aVoid -> {
                        etComment.setText("");
                        cancelReplyOrEdit();
                    });
        } else {
            String commentId = commentsRef.push().getKey();
            if (commentId == null) return;

            Comment comment = new Comment(
                    commentId,
                    currentUid,
                    currentUserAccount.getName(),
                    currentUserAccount.getProfileImageUrl(),
                    text,
                    System.currentTimeMillis()
            );

            if (selectedReplyToComment != null) {
                comment.setParentCommentId(selectedReplyToComment.getId());
                comment.setDepth(selectedReplyToComment.getDepth() + 1);
            } else {
                comment.setDepth(0);
            }

            commentsRef.child(commentId).setValue(comment).addOnSuccessListener(aVoid -> {
                etComment.setText("");
                if (selectedReplyToComment != null) {
                    // Notify parent comment author if they're not the replier
                    if (!currentUid.equals(selectedReplyToComment.getUserId())) {
                        sendNotificationToUser(selectedReplyToComment.getUserId(), "replied to your comment: " + text);
                    }
                } else if (currentReport != null && !currentUid.equals(currentReport.getUserId())) {
                    // Notify report owner if they're not the commenter
                    sendNotificationToUser(currentReport.getUserId(), "commented: " + text);
                }
                cancelReplyOrEdit();
            });
        }
    }

    private void sendNotificationToUser(String targetUserId, String message) {
        DatabaseReference userSettingsRef = FirebaseDatabase.getInstance().getReference("users")
                .child(targetUserId).child("settings");
                
        userSettingsRef.child("commentNotifications").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean enabled = snapshot.getValue(Boolean.class);
                if (enabled == null || enabled) {
                    DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications")
                            .child(targetUserId);
                    String notifId = notifRef.push().getKey();
                    if (notifId != null) {
                        Notification notification = new Notification(
                                notifId,
                                currentUid,
                                currentUserAccount.getName(),
                                message,
                                reportId,
                                System.currentTimeMillis()
                        );
                        notifRef.child(notifId).setValue(notification);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cancelReplyOrEdit() {
        selectedReplyToComment = null;
        editingComment = null;
        replyIndicatorLayout.setVisibility(View.GONE);
        etComment.setText("");
    }

    private void loadComments() {
        commentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                List<Comment> topLevel = new ArrayList<>();
                Map<String, List<Comment>> repliesMap = new HashMap<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Comment comment = data.getValue(Comment.class);
                    if (comment != null && comment.getDepth() <= 2) {
                        if (comment.getParentCommentId() == null) {
                            topLevel.add(comment);
                        } else {
                            String parentId = comment.getParentCommentId();
                            if (!repliesMap.containsKey(parentId)) {
                                repliesMap.put(parentId, new ArrayList<>());
                            }
                            repliesMap.get(parentId).add(comment);
                        }
                    }
                }

                topLevel.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                for (Comment parent : topLevel) {
                    addCommentWithReplies(parent, repliesMap);
                }
                
                if (commentsAdapter != null) {
                    commentsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addCommentWithReplies(Comment parent, Map<String, List<Comment>> repliesMap) {
        commentList.add(parent);
        List<Comment> replies = repliesMap.get(parent.getId());
        if (replies != null) {
            replies.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
            for (Comment reply : replies) {
                addCommentWithReplies(reply, repliesMap);
            }
        }
    }

    private void toggleVote() {
        if (currentUid == null) {
            Toast.makeText(this, R.string.login_to_like, Toast.LENGTH_SHORT).show();
            return;
        }

        reportRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                if (mutableData.getValue() == null) {
                    return Transaction.abort();
                }

                Integer count = mutableData.child("voteCount").getValue(Integer.class);
                if (count == null) count = 0;

                @SuppressWarnings("unchecked")
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
                if (isFinishing() || isDestroyed()) return;
                Problem report = snapshot.getValue(Problem.class);
                if (report != null) {
                    currentReport = report;
                    currentReport.setId(snapshot.getKey());
                    updateUI(currentReport);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(ReportDetailActivity.this, "Error loading details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(Problem report) {
        if (isFinishing() || isDestroyed()) return;

        detailType.setText(getString(getResIdForType(report.getType())));
        detailDescription.setText(report.getDescription());
        detailLocation.setText(getString(R.string.location_lat_lon, report.getLatitude(), report.getLongitude()));
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        detailDate.setText(dateFormat.format(new Date(report.getTimestamp())));

        setStatusChip(detailStatusChip, report.getStatus());

        if (report.getImageUrl() != null && !report.getImageUrl().equals(lastImageUrl)) {
            lastImageUrl = report.getImageUrl();
            if (!isFinishing() && !isDestroyed()) {
                Glide.with(this)
                        .load(report.getImageUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.background_gradient)
                        .error(R.drawable.ic_launcher_background)
                        .into(detailImage);
            }
        }

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
        if (currentReport == null || isFinishing() || isDestroyed()) return;
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
                .setPositiveButton(R.string.delete, (dialog, which) -> reportRef.removeValue().addOnSuccessListener(aVoid -> finish()))
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
                getString(R.string.type_graffiti),
                getString(R.string.type_water),
                getString(R.string.type_parking),
                getString(R.string.type_sidewalk),
                getString(R.string.type_sign),
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
        if (localizedType.equals(getString(R.string.type_graffiti))) return "Graffiti";
        if (localizedType.equals(getString(R.string.type_water))) return "Water Leak";
        if (localizedType.equals(getString(R.string.type_parking))) return "Illegal Parking";
        if (localizedType.equals(getString(R.string.type_sidewalk))) return "Sidewalk Damage";
        if (localizedType.equals(getString(R.string.type_sign))) return "Damaged Sign";
        return "Other";
    }
}
