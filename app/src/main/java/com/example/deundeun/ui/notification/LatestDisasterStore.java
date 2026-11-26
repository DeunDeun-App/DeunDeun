package com.example.deundeun.ui.notification;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.deundeun.data.location.LocationReader;
import com.example.deundeun.data.network.dto.disaster.DisasterMessageResponse;
import com.example.deundeun.data.repository.DisasterRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 내 지역의 가장 최근 재난문자를 앱이 켜져 있는 동안 기억해 둔다.
 *
 * 위치를 새로 잡으면 최대 10초가 걸려서, 알림 화면을 열 때 불러오면 재난문자가 늦게 뜬다.
 * 그래서 앱을 켤 때 {@link #preload(Context)} 로 미리 불러 두고,
 * 알림 화면은 기억해 둔 것을 바로 보여준 뒤 뒤에서 다시 불러와 바뀌면 고친다.
 *
 * 모든 메서드는 메인 스레드에서 부른다.
 */
public final class LatestDisasterStore {

    public interface Listener {
        void onLatestChanged();
    }

    @Nullable
    private static DisasterMessageResponse latest;
    /** 한 번이라도 서버 응답을 받았는지 */
    private static boolean loaded;
    private static boolean loading;
    /** 마지막으로 알아낸 지역. 이번에 위치를 못 잡으면 이것으로 조회한다 */
    @Nullable
    private static String lastRegion;
    private static final List<Listener> listeners = new ArrayList<>();

    private LatestDisasterStore() {
    }

    /** 최근 재난문자를 불러오기 시작한다. 이미 불러오는 중이면 아무것도 하지 않는다 */
    @MainThread
    public static void preload(Context context) {
        if (loading) return;
        loading = true;

        Context app = context.getApplicationContext();
        if (!LocationReader.hasPermission(app)) {
            fetch(null);
            return;
        }
        LocationReader reader = new LocationReader(app);
        reader.read((location, address) -> {
            String region = LocationReader.region(address);
            if (region != null) {
                lastRegion = region;
            }
            fetch(lastRegion);
            reader.shutdown();
        });
    }

    private static void fetch(@Nullable String region) {
        new DisasterRepository().getRecentMessages(false, region, result -> {
            loading = false;
            List<DisasterMessageResponse> data = result.getData();
            if (result.isSuccess() && data != null) {
                latest = newest(data);
                loaded = true;
            }
            // 실패하면 전에 기억해 둔 것을 그대로 쓴다
            for (Listener listener : new ArrayList<>(listeners)) {
                listener.onLatestChanged();
            }
        });
    }

    @Nullable
    private static DisasterMessageResponse newest(List<DisasterMessageResponse> messages) {
        DisasterMessageResponse newest = null;
        long newestAt = Long.MIN_VALUE;
        for (DisasterMessageResponse message : messages) {
            long at = NotificationRules.parseSentAt(message.getSentAt());
            if (newest == null || at > newestAt) {
                newest = message;
                newestAt = at;
            }
        }
        return newest;
    }

    /** 서버 응답을 한 번이라도 받았는지. 받기 전에는 {@link #get()} 이 null 이어도 "없음" 이 아니다 */
    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean isLoading() {
        return loading;
    }

    @Nullable
    public static DisasterMessageResponse get() {
        return latest;
    }

    public static void addListener(Listener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
