package com.example.deundeun.ui.profile;

import android.app.DatePickerDialog;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.user.GuardianResponse;
import com.example.deundeun.data.network.dto.user.ProfileResponse;
import com.example.deundeun.data.network.dto.user.ProfileUpdateRequest;
import com.example.deundeun.data.repository.BaseRepository;
import com.example.deundeun.data.repository.UserRepository;
import com.example.deundeun.databinding.ActivityProfileEditBinding;
import com.example.deundeun.databinding.ViewEditFieldBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.SessionNavigator;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** 내 정보 수정. */
public class ProfileEditActivity extends AppCompatActivity {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789]\\d{7,8}$");

    private ActivityProfileEditBinding binding;
    private UserRepository repository;

    private List<TextView> relationChips;

    // 지금 고른 장애 유형의 순번
    private int typeIndex;

    private String birthDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.topBar);
        InsetsUtil.applyBottomInset(binding.profileEditScroll);

        repository = new UserRepository(this);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> save());
        expandSaveTouchArea();

        relationChips = Arrays.asList(
                binding.relationMother, binding.relationFather, binding.relationOther);
        for (TextView chip : relationChips) {
            chip.setOnClickListener(v -> selectRelation((TextView) v));
        }
        binding.relationMother.setTag(ProfileLabels.RELATION_MOTHER);
        binding.relationFather.setTag(ProfileLabels.RELATION_FATHER);
        binding.relationOther.setTag(ProfileLabels.RELATION_ETC);

        binding.btnSave.setEnabled(false);
        loadProfile();
    }

    private void loadProfile() {
        repository.getProfile(result -> {
            if (result.isSuccess() && result.getData() != null) {
                bindFields(result.getData());
                binding.btnSave.setEnabled(true);
            } else {
                handleFailure(result);
            }
        });
    }

    private void expandSaveTouchArea() {
        View parent = (View) binding.btnSave.getParent();
        parent.post(() -> {
            Rect bounds = new Rect();
            binding.btnSave.getHitRect(bounds);
            int extra = getResources().getDimensionPixelSize(R.dimen.space_l);
            bounds.left -= extra;
            bounds.right += extra;
            parent.setTouchDelegate(new TouchDelegate(bounds, binding.btnSave));
        });
    }

    private void bindFields(ProfileResponse profile) {
        bindField(binding.fieldName, R.string.field_name, profile.getName(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME, false);
        bindField(binding.fieldPhone, R.string.field_phone, profile.getPhone(),
                InputType.TYPE_CLASS_PHONE, false);

        birthDate = profile.getBirthDate();
        bindField(binding.fieldBirth, R.string.field_birth,
                ProfileLabels.birthDate(this, birthDate), InputType.TYPE_NULL, true);
        binding.fieldBirth.fieldDivider.setVisibility(View.GONE);
        bindBirthPicker();

        String typeLabel = ProfileLabels.disabilityType(this, profile.getDisabilityType());
        String levelLabel = ProfileLabels.severity(this, profile.getSeverity());
        bindField(binding.fieldDisabilityType, R.string.field_disability_type,
                typeLabel, InputType.TYPE_NULL, true);
        bindField(binding.fieldDisabilityLevel, R.string.field_disability_level,
                levelLabel, InputType.TYPE_NULL, true);
        binding.fieldDisabilityLevel.fieldDivider.setVisibility(View.GONE);

        bindDisabilityPickers(typeLabel);

        GuardianResponse guardian = profile.getGuardian();
        bindField(binding.fieldGuardianName, R.string.field_guardian_name,
                guardian == null ? null : guardian.getName(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME, false);
        bindField(binding.fieldGuardianPhone, R.string.field_guardian_phone,
                guardian == null ? null : guardian.getPhone(), InputType.TYPE_CLASS_PHONE, false);
        binding.fieldGuardianPhone.fieldDivider.setVisibility(View.GONE);

        selectRelationByCode(guardian == null ? ProfileLabels.RELATION_MOTHER : guardian.getRelation());
    }

    private void bindBirthPicker() {
        View.OnClickListener open = v -> showBirthPicker();
        binding.fieldBirth.fieldValue.setOnClickListener(open);
        binding.fieldBirth.fieldChevron.setOnClickListener(open);
        binding.fieldBirth.getRoot().setOnClickListener(open);
    }

    private void showBirthPicker() {
        int[] current = ProfileLabels.parseDate(birthDate);
        int year = current == null ? 2000 : current[0];
        int month = current == null ? 0 : current[1] - 1;
        int day = current == null ? 1 : current[2];

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            birthDate = ProfileLabels.isoDate(y, m + 1, d);
            binding.fieldBirth.fieldValue.setText(ProfileLabels.birthDate(this, birthDate));
        }, year, month, day);
        dialog.getDatePicker().setMaxDate(Calendar.getInstance().getTimeInMillis());
        dialog.show();
    }

    private void bindField(ViewEditFieldBinding field, @StringRes int labelRes, String value,
                           int inputType, boolean dropdown) {
        field.fieldLabel.setText(labelRes);
        field.fieldValue.setText(value);
        field.fieldValue.setInputType(inputType);
        field.fieldValue.setContentDescription(getString(labelRes));

        if (dropdown) {
            field.fieldValue.setFocusable(false);
            field.fieldValue.setCursorVisible(false);
            field.fieldChevron.setVisibility(View.VISIBLE);
            field.fieldValue.setHint(R.string.hint_select);
        }
    }

    // 장애 유형·정도 드롭다운
    private void bindDisabilityPickers(@Nullable String typeLabel) {
        String[] types = getResources().getStringArray(R.array.disability_types);
        typeIndex = Math.max(0, indexOf(types, typeLabel));

        bindPicker(binding.fieldDisabilityType, () -> types, position -> {
            typeIndex = position;
            binding.fieldDisabilityType.fieldValue.setText(types[position]);
            binding.fieldDisabilityLevel.fieldValue.setText("");
        });

        bindPicker(binding.fieldDisabilityLevel, this::levelsForCurrentType, position ->
                binding.fieldDisabilityLevel.fieldValue.setText(
                        levelsForCurrentType()[position]));
    }

    // 값이 목록의 몇 번째인지
    private int indexOf(String[] options, String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    // 지금 고른 유형에 맞는 정도 선택지
    private String[] levelsForCurrentType() {
        TypedArray byType = getResources().obtainTypedArray(R.array.disability_levels_by_type);
        int arrayRes = byType.getResourceId(typeIndex, 0);
        byType.recycle();
        return getResources().getStringArray(arrayRes);
    }
    private void bindPicker(ViewEditFieldBinding field, Supplier<String[]> options,
                            IntConsumer onSelected) {
        View.OnClickListener open = v -> showDropdown(field, options.get(), onSelected);

        field.fieldValue.setOnClickListener(open);
        field.fieldChevron.setOnClickListener(open);
        field.getRoot().setOnClickListener(open);
    }

    private void showDropdown(ViewEditFieldBinding field, String[] options,
                              IntConsumer onSelected) {
        String current = field.fieldValue.getText().toString();

        ListPopupWindow popup = new ListPopupWindow(this);
        popup.setAnchorView(field.getRoot());
        popup.setModal(true);
        popup.setBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.bg_dropdown));
        popup.setVerticalOffset(-field.fieldDivider.getHeight());
        popup.setAdapter(new ArrayAdapter<String>(
                this, R.layout.item_dropdown_option, R.id.option_label, options) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                boolean selected = options[position].equals(current);
                view.setTextColor(ContextCompat.getColor(ProfileEditActivity.this,
                        selected ? R.color.orange_primary : R.color.text_primary));
                return view;
            }
        });

        popup.setOnItemClickListener((parent, view, position, id) -> {
            onSelected.accept(position);
            popup.dismiss();
        });

        field.fieldChevron.setRotation(90f);
        popup.setOnDismissListener(() -> field.fieldChevron.setRotation(-90f));

        popup.show();
    }

    private void selectRelationByCode(@Nullable String code) {
        boolean matched = false;
        for (TextView chip : relationChips) {
            boolean selected = chip.getTag().equals(code);
            chip.setSelected(selected);
            matched |= selected;
        }
        if (!matched) {
            binding.relationOther.setSelected(true);
        }
    }

    private void selectRelation(TextView selected) {
        for (TextView chip : relationChips) {
            chip.setSelected(chip == selected);
        }
    }

    private void save() {
        String name = text(binding.fieldName);
        String phone = text(binding.fieldPhone);
        String guardianName = text(binding.fieldGuardianName);
        String guardianPhone = text(binding.fieldGuardianPhone);

        if (name.isEmpty()) {
            showError(binding.fieldName, R.string.error_name_required);
            return;
        }
        if (!phone.isEmpty() && !isPhone(phone)) {
            showError(binding.fieldPhone, R.string.error_phone_format);
            return;
        }
        if (guardianName.isEmpty() && !guardianPhone.isEmpty()) {
            showError(binding.fieldGuardianName, R.string.error_guardian_name_required);
            return;
        }
        if (!guardianName.isEmpty() && guardianPhone.isEmpty()) {
            showError(binding.fieldGuardianPhone, R.string.error_guardian_phone_required);
            return;
        }
        if (!guardianPhone.isEmpty() && !isPhone(guardianPhone)) {
            showError(binding.fieldGuardianPhone, R.string.error_guardian_phone_format);
            return;
        }

        ProfileUpdateRequest request = new ProfileUpdateRequest(
                name,
                phone,
                birthDate,
                ProfileLabels.disabilityTypeCode(this, text(binding.fieldDisabilityType)),
                ProfileLabels.severityCode(this, text(binding.fieldDisabilityLevel)),
                selectedRelationCode(),
                guardianName,
                guardianPhone);

        binding.btnSave.setEnabled(false);
        repository.updateProfile(request, result -> {
            binding.btnSave.setEnabled(true);
            if (result.isSuccess()) {
                Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                handleFailure(result);
            }
        });
    }

    private String selectedRelationCode() {
        for (TextView chip : relationChips) {
            if (chip.isSelected()) {
                return (String) chip.getTag();
            }
        }
        return ProfileLabels.RELATION_ETC;
    }

    private String text(ViewEditFieldBinding field) {
        return field.fieldValue.getText().toString().trim();
    }

    private boolean isPhone(String value) {
        return PHONE_PATTERN.matcher(value.replaceAll("[\\s-]", "")).matches();
    }

    private void showError(ViewEditFieldBinding field, @StringRes int messageRes) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
        field.fieldValue.requestFocus();
    }

    private void handleFailure(BaseRepository.Result<?> result) {
        if (result.hasCode("SESSION_EXPIRED")) {
            SessionNavigator.toLogin(this, R.string.error_session_expired);
            return;
        }
        Toast.makeText(this, result.getMessageOr(getString(R.string.error_network)),
                Toast.LENGTH_SHORT).show();
    }
}
