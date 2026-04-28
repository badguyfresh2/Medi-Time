package com.example.meditime;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.Comparator;
import java.util.List;

public class HospitalActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private LinearLayout hospitalContainer;
    private TextView tvCurrentLocation;
    private Spinner spinnerRadius;
    private boolean sortByNearest = false;

    // ── Real Kampala hospitals ───────────────────────────────────────────────
    private static class Hospital {
        String name, services, phone, address;
        double lat, lng;
        float rating;
        String status; // "open24", "open", "closed"

        Hospital(String name, String address, double lat, double lng,
                 float rating, String status, String services, String phone) {
            this.name = name; this.address = address;
            this.lat = lat; this.lng = lng; this.rating = rating;
            this.status = status; this.services = services; this.phone = phone;
        }
    }

    private final List<Hospital> hospitals = new ArrayList<Hospital>() {{
        add(new Hospital("Mulago National Referral Hospital",
                "Mulago Hill Rd, Kampala", 0.3364, 32.5760,
                4.3f, "open24", "Emergency · ICU · Surgery · Maternity · Lab", "+256415540001"));
        add(new Hospital("Mengo Hospital",
                "Namirembe Rd, Kampala", 0.3124, 32.5537,
                4.7f, "open24", "Specialist Clinics · Lab · Imaging · Pharmacy", "+256312260699"));
        add(new Hospital("Nakasero Hospital",
                "Nakasero, Kampala", 0.3280, 32.5890,
                4.5f, "open", "General Medicine · Surgery · Pediatrics · ICU", "+256312206520"));
        add(new Hospital("Case Hospital",
                "Hannington Rd, Kampala", 0.3416, 32.5899,
                4.2f, "open", "Cardiology · Neurology · Oncology · Trauma", "+256312260011"));
        add(new Hospital("International Hospital Kampala (IHK)",
                "Namuwongo, Kampala", 0.2981, 32.6049,
                4.6f, "open24", "Multi-specialty · ICU · NICU · Trauma · Imaging", "+256312200400"));
        add(new Hospital("Rubaga Hospital",
                "Rubaga Rd, Kampala", 0.3053, 32.5481,
                4.1f, "open", "General · Maternity · Pediatrics · Dentistry", "+256414268291"));
        add(new Hospital("Norvik Hospital",
                "Buganda Rd, Kampala", 0.3202, 32.5682,
                4.4f, "open", "ENT · Eye · Dermatology · General Surgery", "+256312280140"));
        add(new Hospital("Victoria Hospital",
                "Tank Hill Rd, Muyenga", 0.2853, 32.5793,
                4.3f, "open24", "Obstetrics · Gynaecology · Paediatrics · ICU", "+256312292899"));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        hospitalContainer  = findViewById(R.id.hospitalContainer);
        tvCurrentLocation  = findViewById(R.id.tvCurrentLocation);
        spinnerRadius      = findViewById(R.id.spinnerRadius);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnSort = findViewById(R.id.btnSort);
        if (btnSort != null) btnSort.setOnClickListener(v -> {
            sortByNearest = !sortByNearest;
            btnSort.setText(sortByNearest ? "Sort: Nearest" : "Sort: Rating");
            renderHospitals();
        });

        // Map setup
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapContainer);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Radius spinner
        if (spinnerRadius != null) {
            spinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { renderHospitals(); }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
        }

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
                updateLocationLabel(location);
                if (googleMap != null) centerMapOnUser(location);
            } else {
                // Default to Kampala city centre
                Location defaultLoc = new Location("default");
                defaultLoc.setLatitude(0.3163);
                defaultLoc.setLongitude(32.5822);
                userLocation = defaultLoc;
                updateLocationLabel(defaultLoc);
            }
            renderHospitals();
        });
    }

    private void updateLocationLabel(Location loc) {
        if (tvCurrentLocation != null)
            tvCurrentLocation.setText(String.format("%.4f°N, %.4f°E • Kampala", loc.getLatitude(), loc.getLongitude()));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        // Add hospital markers
        for (Hospital h : hospitals) {
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(
                    h.status.equals("open24") ? BitmapDescriptorFactory.HUE_VIOLET :
                    h.status.equals("open")   ? BitmapDescriptorFactory.HUE_GREEN :
                                                BitmapDescriptorFactory.HUE_RED);
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(h.lat, h.lng))
                    .title(h.name)
                    .snippet(h.status.equals("open24") ? "Open 24 Hours" :
                             h.status.equals("open")   ? "Open Now" : "Closed")
                    .icon(icon));
        }

        // Center on Kampala
        LatLng kampala = new LatLng(0.3163, 32.5822);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kampala, 13f));

        if (userLocation != null) centerMapOnUser(userLocation);
    }

    private void centerMapOnUser(Location loc) {
        LatLng userLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        googleMap.addMarker(new MarkerOptions()
                .position(userLatLng)
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f));
    }

    private float getSelectedRadius() {
        if (spinnerRadius == null) return 25f;
        switch (spinnerRadius.getSelectedItemPosition()) {
            case 0: return 5f;
            case 1: return 10f;
            case 2: return 25f;
            default: return 25f;
        }
    }

    private float distanceTo(Hospital h) {
        if (userLocation == null) return 999f;
        float[] result = new float[1];
        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                h.lat, h.lng, result);
        return result[0] / 1000f; // km
    }

    private void renderHospitals() {
        if (hospitalContainer == null) return;
        hospitalContainer.removeAllViews();

        float maxRadius = getSelectedRadius();
        List<Hospital> filtered = new ArrayList<>();
        for (Hospital h : hospitals) {
            if (distanceTo(h) <= maxRadius) filtered.add(h);
        }

        if (sortByNearest) {
            filtered.sort(Comparator.comparingDouble(this::distanceTo));
        } else {
            filtered.sort((a, b) -> Float.compare(b.rating, a.rating));
        }

        for (Hospital h : filtered) {
            View card = buildHospitalCard(h);
            hospitalContainer.addView(card);
        }

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No hospitals found within " + (int) maxRadius + " km radius");
            empty.setPadding(32, 48, 32, 48);
            empty.setTextColor(0xFF6B7280);
            empty.setGravity(android.view.Gravity.CENTER);
            hospitalContainer.addView(empty);
        }
    }

    private View buildHospitalCard(Hospital h) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_hospital_card, hospitalContainer, false);
        if (card == null) {
            // Fallback: build programmatically if layout not found
            return buildHospitalCardProgrammatic(h);
        }

        TextView tvName    = card.findViewById(R.id.tvHospitalName);
        TextView tvAddr    = card.findViewById(R.id.tvHospitalAddress);
        TextView tvRating  = card.findViewById(R.id.tvHospitalRating);
        TextView tvStatus  = card.findViewById(R.id.tvHospitalStatus);
        TextView tvServices= card.findViewById(R.id.tvHospitalServices);
        TextView tvDist    = card.findViewById(R.id.tvHospitalDistance);
        ImageView btnDir   = card.findViewById(R.id.btnDirections);
        Button    btnCall  = card.findViewById(R.id.btnCall);

        if (tvName    != null) tvName.setText(h.name);
        if (tvAddr    != null) tvAddr.setText(h.address);
        if (tvServices!= null) tvServices.setText(h.services);

        float dist = distanceTo(h);
        if (tvRating != null) tvRating.setText(String.format("★ %.1f", h.rating));
        if (tvDist   != null) tvDist.setText(dist < 100 ? String.format("%.1f km", dist) : "—");

        if (tvStatus != null) {
            switch (h.status) {
                case "open24":
                    tvStatus.setText("Open 24h");
                    tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                    break;
                case "open":
                    tvStatus.setText("Open");
                    tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                    break;
                default:
                    tvStatus.setText("Closed");
                    tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF4444));
            }
        }

        if (btnDir != null) btnDir.setOnClickListener(v -> openDirections(h));
        if (btnCall != null) btnCall.setOnClickListener(v -> callHospital(h.phone));

        return card;
    }

    /** Programmatic fallback card if XML layout is missing */
    private View buildHospitalCardProgrammatic(Hospital h) {
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

        // Row 1: name + status badge
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(h.name);
        tvName.setTextColor(0xFF1A1A2E);
        tvName.setTextSize(14f);
        tvName.setTypeface(tvName.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameParams);
        row1.addView(tvName);

        TextView tvStatus = new TextView(this);
        String statusText = h.status.equals("open24") ? "Open 24h" : h.status.equals("open") ? "Open" : "Closed";
        tvStatus.setText(statusText);
        tvStatus.setTextColor(0xFFFFFFFF);
        tvStatus.setTextSize(11f);
        tvStatus.setPadding(20, 6, 20, 6);
        int bgColor = h.status.equals("closed") ? 0xFFEF4444 : 0xFF10B981;
        tvStatus.setBackgroundColor(bgColor);
        row1.addView(tvStatus);
        inner.addView(row1);

        // Row 2: rating + distance
        float dist = distanceTo(h);
        TextView tvMeta = new TextView(this);
        tvMeta.setText(String.format("★ %.1f  •  %.1f km  •  %s", h.rating, dist, h.address));
        tvMeta.setTextColor(0xFF6B7280);
        tvMeta.setTextSize(12f);
        tvMeta.setPadding(0, 8, 0, 0);
        inner.addView(tvMeta);

        // Services
        TextView tvSvc = new TextView(this);
        tvSvc.setText(h.services);
        tvSvc.setTextColor(0xFF6B7280);
        tvSvc.setTextSize(12f);
        tvSvc.setPadding(0, 8, 0, 16);
        inner.addView(tvSvc);

        // Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnDir = new Button(this);
        btnDir.setText("🗺 Directions");
        btnDir.setTextSize(12f);
        btnDir.setBackgroundColor(0xFF7B5EA7);
        btnDir.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bp.setMarginEnd(16);
        btnDir.setLayoutParams(bp);
        btnDir.setOnClickListener(v -> openDirections(h));
        btnRow.addView(btnDir);

        Button btnCall = new Button(this);
        btnCall.setText("📞 Call");
        btnCall.setTextSize(12f);
        btnCall.setBackgroundColor(0xFFF3F4F6);
        btnCall.setTextColor(0xFF7B5EA7);
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnCall.setLayoutParams(bp2);
        btnCall.setOnClickListener(v -> callHospital(h.phone));
        btnRow.addView(btnCall);

        inner.addView(btnRow);
        card.addView(inner);
        return card;
    }

    private void openDirections(Hospital h) {
        Uri uri = Uri.parse("geo:" + h.lat + "," + h.lng + "?q=" + Uri.encode(h.name));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Fallback to browser maps
            Uri webUri = Uri.parse("https://maps.google.com/?q=" + Uri.encode(h.name + " Kampala Uganda"));
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private void callHospital(String phone) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
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
            renderHospitals(); // render with no distance info
        }
    }
}
