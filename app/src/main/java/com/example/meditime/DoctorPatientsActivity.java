package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class DoctorPatientsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTotal;
    private EditText etSearch;
    private DatabaseReference dbRef;
    private String doctorId;
    private final List<User> allPatients  = new ArrayList<>();
    private final List<User> showPatients = new ArrayList<>();
    private PatientAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patients);

        dbRef = FirebaseDatabase.getInstance().getReference();
        doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        recyclerView = findViewById(R.id.rvPatients);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);
        tvTotal      = findViewById(R.id.tvTotal);
        etSearch     = findViewById(R.id.etSearch);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new PatientAdapter(showPatients, patient -> {
            Intent intent = new Intent(this, PatientDetailsActivity.class);
            intent.putExtra("patientId", patient.getUserId());
            startActivity(intent);
        });
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterPatients(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        loadPatients();
    }

    private void loadPatients() {
        if (doctorId.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        dbRef.child("appointments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                Set<String> patientIds = new HashSet<>();
                for (DataSnapshot child : snap.getChildren()) {
                    String pid = child.child("patientId").getValue(String.class);
                    if (pid != null) patientIds.add(pid);
                }

                if (patientIds.isEmpty()) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    return;
                }

                final int[] loaded = {0};
                final int total = patientIds.size();
                allPatients.clear();

                for (String pid : patientIds) {
                    dbRef.child("users").child(pid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                            if (userSnap.exists()) {
                                User u = userSnap.getValue(User.class);
                                if (u != null) {
                                    if (u.getUserId() == null) u.setUserId(userSnap.getKey());
                                    allPatients.add(u);
                                }
                            }
                            loaded[0]++;
                            if (loaded[0] == total) {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                filterPatients("");
                                if (tvTotal != null) tvTotal.setText(allPatients.size() + " Total");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loaded[0]++;
                            if (loaded[0] == total && progressBar != null)
                                progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void filterPatients(String query) {
        showPatients.clear();
        for (User u : allPatients) {
            if (query.isEmpty() || (u.getName() != null && u.getName().toLowerCase().contains(query.toLowerCase()))) {
                showPatients.add(u);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) tvEmpty.setVisibility(showPatients.isEmpty() ? View.VISIBLE : View.GONE);
    }
}