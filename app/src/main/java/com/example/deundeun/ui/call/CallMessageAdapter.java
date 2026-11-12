package com.example.deundeun.ui.call;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ItemCallMessageBinding;

import java.util.ArrayList;
import java.util.List;

/** 통화 대화 목록. */
public class CallMessageAdapter extends RecyclerView.Adapter<CallMessageAdapter.MessageHolder> {

    private final List<CallMessage> items = new ArrayList<>();

    /** 방금 추가돼서 올라오는 효과를 줄 위치. 한 번 보여주면 지운다. */
    private int animatePosition = RecyclerView.NO_POSITION;

    public void add(CallMessage message) {
        items.add(message);
        animatePosition = items.size() - 1;
        notifyItemInserted(animatePosition);
    }

    public int getLastPosition() {
        return Math.max(0, items.size() - 1);
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MessageHolder(ItemCallMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
        CallMessage message = items.get(position);
        holder.bind(message);

        if (position == animatePosition) {
            animatePosition = RecyclerView.NO_POSITION;
            holder.riseIn(message.isMine());
        } else {
            holder.resetAnimation();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MessageHolder extends RecyclerView.ViewHolder {

        private final ItemCallMessageBinding binding;

        MessageHolder(ItemCallMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CallMessage message) {
            binding.messageBubble.setText(message.getText());

            binding.avatarOperator.setVisibility(message.isMine() ? View.GONE : View.VISIBLE);
            binding.avatarMe.setVisibility(message.isMine() ? View.VISIBLE : View.GONE);

            binding.messageBubble.setBackgroundResource(message.isMine()
                    ? R.drawable.bg_bubble_mine : R.drawable.bg_bubble_other);

            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) binding.messageBubble.getLayoutParams();
            params.removeRule(RelativeLayout.END_OF);
            params.removeRule(RelativeLayout.START_OF);
            if (message.isMine()) {
                params.addRule(RelativeLayout.START_OF, R.id.avatar_me);
            } else {
                params.addRule(RelativeLayout.END_OF, R.id.avatar_operator);
            }
            binding.messageBubble.setLayoutParams(params);

            String speaker = itemView.getContext().getString(
                    message.isMine() ? R.string.cd_me : R.string.cd_operator);
            itemView.setContentDescription(speaker + ", " + message.getText());
        }

        /**
         * 아래에서 올라오며 나타난다.
         * 내 말풍선은 마이크 위 자막 자리에서 올라오는 느낌이 나도록 더 멀리서, 오른쪽을 기준으로 커진다.
         */
        void riseIn(boolean mine) {
            float density = itemView.getResources().getDisplayMetrics().density;
            View bubble = binding.messageBubble;

            itemView.animate().cancel();
            bubble.animate().cancel();

            itemView.setAlpha(0f);
            itemView.setTranslationY((mine ? 90f : 24f) * density);
            itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(mine ? 380L : 260L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();

            bubble.post(() -> {
                bubble.setPivotX(mine ? bubble.getWidth() : 0f);
                bubble.setPivotY(bubble.getHeight());
            });
            bubble.setScaleX(0.85f);
            bubble.setScaleY(0.85f);
            bubble.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(mine ? 380L : 260L)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }

        /** 재사용된 줄에 이전 효과가 남지 않게 한다. */
        void resetAnimation() {
            itemView.animate().cancel();
            binding.messageBubble.animate().cancel();
            itemView.setAlpha(1f);
            itemView.setTranslationY(0f);
            binding.messageBubble.setScaleX(1f);
            binding.messageBubble.setScaleY(1f);
        }
    }
}
