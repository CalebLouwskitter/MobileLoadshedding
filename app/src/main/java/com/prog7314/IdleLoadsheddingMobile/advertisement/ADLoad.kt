package com.prog7314.IdleLoadsheddingMobile.advertisement

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

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

    fun showRewardAds(afterCodeRewardCoin: (RewardItem) -> Unit){
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

    final val adUnitIdl = "ca-app-pub-3940256099942544/5224354917"

}
/*
* References:
* Coding-Meet (2023). Google-Admob-Integration-App. [online] GitHub. Available at: https://github.com/Coding-Meet/Google-Admob-Integration-App.git [Accessed 25 Oct. 2023].
* Google Developers (2023a). Get Started | Android. [online] Google for Developers. Available at: https://developers.google.com/admob/android/quick-start [Accessed 25 Oct. 2023].
* Google Developers (2023b). Rewarded Ads | Android. [online] Google for Developers. Available at: https://developers.google.com/admob/android/rewarded#kotlin_1 [Accessed 25 Oct. 2023].
* Google LLC (2023). googleads-mobile-android-examples. [online] GitHub. Available at: https://github.com/googleads/googleads-mobile-android-examples.kt [Accessed 25 Oct. 2023].
* hall9zeha (2022). AdvertisementsApp. [online] GitHub. Available at: https://github.com/hall9zeha/AdvertisementsApp.git [Accessed 25 Oct. 2023].
*/