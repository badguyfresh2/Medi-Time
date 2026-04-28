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
 * Patient-facing Medical Records screen.
 *
 * Tab 1 — My Uploads   : patient can upload PDF/images; list of their own uploads.
 * Tab 2 — From Doctor  : read-only list of records shared by the doctor.
 */
public class MedicalRecordsActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 400;

    // Firebase
    private DatabaseReference dbRef;
    private StorageReference  storageRef;
    private FirebaseAuth      mAuth;
    private String            uid;

    // Tab views
    private View   tabMine, tabDoctor;
    private View   panelMine, panelDoctor;

    // My Uploads panel
    private LinearLayout  myRecordsContainer;
    private ProgressBar   progressMyRecords;
    private TextView      tvMyEmpty;
    private Button        btnUploadNew;

    // Doctor Records panel
    private LinearLayout  drRecordsContainer;
    private ProgressBar   progressDrRecords;
    private TextView      tvDrEmpty;

    // Upload state
    private Uri    pendingUri      = null;
    private String pendingFileName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_records);

        mAuth      = FirebaseAuth.getInstance();
        dbRef      = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference("medical_records");
        uid        = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageView btnAddRecord = findViewById(R.id.btnAddRecord);
        if (btnAddRecord != null) btnAddRecord.setOnClickListener(v -> openFilePicker());

        // Tabs
        tabMine   = findViewById(R.id.tabMine);
        tabDoctor = findViewById(R.id.tabDoctor);
        panelMine = findViewById(R.id.panelMine);
        panelDoctor= findViewById(R.id.panelDoctor);

        if (tabMine   != null) tabMine.setOnClickListener(v   -> switchTab(true));
        if (tabDoctor != null) tabDoctor.setOnClickListener(v -> switchTab(false));

        // My Uploads panel
        myRecordsContainer = findViewById(R.id.myRecordsContainer);
        progressMyRecords  = findViewById(R.id.progressMyRecords);
        tvMyEmpty          = findViewById(R.id.tvMyEmpty);
        btnUploadNew       = findViewById(R.id.btnUploadNew);
        if (btnUploadNew != null) btnUploadNew.setOnClickListener(v -> openFilePicker());

        // Doctor Records panel
        drRecordsContainer = findViewById(R.id.drRecordsContainer);
        progressDrRecords  = findViewById(R.id.progressDrRecords);
        tvDrEmpty          = findViewById(R.id.tvDrEmpty);

        switchTab(true);
        loadMyRecords();
        loadDoctorRecords();
    }

    private void switchTab(boolean mine) {
        if (panelMine   != null) panelMine.setVisibility(mine ? View.VISIBLE : View.GONE);
        if (panelDoctor != null) panelDoctor.setVisibility(mine ? View.GONE : View.VISIBLE);

        int activeColor   = 0xFF7B5EA7;
        int inactiveColor = 0xFF9CA3AF;
        if (tabMine   instanceof TextView) ((TextView) tabMine).setTextColor(mine ? activeColor : inactiveColor);
        if (tabDoctor instanceof TextView) ((TextView) tabDoctor).setTextColor(mine ? inactiveColor : activeColor);
    }

    // ── My Uploads ────────────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/pdf", "image/jpeg", "image/png", "image/webp"});
        startActivityForResult(Intent.createChooser(intent, "Select Medical Record"), PICK_FILE_REQUEST);
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
        builder.setTitle("Upload Medical Record");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_record, null);
        EditText etDocName = dialogView != null ? dialogView.findViewById(R.id.etDocumentName) : null;
        Spinner  spType    = dialogView != null ? dialogView.findViewById(R.id.spinnerDocType)  : null;

        if (etDocName != null && pendingFileName != null)
            etDocName.setText(pendingFileName.replaceFirst("\\.[^.]+$", ""));

        if (spType != null) {
            String[] types = {"Lab Report", "X-Ray / Scan", "Prescription", "Insurance Card",
                    "Vaccination Record", "Discharge Summary", "Other"};
            spType.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, types));
        }

        builder.setView(dialogView)
                .setPositiveButton("Upload", (dlg, w) -> {
                    String docName = etDocName != null ? etDocName.getText().toString().trim() : pendingFileName;
                    String docType = spType    != null ? spType.getSelectedItem().toString()    : "Document";
                    if (docName.isEmpty()) docName = pendingFileName;
                    uploadFile(docName, docType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadFile(String docName, String docType) {
        if (uid.isEmpty() || pendingUri == null) return;
        if (progressMyRecords != null) progressMyRecords.setVisibility(View.VISIBLE);

        String fileName = System.currentTimeMillis() + "_" + pendingFileName;
        StorageReference fileRef = storageRef.child(uid).child(fileName);

        fileRef.putFile(pendingUri)
                .addOnSuccessListener(snap -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveRecordMeta(docName, docType, uri.toString(), fileName);
                }))
                .addOnFailureListener(e -> {
                    if (progressMyRecords != null) progressMyRecords.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveRecordMeta(String docName, String docType, String fileUrl, String fileName) {
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        Map<String, Object> record = new HashMap<>();
        record.put("patientId", uid);
        record.put("uploadedBy", "patient");
        record.put("docName",    docName);
        record.put("docType",    docType);
        record.put("fileUrl",    fileUrl);
        record.put("fileName",   fileName);
        record.put("date",       date);
        record.put("timestamp",  System.currentTimeMillis());

        dbRef.child("medical_records").child(uid).child("patient_uploads").push()
                .setValue(record)
                .addOnSuccessListener(unused -> {
                    if (progressMyRecords != null) progressMyRecords.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ Record uploaded successfully", Toast.LENGTH_SHORT).show();
                    loadMyRecords();
                })
                .addOnFailureListener(e -> {
                    if (progressMyRecords != null) progressMyRecords.setVisibility(View.GONE);
                    Toast.makeText(this, "Error saving record: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadMyRecords() {
        if (uid.isEmpty()) return;
        if (progressMyRecords != null) progressMyRecords.setVisibility(View.VISIBLE);

        dbRef.child("medical_records").child(uid).child("patient_uploads")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (progressMyRecords != null) progressMyRecords.setVisibility(View.GONE);
                        if (myRecordsContainer == null) return;
                        myRecordsContainer.removeAllViews();
                        boolean empty = true;
                        // Iterate in reverse (newest first)
                        List<DataSnapshot> snaps = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) snaps.add(child);
                        Collections.reverse(snaps);
                        for (DataSnapshot child : snaps) {
                            String docName = child.child("docName").getValue(String.class);
                            String docType = child.child("docType").getValue(String.class);
                            String date    = child.child("date").getValue(String.class);
                            String fileUrl = child.child("fileUrl").getValue(String.class);
                            String key     = child.getKey();
                            myRecordsContainer.addView(buildRecordCard(
                                    docName != null ? docName : "Document",
                                    docType != null ? docType : "",
                                    date    != null ? date    : "",
                                    fileUrl, key, true));
                            empty = false;
                        }
                        if (tvMyEmpty != null) tvMyEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (progressMyRecords != null) progressMyRecords.setVisibility(View.GONE);
                    }
                });
    }

    // ── Doctor-shared Records ─────────────────────────────────────────────────

    private void loadDoctorRecords() {
        if (uid.isEmpty()) return;
        if (progressDrRecords != null) progressDrRecords.setVisibility(View.VISIBLE);

        dbRef.child("medical_records").child(uid).child("doctor_uploads")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (progressDrRecords != null) progressDrRecords.setVisibility(View.GONE);
                        if (drRecordsContainer == null) return;
                        drRecordsContainer.removeAllViews();
                        boolean empty = true;
                        List<DataSnapshot> snaps = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) snaps.add(child);
                        Collections.reverse(snaps);
                        for (DataSnapshot child : snaps) {
                            String docName    = child.child("docName").getValue(String.class);
                            String docType    = child.child("docType").getValue(String.class);
                            String date       = child.child("date").getValue(String.class);
                            String fileUrl    = child.child("fileUrl").getValue(String.class);
                            String doctorName = child.child("doctorName").getValue(String.class);
                            drRecordsContainer.addView(buildDoctorRecordCard(
                                    docName != null ? docName : "Document",
                                    docType != null ? docType : "",
                                    date    != null ? date    : "",
                                    doctorName != null ? doctorName : "Doctor",
                                    fileUrl));
                            empty = false;
                        }
                        if (tvDrEmpty != null) tvDrEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (progressDrRecords != null) progressDrRecords.setVisibility(View.GONE);
                    }
                });
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    private View buildRecordCard(String name, String type, String date,
                                  String fileUrl, String recordKey, boolean canDelete) {
        View card = getLayoutInflater().inflate(R.layout.item_medical_record, null);
        if (card == null) return new View(this);

        TextView tvName   = card.findViewById(R.id.tvRecordName);
        TextView tvType   = card.findViewById(R.id.tvRecordType);
        TextView tvDate   = card.findViewById(R.id.tvRecordDate);
        ImageView btnView = card.findViewById(R.id.btnViewRecord);
        ImageView btnDel  = card.findViewById(R.id.btnDeleteRecord);

        if (tvName != null) tvName.setText(name);
        if (tvType != null) tvType.setText(type);
        if (tvDate != null) tvDate.setText(date);

        if (btnView != null) {
            btnView.setOnClickListener(v -> {
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl)));
                } else {
                    Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnDel != null) {
            if (!canDelete) { btnDel.setVisibility(View.GONE); }
            else {
                btnDel.setOnClickListener(v ->
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("Delete Record")
                                .setMessage("Are you sure you want to delete \"" + name + "\"?")
                                .setPositiveButton("Delete", (dlg, w) -> deleteRecord(recordKey))
                                .setNegativeButton("Cancel", null)
                                .show());
            }
        }

        return card;
    }

    private View buildDoctorRecordCard(String name, String type, String date, String doctorName, String fileUrl) {
        View card = getLayoutInflater().inflate(R.layout.item_medical_record, null);
        if (card == null) return new View(this);

        TextView tvName   = card.findViewById(R.id.tvRecordName);
        TextView tvType   = card.findViewById(R.id.tvRecordType);
        TextView tvDate   = card.findViewById(R.id.tvRecordDate);
        ImageView btnView = card.findViewById(R.id.btnViewRecord);
        ImageView btnDel  = card.findViewById(R.id.btnDeleteRecord);
        TextView tvDoctor = card.findViewById(R.id.tvRecordDoctor);

        if (tvName   != null) tvName.setText(name);
        if (tvType   != null) tvType.setText(type);
        if (tvDate   != null) tvDate.setText(date);
        if (tvDoctor != null) { tvDoctor.setVisibility(View.VISIBLE); tvDoctor.setText("Shared by Dr. " + doctorName); }
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

    private void deleteRecord(String recordKey) {
        dbRef.child("medical_records").child(uid).child("patient_uploads").child(recordKey)
                .removeValue()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
