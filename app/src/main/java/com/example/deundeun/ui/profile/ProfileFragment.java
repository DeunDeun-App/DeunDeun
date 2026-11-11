package com.example.deundeun.ui.profile;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.deundeun.R;
import com.example.deundeun.data.model.GuardianNotice;
import com.example.deundeun.data.network.dto.user.NotificationResponse;
import com.example.deundeun.data.network.dto.user.ProfileResponse;
import com.example.deundeun.data.network.dto.user.SettingsRequest;
import com.example.deundeun.data.repository.BaseRepository;
import com.example.deundeun.data.repository.UserRepository;
import com.example.deundeun.databinding.DialogConfirmBinding;
import com.example.deundeun.databinding.FragmentProfileBinding;
import com.example.deundeun.databinding.ViewProfileInfoRowBinding;
import com.example.deundeun.databinding.ViewProfileSwitchRowBinding;
import com.example.deundeun.ui.common.DialogHelper;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.SessionNavigator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 내정보. */
public class ProfileFragment extends Fragment {

    public static final String TAG = "profile";

    private FragmentProfileBinding binding;
    private UserRepository repository;
    private GuardianNoticeAdapter noticeAdapter;

    private boolean bindingSwitches;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.applyBottomInset(binding.profileScroll);

        repository = new UserRepository(requireContext());

        setUpSwitchRow(binding.shareLocation,
                R.string.profile_share_location, R.string.profile_share_location_desc);
        setUpSwitchRow(binding.shareResult,
                R.string.profile_share_result, R.string.profile_share_result_desc);
        setUpSwitchRow(binding.shareSos,
                R.string.profile_share_sos, R.string.profile_share_sos_desc);

        noticeAdapter = new GuardianNoticeAdapter();
        binding.noticeList.setAdapter(noticeAdapter);

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileEditActivity.class)));
        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnWithdraw.setOnClickListener(v -> confirmWithdraw());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        loadNotices();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null) {
            loadProfile();
            loadNotices();
        }
    }

    private void loadProfile() {
        repository.getProfile(result -> {
            if (binding == null) {
                return;
            }
            if (result.isSuccess() && result.getData() != null) {
                bindProfile(result.getData());
            } else {
                handleFailure(result);
            }
        });
    }

    private void bindProfile(ProfileResponse profile) {
        String empty = getString(R.string.profile_empty);

        binding.profileName.setText(profile.getName());
        binding.profilePhone.setText(isBlank(profile.getPhone()) ? empty : profile.getPhone());

        String birth = ProfileLabels.birthDate(requireContext(), profile.getBirthDate());
        bindInfoRow(binding.rowBirth, R.string.profile_birth, birth.isEmpty() ? empty : birth);

        String type = ProfileLabels.disabilityType(requireContext(), profile.getDisabilityType());
        String level = ProfileLabels.severity(requireContext(), profile.getSeverity());
        String disability = ((type == null ? "" : type) + " " + (level == null ? "" : level)).trim();
        bindInfoRow(binding.rowDisability, R.string.profile_disability,
                disability.isEmpty() ? empty : disability);

        ProfileResponse.Guardian guardian = profile.getGuardian();
        String guardianText = guardian == null ? empty
                : getString(R.string.profile_guardian_format,
                        ProfileLabels.relation(requireContext(), guardian.getRelation()),
                        guardian.getPhone());
        bindInfoRow(binding.rowGuardian, R.string.profile_guardian, guardianText);

        bindingSwitches = true;
        binding.shareLocation.switchToggle.setChecked(profile.isShareLocation());
        binding.shareResult.switchToggle.setChecked(profile.isShareResult());
        binding.shareSos.switchToggle.setChecked(profile.isSosAlert());
        bindingSwitches = false;
    }

    private void bindInfoRow(ViewProfileInfoRowBinding row, @StringRes int labelRes, String value) {
        row.infoLabel.setText(labelRes);
        row.infoValue.setText(value);
        row.getRoot().setContentDescription(getString(labelRes) + ", " + value);
    }

    private void setUpSwitchRow(ViewProfileSwitchRowBinding row,
                                @StringRes int titleRes, @StringRes int descRes) {
        row.switchTitle.setText(titleRes);
        row.switchDesc.setText(descRes);
        row.switchToggle.setContentDescription(getString(titleRes));
        row.getRoot().setOnClickListener(v -> row.switchToggle.toggle());
        row.switchToggle.setOnCheckedChangeListener((button, checked) -> {
            if (!bindingSwitches) {
                saveSettings(row, checked);
            }
        });
    }

    private void saveSettings(ViewProfileSwitchRowBinding changed, boolean checked) {
        SettingsRequest request = new SettingsRequest(
                binding.shareLocation.switchToggle.isChecked(),
                binding.shareResult.switchToggle.isChecked(),
                binding.shareSos.switchToggle.isChecked());

        repository.updateSettings(request, result -> {
            if (binding == null) {
                return;
            }
            if (result.isSuccess() && result.getData() != null) {
                bindProfile(result.getData());
                return;
            }
            bindingSwitches = true;
            changed.switchToggle.setChecked(!checked);
            bindingSwitches = false;
            handleFailure(result);
        });
    }

    private void loadNotices() {
        repository.getNotifications(result -> {
            if (binding == null) {
                return;
            }
            if (!result.isSuccess()) {
                return;
            }

            List<GuardianNotice> notices = new ArrayList<>();
            if (result.getData() != null) {
                for (NotificationResponse item : result.getData()) {
                    notices.add(toNotice(item));
                }
            }

            noticeAdapter.submit(notices);
            binding.noticeCount.setText(getString(R.string.profile_notice_count, notices.size()));
            binding.noticeEmpty.setVisibility(notices.isEmpty() ? View.VISIBLE : View.GONE);
            binding.noticeList.setVisibility(notices.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private GuardianNotice toNotice(NotificationResponse item) {
        String date = "";
        String time = "";
        String createdAt = item.getCreatedAt();
        if (createdAt != null && createdAt.length() >= 19) {
            try {
                Date parsed = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
                        .parse(createdAt.substring(0, 19));
                if (parsed != null) {
                    date = new SimpleDateFormat("M월 d일", Locale.KOREA).format(parsed);
                    time = new SimpleDateFormat("a h:mm", Locale.KOREA).format(parsed);
                }
            } catch (ParseException ignored) {
            }
        }
        return new GuardianNotice(item.getTitle(), item.getBody(), date, time);
    }

    private void logout() {
        binding.btnLogout.setEnabled(false);
        repository.logout(result -> {
            if (isAdded()) {
                SessionNavigator.toLogin(requireActivity(), R.string.logout_done);
            }
        });
    }

    private void confirmWithdraw() {
        DialogConfirmBinding dialogBinding = DialogConfirmBinding.inflate(getLayoutInflater());
        dialogBinding.dialogTitle.setText(R.string.withdraw_title);
        dialogBinding.dialogMessage.setText(R.string.withdraw_message);
        dialogBinding.dialogConfirm.setText(R.string.withdraw_confirm);

        Dialog dialog = DialogHelper.show(requireContext(), dialogBinding.getRoot(), 0.84f, true);
        dialogBinding.dialogCancel.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.dialogConfirm.setOnClickListener(v -> {
            dialogBinding.dialogConfirm.setEnabled(false);
            repository.withdraw(result -> {
                dialog.dismiss();
                if (!isAdded()) {
                    return;
                }
                if (result.isSuccess()) {
                    SessionNavigator.toLogin(requireActivity(), R.string.withdraw_done);
                } else {
                    handleFailure(result);
                }
            });
        });
    }

    private void handleFailure(BaseRepository.Result<?> result) {
        if (!isAdded()) {
            return;
        }
        if (result.hasCode("SESSION_EXPIRED")) {
            SessionNavigator.toLogin(requireActivity(), R.string.error_session_expired);
            return;
        }
        Toast.makeText(requireContext(), result.getMessageOr(getString(R.string.error_network)),
                Toast.LENGTH_SHORT).show();
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
