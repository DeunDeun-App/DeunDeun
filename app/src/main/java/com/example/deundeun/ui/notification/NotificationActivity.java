package com.example.deundeun.ui.notification;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.disaster.DisasterMessageResponse;
import com.example.deundeun.data.network.dto.record.RecordResponse;
import com.example.deundeun.data.repository.RecordRepository;
import com.example.deundeun.databinding.ActivityNotificationBinding;
import com.example.deundeun.databinding.ItemNotificationBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.simulation.SimulationIntroActivity;
import com.example.deundeun.ui.translate.DisasterTranslateActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * 알림 목록.
 *
 * 두 가지를 보여준다.
 * - 재난문자 도착: 내 지역의 가장 최근 재난문자. 처음 보는 문자면 점을 찍고, 누르면 쉬운 말로 보기로 간다
 * - 오늘 훈련: 지진 대피·화재 대피를 하루 3번씩 했는지. 누르면 재난 시뮬레이션으로 간다
 *
 * 재난문자는 앱을 켤 때 {@link LatestDisasterStore} 가 미리 불러 둔 것을 바로 보여주고,
 * 뒤에서 다시 불러와 바뀌면 고친다. 위치를 잡느라 늦게 뜨지 않게 하려는 것이다.
 * 훈련은 화면에 돌아올 때마다 다시 불러와서, 훈련하고 오면 남은 횟수가 바로 줄어든다.
 * 목록은 최신 것이 위로 오게 정렬한다. 오늘 훈련은 오늘 0시 알림으로 본다.
 */
public class NotificationActivity extends AppCompatActivity
        implements LatestDisasterStore.Listener {

    private static final String PREFS = "notifications";
    /** 알림 화면에서 마지막으로 본 재난문자 id. 이것과 다르면 새 문자로 본다 */
    private static final String KEY_SEEN_DISASTER = "seen_disaster_id";

    private ActivityNotificationBinding binding;
    private NotificationAdapter adapter;

    private RecordRepository recordRepository;
    private SharedPreferences prefs;
    /** 이 화면을 열 때 이미 본 재난문자 id. 화면에 있는 동안 점이 사라지지 않게 열 때 한 번만 읽는다 */
    @Nullable
    private String seenAtOpen;

    @Nullable
    private Item disasterItem;
    @Nullable
    private Item trainingItem;
    /** 두 가지를 모두 불러왔는지. 다 불러오기 전에는 "알림이 없어요" 를 띄우지 않는다 */
    private boolean disasterLoaded;
    private boolean trainingLoaded;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyVerticalInsets(binding.notificationScroll);

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new NotificationAdapter();
        binding.notificationList.setAdapter(adapter);

        recordRepository = new RecordRepository(this);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        seenAtOpen = prefs.getString(KEY_SEEN_DISASTER, null);
        trainingLoaded = false;
        LatestDisasterStore.addListener(this);
        // 뒤에서 최신으로 다시 불러온다. 바뀌면 onLatestChanged 로 알려준다
        LatestDisasterStore.preload(this);
        // 미리 불러 둔 것이 있으면 바로 보여준다
        showDisaster();
        loadTraining();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LatestDisasterStore.removeListener(this);
    }

    // ---- 재난문자 ----

    @Override
    public void onLatestChanged() {
        if (isFinishing() || isDestroyed()) return;
        showDisaster();
    }

    private void showDisaster() {
        disasterItem = disasterItemOf(LatestDisasterStore.get());
        // 처음 불러오는 중이면 아직 "없음" 이 아니다
        disasterLoaded = LatestDisasterStore.isLoaded() || !LatestDisasterStore.isLoading();
        render();
    }

    @Nullable
    private Item disasterItemOf(@Nullable DisasterMessageResponse message) {
        if (message == null) return null;

        String kind = NotificationRules.disasterKind(message.getText(), message.getStep());
        String region = NotificationRules.shortRegion(message.getRegion());
        String desc;
        if (kind != null && region != null) {
            desc = getString(R.string.notification_disaster_desc, kind, region);
        } else if (kind != null || region != null) {
            desc = kind != null ? kind : region;
        } else {
            desc = getString(R.string.notification_disaster_fallback);
        }

        String id = message.getId();
        boolean unread = id != null && !id.equals(seenAtOpen);
        if (id != null) {
            // 한 번 보여주면 읽은 것으로 친다. 이번 화면에는 점을 그대로 둔다
            prefs.edit().putString(KEY_SEEN_DISASTER, id).apply();
        }

        long sentAt = NotificationRules.parseSentAt(message.getSentAt());
        return new Item(getString(R.string.notification_disaster_title), desc,
                sentAt < 0 ? "" : relativeTime(sentAt), Math.max(sentAt, 0), unread,
                new Intent(this, DisasterTranslateActivity.class));
    }

    // ---- 오늘 훈련 ----

    private void loadTraining() {
        if (!recordRepository.isLoggedIn()) {
            trainingItem = null;
            trainingLoaded = true;
            render();
            return;
        }
        recordRepository.getRecords(result -> {
            if (isFinishing() || isDestroyed()) return;
            RecordResponse data = result.getData();
            trainingItem = result.isSuccess() && data != null ? trainingItemOf(data.getRecords()) : null;
            trainingLoaded = true;
            render();
        });
    }

    private Item trainingItemOf(List<RecordResponse.Item> records) {
        String today = NotificationRules.today(System.currentTimeMillis());
        int quakeLeft = NotificationRules.remaining(
                NotificationRules.countToday(records, NotificationRules.EARTHQUAKE_TITLE, today));
        int fireLeft = NotificationRules.remaining(
                NotificationRules.countToday(records, NotificationRules.FIRE_TITLE, today));

        String quake = getString(R.string.sim_scenario_earthquake);
        String fire = getString(R.string.sim_scenario_fire);
        String title;
        String desc;
        if (quakeLeft == 0 && fireLeft == 0) {
            title = getString(R.string.notification_training_done_title);
            desc = getString(R.string.notification_training_done_desc,
                    NotificationRules.DAILY_GOAL);
        } else {
            title = getString(R.string.notification_training_title);
            List<String> parts = new ArrayList<>();
            if (quakeLeft > 0) parts.add(getString(R.string.notification_training_left, quake, quakeLeft));
            if (fireLeft > 0) parts.add(getString(R.string.notification_training_left, fire, fireLeft));
            desc = getString(R.string.notification_training_desc, TextUtils.join(" · ", parts));
        }
        return new Item(title, desc, getString(R.string.notification_time_today),
                startOfToday(), false,
                new Intent(this, SimulationIntroActivity.class));
    }

    // ---- 화면 ----

    private void render() {
        List<Item> items = new ArrayList<>();
        if (disasterItem != null) items.add(disasterItem);
        if (trainingItem != null) items.add(trainingItem);
        // 최신 알림이 위로
        Collections.sort(items, (a, b) -> Long.compare(b.sortTime, a.sortTime));
        adapter.submit(items);

        boolean allLoaded = disasterLoaded && trainingLoaded;
        binding.notificationEmpty.setVisibility(allLoaded && items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static long startOfToday() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTimeInMillis();
    }

    private String relativeTime(long thenMs) {
        long minutes = Math.max(0, (System.currentTimeMillis() - thenMs) / 60_000L);
        if (minutes < 1) return getString(R.string.notification_time_now);
        if (minutes < 60) return getString(R.string.notification_time_minutes, (int) minutes);
        long hours = minutes / 60;
        if (hours < 24) return getString(R.string.notification_time_hours, (int) hours);
        return getString(R.string.notification_time_days, (int) (hours / 24));
    }

    static class Item {
        final String title;
        final String desc;
        final String time;
        /** 정렬 기준 시각. 클수록 위로 */
        final long sortTime;
        final boolean unread;
        final Intent target;

        Item(String title, String desc, String time, long sortTime, boolean unread, Intent target) {
            this.title = title;
            this.desc = desc;
            this.time = time;
            this.sortTime = sortTime;
            this.unread = unread;
            this.target = target;
        }
    }

    static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {

        private final List<Item> items = new ArrayList<>();

        void submit(List<Item> notifications) {
            items.clear();
            items.addAll(notifications);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemNotificationBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {

            private final ItemNotificationBinding binding;

            Holder(ItemNotificationBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(Item item) {
                binding.notificationTitle.setText(item.title);
                binding.notificationDesc.setText(item.desc);
                binding.notificationTime.setText(item.time);
                binding.notificationDot.setVisibility(item.unread ? View.VISIBLE : View.INVISIBLE);

                Context context = itemView.getContext();
                String description = item.title + ", " + item.desc
                        + (item.time.isEmpty() ? "" : ", " + item.time);
                if (item.unread) {
                    description += ", " + context.getString(R.string.cd_unread);
                }
                itemView.setContentDescription(description);
                itemView.setOnClickListener(v -> context.startActivity(item.target));
            }
        }
    }
}
