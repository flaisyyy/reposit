package com.google.android.exoplayer2.text.subrip;

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.SimpleSubtitleDecoder;
import com.google.android.exoplayer2.util.LongArray;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SubripDecoder extends SimpleSubtitleDecoder {
    private static final String SUBRIP_TIMECODE = "(?:(\\d+):)?(\\d+):(\\d+),(\\d+)";
    private static final Pattern SUBRIP_TIMING_LINE = Pattern.compile("\\s*((?:(\\d+):)?(\\d+):(\\d+),(\\d+))\\s*-->\\s*((?:(\\d+):)?(\\d+):(\\d+),(\\d+))?\\s*");
    private static final String TAG = "SubripDecoder";
    private final StringBuilder textBuilder = new StringBuilder();

    public SubripDecoder() {
        super(TAG);
    }

    /* access modifiers changed from: protected */
    public SubripSubtitle decode(byte[] bytes, int length, boolean reset) {
        ArrayList<Cue> cues = new ArrayList<>();
        LongArray cueTimesUs = new LongArray();
        ParsableByteArray subripData = new ParsableByteArray(bytes, length);
        while (true) {
            String currentLine = subripData.readLine();
            if (currentLine == null) {
                break;
            } else if (currentLine.length() != 0) {
                try {
                    Integer.parseInt(currentLine);
                    boolean haveEndTimecode = false;
                    String currentLine2 = subripData.readLine();
                    if (currentLine2 == null) {
                        Log.w(TAG, "Unexpected end");
                        break;
                    }
                    Matcher matcher = SUBRIP_TIMING_LINE.matcher(currentLine2);
                    if (matcher.matches()) {
                        cueTimesUs.add(parseTimecode(matcher, 1));
                        if (!TextUtils.isEmpty(matcher.group(6))) {
                            haveEndTimecode = true;
                            cueTimesUs.add(parseTimecode(matcher, 6));
                        }
                        this.textBuilder.setLength(0);
                        while (true) {
                            String currentLine3 = subripData.readLine();
                            if (TextUtils.isEmpty(currentLine3)) {
                                break;
                            }
                            if (this.textBuilder.length() > 0) {
                                this.textBuilder.append("<br>");
                            }
                            this.textBuilder.append(currentLine3.trim());
                        }
                        cues.add(new Cue(Html.fromHtml(this.textBuilder.toString())));
                        if (haveEndTimecode) {
                            cues.add((Object) null);
                        }
                    } else {
                        Log.w(TAG, "Skipping invalid timing: " + currentLine2);
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Skipping invalid index: " + currentLine);
                }
            }
        }
        Cue[] cuesArray = new Cue[cues.size()];
        cues.toArray(cuesArray);
        return new SubripSubtitle(cuesArray, cueTimesUs.toArray());
    }

    private static long parseTimecode(Matcher matcher, int groupOffset) {
        return ((Long.parseLong(matcher.group(groupOffset + 1)) * 60 * 60 * 1000) + (Long.parseLong(matcher.group(groupOffset + 2)) * 60 * 1000) + (Long.parseLong(matcher.group(groupOffset + 3)) * 1000) + Long.parseLong(matcher.group(groupOffset + 4))) * 1000;
    }
}
