package com.example.deundeun.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ViewAuthHeaderBinding;

/** 인증 화면 공통 머리말. 뒤로가기 · 제목 · (선택) 안내문. */
public class AuthHeaderView extends LinearLayout {

    private final ViewAuthHeaderBinding binding;

    public AuthHeaderView(Context context) {
        this(context, null);
    }

    public AuthHeaderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AuthHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);

        binding = ViewAuthHeaderBinding.inflate(LayoutInflater.from(context), this);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AuthHeaderView);
            CharSequence title = a.getText(R.styleable.AuthHeaderView_authTitle);
            CharSequence desc = a.getText(R.styleable.AuthHeaderView_authDesc);
            a.recycle();

            if (title != null) {
                binding.authTitle.setText(title);
            }
            if (!TextUtils.isEmpty(desc)) {
                binding.authDesc.setText(desc);
                binding.authDesc.setVisibility(VISIBLE);
            }
        }
    }

    public void setOnBackClickListener(OnClickListener listener) {
        binding.btnBack.setOnClickListener(listener);
    }

    public View getTitleView() {
        return binding.authTitle;
    }
}
