package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meditime.model.Appointment;
import com.example.meditime.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvBloodGroup, tvAppointmentCount;
    private ImageView imgProfile;
    private RecyclerView rvAppointments;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private ValueEventListener appointmentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        tvName           = findViewById(R.id.tvUserName);
        tvEmail          = findViewById(R.id.tvUserEmail);
        tvPhone          = findViewById(R.id.tvUserPhone);
        tvBloodGroup     = findViewById(R.id.tvBloodGroup);
        tvAppointmentCount = findViewById(R.id.tvAppointmentCount);
        imgProfile       = findViewById(R.id.imgProfile);
        rvAppointments   = findViewById(R.id.rvUpcomingAppointments);
        progressBar      = findViewById(R.id.progressBar);

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        LinearLayout navHome      = findViewById(R.id.nav_home);
        LinearLayout navDoctor    = findViewById(R.id.nav_doctor);
        LinearLayout navAmbulance = findViewById(R.id.nav_ambulance);
        if (navHome != null)      navHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        if (navDoctor != null)    navDoctor.setOnClickListener(v -> startActivity(new Intent(this, DoctorActivity.class)));
        if (navAmbulance != null) navAmbulance.setOnClickListener(v -> startActivity(new Intent(this, AmbulanceActivity.class)));

        CardView cardNotifications = findViewById(R.id.cardNotifications);
        CardView cardEditProfile   = findViewById(R.id.cardEditProfile);
        CardView cardAppointments  = findViewById(R.id.cardDoctorPortal);
        if (cardNotifications != null) cardNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        if (cardEditProfile != null)   cardEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        if (cardAppointments != null)  cardAppointments.setOnClickListener(v -> startActivity(new Intent(this, UserAppointmentsActivity.class)));

        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        loadUserData();
        loadUpcomingAppointments();
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot doc) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (!doc.exists()) return;
                User user = doc.getValue(User.class);
                if (user == null) return;
                if (tvName != null)     tvName.setText(user.getName() != null ? user.getName() : "N/A");
                if (tvEmail != null)    tvEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
                if (tvPhone != null)    tvPhone.setText(user.getPhone() != null ? user.getPhone() : "N/A");
                if (tvBloodGroup != null) tvBloodGroup.setText(user.getBloodGroup() != null ? user.getBloodGroup() : "N/A");
                if (imgProfile != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    Glide.with(ProfileActivity.this).load(new File(user.getProfileImageUrl())).circleCrop().into(imgProfile);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadUpcomingAppointments() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        if (rvAppointments == null) return;

        List<Appointment> list = new ArrayList<>();
        AppointmentCardAdapter adapter = new AppointmentCardAdapter(list, appointment -> {
            Intent intent = new Intent(this, AppointmentDetailsActivity.class);
            intent.putExtra("appointmentId", appointment.getAppointmentId());
            startActivity(intent);
        });
        rvAppointments.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvAppointments.setAdapter(adapter);

        appointmentListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment a = child.getValue(Appointment.class);
                    if (a != null && uid.equals(a.getPatientId()) &&
                            ("pending".equals(a.getStatus()) || "confirmed".equals(a.getStatus()))) {
                        a.setAppointmentId(child.getKey());
                        list.add(a);
                    }
                }
                adapter.notifyDataSetChanged();
                if (tvAppointmentCount != null) tvAppointmentCount.setText(String.valueOf(list.size()));
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };

        dbRef.child("appointments").addValueEventListener(appointmentListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentListener != null) dbRef.child("appointments").removeEventListener(appointmentListener);
    }
}