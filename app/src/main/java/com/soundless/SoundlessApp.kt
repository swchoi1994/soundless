package com.soundless

import android.app.Application
import com.google.android.gms.ads.MobileAds

class SoundlessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {
            AppOpenAdManager(this).init()
        }
    }
}
