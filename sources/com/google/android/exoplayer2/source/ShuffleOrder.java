package com.google.android.exoplayer2.source;

import java.util.Arrays;
import java.util.Random;

public interface ShuffleOrder {
    ShuffleOrder cloneAndInsert(int i, int i2);

    ShuffleOrder cloneAndRemove(int i);

    int getFirstIndex();

    int getLastIndex();

    int getLength();

    int getNextIndex(int i);

    int getPreviousIndex(int i);

    public static class DefaultShuffleOrder implements ShuffleOrder {
        private final int[] indexInShuffled;
        private final Random random;
        private final int[] shuffled;

        public DefaultShuffleOrder(int length) {
            this(length, new Random());
        }

        public DefaultShuffleOrder(int length, long randomSeed) {
            this(length, new Random(randomSeed));
        }

        private DefaultShuffleOrder(int length, Random random2) {
            this(createShuffledList(length, random2), random2);
        }

        private DefaultShuffleOrder(int[] shuffled2, Random random2) {
            this.shuffled = shuffled2;
            this.random = random2;
            this.indexInShuffled = new int[shuffled2.length];
            for (int i = 0; i < shuffled2.length; i++) {
                this.indexInShuffled[shuffled2[i]] = i;
            }
        }

        public int getLength() {
            return this.shuffled.length;
        }

        public int getNextIndex(int index) {
            int shuffledIndex = this.indexInShuffled[index] + 1;
            if (shuffledIndex < this.shuffled.length) {
                return this.shuffled[shuffledIndex];
            }
            return -1;
        }

        public int getPreviousIndex(int index) {
            int shuffledIndex = this.indexInShuffled[index] - 1;
            if (shuffledIndex >= 0) {
                return this.shuffled[shuffledIndex];
            }
            return -1;
        }

        public int getLastIndex() {
            if (this.shuffled.length > 0) {
                return this.shuffled[this.shuffled.length - 1];
            }
            return -1;
        }

        public int getFirstIndex() {
            if (this.shuffled.length > 0) {
                return this.shuffled[0];
            }
            return -1;
        }

        public ShuffleOrder cloneAndInsert(int insertionIndex, int insertionCount) {
            int[] insertionPoints = new int[insertionCount];
            int[] insertionValues = new int[insertionCount];
            for (int i = 0; i < insertionCount; i++) {
                insertionPoints[i] = this.random.nextInt(this.shuffled.length + 1);
                int swapIndex = this.random.nextInt(i + 1);
                insertionValues[i] = insertionValues[swapIndex];
                insertionValues[swapIndex] = i + insertionIndex;
            }
            Arrays.sort(insertionPoints);
            int[] newShuffled = new int[(this.shuffled.length + insertionCount)];
            int indexInOldShuffled = 0;
            int indexInInsertionList = 0;
            for (int i2 = 0; i2 < this.shuffled.length + insertionCount; i2++) {
                if (indexInInsertionList >= insertionCount || indexInOldShuffled != insertionPoints[indexInInsertionList]) {
                    int indexInOldShuffled2 = indexInOldShuffled + 1;
                    newShuffled[i2] = this.shuffled[indexInOldShuffled];
                    if (newShuffled[i2] >= insertionIndex) {
                        newShuffled[i2] = newShuffled[i2] + insertionCount;
                    }
                    indexInOldShuffled = indexInOldShuffled2;
                } else {
                    newShuffled[i2] = insertionValues[indexInInsertionList];
                    indexInInsertionList++;
                }
            }
            return new DefaultShuffleOrder(newShuffled, new Random(this.random.nextLong()));
        }

        public ShuffleOrder cloneAndRemove(int removalIndex) {
            int[] newShuffled = new int[(this.shuffled.length - 1)];
            boolean foundRemovedElement = false;
            for (int i = 0; i < this.shuffled.length; i++) {
                if (this.shuffled[i] == removalIndex) {
                    foundRemovedElement = true;
                } else {
                    newShuffled[foundRemovedElement ? i - 1 : i] = this.shuffled[i] > removalIndex ? this.shuffled[i] - 1 : this.shuffled[i];
                }
            }
            return new DefaultShuffleOrder(newShuffled, new Random(this.random.nextLong()));
        }

        private static int[] createShuffledList(int length, Random random2) {
            int[] shuffled2 = new int[length];
            for (int i = 0; i < length; i++) {
                int swapIndex = random2.nextInt(i + 1);
                shuffled2[i] = shuffled2[swapIndex];
                shuffled2[swapIndex] = i;
            }
            return shuffled2;
        }
    }

    public static final class UnshuffledShuffleOrder implements ShuffleOrder {
        private final int length;

        public UnshuffledShuffleOrder(int length2) {
            this.length = length2;
        }

        public int getLength() {
            return this.length;
        }

        public int getNextIndex(int index) {
            int index2 = index + 1;
            if (index2 < this.length) {
                return index2;
            }
            return -1;
        }

        public int getPreviousIndex(int index) {
            int index2 = index - 1;
            if (index2 >= 0) {
                return index2;
            }
            return -1;
        }

        public int getLastIndex() {
            if (this.length > 0) {
                return this.length - 1;
            }
            return -1;
        }

        public int getFirstIndex() {
            return this.length > 0 ? 0 : -1;
        }

        public ShuffleOrder cloneAndInsert(int insertionIndex, int insertionCount) {
            return new UnshuffledShuffleOrder(this.length + insertionCount);
        }

        public ShuffleOrder cloneAndRemove(int removalIndex) {
            return new UnshuffledShuffleOrder(this.length - 1);
        }
    }
}
