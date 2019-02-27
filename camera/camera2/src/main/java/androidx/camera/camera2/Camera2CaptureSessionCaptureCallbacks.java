/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Different implementations of {@link CameraCaptureSession.CaptureCallback}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class Camera2CaptureSessionCaptureCallbacks {
    private Camera2CaptureSessionCaptureCallbacks() {
    }

    /** Returns a session capture callback which does nothing. */
    public static CameraCaptureSession.CaptureCallback createNoOpCallback() {
        return new NoOpSessionCaptureCallback();
    }

    /** Returns a session capture callback which calls a list of other callbacks. */
    static CameraCaptureSession.CaptureCallback createComboCallback(
            List<CameraCaptureSession.CaptureCallback> callbacks) {
        return new ComboSessionCaptureCallback(callbacks);
    }

    /** Returns a session capture callback which calls a list of other callbacks. */
    public static CameraCaptureSession.CaptureCallback createComboCallback(
            CameraCaptureSession.CaptureCallback... callbacks) {
        return createComboCallback(Arrays.asList(callbacks));
    }

    static final class NoOpSessionCaptureCallback
            extends CameraCaptureSession.CaptureCallback {
        @Override
        public void onCaptureBufferLost(
                CameraCaptureSession session,
                CaptureRequest request,
                Surface surface,
                long frame) {
        }

        @Override
        public void onCaptureCompleted(
                CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
        }

        @Override
        public void onCaptureFailed(
                CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
        }

        @Override
        public void onCaptureProgressed(
                CameraCaptureSession session,
                CaptureRequest request,
                CaptureResult partialResult) {
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
        }

        @Override
        public void onCaptureSequenceCompleted(
                CameraCaptureSession session, int sequenceId, long frame) {
        }

        @Override
        public void onCaptureStarted(
                CameraCaptureSession session, CaptureRequest request, long timestamp, long frame) {
        }
    }

    private static final class ComboSessionCaptureCallback
            extends CameraCaptureSession.CaptureCallback {
        private final List<CameraCaptureSession.CaptureCallback> mCallbacks = new ArrayList<>();

        ComboSessionCaptureCallback(List<CameraCaptureSession.CaptureCallback> callbacks) {
            for (CameraCaptureSession.CaptureCallback callback : callbacks) {
                // A no-op callback doesn't do anything, so avoid adding it to the final list.
                if (!(callback instanceof NoOpSessionCaptureCallback)) {
                    mCallbacks.add(callback);
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onCaptureBufferLost(
                CameraCaptureSession session, CaptureRequest request, Surface surface, long frame) {
            for (CameraCaptureSession.CaptureCallback callback : mCallbacks) {
                callback.onCaptureBufferLost(session, request, surface, frame);
            }
        }

        @Override
        public void onCaptureCompleted(
                CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            for (CameraCaptureSession.CaptureCallback callback : mCallbacks) {
                callback.onCaptureCompleted(session, request, result);
            }
        }

        @Override
        public void onCaptureFailed(
                CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            for (CameraCaptureSession.CaptureCallback callback : mCallbacks) {
                callback.onCaptureFailed(session, request, failure);
            }
        }

        @Override
        public void onCaptureProgressed(
                CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            for (CameraCaptureSession.CaptureCallback callback : mCallbacks) {
                callback.onCaptureProgressed(session, request, partialResult);
            }
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            for (CameraCaptureSession.CaptureCallback callback : mCallbacks) {
                callback.onCaptureSequenceAborted(session, sequenceId);
            }
        }

        @Override
        public void onCaptureSequenceCompleted(
                CameraCaptureSession session, int sequenceId, long frame) {
            for (CameraCaptureSession.CaptureCallback callback : mCallbacks) {
                callback.onCaptureSequenceCompleted(session, sequenceId, frame);
            }
        }

        @Override
        public void onCaptureStarted(
                CameraCaptureSession session, CaptureRequest request, long timestamp, long frame) {
            for (CameraCaptureSession.CaptureCallback callback : mCallbacks) {
                callback.onCaptureStarted(session, request, timestamp, frame);
            }
        }
    }
}
