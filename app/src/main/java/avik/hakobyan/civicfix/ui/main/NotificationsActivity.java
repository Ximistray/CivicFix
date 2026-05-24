package avik.hakobyan.civicfix.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Notification;
import avik.hakobyan.civicfix.ui.report.ReportDetailActivity;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationsAdapter adapter;
    private List<Notification> notificationList;
    private DatabaseReference notificationsRef;
    private String currentUid;
    private TextView tvNoNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        notificationList = new ArrayList<>();
        currentUid = FirebaseAuth.getInstance().getUid();

        if (currentUid == null) {
            finish();
            return;
        }

        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(currentUid);
        
        setupRecyclerView();
        loadNotifications();
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationsAdapter(this, notificationList, notification -> {
            // Mark as read
            notificationsRef.child(notification.getId()).child("read").setValue(true);
            
            // Navigate to report
            Intent intent = new Intent(NotificationsActivity.this, ReportDetailActivity.class);
            intent.putExtra("reportId", notification.getReportId());
            startActivity(intent);
        });
        rvNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Notification notification = data.getValue(Notification.class);
                    if (notification != null) {
                        notification.setId(data.getKey());
                        notificationList.add(notification);
                    }
                }
                Collections.sort(notificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();
                tvNoNotifications.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
