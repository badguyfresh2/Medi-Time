package com.example.meditime;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PharmacyActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1002;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private LinearLayout pharmacyContainer;
    private EditText etSearch;
    private String activeFilter = "all"; // all | open | delivery

    // ── Real Kampala pharmacies ──────────────────────────────────────────────
    private static class Pharmacy {
        String name, address, phone;
        double lat, lng;
        int openHour, closeHour;  // 24-h clock; closeHour==0 → 24h
        boolean delivery;
        String deliveryTime;

        Pharmacy(String name, String address, double lat, double lng,
                 int openHour, int closeHour, boolean delivery, String deliveryTime, String phone) {
            this.name = name; this.address = address;
            this.lat = lat; this.lng = lng;
            this.openHour = openHour; this.closeHour = closeHour;
            this.delivery = delivery; this.deliveryTime = deliveryTime;
            this.phone = phone;
        }

        boolean isOpen() {
            if (closeHour == 0) return true; // 24h
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            return hour >= openHour && hour < closeHour;
        }

        String statusLabel() {
            if (closeHour == 0) return "Open 24h";
            return isOpen() ? "Open Now" : "Closed";
        }

        String hoursLabel() {
            if (closeHour == 0) return "Open 24 hours";
            return openHour + ":00 – " + closeHour + ":00";
        }
    }

    private final List<Pharmacy> allPharmacies = new ArrayList<Pharmacy>() {{
        add(new Pharmacy("Kampala Pharmacy Ltd",
                "William St, Kampala", 0.3158, 32.5825,
                8, 22, true, "~30 min", "+256414344000"));
        add(new Pharmacy("Medipal International Pharmacy",
                "Nakasero Rd, Kampala", 0.3221, 32.5789,
                0, 0, true, "~45 min", "+256312296800"));
        add(new Pharmacy("HealthPlus Pharmacy",
                "Acacia Mall, Kololo", 0.3312, 32.5941,
                7, 21, true, "~1 hr", "+256312200700"));
        add(new Pharmacy("Nakivubo Pharmacy",
                "Nakivubo Rd, Kampala", 0.3097, 32.5766,
                8, 18, false, null, "+256414230500"));
        add(new Pharmacy("Capital Pharmacy",
                "Garden City Mall, Kampala", 0.3248, 32.5895,
                9, 20, true, "~40 min", "+256312265000"));
        add(new Pharmacy("Nsambya Hospital Pharmacy",
                "Nsambya, Kampala", 0.2971, 32.5881,
                0, 0, false, null, "+256414267000"));
        add(new Pharmacy("AAR Pharmacy",
                "Jubilee Insurance House, Kampala", 0.3185, 32.5862,
                8, 19, true, "~1 hr", "+256312312100"));
        add(new Pharmacy("IHK Pharmacy",
                "Namuwongo, Kampala", 0.2983, 32.6048,
                0, 0, true, "~50 min", "+256312200400"));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        pharmacyContainer   = findViewById(R.id.pharmacyContainer);
        etSearch            = findViewById(R.id.etSearch);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnUploadPrescription = findViewById(R.id.btnUploadPrescription);
        if (btnUploadPrescription != null)
            btnUploadPrescription.setOnClickListener(v -> openImagePicker());

        Button btnUpload = findViewById(R.id.btnUpload);
        if (btnUpload != null) btnUpload.setOnClickListener(v -> openImagePicker());

        // Filter chips
        View chipAll      = findViewById(R.id.chipAll);
        View chipOpen     = findViewById(R.id.chipOpen);
        View chipDelivery = findViewById(R.id.chipDelivery);
        if (chipAll      != null) chipAll.setOnClickListener(v      -> { activeFilter = "all";      renderPharmacies(); });
        if (chipOpen     != null) chipOpen.setOnClickListener(v     -> { activeFilter = "open";     renderPharmacies(); });
        if (chipDelivery != null) chipDelivery.setOnClickListener(v -> { activeFilter = "delivery"; renderPharmacies(); });

        // Search
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { renderPharmacies(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.pharmMapContainer);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        requestLocation();
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = location;
            } else {
                Location def = new Location("default");
                def.setLatitude(0.3163); def.setLongitude(32.5822);
                userLocation = def;
            }
            renderPharmacies();
            if (googleMap != null) addUserMarker();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        // Add pharmacy markers
        for (Pharmacy p : allPharmacies) {
            float hue = p.isOpen() ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED;
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.lat, p.lng))
                    .title(p.name)
                    .snippet(p.statusLabel() + (p.delivery ? " • Delivery" : ""))
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0.3163, 32.5822), 13f));
        if (userLocation != null) addUserMarker();
    }

    private void addUserMarker() {
        if (googleMap == null || userLocation == null) return;
        LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
        googleMap.addMarker(new MarkerOptions()
                .position(userLatLng).title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f));
    }

    private float distanceTo(Pharmacy p) {
        if (userLocation == null) return 999f;
        float[] r = new float[1];
        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), p.lat, p.lng, r);
        return r[0] / 1000f;
    }

    private void renderPharmacies() {
        if (pharmacyContainer == null) return;
        pharmacyContainer.removeAllViews();

        String query = etSearch != null ? etSearch.getText().toString().toLowerCase() : "";

        List<Pharmacy> list = new ArrayList<>();
        for (Pharmacy p : allPharmacies) {
            if (!query.isEmpty() && !p.name.toLowerCase().contains(query)) continue;
            if (activeFilter.equals("open") && !p.isOpen()) continue;
            if (activeFilter.equals("delivery") && !p.delivery) continue;
            list.add(p);
        }

        // Sort nearest first
        list.sort((a, b) -> Float.compare(distanceTo(a), distanceTo(b)));

        for (Pharmacy p : list) pharmacyContainer.addView(buildPharmacyCard(p));

        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No pharmacies match your filter");
            empty.setPadding(32, 48, 32, 48);
            empty.setTextColor(0xFF6B7280);
            empty.setGravity(Gravity.CENTER);
            pharmacyContainer.addView(empty);
        }
    }

    private View buildPharmacyCard(Pharmacy p) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 32);
        card.setLayoutParams(lp);
        card.setRadius(40f);
        card.setCardElevation(6f);
        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(36, 36, 36, 36);

        // Header row
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        // Icon
        LinearLayout icon = new LinearLayout(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(112, 112));
        icon.setBackgroundColor(0xFFD1FAE5);
        icon.setGravity(Gravity.CENTER);
        TextView iconTv = new TextView(this);
        iconTv.setText("💊");
        iconTv.setTextSize(22f);
        icon.addView(iconTv);
        header.addView(icon);

        // Info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        infoLp.setMarginStart(24);
        info.setLayoutParams(infoLp);

        TextView tvName = new TextView(this);
        tvName.setText(p.name);
        tvName.setTextColor(0xFF1A1A2E);
        tvName.setTextSize(14f);
        tvName.setTypeface(tvName.getTypeface(), android.graphics.Typeface.BOLD);
        info.addView(tvName);

        float dist = distanceTo(p);
        TextView tvMeta = new TextView(this);
        tvMeta.setText(String.format("%.1f km  •  %s", dist, p.hoursLabel()));
        tvMeta.setTextColor(0xFF6B7280);
        tvMeta.setTextSize(12f);
        info.addView(tvMeta);

        // Status badge
        TextView tvStatus = new TextView(this);
        tvStatus.setText(p.statusLabel());
        tvStatus.setTextColor(0xFFFFFFFF);
        tvStatus.setTextSize(10f);
        tvStatus.setPadding(16, 6, 16, 6);
        tvStatus.setBackgroundColor(p.isOpen() ? 0xFF10B981 : 0xFFEF4444);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusLp.setMargins(0, 8, 0, 0);
        tvStatus.setLayoutParams(statusLp);
        info.addView(tvStatus);

        header.addView(info);
        inner.addView(header);

        // Delivery banner
        if (p.delivery) {
            LinearLayout deliveryBanner = new LinearLayout(this);
            deliveryBanner.setBackgroundColor(0xFFF0FDF4);
            LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dbLp.setMargins(0, 16, 0, 0);
            deliveryBanner.setLayoutParams(dbLp);
            deliveryBanner.setPadding(20, 14, 20, 14);

            TextView tvDelivery = new TextView(this);
            tvDelivery.setText("🛵  Delivery available  •  " + p.deliveryTime);
            tvDelivery.setTextColor(0xFF166534);
            tvDelivery.setTextSize(12f);
            deliveryBanner.addView(tvDelivery);
            inner.addView(deliveryBanner);
        }

        // Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowLp.setMargins(0, 16, 0, 0);
        btnRow.setLayoutParams(btnRowLp);

        Button btnOrder = new Button(this);
        btnOrder.setText("Order Medicines");
        btnOrder.setTextSize(12f);
        btnOrder.setBackgroundColor(0xFF32CD32);
        btnOrder.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams bop = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bop.setMarginEnd(16);
        btnOrder.setLayoutParams(bop);
        btnOrder.setEnabled(p.isOpen());
        btnOrder.setAlpha(p.isOpen() ? 1f : 0.5f);
        btnOrder.setOnClickListener(v -> Toast.makeText(this, "Ordering from " + p.name, Toast.LENGTH_SHORT).show());
        btnRow.addView(btnOrder);

        Button btnDir = new Button(this);
        btnDir.setText("🗺 Directions");
        btnDir.setTextSize(12f);
        btnDir.setBackgroundColor(0xFFF3F4F6);
        btnDir.setTextColor(0xFF32CD32);
        btnDir.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnDir.setOnClickListener(v -> openDirections(p));
        btnRow.addView(btnDir);

        inner.addView(btnRow);
        card.addView(inner);
        return card;
    }

    private void openDirections(Pharmacy p) {
        Uri uri = Uri.parse("geo:" + p.lat + "," + p.lng + "?q=" + Uri.encode(p.name));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=" + Uri.encode(p.name + " Kampala"))));
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        startActivityForResult(Intent.createChooser(intent, "Upload Prescription"), 200);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            Toast.makeText(this, "✅ Prescription uploaded! Pharmacy will prepare your order.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            renderPharmacies();
        }
    }
}
