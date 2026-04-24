package com.example.meditime;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.AppNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private DatabaseReference dbRef;
    private ValueEventListener listener;
    private final List<AppNotification> list = new ArrayList<>();
    private NotifAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        dbRef = FirebaseDatabase.getInstance().getReference();
        recyclerView = findViewById(R.id.rvNotifications);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new NotifAdapter(list, notif -> markRead(notif));
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
        loadNotifications();
    }

    private void loadNotifications() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                list.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppNotification n = child.getValue(AppNotification.class);
                    if (n != null) {
                        n.setNotificationId(child.getKey());
                        list.add(n);
                    }
                }
                // sort by timestamp descending if exists
                Collections.sort(list, (a, b) -> {
                    Long ta = a.getTimestamp() != null ? a.getTimestamp().getTime() : 0;
                    Long tb = b.getTimestamp() != null ? b.getTimestamp().getTime() : 0;
                    return tb.compareTo(ta);
                });
                adapter.notifyDataSetChanged();
                if (tvEmpty != null) tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        };

        dbRef.child("users").child(uid).child("notifications").addValueEventListener(listener);
    }

    private void markRead(AppNotification notif) {
        if (notif.isRead()) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null || notif.getNotificationId() == null) return;
        dbRef.child("users").child(uid).child("notifications")
                .child(notif.getNotificationId()).child("read").setValue(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (uid != null) dbRef.child("users").child(uid).child("notifications").removeEventListener(listener);
        }
    }

    // Inner adapter
    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        interface OnTap { void onTap(AppNotification n); }
        private final List<AppNotification> list;
        private final OnTap listener;
        NotifAdapter(List<AppNotification> list, OnTap listener) { this.list = list; this.listener = listener; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AppNotification n = list.get(pos);
            h.tvTitle.setText(n.getTitle() != null ? n.getTitle() : "");
            h.tvBody.setText(n.getBody() != null ? n.getBody() : "");
            if (h.tvTime != null && n.getTimestamp() != null)
                h.tvTime.setText(new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(new Date(String.valueOf(n.getTimestamp()))));
            h.itemView.setAlpha(n.isRead() ? 0.6f : 1f);
            h.itemView.setOnClickListener(v -> listener.onTap(n));
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvBody, tvTime;
            VH(View v) { super(v); tvTitle = v.findViewById(R.id.tvTitle); tvBody = v.findViewById(R.id.tvBody); tvTime = v.findViewById(R.id.tvTime); }
        }
    }
}