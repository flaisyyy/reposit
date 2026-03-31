package com.google.android.exoplayer2.text.ssa;

import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.SimpleSubtitleDecoder;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.LongArray;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SsaDecoder extends SimpleSubtitleDecoder {
    private static final String DIALOGUE_LINE_PREFIX = "Dialogue: ";
    private static final String FORMAT_LINE_PREFIX = "Format: ";
    private static final Pattern SSA_TIMECODE_PATTERN = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d+)(?::|\\.)(\\d+)");
    private static final String TAG = "SsaDecoder";
    private int formatEndIndex;
    private int formatKeyCount;
    private int formatStartIndex;
    private int formatTextIndex;
    private final boolean haveInitializationData;

    public SsaDecoder() {
        this((List<byte[]>) null);
    }

    public SsaDecoder(List<byte[]> initializationData) {
        super(TAG);
        if (initializationData == null || initializationData.isEmpty()) {
            this.haveInitializationData = false;
            return;
        }
        this.haveInitializationData = true;
        String formatLine = new String(initializationData.get(0));
        Assertions.checkArgument(formatLine.startsWith(FORMAT_LINE_PREFIX));
        parseFormatLine(formatLine);
        parseHeader(new ParsableByteArray(initializationData.get(1)));
    }

    /* access modifiers changed from: protected */
    public SsaSubtitle decode(byte[] bytes, int length, boolean reset) {
        ArrayList<Cue> cues = new ArrayList<>();
        LongArray cueTimesUs = new LongArray();
        ParsableByteArray data = new ParsableByteArray(bytes, length);
        if (!this.haveInitializationData) {
            parseHeader(data);
        }
        parseEventBody(data, cues, cueTimesUs);
        Cue[] cuesArray = new Cue[cues.size()];
        cues.toArray(cuesArray);
        return new SsaSubtitle(cuesArray, cueTimesUs.toArray());
    }

    /*  JADX ERROR: StackOverflow in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxOverflowException: 
        	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:47)
        	at jadx.core.utils.ErrorsCounter.methodError(ErrorsCounter.java:81)
        */
    private void parseHeader(com.google.android.exoplayer2.util.ParsableByteArray r3) {
        /*
            r2 = this;
        L_0x0000:
            java.lang.String r0 = r3.readLine()
            if (r0 == 0) goto L_0x000e
            java.lang.String r1 = "[Events]"
            boolean r1 = r0.startsWith(r1)
            if (r1 == 0) goto L_0x0000
        L_0x000e:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.text.ssa.SsaDecoder.parseHeader(com.google.android.exoplayer2.util.ParsableByteArray):void");
    }

    private void parseEventBody(ParsableByteArray data, List<Cue> cues, LongArray cueTimesUs) {
        while (true) {
            String currentLine = data.readLine();
            if (currentLine == null) {
                return;
            }
            if (!this.haveInitializationData && currentLine.startsWith(FORMAT_LINE_PREFIX)) {
                parseFormatLine(currentLine);
            } else if (currentLine.startsWith(DIALOGUE_LINE_PREFIX)) {
                parseDialogueLine(currentLine, cues, cueTimesUs);
            }
        }
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseFormatLine(java.lang.String r8) {
        /*
            r7 = this;
            r4 = 0
            r5 = -1
            java.lang.String r3 = "Format: "
            int r3 = r3.length()
            java.lang.String r3 = r8.substring(r3)
            java.lang.String r6 = ","
            java.lang.String[] r2 = android.text.TextUtils.split(r3, r6)
            int r3 = r2.length
            r7.formatKeyCount = r3
            r7.formatStartIndex = r5
            r7.formatEndIndex = r5
            r7.formatTextIndex = r5
            r0 = 0
        L_0x001c:
            int r3 = r7.formatKeyCount
            if (r0 >= r3) goto L_0x005f
            r3 = r2[r0]
            java.lang.String r3 = r3.trim()
            java.lang.String r1 = com.google.android.exoplayer2.util.Util.toLowerInvariant(r3)
            int r3 = r1.hashCode()
            switch(r3) {
                case 100571: goto L_0x0042;
                case 3556653: goto L_0x004c;
                case 109757538: goto L_0x0038;
                default: goto L_0x0031;
            }
        L_0x0031:
            r3 = r5
        L_0x0032:
            switch(r3) {
                case 0: goto L_0x0056;
                case 1: goto L_0x0059;
                case 2: goto L_0x005c;
                default: goto L_0x0035;
            }
        L_0x0035:
            int r0 = r0 + 1
            goto L_0x001c
        L_0x0038:
            java.lang.String r3 = "start"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x0031
            r3 = r4
            goto L_0x0032
        L_0x0042:
            java.lang.String r3 = "end"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x0031
            r3 = 1
            goto L_0x0032
        L_0x004c:
            java.lang.String r3 = "text"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x0031
            r3 = 2
            goto L_0x0032
        L_0x0056:
            r7.formatStartIndex = r0
            goto L_0x0035
        L_0x0059:
            r7.formatEndIndex = r0
            goto L_0x0035
        L_0x005c:
            r7.formatTextIndex = r0
            goto L_0x0035
        L_0x005f:
            int r3 = r7.formatStartIndex
            if (r3 == r5) goto L_0x006b
            int r3 = r7.formatEndIndex
            if (r3 == r5) goto L_0x006b
            int r3 = r7.formatTextIndex
            if (r3 != r5) goto L_0x006d
        L_0x006b:
            r7.formatKeyCount = r4
        L_0x006d:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.text.ssa.SsaDecoder.parseFormatLine(java.lang.String):void");
    }

    private void parseDialogueLine(String dialogueLine, List<Cue> cues, LongArray cueTimesUs) {
        if (this.formatKeyCount == 0) {
            Log.w(TAG, "Skipping dialogue line before complete format: " + dialogueLine);
            return;
        }
        String[] lineValues = dialogueLine.substring(DIALOGUE_LINE_PREFIX.length()).split(",", this.formatKeyCount);
        if (lineValues.length != this.formatKeyCount) {
            Log.w(TAG, "Skipping dialogue line with fewer columns than format: " + dialogueLine);
            return;
        }
        long startTimeUs = parseTimecodeUs(lineValues[this.formatStartIndex]);
        if (startTimeUs == C.TIME_UNSET) {
            Log.w(TAG, "Skipping invalid timing: " + dialogueLine);
            return;
        }
        long endTimeUs = C.TIME_UNSET;
        String endTimeString = lineValues[this.formatEndIndex];
        if (!endTimeString.trim().isEmpty()) {
            endTimeUs = parseTimecodeUs(endTimeString);
            if (endTimeUs == C.TIME_UNSET) {
                Log.w(TAG, "Skipping invalid timing: " + dialogueLine);
                return;
            }
        }
        cues.add(new Cue(lineValues[this.formatTextIndex].replaceAll("\\{.*?\\}", "").replaceAll("\\\\N", "\n").replaceAll("\\\\n", "\n")));
        cueTimesUs.add(startTimeUs);
        if (endTimeUs != C.TIME_UNSET) {
            cues.add((Object) null);
            cueTimesUs.add(endTimeUs);
        }
    }

    public static long parseTimecodeUs(String timeString) {
        Matcher matcher = SSA_TIMECODE_PATTERN.matcher(timeString);
        if (!matcher.matches()) {
            return C.TIME_UNSET;
        }
        return (Long.parseLong(matcher.group(1)) * 60 * 60 * C.MICROS_PER_SECOND) + (Long.parseLong(matcher.group(2)) * 60 * C.MICROS_PER_SECOND) + (Long.parseLong(matcher.group(3)) * C.MICROS_PER_SECOND) + (Long.parseLong(matcher.group(4)) * 10000);
    }
}
