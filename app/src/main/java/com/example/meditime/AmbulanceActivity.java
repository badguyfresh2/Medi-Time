package com.example.meditime;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AmbulanceActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1003;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng userLatLng;
    private Marker userMarker, ambulanceMarker;

    // Views
    private Button btnSOS;
    private TextView tvLocationAddress, tvLocationCoords, tvETA, tvSOSStatus;
    private ProgressBar progressSOS;
    private EditText etPickupAddress, etContactNumber;
    private Spinner spinnerEmergencyType;

    // SOS hold timer
    private CountDownTimer sosHoldTimer;
    private boolean sosActivated = false;
    private ValueAnimator pulseAnimator;

    // Simulated ambulance positions (Kampala routes)
    private final LatLng[] ambulanceRoute = {
            new LatLng(0.3364, 32.5760), // Mulago
            new LatLng(0.3280, 32.5820),
            new LatLng(0.3220, 32.5830),
            new LatLng(0.3180, 32.5825),
    };
    private int routeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Views
        btnSOS              = findViewById(R.id.btnSOS);
        tvLocationAddress   = findViewById(R.id.tvLocationAddress);
        tvLocationCoords    = findViewById(R.id.tvLocationCoords);
        tvETA               = findViewById(R.id.tvETA);
        tvSOSStatus         = findViewById(R.id.tvSOSStatus);
        progressSOS         = findViewById(R.id.progressSOS);
        etPickupAddress     = findViewById(R.id.etPickupAddress);
        etContactNumber     = findViewById(R.id.etContactNumber);
        spinnerEmergencyType= findViewById(R.id.spinnerEmergencyType);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Bottom nav
        LinearLayout navHome   = findViewById(R.id.nav_home);
        LinearLayout navDoctor = findViewById(R.id.nav_doctor);
        LinearLayout navProfile= findViewById(R.id.nav_profile);
        if (navHome   != null) navHome.setOnClickListener(v -> { startActivity(new Intent(this, HomeActivity.class)); finish(); });
        if (navDoctor != null) navDoctor.setOnClickListener(v -> { startActivity(new Intent(this, DoctorActivity.class)); finish(); });
        if (navProfile!= null) navProfile.setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class)); finish(); });

        // SOS button — press & hold 2 seconds
        setupSOSButton();

        // Emergency call buttons
        setupCallButtons();

        // Book button
        Button btnBook = findViewById(R.id.btnBookAmbulance);
        if (btnBook != null) btnBook.setOnClickListener(v -> bookAmbulance());

        Button btnUpdateLocation = findViewById(R.id.btnUpdateLocation);
        if (btnUpdateLocation != null) btnUpdateLocation.setOnClickListener(v -> detectLocation());

        // Map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.ambMapContainer);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        requestLocation();
    }

    private void setupSOSButton() {
        if (btnSOS == null) return;

        btnSOS.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!sosActivated) startSOSCountdown();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!sosActivated) cancelSOSCountdown();
                    break;
            }
            return true;
        });
    }

    private void startSOSCountdown() {
        if (progressSOS != null) { progressSOS.setVisibility(View.VISIBLE); progressSOS.setProgress(0); }
        if (tvSOSStatus != null) tvSOSStatus.setText("Hold for 2 seconds…");

        sosHoldTimer = new CountDownTimer(2000, 50) {
            @Override public void onTick(long remaining) {
                int progress = (int) ((2000 - remaining) * 100 / 2000);
                if (progressSOS != null) progressSOS.setProgress(progress);
            }
            @Override public void onFinish() {
                activateSOS();
            }
        }.start();
    }

    private void cancelSOSCountdown() {
        if (sosHoldTimer != null) sosHoldTimer.cancel();
        if (progressSOS  != null) { progressSOS.setVisibility(View.GONE); progressSOS.setProgress(0); }
        if (tvSOSStatus  != null) tvSOSStatus.setText("Press & hold SOS for emergency dispatch");
    }

    private void activateSOS() {
        sosActivated = true;
        if (progressSOS != null) progressSOS.setVisibility(View.GONE);
        if (tvSOSStatus != null) tvSOSStatus.setText("🚨 SOS ACTIVATED — Ambulance dispatched!");
        if (btnSOS      != null) { btnSOS.setText("DISPATCHED"); btnSOS.setBackgroundColor(0xFF4CAF50); }
        if (tvETA       != null) tvETA.setText("ETA: ~8 min");

        Toast.makeText(this, "🚑 Emergency services notified! Ambulance en route.", Toast.LENGTH_LONG).show();

        // Start simulated ambulance tracking on map
        simulateAmbulanceMovement();

        // Auto-call emergency after dispatch
        new android.os.Handler(getMainLooper()).postDelayed(() ->
                makeCall("999"), 3000);
    }

    private void simulateAmbulanceMovement() {
        if (googleMap == null || userLatLng == null) return;

        // Add ambulance marker starting at Mulago
        ambulanceMarker = googleMap.addMarker(new MarkerOptions()
                .position(ambulanceRoute[0])
                .title("Ambulance")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Animate along route every 2 seconds
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        Runnable moveAmbulance = new Runnable() {
            int idx = 0;
            @Override public void run() {
                if (ambulanceMarker == null || isDestroyed()) return;
                if (idx < ambulanceRoute.length) {
                    ambulanceMarker.setPosition(ambulanceRoute[idx]);
                    idx++;
                    int etaMins = ambulanceRoute.length - idx;
                    if (tvETA != null) tvETA.setText(etaMins > 0 ? "ETA: ~" + etaMins * 2 + " min" : "🚑 Arrived!");
                    handler.postDelayed(this, 2000);
                }
            }
        };
        handler.post(moveAmbulance);
    }

    private void setupCallButtons() {
        Button btnCallNational = findViewById(R.id.btnCallNational);
        Button btnCallKCCA     = findViewById(R.id.btnCallKCCA);
        Button btnCallMulago   = findViewById(R.id.btnCallMulago);
        Button btnCallCase     = findViewById(R.id.btnCallCase);

        if (btnCallNational != null) btnCallNational.setOnClickListener(v -> makeCall("999"));
        if (btnCallKCCA     != null) btnCallKCCA.setOnClickListener(v -> makeCall("112"));
        if (btnCallMulago   != null) btnCallMulago.setOnClickListener(v -> makeCall("+256415540001"));
        if (btnCallCase     != null) btnCallCase.setOnClickListener(v -> makeCall("+256312260011"));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        // Show nearby hospitals as reference points
        addHospitalMarkers();

        LatLng kampala = new LatLng(0.3163, 32.5822);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kampala, 14f));

        if (userLatLng != null) centerMap();
    }

    private void addHospitalMarkers() {
        if (googleMap == null) return;
        double[][] hospitals = {
                {0.3364, 32.5760}, {0.3124, 32.5537}, {0.3280, 32.5890}
        };
        String[] names = {"Mulago Hospital", "Mengo Hospital", "Nakasero Hospital"};
        for (int i = 0; i < hospitals.length; i++) {
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(hospitals[i][0], hospitals[i][1]))
                    .title(names[i])
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
        }
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            detectLocation();
        }
    }

    private void detectLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        if (tvLocationAddress != null) tvLocationAddress.setText("Detecting location...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateLocation(location);
            } else {
                // Default
                Location def = new Location("default");
                def.setLatitude(0.3163); def.setLongitude(32.5822);
                updateLocation(def);
            }
        });
    }

    private void updateLocation(Location location) {
        userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (tvLocationCoords != null)
            tvLocationCoords.setText(String.format(Locale.getDefault(),
                    "%.4f°N, %.4f°E • Kampala, Uganda", location.getLatitude(), location.getLongitude()));

        // Reverse geocode
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    String display = addr.getThoroughfare() != null
                            ? addr.getThoroughfare() + (addr.getSubLocality() != null ? ", " + addr.getSubLocality() : "")
                            : "Kampala, Uganda";
                    runOnUiThread(() -> {
                        if (tvLocationAddress != null) tvLocationAddress.setText(display);
                        if (etPickupAddress  != null && etPickupAddress.getText().toString().isEmpty())
                            etPickupAddress.setText(display);
                    });
                }
            } catch (IOException ignored) {
                runOnUiThread(() -> { if (tvLocationAddress != null) tvLocationAddress.setText("Kampala, Uganda"); });
            }
        }).start();

        if (googleMap != null) centerMap();
    }

    private void centerMap() {
        if (googleMap == null || userLatLng == null) return;
        if (userMarker != null) userMarker.remove();
        userMarker = googleMap.addMarker(new MarkerOptions()
                .position(userLatLng)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
    }

    private void bookAmbulance() {
        String pickup = etPickupAddress != null ? etPickupAddress.getText().toString().trim() : "";
        String contact = etContactNumber != null ? etContactNumber.getText().toString().trim() : "";
        String type = spinnerEmergencyType != null
                ? spinnerEmergencyType.getSelectedItem().toString() : "Emergency";

        if (pickup.isEmpty()) {
            Toast.makeText(this, "⚠️ Please enter your pickup address", Toast.LENGTH_SHORT).show();
            return;
        }

        String msg = "🚑 Ambulance booked!\n"
                + "Type: " + type + "\n"
                + "Pickup: " + pickup + "\n"
                + (contact.isEmpty() ? "" : "Contact: " + contact + "\n")
                + "Estimated arrival: 10–15 minutes";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        if (tvETA != null) tvETA.setText("ETA: ~12 min");

        // Start map tracking
        simulateAmbulanceMovement();

        if (etPickupAddress  != null) etPickupAddress.setText("");
        if (etContactNumber  != null) etContactNumber.setText("");
    }

    private void makeCall(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (sosHoldTimer != null) sosHoldTimer.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }
}
