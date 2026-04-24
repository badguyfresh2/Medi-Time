package com.example.meditime;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.DiagnosticReport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class DiagnosticActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private DatabaseReference dbRef;
    private ValueEventListener listener;
    private final List<DiagnosticReport> reports = new ArrayList<>();
    private DiagnosticAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostic);

        dbRef = FirebaseDatabase.getInstance().getReference();

        recyclerView = findViewById(R.id.rvReports);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new DiagnosticAdapter(reports, report -> {
            if (report.getFileUrl() != null && !report.getFileUrl().isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(report.getFileUrl())));
            }
        });

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }

        loadReports();
    }

    private void loadReports() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                reports.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    DiagnosticReport r = child.getValue(DiagnosticReport.class);
                    if (r != null && uid.equals(r.getPatientId())) {
                        r.setReportId(child.getKey());
                        reports.add(r);
                    }
                }
                Collections.sort(reports, (a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                adapter.notifyDataSetChanged();
                if (tvEmpty != null) tvEmpty.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        };

        dbRef.child("diagnostics").addValueEventListener(listener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) dbRef.child("diagnostics").removeEventListener(listener);
    }
}