package com.google.android.exoplayer2.extractor.mp4;

import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.extractor.ChunkIndex;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.extractor.mp4.Atom;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import com.google.android.exoplayer2.text.cea.CeaUtil;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.NalUnitUtil;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.TimestampAdjuster;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

public final class FragmentedMp4Extractor implements Extractor {
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() {
        public Extractor[] createExtractors() {
            return new Extractor[]{new FragmentedMp4Extractor()};
        }
    };
    public static final int FLAG_ENABLE_EMSG_TRACK = 4;
    private static final int FLAG_SIDELOADED = 8;
    public static final int FLAG_WORKAROUND_EVERY_VIDEO_FRAME_IS_SYNC_FRAME = 1;
    public static final int FLAG_WORKAROUND_IGNORE_EDIT_LISTS = 16;
    public static final int FLAG_WORKAROUND_IGNORE_TFDT_BOX = 2;
    private static final byte[] PIFF_SAMPLE_ENCRYPTION_BOX_EXTENDED_TYPE = {-94, 57, 79, 82, 90, -101, 79, 20, -94, 68, 108, 66, 124, 100, -115, -12};
    private static final int SAMPLE_GROUP_TYPE_seig = Util.getIntegerCodeForString("seig");
    private static final int STATE_READING_ATOM_HEADER = 0;
    private static final int STATE_READING_ATOM_PAYLOAD = 1;
    private static final int STATE_READING_ENCRYPTION_DATA = 2;
    private static final int STATE_READING_SAMPLE_CONTINUE = 4;
    private static final int STATE_READING_SAMPLE_START = 3;
    private static final String TAG = "FragmentedMp4Extractor";
    private ParsableByteArray atomData;
    private final ParsableByteArray atomHeader;
    private int atomHeaderBytesRead;
    private long atomSize;
    private int atomType;
    private TrackOutput[] cea608TrackOutputs;
    private final List<Format> closedCaptionFormats;
    private final Stack<Atom.ContainerAtom> containerAtoms;
    private TrackBundle currentTrackBundle;
    private final ParsableByteArray defaultInitializationVector;
    private long durationUs;
    private final ParsableByteArray encryptionSignalByte;
    private long endOfMdatPosition;
    private TrackOutput eventMessageTrackOutput;
    private final byte[] extendedTypeScratch;
    private ExtractorOutput extractorOutput;
    private final int flags;
    private boolean haveOutputSeekMap;
    private final ParsableByteArray nalBuffer;
    private final ParsableByteArray nalPrefix;
    private final ParsableByteArray nalStartCode;
    private int parserState;
    private int pendingMetadataSampleBytes;
    private final LinkedList<MetadataSampleInfo> pendingMetadataSampleInfos;
    private boolean processSeiNalUnitPayload;
    private int sampleBytesWritten;
    private int sampleCurrentNalBytesRemaining;
    private int sampleSize;
    private long segmentIndexEarliestPresentationTimeUs;
    private final DrmInitData sideloadedDrmInitData;
    private final Track sideloadedTrack;
    private final TimestampAdjuster timestampAdjuster;
    private final SparseArray<TrackBundle> trackBundles;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    public FragmentedMp4Extractor() {
        this(0);
    }

    public FragmentedMp4Extractor(int flags2) {
        this(flags2, (TimestampAdjuster) null);
    }

    public FragmentedMp4Extractor(int flags2, TimestampAdjuster timestampAdjuster2) {
        this(flags2, timestampAdjuster2, (Track) null, (DrmInitData) null);
    }

    public FragmentedMp4Extractor(int flags2, TimestampAdjuster timestampAdjuster2, Track sideloadedTrack2, DrmInitData sideloadedDrmInitData2) {
        this(flags2, timestampAdjuster2, sideloadedTrack2, sideloadedDrmInitData2, Collections.emptyList());
    }

    public FragmentedMp4Extractor(int flags2, TimestampAdjuster timestampAdjuster2, Track sideloadedTrack2, DrmInitData sideloadedDrmInitData2, List<Format> closedCaptionFormats2) {
        this.flags = (sideloadedTrack2 != null ? 8 : 0) | flags2;
        this.timestampAdjuster = timestampAdjuster2;
        this.sideloadedTrack = sideloadedTrack2;
        this.sideloadedDrmInitData = sideloadedDrmInitData2;
        this.closedCaptionFormats = Collections.unmodifiableList(closedCaptionFormats2);
        this.atomHeader = new ParsableByteArray(16);
        this.nalStartCode = new ParsableByteArray(NalUnitUtil.NAL_START_CODE);
        this.nalPrefix = new ParsableByteArray(5);
        this.nalBuffer = new ParsableByteArray();
        this.encryptionSignalByte = new ParsableByteArray(1);
        this.defaultInitializationVector = new ParsableByteArray();
        this.extendedTypeScratch = new byte[16];
        this.containerAtoms = new Stack<>();
        this.pendingMetadataSampleInfos = new LinkedList<>();
        this.trackBundles = new SparseArray<>();
        this.durationUs = C.TIME_UNSET;
        this.segmentIndexEarliestPresentationTimeUs = C.TIME_UNSET;
        enterReadingAtomHeaderState();
    }

    public boolean sniff(ExtractorInput input) throws IOException, InterruptedException {
        return Sniffer.sniffFragmented(input);
    }

    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
        if (this.sideloadedTrack != null) {
            TrackBundle bundle = new TrackBundle(output.track(0, this.sideloadedTrack.type));
            bundle.init(this.sideloadedTrack, new DefaultSampleValues(0, 0, 0, 0));
            this.trackBundles.put(0, bundle);
            maybeInitExtraTracks();
            this.extractorOutput.endTracks();
        }
    }

    public void seek(long position, long timeUs) {
        int trackCount = this.trackBundles.size();
        for (int i = 0; i < trackCount; i++) {
            this.trackBundles.valueAt(i).reset();
        }
        this.pendingMetadataSampleInfos.clear();
        this.pendingMetadataSampleBytes = 0;
        this.containerAtoms.clear();
        enterReadingAtomHeaderState();
    }

    public void release() {
    }

    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {
        while (true) {
            switch (this.parserState) {
                case 0:
                    if (readAtomHeader(input)) {
                        break;
                    } else {
                        return -1;
                    }
                case 1:
                    readAtomPayload(input);
                    break;
                case 2:
                    readEncryptionData(input);
                    break;
                default:
                    if (!readSample(input)) {
                        break;
                    } else {
                        return 0;
                    }
            }
        }
    }

    private void enterReadingAtomHeaderState() {
        this.parserState = 0;
        this.atomHeaderBytesRead = 0;
    }

    private boolean readAtomHeader(ExtractorInput input) throws IOException, InterruptedException {
        if (this.atomHeaderBytesRead == 0) {
            if (!input.readFully(this.atomHeader.data, 0, 8, true)) {
                return false;
            }
            this.atomHeaderBytesRead = 8;
            this.atomHeader.setPosition(0);
            this.atomSize = this.atomHeader.readUnsignedInt();
            this.atomType = this.atomHeader.readInt();
        }
        if (this.atomSize == 1) {
            input.readFully(this.atomHeader.data, 8, 8);
            this.atomHeaderBytesRead += 8;
            this.atomSize = this.atomHeader.readUnsignedLongToLong();
        } else if (this.atomSize == 0) {
            long endPosition = input.getLength();
            if (endPosition == -1 && !this.containerAtoms.isEmpty()) {
                endPosition = this.containerAtoms.peek().endPosition;
            }
            if (endPosition != -1) {
                this.atomSize = (endPosition - input.getPosition()) + ((long) this.atomHeaderBytesRead);
            }
        }
        if (this.atomSize < ((long) this.atomHeaderBytesRead)) {
            throw new ParserException("Atom size less than header length (unsupported).");
        }
        long atomPosition = input.getPosition() - ((long) this.atomHeaderBytesRead);
        if (this.atomType == Atom.TYPE_moof) {
            int trackCount = this.trackBundles.size();
            for (int i = 0; i < trackCount; i++) {
                TrackFragment fragment = this.trackBundles.valueAt(i).fragment;
                fragment.atomPosition = atomPosition;
                fragment.auxiliaryDataPosition = atomPosition;
                fragment.dataPosition = atomPosition;
            }
        }
        if (this.atomType == Atom.TYPE_mdat) {
            this.currentTrackBundle = null;
            this.endOfMdatPosition = this.atomSize + atomPosition;
            if (!this.haveOutputSeekMap) {
                this.extractorOutput.seekMap(new SeekMap.Unseekable(this.durationUs, atomPosition));
                this.haveOutputSeekMap = true;
            }
            this.parserState = 2;
            return true;
        }
        if (shouldParseContainerAtom(this.atomType)) {
            long endPosition2 = (input.getPosition() + this.atomSize) - 8;
            this.containerAtoms.add(new Atom.ContainerAtom(this.atomType, endPosition2));
            if (this.atomSize == ((long) this.atomHeaderBytesRead)) {
                processAtomEnded(endPosition2);
            } else {
                enterReadingAtomHeaderState();
            }
        } else if (shouldParseLeafAtom(this.atomType)) {
            if (this.atomHeaderBytesRead != 8) {
                throw new ParserException("Leaf atom defines extended atom size (unsupported).");
            } else if (this.atomSize > 2147483647L) {
                throw new ParserException("Leaf atom with length > 2147483647 (unsupported).");
            } else {
                this.atomData = new ParsableByteArray((int) this.atomSize);
                System.arraycopy(this.atomHeader.data, 0, this.atomData.data, 0, 8);
                this.parserState = 1;
            }
        } else if (this.atomSize > 2147483647L) {
            throw new ParserException("Skipping atom with length > 2147483647 (unsupported).");
        } else {
            this.atomData = null;
            this.parserState = 1;
        }
        return true;
    }

    private void readAtomPayload(ExtractorInput input) throws IOException, InterruptedException {
        int atomPayloadSize = ((int) this.atomSize) - this.atomHeaderBytesRead;
        if (this.atomData != null) {
            input.readFully(this.atomData.data, 8, atomPayloadSize);
            onLeafAtomRead(new Atom.LeafAtom(this.atomType, this.atomData), input.getPosition());
        } else {
            input.skipFully(atomPayloadSize);
        }
        processAtomEnded(input.getPosition());
    }

    private void processAtomEnded(long atomEndPosition) throws ParserException {
        while (!this.containerAtoms.isEmpty() && this.containerAtoms.peek().endPosition == atomEndPosition) {
            onContainerAtomRead(this.containerAtoms.pop());
        }
        enterReadingAtomHeaderState();
    }

    private void onLeafAtomRead(Atom.LeafAtom leaf, long inputPosition) throws ParserException {
        if (!this.containerAtoms.isEmpty()) {
            this.containerAtoms.peek().add(leaf);
        } else if (leaf.type == Atom.TYPE_sidx) {
            Pair<Long, ChunkIndex> result = parseSidx(leaf.data, inputPosition);
            this.segmentIndexEarliestPresentationTimeUs = ((Long) result.first).longValue();
            this.extractorOutput.seekMap((SeekMap) result.second);
            this.haveOutputSeekMap = true;
        } else if (leaf.type == Atom.TYPE_emsg) {
            onEmsgLeafAtomRead(leaf.data);
        }
    }

    private void onContainerAtomRead(Atom.ContainerAtom container) throws ParserException {
        if (container.type == Atom.TYPE_moov) {
            onMoovContainerAtomRead(container);
        } else if (container.type == Atom.TYPE_moof) {
            onMoofContainerAtomRead(container);
        } else if (!this.containerAtoms.isEmpty()) {
            this.containerAtoms.peek().add(container);
        }
    }

    private void onMoovContainerAtomRead(Atom.ContainerAtom moov) throws ParserException {
        DrmInitData drmInitData;
        Assertions.checkState(this.sideloadedTrack == null, "Unexpected moov box.");
        if (this.sideloadedDrmInitData != null) {
            drmInitData = this.sideloadedDrmInitData;
        } else {
            drmInitData = getDrmInitDataFromAtoms(moov.leafChildren);
        }
        Atom.ContainerAtom mvex = moov.getContainerAtomOfType(Atom.TYPE_mvex);
        SparseArray<DefaultSampleValues> defaultSampleValuesArray = new SparseArray<>();
        long duration = C.TIME_UNSET;
        int mvexChildrenSize = mvex.leafChildren.size();
        for (int i = 0; i < mvexChildrenSize; i++) {
            Atom.LeafAtom atom = mvex.leafChildren.get(i);
            if (atom.type == Atom.TYPE_trex) {
                Pair<Integer, DefaultSampleValues> trexData = parseTrex(atom.data);
                defaultSampleValuesArray.put(((Integer) trexData.first).intValue(), trexData.second);
            } else if (atom.type == Atom.TYPE_mehd) {
                duration = parseMehd(atom.data);
            }
        }
        SparseArray<Track> tracks = new SparseArray<>();
        int moovContainerChildrenSize = moov.containerChildren.size();
        for (int i2 = 0; i2 < moovContainerChildrenSize; i2++) {
            Atom.ContainerAtom atom2 = moov.containerChildren.get(i2);
            if (atom2.type == Atom.TYPE_trak) {
                Track track = AtomParsers.parseTrak(atom2, moov.getLeafAtomOfType(Atom.TYPE_mvhd), duration, drmInitData, (this.flags & 16) != 0, false);
                if (track != null) {
                    tracks.put(track.id, track);
                }
            }
        }
        int trackCount = tracks.size();
        if (this.trackBundles.size() == 0) {
            for (int i3 = 0; i3 < trackCount; i3++) {
                Track track2 = tracks.valueAt(i3);
                TrackBundle trackBundle = new TrackBundle(this.extractorOutput.track(i3, track2.type));
                trackBundle.init(track2, defaultSampleValuesArray.get(track2.id));
                this.trackBundles.put(track2.id, trackBundle);
                this.durationUs = Math.max(this.durationUs, track2.durationUs);
            }
            maybeInitExtraTracks();
            this.extractorOutput.endTracks();
            return;
        }
        Assertions.checkState(this.trackBundles.size() == trackCount);
        for (int i4 = 0; i4 < trackCount; i4++) {
            Track track3 = tracks.valueAt(i4);
            this.trackBundles.get(track3.id).init(track3, defaultSampleValuesArray.get(track3.id));
        }
    }

    private void onMoofContainerAtomRead(Atom.ContainerAtom moof) throws ParserException {
        DrmInitData drmInitData;
        parseMoof(moof, this.trackBundles, this.flags, this.extendedTypeScratch);
        if (this.sideloadedDrmInitData != null) {
            drmInitData = null;
        } else {
            drmInitData = getDrmInitDataFromAtoms(moof.leafChildren);
        }
        if (drmInitData != null) {
            int trackCount = this.trackBundles.size();
            for (int i = 0; i < trackCount; i++) {
                this.trackBundles.valueAt(i).updateDrmInitData(drmInitData);
            }
        }
    }

    private void maybeInitExtraTracks() {
        if ((this.flags & 4) != 0 && this.eventMessageTrackOutput == null) {
            this.eventMessageTrackOutput = this.extractorOutput.track(this.trackBundles.size(), 4);
            this.eventMessageTrackOutput.format(Format.createSampleFormat((String) null, MimeTypes.APPLICATION_EMSG, Long.MAX_VALUE));
        }
        if (this.cea608TrackOutputs == null) {
            this.cea608TrackOutputs = new TrackOutput[this.closedCaptionFormats.size()];
            for (int i = 0; i < this.cea608TrackOutputs.length; i++) {
                TrackOutput output = this.extractorOutput.track(this.trackBundles.size() + 1 + i, 3);
                output.format(this.closedCaptionFormats.get(i));
                this.cea608TrackOutputs[i] = output;
            }
        }
    }

    private void onEmsgLeafAtomRead(ParsableByteArray atom) {
        if (this.eventMessageTrackOutput != null) {
            atom.setPosition(12);
            atom.readNullTerminatedString();
            atom.readNullTerminatedString();
            long presentationTimeDeltaUs = Util.scaleLargeTimestamp(atom.readUnsignedInt(), C.MICROS_PER_SECOND, atom.readUnsignedInt());
            atom.setPosition(12);
            int sampleSize2 = atom.bytesLeft();
            this.eventMessageTrackOutput.sampleData(atom, sampleSize2);
            if (this.segmentIndexEarliestPresentationTimeUs != C.TIME_UNSET) {
                this.eventMessageTrackOutput.sampleMetadata(this.segmentIndexEarliestPresentationTimeUs + presentationTimeDeltaUs, 1, sampleSize2, 0, (TrackOutput.CryptoData) null);
                return;
            }
            this.pendingMetadataSampleInfos.addLast(new MetadataSampleInfo(presentationTimeDeltaUs, sampleSize2));
            this.pendingMetadataSampleBytes += sampleSize2;
        }
    }

    private static Pair<Integer, DefaultSampleValues> parseTrex(ParsableByteArray trex) {
        trex.setPosition(12);
        return Pair.create(Integer.valueOf(trex.readInt()), new DefaultSampleValues(trex.readUnsignedIntToInt() - 1, trex.readUnsignedIntToInt(), trex.readUnsignedIntToInt(), trex.readInt()));
    }

    private static long parseMehd(ParsableByteArray mehd) {
        mehd.setPosition(8);
        return Atom.parseFullAtomVersion(mehd.readInt()) == 0 ? mehd.readUnsignedInt() : mehd.readUnsignedLongToLong();
    }

    private static void parseMoof(Atom.ContainerAtom moof, SparseArray<TrackBundle> trackBundleArray, int flags2, byte[] extendedTypeScratch2) throws ParserException {
        int moofContainerChildrenSize = moof.containerChildren.size();
        for (int i = 0; i < moofContainerChildrenSize; i++) {
            Atom.ContainerAtom child = moof.containerChildren.get(i);
            if (child.type == Atom.TYPE_traf) {
                parseTraf(child, trackBundleArray, flags2, extendedTypeScratch2);
            }
        }
    }

    private static void parseTraf(Atom.ContainerAtom traf, SparseArray<TrackBundle> trackBundleArray, int flags2, byte[] extendedTypeScratch2) throws ParserException {
        TrackBundle trackBundle = parseTfhd(traf.getLeafAtomOfType(Atom.TYPE_tfhd).data, trackBundleArray, flags2);
        if (trackBundle != null) {
            TrackFragment fragment = trackBundle.fragment;
            long decodeTime = fragment.nextFragmentDecodeTime;
            trackBundle.reset();
            if (traf.getLeafAtomOfType(Atom.TYPE_tfdt) != null && (flags2 & 2) == 0) {
                decodeTime = parseTfdt(traf.getLeafAtomOfType(Atom.TYPE_tfdt).data);
            }
            parseTruns(traf, trackBundle, decodeTime, flags2);
            TrackEncryptionBox encryptionBox = trackBundle.track.getSampleDescriptionEncryptionBox(fragment.header.sampleDescriptionIndex);
            Atom.LeafAtom saiz = traf.getLeafAtomOfType(Atom.TYPE_saiz);
            if (saiz != null) {
                parseSaiz(encryptionBox, saiz.data, fragment);
            }
            Atom.LeafAtom saio = traf.getLeafAtomOfType(Atom.TYPE_saio);
            if (saio != null) {
                parseSaio(saio.data, fragment);
            }
            Atom.LeafAtom senc = traf.getLeafAtomOfType(Atom.TYPE_senc);
            if (senc != null) {
                parseSenc(senc.data, fragment);
            }
            Atom.LeafAtom sbgp = traf.getLeafAtomOfType(Atom.TYPE_sbgp);
            Atom.LeafAtom sgpd = traf.getLeafAtomOfType(Atom.TYPE_sgpd);
            if (!(sbgp == null || sgpd == null)) {
                parseSgpd(sbgp.data, sgpd.data, encryptionBox != null ? encryptionBox.schemeType : null, fragment);
            }
            int leafChildrenSize = traf.leafChildren.size();
            for (int i = 0; i < leafChildrenSize; i++) {
                Atom.LeafAtom atom = traf.leafChildren.get(i);
                if (atom.type == Atom.TYPE_uuid) {
                    parseUuid(atom.data, fragment, extendedTypeScratch2);
                }
            }
        }
    }

    private static void parseTruns(Atom.ContainerAtom traf, TrackBundle trackBundle, long decodeTime, int flags2) {
        int trunCount = 0;
        int totalSampleCount = 0;
        List<Atom.LeafAtom> leafChildren = traf.leafChildren;
        int leafChildrenSize = leafChildren.size();
        for (int i = 0; i < leafChildrenSize; i++) {
            Atom.LeafAtom atom = leafChildren.get(i);
            if (atom.type == Atom.TYPE_trun) {
                ParsableByteArray trunData = atom.data;
                trunData.setPosition(12);
                int trunSampleCount = trunData.readUnsignedIntToInt();
                if (trunSampleCount > 0) {
                    totalSampleCount += trunSampleCount;
                    trunCount++;
                }
            }
        }
        trackBundle.currentTrackRunIndex = 0;
        trackBundle.currentSampleInTrackRun = 0;
        trackBundle.currentSampleIndex = 0;
        trackBundle.fragment.initTables(trunCount, totalSampleCount);
        int trunIndex = 0;
        int trunStartPosition = 0;
        for (int i2 = 0; i2 < leafChildrenSize; i2++) {
            Atom.LeafAtom trun = leafChildren.get(i2);
            if (trun.type == Atom.TYPE_trun) {
                trunStartPosition = parseTrun(trackBundle, trunIndex, decodeTime, flags2, trun.data, trunStartPosition);
                trunIndex++;
            }
        }
    }

    private static void parseSaiz(TrackEncryptionBox encryptionBox, ParsableByteArray saiz, TrackFragment out) throws ParserException {
        int vectorSize = encryptionBox.initializationVectorSize;
        saiz.setPosition(8);
        if ((Atom.parseFullAtomFlags(saiz.readInt()) & 1) == 1) {
            saiz.skipBytes(8);
        }
        int defaultSampleInfoSize = saiz.readUnsignedByte();
        int sampleCount = saiz.readUnsignedIntToInt();
        if (sampleCount != out.sampleCount) {
            throw new ParserException("Length mismatch: " + sampleCount + ", " + out.sampleCount);
        }
        int totalSize = 0;
        if (defaultSampleInfoSize == 0) {
            boolean[] sampleHasSubsampleEncryptionTable = out.sampleHasSubsampleEncryptionTable;
            for (int i = 0; i < sampleCount; i++) {
                int sampleInfoSize = saiz.readUnsignedByte();
                totalSize += sampleInfoSize;
                sampleHasSubsampleEncryptionTable[i] = sampleInfoSize > vectorSize;
            }
        } else {
            totalSize = 0 + (defaultSampleInfoSize * sampleCount);
            Arrays.fill(out.sampleHasSubsampleEncryptionTable, 0, sampleCount, defaultSampleInfoSize > vectorSize);
        }
        out.initEncryptionData(totalSize);
    }

    private static void parseSaio(ParsableByteArray saio, TrackFragment out) throws ParserException {
        saio.setPosition(8);
        int fullAtom = saio.readInt();
        if ((Atom.parseFullAtomFlags(fullAtom) & 1) == 1) {
            saio.skipBytes(8);
        }
        int entryCount = saio.readUnsignedIntToInt();
        if (entryCount != 1) {
            throw new ParserException("Unexpected saio entry count: " + entryCount);
        }
        int version = Atom.parseFullAtomVersion(fullAtom);
        out.auxiliaryDataPosition = (version == 0 ? saio.readUnsignedInt() : saio.readUnsignedLongToLong()) + out.auxiliaryDataPosition;
    }

    private static TrackBundle parseTfhd(ParsableByteArray tfhd, SparseArray<TrackBundle> trackBundles2, int flags2) {
        tfhd.setPosition(8);
        int atomFlags = Atom.parseFullAtomFlags(tfhd.readInt());
        int trackId = tfhd.readInt();
        if ((flags2 & 8) != 0) {
            trackId = 0;
        }
        TrackBundle trackBundle = trackBundles2.get(trackId);
        if (trackBundle == null) {
            return null;
        }
        if ((atomFlags & 1) != 0) {
            long baseDataPosition = tfhd.readUnsignedLongToLong();
            trackBundle.fragment.dataPosition = baseDataPosition;
            trackBundle.fragment.auxiliaryDataPosition = baseDataPosition;
        }
        DefaultSampleValues defaultSampleValues = trackBundle.defaultSampleValues;
        trackBundle.fragment.header = new DefaultSampleValues((atomFlags & 2) != 0 ? tfhd.readUnsignedIntToInt() - 1 : defaultSampleValues.sampleDescriptionIndex, (atomFlags & 8) != 0 ? tfhd.readUnsignedIntToInt() : defaultSampleValues.duration, (atomFlags & 16) != 0 ? tfhd.readUnsignedIntToInt() : defaultSampleValues.size, (atomFlags & 32) != 0 ? tfhd.readUnsignedIntToInt() : defaultSampleValues.flags);
        return trackBundle;
    }

    private static long parseTfdt(ParsableByteArray tfdt) {
        tfdt.setPosition(8);
        return Atom.parseFullAtomVersion(tfdt.readInt()) == 1 ? tfdt.readUnsignedLongToLong() : tfdt.readUnsignedInt();
    }

    private static int parseTrun(TrackBundle trackBundle, int index, long decodeTime, int flags2, ParsableByteArray trun, int trackRunStart) {
        long cumulativeTime;
        int sampleFlags;
        trun.setPosition(8);
        int atomFlags = Atom.parseFullAtomFlags(trun.readInt());
        Track track = trackBundle.track;
        TrackFragment fragment = trackBundle.fragment;
        DefaultSampleValues defaultSampleValues = fragment.header;
        fragment.trunLength[index] = trun.readUnsignedIntToInt();
        fragment.trunDataPosition[index] = fragment.dataPosition;
        if ((atomFlags & 1) != 0) {
            long[] jArr = fragment.trunDataPosition;
            jArr[index] = jArr[index] + ((long) trun.readInt());
        }
        boolean firstSampleFlagsPresent = (atomFlags & 4) != 0;
        int firstSampleFlags = defaultSampleValues.flags;
        if (firstSampleFlagsPresent) {
            firstSampleFlags = trun.readUnsignedIntToInt();
        }
        boolean sampleDurationsPresent = (atomFlags & 256) != 0;
        boolean sampleSizesPresent = (atomFlags & 512) != 0;
        boolean sampleFlagsPresent = (atomFlags & 1024) != 0;
        boolean sampleCompositionTimeOffsetsPresent = (atomFlags & 2048) != 0;
        long edtsOffset = 0;
        if (track.editListDurations != null && track.editListDurations.length == 1 && track.editListDurations[0] == 0) {
            edtsOffset = Util.scaleLargeTimestamp(track.editListMediaTimes[0], 1000, track.timescale);
        }
        int[] sampleSizeTable = fragment.sampleSizeTable;
        int[] sampleCompositionTimeOffsetTable = fragment.sampleCompositionTimeOffsetTable;
        long[] sampleDecodingTimeTable = fragment.sampleDecodingTimeTable;
        boolean[] sampleIsSyncFrameTable = fragment.sampleIsSyncFrameTable;
        boolean workaroundEveryVideoFrameIsSyncFrame = track.type == 2 && (flags2 & 1) != 0;
        int trackRunEnd = trackRunStart + fragment.trunLength[index];
        long timescale = track.timescale;
        if (index > 0) {
            cumulativeTime = fragment.nextFragmentDecodeTime;
        } else {
            cumulativeTime = decodeTime;
        }
        int i = trackRunStart;
        while (i < trackRunEnd) {
            int sampleDuration = sampleDurationsPresent ? trun.readUnsignedIntToInt() : defaultSampleValues.duration;
            int sampleSize2 = sampleSizesPresent ? trun.readUnsignedIntToInt() : defaultSampleValues.size;
            if (i != 0 || !firstSampleFlagsPresent) {
                sampleFlags = sampleFlagsPresent ? trun.readInt() : defaultSampleValues.flags;
            } else {
                sampleFlags = firstSampleFlags;
            }
            if (sampleCompositionTimeOffsetsPresent) {
                sampleCompositionTimeOffsetTable[i] = (int) ((((long) trun.readInt()) * 1000) / timescale);
            } else {
                sampleCompositionTimeOffsetTable[i] = 0;
            }
            sampleDecodingTimeTable[i] = Util.scaleLargeTimestamp(cumulativeTime, 1000, timescale) - edtsOffset;
            sampleSizeTable[i] = sampleSize2;
            sampleIsSyncFrameTable[i] = ((sampleFlags >> 16) & 1) == 0 && (!workaroundEveryVideoFrameIsSyncFrame || i == 0);
            cumulativeTime += (long) sampleDuration;
            i++;
        }
        fragment.nextFragmentDecodeTime = cumulativeTime;
        return trackRunEnd;
    }

    private static void parseUuid(ParsableByteArray uuid, TrackFragment out, byte[] extendedTypeScratch2) throws ParserException {
        uuid.setPosition(8);
        uuid.readBytes(extendedTypeScratch2, 0, 16);
        if (Arrays.equals(extendedTypeScratch2, PIFF_SAMPLE_ENCRYPTION_BOX_EXTENDED_TYPE)) {
            parseSenc(uuid, 16, out);
        }
    }

    private static void parseSenc(ParsableByteArray senc, TrackFragment out) throws ParserException {
        parseSenc(senc, 0, out);
    }

    private static void parseSenc(ParsableByteArray senc, int offset, TrackFragment out) throws ParserException {
        boolean subsampleEncryption;
        senc.setPosition(offset + 8);
        int flags2 = Atom.parseFullAtomFlags(senc.readInt());
        if ((flags2 & 1) != 0) {
            throw new ParserException("Overriding TrackEncryptionBox parameters is unsupported.");
        }
        if ((flags2 & 2) != 0) {
            subsampleEncryption = true;
        } else {
            subsampleEncryption = false;
        }
        int sampleCount = senc.readUnsignedIntToInt();
        if (sampleCount != out.sampleCount) {
            throw new ParserException("Length mismatch: " + sampleCount + ", " + out.sampleCount);
        }
        Arrays.fill(out.sampleHasSubsampleEncryptionTable, 0, sampleCount, subsampleEncryption);
        out.initEncryptionData(senc.bytesLeft());
        out.fillEncryptionData(senc);
    }

    private static void parseSgpd(ParsableByteArray sbgp, ParsableByteArray sgpd, String schemeType, TrackFragment out) throws ParserException {
        sbgp.setPosition(8);
        int sbgpFullAtom = sbgp.readInt();
        if (sbgp.readInt() == SAMPLE_GROUP_TYPE_seig) {
            if (Atom.parseFullAtomVersion(sbgpFullAtom) == 1) {
                sbgp.skipBytes(4);
            }
            if (sbgp.readInt() != 1) {
                throw new ParserException("Entry count in sbgp != 1 (unsupported).");
            }
            sgpd.setPosition(8);
            int sgpdFullAtom = sgpd.readInt();
            if (sgpd.readInt() == SAMPLE_GROUP_TYPE_seig) {
                int sgpdVersion = Atom.parseFullAtomVersion(sgpdFullAtom);
                if (sgpdVersion == 1) {
                    if (sgpd.readUnsignedInt() == 0) {
                        throw new ParserException("Variable length description in sgpd found (unsupported)");
                    }
                } else if (sgpdVersion >= 2) {
                    sgpd.skipBytes(4);
                }
                if (sgpd.readUnsignedInt() != 1) {
                    throw new ParserException("Entry count in sgpd != 1 (unsupported).");
                }
                sgpd.skipBytes(1);
                int patternByte = sgpd.readUnsignedByte();
                int cryptByteBlock = (patternByte & PsExtractor.VIDEO_STREAM_MASK) >> 4;
                int skipByteBlock = patternByte & 15;
                boolean isProtected = sgpd.readUnsignedByte() == 1;
                if (isProtected) {
                    int perSampleIvSize = sgpd.readUnsignedByte();
                    byte[] keyId = new byte[16];
                    sgpd.readBytes(keyId, 0, keyId.length);
                    byte[] constantIv = null;
                    if (isProtected && perSampleIvSize == 0) {
                        int constantIvSize = sgpd.readUnsignedByte();
                        constantIv = new byte[constantIvSize];
                        sgpd.readBytes(constantIv, 0, constantIvSize);
                    }
                    out.definesEncryptionData = true;
                    out.trackEncryptionBox = new TrackEncryptionBox(isProtected, schemeType, perSampleIvSize, keyId, cryptByteBlock, skipByteBlock, constantIv);
                }
            }
        }
    }

    private static Pair<Long, ChunkIndex> parseSidx(ParsableByteArray atom, long inputPosition) throws ParserException {
        long earliestPresentationTime;
        long offset;
        atom.setPosition(8);
        int version = Atom.parseFullAtomVersion(atom.readInt());
        atom.skipBytes(4);
        long timescale = atom.readUnsignedInt();
        long offset2 = inputPosition;
        if (version == 0) {
            earliestPresentationTime = atom.readUnsignedInt();
            offset = offset2 + atom.readUnsignedInt();
        } else {
            earliestPresentationTime = atom.readUnsignedLongToLong();
            offset = offset2 + atom.readUnsignedLongToLong();
        }
        long earliestPresentationTimeUs = Util.scaleLargeTimestamp(earliestPresentationTime, C.MICROS_PER_SECOND, timescale);
        atom.skipBytes(2);
        int referenceCount = atom.readUnsignedShort();
        int[] sizes = new int[referenceCount];
        long[] offsets = new long[referenceCount];
        long[] durationsUs = new long[referenceCount];
        long[] timesUs = new long[referenceCount];
        long time = earliestPresentationTime;
        long timeUs = earliestPresentationTimeUs;
        for (int i = 0; i < referenceCount; i++) {
            int firstInt = atom.readInt();
            if ((Integer.MIN_VALUE & firstInt) != 0) {
                throw new ParserException("Unhandled indirect reference");
            }
            long referenceDuration = atom.readUnsignedInt();
            sizes[i] = Integer.MAX_VALUE & firstInt;
            offsets[i] = offset;
            timesUs[i] = timeUs;
            time += referenceDuration;
            timeUs = Util.scaleLargeTimestamp(time, C.MICROS_PER_SECOND, timescale);
            durationsUs[i] = timeUs - timesUs[i];
            atom.skipBytes(4);
            offset += (long) sizes[i];
        }
        return Pair.create(Long.valueOf(earliestPresentationTimeUs), new ChunkIndex(sizes, offsets, durationsUs, timesUs));
    }

    private void readEncryptionData(ExtractorInput input) throws IOException, InterruptedException {
        TrackBundle nextTrackBundle = null;
        long nextDataOffset = Long.MAX_VALUE;
        int trackBundlesSize = this.trackBundles.size();
        for (int i = 0; i < trackBundlesSize; i++) {
            TrackFragment trackFragment = this.trackBundles.valueAt(i).fragment;
            if (trackFragment.sampleEncryptionDataNeedsFill && trackFragment.auxiliaryDataPosition < nextDataOffset) {
                nextDataOffset = trackFragment.auxiliaryDataPosition;
                nextTrackBundle = this.trackBundles.valueAt(i);
            }
        }
        if (nextTrackBundle == null) {
            this.parserState = 3;
            return;
        }
        int bytesToSkip = (int) (nextDataOffset - input.getPosition());
        if (bytesToSkip < 0) {
            throw new ParserException("Offset to encryption data was negative.");
        }
        input.skipFully(bytesToSkip);
        nextTrackBundle.fragment.fillEncryptionData(input);
    }

    private boolean readSample(ExtractorInput input) throws IOException, InterruptedException {
        TrackEncryptionBox encryptionBox;
        int writtenBytes;
        if (this.parserState == 3) {
            if (this.currentTrackBundle == null) {
                TrackBundle currentTrackBundle2 = getNextFragmentRun(this.trackBundles);
                if (currentTrackBundle2 == null) {
                    int bytesToSkip = (int) (this.endOfMdatPosition - input.getPosition());
                    if (bytesToSkip < 0) {
                        throw new ParserException("Offset to end of mdat was negative.");
                    }
                    input.skipFully(bytesToSkip);
                    enterReadingAtomHeaderState();
                    return false;
                }
                int bytesToSkip2 = (int) (currentTrackBundle2.fragment.trunDataPosition[currentTrackBundle2.currentTrackRunIndex] - input.getPosition());
                if (bytesToSkip2 < 0) {
                    Log.w(TAG, "Ignoring negative offset to sample data.");
                    bytesToSkip2 = 0;
                }
                input.skipFully(bytesToSkip2);
                this.currentTrackBundle = currentTrackBundle2;
            }
            this.sampleSize = this.currentTrackBundle.fragment.sampleSizeTable[this.currentTrackBundle.currentSampleIndex];
            if (this.currentTrackBundle.fragment.definesEncryptionData) {
                this.sampleBytesWritten = appendSampleEncryptionData(this.currentTrackBundle);
                this.sampleSize += this.sampleBytesWritten;
            } else {
                this.sampleBytesWritten = 0;
            }
            if (this.currentTrackBundle.track.sampleTransformation == 1) {
                this.sampleSize -= 8;
                input.skipFully(8);
            }
            this.parserState = 4;
            this.sampleCurrentNalBytesRemaining = 0;
        }
        TrackFragment fragment = this.currentTrackBundle.fragment;
        Track track = this.currentTrackBundle.track;
        TrackOutput output = this.currentTrackBundle.output;
        int sampleIndex = this.currentTrackBundle.currentSampleIndex;
        if (track.nalUnitLengthFieldLength != 0) {
            byte[] nalPrefixData = this.nalPrefix.data;
            nalPrefixData[0] = 0;
            nalPrefixData[1] = 0;
            nalPrefixData[2] = 0;
            int nalUnitPrefixLength = track.nalUnitLengthFieldLength + 1;
            int nalUnitLengthFieldLengthDiff = 4 - track.nalUnitLengthFieldLength;
            while (this.sampleBytesWritten < this.sampleSize) {
                if (this.sampleCurrentNalBytesRemaining == 0) {
                    input.readFully(nalPrefixData, nalUnitLengthFieldLengthDiff, nalUnitPrefixLength);
                    this.nalPrefix.setPosition(0);
                    this.sampleCurrentNalBytesRemaining = this.nalPrefix.readUnsignedIntToInt() - 1;
                    this.nalStartCode.setPosition(0);
                    output.sampleData(this.nalStartCode, 4);
                    output.sampleData(this.nalPrefix, 1);
                    this.processSeiNalUnitPayload = this.cea608TrackOutputs.length > 0 && NalUnitUtil.isNalUnitSei(track.format.sampleMimeType, nalPrefixData[4]);
                    this.sampleBytesWritten += 5;
                    this.sampleSize += nalUnitLengthFieldLengthDiff;
                } else {
                    if (this.processSeiNalUnitPayload) {
                        this.nalBuffer.reset(this.sampleCurrentNalBytesRemaining);
                        input.readFully(this.nalBuffer.data, 0, this.sampleCurrentNalBytesRemaining);
                        output.sampleData(this.nalBuffer, this.sampleCurrentNalBytesRemaining);
                        writtenBytes = this.sampleCurrentNalBytesRemaining;
                        int unescapedLength = NalUnitUtil.unescapeStream(this.nalBuffer.data, this.nalBuffer.limit());
                        this.nalBuffer.setPosition(MimeTypes.VIDEO_H265.equals(track.format.sampleMimeType) ? 1 : 0);
                        this.nalBuffer.setLimit(unescapedLength);
                        CeaUtil.consume(fragment.getSamplePresentationTime(sampleIndex) * 1000, this.nalBuffer, this.cea608TrackOutputs);
                    } else {
                        writtenBytes = output.sampleData(input, this.sampleCurrentNalBytesRemaining, false);
                    }
                    this.sampleBytesWritten += writtenBytes;
                    this.sampleCurrentNalBytesRemaining -= writtenBytes;
                }
            }
        } else {
            while (this.sampleBytesWritten < this.sampleSize) {
                this.sampleBytesWritten += output.sampleData(input, this.sampleSize - this.sampleBytesWritten, false);
            }
        }
        long sampleTimeUs = fragment.getSamplePresentationTime(sampleIndex) * 1000;
        if (this.timestampAdjuster != null) {
            sampleTimeUs = this.timestampAdjuster.adjustSampleTimestamp(sampleTimeUs);
        }
        int sampleFlags = fragment.sampleIsSyncFrameTable[sampleIndex] ? 1 : 0;
        TrackOutput.CryptoData cryptoData = null;
        if (fragment.definesEncryptionData) {
            sampleFlags |= 1073741824;
            if (fragment.trackEncryptionBox != null) {
                encryptionBox = fragment.trackEncryptionBox;
            } else {
                encryptionBox = track.getSampleDescriptionEncryptionBox(fragment.header.sampleDescriptionIndex);
            }
            cryptoData = encryptionBox.cryptoData;
        }
        output.sampleMetadata(sampleTimeUs, sampleFlags, this.sampleSize, 0, cryptoData);
        while (!this.pendingMetadataSampleInfos.isEmpty()) {
            MetadataSampleInfo sampleInfo = this.pendingMetadataSampleInfos.removeFirst();
            this.pendingMetadataSampleBytes -= sampleInfo.size;
            this.eventMessageTrackOutput.sampleMetadata(sampleInfo.presentationTimeDeltaUs + sampleTimeUs, 1, sampleInfo.size, this.pendingMetadataSampleBytes, (TrackOutput.CryptoData) null);
        }
        this.currentTrackBundle.currentSampleIndex++;
        this.currentTrackBundle.currentSampleInTrackRun++;
        if (this.currentTrackBundle.currentSampleInTrackRun == fragment.trunLength[this.currentTrackBundle.currentTrackRunIndex]) {
            this.currentTrackBundle.currentTrackRunIndex++;
            this.currentTrackBundle.currentSampleInTrackRun = 0;
            this.currentTrackBundle = null;
        }
        this.parserState = 3;
        return true;
    }

    private static TrackBundle getNextFragmentRun(SparseArray<TrackBundle> trackBundles2) {
        TrackBundle nextTrackBundle = null;
        long nextTrackRunOffset = Long.MAX_VALUE;
        int trackBundlesSize = trackBundles2.size();
        for (int i = 0; i < trackBundlesSize; i++) {
            TrackBundle trackBundle = trackBundles2.valueAt(i);
            if (trackBundle.currentTrackRunIndex != trackBundle.fragment.trunCount) {
                long trunOffset = trackBundle.fragment.trunDataPosition[trackBundle.currentTrackRunIndex];
                if (trunOffset < nextTrackRunOffset) {
                    nextTrackBundle = trackBundle;
                    nextTrackRunOffset = trunOffset;
                }
            }
        }
        return nextTrackBundle;
    }

    private int appendSampleEncryptionData(TrackBundle trackBundle) {
        TrackEncryptionBox encryptionBox;
        ParsableByteArray initializationVectorData;
        int vectorSize;
        int i;
        TrackFragment trackFragment = trackBundle.fragment;
        int sampleDescriptionIndex = trackFragment.header.sampleDescriptionIndex;
        if (trackFragment.trackEncryptionBox != null) {
            encryptionBox = trackFragment.trackEncryptionBox;
        } else {
            encryptionBox = trackBundle.track.getSampleDescriptionEncryptionBox(sampleDescriptionIndex);
        }
        if (encryptionBox.initializationVectorSize != 0) {
            initializationVectorData = trackFragment.sampleEncryptionData;
            vectorSize = encryptionBox.initializationVectorSize;
        } else {
            byte[] initVectorData = encryptionBox.defaultInitializationVector;
            this.defaultInitializationVector.reset(initVectorData, initVectorData.length);
            initializationVectorData = this.defaultInitializationVector;
            vectorSize = initVectorData.length;
        }
        boolean subsampleEncryption = trackFragment.sampleHasSubsampleEncryptionTable[trackBundle.currentSampleIndex];
        byte[] bArr = this.encryptionSignalByte.data;
        if (subsampleEncryption) {
            i = 128;
        } else {
            i = 0;
        }
        bArr[0] = (byte) (i | vectorSize);
        this.encryptionSignalByte.setPosition(0);
        TrackOutput output = trackBundle.output;
        output.sampleData(this.encryptionSignalByte, 1);
        output.sampleData(initializationVectorData, vectorSize);
        if (!subsampleEncryption) {
            return vectorSize + 1;
        }
        ParsableByteArray subsampleEncryptionData = trackFragment.sampleEncryptionData;
        int subsampleCount = subsampleEncryptionData.readUnsignedShort();
        subsampleEncryptionData.skipBytes(-2);
        int subsampleDataLength = (subsampleCount * 6) + 2;
        output.sampleData(subsampleEncryptionData, subsampleDataLength);
        return vectorSize + 1 + subsampleDataLength;
    }

    private static DrmInitData getDrmInitDataFromAtoms(List<Atom.LeafAtom> leafChildren) {
        ArrayList<DrmInitData.SchemeData> schemeDatas = null;
        int leafChildrenSize = leafChildren.size();
        for (int i = 0; i < leafChildrenSize; i++) {
            Atom.LeafAtom child = leafChildren.get(i);
            if (child.type == Atom.TYPE_pssh) {
                if (schemeDatas == null) {
                    schemeDatas = new ArrayList<>();
                }
                byte[] psshData = child.data.data;
                UUID uuid = PsshAtomUtil.parseUuid(psshData);
                if (uuid == null) {
                    Log.w(TAG, "Skipped pssh atom (failed to extract uuid)");
                } else {
                    schemeDatas.add(new DrmInitData.SchemeData(uuid, MimeTypes.VIDEO_MP4, psshData));
                }
            }
        }
        if (schemeDatas == null) {
            return null;
        }
        return new DrmInitData((List<DrmInitData.SchemeData>) schemeDatas);
    }

    private static boolean shouldParseLeafAtom(int atom) {
        return atom == Atom.TYPE_hdlr || atom == Atom.TYPE_mdhd || atom == Atom.TYPE_mvhd || atom == Atom.TYPE_sidx || atom == Atom.TYPE_stsd || atom == Atom.TYPE_tfdt || atom == Atom.TYPE_tfhd || atom == Atom.TYPE_tkhd || atom == Atom.TYPE_trex || atom == Atom.TYPE_trun || atom == Atom.TYPE_pssh || atom == Atom.TYPE_saiz || atom == Atom.TYPE_saio || atom == Atom.TYPE_senc || atom == Atom.TYPE_uuid || atom == Atom.TYPE_sbgp || atom == Atom.TYPE_sgpd || atom == Atom.TYPE_elst || atom == Atom.TYPE_mehd || atom == Atom.TYPE_emsg;
    }

    private static boolean shouldParseContainerAtom(int atom) {
        return atom == Atom.TYPE_moov || atom == Atom.TYPE_trak || atom == Atom.TYPE_mdia || atom == Atom.TYPE_minf || atom == Atom.TYPE_stbl || atom == Atom.TYPE_moof || atom == Atom.TYPE_traf || atom == Atom.TYPE_mvex || atom == Atom.TYPE_edts;
    }

    private static final class MetadataSampleInfo {
        public final long presentationTimeDeltaUs;
        public final int size;

        public MetadataSampleInfo(long presentationTimeDeltaUs2, int size2) {
            this.presentationTimeDeltaUs = presentationTimeDeltaUs2;
            this.size = size2;
        }
    }

    private static final class TrackBundle {
        public int currentSampleInTrackRun;
        public int currentSampleIndex;
        public int currentTrackRunIndex;
        public DefaultSampleValues defaultSampleValues;
        public final TrackFragment fragment = new TrackFragment();
        public final TrackOutput output;
        public Track track;

        public TrackBundle(TrackOutput output2) {
            this.output = output2;
        }

        public void init(Track track2, DefaultSampleValues defaultSampleValues2) {
            this.track = (Track) Assertions.checkNotNull(track2);
            this.defaultSampleValues = (DefaultSampleValues) Assertions.checkNotNull(defaultSampleValues2);
            this.output.format(track2.format);
            reset();
        }

        public void reset() {
            this.fragment.reset();
            this.currentSampleIndex = 0;
            this.currentTrackRunIndex = 0;
            this.currentSampleInTrackRun = 0;
        }

        public void updateDrmInitData(DrmInitData drmInitData) {
            TrackEncryptionBox encryptionBox = this.track.getSampleDescriptionEncryptionBox(this.fragment.header.sampleDescriptionIndex);
            this.output.format(this.track.format.copyWithDrmInitData(drmInitData.copyWithSchemeType(encryptionBox != null ? encryptionBox.schemeType : null)));
        }
    }
}
