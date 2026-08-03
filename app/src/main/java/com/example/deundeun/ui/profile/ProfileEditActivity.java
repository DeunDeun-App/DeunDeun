package com.example.deundeun.ui.profile;

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
import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;
import com.example.deundeun.data.FakeTrainingRepository;
import com.example.deundeun.data.model.UserProfile;
import com.example.deundeun.databinding.ActivityProfileEditBinding;
import com.example.deundeun.databinding.ViewEditFieldBinding;
import com.example.deundeun.ui.common.InsetsUtil;

import java.util.Arrays;
import java.util.List;

/** Figma 127:76 — 내 정보 수정. */
public class ProfileEditActivity extends AppCompatActivity {

    private ActivityProfileEditBinding binding;
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

    private List<TextView> relationChips;

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

    /**
     * "저장" 은 시안의 68dp 간격을 맞추려고 글자 폭에 딱 맞춰 놓았다.
     * 그대로 두면 누를 곳이 너무 좁으므로 배치는 건드리지 않고 터치 영역만 넓힌다.
     */
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
        // 기본 정보 카드의 마지막 줄은 카드 경계와 겹치므로 구분선을 숨긴다.
        binding.fieldBirth.fieldDivider.setVisibility(View.GONE);

        // 장애 정보는 선택형이라 직접 입력을 막고 셰브런을 보인다.
        bindField(binding.fieldDisabilityType, R.string.field_disability_type,
                profile.getDisabilityType(), InputType.TYPE_NULL, true);
        bindField(binding.fieldDisabilityLevel, R.string.field_disability_level,
                profile.getDisabilityLevel(), InputType.TYPE_NULL, true);
        binding.fieldDisabilityLevel.fieldDivider.setVisibility(View.GONE);

        bindPicker(binding.fieldDisabilityType, R.array.disability_types);
        bindPicker(binding.fieldDisabilityLevel, R.array.disability_levels);

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
        }
    }

    /**
     * 선택형 필드에 드롭다운을 붙인다.
     *
     * <p>셰브런이 아래를 가리키므로 목록도 필드 바로 아래로 펼쳐져야 한다.
     * 가운데 뜨는 다이얼로그는 어느 필드를 고르는 중인지 알기 어렵다.
     */
    private void bindPicker(ViewEditFieldBinding field, @ArrayRes int optionsRes) {
        String[] options = getResources().getStringArray(optionsRes);

        View.OnClickListener open = v -> showDropdown(field, options);

        // 값과 셰브런 어디를 눌러도 열리게 한다.
        field.fieldValue.setOnClickListener(open);
        field.fieldChevron.setOnClickListener(open);
        field.getRoot().setOnClickListener(open);
    }

    private void showDropdown(ViewEditFieldBinding field, String[] options) {
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
                // 지금 값이 어느 것인지 색으로 알려준다.
                view.setTextColor(ContextCompat.getColor(ProfileEditActivity.this,
                        selected ? R.color.orange_primary : R.color.text_primary));
                return view;
            }
        });

        popup.setOnItemClickListener((parent, view, position, id) -> {
            field.fieldValue.setText(options[position]);
            popup.dismiss();
        });

        // 열려 있는 동안 셰브런을 위로 뒤집어 상태를 보여준다.
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
