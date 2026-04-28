package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditime.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnSend;
    private TextView tvOtherName;
    private ProgressBar progressBar;
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private String currentUid, otherUserId, chatRoomId;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db    = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUid   = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        otherUserId  = getIntent().getStringExtra("doctorId") != null
                ? getIntent().getStringExtra("doctorId")
                : getIntent().getStringExtra("patientId") != null
                ? getIntent().getStringExtra("patientId") : "";

        // Consistent chat room ID regardless of who initiates
        String[] ids = new String[]{currentUid, otherUserId};
        java.util.Arrays.sort(ids);
        chatRoomId = "chat_" + ids[0] + "_" + ids[1];

        rvMessages  = findViewById(R.id.rvMessages);
        etMessage   = findViewById(R.id.etMessage);
        btnSend     = findViewById(R.id.btnSend);
        tvOtherName = findViewById(R.id.tvOtherName);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageView btnVideoCall = findViewById(R.id.btnVideoCall);
        if (btnVideoCall != null) btnVideoCall.setOnClickListener(v -> startActivity(new Intent(this, VideoCallActivity.class)));

        adapter = new ChatAdapter(messages, currentUid);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        loadOtherUserName();
        listenForMessages();

        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadOtherUserName() {
        if (otherUserId.isEmpty()) return;
        // Try users node first, then doctors
        db.child("users").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && tvOtherName != null) {
                    String name = snapshot.child("name").getValue(String.class);
                    tvOtherName.setText(name != null ? name : "Chat");
                } else {
                    db.child("doctors").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot d) {
                            if (tvOtherName != null && d.exists()) {
                                String n = d.child("name").getValue(String.class);
                                tvOtherName.setText(n != null ? "Dr. " + n : "Doctor");
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForMessages() {
        listener = db.child("chat_rooms").child(chatRoomId).child("messages")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    messages.clear();
                    for (DataSnapshot doc : snapshot.getChildren()) {
                        ChatMessage msg = doc.getValue(ChatMessage.class);
                        if (msg != null) {
                            msg.setMessageId(doc.getKey());
                            messages.add(msg);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
                }

                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void sendMessage() {
        if (etMessage == null) return;
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        String msgId = db.child("chat_rooms").child(chatRoomId).child("messages").push().getKey();
        if (msgId == null) return;

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("senderId", currentUid);
        msgMap.put("text", text);
        msgMap.put("type", "text");
        msgMap.put("seen", false);
        msgMap.put("timestamp", ServerValue.TIMESTAMP);

        Map<String, Object> updates = new HashMap<>();
        updates.put("/chat_rooms/" + chatRoomId + "/messages/" + msgId, msgMap);
        updates.put("/chat_rooms/" + chatRoomId + "/lastMessage", text);
        updates.put("/chat_rooms/" + chatRoomId + "/updatedAt", ServerValue.TIMESTAMP);
        updates.put("/chat_rooms/" + chatRoomId + "/participants", java.util.Arrays.asList(currentUid, otherUserId));

        db.updateChildren(updates)
            .addOnSuccessListener(aVoid -> etMessage.setText(""))
            .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Send failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            db.child("chat_rooms").child(chatRoomId).child("messages").removeEventListener(listener);
        }
    }
}
