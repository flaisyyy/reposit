package com.google.android.exoplayer2.audio;

import android.support.v4.media.session.PlaybackStateCompat;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public final class SonicAudioProcessor implements AudioProcessor {
    private static final float CLOSE_THRESHOLD = 0.01f;
    public static final float MAXIMUM_PITCH = 8.0f;
    public static final float MAXIMUM_SPEED = 8.0f;
    public static final float MINIMUM_PITCH = 0.1f;
    public static final float MINIMUM_SPEED = 0.1f;
    private static final int MIN_BYTES_FOR_SPEEDUP_CALCULATION = 1024;
    public static final int SAMPLE_RATE_NO_CHANGE = -1;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private int channelCount = -1;
    private long inputBytes;
    private boolean inputEnded;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private long outputBytes;
    private int outputSampleRateHz = -1;
    private int pendingOutputSampleRateHz = -1;
    private float pitch = 1.0f;
    private int sampleRateHz = -1;
    private ShortBuffer shortBuffer = this.buffer.asShortBuffer();
    private Sonic sonic;
    private float speed = 1.0f;

    public float setSpeed(float speed2) {
        this.speed = Util.constrainValue(speed2, 0.1f, 8.0f);
        return this.speed;
    }

    public float setPitch(float pitch2) {
        this.pitch = Util.constrainValue(pitch2, 0.1f, 8.0f);
        return pitch2;
    }

    public void setOutputSampleRateHz(int sampleRateHz2) {
        this.pendingOutputSampleRateHz = sampleRateHz2;
    }

    public long scaleDurationForSpeedup(long duration) {
        if (this.outputBytes < PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
            return (long) (((double) this.speed) * ((double) duration));
        }
        if (this.outputSampleRateHz == this.sampleRateHz) {
            return Util.scaleLargeTimestamp(duration, this.inputBytes, this.outputBytes);
        }
        return Util.scaleLargeTimestamp(duration, ((long) this.outputSampleRateHz) * this.inputBytes, ((long) this.sampleRateHz) * this.outputBytes);
    }

    public boolean configure(int sampleRateHz2, int channelCount2, int encoding) throws AudioProcessor.UnhandledFormatException {
        if (encoding != 2) {
            throw new AudioProcessor.UnhandledFormatException(sampleRateHz2, channelCount2, encoding);
        }
        int outputSampleRateHz2 = this.pendingOutputSampleRateHz == -1 ? sampleRateHz2 : this.pendingOutputSampleRateHz;
        if (this.sampleRateHz == sampleRateHz2 && this.channelCount == channelCount2 && this.outputSampleRateHz == outputSampleRateHz2) {
            return false;
        }
        this.sampleRateHz = sampleRateHz2;
        this.channelCount = channelCount2;
        this.outputSampleRateHz = outputSampleRateHz2;
        return true;
    }

    public boolean isActive() {
        return Math.abs(this.speed - 1.0f) >= CLOSE_THRESHOLD || Math.abs(this.pitch - 1.0f) >= CLOSE_THRESHOLD || this.outputSampleRateHz != this.sampleRateHz;
    }

    public int getOutputChannelCount() {
        return this.channelCount;
    }

    public int getOutputEncoding() {
        return 2;
    }

    public int getOutputSampleRateHz() {
        return this.outputSampleRateHz;
    }

    public void queueInput(ByteBuffer inputBuffer) {
        if (inputBuffer.hasRemaining()) {
            ShortBuffer shortBuffer2 = inputBuffer.asShortBuffer();
            int inputSize = inputBuffer.remaining();
            this.inputBytes += (long) inputSize;
            this.sonic.queueInput(shortBuffer2);
            inputBuffer.position(inputBuffer.position() + inputSize);
        }
        int outputSize = this.sonic.getSamplesAvailable() * this.channelCount * 2;
        if (outputSize > 0) {
            if (this.buffer.capacity() < outputSize) {
                this.buffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder());
                this.shortBuffer = this.buffer.asShortBuffer();
            } else {
                this.buffer.clear();
                this.shortBuffer.clear();
            }
            this.sonic.getOutput(this.shortBuffer);
            this.outputBytes += (long) outputSize;
            this.buffer.limit(outputSize);
            this.outputBuffer = this.buffer;
        }
    }

    public void queueEndOfStream() {
        this.sonic.queueEndOfStream();
        this.inputEnded = true;
    }

    public ByteBuffer getOutput() {
        ByteBuffer outputBuffer2 = this.outputBuffer;
        this.outputBuffer = EMPTY_BUFFER;
        return outputBuffer2;
    }

    public boolean isEnded() {
        return this.inputEnded && (this.sonic == null || this.sonic.getSamplesAvailable() == 0);
    }

    public void flush() {
        this.sonic = new Sonic(this.sampleRateHz, this.channelCount, this.speed, this.pitch, this.outputSampleRateHz);
        this.outputBuffer = EMPTY_BUFFER;
        this.inputBytes = 0;
        this.outputBytes = 0;
        this.inputEnded = false;
    }

    public void reset() {
        this.sonic = null;
        this.buffer = EMPTY_BUFFER;
        this.shortBuffer = this.buffer.asShortBuffer();
        this.outputBuffer = EMPTY_BUFFER;
        this.channelCount = -1;
        this.sampleRateHz = -1;
        this.outputSampleRateHz = -1;
        this.inputBytes = 0;
        this.outputBytes = 0;
        this.inputEnded = false;
        this.pendingOutputSampleRateHz = -1;
    }
}
