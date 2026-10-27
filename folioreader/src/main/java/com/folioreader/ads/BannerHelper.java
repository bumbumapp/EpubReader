package com.folioreader.ads;

import android.app.Activity;
import com.folioreader.R;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

/** Anchored adaptive banner loaded into a container (only after consent allows ad requests). */
public final class BannerHelper {

    private BannerHelper() {
    }

    public static AdView load(Activity activity, ViewGroup container) {
        if (container == null || !AdsConsent.canRequestAds(activity)) {
            return null;
        }
        container.removeAllViews();
        AdView adView = new AdView(activity);
        adView.setAdUnitId(activity.getString(R.string.admob_banner_id));
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthPx = container.getWidth() > 0 ? container.getWidth() : metrics.widthPixels;
        int widthDp = (int) (widthPx / metrics.density);
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp));
        container.addView(adView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER_HORIZONTAL));
        adView.loadAd(new AdRequest.Builder().build());
        return adView;
    }
}
