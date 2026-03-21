package com.soundless

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

private const val TAG = "SoundlessAppOpen"
private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-5340843733211852/2054744544"

class AppOpenAdManager(private val application: Application) :
    Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null
    private var hasShownOnce = false

    fun init() {
        application.registerActivityLifecycleCallbacks(this)
        loadAd()
    }

    private fun loadAd() {
        if (isLoading || appOpenAd != null) return
        // Check if ads are removed
        val prefs = application.getSharedPreferences("soundless", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("ads_removed", false)) return

        isLoading = true
        AppOpenAd.load(
            application,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App open ad loaded")
                    appOpenAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "App open ad failed to load: ${error.message}")
                    isLoading = false
                }
            }
        )
    }

    private fun showAdIfReady() {
        if (isShowingAd || hasShownOnce) return
        val activity = currentActivity ?: return
        val ad = appOpenAd ?: return

        // Check if ads are removed
        val prefs = application.getSharedPreferences("soundless", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("ads_removed", false)) return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                appOpenAd = null
                isShowingAd = false
                hasShownOnce = true
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Ad failed to show: ${error.message}")
                appOpenAd = null
                isShowingAd = false
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad shown")
                isShowingAd = true
            }
        }
        ad.show(activity)
    }

    // ActivityLifecycleCallbacks
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        showAdIfReady()
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }
}
