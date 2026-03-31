package com.google.android.exoplayer2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class DefaultRenderersFactory implements RenderersFactory {
    public static final long DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS = 5000;
    public static final int EXTENSION_RENDERER_MODE_OFF = 0;
    public static final int EXTENSION_RENDERER_MODE_ON = 1;
    public static final int EXTENSION_RENDERER_MODE_PREFER = 2;
    protected static final int MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY = 50;
    private static final String TAG = "DefaultRenderersFactory";
    private final long allowedVideoJoiningTimeMs;
    private final Context context;
    @Nullable
    private final DrmSessionManager<FrameworkMediaCrypto> drmSessionManager;
    private final int extensionRendererMode;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ExtensionRendererMode {
    }

    public DefaultRenderersFactory(Context context2) {
        this(context2, (DrmSessionManager<FrameworkMediaCrypto>) null);
    }

    public DefaultRenderersFactory(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2) {
        this(context2, drmSessionManager2, 0);
    }

    public DefaultRenderersFactory(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, int extensionRendererMode2) {
        this(context2, drmSessionManager2, extensionRendererMode2, DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS);
    }

    public DefaultRenderersFactory(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, int extensionRendererMode2, long allowedVideoJoiningTimeMs2) {
        this.context = context2;
        this.drmSessionManager = drmSessionManager2;
        this.extensionRendererMode = extensionRendererMode2;
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs2;
    }

    public Renderer[] createRenderers(Handler eventHandler, VideoRendererEventListener videoRendererEventListener, AudioRendererEventListener audioRendererEventListener, TextOutput textRendererOutput, MetadataOutput metadataRendererOutput) {
        ArrayList<Renderer> renderersList = new ArrayList<>();
        buildVideoRenderers(this.context, this.drmSessionManager, this.allowedVideoJoiningTimeMs, eventHandler, videoRendererEventListener, this.extensionRendererMode, renderersList);
        buildAudioRenderers(this.context, this.drmSessionManager, buildAudioProcessors(), eventHandler, audioRendererEventListener, this.extensionRendererMode, renderersList);
        buildTextRenderers(this.context, textRendererOutput, eventHandler.getLooper(), this.extensionRendererMode, renderersList);
        buildMetadataRenderers(this.context, metadataRendererOutput, eventHandler.getLooper(), this.extensionRendererMode, renderersList);
        buildMiscellaneousRenderers(this.context, eventHandler, this.extensionRendererMode, renderersList);
        return (Renderer[]) renderersList.toArray(new Renderer[renderersList.size()]);
    }

    /* access modifiers changed from: protected */
    public void buildVideoRenderers(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, long allowedVideoJoiningTimeMs2, Handler eventHandler, VideoRendererEventListener eventListener, int extensionRendererMode2, ArrayList<Renderer> out) {
        int extensionRendererIndex;
        out.add(new MediaCodecVideoRenderer(context2, MediaCodecSelector.DEFAULT, allowedVideoJoiningTimeMs2, drmSessionManager2, false, eventHandler, eventListener, 50));
        if (extensionRendererMode2 != 0) {
            int extensionRendererIndex2 = out.size();
            if (extensionRendererMode2 == 2) {
                extensionRendererIndex = extensionRendererIndex2 - 1;
            } else {
                extensionRendererIndex = extensionRendererIndex2;
            }
            try {
                int i = extensionRendererIndex + 1;
                try {
                    out.add(extensionRendererIndex, (Renderer) Class.forName("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer").getConstructor(new Class[]{Boolean.TYPE, Long.TYPE, Handler.class, VideoRendererEventListener.class, Integer.TYPE}).newInstance(new Object[]{true, Long.valueOf(allowedVideoJoiningTimeMs2), eventHandler, eventListener, 50}));
                    Log.i(TAG, "Loaded LibvpxVideoRenderer.");
                } catch (ClassNotFoundException e) {
                } catch (Exception e2) {
                    e = e2;
                    throw new RuntimeException(e);
                }
            } catch (ClassNotFoundException e3) {
                int i2 = extensionRendererIndex;
            } catch (Exception e4) {
                e = e4;
                int i3 = extensionRendererIndex;
                throw new RuntimeException(e);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void buildAudioRenderers(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, AudioProcessor[] audioProcessors, Handler eventHandler, AudioRendererEventListener eventListener, int extensionRendererMode2, ArrayList<Renderer> out) {
        int extensionRendererIndex;
        int extensionRendererIndex2;
        int extensionRendererIndex3;
        int extensionRendererIndex4;
        int extensionRendererIndex5;
        out.add(new MediaCodecAudioRenderer(MediaCodecSelector.DEFAULT, drmSessionManager2, true, eventHandler, eventListener, AudioCapabilities.getCapabilities(context2), audioProcessors));
        if (extensionRendererMode2 != 0) {
            int extensionRendererIndex6 = out.size();
            if (extensionRendererMode2 == 2) {
                extensionRendererIndex = extensionRendererIndex6 - 1;
            } else {
                extensionRendererIndex = extensionRendererIndex6;
            }
            try {
                extensionRendererIndex2 = extensionRendererIndex + 1;
                try {
                    out.add(extensionRendererIndex, (Renderer) Class.forName("com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                    Log.i(TAG, "Loaded LibopusAudioRenderer.");
                    extensionRendererIndex3 = extensionRendererIndex2;
                } catch (ClassNotFoundException e) {
                    extensionRendererIndex3 = extensionRendererIndex2;
                    extensionRendererIndex4 = extensionRendererIndex3 + 1;
                    out.add(extensionRendererIndex3, (Renderer) Class.forName("com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                    Log.i(TAG, "Loaded LibflacAudioRenderer.");
                    extensionRendererIndex5 = extensionRendererIndex4;
                    int i = extensionRendererIndex5 + 1;
                    try {
                        out.add(extensionRendererIndex5, (Renderer) Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                        Log.i(TAG, "Loaded FfmpegAudioRenderer.");
                    } catch (ClassNotFoundException e2) {
                        return;
                    } catch (Exception e3) {
                        e = e3;
                        throw new RuntimeException(e);
                    }
                } catch (Exception e4) {
                    e = e4;
                    throw new RuntimeException(e);
                }
            } catch (ClassNotFoundException e5) {
                extensionRendererIndex2 = extensionRendererIndex;
                extensionRendererIndex3 = extensionRendererIndex2;
                extensionRendererIndex4 = extensionRendererIndex3 + 1;
                out.add(extensionRendererIndex3, (Renderer) Class.forName("com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                Log.i(TAG, "Loaded LibflacAudioRenderer.");
                extensionRendererIndex5 = extensionRendererIndex4;
                int i2 = extensionRendererIndex5 + 1;
                out.add(extensionRendererIndex5, (Renderer) Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                Log.i(TAG, "Loaded FfmpegAudioRenderer.");
            } catch (Exception e6) {
                e = e6;
                int i3 = extensionRendererIndex;
                throw new RuntimeException(e);
            }
            try {
                extensionRendererIndex4 = extensionRendererIndex3 + 1;
                try {
                    out.add(extensionRendererIndex3, (Renderer) Class.forName("com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                    Log.i(TAG, "Loaded LibflacAudioRenderer.");
                    extensionRendererIndex5 = extensionRendererIndex4;
                } catch (ClassNotFoundException e7) {
                    extensionRendererIndex5 = extensionRendererIndex4;
                    int i22 = extensionRendererIndex5 + 1;
                    out.add(extensionRendererIndex5, (Renderer) Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                    Log.i(TAG, "Loaded FfmpegAudioRenderer.");
                } catch (Exception e8) {
                    e = e8;
                    throw new RuntimeException(e);
                }
            } catch (ClassNotFoundException e9) {
                extensionRendererIndex4 = extensionRendererIndex3;
                extensionRendererIndex5 = extensionRendererIndex4;
                int i222 = extensionRendererIndex5 + 1;
                out.add(extensionRendererIndex5, (Renderer) Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                Log.i(TAG, "Loaded FfmpegAudioRenderer.");
            } catch (Exception e10) {
                e = e10;
                int i4 = extensionRendererIndex3;
                throw new RuntimeException(e);
            }
            try {
                int i2222 = extensionRendererIndex5 + 1;
                out.add(extensionRendererIndex5, (Renderer) Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                Log.i(TAG, "Loaded FfmpegAudioRenderer.");
            } catch (ClassNotFoundException e11) {
                int i5 = extensionRendererIndex5;
            } catch (Exception e12) {
                e = e12;
                int i6 = extensionRendererIndex5;
                throw new RuntimeException(e);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void buildTextRenderers(Context context2, TextOutput output, Looper outputLooper, int extensionRendererMode2, ArrayList<Renderer> out) {
        out.add(new TextRenderer(output, outputLooper));
    }

    /* access modifiers changed from: protected */
    public void buildMetadataRenderers(Context context2, MetadataOutput output, Looper outputLooper, int extensionRendererMode2, ArrayList<Renderer> out) {
        out.add(new MetadataRenderer(output, outputLooper));
    }

    /* access modifiers changed from: protected */
    public void buildMiscellaneousRenderers(Context context2, Handler eventHandler, int extensionRendererMode2, ArrayList<Renderer> arrayList) {
    }

    /* access modifiers changed from: protected */
    public AudioProcessor[] buildAudioProcessors() {
        return new AudioProcessor[0];
    }
}
