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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Doctor's patient list.
 *
 * NEW per-patient action buttons:
 *   • "View Notes"   → ViewNotesActivity  (existing)
 *   • "Diagnostics"  → DoctorDiagnosticActivity  (NEW)
 *   • "Records"      → DoctorMedicalRecordsActivity  (NEW)
 */
public class DoctorPatientsActivity extends AppCompatActivity {

    private RecyclerView rvPatients;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private EditText etSearch;
    private PatientAdapter adapter;
    private DatabaseReference dbRef;
    private String doctorId;
    private final List<com.example.meditime.model.Patient> allPatients = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patients);

        dbRef    = FirebaseDatabase.getInstance().getReference();
        doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        rvPatients  = findViewById(R.id.rvPatients);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);
        etSearch    = findViewById(R.id.etSearch);

        // Build adapter with three action callbacks
        adapter = new PatientAdapter(
            allPatients,
            // PRIMARY tap → PatientDetailsActivity
            patient -> {
                Intent intent = new Intent(this, PatientDetailsActivity.class);
                intent.putExtra("patientId",   patient.getPatientId());
                intent.putExtra("patientName", patient.getName());
                startActivity(intent);
            },
            // DIAGNOSTICS button
            patient -> {
                Intent intent = new Intent(this, DoctorDiagnosticActivity.class);
                intent.putExtra("patientId",   patient.getPatientId());
                intent.putExtra("patientName", patient.getName());
                startActivity(intent);
            },
            // RECORDS button
            patient -> {
                Intent intent = new Intent(this, DoctorMedicalRecordsActivity.class);
                intent.putExtra("patientId",   patient.getPatientId());
                intent.putExtra("patientName", patient.getName());
                startActivity(intent);
            }
        );

        if (rvPatients != null) {
            rvPatients.setLayoutManager(new LinearLayoutManager(this));
            rvPatients.setAdapter(adapter);
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    adapter.filter(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        loadPatients();
    }

    private void loadPatients() {
        if (doctorId.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Load all appointments for this doctor, collect unique patient IDs
        dbRef.child("appointments").orderByChild("doctorId").equalTo(doctorId)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Collect unique patient IDs
                        java.util.Set<String> patientIds = new java.util.LinkedHashSet<>();
                        java.util.Map<String, String> patientNames = new java.util.HashMap<>();
                        for (DataSnapshot doc : snapshot.getChildren()) {
                            String pid  = doc.child("patientId").getValue(String.class);
                            String pName= doc.child("patientName").getValue(String.class);
                            if (pid != null && !pid.isEmpty()) {
                                patientIds.add(pid);
                                if (pName != null) patientNames.put(pid, pName);
                            }
                        }

                        allPatients.clear();
                        for (String pid : patientIds) {
                            com.example.meditime.model.Patient p = new com.example.meditime.model.Patient();
                            p.setPatientId(pid);
                            p.setName(patientNames.getOrDefault(pid, "Patient"));
                            allPatients.add(p);
                        }

                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                        if (tvEmpty != null)
                            tvEmpty.setVisibility(allPatients.isEmpty() ? View.VISIBLE : View.GONE);

                        // Enrich with full profile data from /users/{patientId}
                        for (com.example.meditime.model.Patient p : allPatients) {
                            enrichPatientProfile(p);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void enrichPatientProfile(com.example.meditime.model.Patient p) {
        dbRef.child("users").child(p.getPatientId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        String name  = snapshot.child("name").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String blood = snapshot.child("bloodType").getValue(String.class);
                        String img   = snapshot.child("profileImageUrl").getValue(String.class);

                        if (name  != null) p.setName(name);
                        if (email != null) p.setEmail(email);
                        if (phone != null) p.setPhone(phone);
                        if (blood != null) p.setBloodType(blood);
                        if (img   != null) p.setProfileImageUrl(img);

                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
