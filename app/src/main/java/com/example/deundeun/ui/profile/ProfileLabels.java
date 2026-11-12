package com.example.deundeun.ui.profile;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.deundeun.R;

import java.util.Locale;

public final class ProfileLabels {

    public static final String RELATION_MOTHER = "MOTHER";
    public static final String RELATION_FATHER = "FATHER";
    public static final String RELATION_ETC = "ETC";

    private ProfileLabels() {
    }

    public static String disabilityType(Context context, @Nullable String code) {
        return mapCode(context, code, R.array.disability_type_codes, R.array.disability_types);
    }

    @Nullable
    public static String disabilityTypeCode(Context context, @Nullable String label) {
        return mapCode(context, label, R.array.disability_types, R.array.disability_type_codes);
    }

    public static String severity(Context context, @Nullable String code) {
        return mapCode(context, code, R.array.severity_codes, R.array.severity_labels);
    }

    @Nullable
    public static String severityCode(Context context, @Nullable String label) {
        return mapCode(context, label, R.array.severity_labels, R.array.severity_codes);
    }

    public static String relation(Context context, @Nullable String code) {
        if (RELATION_MOTHER.equals(code)) {
            return context.getString(R.string.relation_mother);
        }
        if (RELATION_FATHER.equals(code)) {
            return context.getString(R.string.relation_father);
        }
        return context.getString(R.string.relation_other);
    }

    public static String birthDate(Context context, @Nullable String isoDate) {
        int[] date = parseDate(isoDate);
        return date == null ? ""
                : context.getString(R.string.profile_birth_format, date[0], date[1], date[2]);
    }

    @Nullable
    public static int[] parseDate(@Nullable String isoDate) {
        if (isoDate == null) {
            return null;
        }
        String[] parts = isoDate.split("-");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String isoDate(int year, int month, int day) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day);
    }

    private static String mapCode(Context context, @Nullable String value, int fromRes, int toRes) {
        if (value == null) {
            return null;
        }
        String[] from = context.getResources().getStringArray(fromRes);
        String[] to = context.getResources().getStringArray(toRes);
        for (int i = 0; i < from.length && i < to.length; i++) {
            if (from[i].equals(value)) {
                return to[i];
            }
        }
        return null;
    }
}
