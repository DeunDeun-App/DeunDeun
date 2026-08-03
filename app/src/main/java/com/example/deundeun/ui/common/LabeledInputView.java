package com.example.deundeun.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ViewLabeledInputBinding;

/**
 * 인증 화면 전반에서 쓰는 입력 한 벌 : 라벨(+필수 표시) · 입력칸 · 상태 메시지.
 *
 * <p>테두리 색과 메시지 색이 함께 움직이므로 상태를 이 뷰가 직접 들고 있는다.
 * 화면 쪽에서는 {@link #showError}/{@link #showSuccess}/{@link #clearState} 만 부른다.
 */
public class LabeledInputView extends LinearLayout {

    /** 시안의 필드 상태. 테두리와 메시지 색이 여기서 갈린다. */
    public enum State {NORMAL, ERROR, SUCCESS}

    private final ViewLabeledInputBinding binding;

    private CharSequence labelText = "";
    private boolean required;
    private boolean passwordVisible;
    private State state = State.NORMAL;

    public LabeledInputView(Context context) {
        this(context, null);
    }

    public LabeledInputView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LabeledInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);

        binding = ViewLabeledInputBinding.inflate(LayoutInflater.from(context), this);

        boolean password = false;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LabeledInputView);
            labelText = a.getText(R.styleable.LabeledInputView_labelText);
            required = a.getBoolean(R.styleable.LabeledInputView_required, false);
            password = a.getBoolean(R.styleable.LabeledInputView_password, false);
            CharSequence hint = a.getText(R.styleable.LabeledInputView_android_hint);
            int inputType = a.getInt(R.styleable.LabeledInputView_android_inputType,
                    InputType.TYPE_CLASS_TEXT);
            a.recycle();

            if (hint != null) {
                binding.inputEdit.setHint(hint);
            }
            binding.inputEdit.setInputType(inputType);
        }

        applyLabel();
        setPasswordField(password);

        // 사용자가 다시 입력하기 시작하면 이전 오류 표시는 의미가 없다.
        binding.inputEdit.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (state != State.NORMAL) {
                    clearState();
                }
            }
        });
    }

    private void applyLabel() {
        CharSequence label = labelText == null ? "" : labelText;
        if (!required) {
            binding.inputLabel.setText(label);
            return;
        }
        // 필수 표시 별표만 빨간색. 라벨 전체를 빨갛게 하지 않는다.
        SpannableStringBuilder builder = new SpannableStringBuilder(label).append(" *");
        builder.setSpan(new ForegroundColorSpan(Color.RED),
                builder.length() - 1, builder.length(), 0);
        binding.inputLabel.setText(builder);
        binding.inputLabel.setContentDescription(label + ", 필수 입력");
    }

    private void setPasswordField(boolean password) {
        binding.inputToggle.setVisibility(password ? VISIBLE : GONE);
        if (!password) {
            return;
        }
        passwordVisible = false;
        applyPasswordTransformation();
        binding.inputToggle.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            applyPasswordTransformation();
        });
    }

    private void applyPasswordTransformation() {
        binding.inputEdit.setTransformationMethod(
                passwordVisible ? null : new PasswordTransformationMethod());
        binding.inputToggle.setImageResource(
                passwordVisible ? R.drawable.ic_eye : R.drawable.ic_eye_off);
        binding.inputToggle.setContentDescription(getContext().getString(
                passwordVisible ? R.string.cd_hide_password : R.string.cd_show_password));
        // 표시 방식을 바꾸면 커서가 앞으로 튀므로 끝으로 되돌린다.
        binding.inputEdit.setSelection(binding.inputEdit.getText().length());
    }

    public String getText() {
        return binding.inputEdit.getText().toString().trim();
    }

    public void setText(String text) {
        binding.inputEdit.setText(text);
    }

    public void showError(String message) {
        applyState(State.ERROR, message, R.drawable.bg_field_error, R.color.error_red);
    }

    public void showSuccess(String message) {
        applyState(State.SUCCESS, message, R.drawable.bg_field_success, R.color.green_success);
    }

    public void clearState() {
        state = State.NORMAL;
        binding.inputBox.setBackgroundResource(R.drawable.bg_field);
        binding.inputMessage.setVisibility(GONE);
        setContentDescription(null);
    }

    private void applyState(State next, String message, int backgroundRes, int colorRes) {
        state = next;
        binding.inputBox.setBackgroundResource(backgroundRes);
        binding.inputMessage.setText(message);
        binding.inputMessage.setTextColor(ContextCompat.getColor(getContext(), colorRes));
        binding.inputMessage.setVisibility(message == null || message.isEmpty() ? GONE : VISIBLE);

        // 색만으로 상태를 알리면 저시력·색약 사용자가 놓친다. 스크린리더에도 상태를 실어준다.
        setContentDescription(labelText + ", " + message);
        binding.inputMessage.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public State getState() {
        return state;
    }

    public boolean isEmpty() {
        return getText().isEmpty();
    }

    /** 입력 변화만 필요할 때 쓰는 빈 구현. */
    public abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    public void addTextWatcher(TextWatcher watcher) {
        binding.inputEdit.addTextChangedListener(watcher);
    }

    public View getEditView() {
        return binding.inputEdit;
    }
}
