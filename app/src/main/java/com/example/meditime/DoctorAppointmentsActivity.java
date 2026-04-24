package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class DoctorAppointmentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private DatabaseReference dbRef;
    private String doctorId;
    private ValueEventListener listener;
    private final List<Appointment> allList  = new ArrayList<>();
    private final List<Appointment> showList = new ArrayList<>();
    private DoctorAppointmentAdapter adapter;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointments);

        dbRef = FirebaseDatabase.getInstance().getReference();
        doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        recyclerView = findViewById(R.id.rvAppointments);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tabAll       = findViewById(R.id.tab_all);
        TextView tabUpcoming  = findViewById(R.id.tab_upcoming);
        TextView tabCompleted = findViewById(R.id.tab_completed);
        if (tabAll != null)       tabAll.setOnClickListener(v -> applyFilter("all"));
        if (tabUpcoming != null)  tabUpcoming.setOnClickListener(v -> applyFilter("pending"));
        if (tabCompleted != null) tabCompleted.setOnClickListener(v -> applyFilter("completed"));

        adapter = new DoctorAppointmentAdapter(showList, a -> {
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

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }

        loadAppointments();
    }

    private void loadAppointments() {
        if (doctorId.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                allList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment a = child.getValue(Appointment.class);
                    if (a != null && doctorId.equals(a.getDoctorId())) {
                        a.setAppointmentId(child.getKey());
                        allList.add(a);
                    }
                }
                Collections.sort(allList, (o1, o2) -> Long.compare(o2.getCreatedAtLong(), o1.getCreatedAtLong()));
                applyFilter(currentFilter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        };

        dbRef.child("appointments").addValueEventListener(listener);
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        showList.clear();
        for (Appointment a : allList) {
            String status = a.getStatus() != null ? a.getStatus() : "";
            if ("all".equals(filter) || status.equals(filter) ||
                    ("pending".equals(filter) && "confirmed".equals(status))) {
                showList.add(a);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) tvEmpty.setVisibility(showList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) dbRef.child("appointments").removeEventListener(listener);
    }
}