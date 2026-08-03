package com.example.deundeun.ui.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deundeun.data.model.GuardianNotice;
import com.example.deundeun.databinding.ItemGuardianNoticeBinding;

import java.util.ArrayList;
import java.util.List;

/** Figma 83:284 — 보호자에게 보낸 알림 목록. */
public class GuardianNoticeAdapter
        extends RecyclerView.Adapter<GuardianNoticeAdapter.NoticeHolder> {

    private final List<GuardianNotice> items = new ArrayList<>();

    public void submit(List<GuardianNotice> notices) {
        items.clear();
        items.addAll(notices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoticeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoticeHolder(ItemGuardianNoticeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NoticeHolder extends RecyclerView.ViewHolder {

        private final ItemGuardianNoticeBinding binding;

        NoticeHolder(ItemGuardianNoticeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GuardianNotice notice) {
            binding.noticeTitle.setText(notice.getTitle());
            binding.noticeDesc.setText(notice.getDescription());
            binding.noticeDate.setText(notice.getDate());
            binding.noticeTime.setText(notice.getTime());

            // 날짜와 시각이 따로 읽히면 어색하므로 카드 한 장을 한 덩어리로 묶는다.
            itemView.setContentDescription(notice.getTitle() + ", " + notice.getDescription()
                    + ", " + notice.getDate() + " " + notice.getTime());
        }
    }
}
