package avik.hakobyan.civicfix.model;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.ui.report.ReportDetailActivity;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ViewHolder> {

    private final Context context;
    private final List<Problem> list;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public ReportsAdapter(Context context, List<Problem> list) {
        this.context = context;
        this.list = list;
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

        // Display translated type
        holder.textType.setText(context.getString(getResIdForType(report.getType())));
        holder.textDescription.setText(report.getDescription());
        holder.textDate.setText(dateFormat.format(new Date(report.getTimestamp())));

        setStatusChip(holder.chipStatus, report.getStatus());

        Glide.with(context)
                .load(report.getImageUrl())
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.background_gradient)
                .error(R.drawable.background_gradient)
                .into(holder.imageReport);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReportDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            context.startActivity(intent);
        });
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
        ImageView imageReport;
        TextView textType, textDescription, textDate;
        Chip chipStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageReport = itemView.findViewById(R.id.imageReport);
            textType = itemView.findViewById(R.id.textType);
            textDescription = itemView.findViewById(R.id.textDescription);
            textDate = itemView.findViewById(R.id.textDate);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }
}
