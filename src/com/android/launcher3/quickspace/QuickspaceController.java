/*
 * Copyright (C) 2018 CypherOS
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
package com.android.launcher3.quickspace;

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;

import android.annotation.NonNull;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.MediaSessionManagerHelper;
import com.android.launcher3.util.MSMHProxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuickspaceController implements MediaSessionManagerHelper.MediaMetadataListener {

    private static final String TAG = "Launcher3:QuickspaceController";

    private final List<OnDataListener> mListeners =
        Collections.synchronizedList(new ArrayList<>());
    private final Context mContext;
    private QuickEventsController mEventsController;
    private boolean mMediaRegistered = false;

    private static final long DATE_UPDATE_DELAY_MS = 3 * 60 * 1000;

    private final Handler mHandler = MAIN_EXECUTOR.getHandler();

    private final Runnable mOnDataUpdatedRunnable = new Runnable() {
            @Override
            public void run() {
                for (OnDataListener list : new ArrayList<>(mListeners)) {
                    list.onDataUpdated();
                }
            }
        };

    private Runnable mDateRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.removeCallbacks(this);
                if (mEventsController == null) return;
                mEventsController.updateDateAndGreetings();
                mHandler.postDelayed(this, DATE_UPDATE_DELAY_MS);
                notifyListeners();
            }
        };

    public interface OnDataListener {
        void onDataUpdated();
    }

    public QuickspaceController(Context context) {
        mContext = context;
        mEventsController = new QuickEventsController(context);
    }

    public void addListener(OnDataListener listener) {
        if (listener == null) return;
        boolean wasEmpty = mListeners.isEmpty();
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
        if (wasEmpty) {
            registerMediaController();
            mEventsController.initQuickEvents();
            scheduleDateUpdate();
        }
        listener.onDataUpdated();
    }

    public void removeListener(OnDataListener listener) {
        if (listener == null) return;
        mListeners.remove(listener);
        if (mListeners.isEmpty()) {
            unregisterMediaController();
            mHandler.removeCallbacks(mDateRunnable);
            mHandler.removeCallbacks(mOnDataUpdatedRunnable);
        }
    }

    public boolean isQuickEvent() {
        return mEventsController.isQuickEvent();
    }

    public QuickEventsController getEventController() {
        return mEventsController;
    }

    public void onPause() {
        unregisterMediaController();
        mHandler.removeCallbacks(mDateRunnable);
        mHandler.removeCallbacks(mOnDataUpdatedRunnable);
    }

    public void onResume() {
        registerMediaController();
        updateMediaController();
        scheduleDateUpdate();
        notifyListeners();
    }

    public void onDestroy() {
        unregisterMediaController();
        mHandler.removeCallbacks(mDateRunnable);
        mHandler.removeCallbacks(mOnDataUpdatedRunnable);
        for (OnDataListener listener : new ArrayList<>(mListeners)) {
            removeListener(listener);
        }
    }

    private void scheduleDateUpdate() {
        mHandler.removeCallbacks(mDateRunnable);
        mHandler.post(mDateRunnable);
    }

    public void notifyListeners() {
        mHandler.removeCallbacks(mOnDataUpdatedRunnable);
        mHandler.post(mOnDataUpdatedRunnable);
    }

    private void registerMediaController() {
        if (mMediaRegistered || !Utilities.isQuickspaceNowPlaying(mContext)) return;
        MSMHProxy.INSTANCE(mContext).addMediaMetadataListener(this);
        mMediaRegistered = true;
    }

    private void unregisterMediaController() {
        if (!mMediaRegistered) return;
        MSMHProxy.INSTANCE(mContext).removeMediaMetadataListener(this);
        mMediaRegistered = false;
    }

    private boolean updateMediaController() {
        if (!Utilities.isQuickspaceNowPlaying(mContext)) {
            return false;
        }
        MediaMetadata mediaMetadata = MSMHProxy.INSTANCE(mContext).getCurrentMediaMetadata();
        boolean isPlaying = MSMHProxy.INSTANCE(mContext).isMediaPlaying();
        String trackArtist = isPlaying && mediaMetadata != null ? mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) : "";
        String trackTitle = isPlaying && mediaMetadata != null ? mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE) : "";
        mEventsController.setMediaInfo(trackTitle, trackArtist, isPlaying);
        mEventsController.updateQuickEvents();
        return true;
    }

    @Override
    public void onMediaMetadataChanged() {
        if (updateMediaController()) notifyListeners();
    }

    @Override
    public void onPlaybackStateChanged() {
        if (updateMediaController()) notifyListeners();
    }
}
