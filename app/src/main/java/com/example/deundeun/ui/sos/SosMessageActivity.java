package com.example.deundeun.ui.sos;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.sos.SosRequest;
import com.example.deundeun.data.network.dto.sos.SosResponse;
import com.example.deundeun.data.repository.SosRepository;
import com.example.deundeun.databinding.ActivitySosMessageBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.SessionNavigator;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 보낼 메시지 미리보기와 전송.
 *
 * 전송을 누르면 서버에 요청을 남기고, 보호자 번호와 본문이 채워진 문자앱을 연다.
 * 문자앱은 전송 여부를 알려주지 않으므로, 돌아오면 보냈는지 묻고
 * "보냈어요" 를 누를 때만 전송 완료 화면으로 넘어간다.
 */
public class SosMessageActivity extends AppCompatActivity {

    private static final String MAP_URL = "https://maps.google.com/?q=%.6f,%.6f";

    private static final String STATE_AWAITING = "awaiting_confirm";
    private static final String STATE_SOS_ID = "sos_id";
    private static final String STATE_PHONES = "phones";
    private static final String STATE_MESSAGE = "message";
    private static final String STATE_HAS_LOCATION = "has_location";

    private ActivitySosMessageBinding binding;
    private SosRepository repository;
    private LocationReader locationReader;

    @Nullable
    private SosAnswers answers;
    private String guardianName;

    @Nullable
    private Location location;
    @Nullable
    private String address;
    /** 위치 읽기가 끝났는지. 못 읽은 경우도 포함 */
    private boolean locationChecked;
    private boolean shareLocation;
    private boolean sending;

    // 서버에 남긴 요청. 문자앱을 다시 열 때 그대로 쓴다
    @Nullable
    private Long sosId;
    @Nullable
    private ArrayList<String> phones;
    @Nullable
    private String sentMessage;
    private boolean sentHasLocation;
    /** 문자앱을 열었고, 보냈는지 아직 답을 못 받았는지 */
    private boolean awaitingConfirm;
    @Nullable
    private AlertDialog confirmDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySosMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.messageScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        answers = (SosAnswers) getIntent().getSerializableExtra(SosAnswers.EXTRA);
        guardianName = getIntent().getStringExtra(SosActivity.EXTRA_GUARDIAN_NAME);
        shareLocation = getIntent().getBooleanExtra(SosActivity.EXTRA_SHARE_LOCATION, true);
        if (guardianName == null) {
            guardianName = getString(R.string.sos_guardian_fallback);
        }
        repository = new SosRepository(this);

        binding.guardianName.setText(guardianName);
        bindMessage();

        binding.btnEdit.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> send());

        locationReader = new LocationReader(this);

        if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_AWAITING)) {
            // 문자앱에 가 있는 동안 화면이 다시 만들어졌다. 위치는 다시 읽지 않고 보낸 내용을 그대로 쓴다
            sosId = savedInstanceState.getLong(STATE_SOS_ID);
            phones = savedInstanceState.getStringArrayList(STATE_PHONES);
            sentMessage = savedInstanceState.getString(STATE_MESSAGE);
            sentHasLocation = savedInstanceState.getBoolean(STATE_HAS_LOCATION);
            awaitingConfirm = true;
            binding.messageBody.setText(sentMessage);
            setSending(true);
            return;
        }
        readLocation();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (awaitingConfirm && sosId != null) {
            outState.putBoolean(STATE_AWAITING, true);
            outState.putLong(STATE_SOS_ID, sosId);
            outState.putStringArrayList(STATE_PHONES, phones);
            outState.putString(STATE_MESSAGE, sentMessage);
            outState.putBoolean(STATE_HAS_LOCATION, sentHasLocation);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (confirmDialog != null) {
            confirmDialog.dismiss();
        }
        if (locationReader != null) {
            locationReader.shutdown();
        }
    }

    private void readLocation() {
        if (!shareLocation || !LocationReader.hasPermission(this)) {
            locationChecked = true;
            bindLocation(false);
            bindMessage();
            return;
        }
        binding.gpsTitle.setText(R.string.sos_gps_loading_title);
        binding.gpsValue.setText(R.string.sos_gps_loading_value);
        binding.gpsBadge.setVisibility(View.GONE);

        locationReader.read((found, foundAddress) -> {
            location = found;
            address = foundAddress;
            locationChecked = true;
            bindLocation(found != null);
            bindMessage();
        });
    }

    private void bindLocation(boolean found) {
        if (found) {
            binding.gpsTitle.setText(R.string.sos_gps_title);
            binding.gpsValue.setText(address != null
                    ? getString(R.string.sos_gps_near, address)
                    : getString(R.string.sos_gps_no_address));
            binding.gpsBadge.setVisibility(View.VISIBLE);
        } else if (!shareLocation) {
            binding.gpsTitle.setText(R.string.sos_gps_off_title);
            binding.gpsValue.setText(R.string.sos_gps_missing_value);
            binding.gpsBadge.setVisibility(View.GONE);
        } else {
            binding.gpsTitle.setText(R.string.sos_gps_missing_title);
            binding.gpsValue.setText(R.string.sos_gps_missing_value);
            binding.gpsBadge.setVisibility(View.GONE);
        }
    }

    private void bindMessage() {
        binding.messageBody.setText(buildMessage());
    }

    private String buildMessage() {
        if (answers == null) {
            return "";
        }
        String[] situationSentences = getResources().getStringArray(R.array.sos_situation_sentences);
        int index = answers.getSituationIndex() == null ? 0 : answers.getSituationIndex();
        String situation = situationSentences[Math.min(index, situationSentences.length - 1)];

        String trapped = getString(Boolean.TRUE.equals(answers.getTrapped())
                ? R.string.sos_trapped_line : R.string.sos_not_trapped_line);
        String hurt = getString(Boolean.TRUE.equals(answers.getHurt())
                ? R.string.sos_hurt_line : R.string.sos_not_hurt_line);
        String message = getString(R.string.sos_preview_message, guardianName, situation, trapped, hurt);

        if (location == null) {
            // 아직 읽는 중이면 붙이지 않는다. 못 읽었으면 보호자가 전화하도록 알린다
            if (!locationChecked) {
                return message;
            }
            return message + "\n" + getString(shareLocation
                    ? R.string.sos_location_missing_line : R.string.sos_location_off_line);
        }
        String mapUrl = String.format(Locale.US, MAP_URL, location.getLatitude(), location.getLongitude());
        String place = address != null ? address : getString(R.string.sos_gps_no_address);
        return message + "\n" + getString(R.string.sos_location_line, place, mapUrl);
    }

    private void send() {
        if (sending || answers == null || !answers.isComplete()) {
            return;
        }
        if (!repository.isLoggedIn()) {
            Toast.makeText(this, R.string.sos_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        // 위치를 아직 읽는 중이면 기다리지 않고 지금까지 아는 것으로 보낸다
        locationReader.cancel();
        locationChecked = true;

        String message = buildMessage();
        SosRequest request = new SosRequest(
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                answers.getSituationCode(), answers.getTrapped(), answers.getHurt(), message);

        setSending(true);
        repository.request(request, result -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            SosResponse response = result.getData();
            if ("SESSION_EXPIRED".equals(result.getCode())) {
                SessionNavigator.toLogin(this, R.string.error_session_expired);
                return;
            }
            if (!result.isSuccess() || response == null) {
                setSending(false);
                Toast.makeText(this, result.getMessageOr(getString(R.string.sos_network_error)),
                        Toast.LENGTH_LONG).show();
                return;
            }
            openSmsApp(response, message);
        });
    }

    private void openSmsApp(SosResponse response, String message) {
        sosId = response.getSosId();
        phones = new ArrayList<>(response.getPhones());
        sentMessage = message;
        sentHasLocation = location != null;
        openSmsApp();
    }

    /** 서버 요청은 한 번만 하고, 다시 보내기는 문자앱만 다시 연다. */
    private void openSmsApp() {
        Intent sms = new Intent(Intent.ACTION_SENDTO,
                Uri.parse("smsto:" + Uri.encode(TextUtils.join(";", phones))));
        sms.putExtra("sms_body", sentMessage);

        try {
            startActivity(sms);
        } catch (ActivityNotFoundException e) {
            setSending(false);
            Toast.makeText(this, R.string.sos_no_sms_app, Toast.LENGTH_LONG).show();
            return;
        }
        // 문자앱은 전송했는지 알려주지 않으므로, 돌아오면 사용자에게 묻는다
        awaitingConfirm = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (awaitingConfirm) {
            showSendConfirm();
        }
    }

    private void showSendConfirm() {
        if (confirmDialog != null && confirmDialog.isShowing()) {
            return;
        }
        confirmDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.sos_confirm_title)
                .setMessage(getString(R.string.sos_confirm_message, guardianName))
                .setCancelable(false)
                .setPositiveButton(R.string.sos_confirm_sent, (dialog, which) -> onSmsSent())
                .setNegativeButton(R.string.sos_confirm_retry, (dialog, which) -> openSmsApp())
                .show();
    }

    private void onSmsSent() {
        awaitingConfirm = false;
        repository.markSent(sosId, result -> {
            // 기록 실패는 사용자에게 알리지 않는다. 문자는 이미 보냈다
        });

        Intent sent = new Intent(this, SosSentActivity.class);
        sent.putExtra(SosActivity.EXTRA_GUARDIAN_NAME, guardianName);
        sent.putExtra(SosSentActivity.EXTRA_HAS_LOCATION, sentHasLocation);
        startActivity(sent);
        finish();
    }

    private void setSending(boolean sending) {
        this.sending = sending;
        binding.btnSend.setEnabled(!sending);
        binding.btnEdit.setEnabled(!sending);
    }
}
