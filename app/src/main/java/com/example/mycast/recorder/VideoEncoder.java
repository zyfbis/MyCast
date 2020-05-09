/*
 * Copyright (c) 2017 Yrom Wang <http://www.yrom.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mycast.recorder;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import java.util.Objects;

/**
 * @author yrom
 * @version 2017/12/3
 */
class VideoEncoder extends BaseEncoder {
    private static final boolean VERBOSE = false;
    private VideoEncodeConfig mConfig;
    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection mMediaProjection;

    VideoEncoder(VideoEncodeConfig config, MediaProjection mediaProjection) {
        super(config.codecName);
        this.mConfig = config;
        this.mMediaProjection = mediaProjection;
    }

    @Override
    protected void onEncoderConfigured(MediaCodec encoder) {
        mSurface = encoder.createInputSurface();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenRecorder-display",
                mConfig.height, mConfig.width, 1 /*dpi*/,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mSurface, null, null);
        if (VERBOSE) Log.i("@@", "VideoEncoder create input surface: " + mSurface);
    }

    @Override
    protected MediaFormat createMediaFormat() {
        return mConfig.toFormat();
    }

    /**
     * @throws NullPointerException if prepare() not call
     */
    Surface getInputSurface() {
        return Objects.requireNonNull(mSurface, "doesn't prepare()");
    }

    @Override
    public void release() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        super.release();
    }


}
