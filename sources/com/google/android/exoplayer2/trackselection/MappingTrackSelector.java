package com.google.android.exoplayer2.trackselection;

import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.RendererConfiguration;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class MappingTrackSelector extends TrackSelector {
    private MappedTrackInfo currentMappedTrackInfo;
    private final SparseBooleanArray rendererDisabledFlags = new SparseBooleanArray();
    private final SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides = new SparseArray<>();
    private int tunnelingAudioSessionId = 0;

    /* access modifiers changed from: protected */
    public abstract TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilitiesArr, TrackGroupArray[] trackGroupArrayArr, int[][][] iArr) throws ExoPlaybackException;

    public static final class MappedTrackInfo {
        public static final int RENDERER_SUPPORT_EXCEEDS_CAPABILITIES_TRACKS = 2;
        public static final int RENDERER_SUPPORT_NO_TRACKS = 0;
        public static final int RENDERER_SUPPORT_PLAYABLE_TRACKS = 3;
        public static final int RENDERER_SUPPORT_UNSUPPORTED_TRACKS = 1;
        private final int[][][] formatSupport;
        public final int length;
        private final int[] mixedMimeTypeAdaptiveSupport;
        private final int[] rendererTrackTypes;
        private final TrackGroupArray[] trackGroups;
        private final TrackGroupArray unassociatedTrackGroups;

        MappedTrackInfo(int[] rendererTrackTypes2, TrackGroupArray[] trackGroups2, int[] mixedMimeTypeAdaptiveSupport2, int[][][] formatSupport2, TrackGroupArray unassociatedTrackGroups2) {
            this.rendererTrackTypes = rendererTrackTypes2;
            this.trackGroups = trackGroups2;
            this.formatSupport = formatSupport2;
            this.mixedMimeTypeAdaptiveSupport = mixedMimeTypeAdaptiveSupport2;
            this.unassociatedTrackGroups = unassociatedTrackGroups2;
            this.length = trackGroups2.length;
        }

        public TrackGroupArray getTrackGroups(int rendererIndex) {
            return this.trackGroups[rendererIndex];
        }

        public int getRendererSupport(int rendererIndex) {
            int trackRendererSupport;
            int bestRendererSupport = 0;
            int[][] rendererFormatSupport = this.formatSupport[rendererIndex];
            for (int i = 0; i < rendererFormatSupport.length; i++) {
                for (int i2 : rendererFormatSupport[i]) {
                    switch (i2 & 7) {
                        case 3:
                            trackRendererSupport = 2;
                            break;
                        case 4:
                            return 3;
                        default:
                            trackRendererSupport = 1;
                            break;
                    }
                    bestRendererSupport = Math.max(bestRendererSupport, trackRendererSupport);
                }
            }
            return bestRendererSupport;
        }

        public int getTrackTypeRendererSupport(int trackType) {
            int bestRendererSupport = 0;
            for (int i = 0; i < this.length; i++) {
                if (this.rendererTrackTypes[i] == trackType) {
                    bestRendererSupport = Math.max(bestRendererSupport, getRendererSupport(i));
                }
            }
            return bestRendererSupport;
        }

        public int getTrackFormatSupport(int rendererIndex, int groupIndex, int trackIndex) {
            return this.formatSupport[rendererIndex][groupIndex][trackIndex] & 7;
        }

        public int getAdaptiveSupport(int rendererIndex, int groupIndex, boolean includeCapabilitiesExceededTracks) {
            int trackIndexCount;
            int trackCount = this.trackGroups[rendererIndex].get(groupIndex).length;
            int[] trackIndices = new int[trackCount];
            int i = 0;
            int trackIndexCount2 = 0;
            while (i < trackCount) {
                int fixedSupport = getTrackFormatSupport(rendererIndex, groupIndex, i);
                if (fixedSupport == 4 || (includeCapabilitiesExceededTracks && fixedSupport == 3)) {
                    trackIndexCount = trackIndexCount2 + 1;
                    trackIndices[trackIndexCount2] = i;
                } else {
                    trackIndexCount = trackIndexCount2;
                }
                i++;
                trackIndexCount2 = trackIndexCount;
            }
            return getAdaptiveSupport(rendererIndex, groupIndex, Arrays.copyOf(trackIndices, trackIndexCount2));
        }

        public int getAdaptiveSupport(int rendererIndex, int groupIndex, int[] trackIndices) {
            int handledTrackCount = 0;
            int adaptiveSupport = 16;
            boolean multipleMimeTypes = false;
            String firstSampleMimeType = null;
            int i = 0;
            while (i < trackIndices.length) {
                String sampleMimeType = this.trackGroups[rendererIndex].get(groupIndex).getFormat(trackIndices[i]).sampleMimeType;
                int handledTrackCount2 = handledTrackCount + 1;
                if (handledTrackCount == 0) {
                    firstSampleMimeType = sampleMimeType;
                } else {
                    multipleMimeTypes |= !Util.areEqual(firstSampleMimeType, sampleMimeType);
                }
                adaptiveSupport = Math.min(adaptiveSupport, this.formatSupport[rendererIndex][groupIndex][i] & 24);
                i++;
                handledTrackCount = handledTrackCount2;
            }
            if (multipleMimeTypes) {
                return Math.min(adaptiveSupport, this.mixedMimeTypeAdaptiveSupport[rendererIndex]);
            }
            return adaptiveSupport;
        }

        public TrackGroupArray getUnassociatedTrackGroups() {
            return this.unassociatedTrackGroups;
        }
    }

    public static final class SelectionOverride {
        public final TrackSelection.Factory factory;
        public final int groupIndex;
        public final int length;
        public final int[] tracks;

        public SelectionOverride(TrackSelection.Factory factory2, int groupIndex2, int... tracks2) {
            this.factory = factory2;
            this.groupIndex = groupIndex2;
            this.tracks = tracks2;
            this.length = tracks2.length;
        }

        public TrackSelection createTrackSelection(TrackGroupArray groups) {
            return this.factory.createTrackSelection(groups.get(this.groupIndex), this.tracks);
        }

        public boolean containsTrack(int track) {
            for (int overrideTrack : this.tracks) {
                if (overrideTrack == track) {
                    return true;
                }
            }
            return false;
        }
    }

    public final MappedTrackInfo getCurrentMappedTrackInfo() {
        return this.currentMappedTrackInfo;
    }

    public final void setRendererDisabled(int rendererIndex, boolean disabled) {
        if (this.rendererDisabledFlags.get(rendererIndex) != disabled) {
            this.rendererDisabledFlags.put(rendererIndex, disabled);
            invalidate();
        }
    }

    public final boolean getRendererDisabled(int rendererIndex) {
        return this.rendererDisabledFlags.get(rendererIndex);
    }

    public final void setSelectionOverride(int rendererIndex, TrackGroupArray groups, SelectionOverride override) {
        Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
        if (overrides == null) {
            overrides = new HashMap<>();
            this.selectionOverrides.put(rendererIndex, overrides);
        }
        if (!overrides.containsKey(groups) || !Util.areEqual(overrides.get(groups), override)) {
            overrides.put(groups, override);
            invalidate();
        }
    }

    public final boolean hasSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
        return overrides != null && overrides.containsKey(groups);
    }

    public final SelectionOverride getSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
        if (overrides != null) {
            return overrides.get(groups);
        }
        return null;
    }

    public final void clearSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
        if (overrides != null && overrides.containsKey(groups)) {
            overrides.remove(groups);
            if (overrides.isEmpty()) {
                this.selectionOverrides.remove(rendererIndex);
            }
            invalidate();
        }
    }

    public final void clearSelectionOverrides(int rendererIndex) {
        Map<TrackGroupArray, ?> overrides = this.selectionOverrides.get(rendererIndex);
        if (overrides != null && !overrides.isEmpty()) {
            this.selectionOverrides.remove(rendererIndex);
            invalidate();
        }
    }

    public final void clearSelectionOverrides() {
        if (this.selectionOverrides.size() != 0) {
            this.selectionOverrides.clear();
            invalidate();
        }
    }

    public void setTunnelingAudioSessionId(int tunnelingAudioSessionId2) {
        if (this.tunnelingAudioSessionId != tunnelingAudioSessionId2) {
            this.tunnelingAudioSessionId = tunnelingAudioSessionId2;
            invalidate();
        }
    }

    public final TrackSelectorResult selectTracks(RendererCapabilities[] rendererCapabilities, TrackGroupArray trackGroups) throws ExoPlaybackException {
        TrackSelection createTrackSelection;
        int[] rendererFormatSupport;
        int[] rendererTrackGroupCounts = new int[(rendererCapabilities.length + 1)];
        TrackGroup[][] rendererTrackGroups = new TrackGroup[(rendererCapabilities.length + 1)][];
        int[][][] rendererFormatSupports = new int[(rendererCapabilities.length + 1)][][];
        for (int i = 0; i < rendererTrackGroups.length; i++) {
            rendererTrackGroups[i] = new TrackGroup[trackGroups.length];
            rendererFormatSupports[i] = new int[trackGroups.length][];
        }
        int[] mixedMimeTypeAdaptationSupport = getMixedMimeTypeAdaptationSupport(rendererCapabilities);
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            int rendererIndex = findRenderer(rendererCapabilities, group);
            if (rendererIndex == rendererCapabilities.length) {
                rendererFormatSupport = new int[group.length];
            } else {
                rendererFormatSupport = getFormatSupport(rendererCapabilities[rendererIndex], group);
            }
            int rendererTrackGroupCount = rendererTrackGroupCounts[rendererIndex];
            rendererTrackGroups[rendererIndex][rendererTrackGroupCount] = group;
            rendererFormatSupports[rendererIndex][rendererTrackGroupCount] = rendererFormatSupport;
            rendererTrackGroupCounts[rendererIndex] = rendererTrackGroupCounts[rendererIndex] + 1;
        }
        TrackGroupArray[] rendererTrackGroupArrays = new TrackGroupArray[rendererCapabilities.length];
        int[] rendererTrackTypes = new int[rendererCapabilities.length];
        for (int i2 = 0; i2 < rendererCapabilities.length; i2++) {
            int rendererTrackGroupCount2 = rendererTrackGroupCounts[i2];
            rendererTrackGroupArrays[i2] = new TrackGroupArray((TrackGroup[]) Arrays.copyOf(rendererTrackGroups[i2], rendererTrackGroupCount2));
            rendererFormatSupports[i2] = (int[][]) Arrays.copyOf(rendererFormatSupports[i2], rendererTrackGroupCount2);
            rendererTrackTypes[i2] = rendererCapabilities[i2].getTrackType();
        }
        TrackGroupArray unassociatedTrackGroupArray = new TrackGroupArray((TrackGroup[]) Arrays.copyOf(rendererTrackGroups[rendererCapabilities.length], rendererTrackGroupCounts[rendererCapabilities.length]));
        TrackSelection[] trackSelections = selectTracks(rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports);
        for (int i3 = 0; i3 < rendererCapabilities.length; i3++) {
            if (this.rendererDisabledFlags.get(i3)) {
                trackSelections[i3] = null;
            } else {
                TrackGroupArray rendererTrackGroup = rendererTrackGroupArrays[i3];
                if (hasSelectionOverride(i3, rendererTrackGroup)) {
                    SelectionOverride override = (SelectionOverride) this.selectionOverrides.get(i3).get(rendererTrackGroup);
                    if (override == null) {
                        createTrackSelection = null;
                    } else {
                        createTrackSelection = override.createTrackSelection(rendererTrackGroup);
                    }
                    trackSelections[i3] = createTrackSelection;
                }
            }
        }
        boolean[] rendererEnabled = determineEnabledRenderers(rendererCapabilities, trackSelections);
        MappedTrackInfo mappedTrackInfo = new MappedTrackInfo(rendererTrackTypes, rendererTrackGroupArrays, mixedMimeTypeAdaptationSupport, rendererFormatSupports, unassociatedTrackGroupArray);
        RendererConfiguration[] rendererConfigurations = new RendererConfiguration[rendererCapabilities.length];
        for (int i4 = 0; i4 < rendererCapabilities.length; i4++) {
            rendererConfigurations[i4] = rendererEnabled[i4] ? RendererConfiguration.DEFAULT : null;
        }
        maybeConfigureRenderersForTunneling(rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports, rendererConfigurations, trackSelections, this.tunnelingAudioSessionId);
        return new TrackSelectorResult(trackGroups, rendererEnabled, new TrackSelectionArray(trackSelections), mappedTrackInfo, rendererConfigurations);
    }

    private boolean[] determineEnabledRenderers(RendererCapabilities[] rendererCapabilities, TrackSelection[] trackSelections) {
        boolean[] rendererEnabled = new boolean[trackSelections.length];
        for (int i = 0; i < rendererEnabled.length; i++) {
            rendererEnabled[i] = !this.rendererDisabledFlags.get(i) && (rendererCapabilities[i].getTrackType() == 5 || trackSelections[i] != null);
        }
        return rendererEnabled;
    }

    public final void onSelectionActivated(Object info) {
        this.currentMappedTrackInfo = (MappedTrackInfo) info;
    }

    private static int findRenderer(RendererCapabilities[] rendererCapabilities, TrackGroup group) throws ExoPlaybackException {
        int bestRendererIndex = rendererCapabilities.length;
        int bestFormatSupportLevel = 0;
        for (int rendererIndex = 0; rendererIndex < rendererCapabilities.length; rendererIndex++) {
            RendererCapabilities rendererCapability = rendererCapabilities[rendererIndex];
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                int formatSupportLevel = rendererCapability.supportsFormat(group.getFormat(trackIndex)) & 7;
                if (formatSupportLevel > bestFormatSupportLevel) {
                    bestRendererIndex = rendererIndex;
                    bestFormatSupportLevel = formatSupportLevel;
                    if (bestFormatSupportLevel == 4) {
                        return bestRendererIndex;
                    }
                }
            }
        }
        return bestRendererIndex;
    }

    private static int[] getFormatSupport(RendererCapabilities rendererCapabilities, TrackGroup group) throws ExoPlaybackException {
        int[] formatSupport = new int[group.length];
        for (int i = 0; i < group.length; i++) {
            formatSupport[i] = rendererCapabilities.supportsFormat(group.getFormat(i));
        }
        return formatSupport;
    }

    private static int[] getMixedMimeTypeAdaptationSupport(RendererCapabilities[] rendererCapabilities) throws ExoPlaybackException {
        int[] mixedMimeTypeAdaptationSupport = new int[rendererCapabilities.length];
        for (int i = 0; i < mixedMimeTypeAdaptationSupport.length; i++) {
            mixedMimeTypeAdaptationSupport[i] = rendererCapabilities[i].supportsMixedMimeTypeAdaptation();
        }
        return mixedMimeTypeAdaptationSupport;
    }

    private static void maybeConfigureRenderersForTunneling(RendererCapabilities[] rendererCapabilities, TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports, RendererConfiguration[] rendererConfigurations, TrackSelection[] trackSelections, int tunnelingAudioSessionId2) {
        if (tunnelingAudioSessionId2 != 0) {
            int tunnelingAudioRendererIndex = -1;
            int tunnelingVideoRendererIndex = -1;
            boolean enableTunneling = true;
            int i = 0;
            while (true) {
                if (i >= rendererCapabilities.length) {
                    break;
                }
                int rendererType = rendererCapabilities[i].getTrackType();
                TrackSelection trackSelection = trackSelections[i];
                if ((rendererType == 1 || rendererType == 2) && trackSelection != null && rendererSupportsTunneling(rendererFormatSupports[i], rendererTrackGroupArrays[i], trackSelection)) {
                    if (rendererType == 1) {
                        if (tunnelingAudioRendererIndex != -1) {
                            enableTunneling = false;
                            break;
                        }
                        tunnelingAudioRendererIndex = i;
                    } else if (tunnelingVideoRendererIndex != -1) {
                        enableTunneling = false;
                        break;
                    } else {
                        tunnelingVideoRendererIndex = i;
                    }
                }
                i++;
            }
            if (enableTunneling && ((tunnelingAudioRendererIndex == -1 || tunnelingVideoRendererIndex == -1) ? false : true)) {
                RendererConfiguration tunnelingRendererConfiguration = new RendererConfiguration(tunnelingAudioSessionId2);
                rendererConfigurations[tunnelingAudioRendererIndex] = tunnelingRendererConfiguration;
                rendererConfigurations[tunnelingVideoRendererIndex] = tunnelingRendererConfiguration;
            }
        }
    }

    private static boolean rendererSupportsTunneling(int[][] formatSupport, TrackGroupArray trackGroups, TrackSelection selection) {
        if (selection == null) {
            return false;
        }
        int trackGroupIndex = trackGroups.indexOf(selection.getTrackGroup());
        for (int i = 0; i < selection.length(); i++) {
            if ((formatSupport[trackGroupIndex][selection.getIndexInTrackGroup(i)] & 32) != 32) {
                return false;
            }
        }
        return true;
    }
}
