package org.twinone.ads;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class AdMobInterstitialHelper {

	private final AdInterface mInterface;
	private final Context mContext;
	private InterstitialAd mAd;

	public AdMobInterstitialHelper(Context context, AdInterface iface) {
		mContext = context;
		mInterface = iface;
		mAd = new InterstitialAd(context);
		mAd.setAdUnitId(mInterface.getInterstitialAdUnitId());
	}

	public void load() {
		AdRequest.Builder builder = new AdRequest.Builder();
		for (String device : mInterface.getTestDevices()) {
			builder.addTestDevice(device);
		}
		AdRequest request = builder.build();
		Log.d("INTERSTITIAL", "Loading...");
		mAd.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				super.onAdLoaded();
				show();
			}
		});

		mAd.loadAd(request);
	}

	private void show() {
		if (mAd.isLoaded()) {
			Log.d("INTERSTITIAL", "Done loading");
			mAd.show();
		} else {
			Log.w("INTERSTITIAL", "NOT Done loading but show() called");

		}
	}

}
