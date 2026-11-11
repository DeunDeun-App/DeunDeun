package com.example.deundeun.ui.translate;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.disaster.DisasterMessageResponse;
import com.example.deundeun.data.network.dto.disaster.EasyMessageResponse;
import com.example.deundeun.data.repository.DisasterRepository;
import com.example.deundeun.databinding.ActivityDisasterTranslateBinding;
import com.example.deundeun.databinding.ViewTranslateResultBinding;
import com.example.deundeun.ui.common.InsetsUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 재난문자 쉬운 말로 보기
 */
public class DisasterTranslateActivity extends AppCompatActivity {

    private ActivityDisasterTranslateBinding binding;
    private final DisasterRepository repository = new DisasterRepository();

    private final List<DisasterMessageResponse> messages = new ArrayList<>();
    private final Map<String, EasyMessageResponse> easyMessages = new HashMap<>();
    private int position;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDisasterTranslateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyVerticalInsets(binding.translateScroll);

        binding.header.setOnBackClickListener(v -> finish());
        binding.btnPrevMessage.setOnClickListener(v -> showMessage(position - 1));
        binding.btnNextMessage.setOnClickListener(v -> showMessage(position + 1));

        loadMessages(false);
    }

    private void loadMessages(boolean refresh) {
        binding.originalMessage.setText(R.string.translate_list_loading);
        showStatus(getString(R.string.translate_list_loading), true, null);

        repository.getRecentMessages(refresh, result -> {
            if (isFinishing()) {
                return;
            }
            List<DisasterMessageResponse> data = result.getData();
            if (!result.isSuccess() || data == null) {
                String message = result.getMessageOr(getString(R.string.error_network));
                binding.originalMessage.setText(message);
                showStatus(getString(R.string.translate_retry, message), false, v -> loadMessages(true));
                return;
            }

            messages.clear();
            messages.addAll(data);
            if (messages.isEmpty()) {
                binding.originalMessage.setText(R.string.translate_empty);
                binding.resultCard.setVisibility(View.GONE);
                return;
            }
            showMessage(0);
        });
    }

    private void showMessage(int index) {
        if (index < 0 || index >= messages.size()) {
            return;
        }
        position = index;
        DisasterMessageResponse message = messages.get(index);

        binding.originalMessage.setText(message.getText());
        String meta = metaText(message);
        binding.originalMeta.setText(meta);
        binding.originalMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        binding.messagePager.setVisibility(messages.size() > 1 ? View.VISIBLE : View.GONE);
        binding.messagePosition.setText(getString(R.string.translate_position, index + 1, messages.size()));
        binding.btnPrevMessage.setEnabled(index > 0);
        binding.btnPrevMessage.setAlpha(index > 0 ? 1f : 0.3f);
        binding.btnNextMessage.setEnabled(index < messages.size() - 1);
        binding.btnNextMessage.setAlpha(index < messages.size() - 1 ? 1f : 0.3f);

        binding.resultCard.setVisibility(View.VISIBLE);
        EasyMessageResponse cached = easyMessages.get(message.getId());
        if (cached != null) {
            bindEasyMessage(cached);
        } else {
            requestEasyMessage(message);
        }
    }

    private void requestEasyMessage(DisasterMessageResponse message) {
        showStatus(getString(R.string.translate_loading), true, null);

        repository.toEasyMessage(message.getId(), result -> {
            if (isFinishing()) {
                return;
            }
            EasyMessageResponse data = result.getData();
            if (result.isSuccess() && data != null) {
                easyMessages.put(message.getId(), data);
            }

            boolean stillShown = !messages.isEmpty()
                    && messages.get(position).getId().equals(message.getId());
            if (!stillShown) {
                return;
            }

            if (result.isSuccess() && data != null) {
                bindEasyMessage(data);
            } else if (result.hasCode("MESSAGE_NOT_FOUND")) {
                showStatus(getString(R.string.translate_retry,
                        result.getMessageOr(getString(R.string.error_network))), false,
                        v -> loadMessages(true));
            } else {
                showStatus(getString(R.string.translate_retry,
                        result.getMessageOr(getString(R.string.error_network))), false,
                        v -> requestEasyMessage(message));
            }
        });
    }

    private void bindEasyMessage(EasyMessageResponse easy) {
        binding.resultStatus.setVisibility(View.GONE);
        binding.resultHeader.setVisibility(View.VISIBLE);
        binding.resultBadge.setText(easy.isAiGenerated()
                ? R.string.translate_badge : R.string.translate_basic_badge);
        binding.resultBadgeDesc.setText(easy.isAiGenerated()
                ? R.string.translate_badge_desc : R.string.translate_basic_desc);

        binding.resultList.removeAllViews();
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_m);
        for (EasyMessageResponse.Item item : easy.getItems()) {
            ViewTranslateResultBinding row = ViewTranslateResultBinding.inflate(
                    getLayoutInflater(), binding.resultList, false);
            row.resultEmoji.setText(PictogramEmoji.of(item.getPictogram()));
            row.resultTitle.setText(item.getTitle());
            row.resultDesc.setText(item.getDescription());
            row.resultDesc.setVisibility(isBlank(item.getDescription()) ? View.GONE : View.VISIBLE);
            row.getRoot().setContentDescription(item.getTitle() + ". " + item.getDescription());

            if (binding.resultList.getChildCount() > 0) {
                ((ViewGroup.MarginLayoutParams) row.getRoot().getLayoutParams())
                        .topMargin = spacing;
            }
            binding.resultList.addView(row.getRoot());
        }

        List<String> actions = easy.getActions();
        binding.actionCard.setVisibility(actions.isEmpty() ? View.GONE : View.VISIBLE);
        binding.actionDesc.setText(String.join("\n", actions));
    }

    private void showStatus(String text, boolean loading, @Nullable View.OnClickListener retry) {
        binding.resultHeader.setVisibility(View.GONE);
        binding.resultList.removeAllViews();
        binding.actionCard.setVisibility(View.GONE);

        binding.resultStatus.setVisibility(View.VISIBLE);
        binding.resultProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.resultStatusText.setText(text);
        binding.resultStatus.setOnClickListener(retry);
        binding.resultStatus.setClickable(retry != null);
    }

    private String metaText(DisasterMessageResponse message) {
        String region = message.getRegion() == null ? "" : message.getRegion().trim();
        String sentAt = sentAtText(message.getSentAt());
        if (region.isEmpty()) {
            return sentAt;
        }
        return sentAt.isEmpty() ? region : getString(R.string.translate_meta, region, sentAt);
    }

    private String sentAtText(@Nullable String sentAt) {
        if (sentAt == null || sentAt.length() < 16) {
            return "";
        }
        try {
            int month = Integer.parseInt(sentAt.substring(5, 7));
            int day = Integer.parseInt(sentAt.substring(8, 10));
            int hour = Integer.parseInt(sentAt.substring(11, 13));
            int minute = Integer.parseInt(sentAt.substring(14, 16));
            return getString(R.string.translate_sent_at, month, day, hour, minute);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }
}
