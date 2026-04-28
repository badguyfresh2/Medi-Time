package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.Doctor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvDoctors;
    private TextView tvUserName;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private final List<Doctor> doctors = new ArrayList<>();
    private HomeDoctorAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);

        LinearLayout serviceDoctor = findViewById(R.id.service_doctor);
        LinearLayout serviceHospital = findViewById(R.id.service_hospital);
        LinearLayout servicePharmacy = findViewById(R.id.service_pharmacy);
        LinearLayout serviceAmbulance = findViewById(R.id.service_ambulance);
        LinearLayout serviceDiagnostic = findViewById(R.id.service_diagnostic);
        LinearLayout serviceReminder = findViewById(R.id.service_reminder);
        LinearLayout servicePrescription = findViewById(R.id.service_prescription);
        LinearLayout serviceRecords = findViewById(R.id.service_records);

        if (serviceDoctor != null) serviceDoctor.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DoctorActivity.class)));
        if (serviceHospital != null) serviceHospital.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, HospitalActivity.class)));
        if (servicePharmacy != null) servicePharmacy.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PharmacyActivity.class)));
        if (serviceAmbulance != null) serviceAmbulance.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AmbulanceActivity.class)));
        if (serviceDiagnostic != null) serviceDiagnostic.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DiagnosticActivity.class)));
        if (serviceReminder != null) serviceReminder.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ReminderActivity.class)));
        if (servicePrescription != null) servicePrescription.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PrescriptionActivity.class)));
        if (serviceRecords != null) serviceRecords.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, MedicalRecordsActivity.class)));

        TextView btnOrderNow = findViewById(R.id.btnOrderNow);
        if (btnOrderNow != null) btnOrderNow.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DoctorActivity.class)));

        RelativeLayout search = findViewById(R.id.search_bar);
        if (search != null) search.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DoctorActivity.class)));

        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        tvUserName  = findViewById(R.id.tvUserName);
        rvDoctors   = findViewById(R.id.rvDoctors);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        LinearLayout navDoctor    = findViewById(R.id.nav_doctor);
        LinearLayout navAmbulance = findViewById(R.id.nav_ambulance);
        LinearLayout navProfile   = findViewById(R.id.nav_profile);
        if (navDoctor != null) navDoctor.setOnClickListener(v -> startActivity(new Intent(this, DoctorActivity.class)));
        if (navAmbulance != null) navAmbulance.setOnClickListener(v -> startActivity(new Intent(this, AmbulanceActivity.class)));
        if (navProfile != null) navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        if (mAuth.getCurrentUser() != null) {
            dbRef.child("users").child(mAuth.getCurrentUser().getUid()).child("name")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot ds) {
                            String name = ds.getValue(String.class);
                            if (tvUserName != null)
                                tvUserName.setText(getString(R.string.hello_prefix, (name != null ? name : "there")));
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        adapter = new HomeDoctorAdapter(doctors, doctor -> {
            Intent intent = new Intent(this, DoctorDetailActivity.class);
            intent.putExtra("doctorId", doctor.getUserId());
            startActivity(intent);
        });
        if (rvDoctors != null) {
            rvDoctors.setLayoutManager(new LinearLayoutManager(this));
            rvDoctors.setAdapter(adapter);
        }

        loadDoctors();
    }

    private void loadDoctors() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        dbRef.child("doctors").limitToFirst(20).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                doctors.clear();
                for (DataSnapshot child : snap.getChildren()) {
                    Doctor d = child.getValue(Doctor.class);
                    if (d != null) {
                        if (d.getUserId() == null) d.setUserId(child.getKey());
                        doctors.add(d);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }
}