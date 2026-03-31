package com.google.android.exoplayer2.source.ads;

import android.net.Uri;
import com.google.android.exoplayer2.C;
import java.util.Arrays;

public final class AdPlaybackState {
    public final int[] adCounts;
    public final int adGroupCount;
    public final long[] adGroupTimesUs;
    public long adResumePositionUs;
    public final Uri[][] adUris;
    public final int[] adsLoadedCounts;
    public final int[] adsPlayedCounts;
    public long contentDurationUs;

    public AdPlaybackState(long[] adGroupTimesUs2) {
        this.adGroupTimesUs = adGroupTimesUs2;
        this.adGroupCount = adGroupTimesUs2.length;
        this.adsPlayedCounts = new int[this.adGroupCount];
        this.adCounts = new int[this.adGroupCount];
        Arrays.fill(this.adCounts, -1);
        this.adUris = new Uri[this.adGroupCount][];
        Arrays.fill(this.adUris, new Uri[0]);
        this.adsLoadedCounts = new int[adGroupTimesUs2.length];
        this.contentDurationUs = C.TIME_UNSET;
    }

    private AdPlaybackState(long[] adGroupTimesUs2, int[] adCounts2, int[] adsLoadedCounts2, int[] adsPlayedCounts2, Uri[][] adUris2, long contentDurationUs2, long adResumePositionUs2) {
        this.adGroupTimesUs = adGroupTimesUs2;
        this.adCounts = adCounts2;
        this.adsLoadedCounts = adsLoadedCounts2;
        this.adsPlayedCounts = adsPlayedCounts2;
        this.adUris = adUris2;
        this.contentDurationUs = contentDurationUs2;
        this.adResumePositionUs = adResumePositionUs2;
        this.adGroupCount = adGroupTimesUs2.length;
    }

    public AdPlaybackState copy() {
        Uri[][] adUris2 = new Uri[this.adGroupTimesUs.length][];
        for (int i = 0; i < this.adUris.length; i++) {
            adUris2[i] = (Uri[]) Arrays.copyOf(this.adUris[i], this.adUris[i].length);
        }
        return new AdPlaybackState(Arrays.copyOf(this.adGroupTimesUs, this.adGroupCount), Arrays.copyOf(this.adCounts, this.adGroupCount), Arrays.copyOf(this.adsLoadedCounts, this.adGroupCount), Arrays.copyOf(this.adsPlayedCounts, this.adGroupCount), adUris2, this.contentDurationUs, this.adResumePositionUs);
    }

    public void setAdCount(int adGroupIndex, int adCount) {
        this.adCounts[adGroupIndex] = adCount;
    }

    public void addAdUri(int adGroupIndex, Uri uri) {
        int adIndexInAdGroup = this.adUris[adGroupIndex].length;
        this.adUris[adGroupIndex] = (Uri[]) Arrays.copyOf(this.adUris[adGroupIndex], adIndexInAdGroup + 1);
        this.adUris[adGroupIndex][adIndexInAdGroup] = uri;
        int[] iArr = this.adsLoadedCounts;
        iArr[adGroupIndex] = iArr[adGroupIndex] + 1;
    }

    public void playedAd(int adGroupIndex) {
        this.adResumePositionUs = 0;
        int[] iArr = this.adsPlayedCounts;
        iArr[adGroupIndex] = iArr[adGroupIndex] + 1;
    }

    public void playedAdGroup(int adGroupIndex) {
        this.adResumePositionUs = 0;
        if (this.adCounts[adGroupIndex] == -1) {
            this.adCounts[adGroupIndex] = 0;
        }
        this.adsPlayedCounts[adGroupIndex] = this.adCounts[adGroupIndex];
    }

    public void setAdResumePositionUs(long adResumePositionUs2) {
        this.adResumePositionUs = adResumePositionUs2;
    }
}
