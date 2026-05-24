package avik.hakobyan.civicfix.ui.main;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Notification;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private final Context context;
    private final List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationsAdapter(Context context, List<Notification> notifications, OnNotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.tvTitle.setText(notification.getFromUserName());
        holder.tvText.setText(notification.getText());
        
        String timeAgo = DateUtils.getRelativeTimeSpanString(notification.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
        holder.tvTime.setText(timeAgo);

        holder.unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvText, tvTime;
        View unreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvText = itemView.findViewById(R.id.tvNotificationText);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
}
