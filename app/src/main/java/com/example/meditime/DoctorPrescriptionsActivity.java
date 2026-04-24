package com.example.meditime;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.meditime.model.MedicineItem;
import com.example.meditime.model.Prescription;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorPrescriptionsActivity extends AppCompatActivity {

    private TextView tvPatientName;
    private EditText etDiagnosis, etNotes;
    private LinearLayout medicineContainer;
    private Button btnAddMedicine, btnSubmit;
    private ProgressBar progressBar;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private String patientId, patientName, appointmentId;
    private final List<View> medicineViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_prescriptions);

        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        patientId     = getIntent().getStringExtra("patientId");
        patientName   = getIntent().getStringExtra("patientName");
        appointmentId = getIntent().getStringExtra("appointmentId");

        tvPatientName    = findViewById(R.id.tvPatientName);
        etDiagnosis      = findViewById(R.id.etDiagnosis);
        etNotes          = findViewById(R.id.etNotes);
        medicineContainer= findViewById(R.id.medicineContainer);
        btnAddMedicine   = findViewById(R.id.btn_add_medicine);
        btnSubmit        = findViewById(R.id.btn_issue_prescription);
        progressBar      = findViewById(R.id.progressBar);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        if (patientName != null && tvPatientName != null) {
            tvPatientName.setText("Patient: " + patientName);
        } else if (patientId != null) {
            dbRef.child("users").child(patientId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot ds) {
                    String name = ds.child("name").getValue(String.class);
                    if (name != null) {
                        patientName = name;
                        if (tvPatientName != null) tvPatientName.setText("Patient: " + name);
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {}
            });
        }

        addMedicineRow();

        if (btnAddMedicine != null) btnAddMedicine.setOnClickListener(v -> addMedicineRow());
        if (btnSubmit != null)     btnSubmit.setOnClickListener(v -> submitPrescription());
    }

    private void addMedicineRow() {
        if (medicineContainer == null) return;
        View row = LayoutInflater.from(this).inflate(R.layout.item_medicine_row, medicineContainer, false);
        medicineContainer.addView(row);
        medicineViews.add(row);

        Button btnRemove = row.findViewById(R.id.btnRemove);
        if (btnRemove != null) {
            btnRemove.setOnClickListener(v -> {
                medicineContainer.removeView(row);
                medicineViews.remove(row);
            });
        }
    }

    private void submitPrescription() {
        if (mAuth.getCurrentUser() == null || patientId == null) return;
        String doctorId = mAuth.getCurrentUser().getUid();

        List<MedicineItem> medicines = new ArrayList<>();
        for (View row : medicineViews) {
            EditText etName     = row.findViewById(R.id.etMedicineName);
            EditText etDosage   = row.findViewById(R.id.etDosage);
            EditText etDuration = row.findViewById(R.id.etDuration);
            EditText etMedNotes = row.findViewById(R.id.etMedNotes);

            String name     = etName != null ? etName.getText().toString().trim() : "";
            String dosage   = etDosage != null ? etDosage.getText().toString().trim() : "";
            String duration = etDuration != null ? etDuration.getText().toString().trim() : "";
            String medNotes = etMedNotes != null ? etMedNotes.getText().toString().trim() : "";

            if (!name.isEmpty()) medicines.add(new MedicineItem(name, dosage, duration, medNotes));
        }

        if (medicines.isEmpty()) { Toast.makeText(this, "Add at least one medicine", Toast.LENGTH_SHORT).show(); return; }

        String diagnosis = etDiagnosis != null ? etDiagnosis.getText().toString().trim() : "";
        String notes     = etNotes != null ? etNotes.getText().toString().trim() : "";

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        dbRef.child("doctors").child(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot doctorDoc) {
                String doctorName = doctorDoc.child("name").getValue(String.class);
                if (doctorName == null) doctorName = "Doctor";

                Prescription prescription = new Prescription();
                prescription.setPatientId(patientId);
                prescription.setDoctorId(doctorId);
                prescription.setDoctorName(doctorName);
                prescription.setPatientName(patientName != null ? patientName : "Patient");
                prescription.setAppointmentId(appointmentId != null ? appointmentId : "");
                prescription.setDiagnosis(diagnosis);
                prescription.setNotes(notes);
                prescription.setMedicines(medicines);

                DatabaseReference prescRef = dbRef.child("prescriptions").push();
                prescription.setPrescriptionId(prescRef.getKey());
                prescRef.setValue(prescription)
                        .addOnSuccessListener(aVoid -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(DoctorPrescriptionsActivity.this, "Prescription sent successfully!", Toast.LENGTH_LONG).show();
                            finish();
                        })
                        .addOnFailureListener(ex -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(DoctorPrescriptionsActivity.this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
            @Override
            public void onCancelled(DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }
}