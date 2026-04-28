package com.example.meditime;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.DiagnosticReport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Doctor-facing screen to add diagnostic reports for a patient.
 * Receives optional extras: "patientId", "patientName", "appointmentId".
 * Uploads a report (with optional file) to Firebase then writes
 * the DiagnosticReport node so the patient can see it in DiagnosticActivity.
 */
public class DoctorDiagnosticActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 300;

    // Inputs
    private Spinner spinnerPatient, spinnerReportType;
    private EditText etFindings, etRecommendations;
    private RadioGroup rgSeverity;
    private TextView tvFileName;
    private Button btnAttach, btnSubmit;
    private ProgressBar progressUpload;
    private RecyclerView rvPreviousReports;

    // Firebase
    private DatabaseReference dbRef;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;

    private Uri attachedFileUri = null;
    private String attachedFileName = null;

    // Passed in from caller (optional)
    private String presetPatientId   = null;
    private String presetPatientName = null;

    // Simple patient list (in production load from Firebase)
    private final String[] patientNames = {
            "John Mukasa",
            "Mary Nakato",
            "Peter Ssemwogerere",
            "Grace Akinyi",
            "Robert Kiggundu",
            "Fatima Nakayiwa"
    };

    private final List<DiagnosticReport> previousReports = new ArrayList<>();
    private DiagnosticAdapter reportAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_diagnostic);

        mAuth      = FirebaseAuth.getInstance();
        dbRef      = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference("diagnostics");

        // Optional extras passed from patient appointment
        presetPatientId   = getIntent().getStringExtra("patientId");
        presetPatientName = getIntent().getStringExtra("patientName");

        // Views
        spinnerPatient     = findViewById(R.id.spinnerPatient);
        spinnerReportType  = findViewById(R.id.spinnerReportType);
        etFindings         = findViewById(R.id.etFindings);
        etRecommendations  = findViewById(R.id.etRecommendations);
        rgSeverity         = findViewById(R.id.rgSeverity);
        tvFileName         = findViewById(R.id.tvFileName);
        btnAttach          = findViewById(R.id.btnAttach);
        btnSubmit          = findViewById(R.id.btnSubmit);
        progressUpload     = findViewById(R.id.progressUpload);
        rvPreviousReports  = findViewById(R.id.rvPreviousReports);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupPatientSpinner();
        setupReportTypeSpinner();

        if (btnAttach != null) btnAttach.setOnClickListener(v -> pickFile());
        if (btnSubmit != null) btnSubmit.setOnClickListener(v -> submitReport());

        setupPreviousReports();
        loadPreviousReports();
    }

    private void setupPatientSpinner() {
        if (spinnerPatient == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, patientNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatient.setAdapter(adapter);

        // Pre-select if a patient was passed in
        if (presetPatientName != null) {
            for (int i = 0; i < patientNames.length; i++) {
                if (patientNames[i].equalsIgnoreCase(presetPatientName)) {
                    spinnerPatient.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupReportTypeSpinner() {
        if (spinnerReportType == null) return;
        String[] types = {"Blood Test", "Urine Analysis", "X-Ray", "ECG", "MRI Scan",
                "CT Scan", "Ultrasound", "Biopsy", "Colonoscopy", "General Consultation"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReportType.setAdapter(adapter);
    }

    private void setupPreviousReports() {
        if (rvPreviousReports == null) return;
        reportAdapter = new DiagnosticAdapter(previousReports, report -> {
            if (report.getFileUrl() != null && !report.getFileUrl().isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(report.getFileUrl())));
            }
        });
        rvPreviousReports.setLayoutManager(new LinearLayoutManager(this));
        rvPreviousReports.setAdapter(reportAdapter);
    }

    private void loadPreviousReports() {
        String doctorId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (doctorId.isEmpty()) return;

        dbRef.child("diagnostics").orderByChild("doctorId").equalTo(doctorId)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        previousReports.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            DiagnosticReport r = child.getValue(DiagnosticReport.class);
                            if (r != null) { r.setReportId(child.getKey()); previousReports.add(r); }
                        }
                        previousReports.sort((a, b) -> {
                            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        });
                        if (reportAdapter != null) reportAdapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/pdf", "image/jpeg", "image/png"});
        startActivityForResult(Intent.createChooser(intent, "Attach Report File"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            attachedFileUri = data.getData();
            attachedFileName = getFileName(attachedFileUri);
            if (tvFileName != null) tvFileName.setText(attachedFileName + " ✓");
        }
    }

    private String getFileName(Uri uri) {
        String result = "report_" + System.currentTimeMillis();
        android.database.Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
            if (idx >= 0) result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private void submitReport() {
        String findings = etFindings != null ? etFindings.getText().toString().trim() : "";
        if (findings.isEmpty()) {
            Toast.makeText(this, "Please enter your findings / diagnosis", Toast.LENGTH_SHORT).show();
            return;
        }

        String recommendations = etRecommendations != null
                ? etRecommendations.getText().toString().trim() : "";
        String patientName = spinnerPatient != null
                ? spinnerPatient.getSelectedItem().toString() : patientNames[0];
        String reportType  = spinnerReportType != null
                ? spinnerReportType.getSelectedItem().toString() : "Report";

        String severity = "Normal";
        if (rgSeverity != null) {
            int checkedId = rgSeverity.getCheckedRadioButtonId();
            if      (checkedId == R.id.rbMild)     severity = "Mild";
            else if (checkedId == R.id.rbModerate)  severity = "Moderate";
            else if (checkedId == R.id.rbCritical)  severity = "Critical";
        }

        String doctorId   = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        String doctorName = mAuth.getCurrentUser() != null
                ? (mAuth.getCurrentUser().getDisplayName() != null
                        ? mAuth.getCurrentUser().getDisplayName() : "Doctor") : "Doctor";

        // Resolve patientId: prefer preset, otherwise look up by name in Firebase
        String resolvedPatientId = presetPatientId != null ? presetPatientId : "patient_" + patientName.replaceAll("\\s+", "_").toLowerCase();

        // Build the report object
        DiagnosticReport report = new DiagnosticReport();
        report.setPatientId(resolvedPatientId);
        report.setDoctorId(doctorId);
        report.setDoctorName(doctorName);
        report.setTitle(reportType + " — " + patientName);
        report.setDescription(findings
                + (recommendations.isEmpty() ? "" : "\n\n📋 Recommendations:\n" + recommendations)
                + "\n\n⚠️ Severity: " + severity);
        report.setFileType("pdf");
        report.setCreatedAt(System.currentTimeMillis());

        if (progressUpload != null) progressUpload.setVisibility(View.VISIBLE);
        if (btnSubmit      != null) btnSubmit.setEnabled(false);

        if (attachedFileUri != null) {
            // Upload file first, then save report
            StorageReference fileRef = storageRef.child(doctorId + "/" + System.currentTimeMillis() + "_" + attachedFileName);
            fileRef.putFile(attachedFileUri)
                    .addOnSuccessListener(snap -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        report.setFileUrl(uri.toString());
                        saveReportToDatabase(report, patientName, reportType);
                    }))
                    .addOnFailureListener(e -> {
                        // Save without file URL if upload fails
                        saveReportToDatabase(report, patientName, reportType);
                    });
        } else {
            saveReportToDatabase(report, patientName, reportType);
        }
    }

    private void saveReportToDatabase(DiagnosticReport report, String patientName, String reportType) {
        String key = dbRef.child("diagnostics").push().getKey();
        if (key == null) {
            showError("Failed to generate report key");
            return;
        }
        report.setReportId(key);

        dbRef.child("diagnostics").child(key).setValue(report)
                .addOnSuccessListener(unused -> {
                    if (progressUpload != null) progressUpload.setVisibility(View.GONE);
                    if (btnSubmit      != null) btnSubmit.setEnabled(true);

                    Toast.makeText(this,
                            "✅ " + reportType + " report submitted for " + patientName,
                            Toast.LENGTH_LONG).show();

                    // Clear form
                    if (etFindings        != null) etFindings.setText("");
                    if (etRecommendations != null) etRecommendations.setText("");
                    if (tvFileName        != null) tvFileName.setText("No file attached");
                    if (rgSeverity        != null) rgSeverity.check(R.id.rbNormal);
                    attachedFileUri  = null;
                    attachedFileName = null;
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void showError(String msg) {
        if (progressUpload != null) progressUpload.setVisibility(View.GONE);
        if (btnSubmit      != null) btnSubmit.setEnabled(true);
        Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
    }
}
