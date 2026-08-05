package com.example.deundeun.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ViewNavItemBinding;

/**
 * 하단탭 항목
 *
 * 아이콘 tint 와 라벨 색은 모두 {@code @color/nav_item} 셀렉터를 쓰므로
 * 이 뷰의 selected 만 바꾸면 둘 다 함께 바뀐다.
 */
public class NavItemView extends LinearLayout {

    private final ViewNavItemBinding binding;

    public NavItemView(Context context) {
        this(context, null);
    }

    public NavItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        setClickable(true);
        setFocusable(true);

        binding = ViewNavItemBinding.inflate(LayoutInflater.from(context), this);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NavItemView);
            int iconRes = a.getResourceId(R.styleable.NavItemView_navIcon, 0);
            CharSequence label = a.getText(R.styleable.NavItemView_navLabel);
            a.recycle();

            if (iconRes != 0) {
                binding.navIcon.setImageResource(iconRes);
            }
            if (label != null) {
                setLabel(label);
            }
        }
    }

    public void setIcon(@DrawableRes int iconRes) {
        binding.navIcon.setImageResource(iconRes);
    }

    public void setLabel(@StringRes int labelRes) {
        setLabel(getContext().getText(labelRes));
    }

    private void setLabel(CharSequence label) {
        binding.navLabel.setText(label);
        setContentDescription(label);
    }
}
