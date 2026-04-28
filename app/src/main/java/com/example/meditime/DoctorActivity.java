package com.example.meditime;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.Doctor;
import com.google.firebase.database.*;

import java.util.*;

public class DoctorActivity extends AppCompatActivity {

    private RecyclerView rvDoctors;
    private EditText etSearch;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvCount;
    private LinearLayout filterContainer;

    private DatabaseReference dbRef;
    private ValueEventListener listener;
    private final List<Doctor> allDoctors  = new ArrayList<>();
    private final List<Doctor> showDoctors = new ArrayList<>();
    private DoctorListAdapter adapter;
    private String activeFilter = "All";

    private static final String[] SPECIALIZATIONS = {
        "All", "General", "Cardiologist", "Dermatologist",
        "Pediatrician", "Gynecologist", "Orthopedic",
        "Neurologist", "Dentist", "Ophthalmologist"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        dbRef = FirebaseDatabase.getInstance().getReference("doctors");

        rvDoctors      = findViewById(R.id.rvDoctors);
        etSearch       = findViewById(R.id.etSearch);
        progressBar    = findViewById(R.id.progressBar);
        tvEmpty        = findViewById(R.id.tvEmpty);
        tvCount        = findViewById(R.id.tvCount);
        filterContainer= findViewById(R.id.filterContainer);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new DoctorListAdapter(showDoctors, doctor -> {
            Intent intent = new Intent(this, DoctorDetailActivity.class);
            intent.putExtra("doctorId", doctor.getUserId());
            startActivity(intent);
        });
        rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        rvDoctors.setAdapter(adapter);

        buildFilterChips();

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    applyFilters(s.toString());
                }
            });
        }

        loadDoctors();
    }

    private void buildFilterChips() {
        if (filterContainer == null) return;
        filterContainer.removeAllViews();
        for (String spec : SPECIALIZATIONS) {
            TextView chip = new TextView(this);
            chip.setText(spec);
            chip.setTextSize(12f);
            chip.setPadding(40, 14, 40, 14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            chip.setLayoutParams(lp);
            styleChip(chip, spec.equals(activeFilter));
            chip.setOnClickListener(v -> {
                activeFilter = spec;
                buildFilterChips();
                applyFilters(etSearch != null ? etSearch.getText().toString() : "");
            });
            filterContainer.addView(chip);
        }
    }

    private void styleChip(TextView chip, boolean active) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(100f); // pill shape

        if (active) {
            bg.setColor(0xFF4A90D9);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            bg.setColor(0xFFFFFFFF);
            bg.setStroke(2, 0xFFD1D5DB); // subtle border for inactive
            chip.setTextColor(0xFF374151);
        }

        chip.setBackground(bg);
    }

    private void loadDoctors() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        listener = dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                allDoctors.clear();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Doctor d = doc.getValue(Doctor.class);
                    if (d != null) {
                        if (d.getUserId() == null) d.setUserId(doc.getKey());
                        allDoctors.add(d);
                    }
                }
                applyFilters(etSearch != null ? etSearch.getText().toString() : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Could not load doctors. Check connection.");
                }
            }
        });
    }

    private void applyFilters(String query) {
        showDoctors.clear();
        for (Doctor d : allDoctors) {
            // Filter by specialization chip
            boolean matchesSpec = activeFilter.equals("All") ||
                (d.getSpecialization() != null &&
                 d.getSpecialization().toLowerCase().contains(activeFilter.toLowerCase()));
            // Filter by search text (name or specialization)
            boolean matchesSearch = query.isEmpty() ||
                (d.getName() != null && d.getName().toLowerCase().contains(query.toLowerCase())) ||
                (d.getSpecialization() != null && d.getSpecialization().toLowerCase().contains(query.toLowerCase())) ||
                (d.getHospital() != null && d.getHospital().toLowerCase().contains(query.toLowerCase()));

            if (matchesSpec && matchesSearch) showDoctors.add(d);
        }
        adapter.notifyDataSetChanged();

        boolean empty = showDoctors.isEmpty();
        if (tvEmpty != null)  tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (tvCount != null)  tvCount.setText(showDoctors.size() + " doctor" + (showDoctors.size() == 1 ? "" : "s") + " found");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null && dbRef != null) {
            dbRef.removeEventListener(listener);
        }
    }
}
