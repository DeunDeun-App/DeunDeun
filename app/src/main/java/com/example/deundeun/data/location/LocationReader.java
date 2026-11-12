package com.example.deundeun.data.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * 지금 위치를 한 번 읽고, 가능하면 주소로 바꿔준다. 안드로이드 기본 {@link LocationManager} 만 쓴다.
 *
 * 권한이 없거나, 위치가 꺼져 있거나, 시간 안에 못 받으면 {@code null} 로 알려준다.
 * SOS 는 위치 없이도 보내야 하므로 실패를 오류로 다루지 않는다.
 */
public class LocationReader {

    public interface Listener {
        /**
         * @param location 읽은 위치. 못 읽으면 null
         * @param address  주소. 못 바꾸면 null
         */
        void onLocation(@Nullable Location location, @Nullable Address address);
    }

    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private static final long TIMEOUT_MS = 10_000;
    // 이보다 오래된 마지막 위치는 쓰지 않는다
    private static final long LAST_KNOWN_MAX_AGE_MS = 2 * 60_000;
    private static final String COUNTRY_PREFIX = "대한민국 ";
    private static final int MAX_RESULTS = 5;
    private static final Pattern PROVINCE = Pattern.compile("[가-힣]+[시도]$");
    private static final Pattern DISTRICT = Pattern.compile(".+[시군구]$");

    private final Context context;
    private final LocationManager locationManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    private Listener listener;
    @Nullable
    private CancellationSignal cancellation;
    @Nullable
    private LocationListener legacyListener;

    public LocationReader(Context context) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** 위치를 읽기 시작한다. 결과는 메인 스레드에서 한 번만 온다. */
    @SuppressLint("MissingPermission")
    public void read(Listener listener) {
        cancel();
        this.listener = listener;

        String provider = pickProvider();
        if (provider == null) {
            deliver(null);
            return;
        }

        Location last = locationManager.getLastKnownLocation(provider);
        if (last != null && ageMs(last) <= LAST_KNOWN_MAX_AGE_MS) {
            deliver(last);
            return;
        }

        handler.postDelayed(this::onTimeout, TIMEOUT_MS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cancellation = new CancellationSignal();
            locationManager.getCurrentLocation(provider, cancellation,
                    ContextCompat.getMainExecutor(context), this::deliver);
        } else {
            legacyListener = this::deliver;
            locationManager.requestSingleUpdate(provider, legacyListener, Looper.getMainLooper());
        }
    }

    /** 읽는 중이면 멈춘다. 이때 결과는 오지 않는다. */
    public void cancel() {
        listener = null;
        handler.removeCallbacksAndMessages(null);
        if (cancellation != null) {
            cancellation.cancel();
            cancellation = null;
        }
        if (legacyListener != null) {
            locationManager.removeUpdates(legacyListener);
            legacyListener = null;
        }
    }

    public void shutdown() {
        cancel();
        geocodeExecutor.shutdownNow();
    }

    @Nullable
    private String pickProvider() {
        if (locationManager == null || !hasPermission(context)) {
            return null;
        }
        // fused 는 대략적인 위치 권한만 있어도 쓸 수 있고, GPS 와 네트워크를 알아서 섞어준다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
            return LocationManager.FUSED_PROVIDER;
        }
        boolean fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return null;
    }

    private void onTimeout() {
        // 실내라 GPS 가 안 잡히면 오래된 위치라도 쓴다
        String provider = pickProvider();
        @SuppressLint("MissingPermission")
        Location last = provider == null ? null : locationManager.getLastKnownLocation(provider);
        deliver(last);
    }

    private void deliver(@Nullable Location location) {
        Listener target = listener;
        if (target == null) {
            return;
        }
        cancel();

        if (location == null || !Geocoder.isPresent()) {
            target.onLocation(location, null);
            return;
        }
        geocodeExecutor.execute(() -> {
            Address address = toAddress(location);
            handler.post(() -> target.onLocation(location, address));
        });
    }

    @Nullable
    @SuppressWarnings("deprecation") // 새 콜백 방식은 API 33 부터라 백그라운드 스레드에서 옛 방식을 쓴다
    private Address toAddress(@NonNull Location location) {
        try {
            List<Address> results = new Geocoder(context, Locale.KOREAN)
                    .getFromLocation(location.getLatitude(), location.getLongitude(), MAX_RESULTS);
            if (results == null || results.isEmpty()) {
                return null;
            }
            for (Address result : results) {
                if (result.getAdminArea() != null) {
                    return result;
                }
            }
            return results.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String addressLine(@Nullable Address address) {
        String line = address == null ? null : address.getAddressLine(0);
        if (line == null) {
            return null;
        }
        return line.startsWith(COUNTRY_PREFIX) ? line.substring(COUNTRY_PREFIX.length()) : line;
    }

    @Nullable
    public static String region(@Nullable Address address) {
        String line = addressLine(address);
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String[] tokens = line.trim().split("\\s+");
        if (!PROVINCE.matcher(tokens[0]).matches()) {
            return null;
        }
        StringBuilder region = new StringBuilder(tokens[0]);
        for (int i = 1; i < tokens.length && DISTRICT.matcher(tokens[i]).matches(); i++) {
            region.append(' ').append(tokens[i]);
        }
        return region.toString();
    }

    private static long ageMs(Location location) {
        return System.currentTimeMillis() - location.getTime();
    }
}
