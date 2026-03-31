package com.google.android.exoplayer2.extractor.mp4;

import android.util.Pair;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.extractor.mp4.Atom;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class AtomParsers {
    private static final String TAG = "AtomParsers";
    private static final int TYPE_cenc = Util.getIntegerCodeForString(C.CENC_TYPE_cenc);
    private static final int TYPE_clcp = Util.getIntegerCodeForString("clcp");
    private static final int TYPE_meta = Util.getIntegerCodeForString("meta");
    private static final int TYPE_sbtl = Util.getIntegerCodeForString("sbtl");
    private static final int TYPE_soun = Util.getIntegerCodeForString("soun");
    private static final int TYPE_subt = Util.getIntegerCodeForString("subt");
    private static final int TYPE_text = Util.getIntegerCodeForString(MimeTypes.BASE_TYPE_TEXT);
    private static final int TYPE_vide = Util.getIntegerCodeForString("vide");

    private interface SampleSizeBox {
        int getSampleCount();

        boolean isFixedSampleSize();

        int readNextSampleSize();
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v21, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r22v2, resolved type: long[]} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v23, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r23v2, resolved type: long[]} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static com.google.android.exoplayer2.extractor.mp4.Track parseTrak(com.google.android.exoplayer2.extractor.mp4.Atom.ContainerAtom r32, com.google.android.exoplayer2.extractor.mp4.Atom.LeafAtom r33, long r34, com.google.android.exoplayer2.drm.DrmInitData r36, boolean r37, boolean r38) throws com.google.android.exoplayer2.ParserException {
        /*
            int r2 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_mdia
            r0 = r32
            com.google.android.exoplayer2.extractor.mp4.Atom$ContainerAtom r26 = r0.getContainerAtomOfType(r2)
            int r2 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_hdlr
            r0 = r26
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r2 = r0.getLeafAtomOfType(r2)
            com.google.android.exoplayer2.util.ParsableByteArray r2 = r2.data
            int r30 = parseHdlr(r2)
            r2 = -1
            r0 = r30
            if (r0 != r2) goto L_0x001d
            r9 = 0
        L_0x001c:
            return r9
        L_0x001d:
            int r2 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_tkhd
            r0 = r32
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r2 = r0.getLeafAtomOfType(r2)
            com.google.android.exoplayer2.util.ParsableByteArray r2 = r2.data
            com.google.android.exoplayer2.extractor.mp4.AtomParsers$TkhdData r29 = parseTkhd(r2)
            r2 = -9223372036854775807(0x8000000000000001, double:-4.9E-324)
            int r2 = (r34 > r2 ? 1 : (r34 == r2 ? 0 : -1))
            if (r2 != 0) goto L_0x0038
            long r34 = r29.duration
        L_0x0038:
            r0 = r33
            com.google.android.exoplayer2.util.ParsableByteArray r2 = r0.data
            long r6 = parseMvhd(r2)
            r2 = -9223372036854775807(0x8000000000000001, double:-4.9E-324)
            int r2 = (r34 > r2 ? 1 : (r34 == r2 ? 0 : -1))
            if (r2 != 0) goto L_0x00b5
            r16 = -9223372036854775807(0x8000000000000001, double:-4.9E-324)
        L_0x004e:
            int r2 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_minf
            r0 = r26
            com.google.android.exoplayer2.extractor.mp4.Atom$ContainerAtom r2 = r0.getContainerAtomOfType(r2)
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_stbl
            com.google.android.exoplayer2.extractor.mp4.Atom$ContainerAtom r27 = r2.getContainerAtomOfType(r3)
            int r2 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_mdhd
            r0 = r26
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r2 = r0.getLeafAtomOfType(r2)
            com.google.android.exoplayer2.util.ParsableByteArray r2 = r2.data
            android.util.Pair r25 = parseMdhd(r2)
            int r2 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_stsd
            r0 = r27
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r2 = r0.getLeafAtomOfType(r2)
            com.google.android.exoplayer2.util.ParsableByteArray r8 = r2.data
            int r9 = r29.id
            int r10 = r29.rotationDegrees
            r0 = r25
            java.lang.Object r11 = r0.second
            java.lang.String r11 = (java.lang.String) r11
            r12 = r36
            r13 = r38
            com.google.android.exoplayer2.extractor.mp4.AtomParsers$StsdData r28 = parseStsd(r8, r9, r10, r11, r12, r13)
            r22 = 0
            r23 = 0
            if (r37 != 0) goto L_0x00ac
            int r2 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_edts
            r0 = r32
            com.google.android.exoplayer2.extractor.mp4.Atom$ContainerAtom r2 = r0.getContainerAtomOfType(r2)
            android.util.Pair r24 = parseEdts(r2)
            r0 = r24
            java.lang.Object r0 = r0.first
            r22 = r0
            long[] r22 = (long[]) r22
            r0 = r24
            java.lang.Object r0 = r0.second
            r23 = r0
            long[] r23 = (long[]) r23
        L_0x00ac:
            r0 = r28
            com.google.android.exoplayer2.Format r2 = r0.format
            if (r2 != 0) goto L_0x00bf
            r9 = 0
            goto L_0x001c
        L_0x00b5:
            r4 = 1000000(0xf4240, double:4.940656E-318)
            r2 = r34
            long r16 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r2, r4, r6)
            goto L_0x004e
        L_0x00bf:
            com.google.android.exoplayer2.extractor.mp4.Track r9 = new com.google.android.exoplayer2.extractor.mp4.Track
            int r10 = r29.id
            r0 = r25
            java.lang.Object r2 = r0.first
            java.lang.Long r2 = (java.lang.Long) r2
            long r12 = r2.longValue()
            r0 = r28
            com.google.android.exoplayer2.Format r0 = r0.format
            r18 = r0
            r0 = r28
            int r0 = r0.requiredSampleTransformation
            r19 = r0
            r0 = r28
            com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox[] r0 = r0.trackEncryptionBoxes
            r20 = r0
            r0 = r28
            int r0 = r0.nalUnitLengthFieldLength
            r21 = r0
            r11 = r30
            r14 = r6
            r9.<init>(r10, r11, r12, r14, r16, r18, r19, r20, r21, r22, r23)
            goto L_0x001c
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.extractor.mp4.AtomParsers.parseTrak(com.google.android.exoplayer2.extractor.mp4.Atom$ContainerAtom, com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom, long, com.google.android.exoplayer2.drm.DrmInitData, boolean, boolean):com.google.android.exoplayer2.extractor.mp4.Track");
    }

    /* JADX WARNING: type inference failed for: r74v1, types: [com.google.android.exoplayer2.extractor.mp4.AtomParsers$SampleSizeBox] */
    /* JADX WARNING: type inference failed for: r0v116, types: [com.google.android.exoplayer2.extractor.mp4.AtomParsers$Stz2SampleSizeBox] */
    /* JADX WARNING: type inference failed for: r0v117, types: [com.google.android.exoplayer2.extractor.mp4.AtomParsers$StszSampleSizeBox] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static com.google.android.exoplayer2.extractor.mp4.TrackSampleTable parseStbl(com.google.android.exoplayer2.extractor.mp4.Track r88, com.google.android.exoplayer2.extractor.mp4.Atom.ContainerAtom r89, com.google.android.exoplayer2.extractor.GaplessInfoHolder r90) throws com.google.android.exoplayer2.ParserException {
        /*
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_stsz
            r0 = r89
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r79 = r0.getLeafAtomOfType(r4)
            if (r79 == 0) goto L_0x002c
            com.google.android.exoplayer2.extractor.mp4.AtomParsers$StszSampleSizeBox r74 = new com.google.android.exoplayer2.extractor.mp4.AtomParsers$StszSampleSizeBox
            r0 = r74
            r1 = r79
            r0.<init>(r1)
        L_0x0013:
            int r72 = r74.getSampleCount()
            if (r72 != 0) goto L_0x0048
            com.google.android.exoplayer2.extractor.mp4.TrackSampleTable r4 = new com.google.android.exoplayer2.extractor.mp4.TrackSampleTable
            r12 = 0
            long[] r5 = new long[r12]
            r12 = 0
            int[] r6 = new int[r12]
            r7 = 0
            r12 = 0
            long[] r8 = new long[r12]
            r12 = 0
            int[] r9 = new int[r12]
            r4.<init>(r5, r6, r7, r8, r9)
        L_0x002b:
            return r4
        L_0x002c:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_stz2
            r0 = r89
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r81 = r0.getLeafAtomOfType(r4)
            if (r81 != 0) goto L_0x003e
            com.google.android.exoplayer2.ParserException r4 = new com.google.android.exoplayer2.ParserException
            java.lang.String r12 = "Track has no sample table size information"
            r4.<init>((java.lang.String) r12)
            throw r4
        L_0x003e:
            com.google.android.exoplayer2.extractor.mp4.AtomParsers$Stz2SampleSizeBox r74 = new com.google.android.exoplayer2.extractor.mp4.AtomParsers$Stz2SampleSizeBox
            r0 = r74
            r1 = r81
            r0.<init>(r1)
            goto L_0x0013
        L_0x0048:
            r20 = 0
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_stco
            r0 = r89
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r21 = r0.getLeafAtomOfType(r4)
            if (r21 != 0) goto L_0x005e
            r20 = 1
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_co64
            r0 = r89
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r21 = r0.getLeafAtomOfType(r4)
        L_0x005e:
            r0 = r21
            com.google.android.exoplayer2.util.ParsableByteArray r0 = r0.data
            r19 = r0
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_stsc
            r0 = r89
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r4 = r0.getLeafAtomOfType(r4)
            com.google.android.exoplayer2.util.ParsableByteArray r0 = r4.data
            r76 = r0
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_stts
            r0 = r89
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r4 = r0.getLeafAtomOfType(r4)
            com.google.android.exoplayer2.util.ParsableByteArray r0 = r4.data
            r80 = r0
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_stss
            r0 = r89
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r78 = r0.getLeafAtomOfType(r4)
            if (r78 == 0) goto L_0x013d
            r0 = r78
            com.google.android.exoplayer2.util.ParsableByteArray r0 = r0.data
            r77 = r0
        L_0x008c:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_ctts
            r0 = r89
            com.google.android.exoplayer2.extractor.mp4.Atom$LeafAtom r27 = r0.getLeafAtomOfType(r4)
            if (r27 == 0) goto L_0x0141
            r0 = r27
            com.google.android.exoplayer2.util.ParsableByteArray r0 = r0.data
            r26 = r0
        L_0x009c:
            com.google.android.exoplayer2.extractor.mp4.AtomParsers$ChunkIterator r18 = new com.google.android.exoplayer2.extractor.mp4.AtomParsers$ChunkIterator
            r0 = r18
            r1 = r76
            r2 = r19
            r3 = r20
            r0.<init>(r1, r2, r3)
            r4 = 12
            r0 = r80
            r0.setPosition(r4)
            int r4 = r80.readUnsignedIntToInt()
            int r70 = r4 + -1
            int r66 = r80.readUnsignedIntToInt()
            int r84 = r80.readUnsignedIntToInt()
            r67 = 0
            r71 = 0
            r85 = 0
            if (r26 == 0) goto L_0x00d1
            r4 = 12
            r0 = r26
            r0.setPosition(r4)
            int r71 = r26.readUnsignedIntToInt()
        L_0x00d1:
            r55 = -1
            r69 = 0
            if (r77 == 0) goto L_0x00ea
            r4 = 12
            r0 = r77
            r0.setPosition(r4)
            int r69 = r77.readUnsignedIntToInt()
            if (r69 <= 0) goto L_0x0145
            int r4 = r77.readUnsignedIntToInt()
            int r55 = r4 + -1
        L_0x00ea:
            boolean r4 = r74.isFixedSampleSize()
            if (r4 == 0) goto L_0x0148
            java.lang.String r4 = "audio/raw"
            r0 = r88
            com.google.android.exoplayer2.Format r12 = r0.format
            java.lang.String r12 = r12.sampleMimeType
            boolean r4 = r4.equals(r12)
            if (r4 == 0) goto L_0x0148
            if (r70 != 0) goto L_0x0148
            if (r71 != 0) goto L_0x0148
            if (r69 != 0) goto L_0x0148
            r48 = 1
        L_0x0106:
            r7 = 0
            r86 = 0
            if (r48 != 0) goto L_0x023a
            r0 = r72
            long[] r5 = new long[r0]
            r0 = r72
            int[] r6 = new int[r0]
            r0 = r72
            long[] r8 = new long[r0]
            r0 = r72
            int[] r9 = new int[r0]
            r56 = 0
            r68 = 0
            r47 = 0
        L_0x0121:
            r0 = r47
            r1 = r72
            if (r0 >= r1) goto L_0x01ad
        L_0x0127:
            if (r68 != 0) goto L_0x014b
            boolean r4 = r18.moveNext()
            com.google.android.exoplayer2.util.Assertions.checkState(r4)
            r0 = r18
            long r0 = r0.offset
            r56 = r0
            r0 = r18
            int r0 = r0.numSamples
            r68 = r0
            goto L_0x0127
        L_0x013d:
            r77 = 0
            goto L_0x008c
        L_0x0141:
            r26 = 0
            goto L_0x009c
        L_0x0145:
            r77 = 0
            goto L_0x00ea
        L_0x0148:
            r48 = 0
            goto L_0x0106
        L_0x014b:
            if (r26 == 0) goto L_0x015e
        L_0x014d:
            if (r67 != 0) goto L_0x015c
            if (r71 <= 0) goto L_0x015c
            int r67 = r26.readUnsignedIntToInt()
            int r85 = r26.readInt()
            int r71 = r71 + -1
            goto L_0x014d
        L_0x015c:
            int r67 = r67 + -1
        L_0x015e:
            r5[r47] = r56
            int r4 = r74.readNextSampleSize()
            r6[r47] = r4
            r4 = r6[r47]
            if (r4 <= r7) goto L_0x016c
            r7 = r6[r47]
        L_0x016c:
            r0 = r85
            long r12 = (long) r0
            long r12 = r12 + r86
            r8[r47] = r12
            if (r77 != 0) goto L_0x01ab
            r4 = 1
        L_0x0176:
            r9[r47] = r4
            r0 = r47
            r1 = r55
            if (r0 != r1) goto L_0x018b
            r4 = 1
            r9[r47] = r4
            int r69 = r69 + -1
            if (r69 <= 0) goto L_0x018b
            int r4 = r77.readUnsignedIntToInt()
            int r55 = r4 + -1
        L_0x018b:
            r0 = r84
            long r12 = (long) r0
            long r86 = r86 + r12
            int r66 = r66 + -1
            if (r66 != 0) goto L_0x01a0
            if (r70 <= 0) goto L_0x01a0
            int r66 = r80.readUnsignedIntToInt()
            int r84 = r80.readInt()
            int r70 = r70 + -1
        L_0x01a0:
            r4 = r6[r47]
            long r12 = (long) r4
            long r56 = r56 + r12
            int r68 = r68 + -1
            int r47 = r47 + 1
            goto L_0x0121
        L_0x01ab:
            r4 = 0
            goto L_0x0176
        L_0x01ad:
            if (r67 != 0) goto L_0x01c5
            r4 = 1
        L_0x01b0:
            com.google.android.exoplayer2.util.Assertions.checkArgument(r4)
        L_0x01b3:
            if (r71 <= 0) goto L_0x01c9
            int r4 = r26.readUnsignedIntToInt()
            if (r4 != 0) goto L_0x01c7
            r4 = 1
        L_0x01bc:
            com.google.android.exoplayer2.util.Assertions.checkArgument(r4)
            r26.readInt()
            int r71 = r71 + -1
            goto L_0x01b3
        L_0x01c5:
            r4 = 0
            goto L_0x01b0
        L_0x01c7:
            r4 = 0
            goto L_0x01bc
        L_0x01c9:
            if (r69 != 0) goto L_0x01d1
            if (r66 != 0) goto L_0x01d1
            if (r68 != 0) goto L_0x01d1
            if (r70 == 0) goto L_0x021d
        L_0x01d1:
            java.lang.String r4 = "AtomParsers"
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
            java.lang.String r13 = "Inconsistent stbl box for track "
            java.lang.StringBuilder r12 = r12.append(r13)
            r0 = r88
            int r13 = r0.id
            java.lang.StringBuilder r12 = r12.append(r13)
            java.lang.String r13 = ": remainingSynchronizationSamples "
            java.lang.StringBuilder r12 = r12.append(r13)
            r0 = r69
            java.lang.StringBuilder r12 = r12.append(r0)
            java.lang.String r13 = ", remainingSamplesAtTimestampDelta "
            java.lang.StringBuilder r12 = r12.append(r13)
            r0 = r66
            java.lang.StringBuilder r12 = r12.append(r0)
            java.lang.String r13 = ", remainingSamplesInChunk "
            java.lang.StringBuilder r12 = r12.append(r13)
            r0 = r68
            java.lang.StringBuilder r12 = r12.append(r0)
            java.lang.String r13 = ", remainingTimestampDeltaChanges "
            java.lang.StringBuilder r12 = r12.append(r13)
            r0 = r70
            java.lang.StringBuilder r12 = r12.append(r0)
            java.lang.String r12 = r12.toString()
            android.util.Log.w(r4, r12)
        L_0x021d:
            r0 = r88
            long[] r4 = r0.editListDurations
            if (r4 == 0) goto L_0x0229
            boolean r4 = r90.hasGaplessInfo()
            if (r4 == 0) goto L_0x028b
        L_0x0229:
            r12 = 1000000(0xf4240, double:4.940656E-318)
            r0 = r88
            long r14 = r0.timescale
            com.google.android.exoplayer2.util.Util.scaleLargeTimestampsInPlace(r8, r12, r14)
            com.google.android.exoplayer2.extractor.mp4.TrackSampleTable r4 = new com.google.android.exoplayer2.extractor.mp4.TrackSampleTable
            r4.<init>(r5, r6, r7, r8, r9)
            goto L_0x002b
        L_0x023a:
            r0 = r18
            int r4 = r0.length
            long[] r0 = new long[r4]
            r22 = r0
            r0 = r18
            int r4 = r0.length
            int[] r0 = new int[r4]
            r23 = r0
        L_0x024a:
            boolean r4 = r18.moveNext()
            if (r4 == 0) goto L_0x0265
            r0 = r18
            int r4 = r0.index
            r0 = r18
            long r12 = r0.offset
            r22[r4] = r12
            r0 = r18
            int r4 = r0.index
            r0 = r18
            int r12 = r0.numSamples
            r23[r4] = r12
            goto L_0x024a
        L_0x0265:
            int r43 = r74.readNextSampleSize()
            r0 = r84
            long r12 = (long) r0
            r0 = r43
            r1 = r22
            r2 = r23
            com.google.android.exoplayer2.extractor.mp4.FixedSampleSizeRechunker$Results r59 = com.google.android.exoplayer2.extractor.mp4.FixedSampleSizeRechunker.rechunk(r0, r1, r2, r12)
            r0 = r59
            long[] r5 = r0.offsets
            r0 = r59
            int[] r6 = r0.sizes
            r0 = r59
            int r7 = r0.maximumSize
            r0 = r59
            long[] r8 = r0.timestamps
            r0 = r59
            int[] r9 = r0.flags
            goto L_0x021d
        L_0x028b:
            r0 = r88
            long[] r4 = r0.editListDurations
            int r4 = r4.length
            r12 = 1
            if (r4 != r12) goto L_0x0337
            r0 = r88
            int r4 = r0.type
            r12 = 1
            if (r4 != r12) goto L_0x0337
            int r4 = r8.length
            r12 = 2
            if (r4 < r12) goto L_0x0337
            r0 = r88
            long[] r4 = r0.editListMediaTimes
            r12 = 0
            r30 = r4[r12]
            r0 = r88
            long[] r4 = r0.editListDurations
            r12 = 0
            r10 = r4[r12]
            r0 = r88
            long r12 = r0.timescale
            r0 = r88
            long r14 = r0.movieTimescale
            long r12 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r10, r12, r14)
            long r28 = r30 + r12
            r50 = r86
            r4 = 0
            r12 = r8[r4]
            int r4 = (r12 > r30 ? 1 : (r12 == r30 ? 0 : -1))
            if (r4 > 0) goto L_0x0337
            r4 = 1
            r12 = r8[r4]
            int r4 = (r30 > r12 ? 1 : (r30 == r12 ? 0 : -1))
            if (r4 >= 0) goto L_0x0337
            int r4 = r8.length
            int r4 = r4 + -1
            r12 = r8[r4]
            int r4 = (r12 > r28 ? 1 : (r12 == r28 ? 0 : -1))
            if (r4 >= 0) goto L_0x0337
            int r4 = (r28 > r50 ? 1 : (r28 == r50 ? 0 : -1))
            if (r4 > 0) goto L_0x0337
            long r60 = r50 - r28
            r4 = 0
            r12 = r8[r4]
            long r10 = r30 - r12
            r0 = r88
            com.google.android.exoplayer2.Format r4 = r0.format
            int r4 = r4.sampleRate
            long r12 = (long) r4
            r0 = r88
            long r14 = r0.timescale
            long r38 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r10, r12, r14)
            r0 = r88
            com.google.android.exoplayer2.Format r4 = r0.format
            int r4 = r4.sampleRate
            long r12 = (long) r4
            r0 = r88
            long r14 = r0.timescale
            r10 = r60
            long r40 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r10, r12, r14)
            r12 = 0
            int r4 = (r38 > r12 ? 1 : (r38 == r12 ? 0 : -1))
            if (r4 != 0) goto L_0x030a
            r12 = 0
            int r4 = (r40 > r12 ? 1 : (r40 == r12 ? 0 : -1))
            if (r4 == 0) goto L_0x0337
        L_0x030a:
            r12 = 2147483647(0x7fffffff, double:1.060997895E-314)
            int r4 = (r38 > r12 ? 1 : (r38 == r12 ? 0 : -1))
            if (r4 > 0) goto L_0x0337
            r12 = 2147483647(0x7fffffff, double:1.060997895E-314)
            int r4 = (r40 > r12 ? 1 : (r40 == r12 ? 0 : -1))
            if (r4 > 0) goto L_0x0337
            r0 = r38
            int r4 = (int) r0
            r0 = r90
            r0.encoderDelay = r4
            r0 = r40
            int r4 = (int) r0
            r0 = r90
            r0.encoderPadding = r4
            r12 = 1000000(0xf4240, double:4.940656E-318)
            r0 = r88
            long r14 = r0.timescale
            com.google.android.exoplayer2.util.Util.scaleLargeTimestampsInPlace(r8, r12, r14)
            com.google.android.exoplayer2.extractor.mp4.TrackSampleTable r4 = new com.google.android.exoplayer2.extractor.mp4.TrackSampleTable
            r4.<init>(r5, r6, r7, r8, r9)
            goto L_0x002b
        L_0x0337:
            r0 = r88
            long[] r4 = r0.editListDurations
            int r4 = r4.length
            r12 = 1
            if (r4 != r12) goto L_0x0375
            r0 = r88
            long[] r4 = r0.editListDurations
            r12 = 0
            r12 = r4[r12]
            r14 = 0
            int r4 = (r12 > r14 ? 1 : (r12 == r14 ? 0 : -1))
            if (r4 != 0) goto L_0x0375
            r47 = 0
        L_0x034e:
            int r4 = r8.length
            r0 = r47
            if (r0 >= r4) goto L_0x036e
            r12 = r8[r47]
            r0 = r88
            long[] r4 = r0.editListMediaTimes
            r14 = 0
            r14 = r4[r14]
            long r10 = r12 - r14
            r12 = 1000000(0xf4240, double:4.940656E-318)
            r0 = r88
            long r14 = r0.timescale
            long r12 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r10, r12, r14)
            r8[r47] = r12
            int r47 = r47 + 1
            goto L_0x034e
        L_0x036e:
            com.google.android.exoplayer2.extractor.mp4.TrackSampleTable r4 = new com.google.android.exoplayer2.extractor.mp4.TrackSampleTable
            r4.<init>(r5, r6, r7, r8, r9)
            goto L_0x002b
        L_0x0375:
            r0 = r88
            int r4 = r0.type
            r12 = 1
            if (r4 != r12) goto L_0x03d0
            r58 = 1
        L_0x037e:
            r35 = 0
            r54 = 0
            r24 = 0
            r47 = 0
        L_0x0386:
            r0 = r88
            long[] r4 = r0.editListDurations
            int r4 = r4.length
            r0 = r47
            if (r0 >= r4) goto L_0x03d5
            r0 = r88
            long[] r4 = r0.editListMediaTimes
            r52 = r4[r47]
            r12 = -1
            int r4 = (r52 > r12 ? 1 : (r52 == r12 ? 0 : -1))
            if (r4 == 0) goto L_0x03cd
            r0 = r88
            long[] r4 = r0.editListDurations
            r10 = r4[r47]
            r0 = r88
            long r12 = r0.timescale
            r0 = r88
            long r14 = r0.movieTimescale
            long r10 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r10, r12, r14)
            r4 = 1
            r12 = 1
            r0 = r52
            int r75 = com.google.android.exoplayer2.util.Util.binarySearchCeil((long[]) r8, (long) r0, (boolean) r4, (boolean) r12)
            long r12 = r52 + r10
            r4 = 0
            r0 = r58
            int r42 = com.google.android.exoplayer2.util.Util.binarySearchCeil((long[]) r8, (long) r12, (boolean) r0, (boolean) r4)
            int r4 = r42 - r75
            int r35 = r35 + r4
            r0 = r54
            r1 = r75
            if (r0 == r1) goto L_0x03d3
            r4 = 1
        L_0x03c9:
            r24 = r24 | r4
            r54 = r42
        L_0x03cd:
            int r47 = r47 + 1
            goto L_0x0386
        L_0x03d0:
            r58 = 0
            goto L_0x037e
        L_0x03d3:
            r4 = 0
            goto L_0x03c9
        L_0x03d5:
            r0 = r35
            r1 = r72
            if (r0 == r1) goto L_0x04a0
            r4 = 1
        L_0x03dc:
            r24 = r24 | r4
            if (r24 == 0) goto L_0x04a3
            r0 = r35
            long[] r0 = new long[r0]
            r34 = r0
        L_0x03e6:
            if (r24 == 0) goto L_0x04a7
            r0 = r35
            int[] r0 = new int[r0]
            r36 = r0
        L_0x03ee:
            if (r24 == 0) goto L_0x04ab
            r33 = 0
        L_0x03f2:
            if (r24 == 0) goto L_0x04af
            r0 = r35
            int[] r0 = new int[r0]
            r32 = r0
        L_0x03fa:
            r0 = r35
            long[] r0 = new long[r0]
            r37 = r0
            r62 = 0
            r73 = 0
            r47 = 0
        L_0x0406:
            r0 = r88
            long[] r4 = r0.editListDurations
            int r4 = r4.length
            r0 = r47
            if (r0 >= r4) goto L_0x04b9
            r0 = r88
            long[] r4 = r0.editListMediaTimes
            r52 = r4[r47]
            r0 = r88
            long[] r4 = r0.editListDurations
            r10 = r4[r47]
            r12 = -1
            int r4 = (r52 > r12 ? 1 : (r52 == r12 ? 0 : -1))
            if (r4 == 0) goto L_0x04b3
            r0 = r88
            long r12 = r0.timescale
            r0 = r88
            long r14 = r0.movieTimescale
            long r12 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r10, r12, r14)
            long r44 = r52 + r12
            r4 = 1
            r12 = 1
            r0 = r52
            int r75 = com.google.android.exoplayer2.util.Util.binarySearchCeil((long[]) r8, (long) r0, (boolean) r4, (boolean) r12)
            r4 = 0
            r0 = r44
            r2 = r58
            int r42 = com.google.android.exoplayer2.util.Util.binarySearchCeil((long[]) r8, (long) r0, (boolean) r2, (boolean) r4)
            if (r24 == 0) goto L_0x0465
            int r25 = r42 - r75
            r0 = r75
            r1 = r34
            r2 = r73
            r3 = r25
            java.lang.System.arraycopy(r5, r0, r1, r2, r3)
            r0 = r75
            r1 = r36
            r2 = r73
            r3 = r25
            java.lang.System.arraycopy(r6, r0, r1, r2, r3)
            r0 = r75
            r1 = r32
            r2 = r73
            r3 = r25
            java.lang.System.arraycopy(r9, r0, r1, r2, r3)
        L_0x0465:
            r49 = r75
        L_0x0467:
            r0 = r49
            r1 = r42
            if (r0 >= r1) goto L_0x04b3
            r14 = 1000000(0xf4240, double:4.940656E-318)
            r0 = r88
            long r0 = r0.movieTimescale
            r16 = r0
            r12 = r62
            long r64 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r12, r14, r16)
            r12 = r8[r49]
            long r12 = r12 - r52
            r14 = 1000000(0xf4240, double:4.940656E-318)
            r0 = r88
            long r0 = r0.timescale
            r16 = r0
            long r82 = com.google.android.exoplayer2.util.Util.scaleLargeTimestamp(r12, r14, r16)
            long r12 = r64 + r82
            r37[r73] = r12
            if (r24 == 0) goto L_0x049b
            r4 = r36[r73]
            r0 = r33
            if (r4 <= r0) goto L_0x049b
            r33 = r6[r49]
        L_0x049b:
            int r73 = r73 + 1
            int r49 = r49 + 1
            goto L_0x0467
        L_0x04a0:
            r4 = 0
            goto L_0x03dc
        L_0x04a3:
            r34 = r5
            goto L_0x03e6
        L_0x04a7:
            r36 = r6
            goto L_0x03ee
        L_0x04ab:
            r33 = r7
            goto L_0x03f2
        L_0x04af:
            r32 = r9
            goto L_0x03fa
        L_0x04b3:
            long r62 = r62 + r10
            int r47 = r47 + 1
            goto L_0x0406
        L_0x04b9:
            r46 = 0
            r47 = 0
        L_0x04bd:
            r0 = r32
            int r4 = r0.length
            r0 = r47
            if (r0 >= r4) goto L_0x04d4
            if (r46 != 0) goto L_0x04d4
            r4 = r32[r47]
            r4 = r4 & 1
            if (r4 == 0) goto L_0x04d2
            r4 = 1
        L_0x04cd:
            r46 = r46 | r4
            int r47 = r47 + 1
            goto L_0x04bd
        L_0x04d2:
            r4 = 0
            goto L_0x04cd
        L_0x04d4:
            if (r46 != 0) goto L_0x04ee
            java.lang.String r4 = "AtomParsers"
            java.lang.String r12 = "Ignoring edit list: Edited sample sequence does not contain a sync sample."
            android.util.Log.w(r4, r12)
            r12 = 1000000(0xf4240, double:4.940656E-318)
            r0 = r88
            long r14 = r0.timescale
            com.google.android.exoplayer2.util.Util.scaleLargeTimestampsInPlace(r8, r12, r14)
            com.google.android.exoplayer2.extractor.mp4.TrackSampleTable r4 = new com.google.android.exoplayer2.extractor.mp4.TrackSampleTable
            r4.<init>(r5, r6, r7, r8, r9)
            goto L_0x002b
        L_0x04ee:
            com.google.android.exoplayer2.extractor.mp4.TrackSampleTable r12 = new com.google.android.exoplayer2.extractor.mp4.TrackSampleTable
            r13 = r34
            r14 = r36
            r15 = r33
            r16 = r37
            r17 = r32
            r12.<init>(r13, r14, r15, r16, r17)
            r4 = r12
            goto L_0x002b
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.extractor.mp4.AtomParsers.parseStbl(com.google.android.exoplayer2.extractor.mp4.Track, com.google.android.exoplayer2.extractor.mp4.Atom$ContainerAtom, com.google.android.exoplayer2.extractor.GaplessInfoHolder):com.google.android.exoplayer2.extractor.mp4.TrackSampleTable");
    }

    public static Metadata parseUdta(Atom.LeafAtom udtaAtom, boolean isQuickTime) {
        if (isQuickTime) {
            return null;
        }
        ParsableByteArray udtaData = udtaAtom.data;
        udtaData.setPosition(8);
        while (udtaData.bytesLeft() >= 8) {
            int atomPosition = udtaData.getPosition();
            int atomSize = udtaData.readInt();
            if (udtaData.readInt() == Atom.TYPE_meta) {
                udtaData.setPosition(atomPosition);
                return parseMetaAtom(udtaData, atomPosition + atomSize);
            }
            udtaData.skipBytes(atomSize - 8);
        }
        return null;
    }

    private static Metadata parseMetaAtom(ParsableByteArray meta, int limit) {
        meta.skipBytes(12);
        while (meta.getPosition() < limit) {
            int atomPosition = meta.getPosition();
            int atomSize = meta.readInt();
            if (meta.readInt() == Atom.TYPE_ilst) {
                meta.setPosition(atomPosition);
                return parseIlst(meta, atomPosition + atomSize);
            }
            meta.skipBytes(atomSize - 8);
        }
        return null;
    }

    private static Metadata parseIlst(ParsableByteArray ilst, int limit) {
        ilst.skipBytes(8);
        ArrayList<Metadata.Entry> entries = new ArrayList<>();
        while (ilst.getPosition() < limit) {
            Metadata.Entry entry = MetadataUtil.parseIlstElement(ilst);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        return new Metadata((List<? extends Metadata.Entry>) entries);
    }

    private static long parseMvhd(ParsableByteArray mvhd) {
        int i = 8;
        mvhd.setPosition(8);
        if (Atom.parseFullAtomVersion(mvhd.readInt()) != 0) {
            i = 16;
        }
        mvhd.skipBytes(i);
        return mvhd.readUnsignedInt();
    }

    private static TkhdData parseTkhd(ParsableByteArray tkhd) {
        long duration;
        int rotationDegrees;
        tkhd.setPosition(8);
        int version = Atom.parseFullAtomVersion(tkhd.readInt());
        tkhd.skipBytes(version == 0 ? 8 : 16);
        int trackId = tkhd.readInt();
        tkhd.skipBytes(4);
        boolean durationUnknown = true;
        int durationPosition = tkhd.getPosition();
        int durationByteCount = version == 0 ? 4 : 8;
        int i = 0;
        while (true) {
            if (i >= durationByteCount) {
                break;
            } else if (tkhd.data[durationPosition + i] != -1) {
                durationUnknown = false;
                break;
            } else {
                i++;
            }
        }
        if (durationUnknown) {
            tkhd.skipBytes(durationByteCount);
            duration = C.TIME_UNSET;
        } else {
            duration = version == 0 ? tkhd.readUnsignedInt() : tkhd.readUnsignedLongToLong();
            if (duration == 0) {
                duration = C.TIME_UNSET;
            }
        }
        tkhd.skipBytes(16);
        int a00 = tkhd.readInt();
        int a01 = tkhd.readInt();
        tkhd.skipBytes(4);
        int a10 = tkhd.readInt();
        int a11 = tkhd.readInt();
        if (a00 == 0 && a01 == 65536 && a10 == (-65536) && a11 == 0) {
            rotationDegrees = 90;
        } else if (a00 == 0 && a01 == (-65536) && a10 == 65536 && a11 == 0) {
            rotationDegrees = 270;
        } else if (a00 == (-65536) && a01 == 0 && a10 == 0 && a11 == (-65536)) {
            rotationDegrees = 180;
        } else {
            rotationDegrees = 0;
        }
        return new TkhdData(trackId, duration, rotationDegrees);
    }

    private static int parseHdlr(ParsableByteArray hdlr) {
        hdlr.setPosition(16);
        int trackType = hdlr.readInt();
        if (trackType == TYPE_soun) {
            return 1;
        }
        if (trackType == TYPE_vide) {
            return 2;
        }
        if (trackType == TYPE_text || trackType == TYPE_sbtl || trackType == TYPE_subt || trackType == TYPE_clcp) {
            return 3;
        }
        if (trackType == TYPE_meta) {
            return 4;
        }
        return -1;
    }

    private static Pair<Long, String> parseMdhd(ParsableByteArray mdhd) {
        int i = 8;
        mdhd.setPosition(8);
        int version = Atom.parseFullAtomVersion(mdhd.readInt());
        mdhd.skipBytes(version == 0 ? 8 : 16);
        long timescale = mdhd.readUnsignedInt();
        if (version == 0) {
            i = 4;
        }
        mdhd.skipBytes(i);
        int languageCode = mdhd.readUnsignedShort();
        return Pair.create(Long.valueOf(timescale), "" + ((char) (((languageCode >> 10) & 31) + 96)) + ((char) (((languageCode >> 5) & 31) + 96)) + ((char) ((languageCode & 31) + 96)));
    }

    private static StsdData parseStsd(ParsableByteArray stsd, int trackId, int rotationDegrees, String language, DrmInitData drmInitData, boolean isQuickTime) throws ParserException {
        stsd.setPosition(12);
        int numberOfEntries = stsd.readInt();
        StsdData out = new StsdData(numberOfEntries);
        for (int i = 0; i < numberOfEntries; i++) {
            int childStartPosition = stsd.getPosition();
            int childAtomSize = stsd.readInt();
            Assertions.checkArgument(childAtomSize > 0, "childAtomSize should be positive");
            int childAtomType = stsd.readInt();
            if (childAtomType == Atom.TYPE_avc1 || childAtomType == Atom.TYPE_avc3 || childAtomType == Atom.TYPE_encv || childAtomType == Atom.TYPE_mp4v || childAtomType == Atom.TYPE_hvc1 || childAtomType == Atom.TYPE_hev1 || childAtomType == Atom.TYPE_s263 || childAtomType == Atom.TYPE_vp08 || childAtomType == Atom.TYPE_vp09) {
                parseVideoSampleEntry(stsd, childAtomType, childStartPosition, childAtomSize, trackId, rotationDegrees, drmInitData, out, i);
            } else if (childAtomType == Atom.TYPE_mp4a || childAtomType == Atom.TYPE_enca || childAtomType == Atom.TYPE_ac_3 || childAtomType == Atom.TYPE_ec_3 || childAtomType == Atom.TYPE_dtsc || childAtomType == Atom.TYPE_dtse || childAtomType == Atom.TYPE_dtsh || childAtomType == Atom.TYPE_dtsl || childAtomType == Atom.TYPE_samr || childAtomType == Atom.TYPE_sawb || childAtomType == Atom.TYPE_lpcm || childAtomType == Atom.TYPE_sowt || childAtomType == Atom.TYPE__mp3 || childAtomType == Atom.TYPE_alac) {
                parseAudioSampleEntry(stsd, childAtomType, childStartPosition, childAtomSize, trackId, language, isQuickTime, drmInitData, out, i);
            } else if (childAtomType == Atom.TYPE_TTML || childAtomType == Atom.TYPE_tx3g || childAtomType == Atom.TYPE_wvtt || childAtomType == Atom.TYPE_stpp || childAtomType == Atom.TYPE_c608) {
                parseTextSampleEntry(stsd, childAtomType, childStartPosition, childAtomSize, trackId, language, out);
            } else if (childAtomType == Atom.TYPE_camm) {
                out.format = Format.createSampleFormat(Integer.toString(trackId), MimeTypes.APPLICATION_CAMERA_MOTION, (String) null, -1, (DrmInitData) null);
            }
            stsd.setPosition(childStartPosition + childAtomSize);
        }
        return out;
    }

    private static void parseTextSampleEntry(ParsableByteArray parent, int atomType, int position, int atomSize, int trackId, String language, StsdData out) throws ParserException {
        String mimeType;
        parent.setPosition(position + 8 + 8);
        List<byte[]> initializationData = null;
        long subsampleOffsetUs = Long.MAX_VALUE;
        if (atomType == Atom.TYPE_TTML) {
            mimeType = MimeTypes.APPLICATION_TTML;
        } else if (atomType == Atom.TYPE_tx3g) {
            mimeType = MimeTypes.APPLICATION_TX3G;
            int sampleDescriptionLength = (atomSize - 8) - 8;
            byte[] sampleDescriptionData = new byte[sampleDescriptionLength];
            parent.readBytes(sampleDescriptionData, 0, sampleDescriptionLength);
            initializationData = Collections.singletonList(sampleDescriptionData);
        } else if (atomType == Atom.TYPE_wvtt) {
            mimeType = MimeTypes.APPLICATION_MP4VTT;
        } else if (atomType == Atom.TYPE_stpp) {
            mimeType = MimeTypes.APPLICATION_TTML;
            subsampleOffsetUs = 0;
        } else if (atomType == Atom.TYPE_c608) {
            mimeType = MimeTypes.APPLICATION_MP4CEA608;
            out.requiredSampleTransformation = 1;
        } else {
            throw new IllegalStateException();
        }
        out.format = Format.createTextSampleFormat(Integer.toString(trackId), mimeType, (String) null, -1, 0, language, -1, (DrmInitData) null, subsampleOffsetUs, initializationData);
    }

    /* JADX WARNING: Removed duplicated region for block: B:73:0x0192  */
    /* JADX WARNING: Removed duplicated region for block: B:92:? A[ORIG_RETURN, RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void parseVideoSampleEntry(com.google.android.exoplayer2.util.ParsableByteArray r29, int r30, int r31, int r32, int r33, int r34, com.google.android.exoplayer2.drm.DrmInitData r35, com.google.android.exoplayer2.extractor.mp4.AtomParsers.StsdData r36, int r37) throws com.google.android.exoplayer2.ParserException {
        /*
            int r3 = r31 + 8
            int r3 = r3 + 8
            r0 = r29
            r0.setPosition(r3)
            r3 = 16
            r0 = r29
            r0.skipBytes(r3)
            int r8 = r29.readUnsignedShort()
            int r9 = r29.readUnsignedShort()
            r26 = 0
            r13 = 1065353216(0x3f800000, float:1.0)
            r3 = 50
            r0 = r29
            r0.skipBytes(r3)
            int r21 = r29.getPosition()
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_encv
            r0 = r30
            if (r0 != r3) goto L_0x005a
            r0 = r29
            r1 = r31
            r2 = r32
            android.util.Pair r27 = parseSampleEntryEncryptionData(r0, r1, r2)
            if (r27 == 0) goto L_0x0053
            r0 = r27
            java.lang.Object r3 = r0.first
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r30 = r3.intValue()
            if (r35 != 0) goto L_0x0082
            r35 = 0
        L_0x0047:
            r0 = r36
            com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox[] r5 = r0.trackEncryptionBoxes
            r0 = r27
            java.lang.Object r3 = r0.second
            com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox r3 = (com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox) r3
            r5[r37] = r3
        L_0x0053:
            r0 = r29
            r1 = r21
            r0.setPosition(r1)
        L_0x005a:
            r11 = 0
            r4 = 0
            r14 = 0
            r15 = -1
        L_0x005e:
            int r3 = r21 - r31
            r0 = r32
            if (r3 >= r0) goto L_0x007f
            r0 = r29
            r1 = r21
            r0.setPosition(r1)
            int r22 = r29.getPosition()
            int r19 = r29.readInt()
            if (r19 != 0) goto L_0x0091
            int r3 = r29.getPosition()
            int r3 = r3 - r31
            r0 = r32
            if (r3 != r0) goto L_0x0091
        L_0x007f:
            if (r4 != 0) goto L_0x0192
        L_0x0081:
            return
        L_0x0082:
            r0 = r27
            java.lang.Object r3 = r0.second
            com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox r3 = (com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox) r3
            java.lang.String r3 = r3.schemeType
            r0 = r35
            com.google.android.exoplayer2.drm.DrmInitData r35 = r0.copyWithSchemeType(r3)
            goto L_0x0047
        L_0x0091:
            if (r19 <= 0) goto L_0x00cb
            r3 = 1
        L_0x0094:
            java.lang.String r5 = "childAtomSize should be positive"
            com.google.android.exoplayer2.util.Assertions.checkArgument(r3, r5)
            int r20 = r29.readInt()
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_avcC
            r0 = r20
            if (r0 != r3) goto L_0x00cf
            if (r4 != 0) goto L_0x00cd
            r3 = 1
        L_0x00a6:
            com.google.android.exoplayer2.util.Assertions.checkState(r3)
            java.lang.String r4 = "video/avc"
            int r3 = r22 + 8
            r0 = r29
            r0.setPosition(r3)
            com.google.android.exoplayer2.video.AvcConfig r18 = com.google.android.exoplayer2.video.AvcConfig.parse(r29)
            r0 = r18
            java.util.List<byte[]> r11 = r0.initializationData
            r0 = r18
            int r3 = r0.nalUnitLengthFieldLength
            r0 = r36
            r0.nalUnitLengthFieldLength = r3
            if (r26 != 0) goto L_0x00c8
            r0 = r18
            float r13 = r0.pixelWidthAspectRatio
        L_0x00c8:
            int r21 = r21 + r19
            goto L_0x005e
        L_0x00cb:
            r3 = 0
            goto L_0x0094
        L_0x00cd:
            r3 = 0
            goto L_0x00a6
        L_0x00cf:
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_hvcC
            r0 = r20
            if (r0 != r3) goto L_0x00f7
            if (r4 != 0) goto L_0x00f5
            r3 = 1
        L_0x00d8:
            com.google.android.exoplayer2.util.Assertions.checkState(r3)
            java.lang.String r4 = "video/hevc"
            int r3 = r22 + 8
            r0 = r29
            r0.setPosition(r3)
            com.google.android.exoplayer2.video.HevcConfig r23 = com.google.android.exoplayer2.video.HevcConfig.parse(r29)
            r0 = r23
            java.util.List<byte[]> r11 = r0.initializationData
            r0 = r23
            int r3 = r0.nalUnitLengthFieldLength
            r0 = r36
            r0.nalUnitLengthFieldLength = r3
            goto L_0x00c8
        L_0x00f5:
            r3 = 0
            goto L_0x00d8
        L_0x00f7:
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_vpcC
            r0 = r20
            if (r0 != r3) goto L_0x0111
            if (r4 != 0) goto L_0x010c
            r3 = 1
        L_0x0100:
            com.google.android.exoplayer2.util.Assertions.checkState(r3)
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_vp08
            r0 = r30
            if (r0 != r3) goto L_0x010e
            java.lang.String r4 = "video/x-vnd.on2.vp8"
        L_0x010b:
            goto L_0x00c8
        L_0x010c:
            r3 = 0
            goto L_0x0100
        L_0x010e:
            java.lang.String r4 = "video/x-vnd.on2.vp9"
            goto L_0x010b
        L_0x0111:
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_d263
            r0 = r20
            if (r0 != r3) goto L_0x0122
            if (r4 != 0) goto L_0x0120
            r3 = 1
        L_0x011a:
            com.google.android.exoplayer2.util.Assertions.checkState(r3)
            java.lang.String r4 = "video/3gpp"
            goto L_0x00c8
        L_0x0120:
            r3 = 0
            goto L_0x011a
        L_0x0122:
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_esds
            r0 = r20
            if (r0 != r3) goto L_0x0147
            if (r4 != 0) goto L_0x0145
            r3 = 1
        L_0x012b:
            com.google.android.exoplayer2.util.Assertions.checkState(r3)
            r0 = r29
            r1 = r22
            android.util.Pair r25 = parseEsdsFromParent(r0, r1)
            r0 = r25
            java.lang.Object r4 = r0.first
            java.lang.String r4 = (java.lang.String) r4
            r0 = r25
            java.lang.Object r3 = r0.second
            java.util.List r11 = java.util.Collections.singletonList(r3)
            goto L_0x00c8
        L_0x0145:
            r3 = 0
            goto L_0x012b
        L_0x0147:
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_pasp
            r0 = r20
            if (r0 != r3) goto L_0x0159
            r0 = r29
            r1 = r22
            float r13 = parsePaspFromParent(r0, r1)
            r26 = 1
            goto L_0x00c8
        L_0x0159:
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_sv3d
            r0 = r20
            if (r0 != r3) goto L_0x016b
            r0 = r29
            r1 = r22
            r2 = r19
            byte[] r14 = parseProjFromParent(r0, r1, r2)
            goto L_0x00c8
        L_0x016b:
            int r3 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_st3d
            r0 = r20
            if (r0 != r3) goto L_0x00c8
            int r28 = r29.readUnsignedByte()
            r3 = 3
            r0 = r29
            r0.skipBytes(r3)
            if (r28 != 0) goto L_0x00c8
            int r24 = r29.readUnsignedByte()
            switch(r24) {
                case 0: goto L_0x0186;
                case 1: goto L_0x0189;
                case 2: goto L_0x018c;
                case 3: goto L_0x018f;
                default: goto L_0x0184;
            }
        L_0x0184:
            goto L_0x00c8
        L_0x0186:
            r15 = 0
            goto L_0x00c8
        L_0x0189:
            r15 = 1
            goto L_0x00c8
        L_0x018c:
            r15 = 2
            goto L_0x00c8
        L_0x018f:
            r15 = 3
            goto L_0x00c8
        L_0x0192:
            java.lang.String r3 = java.lang.Integer.toString(r33)
            r5 = 0
            r6 = -1
            r7 = -1
            r10 = -1082130432(0xffffffffbf800000, float:-1.0)
            r16 = 0
            r12 = r34
            r17 = r35
            com.google.android.exoplayer2.Format r3 = com.google.android.exoplayer2.Format.createVideoSampleFormat(r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15, r16, r17)
            r0 = r36
            r0.format = r3
            goto L_0x0081
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.extractor.mp4.AtomParsers.parseVideoSampleEntry(com.google.android.exoplayer2.util.ParsableByteArray, int, int, int, int, int, com.google.android.exoplayer2.drm.DrmInitData, com.google.android.exoplayer2.extractor.mp4.AtomParsers$StsdData, int):void");
    }

    private static Pair<long[], long[]> parseEdts(Atom.ContainerAtom edtsAtom) {
        Atom.LeafAtom elst;
        if (edtsAtom == null || (elst = edtsAtom.getLeafAtomOfType(Atom.TYPE_elst)) == null) {
            return Pair.create((Object) null, (Object) null);
        }
        ParsableByteArray elstData = elst.data;
        elstData.setPosition(8);
        int version = Atom.parseFullAtomVersion(elstData.readInt());
        int entryCount = elstData.readUnsignedIntToInt();
        long[] editListDurations = new long[entryCount];
        long[] editListMediaTimes = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            editListDurations[i] = version == 1 ? elstData.readUnsignedLongToLong() : elstData.readUnsignedInt();
            editListMediaTimes[i] = version == 1 ? elstData.readLong() : (long) elstData.readInt();
            if (elstData.readShort() != 1) {
                throw new IllegalArgumentException("Unsupported media rate.");
            }
            elstData.skipBytes(2);
        }
        return Pair.create(editListDurations, editListMediaTimes);
    }

    private static float parsePaspFromParent(ParsableByteArray parent, int position) {
        parent.setPosition(position + 8);
        return ((float) parent.readUnsignedIntToInt()) / ((float) parent.readUnsignedIntToInt());
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v26, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r21v3, resolved type: byte[]} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void parseAudioSampleEntry(com.google.android.exoplayer2.util.ParsableByteArray r25, int r26, int r27, int r28, int r29, java.lang.String r30, boolean r31, com.google.android.exoplayer2.drm.DrmInitData r32, com.google.android.exoplayer2.extractor.mp4.AtomParsers.StsdData r33, int r34) throws com.google.android.exoplayer2.ParserException {
        /*
            int r4 = r27 + 8
            int r4 = r4 + 8
            r0 = r25
            r0.setPosition(r4)
            r23 = 0
            if (r31 == 0) goto L_0x00eb
            int r23 = r25.readUnsignedShort()
            r4 = 6
            r0 = r25
            r0.skipBytes(r4)
        L_0x0017:
            if (r23 == 0) goto L_0x001e
            r4 = 1
            r0 = r23
            if (r0 != r4) goto L_0x00f4
        L_0x001e:
            int r9 = r25.readUnsignedShort()
            r4 = 6
            r0 = r25
            r0.skipBytes(r4)
            int r10 = r25.readUnsignedFixedPoint1616()
            r4 = 1
            r0 = r23
            if (r0 != r4) goto L_0x0038
            r4 = 16
            r0 = r25
            r0.skipBytes(r4)
        L_0x0038:
            int r19 = r25.getPosition()
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_enca
            r0 = r26
            if (r0 != r4) goto L_0x006f
            r0 = r25
            r1 = r27
            r2 = r28
            android.util.Pair r24 = parseSampleEntryEncryptionData(r0, r1, r2)
            if (r24 == 0) goto L_0x0068
            r0 = r24
            java.lang.Object r4 = r0.first
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r26 = r4.intValue()
            if (r32 != 0) goto L_0x0116
            r32 = 0
        L_0x005c:
            r0 = r33
            com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox[] r6 = r0.trackEncryptionBoxes
            r0 = r24
            java.lang.Object r4 = r0.second
            com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox r4 = (com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox) r4
            r6[r34] = r4
        L_0x0068:
            r0 = r25
            r1 = r19
            r0.setPosition(r1)
        L_0x006f:
            r5 = 0
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_ac_3
            r0 = r26
            if (r0 != r4) goto L_0x0126
            java.lang.String r5 = "audio/ac3"
        L_0x0078:
            r21 = 0
        L_0x007a:
            int r4 = r19 - r27
            r0 = r28
            if (r4 >= r0) goto L_0x0219
            r0 = r25
            r1 = r19
            r0.setPosition(r1)
            int r17 = r25.readInt()
            if (r17 <= 0) goto L_0x018c
            r4 = 1
        L_0x008e:
            java.lang.String r6 = "childAtomSize should be positive"
            com.google.android.exoplayer2.util.Assertions.checkArgument(r4, r6)
            int r18 = r25.readInt()
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_esds
            r0 = r18
            if (r0 == r4) goto L_0x00a5
            if (r31 == 0) goto L_0x019b
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_wave
            r0 = r18
            if (r0 != r4) goto L_0x019b
        L_0x00a5:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_esds
            r0 = r18
            if (r0 != r4) goto L_0x018f
            r20 = r19
        L_0x00ad:
            r4 = -1
            r0 = r20
            if (r0 == r4) goto L_0x00e8
            r0 = r25
            r1 = r20
            android.util.Pair r22 = parseEsdsFromParent(r0, r1)
            r0 = r22
            java.lang.Object r5 = r0.first
            java.lang.String r5 = (java.lang.String) r5
            r0 = r22
            java.lang.Object r0 = r0.second
            r21 = r0
            byte[] r21 = (byte[]) r21
            java.lang.String r4 = "audio/mp4a-latm"
            boolean r4 = r4.equals(r5)
            if (r4 == 0) goto L_0x00e8
            android.util.Pair r16 = com.google.android.exoplayer2.util.CodecSpecificDataUtil.parseAacAudioSpecificConfig(r21)
            r0 = r16
            java.lang.Object r4 = r0.first
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r10 = r4.intValue()
            r0 = r16
            java.lang.Object r4 = r0.second
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r9 = r4.intValue()
        L_0x00e8:
            int r19 = r19 + r17
            goto L_0x007a
        L_0x00eb:
            r4 = 8
            r0 = r25
            r0.skipBytes(r4)
            goto L_0x0017
        L_0x00f4:
            r4 = 2
            r0 = r23
            if (r0 != r4) goto L_0x0241
            r4 = 16
            r0 = r25
            r0.skipBytes(r4)
            double r6 = r25.readDouble()
            long r6 = java.lang.Math.round(r6)
            int r10 = (int) r6
            int r9 = r25.readUnsignedIntToInt()
            r4 = 20
            r0 = r25
            r0.skipBytes(r4)
            goto L_0x0038
        L_0x0116:
            r0 = r24
            java.lang.Object r4 = r0.second
            com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox r4 = (com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox) r4
            java.lang.String r4 = r4.schemeType
            r0 = r32
            com.google.android.exoplayer2.drm.DrmInitData r32 = r0.copyWithSchemeType(r4)
            goto L_0x005c
        L_0x0126:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_ec_3
            r0 = r26
            if (r0 != r4) goto L_0x0130
            java.lang.String r5 = "audio/eac3"
            goto L_0x0078
        L_0x0130:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_dtsc
            r0 = r26
            if (r0 != r4) goto L_0x013a
            java.lang.String r5 = "audio/vnd.dts"
            goto L_0x0078
        L_0x013a:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_dtsh
            r0 = r26
            if (r0 == r4) goto L_0x0146
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_dtsl
            r0 = r26
            if (r0 != r4) goto L_0x014a
        L_0x0146:
            java.lang.String r5 = "audio/vnd.dts.hd"
            goto L_0x0078
        L_0x014a:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_dtse
            r0 = r26
            if (r0 != r4) goto L_0x0154
            java.lang.String r5 = "audio/vnd.dts.hd;profile=lbr"
            goto L_0x0078
        L_0x0154:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_samr
            r0 = r26
            if (r0 != r4) goto L_0x015e
            java.lang.String r5 = "audio/3gpp"
            goto L_0x0078
        L_0x015e:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_sawb
            r0 = r26
            if (r0 != r4) goto L_0x0168
            java.lang.String r5 = "audio/amr-wb"
            goto L_0x0078
        L_0x0168:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_lpcm
            r0 = r26
            if (r0 == r4) goto L_0x0174
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_sowt
            r0 = r26
            if (r0 != r4) goto L_0x0178
        L_0x0174:
            java.lang.String r5 = "audio/raw"
            goto L_0x0078
        L_0x0178:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE__mp3
            r0 = r26
            if (r0 != r4) goto L_0x0182
            java.lang.String r5 = "audio/mpeg"
            goto L_0x0078
        L_0x0182:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_alac
            r0 = r26
            if (r0 != r4) goto L_0x0078
            java.lang.String r5 = "audio/alac"
            goto L_0x0078
        L_0x018c:
            r4 = 0
            goto L_0x008e
        L_0x018f:
            r0 = r25
            r1 = r19
            r2 = r17
            int r20 = findEsdsPosition(r0, r1, r2)
            goto L_0x00ad
        L_0x019b:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_dac3
            r0 = r18
            if (r0 != r4) goto L_0x01bc
            int r4 = r19 + 8
            r0 = r25
            r0.setPosition(r4)
            java.lang.String r4 = java.lang.Integer.toString(r29)
            r0 = r25
            r1 = r30
            r2 = r32
            com.google.android.exoplayer2.Format r4 = com.google.android.exoplayer2.audio.Ac3Util.parseAc3AnnexFFormat(r0, r4, r1, r2)
            r0 = r33
            r0.format = r4
            goto L_0x00e8
        L_0x01bc:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_dec3
            r0 = r18
            if (r0 != r4) goto L_0x01dd
            int r4 = r19 + 8
            r0 = r25
            r0.setPosition(r4)
            java.lang.String r4 = java.lang.Integer.toString(r29)
            r0 = r25
            r1 = r30
            r2 = r32
            com.google.android.exoplayer2.Format r4 = com.google.android.exoplayer2.audio.Ac3Util.parseEAc3AnnexFFormat(r0, r4, r1, r2)
            r0 = r33
            r0.format = r4
            goto L_0x00e8
        L_0x01dd:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_ddts
            r0 = r18
            if (r0 != r4) goto L_0x01fa
            java.lang.String r4 = java.lang.Integer.toString(r29)
            r6 = 0
            r7 = -1
            r8 = -1
            r11 = 0
            r13 = 0
            r12 = r32
            r14 = r30
            com.google.android.exoplayer2.Format r4 = com.google.android.exoplayer2.Format.createAudioSampleFormat(r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14)
            r0 = r33
            r0.format = r4
            goto L_0x00e8
        L_0x01fa:
            int r4 = com.google.android.exoplayer2.extractor.mp4.Atom.TYPE_alac
            r0 = r18
            if (r0 != r4) goto L_0x00e8
            r0 = r17
            byte[] r0 = new byte[r0]
            r21 = r0
            r0 = r25
            r1 = r19
            r0.setPosition(r1)
            r4 = 0
            r0 = r25
            r1 = r21
            r2 = r17
            r0.readBytes(r1, r4, r2)
            goto L_0x00e8
        L_0x0219:
            r0 = r33
            com.google.android.exoplayer2.Format r4 = r0.format
            if (r4 != 0) goto L_0x0241
            if (r5 == 0) goto L_0x0241
            java.lang.String r4 = "audio/raw"
            boolean r4 = r4.equals(r5)
            if (r4 == 0) goto L_0x0242
            r11 = 2
        L_0x022a:
            java.lang.String r4 = java.lang.Integer.toString(r29)
            r6 = 0
            r7 = -1
            r8 = -1
            if (r21 != 0) goto L_0x0244
            r12 = 0
        L_0x0234:
            r14 = 0
            r13 = r32
            r15 = r30
            com.google.android.exoplayer2.Format r4 = com.google.android.exoplayer2.Format.createAudioSampleFormat(r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15)
            r0 = r33
            r0.format = r4
        L_0x0241:
            return
        L_0x0242:
            r11 = -1
            goto L_0x022a
        L_0x0244:
            java.util.List r12 = java.util.Collections.singletonList(r21)
            goto L_0x0234
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.extractor.mp4.AtomParsers.parseAudioSampleEntry(com.google.android.exoplayer2.util.ParsableByteArray, int, int, int, int, java.lang.String, boolean, com.google.android.exoplayer2.drm.DrmInitData, com.google.android.exoplayer2.extractor.mp4.AtomParsers$StsdData, int):void");
    }

    private static int findEsdsPosition(ParsableByteArray parent, int position, int size) {
        int childAtomPosition = parent.getPosition();
        while (childAtomPosition - position < size) {
            parent.setPosition(childAtomPosition);
            int childAtomSize = parent.readInt();
            Assertions.checkArgument(childAtomSize > 0, "childAtomSize should be positive");
            if (parent.readInt() == Atom.TYPE_esds) {
                return childAtomPosition;
            }
            childAtomPosition += childAtomSize;
        }
        return -1;
    }

    private static Pair<String, byte[]> parseEsdsFromParent(ParsableByteArray parent, int position) {
        String mimeType;
        parent.setPosition(position + 8 + 4);
        parent.skipBytes(1);
        parseExpandableClassSize(parent);
        parent.skipBytes(2);
        int flags = parent.readUnsignedByte();
        if ((flags & 128) != 0) {
            parent.skipBytes(2);
        }
        if ((flags & 64) != 0) {
            parent.skipBytes(parent.readUnsignedShort());
        }
        if ((flags & 32) != 0) {
            parent.skipBytes(2);
        }
        parent.skipBytes(1);
        parseExpandableClassSize(parent);
        switch (parent.readUnsignedByte()) {
            case 32:
                mimeType = MimeTypes.VIDEO_MP4V;
                break;
            case 33:
                mimeType = MimeTypes.VIDEO_H264;
                break;
            case 35:
                mimeType = MimeTypes.VIDEO_H265;
                break;
            case 64:
            case 102:
            case 103:
            case 104:
                mimeType = MimeTypes.AUDIO_AAC;
                break;
            case 96:
            case 97:
                mimeType = MimeTypes.VIDEO_MPEG2;
                break;
            case 107:
                return Pair.create(MimeTypes.AUDIO_MPEG, (Object) null);
            case 165:
                mimeType = MimeTypes.AUDIO_AC3;
                break;
            case 166:
                mimeType = MimeTypes.AUDIO_E_AC3;
                break;
            case 169:
            case 172:
                return Pair.create(MimeTypes.AUDIO_DTS, (Object) null);
            case 170:
            case 171:
                return Pair.create(MimeTypes.AUDIO_DTS_HD, (Object) null);
            default:
                mimeType = null;
                break;
        }
        parent.skipBytes(12);
        parent.skipBytes(1);
        int initializationDataSize = parseExpandableClassSize(parent);
        byte[] initializationData = new byte[initializationDataSize];
        parent.readBytes(initializationData, 0, initializationDataSize);
        return Pair.create(mimeType, initializationData);
    }

    private static Pair<Integer, TrackEncryptionBox> parseSampleEntryEncryptionData(ParsableByteArray parent, int position, int size) {
        Pair<Integer, TrackEncryptionBox> result;
        int childPosition = parent.getPosition();
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            Assertions.checkArgument(childAtomSize > 0, "childAtomSize should be positive");
            if (parent.readInt() == Atom.TYPE_sinf && (result = parseCommonEncryptionSinfFromParent(parent, childPosition, childAtomSize)) != null) {
                return result;
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    static Pair<Integer, TrackEncryptionBox> parseCommonEncryptionSinfFromParent(ParsableByteArray parent, int position, int size) {
        boolean z;
        boolean z2 = true;
        int childPosition = position + 8;
        int schemeInformationBoxPosition = -1;
        int schemeInformationBoxSize = 0;
        String schemeType = null;
        Integer dataFormat = null;
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            int childAtomType = parent.readInt();
            if (childAtomType == Atom.TYPE_frma) {
                dataFormat = Integer.valueOf(parent.readInt());
            } else if (childAtomType == Atom.TYPE_schm) {
                parent.skipBytes(4);
                schemeType = parent.readString(4);
            } else if (childAtomType == Atom.TYPE_schi) {
                schemeInformationBoxPosition = childPosition;
                schemeInformationBoxSize = childAtomSize;
            }
            childPosition += childAtomSize;
        }
        if (!C.CENC_TYPE_cenc.equals(schemeType) && !C.CENC_TYPE_cbc1.equals(schemeType) && !C.CENC_TYPE_cens.equals(schemeType) && !C.CENC_TYPE_cbcs.equals(schemeType)) {
            return null;
        }
        Assertions.checkArgument(dataFormat != null, "frma atom is mandatory");
        if (schemeInformationBoxPosition != -1) {
            z = true;
        } else {
            z = false;
        }
        Assertions.checkArgument(z, "schi atom is mandatory");
        TrackEncryptionBox encryptionBox = parseSchiFromParent(parent, schemeInformationBoxPosition, schemeInformationBoxSize, schemeType);
        if (encryptionBox == null) {
            z2 = false;
        }
        Assertions.checkArgument(z2, "tenc atom is mandatory");
        return Pair.create(dataFormat, encryptionBox);
    }

    private static TrackEncryptionBox parseSchiFromParent(ParsableByteArray parent, int position, int size, String schemeType) {
        int childPosition = position + 8;
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            if (parent.readInt() == Atom.TYPE_tenc) {
                int version = Atom.parseFullAtomVersion(parent.readInt());
                parent.skipBytes(1);
                int defaultCryptByteBlock = 0;
                int defaultSkipByteBlock = 0;
                if (version == 0) {
                    parent.skipBytes(1);
                } else {
                    int patternByte = parent.readUnsignedByte();
                    defaultCryptByteBlock = (patternByte & PsExtractor.VIDEO_STREAM_MASK) >> 4;
                    defaultSkipByteBlock = patternByte & 15;
                }
                boolean defaultIsProtected = parent.readUnsignedByte() == 1;
                int defaultPerSampleIvSize = parent.readUnsignedByte();
                byte[] defaultKeyId = new byte[16];
                parent.readBytes(defaultKeyId, 0, defaultKeyId.length);
                byte[] constantIv = null;
                if (defaultIsProtected && defaultPerSampleIvSize == 0) {
                    int constantIvSize = parent.readUnsignedByte();
                    constantIv = new byte[constantIvSize];
                    parent.readBytes(constantIv, 0, constantIvSize);
                }
                return new TrackEncryptionBox(defaultIsProtected, schemeType, defaultPerSampleIvSize, defaultKeyId, defaultCryptByteBlock, defaultSkipByteBlock, constantIv);
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    private static byte[] parseProjFromParent(ParsableByteArray parent, int position, int size) {
        int childPosition = position + 8;
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            if (parent.readInt() == Atom.TYPE_proj) {
                return Arrays.copyOfRange(parent.data, childPosition, childPosition + childAtomSize);
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    private static int parseExpandableClassSize(ParsableByteArray data) {
        int currentByte = data.readUnsignedByte();
        int size = currentByte & 127;
        while ((currentByte & 128) == 128) {
            currentByte = data.readUnsignedByte();
            size = (size << 7) | (currentByte & 127);
        }
        return size;
    }

    private AtomParsers() {
    }

    private static final class ChunkIterator {
        private final ParsableByteArray chunkOffsets;
        private final boolean chunkOffsetsAreLongs;
        public int index;
        public final int length;
        private int nextSamplesPerChunkChangeIndex;
        public int numSamples;
        public long offset;
        private int remainingSamplesPerChunkChanges;
        private final ParsableByteArray stsc;

        public ChunkIterator(ParsableByteArray stsc2, ParsableByteArray chunkOffsets2, boolean chunkOffsetsAreLongs2) {
            boolean z = true;
            this.stsc = stsc2;
            this.chunkOffsets = chunkOffsets2;
            this.chunkOffsetsAreLongs = chunkOffsetsAreLongs2;
            chunkOffsets2.setPosition(12);
            this.length = chunkOffsets2.readUnsignedIntToInt();
            stsc2.setPosition(12);
            this.remainingSamplesPerChunkChanges = stsc2.readUnsignedIntToInt();
            Assertions.checkState(stsc2.readInt() != 1 ? false : z, "first_chunk must be 1");
            this.index = -1;
        }

        public boolean moveNext() {
            long readUnsignedInt;
            int i = this.index + 1;
            this.index = i;
            if (i == this.length) {
                return false;
            }
            if (this.chunkOffsetsAreLongs) {
                readUnsignedInt = this.chunkOffsets.readUnsignedLongToLong();
            } else {
                readUnsignedInt = this.chunkOffsets.readUnsignedInt();
            }
            this.offset = readUnsignedInt;
            if (this.index == this.nextSamplesPerChunkChangeIndex) {
                this.numSamples = this.stsc.readUnsignedIntToInt();
                this.stsc.skipBytes(4);
                int i2 = this.remainingSamplesPerChunkChanges - 1;
                this.remainingSamplesPerChunkChanges = i2;
                this.nextSamplesPerChunkChangeIndex = i2 > 0 ? this.stsc.readUnsignedIntToInt() - 1 : -1;
            }
            return true;
        }
    }

    private static final class TkhdData {
        /* access modifiers changed from: private */
        public final long duration;
        /* access modifiers changed from: private */
        public final int id;
        /* access modifiers changed from: private */
        public final int rotationDegrees;

        public TkhdData(int id2, long duration2, int rotationDegrees2) {
            this.id = id2;
            this.duration = duration2;
            this.rotationDegrees = rotationDegrees2;
        }
    }

    private static final class StsdData {
        public static final int STSD_HEADER_SIZE = 8;
        public Format format;
        public int nalUnitLengthFieldLength;
        public int requiredSampleTransformation = 0;
        public final TrackEncryptionBox[] trackEncryptionBoxes;

        public StsdData(int numberOfEntries) {
            this.trackEncryptionBoxes = new TrackEncryptionBox[numberOfEntries];
        }
    }

    static final class StszSampleSizeBox implements SampleSizeBox {
        private final ParsableByteArray data;
        private final int fixedSampleSize = this.data.readUnsignedIntToInt();
        private final int sampleCount = this.data.readUnsignedIntToInt();

        public StszSampleSizeBox(Atom.LeafAtom stszAtom) {
            this.data = stszAtom.data;
            this.data.setPosition(12);
        }

        public int getSampleCount() {
            return this.sampleCount;
        }

        public int readNextSampleSize() {
            return this.fixedSampleSize == 0 ? this.data.readUnsignedIntToInt() : this.fixedSampleSize;
        }

        public boolean isFixedSampleSize() {
            return this.fixedSampleSize != 0;
        }
    }

    static final class Stz2SampleSizeBox implements SampleSizeBox {
        private int currentByte;
        private final ParsableByteArray data;
        private final int fieldSize = (this.data.readUnsignedIntToInt() & 255);
        private final int sampleCount = this.data.readUnsignedIntToInt();
        private int sampleIndex;

        public Stz2SampleSizeBox(Atom.LeafAtom stz2Atom) {
            this.data = stz2Atom.data;
            this.data.setPosition(12);
        }

        public int getSampleCount() {
            return this.sampleCount;
        }

        public int readNextSampleSize() {
            if (this.fieldSize == 8) {
                return this.data.readUnsignedByte();
            }
            if (this.fieldSize == 16) {
                return this.data.readUnsignedShort();
            }
            int i = this.sampleIndex;
            this.sampleIndex = i + 1;
            if (i % 2 != 0) {
                return this.currentByte & 15;
            }
            this.currentByte = this.data.readUnsignedByte();
            return (this.currentByte & PsExtractor.VIDEO_STREAM_MASK) >> 4;
        }

        public boolean isFixedSampleSize() {
            return false;
        }
    }
}
