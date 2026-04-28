package com.example.meditime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.NumberFormat;
import java.util.*;

public class CheckoutActivity extends AppCompatActivity {

    // ── Intent extras ────────────────────────────────────────────────────
    private String appointmentId, doctorId, doctorName, specialization, hospital, date, timeSlot, patientName;
    private double fee;

    // ── UI ────────────────────────────────────────────────────────────────
    private TextView tvDoctorName, tvSpec, tvDate, tvTime, tvHospital;
    private TextView tvConsultFee, tvServiceFee, tvTotal;
    private CardView cardCard, cardMTN, cardAirtel, cardCash;
    private TextView tvCardLabel, tvMTNLabel, tvAirtelLabel, tvCashLabel;
    private EditText etPhoneNumber;
    private LinearLayout layoutPhoneInput;
    private ProgressBar progressBar;
    private Button btnPay;

    // ── State ─────────────────────────────────────────────────────────────
    private String selectedMethod = "Mobile Money - MTN";
    private static final double SERVICE_FEE = 2000;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseDatabase.getInstance().getReference();

        Intent i = getIntent();
        appointmentId = nvl(i.getStringExtra("appointmentId"));
        doctorId      = nvl(i.getStringExtra("doctorId"));
        doctorName    = nvl(i.getStringExtra("doctorName"), "Doctor");
        specialization= nvl(i.getStringExtra("specialization"), "General");
        hospital      = nvl(i.getStringExtra("hospital"), "N/A");
        date          = nvl(i.getStringExtra("date"));
        timeSlot      = nvl(i.getStringExtra("timeSlot"));
        fee           = i.getDoubleExtra("fee", 0.0);
        patientName   = nvl(i.getStringExtra("patientName"), "Patient");

        // Bind views
        tvDoctorName  = findViewById(R.id.tvCheckoutDoctorName);
        tvSpec        = findViewById(R.id.tvCheckoutSpec);
        tvDate        = findViewById(R.id.tvCheckoutDate);
        tvTime        = findViewById(R.id.tvCheckoutTime);
        tvHospital    = findViewById(R.id.tvCheckoutHospital);
        tvConsultFee  = findViewById(R.id.tvCheckoutConsultFee);
        tvServiceFee  = findViewById(R.id.tvCheckoutServiceFee);
        tvTotal       = findViewById(R.id.tvCheckoutTotal);
        progressBar   = findViewById(R.id.progressBar);
        btnPay        = findViewById(R.id.btnPay);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        layoutPhoneInput = findViewById(R.id.layoutPhoneInput);

        cardCard   = findViewById(R.id.cardPayCard);
        cardMTN    = findViewById(R.id.cardPayMTN);
        cardAirtel = findViewById(R.id.cardPayAirtel);
        cardCash   = findViewById(R.id.cardPayCash);
        tvCardLabel   = findViewById(R.id.tvCardLabel);
        tvMTNLabel    = findViewById(R.id.tvMTNLabel);
        tvAirtelLabel = findViewById(R.id.tvAirtelLabel);
        tvCashLabel   = findViewById(R.id.tvCashLabel);

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressedCompat());

        // Populate summary
        if (tvDoctorName != null) tvDoctorName.setText("Dr. " + doctorName);
        if (tvSpec       != null) tvSpec.setText(specialization);
        if (tvDate       != null) tvDate.setText(date);
        if (tvTime       != null) tvTime.setText(timeSlot);
        if (tvHospital   != null) tvHospital.setText(hospital);

        double total = fee + SERVICE_FEE;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        if (tvConsultFee != null) tvConsultFee.setText("UGX " + nf.format(fee));
        if (tvServiceFee != null) tvServiceFee.setText("UGX " + nf.format(SERVICE_FEE));
        if (tvTotal      != null) tvTotal.setText("UGX " + nf.format(total));
        if (btnPay       != null) btnPay.setText("Pay  UGX " + nf.format(total));

        // Payment method selection
        selectMethod("Mobile Money - MTN");
        if (cardCard   != null) cardCard.setOnClickListener(v -> selectMethod("Credit / Debit Card"));
        if (cardMTN    != null) cardMTN.setOnClickListener(v -> selectMethod("Mobile Money - MTN"));
        if (cardAirtel != null) cardAirtel.setOnClickListener(v -> selectMethod("Mobile Money - Airtel"));
        if (cardCash   != null) cardCash.setOnClickListener(v -> selectMethod("Pay at Clinic"));

        if (btnPay != null) btnPay.setOnClickListener(v -> processPayment());
    }

    private void selectMethod(String method) {
        selectedMethod = method;
        int accentBlue  = 0xFF2A5EE8;
        int accentGreen = 0xFF059669;
        int neutral     = 0xFF6B7280;
        int highlightBg = 0xFFEFF6FF;
        int whiteBg     = 0xFFFFFFFF;

        resetCard(cardCard,   tvCardLabel,   "Credit / Debit Card", neutral, whiteBg);
        resetCard(cardMTN,    tvMTNLabel,    "MTN Mobile Money",    neutral, whiteBg);
        resetCard(cardAirtel, tvAirtelLabel, "Airtel Money",        neutral, whiteBg);
        resetCard(cardCash,   tvCashLabel,   "Pay at Clinic",       neutral, whiteBg);

        switch (method) {
            case "Credit / Debit Card":
                highlightCard(cardCard,   tvCardLabel,   "Credit / Debit Card", accentBlue, highlightBg);
                showPhoneInput(false); break;
            case "Mobile Money - MTN":
                highlightCard(cardMTN,    tvMTNLabel,    "MTN Mobile Money",    accentGreen, 0xFFF0FDF4);
                showPhoneInput(true); break;
            case "Mobile Money - Airtel":
                highlightCard(cardAirtel, tvAirtelLabel, "Airtel Money",        0xFFEA1B2A, 0xFFFFF0F1);
                showPhoneInput(true); break;
            case "Pay at Clinic":
                highlightCard(cardCash,   tvCashLabel,   "Pay at Clinic",       accentBlue, highlightBg);
                showPhoneInput(false); break;
        }
    }

    private void resetCard(CardView card, TextView label, String text, int textColor, int bg) {
        if (card == null) return;
        card.setCardBackgroundColor(bg);
        card.setCardElevation(2f);
        if (label != null) { label.setText(text); label.setTextColor(textColor); }
    }

    private void highlightCard(CardView card, TextView label, String text, int textColor, int bg) {
        if (card == null) return;
        card.setCardBackgroundColor(bg);
        card.setCardElevation(6f);
        if (label != null) { label.setText("✓  " + text); label.setTextColor(textColor); }
    }

    private void showPhoneInput(boolean show) {
        if (layoutPhoneInput != null)
            layoutPhoneInput.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void processPayment() {
        boolean isMobile = selectedMethod.startsWith("Mobile Money");
        if (isMobile && etPhoneNumber != null) {
            String phone = etPhoneNumber.getText().toString().trim();
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.length() < 9) {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnPay != null) btnPay.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentStatus", "paid");
        updates.put("paymentMethod", selectedMethod);
        updates.put("paymentTimestamp", System.currentTimeMillis());
        updates.put("amountPaid", fee + SERVICE_FEE);

        db.child("appointments").child(appointmentId).updateChildren(updates)
                .addOnSuccessListener(v -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    // ── Save in‑app notification ─────────────────────────────
                    String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                    saveNotification(userId,
                            "Booking Confirmed",
                            "Your appointment with Dr. " + doctorName + " on " + date + " at " + timeSlot + " has been booked.");

                    // Navigate to confirmation
                    Intent intent = new Intent(this, BookingConfirmationActivity.class);
                    intent.putExtra("appointmentId",  appointmentId);
                    intent.putExtra("doctorName",     doctorName);
                    intent.putExtra("specialization", specialization);
                    intent.putExtra("hospital",       hospital);
                    intent.putExtra("date",           date);
                    intent.putExtra("timeSlot",       timeSlot);
                    intent.putExtra("fee",            fee);
                    intent.putExtra("total",          fee + SERVICE_FEE);
                    intent.putExtra("paymentMethod",  selectedMethod);
                    intent.putExtra("patientName",    patientName);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(ex -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (btnPay != null) btnPay.setEnabled(true);
                    Toast.makeText(this, "Payment failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void onBackPressedCompat() {
        if (appointmentId != null && !appointmentId.isEmpty()) {
            db.child("appointments").child(appointmentId).child("status").setValue("cancelled");
        }
        finish();
    }

    // ── Notification helper ──────────────────────────────────────────────
    public void saveNotification(String userId, String title, String body) {
        if (userId.isEmpty()) return;
        DatabaseReference ref = db.child("notifications").child(userId);
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("body", body);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        ref.push().setValue(notif);   // ✅ pass 'notif' here
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private String nvl(String s, String def) { return (s != null && !s.isEmpty()) ? s : def; }
}