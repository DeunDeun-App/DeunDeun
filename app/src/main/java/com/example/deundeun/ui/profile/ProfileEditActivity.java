package com.example.deundeun.ui.profile;

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
import com.example.deundeun.data.repository.FakeTrainingRepository;
import com.example.deundeun.data.model.UserProfile;
import com.example.deundeun.databinding.ActivityProfileEditBinding;
import com.example.deundeun.databinding.ViewEditFieldBinding;
import com.example.deundeun.ui.common.InsetsUtil;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** 내 정보 수정. */
public class ProfileEditActivity extends AppCompatActivity {

    private ActivityProfileEditBinding binding;
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

    private List<TextView> relationChips;

    // 지금 고른 장애 유형의 순번
    private int typeIndex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.topBar);
        InsetsUtil.applyBottomInset(binding.profileEditScroll);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> save());
        expandSaveTouchArea();

        relationChips = Arrays.asList(
                binding.relationMother, binding.relationFather, binding.relationOther);
        for (TextView chip : relationChips) {
            chip.setOnClickListener(v -> selectRelation((TextView) v));
        }

        bindFields();
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

    private void bindFields() {
        UserProfile profile = repository.getUserProfile();

        bindField(binding.fieldName, R.string.field_name, profile.getName(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME, false);
        bindField(binding.fieldPhone, R.string.field_phone, profile.getPhone(),
                InputType.TYPE_CLASS_PHONE, false);
        bindField(binding.fieldBirth, R.string.field_birth, profile.getBirthDate(),
                InputType.TYPE_CLASS_TEXT, false);
        binding.fieldBirth.fieldDivider.setVisibility(View.GONE);

        bindField(binding.fieldDisabilityType, R.string.field_disability_type,
                profile.getDisabilityType(), InputType.TYPE_NULL, true);
        bindField(binding.fieldDisabilityLevel, R.string.field_disability_level,
                profile.getDisabilityLevel(), InputType.TYPE_NULL, true);
        binding.fieldDisabilityLevel.fieldDivider.setVisibility(View.GONE);

        bindDisabilityPickers(profile);

        bindField(binding.fieldGuardianName, R.string.field_guardian_name,
                profile.getGuardianName(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME, false);
        bindField(binding.fieldGuardianPhone, R.string.field_guardian_phone,
                profile.getGuardianPhone(), InputType.TYPE_CLASS_PHONE, false);
        binding.fieldGuardianPhone.fieldDivider.setVisibility(View.GONE);

        selectRelationByName(profile.getGuardianRelation());
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
    private void bindDisabilityPickers(UserProfile profile) {
        String[] types = getResources().getStringArray(R.array.disability_types);
        typeIndex = Math.max(0, indexOf(types, profile.getDisabilityType()));

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

    private void selectRelationByName(String relation) {
        for (TextView chip : relationChips) {
            chip.setSelected(chip.getText().toString().equals(relation));
        }
    }

    private void selectRelation(TextView selected) {
        for (TextView chip : relationChips) {
            chip.setSelected(chip == selected);
        }
    }

    private void save() {
        // TODO: 서버 연동 시 PATCH /v1/users/me 로 보낸다
        Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
