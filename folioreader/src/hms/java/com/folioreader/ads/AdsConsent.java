package com.folioreader.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.folioreader.BuildConfig;

import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.NonPersonalizedAd;
import com.huawei.hms.ads.RequestOptions;

/**
 * Huawei AppGallery build: initializes Huawei Ads (Petal) for non-personalized ads only, so no
 * consent form is shown and there are no privacy options. Same API as the Play build's UMP flow.
 */
public final class AdsConsent {

    private static boolean started;
    private static boolean ready;

    private AdsConsent() {
    }

    /** Initializes Huawei Ads once, then runs {@code onReady}. */
    public static void gather(Activity activity, Runnable onReady) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!started) {
            started = true;
            try {
                HwAds.init(activity.getApplicationContext());
                RequestOptions options = HwAds.getRequestOptions().toBuilder()
                        .setNonPersonalizedAd(NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
                        .build();
                HwAds.setRequestOptions(options);
                ready = true;
            } catch (RuntimeException e) {
                Log.w("AdsConsent", "Huawei Ads init failed", e);
            }
        }
        if (ready && onReady != null) {
            onReady.run();
        }
    }

    public static boolean canRequestAds(Context context) {
        return ready;
    }

    public static boolean isPrivacyOptionsRequired(Context context) {
        return false;
    }

    public static void showPrivacyOptions(Activity activity) {
    }

    /** Debug builds always use Huawei's public test slots. */
    static String slot(Context context, int real, int test) {
        return context.getString(BuildConfig.DEBUG ? test : real);
    }
}
