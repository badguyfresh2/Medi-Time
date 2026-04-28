package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meditime.model.Appointment;
import com.example.meditime.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatientDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvBloodGroup, tvAllergies, tvChronic, tvDob;
    private ImageView imgProfile;
    private RecyclerView rvHistory;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private String patientId, doctorId;
    private ValueEventListener historyListener;
    private final List<Appointment> historyList = new ArrayList<>();
    private DoctorAppointmentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        dbRef = FirebaseDatabase.getInstance().getReference();
        patientId = getIntent().getStringExtra("patientId");
        doctorId  = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        tvName      = findViewById(R.id.tvPatientName);
        tvEmail     = findViewById(R.id.tvEmail);
        tvPhone     = findViewById(R.id.tvPhone);
        tvBloodGroup= findViewById(R.id.tvBloodGroup);
        tvAllergies = findViewById(R.id.tvAllergies);
        tvChronic   = findViewById(R.id.tvChronic);
        tvDob       = findViewById(R.id.tvDob);
        imgProfile  = findViewById(R.id.imgProfile);
        rvHistory   = findViewById(R.id.rvAppointmentHistory);
        progressBar = findViewById(R.id.progressBar);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnChat = findViewById(R.id.btnChat);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("patientId", patientId);
                startActivity(intent);
            });
        }

        Button btnPrescribe = findViewById(R.id.btnPrescribe);
        if (btnPrescribe != null) {
            btnPrescribe.setOnClickListener(v -> {
                Intent intent = new Intent(this, DoctorPrescriptionsActivity.class);
                intent.putExtra("patientId", patientId);
                startActivity(intent);
            });
        }

        adapter = new DoctorAppointmentAdapter(historyList, a -> {
            Intent i = new Intent(this, ViewNotesActivity.class);
            i.putExtra("appointmentId", a.getAppointmentId());
            startActivity(i);
        }, a -> {
            Intent i = new Intent(this, DoctorPrescriptionsActivity.class);
            i.putExtra("appointmentId", a.getAppointmentId());
            i.putExtra("patientId", a.getPatientId());
            i.putExtra("patientName", a.getPatientName());
            startActivity(i);
        });

        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            rvHistory.setAdapter(adapter);
        }

        if (patientId != null) { 
            loadPatientData(); 
            loadAppointmentHistory(); 
        }
    }

    private void loadPatientData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        dbRef.child("users").child(patientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                User u = snapshot.getValue(User.class);
                if (u == null) return;
                if (tvName != null)       tvName.setText(u.getName() != null ? u.getName() : "Patient");
                if (tvEmail != null)      tvEmail.setText(u.getEmail() != null ? u.getEmail() : "N/A");
                if (tvPhone != null)      tvPhone.setText(u.getPhone() != null ? u.getPhone() : "N/A");
                if (tvBloodGroup != null) tvBloodGroup.setText(u.getBloodGroup() != null ? u.getBloodGroup() : "N/A");
                if (tvAllergies != null)  tvAllergies.setText(u.getAllergies() != null ? u.getAllergies() : "None");
                if (tvChronic != null)    tvChronic.setText(u.getChronicConditions() != null ? u.getChronicConditions() : "None");
                if (tvDob != null)        tvDob.setText(u.getDateOfBirth() != null ? u.getDateOfBirth() : "N/A");
                if (imgProfile != null && u.getProfileImageUrl() != null && !u.getProfileImageUrl().isEmpty())
                    Glide.with(PatientDetailsActivity.this).load(u.getProfileImageUrl()).circleCrop().into(imgProfile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadAppointmentHistory() {
        Query query = dbRef.child("appointments").orderByChild("patientId").equalTo(patientId);
        historyListener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Appointment a = doc.getValue(Appointment.class);
                    if (a != null && doctorId.equals(a.getDoctorId())) {
                        a.setAppointmentId(doc.getKey());
                        historyList.add(a);
                    }
                }
                Collections.sort(historyList, (a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyListener != null && dbRef != null) {
            dbRef.child("appointments").removeEventListener(historyListener);
        }
    }
}
