package com.folioreader.android.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Seven bars of reading minutes, oldest to today, with weekday labels. */
public class WeekBarsView extends View {

    private long[] values = new long[7];
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public WeekBarsView(Context c) {
        this(c, null);
    }

    public WeekBarsView(Context c, AttributeSet a) {
        super(c, a);
        float d = getResources().getDisplayMetrics().density;
        text.setColor(getResources().getColor(R.color.brand_textMuted, c.getTheme()));
        text.setTextSize(11 * d);
        text.setTextAlign(Paint.Align.CENTER);
        value.setColor(getResources().getColor(R.color.brand_text, c.getTheme()));
        value.setTextSize(11 * d);
        value.setTextAlign(Paint.Align.CENTER);
        try {
            text.setTypeface(ResourcesCompat.getFont(c, R.font.jakarta_medium));
            value.setTypeface(ResourcesCompat.getFont(c, R.font.jakarta_semibold));
        } catch (RuntimeException ignored) {
        }
    }

    public void setValues(long[] ms) {
        values = ms;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.length;
        float d = getResources().getDisplayMetrics().density;
        float left = getPaddingLeft(), right = getWidth() - getPaddingRight();
        float top = getPaddingTop() + 14 * d, bottom = getHeight() - getPaddingBottom() - 18 * d;
        long max = 1;
        for (long v : values) {
            max = Math.max(max, v);
        }
        float slot = (right - left) / n;
        float w = Math.min(slot * 0.5f, 28 * d);
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat("EEE", Locale.getDefault());
        for (int i = 0; i < n; i++) {
            float cx = left + slot * i + slot / 2f;
            float h = values[i] <= 0 ? 3 * d : Math.max(3 * d, (bottom - top) * values[i] / (float) max);
            bar.setColor(getResources().getColor(
                    values[i] <= 0 ? R.color.brand_accentContainer : R.color.brand_accent, getContext().getTheme()));
            rect.set(cx - w / 2, bottom - h, cx + w / 2, bottom);
            canvas.drawRoundRect(rect, 4 * d, 4 * d, bar);
            if (values[i] > 0) {
                canvas.drawText(String.valueOf(Math.max(1, values[i] / 60000L)), cx, bottom - h - 4 * d, value);
            }
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_YEAR, -(n - 1 - i));
            canvas.drawText(fmt.format(cal.getTime()), cx, getHeight() - getPaddingBottom() - 2 * d, text);
        }
    }
}
