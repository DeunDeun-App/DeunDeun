package com.example.deundeun.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ViewFeatureHeaderBinding;

/**
 * 훈련 기능 화면 공통 머리말
 */
public class FeatureHeaderView extends ConstraintLayout {

    private final ViewFeatureHeaderBinding binding;

    public FeatureHeaderView(Context context) {
        this(context, null);
    }

    public FeatureHeaderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FeatureHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        binding = ViewFeatureHeaderBinding.inflate(LayoutInflater.from(context), this);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FeatureHeaderView);
            CharSequence eyebrow = a.getText(R.styleable.FeatureHeaderView_featureEyebrow);
            CharSequence title = a.getText(R.styleable.FeatureHeaderView_featureTitle);
            a.recycle();

            if (eyebrow != null) {
                binding.featureEyebrow.setText(eyebrow);
            }
            if (title != null) {
                binding.featureTitle.setText(title);
            }
        }
    }

    public void setOnBackClickListener(OnClickListener listener) {
        binding.btnBack.setOnClickListener(listener);
    }
}
