package avik.hakobyan.civicfix.ui.report;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Comment;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private final Context context;
    private final List<Comment> comments;
    private final OnCommentActionListener actionListener;
    private final boolean isAdmin;
    private final String currentUserId;

    public interface OnCommentActionListener {
        void onReplyClick(Comment comment);
        void onEditClick(Comment comment);
        void onDeleteClick(Comment comment);
        void onLikeClick(Comment comment);
    }

    public CommentsAdapter(Context context, List<Comment> comments, boolean isAdmin, OnCommentActionListener actionListener) {
        this.context = context;
        this.comments = comments;
        this.isAdmin = isAdmin;
        this.actionListener = actionListener;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);

        holder.tvUserName.setText(comment.getUserName());
        holder.tvText.setText(comment.getText());
        
        String timeAgo = DateUtils.getRelativeTimeSpanString(comment.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
        holder.tvTime.setText(timeAgo);

        Glide.with(context)
                .load(comment.getUserProfileUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(holder.ivUser);

        // Limit replying to comments at depth 2 to prevent deeper hidden threads
        if (comment.getDepth() < 2) {
            holder.btnReply.setVisibility(View.VISIBLE);
            holder.btnReply.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onReplyClick(comment);
                }
            });
        } else {
            holder.btnReply.setVisibility(View.GONE);
        }

        // Like logic
        holder.tvLikeCount.setText(String.valueOf(comment.getLikeCount()));
        boolean isLiked = currentUserId != null && comment.getLikedUsers() != null && comment.getLikedUsers().containsKey(currentUserId);
        
        if (isLiked) {
            holder.ivLike.setImageResource(R.drawable.ic_heart_filled);
            holder.ivLike.setColorFilter(ContextCompat.getColor(context, R.color.like_red));
            holder.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.like_red));
        } else {
            holder.ivLike.setImageResource(R.drawable.ic_heart_outline);
            holder.ivLike.setColorFilter(ContextCompat.getColor(context, R.color.slate_grey));
            holder.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.slate_grey));
        }

        holder.btnLike.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onLikeClick(comment);
            }
        });

        if (isAdmin || (currentUserId != null && currentUserId.equals(comment.getUserId()))) {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.btnMore.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(context, holder.btnMore);
                if (currentUserId != null && currentUserId.equals(comment.getUserId())) {
                    popup.getMenu().add(0, 1, 0, R.string.edit);
                }
                popup.getMenu().add(0, 2, 1, R.string.delete);
                
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        actionListener.onEditClick(comment);
                    } else if (item.getItemId() == 2) {
                        actionListener.onDeleteClick(comment);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.btnMore.setVisibility(View.GONE);
        }

        // Add indentation based on depth (max 2 levels)
        int paddingStart = comment.getDepth() * 48; // 48dp per level
        holder.itemView.setPadding(paddingStart, holder.itemView.getPaddingTop(), holder.itemView.getPaddingRight(), holder.itemView.getPaddingBottom());
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUser, btnMore, ivLike;
        TextView tvUserName, tvText, tvTime, btnReply, tvLikeCount;
        View btnLike;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUser = itemView.findViewById(R.id.ivCommentUser);
            btnMore = itemView.findViewById(R.id.btnCommentMore);
            tvUserName = itemView.findViewById(R.id.tvCommentUserName);
            tvText = itemView.findViewById(R.id.tvCommentText);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            btnReply = itemView.findViewById(R.id.btnReply);
            ivLike = itemView.findViewById(R.id.ivLikeComment);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCommentCount);
            btnLike = itemView.findViewById(R.id.btnLikeComment);
        }
    }
}
