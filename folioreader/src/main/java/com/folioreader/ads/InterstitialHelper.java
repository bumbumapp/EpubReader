package com.folioreader.ads;

import com.folioreader.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Single preloaded interstitial with a frequency cap: at most one per 20 minutes and none in the first
 * 3 minutes after install (shown when opening a book, contents or settings; never mid-page). The cap is stored in SharedPreferences so it survives restarts.
 * Call {@link #preload(Context)} once ads can be requested and {@link #showIfReady(Activity, Runnable)} at
 * natural break points; {@code next} always runs exactly once.
 */
public final class InterstitialHelper {

    public static final long MIN_INTERVAL_MS = 20 * 60_000L;
    public static final long FIRST_GRACE_MS = 3 * 60_000L;

    private static final String PREFS = "interstitial_cap";
    private static final String KEY_LAST = "last_shown_ms";
    private static final String KEY_FIRST_SEEN = "first_seen_ms";

    private static InterstitialAd interstitialAd;
    private static boolean loading;

    private InterstitialHelper() {
    }

    public static void preload(Context context) {
        if (interstitialAd != null || loading || !AdsConsent.canRequestAds(context)) {
            return;
        }
        loading = true;
        InterstitialAd.load(context.getApplicationContext(), context.getString(R.string.admob_interstitial_id),
                new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        loading = false;
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loading = false;
                        interstitialAd = null;
                    }
                });
    }

    /** Pure frequency-cap rule, unit tested. */
    static boolean capAllows(long now, long lastShown, long firstSeen) {
        return now - firstSeen >= FIRST_GRACE_MS && now - lastShown >= MIN_INTERVAL_MS;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static long firstSeen(SharedPreferences p, long now) {
        long first = p.getLong(KEY_FIRST_SEEN, 0L);
        if (first == 0L) {
            first = now;
            p.edit().putLong(KEY_FIRST_SEEN, first).apply();
        }
        return first;
    }

    public static void showIfReady(Activity activity, Runnable next) {
        final Runnable after = next != null ? next : () -> { };
        long now = System.currentTimeMillis();
        SharedPreferences p = activity != null ? prefs(activity) : null;
        if (interstitialAd == null || p == null || activity.isFinishing()
                || !capAllows(now, p.getLong(KEY_LAST, 0L), firstSeen(p, now))) {
            if (activity != null) {
                preload(activity);
            }
            after.run();
            return;
        }
        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                preload(activity);
                after.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                preload(activity);
                after.run();
            }
        });
        p.edit().putLong(KEY_LAST, now).apply();
        ad.show(activity);
    }
}
