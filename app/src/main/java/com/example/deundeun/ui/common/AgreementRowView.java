package com.example.deundeun.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Checkable;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ViewAgreementRowBinding;

/**
 * 약관 동의 한 줄. 체크박스가 아니라 체크 아이콘의 색으로 동의 여부를 나타내는 시안이라
 * {@link Checkable} 을 직접 구현해 상태를 자식 아이콘에 전파한다.
 */
public class AgreementRowView extends LinearLayout implements Checkable {

    private static final int[] CHECKED_STATE = {android.R.attr.state_checked};

    private boolean checked;
    private boolean requiredAgreement;

    private final ViewAgreementRowBinding binding;

    @Nullable
    private OnCheckedChangeListener listener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(AgreementRowView view, boolean checked);
    }

    public AgreementRowView(Context context) {
        this(context, null);
    }

    public AgreementRowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AgreementRowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setClickable(true);
        setFocusable(true);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.min_touch_target));

        binding = ViewAgreementRowBinding.inflate(LayoutInflater.from(context), this);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AgreementRowView);
            CharSequence label = a.getText(R.styleable.AgreementRowView_agreeLabel);
            requiredAgreement = a.getBoolean(R.styleable.AgreementRowView_requiredAgreement, false);
            a.recycle();
            if (label != null) {
                binding.agreeLabel.setText(label);
                setContentDescription(label);
            }
        }

        setOnClickListener(v -> toggle());
    }

    public boolean isRequiredAgreement() {
        return requiredAgreement;
    }

    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.checked == checked) {
            return;
        }
        this.checked = checked;
        refreshDrawableState();
        if (listener != null) {
            listener.onCheckedChanged(this, checked);
        }
    }

    /** 리스너를 타지 않고 상태만 맞춘다. "모두 동의" 와 개별 항목이 서로를 무한히 부르는 것을 막는다. */
    public void setCheckedSilently(boolean checked) {
        OnCheckedChangeListener saved = listener;
        listener = null;
        setChecked(checked);
        listener = saved;
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        if (checked) {
            mergeDrawableStates(state, CHECKED_STATE);
        }
        return state;
    }
}
