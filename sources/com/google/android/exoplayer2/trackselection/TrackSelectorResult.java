package com.google.android.exoplayer2.trackselection;

import com.google.android.exoplayer2.RendererConfiguration;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.util.Util;

public final class TrackSelectorResult {
    public final TrackGroupArray groups;
    public final Object info;
    public final RendererConfiguration[] rendererConfigurations;
    public final boolean[] renderersEnabled;
    public final TrackSelectionArray selections;

    public TrackSelectorResult(TrackGroupArray groups2, boolean[] renderersEnabled2, TrackSelectionArray selections2, Object info2, RendererConfiguration[] rendererConfigurations2) {
        this.groups = groups2;
        this.renderersEnabled = renderersEnabled2;
        this.selections = selections2;
        this.info = info2;
        this.rendererConfigurations = rendererConfigurations2;
    }

    public boolean isEquivalent(TrackSelectorResult other) {
        if (other == null) {
            return false;
        }
        for (int i = 0; i < this.selections.length; i++) {
            if (!isEquivalent(other, i)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEquivalent(TrackSelectorResult other, int index) {
        if (other != null && this.renderersEnabled[index] == other.renderersEnabled[index] && Util.areEqual(this.selections.get(index), other.selections.get(index)) && Util.areEqual(this.rendererConfigurations[index], other.rendererConfigurations[index])) {
            return true;
        }
        return false;
    }
}
