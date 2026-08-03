package com.example.deundeun.ui.call;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ItemCallMessageBinding;

import java.util.ArrayList;
import java.util.List;

/** Figma 125:187 — 통화 대화 목록. */
public class CallMessageAdapter extends RecyclerView.Adapter<CallMessageAdapter.MessageHolder> {

    private final List<CallMessage> items = new ArrayList<>();

    public void add(CallMessage message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
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
        holder.bind(items.get(position));
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

            // 말풍선을 보낸 쪽 아바타 옆에 붙인다.
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
    }
}
