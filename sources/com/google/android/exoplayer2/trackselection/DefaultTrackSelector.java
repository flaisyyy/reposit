package com.google.android.exoplayer2.trackselection;

import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultTrackSelector extends MappingTrackSelector {
    private static final float FRACTION_TO_CONSIDER_FULLSCREEN = 0.98f;
    private static final int[] NO_TRACKS = new int[0];
    private static final int WITHIN_RENDERER_CAPABILITIES_BONUS = 1000;
    private final TrackSelection.Factory adaptiveTrackSelectionFactory;
    private final AtomicReference<Parameters> paramsReference;

    public static final class Parameters {
        public final boolean allowMixedMimeAdaptiveness;
        public final boolean allowNonSeamlessAdaptiveness;
        public final boolean exceedRendererCapabilitiesIfNecessary;
        public final boolean exceedVideoConstraintsIfNecessary;
        public final boolean forceLowestBitrate;
        public final int maxVideoBitrate;
        public final int maxVideoHeight;
        public final int maxVideoWidth;
        public final String preferredAudioLanguage;
        public final String preferredTextLanguage;
        public final boolean selectUndeterminedTextLanguage;
        public final int viewportHeight;
        public final boolean viewportOrientationMayChange;
        public final int viewportWidth;

        public Parameters() {
            this((String) null, (String) null, false, false, false, true, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        }

        public Parameters(String preferredAudioLanguage2, String preferredTextLanguage2, boolean selectUndeterminedTextLanguage2, boolean forceLowestBitrate2, boolean allowMixedMimeAdaptiveness2, boolean allowNonSeamlessAdaptiveness2, int maxVideoWidth2, int maxVideoHeight2, int maxVideoBitrate2, boolean exceedVideoConstraintsIfNecessary2, boolean exceedRendererCapabilitiesIfNecessary2, int viewportWidth2, int viewportHeight2, boolean viewportOrientationMayChange2) {
            this.preferredAudioLanguage = preferredAudioLanguage2;
            this.preferredTextLanguage = preferredTextLanguage2;
            this.selectUndeterminedTextLanguage = selectUndeterminedTextLanguage2;
            this.forceLowestBitrate = forceLowestBitrate2;
            this.allowMixedMimeAdaptiveness = allowMixedMimeAdaptiveness2;
            this.allowNonSeamlessAdaptiveness = allowNonSeamlessAdaptiveness2;
            this.maxVideoWidth = maxVideoWidth2;
            this.maxVideoHeight = maxVideoHeight2;
            this.maxVideoBitrate = maxVideoBitrate2;
            this.exceedVideoConstraintsIfNecessary = exceedVideoConstraintsIfNecessary2;
            this.exceedRendererCapabilitiesIfNecessary = exceedRendererCapabilitiesIfNecessary2;
            this.viewportWidth = viewportWidth2;
            this.viewportHeight = viewportHeight2;
            this.viewportOrientationMayChange = viewportOrientationMayChange2;
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withPreferredAudioLanguage(String preferredAudioLanguage2) {
            String preferredAudioLanguage3 = Util.normalizeLanguageCode(preferredAudioLanguage2);
            if (TextUtils.equals(preferredAudioLanguage3, this.preferredAudioLanguage)) {
                return this;
            }
            return new Parameters(preferredAudioLanguage3, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withPreferredTextLanguage(String preferredTextLanguage2) {
            String preferredTextLanguage3 = Util.normalizeLanguageCode(preferredTextLanguage2);
            if (TextUtils.equals(preferredTextLanguage3, this.preferredTextLanguage)) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, preferredTextLanguage3, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withSelectUndeterminedTextLanguage(boolean selectUndeterminedTextLanguage2) {
            if (selectUndeterminedTextLanguage2 == this.selectUndeterminedTextLanguage) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, selectUndeterminedTextLanguage2, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withForceLowestBitrate(boolean forceLowestBitrate2) {
            if (forceLowestBitrate2 == this.forceLowestBitrate) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, forceLowestBitrate2, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withAllowMixedMimeAdaptiveness(boolean allowMixedMimeAdaptiveness2) {
            if (allowMixedMimeAdaptiveness2 == this.allowMixedMimeAdaptiveness) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, allowMixedMimeAdaptiveness2, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withAllowNonSeamlessAdaptiveness(boolean allowNonSeamlessAdaptiveness2) {
            if (allowNonSeamlessAdaptiveness2 == this.allowNonSeamlessAdaptiveness) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, allowNonSeamlessAdaptiveness2, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withMaxVideoSize(int maxVideoWidth2, int maxVideoHeight2) {
            if (maxVideoWidth2 == this.maxVideoWidth && maxVideoHeight2 == this.maxVideoHeight) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, maxVideoWidth2, maxVideoHeight2, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withMaxVideoBitrate(int maxVideoBitrate2) {
            if (maxVideoBitrate2 == this.maxVideoBitrate) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, maxVideoBitrate2, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        public Parameters withMaxVideoSizeSd() {
            return withMaxVideoSize(1279, 719);
        }

        public Parameters withoutVideoSizeConstraints() {
            return withMaxVideoSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withExceedVideoConstraintsIfNecessary(boolean exceedVideoConstraintsIfNecessary2) {
            if (exceedVideoConstraintsIfNecessary2 == this.exceedVideoConstraintsIfNecessary) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, exceedVideoConstraintsIfNecessary2, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withExceedRendererCapabilitiesIfNecessary(boolean exceedRendererCapabilitiesIfNecessary2) {
            if (exceedRendererCapabilitiesIfNecessary2 == this.exceedRendererCapabilitiesIfNecessary) {
                return this;
            }
            return new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, exceedRendererCapabilitiesIfNecessary2, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange);
        }

        /* Debug info: failed to restart local var, previous not found, register: 16 */
        public Parameters withViewportSize(int viewportWidth2, int viewportHeight2, boolean viewportOrientationMayChange2) {
            return (viewportWidth2 == this.viewportWidth && viewportHeight2 == this.viewportHeight && viewportOrientationMayChange2 == this.viewportOrientationMayChange) ? this : new Parameters(this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.forceLowestBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, viewportWidth2, viewportHeight2, viewportOrientationMayChange2);
        }

        public Parameters withViewportSizeFromContext(Context context, boolean viewportOrientationMayChange2) {
            Point viewportSize = Util.getPhysicalDisplaySize(context);
            return withViewportSize(viewportSize.x, viewportSize.y, viewportOrientationMayChange2);
        }

        public Parameters withoutViewportSizeConstraints() {
            return withViewportSize(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Parameters other = (Parameters) obj;
            if (this.forceLowestBitrate == other.forceLowestBitrate && this.allowMixedMimeAdaptiveness == other.allowMixedMimeAdaptiveness && this.allowNonSeamlessAdaptiveness == other.allowNonSeamlessAdaptiveness && this.maxVideoWidth == other.maxVideoWidth && this.maxVideoHeight == other.maxVideoHeight && this.exceedVideoConstraintsIfNecessary == other.exceedVideoConstraintsIfNecessary && this.exceedRendererCapabilitiesIfNecessary == other.exceedRendererCapabilitiesIfNecessary && this.viewportOrientationMayChange == other.viewportOrientationMayChange && this.viewportWidth == other.viewportWidth && this.viewportHeight == other.viewportHeight && this.maxVideoBitrate == other.maxVideoBitrate && TextUtils.equals(this.preferredAudioLanguage, other.preferredAudioLanguage) && TextUtils.equals(this.preferredTextLanguage, other.preferredTextLanguage)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int i;
            int i2;
            int i3;
            int i4;
            int i5 = 1;
            int hashCode = ((((this.preferredAudioLanguage.hashCode() * 31) + this.preferredTextLanguage.hashCode()) * 31) + (this.forceLowestBitrate ? 1 : 0)) * 31;
            if (this.allowMixedMimeAdaptiveness) {
                i = 1;
            } else {
                i = 0;
            }
            int i6 = (hashCode + i) * 31;
            if (this.allowNonSeamlessAdaptiveness) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            int i7 = (((((((i6 + i2) * 31) + this.maxVideoWidth) * 31) + this.maxVideoHeight) * 31) + this.maxVideoBitrate) * 31;
            if (this.exceedVideoConstraintsIfNecessary) {
                i3 = 1;
            } else {
                i3 = 0;
            }
            int i8 = (i7 + i3) * 31;
            if (this.exceedRendererCapabilitiesIfNecessary) {
                i4 = 1;
            } else {
                i4 = 0;
            }
            int i9 = (i8 + i4) * 31;
            if (!this.viewportOrientationMayChange) {
                i5 = 0;
            }
            return ((((i9 + i5) * 31) + this.viewportWidth) * 31) + this.viewportHeight;
        }
    }

    public DefaultTrackSelector() {
        this((TrackSelection.Factory) null);
    }

    public DefaultTrackSelector(BandwidthMeter bandwidthMeter) {
        this((TrackSelection.Factory) new AdaptiveTrackSelection.Factory(bandwidthMeter));
    }

    public DefaultTrackSelector(TrackSelection.Factory adaptiveTrackSelectionFactory2) {
        this.adaptiveTrackSelectionFactory = adaptiveTrackSelectionFactory2;
        this.paramsReference = new AtomicReference<>(new Parameters());
    }

    public void setParameters(Parameters params) {
        Assertions.checkNotNull(params);
        if (!this.paramsReference.getAndSet(params).equals(params)) {
            invalidate();
        }
    }

    public Parameters getParameters() {
        return this.paramsReference.get();
    }

    /* access modifiers changed from: protected */
    public TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilities, TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports) throws ExoPlaybackException {
        boolean z;
        int rendererCount = rendererCapabilities.length;
        TrackSelection[] rendererTrackSelections = new TrackSelection[rendererCount];
        Parameters params = this.paramsReference.get();
        boolean seenVideoRendererWithMappedTracks = false;
        boolean selectedVideoTracks = false;
        for (int i = 0; i < rendererCount; i++) {
            if (2 == rendererCapabilities[i].getTrackType()) {
                if (!selectedVideoTracks) {
                    rendererTrackSelections[i] = selectVideoTrack(rendererCapabilities[i], rendererTrackGroupArrays[i], rendererFormatSupports[i], params, this.adaptiveTrackSelectionFactory);
                    selectedVideoTracks = rendererTrackSelections[i] != null;
                }
                if (rendererTrackGroupArrays[i].length > 0) {
                    z = true;
                } else {
                    z = false;
                }
                seenVideoRendererWithMappedTracks |= z;
            }
        }
        boolean selectedAudioTracks = false;
        boolean selectedTextTracks = false;
        for (int i2 = 0; i2 < rendererCount; i2++) {
            switch (rendererCapabilities[i2].getTrackType()) {
                case 1:
                    if (selectedAudioTracks) {
                        break;
                    } else {
                        rendererTrackSelections[i2] = selectAudioTrack(rendererTrackGroupArrays[i2], rendererFormatSupports[i2], params, seenVideoRendererWithMappedTracks ? null : this.adaptiveTrackSelectionFactory);
                        if (rendererTrackSelections[i2] == null) {
                            selectedAudioTracks = false;
                            break;
                        } else {
                            selectedAudioTracks = true;
                            break;
                        }
                    }
                case 2:
                    break;
                case 3:
                    if (selectedTextTracks) {
                        break;
                    } else {
                        rendererTrackSelections[i2] = selectTextTrack(rendererTrackGroupArrays[i2], rendererFormatSupports[i2], params);
                        if (rendererTrackSelections[i2] == null) {
                            selectedTextTracks = false;
                            break;
                        } else {
                            selectedTextTracks = true;
                            break;
                        }
                    }
                default:
                    rendererTrackSelections[i2] = selectOtherTrack(rendererCapabilities[i2].getTrackType(), rendererTrackGroupArrays[i2], rendererFormatSupports[i2], params);
                    break;
            }
        }
        return rendererTrackSelections;
    }

    /* access modifiers changed from: protected */
    public TrackSelection selectVideoTrack(RendererCapabilities rendererCapabilities, TrackGroupArray groups, int[][] formatSupport, Parameters params, TrackSelection.Factory adaptiveTrackSelectionFactory2) throws ExoPlaybackException {
        TrackSelection selection = null;
        if (!params.forceLowestBitrate && adaptiveTrackSelectionFactory2 != null) {
            selection = selectAdaptiveVideoTrack(rendererCapabilities, groups, formatSupport, params, adaptiveTrackSelectionFactory2);
        }
        if (selection == null) {
            return selectFixedVideoTrack(groups, formatSupport, params);
        }
        return selection;
    }

    private static TrackSelection selectAdaptiveVideoTrack(RendererCapabilities rendererCapabilities, TrackGroupArray groups, int[][] formatSupport, Parameters params, TrackSelection.Factory adaptiveTrackSelectionFactory2) throws ExoPlaybackException {
        int requiredAdaptiveSupport = params.allowNonSeamlessAdaptiveness ? 24 : 16;
        boolean allowMixedMimeTypes = params.allowMixedMimeAdaptiveness && (rendererCapabilities.supportsMixedMimeTypeAdaptation() & requiredAdaptiveSupport) != 0;
        for (int i = 0; i < groups.length; i++) {
            TrackGroup group = groups.get(i);
            int[] adaptiveTracks = getAdaptiveVideoTracksForGroup(group, formatSupport[i], allowMixedMimeTypes, requiredAdaptiveSupport, params.maxVideoWidth, params.maxVideoHeight, params.maxVideoBitrate, params.viewportWidth, params.viewportHeight, params.viewportOrientationMayChange);
            if (adaptiveTracks.length > 0) {
                return adaptiveTrackSelectionFactory2.createTrackSelection(group, adaptiveTracks);
            }
        }
        return null;
    }

    private static int[] getAdaptiveVideoTracksForGroup(TrackGroup group, int[] formatSupport, boolean allowMixedMimeTypes, int requiredAdaptiveSupport, int maxVideoWidth, int maxVideoHeight, int maxVideoBitrate, int viewportWidth, int viewportHeight, boolean viewportOrientationMayChange) {
        if (group.length < 2) {
            return NO_TRACKS;
        }
        List<Integer> selectedTrackIndices = getViewportFilteredTrackIndices(group, viewportWidth, viewportHeight, viewportOrientationMayChange);
        if (selectedTrackIndices.size() < 2) {
            return NO_TRACKS;
        }
        String selectedMimeType = null;
        if (!allowMixedMimeTypes) {
            HashSet<String> seenMimeTypes = new HashSet<>();
            int selectedMimeTypeTrackCount = 0;
            for (int i = 0; i < selectedTrackIndices.size(); i++) {
                String sampleMimeType = group.getFormat(selectedTrackIndices.get(i).intValue()).sampleMimeType;
                if (seenMimeTypes.add(sampleMimeType)) {
                    int countForMimeType = getAdaptiveVideoTrackCountForMimeType(group, formatSupport, requiredAdaptiveSupport, sampleMimeType, maxVideoWidth, maxVideoHeight, maxVideoBitrate, selectedTrackIndices);
                    if (countForMimeType > selectedMimeTypeTrackCount) {
                        selectedMimeType = sampleMimeType;
                        selectedMimeTypeTrackCount = countForMimeType;
                    }
                }
            }
        }
        filterAdaptiveVideoTrackCountForMimeType(group, formatSupport, requiredAdaptiveSupport, selectedMimeType, maxVideoWidth, maxVideoHeight, maxVideoBitrate, selectedTrackIndices);
        return selectedTrackIndices.size() < 2 ? NO_TRACKS : Util.toArray(selectedTrackIndices);
    }

    private static int getAdaptiveVideoTrackCountForMimeType(TrackGroup group, int[] formatSupport, int requiredAdaptiveSupport, String mimeType, int maxVideoWidth, int maxVideoHeight, int maxVideoBitrate, List<Integer> selectedTrackIndices) {
        int adaptiveTrackCount = 0;
        for (int i = 0; i < selectedTrackIndices.size(); i++) {
            int trackIndex = selectedTrackIndices.get(i).intValue();
            if (isSupportedAdaptiveVideoTrack(group.getFormat(trackIndex), mimeType, formatSupport[trackIndex], requiredAdaptiveSupport, maxVideoWidth, maxVideoHeight, maxVideoBitrate)) {
                adaptiveTrackCount++;
            }
        }
        return adaptiveTrackCount;
    }

    private static void filterAdaptiveVideoTrackCountForMimeType(TrackGroup group, int[] formatSupport, int requiredAdaptiveSupport, String mimeType, int maxVideoWidth, int maxVideoHeight, int maxVideoBitrate, List<Integer> selectedTrackIndices) {
        for (int i = selectedTrackIndices.size() - 1; i >= 0; i--) {
            int trackIndex = selectedTrackIndices.get(i).intValue();
            if (!isSupportedAdaptiveVideoTrack(group.getFormat(trackIndex), mimeType, formatSupport[trackIndex], requiredAdaptiveSupport, maxVideoWidth, maxVideoHeight, maxVideoBitrate)) {
                selectedTrackIndices.remove(i);
            }
        }
    }

    private static boolean isSupportedAdaptiveVideoTrack(Format format, String mimeType, int formatSupport, int requiredAdaptiveSupport, int maxVideoWidth, int maxVideoHeight, int maxVideoBitrate) {
        if (!isSupported(formatSupport, false) || (formatSupport & requiredAdaptiveSupport) == 0) {
            return false;
        }
        if (mimeType != null && !Util.areEqual(format.sampleMimeType, mimeType)) {
            return false;
        }
        if (format.width != -1 && format.width > maxVideoWidth) {
            return false;
        }
        if (format.height != -1 && format.height > maxVideoHeight) {
            return false;
        }
        if (format.bitrate == -1 || format.bitrate <= maxVideoBitrate) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x006f, code lost:
        r21 = r5.width;
        r22 = r26.maxVideoWidth;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x008b, code lost:
        r21 = r5.height;
        r22 = r26.maxVideoHeight;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x00a7, code lost:
        r21 = r5.bitrate;
        r22 = r26.maxVideoBitrate;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static com.google.android.exoplayer2.trackselection.TrackSelection selectFixedVideoTrack(com.google.android.exoplayer2.source.TrackGroupArray r24, int[][] r25, com.google.android.exoplayer2.trackselection.DefaultTrackSelector.Parameters r26) {
        /*
            r12 = 0
            r14 = 0
            r16 = 0
            r11 = -1
            r13 = -1
            r7 = 0
        L_0x0007:
            r0 = r24
            int r0 = r0.length
            r21 = r0
            r0 = r21
            if (r7 >= r0) goto L_0x013d
            r0 = r24
            com.google.android.exoplayer2.source.TrackGroup r18 = r0.get(r7)
            r0 = r26
            int r0 = r0.viewportWidth
            r21 = r0
            r0 = r26
            int r0 = r0.viewportHeight
            r22 = r0
            r0 = r26
            boolean r0 = r0.viewportOrientationMayChange
            r23 = r0
            r0 = r18
            r1 = r21
            r2 = r22
            r3 = r23
            java.util.List r15 = getViewportFilteredTrackIndices(r0, r1, r2, r3)
            r17 = r25[r7]
            r19 = 0
        L_0x0039:
            r0 = r18
            int r0 = r0.length
            r21 = r0
            r0 = r19
            r1 = r21
            if (r0 >= r1) goto L_0x0139
            r21 = r17[r19]
            r0 = r26
            boolean r0 = r0.exceedRendererCapabilitiesIfNecessary
            r22 = r0
            boolean r21 = isSupported(r21, r22)
            if (r21 == 0) goto L_0x00c2
            com.google.android.exoplayer2.Format r5 = r18.getFormat(r19)
            java.lang.Integer r21 = java.lang.Integer.valueOf(r19)
            r0 = r21
            boolean r21 = r15.contains(r0)
            if (r21 == 0) goto L_0x00c6
            int r0 = r5.width
            r21 = r0
            r22 = -1
            r0 = r21
            r1 = r22
            if (r0 == r1) goto L_0x007f
            int r0 = r5.width
            r21 = r0
            r0 = r26
            int r0 = r0.maxVideoWidth
            r22 = r0
            r0 = r21
            r1 = r22
            if (r0 > r1) goto L_0x00c6
        L_0x007f:
            int r0 = r5.height
            r21 = r0
            r22 = -1
            r0 = r21
            r1 = r22
            if (r0 == r1) goto L_0x009b
            int r0 = r5.height
            r21 = r0
            r0 = r26
            int r0 = r0.maxVideoHeight
            r22 = r0
            r0 = r21
            r1 = r22
            if (r0 > r1) goto L_0x00c6
        L_0x009b:
            int r0 = r5.bitrate
            r21 = r0
            r22 = -1
            r0 = r21
            r1 = r22
            if (r0 == r1) goto L_0x00b7
            int r0 = r5.bitrate
            r21 = r0
            r0 = r26
            int r0 = r0.maxVideoBitrate
            r22 = r0
            r0 = r21
            r1 = r22
            if (r0 > r1) goto L_0x00c6
        L_0x00b7:
            r9 = 1
        L_0x00b8:
            if (r9 != 0) goto L_0x00c8
            r0 = r26
            boolean r0 = r0.exceedVideoConstraintsIfNecessary
            r21 = r0
            if (r21 != 0) goto L_0x00c8
        L_0x00c2:
            int r19 = r19 + 1
            goto L_0x0039
        L_0x00c6:
            r9 = 0
            goto L_0x00b8
        L_0x00c8:
            if (r9 == 0) goto L_0x010d
            r20 = 2
        L_0x00cc:
            r21 = r17[r19]
            r22 = 0
            boolean r8 = isSupported(r21, r22)
            if (r8 == 0) goto L_0x00dc
            r0 = r20
            int r0 = r0 + 1000
            r20 = r0
        L_0x00dc:
            r0 = r20
            r1 = r16
            if (r0 <= r1) goto L_0x0110
            r10 = 1
        L_0x00e3:
            r0 = r20
            r1 = r16
            if (r0 != r1) goto L_0x00fe
            r0 = r26
            boolean r0 = r0.forceLowestBitrate
            r21 = r0
            if (r21 == 0) goto L_0x0114
            int r0 = r5.bitrate
            r21 = r0
            r0 = r21
            int r21 = compareFormatValues(r0, r11)
            if (r21 >= 0) goto L_0x0112
            r10 = 1
        L_0x00fe:
            if (r10 == 0) goto L_0x00c2
            r12 = r18
            r14 = r19
            r16 = r20
            int r11 = r5.bitrate
            int r13 = r5.getPixelCount()
            goto L_0x00c2
        L_0x010d:
            r20 = 1
            goto L_0x00cc
        L_0x0110:
            r10 = 0
            goto L_0x00e3
        L_0x0112:
            r10 = 0
            goto L_0x00fe
        L_0x0114:
            int r6 = r5.getPixelCount()
            if (r6 == r13) goto L_0x0126
            int r4 = compareFormatValues(r6, r13)
        L_0x011e:
            if (r8 == 0) goto L_0x0133
            if (r9 == 0) goto L_0x0133
            if (r4 <= 0) goto L_0x0131
            r10 = 1
        L_0x0125:
            goto L_0x00fe
        L_0x0126:
            int r0 = r5.bitrate
            r21 = r0
            r0 = r21
            int r4 = compareFormatValues(r0, r11)
            goto L_0x011e
        L_0x0131:
            r10 = 0
            goto L_0x0125
        L_0x0133:
            if (r4 >= 0) goto L_0x0137
            r10 = 1
            goto L_0x0125
        L_0x0137:
            r10 = 0
            goto L_0x0125
        L_0x0139:
            int r7 = r7 + 1
            goto L_0x0007
        L_0x013d:
            if (r12 != 0) goto L_0x0142
            r21 = 0
        L_0x0141:
            return r21
        L_0x0142:
            com.google.android.exoplayer2.trackselection.FixedTrackSelection r21 = new com.google.android.exoplayer2.trackselection.FixedTrackSelection
            r0 = r21
            r0.<init>(r12, r14)
            goto L_0x0141
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.trackselection.DefaultTrackSelector.selectFixedVideoTrack(com.google.android.exoplayer2.source.TrackGroupArray, int[][], com.google.android.exoplayer2.trackselection.DefaultTrackSelector$Parameters):com.google.android.exoplayer2.trackselection.TrackSelection");
    }

    private static int compareFormatValues(int first, int second) {
        if (first == -1) {
            return second == -1 ? 0 : -1;
        }
        if (second == -1) {
            return 1;
        }
        return first - second;
    }

    /* access modifiers changed from: protected */
    public TrackSelection selectAudioTrack(TrackGroupArray groups, int[][] formatSupport, Parameters params, TrackSelection.Factory adaptiveTrackSelectionFactory2) throws ExoPlaybackException {
        int selectedTrackIndex = -1;
        int selectedGroupIndex = -1;
        AudioTrackScore selectedTrackScore = null;
        for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            TrackGroup trackGroup = groups.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                if (isSupported(trackFormatSupport[trackIndex], params.exceedRendererCapabilitiesIfNecessary)) {
                    AudioTrackScore trackScore = new AudioTrackScore(trackGroup.getFormat(trackIndex), params, trackFormatSupport[trackIndex]);
                    if (selectedTrackScore == null || trackScore.compareTo(selectedTrackScore) > 0) {
                        selectedGroupIndex = groupIndex;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
                }
            }
        }
        if (selectedGroupIndex == -1) {
            return null;
        }
        TrackGroup selectedGroup = groups.get(selectedGroupIndex);
        if (!params.forceLowestBitrate && adaptiveTrackSelectionFactory2 != null) {
            int[] adaptiveTracks = getAdaptiveAudioTracks(selectedGroup, formatSupport[selectedGroupIndex], params.allowMixedMimeAdaptiveness);
            if (adaptiveTracks.length > 0) {
                return adaptiveTrackSelectionFactory2.createTrackSelection(selectedGroup, adaptiveTracks);
            }
        }
        return new FixedTrackSelection(selectedGroup, selectedTrackIndex);
    }

    private static int[] getAdaptiveAudioTracks(TrackGroup group, int[] formatSupport, boolean allowMixedMimeTypes) {
        int configurationCount;
        int selectedConfigurationTrackCount = 0;
        AudioConfigurationTuple selectedConfiguration = null;
        HashSet<AudioConfigurationTuple> seenConfigurationTuples = new HashSet<>();
        for (int i = 0; i < group.length; i++) {
            Format format = group.getFormat(i);
            AudioConfigurationTuple configuration = new AudioConfigurationTuple(format.channelCount, format.sampleRate, allowMixedMimeTypes ? null : format.sampleMimeType);
            if (seenConfigurationTuples.add(configuration) && (configurationCount = getAdaptiveAudioTrackCount(group, formatSupport, configuration)) > selectedConfigurationTrackCount) {
                selectedConfiguration = configuration;
                selectedConfigurationTrackCount = configurationCount;
            }
        }
        if (selectedConfigurationTrackCount <= 1) {
            return NO_TRACKS;
        }
        int[] adaptiveIndices = new int[selectedConfigurationTrackCount];
        int index = 0;
        for (int i2 = 0; i2 < group.length; i2++) {
            if (isSupportedAdaptiveAudioTrack(group.getFormat(i2), formatSupport[i2], selectedConfiguration)) {
                adaptiveIndices[index] = i2;
                index++;
            }
        }
        return adaptiveIndices;
    }

    private static int getAdaptiveAudioTrackCount(TrackGroup group, int[] formatSupport, AudioConfigurationTuple configuration) {
        int count = 0;
        for (int i = 0; i < group.length; i++) {
            if (isSupportedAdaptiveAudioTrack(group.getFormat(i), formatSupport[i], configuration)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isSupportedAdaptiveAudioTrack(Format format, int formatSupport, AudioConfigurationTuple configuration) {
        if (!isSupported(formatSupport, false) || format.channelCount != configuration.channelCount || format.sampleRate != configuration.sampleRate) {
            return false;
        }
        if (configuration.mimeType == null || TextUtils.equals(configuration.mimeType, format.sampleMimeType)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public TrackSelection selectTextTrack(TrackGroupArray groups, int[][] formatSupport, Parameters params) throws ExoPlaybackException {
        int trackScore;
        int trackScore2;
        int i;
        TrackGroup selectedGroup = null;
        int selectedTrackIndex = 0;
        int selectedTrackScore = 0;
        for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            TrackGroup trackGroup = groups.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                if (isSupported(trackFormatSupport[trackIndex], params.exceedRendererCapabilitiesIfNecessary)) {
                    Format format = trackGroup.getFormat(trackIndex);
                    boolean isDefault = (format.selectionFlags & 1) != 0;
                    boolean isForced = (format.selectionFlags & 2) != 0;
                    boolean preferredLanguageFound = formatHasLanguage(format, params.preferredTextLanguage);
                    if (preferredLanguageFound || (params.selectUndeterminedTextLanguage && formatHasNoLanguage(format))) {
                        if (isDefault) {
                            trackScore2 = 8;
                        } else if (!isForced) {
                            trackScore2 = 6;
                        } else {
                            trackScore2 = 4;
                        }
                        if (preferredLanguageFound) {
                            i = 1;
                        } else {
                            i = 0;
                        }
                        trackScore = trackScore2 + i;
                    } else if (isDefault) {
                        trackScore = 3;
                    } else if (isForced) {
                        if (formatHasLanguage(format, params.preferredAudioLanguage)) {
                            trackScore = 2;
                        } else {
                            trackScore = 1;
                        }
                    }
                    if (isSupported(trackFormatSupport[trackIndex], false)) {
                        trackScore += 1000;
                    }
                    if (trackScore > selectedTrackScore) {
                        selectedGroup = trackGroup;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
                }
            }
        }
        if (selectedGroup == null) {
            return null;
        }
        return new FixedTrackSelection(selectedGroup, selectedTrackIndex);
    }

    /* access modifiers changed from: protected */
    public TrackSelection selectOtherTrack(int trackType, TrackGroupArray groups, int[][] formatSupport, Parameters params) throws ExoPlaybackException {
        TrackGroup selectedGroup = null;
        int selectedTrackIndex = 0;
        int selectedTrackScore = 0;
        for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            TrackGroup trackGroup = groups.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                if (isSupported(trackFormatSupport[trackIndex], params.exceedRendererCapabilitiesIfNecessary)) {
                    int trackScore = (trackGroup.getFormat(trackIndex).selectionFlags & 1) != 0 ? 2 : 1;
                    if (isSupported(trackFormatSupport[trackIndex], false)) {
                        trackScore += 1000;
                    }
                    if (trackScore > selectedTrackScore) {
                        selectedGroup = trackGroup;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
                }
            }
        }
        if (selectedGroup == null) {
            return null;
        }
        return new FixedTrackSelection(selectedGroup, selectedTrackIndex);
    }

    protected static boolean isSupported(int formatSupport, boolean allowExceedsCapabilities) {
        int maskedSupport = formatSupport & 7;
        return maskedSupport == 4 || (allowExceedsCapabilities && maskedSupport == 3);
    }

    protected static boolean formatHasNoLanguage(Format format) {
        return TextUtils.isEmpty(format.language) || formatHasLanguage(format, C.LANGUAGE_UNDETERMINED);
    }

    protected static boolean formatHasLanguage(Format format, String language) {
        return language != null && TextUtils.equals(language, Util.normalizeLanguageCode(format.language));
    }

    private static List<Integer> getViewportFilteredTrackIndices(TrackGroup group, int viewportWidth, int viewportHeight, boolean orientationMayChange) {
        ArrayList<Integer> selectedTrackIndices = new ArrayList<>(group.length);
        for (int i = 0; i < group.length; i++) {
            selectedTrackIndices.add(Integer.valueOf(i));
        }
        if (!(viewportWidth == Integer.MAX_VALUE || viewportHeight == Integer.MAX_VALUE)) {
            int maxVideoPixelsToRetain = Integer.MAX_VALUE;
            for (int i2 = 0; i2 < group.length; i2++) {
                Format format = group.getFormat(i2);
                if (format.width > 0 && format.height > 0) {
                    Point maxVideoSizeInViewport = getMaxVideoSizeInViewport(orientationMayChange, viewportWidth, viewportHeight, format.width, format.height);
                    int videoPixels = format.width * format.height;
                    if (format.width >= ((int) (((float) maxVideoSizeInViewport.x) * FRACTION_TO_CONSIDER_FULLSCREEN)) && format.height >= ((int) (((float) maxVideoSizeInViewport.y) * FRACTION_TO_CONSIDER_FULLSCREEN)) && videoPixels < maxVideoPixelsToRetain) {
                        maxVideoPixelsToRetain = videoPixels;
                    }
                }
            }
            if (maxVideoPixelsToRetain != Integer.MAX_VALUE) {
                for (int i3 = selectedTrackIndices.size() - 1; i3 >= 0; i3--) {
                    int pixelCount = group.getFormat(selectedTrackIndices.get(i3).intValue()).getPixelCount();
                    if (pixelCount == -1 || pixelCount > maxVideoPixelsToRetain) {
                        selectedTrackIndices.remove(i3);
                    }
                }
            }
        }
        return selectedTrackIndices;
    }

    private static Point getMaxVideoSizeInViewport(boolean orientationMayChange, int viewportWidth, int viewportHeight, int videoWidth, int videoHeight) {
        boolean z = true;
        if (orientationMayChange) {
            boolean z2 = videoWidth > videoHeight;
            if (viewportWidth <= viewportHeight) {
                z = false;
            }
            if (z2 != z) {
                int tempViewportWidth = viewportWidth;
                viewportWidth = viewportHeight;
                viewportHeight = tempViewportWidth;
            }
        }
        if (videoWidth * viewportHeight >= videoHeight * viewportWidth) {
            return new Point(viewportWidth, Util.ceilDivide(viewportWidth * videoHeight, videoWidth));
        }
        return new Point(Util.ceilDivide(viewportHeight * videoWidth, videoHeight), viewportHeight);
    }

    private static final class AudioTrackScore implements Comparable<AudioTrackScore> {
        private final int bitrate;
        private final int channelCount;
        private final int defaultSelectionFlagScore;
        private final int matchLanguageScore;
        private final Parameters parameters;
        private final int sampleRate;
        private final int withinRendererCapabilitiesScore;

        public AudioTrackScore(Format format, Parameters parameters2, int formatSupport) {
            int i;
            int i2 = 1;
            this.parameters = parameters2;
            this.withinRendererCapabilitiesScore = DefaultTrackSelector.isSupported(formatSupport, false) ? 1 : 0;
            if (DefaultTrackSelector.formatHasLanguage(format, parameters2.preferredAudioLanguage)) {
                i = 1;
            } else {
                i = 0;
            }
            this.matchLanguageScore = i;
            this.defaultSelectionFlagScore = (format.selectionFlags & 1) == 0 ? 0 : i2;
            this.channelCount = format.channelCount;
            this.sampleRate = format.sampleRate;
            this.bitrate = format.bitrate;
        }

        public int compareTo(AudioTrackScore other) {
            int resultSign = 1;
            if (this.withinRendererCapabilitiesScore != other.withinRendererCapabilitiesScore) {
                return DefaultTrackSelector.compareInts(this.withinRendererCapabilitiesScore, other.withinRendererCapabilitiesScore);
            }
            if (this.matchLanguageScore != other.matchLanguageScore) {
                return DefaultTrackSelector.compareInts(this.matchLanguageScore, other.matchLanguageScore);
            }
            if (this.defaultSelectionFlagScore != other.defaultSelectionFlagScore) {
                return DefaultTrackSelector.compareInts(this.defaultSelectionFlagScore, other.defaultSelectionFlagScore);
            }
            if (this.parameters.forceLowestBitrate) {
                return DefaultTrackSelector.compareInts(other.bitrate, this.bitrate);
            }
            if (this.withinRendererCapabilitiesScore != 1) {
                resultSign = -1;
            }
            if (this.channelCount != other.channelCount) {
                return DefaultTrackSelector.compareInts(this.channelCount, other.channelCount) * resultSign;
            }
            if (this.sampleRate != other.sampleRate) {
                return DefaultTrackSelector.compareInts(this.sampleRate, other.sampleRate) * resultSign;
            }
            return DefaultTrackSelector.compareInts(this.bitrate, other.bitrate) * resultSign;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AudioTrackScore that = (AudioTrackScore) o;
            if (this.withinRendererCapabilitiesScore == that.withinRendererCapabilitiesScore && this.matchLanguageScore == that.matchLanguageScore && this.defaultSelectionFlagScore == that.defaultSelectionFlagScore && this.channelCount == that.channelCount && this.sampleRate == that.sampleRate && this.bitrate == that.bitrate) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (((((((((this.withinRendererCapabilitiesScore * 31) + this.matchLanguageScore) * 31) + this.defaultSelectionFlagScore) * 31) + this.channelCount) * 31) + this.sampleRate) * 31) + this.bitrate;
        }
    }

    /* access modifiers changed from: private */
    public static int compareInts(int first, int second) {
        if (first > second) {
            return 1;
        }
        return second > first ? -1 : 0;
    }

    private static final class AudioConfigurationTuple {
        public final int channelCount;
        public final String mimeType;
        public final int sampleRate;

        public AudioConfigurationTuple(int channelCount2, int sampleRate2, String mimeType2) {
            this.channelCount = channelCount2;
            this.sampleRate = sampleRate2;
            this.mimeType = mimeType2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            AudioConfigurationTuple other = (AudioConfigurationTuple) obj;
            if (this.channelCount == other.channelCount && this.sampleRate == other.sampleRate && TextUtils.equals(this.mimeType, other.mimeType)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (((this.channelCount * 31) + this.sampleRate) * 31) + (this.mimeType != null ? this.mimeType.hashCode() : 0);
        }
    }
}
