package avik.hakobyan.civicfix.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.AdminReportsAdapter;
import avik.hakobyan.civicfix.model.Problem;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminReportsAdapter adapter;
    private List<Problem> reportList;
    private DatabaseReference dbRef;
    private ProgressBar progressBar;
    private LinearLayout llEmpty;
    private TabLayout tabLayout;
    private boolean showingPending = true;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        llEmpty = findViewById(R.id.llEmpty);
        tabLayout = findViewById(R.id.adminTabs);
        recyclerView = findViewById(R.id.rvAdminReports);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportList = new ArrayList<>();
        adapter = new AdminReportsAdapter(this, reportList);
        recyclerView.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance().getReference("problems");

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingPending = tab.getPosition() == 0;
                loadReports();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadReports();
    }

    private void loadReports() {
        progressBar.setVisibility(View.VISIBLE);
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reportList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Problem problem = data.getValue(Problem.class);
                    if (problem != null) {
                        problem.setId(data.getKey());
                        if (showingPending) {
                            if (!problem.isVerified()) {
                                reportList.add(problem);
                            }
                        } else {
                            reportList.add(problem);
                        }
                    }
                }
                // Sort by voteCount descending, then by timestamp descending
                Collections.sort(reportList, (p1, p2) -> {
                    int voteCompare = Integer.compare(p2.getVoteCount(), p1.getVoteCount());
                    if (voteCompare != 0) return voteCompare;
                    return Long.compare(p2.getTimestamp(), p1.getTimestamp());
                });
                
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                llEmpty.setVisibility(reportList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
