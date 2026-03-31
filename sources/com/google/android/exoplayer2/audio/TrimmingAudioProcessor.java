package com.google.android.exoplayer2.audio;

import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class TrimmingAudioProcessor implements AudioProcessor {
    private ByteBuffer buffer = EMPTY_BUFFER;
    private int channelCount = -1;
    private byte[] endBuffer;
    private int endBufferSize;
    private boolean inputEnded;
    private boolean isActive;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private int pendingTrimStartBytes;
    private int sampleRateHz;
    private int trimEndSamples;
    private int trimStartSamples;

    public void setTrimSampleCount(int trimStartSamples2, int trimEndSamples2) {
        this.trimStartSamples = trimStartSamples2;
        this.trimEndSamples = trimEndSamples2;
    }

    public boolean configure(int sampleRateHz2, int channelCount2, int encoding) throws AudioProcessor.UnhandledFormatException {
        boolean z;
        if (encoding != 2) {
            throw new AudioProcessor.UnhandledFormatException(sampleRateHz2, channelCount2, encoding);
        }
        this.channelCount = channelCount2;
        this.sampleRateHz = sampleRateHz2;
        this.endBuffer = new byte[(this.trimEndSamples * channelCount2 * 2)];
        this.endBufferSize = 0;
        this.pendingTrimStartBytes = this.trimStartSamples * channelCount2 * 2;
        boolean wasActive = this.isActive;
        if (this.trimStartSamples == 0 && this.trimEndSamples == 0) {
            z = false;
        } else {
            z = true;
        }
        this.isActive = z;
        if (wasActive != this.isActive) {
            return true;
        }
        return false;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public int getOutputChannelCount() {
        return this.channelCount;
    }

    public int getOutputEncoding() {
        return 2;
    }

    public int getOutputSampleRateHz() {
        return this.sampleRateHz;
    }

    public void queueInput(ByteBuffer inputBuffer) {
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int remaining = limit - position;
        int trimBytes = Math.min(remaining, this.pendingTrimStartBytes);
        this.pendingTrimStartBytes -= trimBytes;
        inputBuffer.position(position + trimBytes);
        if (this.pendingTrimStartBytes <= 0) {
            int remaining2 = remaining - trimBytes;
            int remainingBytesToOutput = (this.endBufferSize + remaining2) - this.endBuffer.length;
            if (this.buffer.capacity() < remainingBytesToOutput) {
                this.buffer = ByteBuffer.allocateDirect(remainingBytesToOutput).order(ByteOrder.nativeOrder());
            } else {
                this.buffer.clear();
            }
            int endBufferBytesToOutput = Util.constrainValue(remainingBytesToOutput, 0, this.endBufferSize);
            this.buffer.put(this.endBuffer, 0, endBufferBytesToOutput);
            int inputBufferBytesToOutput = Util.constrainValue(remainingBytesToOutput - endBufferBytesToOutput, 0, remaining2);
            inputBuffer.limit(inputBuffer.position() + inputBufferBytesToOutput);
            this.buffer.put(inputBuffer);
            inputBuffer.limit(limit);
            int remaining3 = remaining2 - inputBufferBytesToOutput;
            this.endBufferSize -= endBufferBytesToOutput;
            System.arraycopy(this.endBuffer, endBufferBytesToOutput, this.endBuffer, 0, this.endBufferSize);
            inputBuffer.get(this.endBuffer, this.endBufferSize, remaining3);
            this.endBufferSize += remaining3;
            this.buffer.flip();
            this.outputBuffer = this.buffer;
        }
    }

    public void queueEndOfStream() {
        this.inputEnded = true;
    }

    public ByteBuffer getOutput() {
        ByteBuffer outputBuffer2 = this.outputBuffer;
        this.outputBuffer = EMPTY_BUFFER;
        return outputBuffer2;
    }

    public boolean isEnded() {
        return this.inputEnded && this.outputBuffer == EMPTY_BUFFER;
    }

    public void flush() {
        this.outputBuffer = EMPTY_BUFFER;
        this.inputEnded = false;
        this.pendingTrimStartBytes = 0;
        this.endBufferSize = 0;
    }

    public void reset() {
        flush();
        this.buffer = EMPTY_BUFFER;
        this.channelCount = -1;
        this.sampleRateHz = -1;
        this.endBuffer = null;
    }
}
