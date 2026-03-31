package com.google.android.exoplayer2.audio;

import com.google.android.exoplayer2.audio.AudioProcessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

final class ChannelMappingAudioProcessor implements AudioProcessor {
    private boolean active;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private int channelCount = -1;
    private boolean inputEnded;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private int[] outputChannels;
    private int[] pendingOutputChannels;
    private int sampleRateHz = -1;

    public void setChannelMap(int[] outputChannels2) {
        this.pendingOutputChannels = outputChannels2;
    }

    public boolean configure(int sampleRateHz2, int channelCount2, int encoding) throws AudioProcessor.UnhandledFormatException {
        boolean z;
        boolean outputChannelsChanged = !Arrays.equals(this.pendingOutputChannels, this.outputChannels);
        this.outputChannels = this.pendingOutputChannels;
        if (this.outputChannels == null) {
            this.active = false;
            return outputChannelsChanged;
        } else if (encoding != 2) {
            throw new AudioProcessor.UnhandledFormatException(sampleRateHz2, channelCount2, encoding);
        } else if (!outputChannelsChanged && this.sampleRateHz == sampleRateHz2 && this.channelCount == channelCount2) {
            return false;
        } else {
            this.sampleRateHz = sampleRateHz2;
            this.channelCount = channelCount2;
            if (channelCount2 != this.outputChannels.length) {
                z = true;
            } else {
                z = false;
            }
            this.active = z;
            int i = 0;
            while (i < this.outputChannels.length) {
                int channelIndex = this.outputChannels[i];
                if (channelIndex >= channelCount2) {
                    throw new AudioProcessor.UnhandledFormatException(sampleRateHz2, channelCount2, encoding);
                }
                this.active = (channelIndex != i) | this.active;
                i++;
            }
            return true;
        }
    }

    public boolean isActive() {
        return this.active;
    }

    public int getOutputChannelCount() {
        return this.outputChannels == null ? this.channelCount : this.outputChannels.length;
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
        int outputSize = this.outputChannels.length * ((limit - position) / (this.channelCount * 2)) * 2;
        if (this.buffer.capacity() < outputSize) {
            this.buffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder());
        } else {
            this.buffer.clear();
        }
        while (position < limit) {
            for (int channelIndex : this.outputChannels) {
                this.buffer.putShort(inputBuffer.getShort((channelIndex * 2) + position));
            }
            position += this.channelCount * 2;
        }
        inputBuffer.position(limit);
        this.buffer.flip();
        this.outputBuffer = this.buffer;
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
    }

    public void reset() {
        flush();
        this.buffer = EMPTY_BUFFER;
        this.channelCount = -1;
        this.sampleRateHz = -1;
        this.outputChannels = null;
        this.active = false;
    }
}
