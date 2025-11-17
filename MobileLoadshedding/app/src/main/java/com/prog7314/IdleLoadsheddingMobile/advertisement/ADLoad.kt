package com.prog7314.IdleLoadsheddingMobile.advertisement

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError //Google, 2025
import com.google.android.gms.ads.AdRequest //Google, 2025
import com.google.android.gms.ads.FullScreenContentCallback //Google, 2025
import com.google.android.gms.ads.LoadAdError //Google, 2025
import com.google.android.gms.ads.OnUserEarnedRewardListener //Google, 2025
import com.google.android.gms.ads.rewarded.RewardItem //Google, 2025
import com.google.android.gms.ads.rewarded.RewardedAd //Google, 2025
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback //Google, 2025
import com.google.android.gms.ads.MobileAds //Google, 2025
import com.google.android.gms.ads.RequestConfiguration //Google, 2025

private val adUnitIdl = "ca-app-pub-3940256099942544/5224354917"

class ADLoad(private val activity: Activity) {

    private var rewardedAd : RewardedAd? = null
    //replace adUnitId with the actual ad unit id, currently sey up for the test API

    fun loadRewardedAds(){
        val adReq = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            adUnitIdl,
            adReq,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d("ADVERTLOADER", "Ad Loaded")
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    Log.d("ADVERTLOADER", "The Ad Load Failed: ${p0.message}")
                }
            },
        )
    }

    fun showRewardAd(afterCodeRewardCoin: (RewardItem) -> Unit){
        if(rewardedAd != null){
            rewardedAd!!.fullScreenContentCallback = object : FullScreenContentCallback(){
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAds()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    rewardedAd = null
                }
            }
            rewardedAd!!.show(activity){
                afterCodeRewardCoin(it)
            }
        } else{
            //do nothing
            //we can add an error window here at a later stage.
            Log.d("ADVERTLOADER", "showRewardAds: Rewarded AD Null ")
        }
    }
}
