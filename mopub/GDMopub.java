/**
 * Copyright 2017 FrogSquare. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package org.godotengine.godot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;

import com.mopub.mobileads.*;
import com.mopub.mobileads.MoPubView.BannerAdListener;
import com.mopub.mobileads.MoPubInterstitial.InterstitialAdListener;

import com.godot.game.BuildConfig;
import com.godot.game.R;

import org.godotengine.godot.Godot;
import org.godotengine.godot.Utils;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.Dictionary;

public class GDMopub extends Godot.SingletonBase {

	static public Godot.SingletonBase initialize (Activity p_activity) {
		return new GDMopub(p_activity);
	}

	public GDMopub(Activity p_activity) {
		activity = p_activity;

		registerClass ("GDMopub", new String[] {
		"init", "show_banner_ad", "show_interstitial_ad"
		});
	}

	public void init (final Dictionary p_dict, final int p_script_id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				_config = new JSONObject(p_dict);
				_init();

				Utils.setScriptInstance(p_script_id);
				Utils.d("Mopub::Initialized");
			}
		});
	}

	private void _init() {
		if (_config.optBoolean("BannerAd", false)) {
			createBanner();
		}

		if (_config.optBoolean("InterstitialAd", false)) {
			createInterstitial();
		}

		if (_config.optBoolean("RewardedVideoAd", false)) {
			createRewardedVideo();
		}
	}

	private void createBanner() {
		FrameLayout layout = ((Godot)activity).layout; // Getting Godots framelayout
		FrameLayout.LayoutParams AdParams = new FrameLayout.LayoutParams(
						 FrameLayout.LayoutParams.MATCH_PARENT,
						 FrameLayout.LayoutParams.WRAP_CONTENT);

		if(moPubView != null) { layout.removeView(moPubView); }

		if (_config.optString("BannerGravity", "BOTTOM").equals("BOTTOM")) {
			AdParams.gravity = Gravity.BOTTOM;
		} else { AdParams.gravity = Gravity.TOP; }

		final String banner_unit_id =
		_config.optString("BannerAdId", activity.getString(R.string.ad_unit_id_banner));

		moPubView = new MoPubView(activity);
		moPubView.setLayoutParams(AdParams);
		moPubView.setAdUnitId(banner_unit_id); // Enter your Ad Unit ID from www.mopub.com
		moPubView.setBannerAdListener(banner_listener);
		moPubView.setAutorefreshEnabled(true);
		moPubView.setVisibility(View.INVISIBLE);
		moPubView.loadAd();

		layout.addView(moPubView);
	}

	private void createInterstitial() {
		final String interstitial_unit_id =
		_config.optString("InterstitialAdId", activity.getString(R.string.ad_unit_id_banner));

		mInterstitial = new MoPubInterstitial(activity, interstitial_unit_id);
		mInterstitial.setInterstitialAdListener(interstitial_listener);
		mInterstitial.load();
	}

	private void createRewardedVideo() {

	}

	public void show_banner_ad(final boolean show) {
		if (moPubView == null) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (show) {
					if (!moPubView.isEnabled()) { moPubView.setEnabled(true); }
					if (moPubView.getVisibility() == View.INVISIBLE) {
						Utils.d("MoPub:Visiblity:On");
						moPubView.setVisibility(View.VISIBLE);
				}
				} else {
					if (moPubView.isEnabled()) { moPubView.setEnabled(false); }
					if (moPubView.getVisibility() != View.INVISIBLE) {
						Utils.d("MoPub:Visiblity:Off");
						moPubView.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
	}

	public void show_interstitial_ad() {
		if (mInterstitial == null) { return; }

		if (mInterstitial.isReady()) {
			Utils.d("Mopub::Interstitial::Showing");
			mInterstitial.show();
		} else {
			// Caching is likely already in progress if `isReady()` is false.
			// Avoid calling `load()` here and instead rely on the callbacks as suggested below.
			Utils.d("Mopub::Interstitial::NotReady");
		}
	}

	private BannerAdListener banner_listener = new BannerAdListener() {
		// Sent when the banner has successfully retrieved an ad.

		@Override
		public void onBannerLoaded(MoPubView banner) {
			// The Banner has been cached and is ready to be shown.
			Utils.d("Mopub::Banner::Load::Success");
			Utils.callScriptFunc("Mopub", "BannerLoad", "Success");
		}

		@Override
		public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
			Utils.d("Mopub::Banner::Load::Failed:: " + errorCode.toString());
			Utils.callScriptFunc("Mopub", "BannerLoad", "Falied");
		}

		@Override
		public void onBannerClicked(MoPubView banner) {
			// Sent when the user has tapped on the banner.
		}

		@Override
		public void onBannerExpanded(MoPubView banner) {
			// Sent when the banner has just taken over the screen.
		}

		@Override
		public void onBannerCollapsed(MoPubView banner) {
			// Sent when an expanded banner has collapsed back to its original size.
		}
	};

	private InterstitialAdListener interstitial_listener = new InterstitialAdListener() {
		@Override
		public void onInterstitialLoaded(MoPubInterstitial interstitial) {
			// The interstitial has been cached and is ready to be shown.
			Utils.d("Mopub::Interstitial::Load::Success");
			Utils.callScriptFunc("Mopub", "InterstitialLoad", "Success");
		}

		@Override
		public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
			// The interstitial has failed to load. Inspect errorCode for additional information.
			// This is an excellent place to load more ads.
			Utils.d("Mopub::Interstitial::Load::Failed:: " + errorCode.toString());
			Utils.callScriptFunc("Mopub", "InterstitialLoad", "Failed");
		}

		@Override
		public void onInterstitialShown(MoPubInterstitial interstitial) {
			// The interstitial has been shown. Pause / save state accordingly.
		}

		@Override
		public void onInterstitialClicked(MoPubInterstitial interstitial) {}

		@Override
		public void onInterstitialDismissed(MoPubInterstitial interstitial) {
			// The interstitial has being dismissed. Resume / load state accordingly.
			// This is an excellent place to load more ads.
		}
	};

	protected void onMainActivityResult (int requestCode, int resultCode, Intent data) {
	}

	protected void onMainPause () {
	}

	protected void onMainResume () {
	}

	protected void onMainDestroy () {
		if (moPubView != null) { moPubView.destroy(); }
		if (mInterstitial != null) { mInterstitial.destroy(); }
	}

	private Activity activity = null;
	private JSONObject _config = null;

	private MoPubView moPubView;
	private MoPubInterstitial mInterstitial;
}
