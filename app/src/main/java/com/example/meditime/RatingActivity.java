package com.example.meditime;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class RatingActivity extends AppCompatActivity {

    private ImageView imgDoctor;
    private TextView tvDoctorName, tvSpecialization, tvCurrentRating, tvRatingLabel;
    private RatingBar ratingBar;
    private EditText etReview;
    private Button btnSubmit;
    private ProgressBar progressBar;

    private DatabaseReference dbRef;
    private String doctorId, appointmentId, patientId;

    private static final String[] RATING_LABELS = {
        "", "Poor", "Fair", "Good", "Very Good", "Excellent"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        dbRef = FirebaseDatabase.getInstance().getReference();
        patientId     = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        doctorId      = getIntent().getStringExtra("doctorId");
        appointmentId = getIntent().getStringExtra("appointmentId");

        imgDoctor        = findViewById(R.id.imgDoctor);
        tvDoctorName     = findViewById(R.id.tvDoctorName);
        tvSpecialization = findViewById(R.id.tvSpecialization);
        tvCurrentRating  = findViewById(R.id.tvCurrentRating);
        tvRatingLabel    = findViewById(R.id.tvRatingLabel);
        ratingBar        = findViewById(R.id.ratingBar);
        etReview         = findViewById(R.id.etReview);
        btnSubmit        = findViewById(R.id.btnSubmit);
        progressBar      = findViewById(R.id.progressBar);

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (ratingBar != null) {
            ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
                int r = (int) rating;
                if (tvRatingLabel != null && r >= 1 && r <= 5)
                    tvRatingLabel.setText(RATING_LABELS[r]);
            });
        }

        loadDoctorInfo();

        if (btnSubmit != null) btnSubmit.setOnClickListener(v -> submitRating());
    }

    private void loadDoctorInfo() {
        if (doctorId == null) return;
        dbRef.child("doctors").child(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String name = snapshot.child("name").getValue(String.class);
                String spec = snapshot.child("specialization").getValue(String.class);
                String img  = snapshot.child("profileImageUrl").getValue(String.class);
                Double rating = snapshot.child("rating").getValue(Double.class);

                if (tvDoctorName     != null) tvDoctorName.setText(name != null ? "Dr. " + name : "Doctor");
                if (tvSpecialization != null) tvSpecialization.setText(spec != null ? spec : "");
                if (tvCurrentRating  != null)
                    tvCurrentRating.setText(rating != null ? String.format("%.1f ★", rating) : "New");
                if (imgDoctor != null && img != null && !img.isEmpty())
                    Glide.with(RatingActivity.this).load(img).circleCrop().into(imgDoctor);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void submitRating() {
        if (ratingBar == null) return;
        float rating = ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String review = etReview != null ? etReview.getText().toString().trim() : "";
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnSubmit   != null) btnSubmit.setEnabled(false);

        // 1. Save this rating entry
        Map<String, Object> ratingEntry = new HashMap<>();
        ratingEntry.put("patientId",     patientId);
        ratingEntry.put("doctorId",      doctorId);
        ratingEntry.put("appointmentId", appointmentId != null ? appointmentId : "");
        ratingEntry.put("rating",        rating);
        ratingEntry.put("review",        review);
        ratingEntry.put("createdAt",     ServerValue.TIMESTAMP);

        dbRef.child("ratings").push().setValue(ratingEntry)
            .addOnSuccessListener(ref -> recalculateDoctorRating())
            .addOnFailureListener(e -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (btnSubmit   != null) btnSubmit.setEnabled(true);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void recalculateDoctorRating() {
        Query query = dbRef.child("ratings").orderByChild("doctorId").equalTo(doctorId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) { finishAfterSave(); return; }
                double total = 0;
                long count = 0;
                for (DataSnapshot d : snapshot.getChildren()) {
                    Double r = d.child("rating").getValue(Double.class);
                    if (r != null) {
                        total += r;
                        count++;
                    }
                }
                if (count == 0) { finishAfterSave(); return; }
                double avg = total / count;

                Map<String, Object> update = new HashMap<>();
                update.put("rating",       Double.parseDouble(String.format("%.1f", avg)));
                update.put("totalRatings", count);

                dbRef.child("doctors").child(doctorId).updateChildren(update)
                    .addOnCompleteListener(t -> finishAfterSave());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                finishAfterSave();
            }
        });
    }

    private void finishAfterSave() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "Thank you! Your rating was submitted.", Toast.LENGTH_LONG).show();
        finish();
    }
}
