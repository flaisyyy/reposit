package com.google.android.exoplayer2.extractor;

public interface SeekMap {
    long getDurationUs();

    long getPosition(long j);

    boolean isSeekable();

    public static final class Unseekable implements SeekMap {
        private final long durationUs;
        private final long startPosition;

        public Unseekable(long durationUs2) {
            this(durationUs2, 0);
        }

        public Unseekable(long durationUs2, long startPosition2) {
            this.durationUs = durationUs2;
            this.startPosition = startPosition2;
        }

        public boolean isSeekable() {
            return false;
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long getPosition(long timeUs) {
            return this.startPosition;
        }
    }
}
