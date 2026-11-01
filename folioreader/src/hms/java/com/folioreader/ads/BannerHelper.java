package com.folioreader.ads;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.folioreader.R;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.banner.BannerView;

/** Huawei Ads banner loaded into a container (only after Huawei Ads is initialized). */
public final class BannerHelper {

    private BannerHelper() {
    }

    public static BannerView load(Activity activity, ViewGroup container) {
        if (container == null || !AdsConsent.canRequestAds(activity)) {
            return null;
        }
        container.removeAllViews();
        BannerView banner = new BannerView(activity);
        banner.setAdId(AdsConsent.slot(activity, R.string.hms_banner_id, R.string.hms_test_banner_id));
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthPx = container.getWidth() > 0 ? container.getWidth() : metrics.widthPixels;
        int widthDp = (int) (widthPx / metrics.density);
        banner.setBannerAdSize(BannerAdSize.getCurrentDirectionBannerSize(activity, widthDp));
        container.addView(banner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER_HORIZONTAL));
        banner.loadAd(new AdParam.Builder().build());
        return banner;
    }
}
