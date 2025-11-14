package com.prog7314.IdleLoadsheddingMobile.advertisement

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class ADLoad(private val activity: Activity) {

    private var rewardedAd : RewardedAd? = null
    //replace AD_UNIT_ID with the actual ad unit id

    fun loadRewardedAds(adUnitIdl : Int){
        val adReq = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            "AD_UNIT_ID",
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

    fun showRewardAds(adUnitIdl: Int, afterCodeRewardCoin: (RewardItem) -> Unit){
        if(rewardedAd != null){
            rewardedAd!!.fullScreenContentCallback = object : FullScreenContentCallback(){
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAds(adUnitIdl)
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    rewardedAd = null
                }
            }
            rewardedAd!!.show(activity){
                afterCodeRewardCoin(it)
            }
        } else{

        }
    }





}