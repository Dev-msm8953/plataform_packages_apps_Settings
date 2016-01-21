/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.Settings;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;

public class ToggleScreenMagnificationPreferenceFragment
        extends ToggleFeaturePreferenceFragment {

    protected class VideoPreference extends Preference {
        public VideoPreference(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            view.setDividerAllowedAbove(false);
            view.setDividerAllowedBelow(false);
            final RelativeLayout background =
                    (RelativeLayout) view.findViewById(R.id.video_background);
            final VideoView videoView = (VideoView) view.findViewById(R.id.video);

            // Hacky adjustment for using VideoView in recycle view and positioning
            // it on the background image
            Resources res = getPrefContext().getResources();
            final int backgroundWidth = res.getDimensionPixelSize(
                    R.dimen.screen_magnification_video_background_width);
            final int backgroundHeight = res.getDimensionPixelSize(
                    R.dimen.screen_magnification_video_background_height);
            final int videoWidth = res.getDimensionPixelSize(
                    R.dimen.screen_magnification_video_width);
            final int videoHeight = res.getDimensionPixelSize(
                    R.dimen.screen_magnification_video_height);
            final int videoMarginTop = res.getDimensionPixelSize(
                    R.dimen.screen_magnification_video_margin_top);
            final int screenWidth = getScreenWidth(getPrefContext());

            RelativeLayout.LayoutParams videoLp = new RelativeLayout.LayoutParams(
                    screenWidth * videoWidth / backgroundWidth,
                    screenWidth * videoHeight / backgroundWidth);
            videoLp.setMargins(0, screenWidth * videoMarginTop / backgroundWidth, 0, 0);
            videoView.setLayoutParams(videoLp);

            RelativeLayout.LayoutParams backgroundLp = new RelativeLayout.LayoutParams(
                    screenWidth,
                    screenWidth * backgroundHeight / backgroundWidth);
            background.setLayoutParams(backgroundLp);
            background.setBackgroundResource(
                    R.drawable.accessibility_screen_magnification_background);
            background.setGravity(Gravity.CENTER_HORIZONTAL);

            // Loop the video.
            videoView.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setLooping(true);
                }
            });

            videoView.setVideoURI(Uri.parse(String.format("%s://%s/%s",
                    ContentResolver.SCHEME_ANDROID_RESOURCE,
                    getPrefContext().getPackageName(),
                    R.raw.accessibility_screen_magnification)));
            videoView.setMediaController(new MediaController(getPrefContext()));
            videoView.start();
        }
    }

    protected VideoPreference mVideoPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideoPreference = new VideoPreference(getPrefContext());
        mVideoPreference.setSelectable(false);
        mVideoPreference.setPersistent(false);
        mVideoPreference.setLayoutResource(R.layout.video_preference);

        final PreferenceScreen preferenceScreen = getPreferenceManager().getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);
        mVideoPreference.setOrder(0);
        mSummaryPreference.setOrder(1);
        preferenceScreen.addPreference(mVideoPreference);
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        // Do nothing.
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                mSwitchBar.setCheckedInternal(checked);
                getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, checked);
                onPreferenceToggled(mPreferenceKey, checked);
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Temporarily enable Magnification on this screen if it's disabled.
        if (Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 0) {
            setMagnificationEnabled(1);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mToggleSwitch.isChecked()) {
            setMagnificationEnabled(0);
        }
    }

    private void setMagnificationEnabled(int enabled) {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, enabled);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION;
    }

    private static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }
}
