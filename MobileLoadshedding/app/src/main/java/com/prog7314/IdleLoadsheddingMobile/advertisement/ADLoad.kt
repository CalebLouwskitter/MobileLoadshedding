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
//replace adUnitId with the actual ad unit id, currently sey up for the test API
//do not worry about this for the deadline leave it as is


class ADLoad(private val activity: Activity) {
    var log = Log.d("ADVERTLOADER", "ADLoad Class called")
    private var rewardedAd : RewardedAd? = null

    fun loadRewardedAds(){
        if (rewardedAd != null) {
            return
        } //so we dont perma overload
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

    fun showRewardAd(/*afterCodeRewardCoin: (RewardItem) -> Unit*/){
        //when allowing for reward addition uncomment the parameter above
        if(rewardedAd != null){
            rewardedAd!!.fullScreenContentCallback = object : FullScreenContentCallback(){
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAds() //so that the next advert starts loading now and is ready asap
                    Log.d("ADVERTLOADER", "Ad dismissed")
                    //so this reports the has been closed after it has been successfully opened.
                    // it doesnt deteremine success just merely closes it
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    rewardedAd = null
                    loadRewardedAds() //so that the next advert starts loading now and is ready asap for when we try again
                    Log.d("ADVERTLOADER", "Ad Failed  ${p0.message}")
                    //the add just didn't work
                }
            }
            //the below is what determines success
            rewardedAd!!.show(activity){
                //sets our return type so that it tells the main block it can execute the code in it's { } in it's main call
                //afterCodeRewardCoin(it)
                // uncomment the above line when implementing reward
                Log.d("ADVERTLOADER", "Ad shown")

                val works = Log.d("ADVERTLOADER", "it's working")
            }
        } else{
            //do nothing
            //we can add an error window here at a later stage if we want but for now leave it as is. cause it's largely unnecessary
            Log.d("ADVERTLOADER", "showRewardAds: Rewarded AD Null please wait for the Ad fetch to fonosh")
        }
    }
}
