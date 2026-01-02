package com.joory.whiteboardapp.functions

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.joory.whiteboardapp.MainActivity
import com.joory.whiteboardapp.MyCanvas

class Ads(var context: Context, var canvas: MyCanvas) {
    private var mInterstitialAd: InterstitialAd? = null
    private var mainHandler = Handler(Looper.getMainLooper())
    private val showAdDelay = Runnable { showAds() }


    fun resetAdInterval() {
        mainHandler.removeCallbacks(showAdDelay)
        showAdInterval()
    }

    fun showAdInterval() {
        mainHandler.postDelayed(showAdDelay, 1000 * 60 * 3)
    }

    fun loadFullScreenAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            "ca-app-pub-1226999690478326/4181492971",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    resetAdInterval()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                    loadFullScreenAd()
                }
            }
        )
    }

    fun showAds() {
        if (mInterstitialAd != null) {
            mInterstitialAd!!.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdClicked() {}

                    override fun onAdDismissedFullScreenContent() {
                        mInterstitialAd = null
                        loadFullScreenAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        mInterstitialAd = null
                        loadFullScreenAd()
                    }

                    override fun onAdImpression() {}

                    override fun onAdShowedFullScreenContent() {}
                }
            mInterstitialAd?.show(context as MainActivity)
        }
        loadFullScreenAd()
    }

}