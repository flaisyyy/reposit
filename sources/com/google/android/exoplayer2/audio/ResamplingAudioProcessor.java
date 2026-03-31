package com.google.android.exoplayer2.audio;

import com.google.android.exoplayer2.audio.AudioProcessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ResamplingAudioProcessor implements AudioProcessor {
    private ByteBuffer buffer = EMPTY_BUFFER;
    private int channelCount = -1;
    private int encoding = 0;
    private boolean inputEnded;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private int sampleRateHz = -1;

    public boolean configure(int sampleRateHz2, int channelCount2, int encoding2) throws AudioProcessor.UnhandledFormatException {
        if (encoding2 != 3 && encoding2 != 2 && encoding2 != Integer.MIN_VALUE && encoding2 != 1073741824) {
            throw new AudioProcessor.UnhandledFormatException(sampleRateHz2, channelCount2, encoding2);
        } else if (this.sampleRateHz == sampleRateHz2 && this.channelCount == channelCount2 && this.encoding == encoding2) {
            return false;
        } else {
            this.sampleRateHz = sampleRateHz2;
            this.channelCount = channelCount2;
            this.encoding = encoding2;
            if (encoding2 == 2) {
                this.buffer = EMPTY_BUFFER;
            }
            return true;
        }
    }

    public boolean isActive() {
        return (this.encoding == 0 || this.encoding == 2) ? false : true;
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
        int resampledSize;
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int size = limit - position;
        switch (this.encoding) {
            case Integer.MIN_VALUE:
                resampledSize = (size / 3) * 2;
                break;
            case 3:
                resampledSize = size * 2;
                break;
            case 1073741824:
                resampledSize = size / 2;
                break;
            default:
                throw new IllegalStateException();
        }
        if (this.buffer.capacity() < resampledSize) {
            this.buffer = ByteBuffer.allocateDirect(resampledSize).order(ByteOrder.nativeOrder());
        } else {
            this.buffer.clear();
        }
        switch (this.encoding) {
            case Integer.MIN_VALUE:
                for (int i = position; i < limit; i += 3) {
                    this.buffer.put(inputBuffer.get(i + 1));
                    this.buffer.put(inputBuffer.get(i + 2));
                }
                break;
            case 3:
                for (int i2 = position; i2 < limit; i2++) {
                    this.buffer.put((byte) 0);
                    this.buffer.put((byte) ((inputBuffer.get(i2) & 255) - 128));
                }
                break;
            case 1073741824:
                for (int i3 = position; i3 < limit; i3 += 4) {
                    this.buffer.put(inputBuffer.get(i3 + 2));
                    this.buffer.put(inputBuffer.get(i3 + 3));
                }
                break;
            default:
                throw new IllegalStateException();
        }
        inputBuffer.position(inputBuffer.limit());
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
        this.sampleRateHz = -1;
        this.channelCount = -1;
        this.encoding = 0;
    }
}
