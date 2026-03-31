package com.google.android.exoplayer2.text.ttml;

import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.text.SimpleSubtitleDecoder;
import com.google.android.exoplayer2.text.SubtitleDecoderException;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.util.XmlPullParserUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public final class TtmlDecoder extends SimpleSubtitleDecoder {
    private static final String ATTR_BEGIN = "begin";
    private static final String ATTR_DURATION = "dur";
    private static final String ATTR_END = "end";
    private static final String ATTR_REGION = "region";
    private static final String ATTR_STYLE = "style";
    private static final Pattern CLOCK_TIME = Pattern.compile("^([0-9][0-9]+):([0-9][0-9]):([0-9][0-9])(?:(\\.[0-9]+)|:([0-9][0-9])(?:\\.([0-9]+))?)?$");
    private static final FrameAndTickRate DEFAULT_FRAME_AND_TICK_RATE = new FrameAndTickRate(30.0f, 1, 1);
    private static final int DEFAULT_FRAME_RATE = 30;
    private static final Pattern FONT_SIZE = Pattern.compile("^(([0-9]*.)?[0-9]+)(px|em|%)$");
    private static final Pattern OFFSET_TIME = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)(h|m|s|ms|f|t)$");
    private static final Pattern PERCENTAGE_COORDINATES = Pattern.compile("^(\\d+\\.?\\d*?)% (\\d+\\.?\\d*?)%$");
    private static final String TAG = "TtmlDecoder";
    private static final String TTP = "http://www.w3.org/ns/ttml#parameter";
    private final XmlPullParserFactory xmlParserFactory;

    public TtmlDecoder() {
        super(TAG);
        try {
            this.xmlParserFactory = XmlPullParserFactory.newInstance();
            this.xmlParserFactory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Couldn't create XmlPullParserFactory instance", e);
        }
    }

    /* access modifiers changed from: protected */
    public TtmlSubtitle decode(byte[] bytes, int length, boolean reset) throws SubtitleDecoderException {
        try {
            XmlPullParser xmlParser = this.xmlParserFactory.newPullParser();
            Map<String, TtmlStyle> globalStyles = new HashMap<>();
            Map<String, TtmlRegion> regionMap = new HashMap<>();
            regionMap.put("", new TtmlRegion((String) null));
            xmlParser.setInput(new ByteArrayInputStream(bytes, 0, length), (String) null);
            TtmlSubtitle ttmlSubtitle = null;
            LinkedList<TtmlNode> nodeStack = new LinkedList<>();
            int unsupportedNodeDepth = 0;
            FrameAndTickRate frameAndTickRate = DEFAULT_FRAME_AND_TICK_RATE;
            for (int eventType = xmlParser.getEventType(); eventType != 1; eventType = xmlParser.getEventType()) {
                TtmlNode parent = nodeStack.peekLast();
                if (unsupportedNodeDepth == 0) {
                    String name = xmlParser.getName();
                    if (eventType == 2) {
                        if (TtmlNode.TAG_TT.equals(name)) {
                            frameAndTickRate = parseFrameAndTickRates(xmlParser);
                        }
                        if (!isSupportedTag(name)) {
                            Log.i(TAG, "Ignoring unsupported tag: " + xmlParser.getName());
                            unsupportedNodeDepth++;
                        } else if (TtmlNode.TAG_HEAD.equals(name)) {
                            parseHeader(xmlParser, globalStyles, regionMap);
                        } else {
                            try {
                                TtmlNode node = parseNode(xmlParser, parent, regionMap, frameAndTickRate);
                                nodeStack.addLast(node);
                                if (parent != null) {
                                    parent.addChild(node);
                                }
                            } catch (SubtitleDecoderException e) {
                                Log.w(TAG, "Suppressing parser error", e);
                                unsupportedNodeDepth++;
                            }
                        }
                    } else if (eventType == 4) {
                        parent.addChild(TtmlNode.buildTextNode(xmlParser.getText()));
                    } else if (eventType == 3) {
                        if (xmlParser.getName().equals(TtmlNode.TAG_TT)) {
                            ttmlSubtitle = new TtmlSubtitle(nodeStack.getLast(), globalStyles, regionMap);
                        }
                        nodeStack.removeLast();
                    }
                } else if (eventType == 2) {
                    unsupportedNodeDepth++;
                } else if (eventType == 3) {
                    unsupportedNodeDepth--;
                }
                xmlParser.next();
            }
            return ttmlSubtitle;
        } catch (XmlPullParserException xppe) {
            throw new SubtitleDecoderException("Unable to decode source", xppe);
        } catch (IOException e2) {
            throw new IllegalStateException("Unexpected error when reading input.", e2);
        }
    }

    private FrameAndTickRate parseFrameAndTickRates(XmlPullParser xmlParser) throws SubtitleDecoderException {
        int frameRate = 30;
        String frameRateString = xmlParser.getAttributeValue(TTP, "frameRate");
        if (frameRateString != null) {
            frameRate = Integer.parseInt(frameRateString);
        }
        float frameRateMultiplier = 1.0f;
        String frameRateMultiplierString = xmlParser.getAttributeValue(TTP, "frameRateMultiplier");
        if (frameRateMultiplierString != null) {
            String[] parts = frameRateMultiplierString.split(" ");
            if (parts.length != 2) {
                throw new SubtitleDecoderException("frameRateMultiplier doesn't have 2 parts");
            }
            frameRateMultiplier = ((float) Integer.parseInt(parts[0])) / ((float) Integer.parseInt(parts[1]));
        }
        int subFrameRate = DEFAULT_FRAME_AND_TICK_RATE.subFrameRate;
        String subFrameRateString = xmlParser.getAttributeValue(TTP, "subFrameRate");
        if (subFrameRateString != null) {
            subFrameRate = Integer.parseInt(subFrameRateString);
        }
        int tickRate = DEFAULT_FRAME_AND_TICK_RATE.tickRate;
        String tickRateString = xmlParser.getAttributeValue(TTP, "tickRate");
        if (tickRateString != null) {
            tickRate = Integer.parseInt(tickRateString);
        }
        return new FrameAndTickRate(((float) frameRate) * frameRateMultiplier, subFrameRate, tickRate);
    }

    private Map<String, TtmlStyle> parseHeader(XmlPullParser xmlParser, Map<String, TtmlStyle> globalStyles, Map<String, TtmlRegion> globalRegions) throws IOException, XmlPullParserException {
        TtmlRegion ttmlRegion;
        do {
            xmlParser.next();
            if (XmlPullParserUtil.isStartTag(xmlParser, "style")) {
                String parentStyleId = XmlPullParserUtil.getAttributeValue(xmlParser, "style");
                TtmlStyle style = parseStyleAttributes(xmlParser, new TtmlStyle());
                if (parentStyleId != null) {
                    for (String id : parseStyleIds(parentStyleId)) {
                        style.chain(globalStyles.get(id));
                    }
                }
                if (style.getId() != null) {
                    globalStyles.put(style.getId(), style);
                }
            } else if (XmlPullParserUtil.isStartTag(xmlParser, "region") && (ttmlRegion = parseRegionAttributes(xmlParser)) != null) {
                globalRegions.put(ttmlRegion.id, ttmlRegion);
            }
        } while (!XmlPullParserUtil.isEndTag(xmlParser, TtmlNode.TAG_HEAD));
        return globalStyles;
    }

    private TtmlRegion parseRegionAttributes(XmlPullParser xmlParser) {
        String regionId = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_ID);
        if (regionId == null) {
            return null;
        }
        String regionOrigin = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_TTS_ORIGIN);
        if (regionOrigin != null) {
            Matcher originMatcher = PERCENTAGE_COORDINATES.matcher(regionOrigin);
            if (originMatcher.matches()) {
                try {
                    float position = Float.parseFloat(originMatcher.group(1)) / 100.0f;
                    float line = Float.parseFloat(originMatcher.group(2)) / 100.0f;
                    String regionExtent = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_TTS_EXTENT);
                    if (regionExtent != null) {
                        Matcher extentMatcher = PERCENTAGE_COORDINATES.matcher(regionExtent);
                        if (extentMatcher.matches()) {
                            try {
                                float width = Float.parseFloat(extentMatcher.group(1)) / 100.0f;
                                float height = Float.parseFloat(extentMatcher.group(2)) / 100.0f;
                                int lineAnchor = 0;
                                String displayAlign = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_TTS_DISPLAY_ALIGN);
                                if (displayAlign != null) {
                                    String lowerInvariant = Util.toLowerInvariant(displayAlign);
                                    char c = 65535;
                                    switch (lowerInvariant.hashCode()) {
                                        case -1364013995:
                                            if (lowerInvariant.equals(TtmlNode.CENTER)) {
                                                c = 0;
                                                break;
                                            }
                                            break;
                                        case 92734940:
                                            if (lowerInvariant.equals("after")) {
                                                c = 1;
                                                break;
                                            }
                                            break;
                                    }
                                    switch (c) {
                                        case 0:
                                            lineAnchor = 1;
                                            line += height / 2.0f;
                                            break;
                                        case 1:
                                            lineAnchor = 2;
                                            line += height;
                                            break;
                                    }
                                }
                                return new TtmlRegion(regionId, position, line, 0, lineAnchor, width);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Ignoring region with malformed extent: " + regionOrigin);
                                return null;
                            }
                        } else {
                            Log.w(TAG, "Ignoring region with unsupported extent: " + regionOrigin);
                            return null;
                        }
                    } else {
                        Log.w(TAG, "Ignoring region without an extent");
                        return null;
                    }
                } catch (NumberFormatException e2) {
                    Log.w(TAG, "Ignoring region with malformed origin: " + regionOrigin);
                    return null;
                }
            } else {
                Log.w(TAG, "Ignoring region with unsupported origin: " + regionOrigin);
                return null;
            }
        } else {
            Log.w(TAG, "Ignoring region without an origin");
            return null;
        }
    }

    private String[] parseStyleIds(String parentStyleIds) {
        return parentStyleIds.split("\\s+");
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private com.google.android.exoplayer2.text.ttml.TtmlStyle parseStyleAttributes(org.xmlpull.v1.XmlPullParser r13, com.google.android.exoplayer2.text.ttml.TtmlStyle r14) {
        /*
            r12 = this;
            r9 = 3
            r8 = 2
            r6 = -1
            r7 = 1
            r5 = 0
            int r0 = r13.getAttributeCount()
            r3 = 0
        L_0x000a:
            if (r3 >= r0) goto L_0x020f
            java.lang.String r1 = r13.getAttributeValue(r3)
            java.lang.String r4 = r13.getAttributeName(r3)
            int r10 = r4.hashCode()
            switch(r10) {
                case -1550943582: goto L_0x005e;
                case -1224696685: goto L_0x0040;
                case -1065511464: goto L_0x0068;
                case -879295043: goto L_0x0072;
                case -734428249: goto L_0x0054;
                case 3355: goto L_0x0022;
                case 94842723: goto L_0x0036;
                case 365601008: goto L_0x004a;
                case 1287124693: goto L_0x002c;
                default: goto L_0x001b;
            }
        L_0x001b:
            r4 = r6
        L_0x001c:
            switch(r4) {
                case 0: goto L_0x007d;
                case 1: goto L_0x0092;
                case 2: goto L_0x00b9;
                case 3: goto L_0x00e1;
                case 4: goto L_0x00eb;
                case 5: goto L_0x010f;
                case 6: goto L_0x011f;
                case 7: goto L_0x012f;
                case 8: goto L_0x01ae;
                default: goto L_0x001f;
            }
        L_0x001f:
            int r3 = r3 + 1
            goto L_0x000a
        L_0x0022:
            java.lang.String r10 = "id"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = r5
            goto L_0x001c
        L_0x002c:
            java.lang.String r10 = "backgroundColor"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = r7
            goto L_0x001c
        L_0x0036:
            java.lang.String r10 = "color"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = r8
            goto L_0x001c
        L_0x0040:
            java.lang.String r10 = "fontFamily"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = r9
            goto L_0x001c
        L_0x004a:
            java.lang.String r10 = "fontSize"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = 4
            goto L_0x001c
        L_0x0054:
            java.lang.String r10 = "fontWeight"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = 5
            goto L_0x001c
        L_0x005e:
            java.lang.String r10 = "fontStyle"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = 6
            goto L_0x001c
        L_0x0068:
            java.lang.String r10 = "textAlign"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = 7
            goto L_0x001c
        L_0x0072:
            java.lang.String r10 = "textDecoration"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001b
            r4 = 8
            goto L_0x001c
        L_0x007d:
            java.lang.String r4 = "style"
            java.lang.String r10 = r13.getName()
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x001f
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setId(r1)
            goto L_0x001f
        L_0x0092:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r12.createIfNull(r14)
            int r4 = com.google.android.exoplayer2.util.ColorParser.parseTtmlColor(r1)     // Catch:{ IllegalArgumentException -> 0x009e }
            r14.setBackgroundColor(r4)     // Catch:{ IllegalArgumentException -> 0x009e }
            goto L_0x001f
        L_0x009e:
            r2 = move-exception
            java.lang.String r4 = "TtmlDecoder"
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r11 = "Failed parsing background value: "
            java.lang.StringBuilder r10 = r10.append(r11)
            java.lang.StringBuilder r10 = r10.append(r1)
            java.lang.String r10 = r10.toString()
            android.util.Log.w(r4, r10)
            goto L_0x001f
        L_0x00b9:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r12.createIfNull(r14)
            int r4 = com.google.android.exoplayer2.util.ColorParser.parseTtmlColor(r1)     // Catch:{ IllegalArgumentException -> 0x00c6 }
            r14.setFontColor(r4)     // Catch:{ IllegalArgumentException -> 0x00c6 }
            goto L_0x001f
        L_0x00c6:
            r2 = move-exception
            java.lang.String r4 = "TtmlDecoder"
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r11 = "Failed parsing color value: "
            java.lang.StringBuilder r10 = r10.append(r11)
            java.lang.StringBuilder r10 = r10.append(r1)
            java.lang.String r10 = r10.toString()
            android.util.Log.w(r4, r10)
            goto L_0x001f
        L_0x00e1:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setFontFamily(r1)
            goto L_0x001f
        L_0x00eb:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r12.createIfNull(r14)     // Catch:{ SubtitleDecoderException -> 0x00f4 }
            parseFontSize(r1, r14)     // Catch:{ SubtitleDecoderException -> 0x00f4 }
            goto L_0x001f
        L_0x00f4:
            r2 = move-exception
            java.lang.String r4 = "TtmlDecoder"
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r11 = "Failed parsing fontSize value: "
            java.lang.StringBuilder r10 = r10.append(r11)
            java.lang.StringBuilder r10 = r10.append(r1)
            java.lang.String r10 = r10.toString()
            android.util.Log.w(r4, r10)
            goto L_0x001f
        L_0x010f:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            java.lang.String r10 = "bold"
            boolean r10 = r10.equalsIgnoreCase(r1)
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setBold(r10)
            goto L_0x001f
        L_0x011f:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            java.lang.String r10 = "italic"
            boolean r10 = r10.equalsIgnoreCase(r1)
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setItalic(r10)
            goto L_0x001f
        L_0x012f:
            java.lang.String r4 = com.google.android.exoplayer2.util.Util.toLowerInvariant(r1)
            int r10 = r4.hashCode()
            switch(r10) {
                case -1364013995: goto L_0x0174;
                case 100571: goto L_0x016a;
                case 3317767: goto L_0x014c;
                case 108511772: goto L_0x0160;
                case 109757538: goto L_0x0156;
                default: goto L_0x013a;
            }
        L_0x013a:
            r4 = r6
        L_0x013b:
            switch(r4) {
                case 0: goto L_0x0140;
                case 1: goto L_0x017e;
                case 2: goto L_0x018a;
                case 3: goto L_0x0196;
                case 4: goto L_0x01a2;
                default: goto L_0x013e;
            }
        L_0x013e:
            goto L_0x001f
        L_0x0140:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            android.text.Layout$Alignment r10 = android.text.Layout.Alignment.ALIGN_NORMAL
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setTextAlign(r10)
            goto L_0x001f
        L_0x014c:
            java.lang.String r10 = "left"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x013a
            r4 = r5
            goto L_0x013b
        L_0x0156:
            java.lang.String r10 = "start"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x013a
            r4 = r7
            goto L_0x013b
        L_0x0160:
            java.lang.String r10 = "right"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x013a
            r4 = r8
            goto L_0x013b
        L_0x016a:
            java.lang.String r10 = "end"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x013a
            r4 = r9
            goto L_0x013b
        L_0x0174:
            java.lang.String r10 = "center"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x013a
            r4 = 4
            goto L_0x013b
        L_0x017e:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            android.text.Layout$Alignment r10 = android.text.Layout.Alignment.ALIGN_NORMAL
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setTextAlign(r10)
            goto L_0x001f
        L_0x018a:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            android.text.Layout$Alignment r10 = android.text.Layout.Alignment.ALIGN_OPPOSITE
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setTextAlign(r10)
            goto L_0x001f
        L_0x0196:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            android.text.Layout$Alignment r10 = android.text.Layout.Alignment.ALIGN_OPPOSITE
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setTextAlign(r10)
            goto L_0x001f
        L_0x01a2:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            android.text.Layout$Alignment r10 = android.text.Layout.Alignment.ALIGN_CENTER
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setTextAlign(r10)
            goto L_0x001f
        L_0x01ae:
            java.lang.String r4 = com.google.android.exoplayer2.util.Util.toLowerInvariant(r1)
            int r10 = r4.hashCode()
            switch(r10) {
                case -1461280213: goto L_0x01e7;
                case -1026963764: goto L_0x01dd;
                case 913457136: goto L_0x01d3;
                case 1679736913: goto L_0x01c9;
                default: goto L_0x01b9;
            }
        L_0x01b9:
            r4 = r6
        L_0x01ba:
            switch(r4) {
                case 0: goto L_0x01bf;
                case 1: goto L_0x01f1;
                case 2: goto L_0x01fb;
                case 3: goto L_0x0205;
                default: goto L_0x01bd;
            }
        L_0x01bd:
            goto L_0x001f
        L_0x01bf:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setLinethrough(r7)
            goto L_0x001f
        L_0x01c9:
            java.lang.String r10 = "linethrough"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x01b9
            r4 = r5
            goto L_0x01ba
        L_0x01d3:
            java.lang.String r10 = "nolinethrough"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x01b9
            r4 = r7
            goto L_0x01ba
        L_0x01dd:
            java.lang.String r10 = "underline"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x01b9
            r4 = r8
            goto L_0x01ba
        L_0x01e7:
            java.lang.String r10 = "nounderline"
            boolean r4 = r4.equals(r10)
            if (r4 == 0) goto L_0x01b9
            r4 = r9
            goto L_0x01ba
        L_0x01f1:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setLinethrough(r5)
            goto L_0x001f
        L_0x01fb:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setUnderline(r7)
            goto L_0x001f
        L_0x0205:
            com.google.android.exoplayer2.text.ttml.TtmlStyle r4 = r12.createIfNull(r14)
            com.google.android.exoplayer2.text.ttml.TtmlStyle r14 = r4.setUnderline(r5)
            goto L_0x001f
        L_0x020f:
            return r14
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.text.ttml.TtmlDecoder.parseStyleAttributes(org.xmlpull.v1.XmlPullParser, com.google.android.exoplayer2.text.ttml.TtmlStyle):com.google.android.exoplayer2.text.ttml.TtmlStyle");
    }

    private TtmlStyle createIfNull(TtmlStyle style) {
        return style == null ? new TtmlStyle() : style;
    }

    private TtmlNode parseNode(XmlPullParser parser, TtmlNode parent, Map<String, TtmlRegion> regionMap, FrameAndTickRate frameAndTickRate) throws SubtitleDecoderException {
        long duration = C.TIME_UNSET;
        long startTime = C.TIME_UNSET;
        long endTime = C.TIME_UNSET;
        String regionId = "";
        String[] styleIds = null;
        int attributeCount = parser.getAttributeCount();
        TtmlStyle style = parseStyleAttributes(parser, (TtmlStyle) null);
        for (int i = 0; i < attributeCount; i++) {
            String attr = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            char c = 65535;
            switch (attr.hashCode()) {
                case -934795532:
                    if (attr.equals("region")) {
                        c = 4;
                        break;
                    }
                    break;
                case 99841:
                    if (attr.equals(ATTR_DURATION)) {
                        c = 2;
                        break;
                    }
                    break;
                case 100571:
                    if (attr.equals("end")) {
                        c = 1;
                        break;
                    }
                    break;
                case 93616297:
                    if (attr.equals(ATTR_BEGIN)) {
                        c = 0;
                        break;
                    }
                    break;
                case 109780401:
                    if (attr.equals("style")) {
                        c = 3;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    startTime = parseTimeExpression(value, frameAndTickRate);
                    break;
                case 1:
                    endTime = parseTimeExpression(value, frameAndTickRate);
                    break;
                case 2:
                    duration = parseTimeExpression(value, frameAndTickRate);
                    break;
                case 3:
                    String[] ids = parseStyleIds(value);
                    if (ids.length <= 0) {
                        break;
                    } else {
                        styleIds = ids;
                        break;
                    }
                case 4:
                    if (!regionMap.containsKey(value)) {
                        break;
                    } else {
                        regionId = value;
                        break;
                    }
            }
        }
        if (!(parent == null || parent.startTimeUs == C.TIME_UNSET)) {
            if (startTime != C.TIME_UNSET) {
                startTime += parent.startTimeUs;
            }
            if (endTime != C.TIME_UNSET) {
                endTime += parent.startTimeUs;
            }
        }
        if (endTime == C.TIME_UNSET) {
            if (duration != C.TIME_UNSET) {
                endTime = startTime + duration;
            } else if (!(parent == null || parent.endTimeUs == C.TIME_UNSET)) {
                endTime = parent.endTimeUs;
            }
        }
        return TtmlNode.buildNode(parser.getName(), startTime, endTime, style, styleIds, regionId);
    }

    private static boolean isSupportedTag(String tag) {
        if (tag.equals(TtmlNode.TAG_TT) || tag.equals(TtmlNode.TAG_HEAD) || tag.equals(TtmlNode.TAG_BODY) || tag.equals(TtmlNode.TAG_DIV) || tag.equals(TtmlNode.TAG_P) || tag.equals(TtmlNode.TAG_SPAN) || tag.equals(TtmlNode.TAG_BR) || tag.equals("style") || tag.equals(TtmlNode.TAG_STYLING) || tag.equals(TtmlNode.TAG_LAYOUT) || tag.equals("region") || tag.equals(TtmlNode.TAG_METADATA) || tag.equals(TtmlNode.TAG_SMPTE_IMAGE) || tag.equals(TtmlNode.TAG_SMPTE_DATA) || tag.equals(TtmlNode.TAG_SMPTE_INFORMATION)) {
            return true;
        }
        return false;
    }

    private static void parseFontSize(String expression, TtmlStyle out) throws SubtitleDecoderException {
        Matcher matcher;
        String[] expressions = expression.split("\\s+");
        if (expressions.length == 1) {
            matcher = FONT_SIZE.matcher(expression);
        } else if (expressions.length == 2) {
            matcher = FONT_SIZE.matcher(expressions[1]);
            Log.w(TAG, "Multiple values in fontSize attribute. Picking the second value for vertical font size and ignoring the first.");
        } else {
            throw new SubtitleDecoderException("Invalid number of entries for fontSize: " + expressions.length + ".");
        }
        if (matcher.matches()) {
            String unit = matcher.group(3);
            char c = 65535;
            switch (unit.hashCode()) {
                case 37:
                    if (unit.equals("%")) {
                        c = 2;
                        break;
                    }
                    break;
                case 3240:
                    if (unit.equals("em")) {
                        c = 1;
                        break;
                    }
                    break;
                case 3592:
                    if (unit.equals("px")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    out.setFontSizeUnit(1);
                    break;
                case 1:
                    out.setFontSizeUnit(2);
                    break;
                case 2:
                    out.setFontSizeUnit(3);
                    break;
                default:
                    throw new SubtitleDecoderException("Invalid unit for fontSize: '" + unit + "'.");
            }
            out.setFontSize(Float.valueOf(matcher.group(1)).floatValue());
            return;
        }
        throw new SubtitleDecoderException("Invalid expression for fontSize: '" + expression + "'.");
    }

    private static long parseTimeExpression(String time, FrameAndTickRate frameAndTickRate) throws SubtitleDecoderException {
        Matcher matcher = CLOCK_TIME.matcher(time);
        if (matcher.matches()) {
            double durationSeconds = ((double) (Long.parseLong(matcher.group(1)) * 3600)) + ((double) (Long.parseLong(matcher.group(2)) * 60)) + ((double) Long.parseLong(matcher.group(3)));
            String fraction = matcher.group(4);
            double durationSeconds2 = durationSeconds + (fraction != null ? Double.parseDouble(fraction) : 0.0d);
            String frames = matcher.group(5);
            double durationSeconds3 = durationSeconds2 + (frames != null ? (double) (((float) Long.parseLong(frames)) / frameAndTickRate.effectiveFrameRate) : 0.0d);
            String subframes = matcher.group(6);
            return (long) (1000000.0d * (durationSeconds3 + (subframes != null ? (((double) Long.parseLong(subframes)) / ((double) frameAndTickRate.subFrameRate)) / ((double) frameAndTickRate.effectiveFrameRate) : 0.0d)));
        }
        Matcher matcher2 = OFFSET_TIME.matcher(time);
        if (matcher2.matches()) {
            double offsetSeconds = Double.parseDouble(matcher2.group(1));
            String unit = matcher2.group(2);
            char c = 65535;
            switch (unit.hashCode()) {
                case 102:
                    if (unit.equals("f")) {
                        c = 4;
                        break;
                    }
                    break;
                case 104:
                    if (unit.equals("h")) {
                        c = 0;
                        break;
                    }
                    break;
                case 109:
                    if (unit.equals("m")) {
                        c = 1;
                        break;
                    }
                    break;
                case 115:
                    if (unit.equals("s")) {
                        c = 2;
                        break;
                    }
                    break;
                case 116:
                    if (unit.equals("t")) {
                        c = 5;
                        break;
                    }
                    break;
                case 3494:
                    if (unit.equals("ms")) {
                        c = 3;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    offsetSeconds *= 3600.0d;
                    break;
                case 1:
                    offsetSeconds *= 60.0d;
                    break;
                case 3:
                    offsetSeconds /= 1000.0d;
                    break;
                case 4:
                    offsetSeconds /= (double) frameAndTickRate.effectiveFrameRate;
                    break;
                case 5:
                    offsetSeconds /= (double) frameAndTickRate.tickRate;
                    break;
            }
            return (long) (1000000.0d * offsetSeconds);
        }
        throw new SubtitleDecoderException("Malformed time expression: " + time);
    }

    private static final class FrameAndTickRate {
        final float effectiveFrameRate;
        final int subFrameRate;
        final int tickRate;

        FrameAndTickRate(float effectiveFrameRate2, int subFrameRate2, int tickRate2) {
            this.effectiveFrameRate = effectiveFrameRate2;
            this.subFrameRate = subFrameRate2;
            this.tickRate = tickRate2;
        }
    }
}
