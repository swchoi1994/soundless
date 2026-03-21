package com.soundless

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "SoundlessAd"
private const val BANNER_AD_UNIT_ID = "ca-app-pub-5340843733211852/2888479916"

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Banner ad failed: code=${error.code} msg=${error.message}")
                }
            }
        }
    }

    DisposableEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
        onDispose { adView.destroy() }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView },
    )
}
