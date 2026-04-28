package com.example.meditime;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.MedicineItem;
import com.example.meditime.model.Prescription;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class PrescriptionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private DatabaseReference dbRef;
    private ValueEventListener listener;
    private final List<Prescription> list = new ArrayList<>();
    private PrescriptionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription);

        dbRef = FirebaseDatabase.getInstance().getReference("prescriptions");
        recyclerView = findViewById(R.id.rvPrescriptions);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new PrescriptionAdapter(list);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
        loadPrescriptions();
    }

    private void loadPrescriptions() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Query query = dbRef.orderByChild("patientId").equalTo(uid);

        listener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                list.clear();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Prescription p = doc.getValue(Prescription.class);
                    if (p != null) {
                        p.setPrescriptionId(doc.getKey());
                        list.add(p);
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

    static class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.VH> {
        private final List<Prescription> list;
        PrescriptionAdapter(List<Prescription> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Prescription p = list.get(pos);
            h.tvDoctor.setText("Dr. " + (p.getDoctorName() != null ? p.getDoctorName() : ""));
            h.tvDiagnosis.setText(p.getDiagnosis() != null ? p.getDiagnosis() : "");
            if (h.tvDate != null && p.getCreatedAt() != null)
                h.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(p.getCreatedAt())));
            // Build medicines string
            if (h.tvMedicines != null && p.getMedicines() != null) {
                StringBuilder sb = new StringBuilder();
                for (MedicineItem m : p.getMedicines()) sb.append("• ").append(m.getName()).append(" - ").append(m.getDosage()).append("\n");
                h.tvMedicines.setText(sb.toString().trim());
            }
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvDoctor, tvDiagnosis, tvDate, tvMedicines;
            VH(View v) { super(v); tvDoctor = v.findViewById(R.id.tvDoctorName); tvDiagnosis = v.findViewById(R.id.tvDiagnosis); tvDate = v.findViewById(R.id.tvDate); tvMedicines = v.findViewById(R.id.tvMedicines); }
        }
    }
}
