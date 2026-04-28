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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserAppointmentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private DatabaseReference dbRef;
    private String currentUid;
    private ValueEventListener listener;
    private final List<Appointment> list = new ArrayList<>();
    private UserAppointmentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_appointments);

        dbRef = FirebaseDatabase.getInstance().getReference("appointments");
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        recyclerView = findViewById(R.id.rvAppointments);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new UserAppointmentAdapter(list, appointment -> {
            Intent intent = new Intent(this, AppointmentDetailsActivity.class);
            intent.putExtra("appointmentId", appointment.getAppointmentId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadAppointments();
    }

    private void loadAppointments() {
        if (currentUid.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Query query = dbRef.orderByChild("patientId").equalTo(currentUid);

        listener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                list.clear();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Appointment a = doc.getValue(Appointment.class);
                    if (a != null) {
                        a.setAppointmentId(doc.getKey());
                        list.add(a);
                    }
                }
                
                // Sort by createdAt descending locally
                Collections.sort(list, (a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });

                adapter.notifyDataSetChanged();
                if (tvEmpty != null) tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null && dbRef != null) {
            dbRef.removeEventListener(listener);
        }
    }
}
