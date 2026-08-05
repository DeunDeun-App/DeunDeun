package com.example.deundeun.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.deundeun.R;
import com.example.deundeun.data.FakeTrainingRepository;
import com.example.deundeun.data.model.GuardianNotice;
import com.example.deundeun.data.model.UserProfile;
import com.example.deundeun.databinding.FragmentProfileBinding;
import com.example.deundeun.databinding.ViewProfileInfoRowBinding;
import com.example.deundeun.databinding.ViewProfileSwitchRowBinding;

import java.util.List;

/** 내정보. */
public class ProfileFragment extends Fragment {

    public static final String TAG = "profile";

    private FragmentProfileBinding binding;
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

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
        com.example.deundeun.ui.common.InsetsUtil.applyBottomInset(binding.profileScroll);

        bindProfile();
        bindShareSettings();
        bindNotices();

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileEditActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        bindProfile();
    }

    private void bindProfile() {
        UserProfile profile = repository.getUserProfile();

        binding.profileName.setText(profile.getName());
        binding.profilePhone.setText(profile.getPhone());

        bindInfoRow(binding.rowBirth, R.string.profile_birth, profile.getBirthDate());
        bindInfoRow(binding.rowDisability, R.string.profile_disability,
                profile.getDisabilitySummary());
        bindInfoRow(binding.rowGuardian, R.string.profile_guardian,
                profile.getGuardianSummary());
    }

    private void bindInfoRow(ViewProfileInfoRowBinding row, @StringRes int labelRes, String value) {
        row.infoLabel.setText(labelRes);
        row.infoValue.setText(value);
        row.getRoot().setContentDescription(getString(labelRes) + ", " + value);
    }

    private void bindShareSettings() {
        bindSwitchRow(binding.shareLocation,
                R.string.profile_share_location, R.string.profile_share_location_desc);
        bindSwitchRow(binding.shareResult,
                R.string.profile_share_result, R.string.profile_share_result_desc);
        bindSwitchRow(binding.shareSos,
                R.string.profile_share_sos, R.string.profile_share_sos_desc);
    }

    private void bindSwitchRow(ViewProfileSwitchRowBinding row,
                               @StringRes int titleRes, @StringRes int descRes) {
        row.switchTitle.setText(titleRes);
        row.switchDesc.setText(descRes);
        // 시안은 셋 다 켜진 상태. TODO: 서버 설정값으로 대체
        row.switchToggle.setChecked(true);
        row.switchToggle.setContentDescription(getString(titleRes));
        row.getRoot().setOnClickListener(v -> row.switchToggle.toggle());
    }

    private void bindNotices() {
        List<GuardianNotice> notices = repository.getGuardianNotices();

        binding.noticeCount.setText(getString(R.string.profile_notice_count, notices.size()));

        GuardianNoticeAdapter adapter = new GuardianNoticeAdapter();
        binding.noticeList.setAdapter(adapter);
        adapter.submit(notices);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
