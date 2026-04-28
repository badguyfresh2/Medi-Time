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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Doctor-facing Medical Records screen.
 *
 * Tab 1 — Patient Uploads : read-only; records the patient uploaded themselves.
 * Tab 2 — My Uploads      : doctor can upload new records (lab PDFs, scan images etc.)
 *                           which then appear in the patient's "From Doctor" tab.
 *
 * Pass extras: "patientId", "patientName".
 */
public class DoctorMedicalRecordsActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 500;

    private DatabaseReference dbRef;
    private StorageReference  storageRef;
    private FirebaseAuth      mAuth;
    private String            doctorId, doctorName;
    private String            patientId, patientName;

    // Tabs
    private View tabPatient, tabDoctor;
    private View panelPatient, panelDoctor;

    // Patient uploads panel
    private LinearLayout patientRecordsContainer;
    private ProgressBar  progressPatient;
    private TextView     tvPatientEmpty;

    // Doctor uploads panel
    private LinearLayout drRecordsContainer;
    private ProgressBar  progressDoctor;
    private TextView     tvDrEmpty;
    private Button       btnDrUpload;

    // Upload state
    private Uri    pendingUri      = null;
    private String pendingFileName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_medical_records);

        mAuth      = FirebaseAuth.getInstance();
        dbRef      = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference("medical_records");

        doctorId   = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        doctorName = mAuth.getCurrentUser() != null
                && mAuth.getCurrentUser().getDisplayName() != null
                ? mAuth.getCurrentUser().getDisplayName() : "Doctor";

        patientId   = getIntent().getStringExtra("patientId");
        patientName = getIntent().getStringExtra("patientName");
        if (patientId   == null) patientId   = "";
        if (patientName == null) patientName = "Patient";

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Update toolbar subtitle
        TextView tvTitle = findViewById(R.id.tvPatientNameTitle);
        if (tvTitle != null) tvTitle.setText("Records — " + patientName);

        // Tabs
        tabPatient   = findViewById(R.id.tabPatientUploads);
        tabDoctor    = findViewById(R.id.tabDoctorUploads);
        panelPatient = findViewById(R.id.panelPatientUploads);
        panelDoctor  = findViewById(R.id.panelDoctorUploads);

        if (tabPatient != null) tabPatient.setOnClickListener(v -> switchTab(true));
        if (tabDoctor  != null) tabDoctor.setOnClickListener(v  -> switchTab(false));

        // Patient uploads panel
        patientRecordsContainer = findViewById(R.id.patientRecordsContainer);
        progressPatient         = findViewById(R.id.progressPatientRecords);
        tvPatientEmpty          = findViewById(R.id.tvPatientEmpty);

        // Doctor uploads panel
        drRecordsContainer = findViewById(R.id.drRecordsContainer);
        progressDoctor     = findViewById(R.id.progressDrRecords);
        tvDrEmpty          = findViewById(R.id.tvDrEmpty);
        btnDrUpload        = findViewById(R.id.btnDrUpload);
        if (btnDrUpload != null) btnDrUpload.setOnClickListener(v -> openFilePicker());

        Button btnUploadNew = findViewById(R.id.btnUploadNew);
        if (btnUploadNew != null) btnUploadNew.setOnClickListener(v -> openFilePicker());

        switchTab(true);
        loadPatientRecords();
        loadDoctorRecords();
    }

    private void switchTab(boolean patientTab) {
        if (panelPatient != null) panelPatient.setVisibility(patientTab ? View.VISIBLE : View.GONE);
        if (panelDoctor  != null) panelDoctor.setVisibility(patientTab ? View.GONE : View.VISIBLE);
        int active   = 0xFF1565C0;
        int inactive = 0xFF9CA3AF;
        if (tabPatient instanceof TextView) ((TextView) tabPatient).setTextColor(patientTab ? active : inactive);
        if (tabDoctor  instanceof TextView) ((TextView) tabDoctor).setTextColor(patientTab ? inactive : active);
    }

    // ── Patient uploads (read-only) ───────────────────────────────────────────

    private void loadPatientRecords() {
        if (patientId.isEmpty()) return;
        if (progressPatient != null) progressPatient.setVisibility(View.VISIBLE);

        dbRef.child("medical_records").child(patientId).child("patient_uploads")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (progressPatient != null) progressPatient.setVisibility(View.GONE);
                        if (patientRecordsContainer == null) return;
                        patientRecordsContainer.removeAllViews();
                        boolean empty = true;
                        List<DataSnapshot> list = new ArrayList<>();
                        for (DataSnapshot c : snapshot.getChildren()) list.add(c);
                        Collections.reverse(list);
                        for (DataSnapshot child : list) {
                            String docName = child.child("docName").getValue(String.class);
                            String docType = child.child("docType").getValue(String.class);
                            String date    = child.child("date").getValue(String.class);
                            String fileUrl = child.child("fileUrl").getValue(String.class);
                            patientRecordsContainer.addView(
                                    buildReadOnlyCard(
                                            docName != null ? docName : "Document",
                                            docType != null ? docType : "",
                                            date    != null ? date    : "",
                                            "Uploaded by " + patientName,
                                            fileUrl, 0xFFDBEAFE, 0xFF1D4ED8));
                            empty = false;
                        }
                        if (tvPatientEmpty != null) tvPatientEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (progressPatient != null) progressPatient.setVisibility(View.GONE);
                    }
                });
    }

    // ── Doctor uploads ────────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/pdf", "image/jpeg", "image/png"});
        startActivityForResult(Intent.createChooser(intent, "Select File for Patient"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            pendingUri      = data.getData();
            pendingFileName = getFileName(pendingUri);
            showUploadDialog();
        }
    }

    private void showUploadDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Upload Record for " + patientName);

        View dv = getLayoutInflater().inflate(R.layout.dialog_upload_record, null);
        EditText etDocName = dv != null ? dv.findViewById(R.id.etDocumentName) : null;
        Spinner  spType    = dv != null ? dv.findViewById(R.id.spinnerDocType)  : null;

        if (etDocName != null && pendingFileName != null)
            etDocName.setText(pendingFileName.replaceFirst("\\.[^.]+$", ""));

        if (spType != null) {
            String[] types = {"Lab Results", "Scan / X-Ray", "Prescription", "Discharge Summary",
                    "Consultation Notes", "Referral Letter", "Other"};
            spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types));
        }

        builder.setView(dv)
                .setPositiveButton("Upload to Patient", (dlg, w) -> {
                    String docName = etDocName != null ? etDocName.getText().toString().trim() : pendingFileName;
                    String docType = spType    != null ? spType.getSelectedItem().toString()    : "Document";
                    if (docName.isEmpty()) docName = pendingFileName;
                    uploadFile(docName, docType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadFile(String docName, String docType) {
        if (patientId.isEmpty() || pendingUri == null) return;
        if (progressDoctor != null) progressDoctor.setVisibility(View.VISIBLE);

        String fileName = System.currentTimeMillis() + "_" + pendingFileName;
        StorageReference fileRef = storageRef.child(patientId).child("doctor").child(fileName);

        fileRef.putFile(pendingUri)
                .addOnSuccessListener(snap -> fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                        saveDocRecord(docName, docType, uri.toString(), fileName)))
                .addOnFailureListener(e -> {
                    if (progressDoctor != null) progressDoctor.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveDocRecord(String docName, String docType, String fileUrl, String fileName) {
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        Map<String, Object> record = new HashMap<>();
        record.put("patientId",  patientId);
        record.put("doctorId",   doctorId);
        record.put("doctorName", doctorName);
        record.put("uploadedBy", "doctor");
        record.put("docName",    docName);
        record.put("docType",    docType);
        record.put("fileUrl",    fileUrl);
        record.put("fileName",   fileName);
        record.put("date",       date);
        record.put("timestamp",  System.currentTimeMillis());

        // Save under patient's doctor_uploads so patient can see it
        dbRef.child("medical_records").child(patientId).child("doctor_uploads").push()
                .setValue(record)
                .addOnSuccessListener(unused -> {
                    if (progressDoctor != null) progressDoctor.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ Record shared with " + patientName, Toast.LENGTH_SHORT).show();
                    loadDoctorRecords();
                })
                .addOnFailureListener(e -> {
                    if (progressDoctor != null) progressDoctor.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDoctorRecords() {
        if (patientId.isEmpty()) return;
        if (progressDoctor != null) progressDoctor.setVisibility(View.VISIBLE);

        dbRef.child("medical_records").child(patientId).child("doctor_uploads")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (progressDoctor != null) progressDoctor.setVisibility(View.GONE);
                        if (drRecordsContainer == null) return;
                        drRecordsContainer.removeAllViews();
                        boolean empty = true;
                        List<DataSnapshot> list = new ArrayList<>();
                        for (DataSnapshot c : snapshot.getChildren()) list.add(c);
                        Collections.reverse(list);
                        for (DataSnapshot child : list) {
                            String docName = child.child("docName").getValue(String.class);
                            String docType = child.child("docType").getValue(String.class);
                            String date    = child.child("date").getValue(String.class);
                            String fileUrl = child.child("fileUrl").getValue(String.class);
                            drRecordsContainer.addView(
                                    buildReadOnlyCard(
                                            docName != null ? docName : "Document",
                                            docType != null ? docType : "",
                                            date    != null ? date    : "",
                                            "Shared with " + patientName,
                                            fileUrl, 0xFFEDE9F7, 0xFF7B5EA7));
                            empty = false;
                        }
                        if (tvDrEmpty != null) tvDrEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (progressDoctor != null) progressDoctor.setVisibility(View.GONE);
                    }
                });
    }

    // ── Card builder ──────────────────────────────────────────────────────────

    private View buildReadOnlyCard(String name, String type, String date,
                                    String subtitle, String fileUrl,
                                    int bgColor, int textColor) {
        View card = getLayoutInflater().inflate(R.layout.item_medical_record, null);
        if (card == null) return new View(this);

        TextView tvName   = card.findViewById(R.id.tvRecordName);
        TextView tvType   = card.findViewById(R.id.tvRecordType);
        TextView tvDate   = card.findViewById(R.id.tvRecordDate);
        TextView tvDoctor = card.findViewById(R.id.tvRecordDoctor);
        ImageView btnView = card.findViewById(R.id.btnViewRecord);
        ImageView btnDel  = card.findViewById(R.id.btnDeleteRecord);

        if (tvName   != null) tvName.setText(name);
        if (tvType   != null) tvType.setText(type);
        if (tvDate   != null) tvDate.setText(date);
        if (tvDoctor != null) { tvDoctor.setVisibility(View.VISIBLE); tvDoctor.setText(subtitle); tvDoctor.setTextColor(textColor); }
        if (btnDel   != null) btnDel.setVisibility(View.GONE);

        if (btnView != null) {
            btnView.setOnClickListener(v -> {
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl)));
                } else {
                    Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        return card;
    }

    private String getFileName(Uri uri) {
        String result = "record_" + System.currentTimeMillis();
        android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
            if (idx >= 0) result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
}
