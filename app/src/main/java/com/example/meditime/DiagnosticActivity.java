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

/**
 * Patient-facing Diagnostic screen.
 * Shows the patient their own diagnostic reports AND any notes the doctor has added.
 */
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

        dbRef = FirebaseDatabase.getInstance().getReference("diagnostics");

        recyclerView = findViewById(R.id.rvReports);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new DiagnosticAdapter(reports, report -> {
            if (report.getFileUrl() != null && !report.getFileUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(report.getFileUrl()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "No file attached to this report", Toast.LENGTH_SHORT).show();
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
        if (uid.isEmpty()) {
            if (tvEmpty != null) {
                tvEmpty.setText("Please log in to view your reports.");
                tvEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Listen to reports where patientId == uid (reports uploaded by doctor for this patient)
        Query query = dbRef.orderByChild("patientId").equalTo(uid);

        listener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                reports.clear();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    DiagnosticReport r = doc.getValue(DiagnosticReport.class);
                    if (r != null) {
                        r.setReportId(doc.getKey());
                        reports.add(r);
                    }
                }
                // Sort newest first by createdAt timestamp
                reports.sort((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });

                adapter.notifyDataSetChanged();
                if (tvEmpty != null)
                    tvEmpty.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
                if (tvEmpty != null && reports.isEmpty())
                    tvEmpty.setText("No diagnostic reports found.\nYour doctor will add reports after your consultation.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(DiagnosticActivity.this,
                        "Error loading reports: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) dbRef.removeEventListener(listener);
    }
}
