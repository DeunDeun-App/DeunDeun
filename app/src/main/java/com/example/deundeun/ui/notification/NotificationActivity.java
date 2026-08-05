package com.example.deundeun.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivityNotificationBinding;
import com.example.deundeun.databinding.ItemNotificationBinding;
import com.example.deundeun.ui.common.InsetsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 알림 목록. */
public class NotificationActivity extends AppCompatActivity {

    private ActivityNotificationBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyVerticalInsets(binding.notificationScroll);

        binding.btnBack.setOnClickListener(v -> finish());

        // TODO: 서버에서 받아온 알림으로 대체
        NotificationAdapter adapter = new NotificationAdapter();
        binding.notificationList.setAdapter(adapter);
        adapter.submit(Arrays.asList(
                new Item(R.string.notification_1_title, R.string.notification_1_desc,
                        R.string.notification_1_time, true),
                new Item(R.string.notification_2_title, R.string.notification_2_desc,
                        R.string.notification_2_time, false)));
    }

    static class Item {
        final int titleRes;
        final int descRes;
        final int timeRes;
        final boolean unread;

        Item(int titleRes, int descRes, int timeRes, boolean unread) {
            this.titleRes = titleRes;
            this.descRes = descRes;
            this.timeRes = timeRes;
            this.unread = unread;
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
                binding.notificationTitle.setText(item.titleRes);
                binding.notificationDesc.setText(item.descRes);
                binding.notificationTime.setText(item.timeRes);
                binding.notificationDot.setVisibility(item.unread ? View.VISIBLE : View.INVISIBLE);

                String description = itemView.getContext().getString(item.titleRes) + ", "
                        + itemView.getContext().getString(item.descRes) + ", "
                        + itemView.getContext().getString(item.timeRes);
                if (item.unread) {
                    description += ", " + itemView.getContext().getString(R.string.cd_unread);
                }
                itemView.setContentDescription(description);
            }
        }
    }
}
