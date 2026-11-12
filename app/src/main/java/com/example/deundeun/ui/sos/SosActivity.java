package com.example.deundeun.ui.sos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.location.LocationReader;
import com.example.deundeun.data.network.dto.sos.GuardianResponse;
import com.example.deundeun.data.network.dto.user.ProfileResponse;
import com.example.deundeun.data.repository.SosRepository;
import com.example.deundeun.data.repository.UserRepository;
import com.example.deundeun.databinding.ActivitySosBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.SessionNavigator;

import java.util.List;

/**
 * SOS 도움요청 진입.
 *
 * 서버에서 보호자를 불러와 보여주고, "도움 요청하기"를 누르면 그때 위치 권한을 묻는다.
 * 권한을 거절해도 위치 없이 요청은 이어간다.
 */
public class SosActivity extends AppCompatActivity {

    /** 다음 화면들에 넘기는 보호자 이름. */
    public static final String EXTRA_GUARDIAN_NAME = "guardian_name";
    public static final String EXTRA_SHARE_LOCATION = "share_location";

    private ActivitySosBinding binding;
    private SosRepository repository;
    private UserRepository userRepository;

    private boolean shareLocation = true;

    @Nullable
    private String guardianName;
    private boolean loading;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), granted -> goToQuestions());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.sosRoot);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        repository = new SosRepository(this);
        if (!repository.isLoggedIn()) {
            Toast.makeText(this, R.string.sos_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository = new UserRepository(this);
        bindGuardian();
        loadShareLocation();

        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnRequest.setOnClickListener(v -> requestHelp());

        loadGuardians(false);
    }

    private void bindGuardian() {
        String name = guardianName != null ? guardianName : getString(R.string.sos_guardian_fallback);
        binding.sosStep1.setText(getString(shareLocation
                ? R.string.sos_step_1 : R.string.sos_step_1_no_location, name));
        binding.sosFootnote.setText(shareLocation
                ? R.string.sos_footnote : R.string.sos_footnote_no_location);
    }

    private void loadShareLocation() {
        userRepository.getProfile(result -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            ProfileResponse profile = result.getData();
            if (result.isSuccess() && profile != null) {
                shareLocation = profile.isShareLocation();
                bindGuardian();
            }
        });
    }

    private void requestHelp() {
        if (guardianName != null) {
            askLocation();
        } else {
            // 처음 불러오기가 실패했으면 한 번 더 시도하고, 그래도 안 되면 이유를 알려준다
            loadGuardians(true);
        }
    }

    /** @param proceed 불러온 뒤 바로 다음 단계로 넘어갈지 */
    private void loadGuardians(boolean proceed) {
        if (loading) {
            return;
        }
        loading = true;
        binding.btnRequest.setEnabled(false);

        repository.getGuardians(result -> {
            loading = false;
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.btnRequest.setEnabled(true);

            List<GuardianResponse> guardians = result.getData();
            if (result.isSuccess() && guardians != null && !guardians.isEmpty()) {
                guardianName = guardians.get(0).getGuardianName();
                bindGuardian();
                if (proceed) {
                    askLocation();
                }
                return;
            }

            if ("SESSION_EXPIRED".equals(result.getCode())) {
                SessionNavigator.toLogin(this, R.string.error_session_expired);
                return;
            }
            if (proceed) {
                showUnavailable(result.getMessageOr(getString(R.string.sos_network_error)));
            }
        });
    }

    private void askLocation() {
        if (!shareLocation || LocationReader.hasPermission(this)) {
            goToQuestions();
            return;
        }
        locationPermission.launch(LocationReader.PERMISSIONS);
    }

    private void goToQuestions() {
        Intent intent = new Intent(this, SosQuestionsActivity.class);
        intent.putExtra(EXTRA_GUARDIAN_NAME, guardianName);
        intent.putExtra(EXTRA_SHARE_LOCATION, shareLocation);
        startActivity(intent);
    }

    /** 보호자에게 보낼 수 없을 때. 119 는 언제든 걸 수 있게 둔다. */
    private void showUnavailable(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sos_unavailable_title)
                .setMessage(message)
                .setPositiveButton(R.string.sos_call119,
                        (dialog, which) -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:119"))))
                .setNegativeButton(R.string.sos_unavailable_close, null)
                .show();
    }
}
