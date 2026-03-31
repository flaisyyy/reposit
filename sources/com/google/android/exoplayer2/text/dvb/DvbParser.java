package com.google.android.exoplayer2.text.dvb;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Region;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.util.ParsableBitArray;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DvbParser {
    private static final int DATA_TYPE_24_TABLE_DATA = 32;
    private static final int DATA_TYPE_28_TABLE_DATA = 33;
    private static final int DATA_TYPE_2BP_CODE_STRING = 16;
    private static final int DATA_TYPE_48_TABLE_DATA = 34;
    private static final int DATA_TYPE_4BP_CODE_STRING = 17;
    private static final int DATA_TYPE_8BP_CODE_STRING = 18;
    private static final int DATA_TYPE_END_LINE = 240;
    private static final int OBJECT_CODING_PIXELS = 0;
    private static final int OBJECT_CODING_STRING = 1;
    private static final int PAGE_STATE_NORMAL = 0;
    private static final int REGION_DEPTH_4_BIT = 2;
    private static final int REGION_DEPTH_8_BIT = 3;
    private static final int SEGMENT_TYPE_CLUT_DEFINITION = 18;
    private static final int SEGMENT_TYPE_DISPLAY_DEFINITION = 20;
    private static final int SEGMENT_TYPE_OBJECT_DATA = 19;
    private static final int SEGMENT_TYPE_PAGE_COMPOSITION = 16;
    private static final int SEGMENT_TYPE_REGION_COMPOSITION = 17;
    private static final String TAG = "DvbParser";
    private static final byte[] defaultMap2To4 = {0, 7, 8, 15};
    private static final byte[] defaultMap2To8 = {0, 119, -120, -1};
    private static final byte[] defaultMap4To8 = {0, 17, 34, 51, 68, 85, 102, 119, -120, -103, -86, -69, -52, -35, -18, -1};
    private Bitmap bitmap;
    private final Canvas canvas;
    private final ClutDefinition defaultClutDefinition;
    private final DisplayDefinition defaultDisplayDefinition;
    private final Paint defaultPaint = new Paint();
    private final Paint fillRegionPaint;
    private final SubtitleService subtitleService;

    public DvbParser(int subtitlePageId, int ancillaryPageId) {
        this.defaultPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.defaultPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.defaultPaint.setPathEffect((PathEffect) null);
        this.fillRegionPaint = new Paint();
        this.fillRegionPaint.setStyle(Paint.Style.FILL);
        this.fillRegionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        this.fillRegionPaint.setPathEffect((PathEffect) null);
        this.canvas = new Canvas();
        this.defaultDisplayDefinition = new DisplayDefinition(719, 575, 0, 719, 0, 575);
        this.defaultClutDefinition = new ClutDefinition(0, generateDefault2BitClutEntries(), generateDefault4BitClutEntries(), generateDefault8BitClutEntries());
        this.subtitleService = new SubtitleService(subtitlePageId, ancillaryPageId);
    }

    public void reset() {
        this.subtitleService.reset();
    }

    public List<Cue> decode(byte[] data, int limit) {
        DisplayDefinition displayDefinition;
        int color;
        ParsableBitArray parsableBitArray = new ParsableBitArray(data, limit);
        while (parsableBitArray.bitsLeft() >= 48 && parsableBitArray.readBits(8) == 15) {
            parseSubtitlingSegment(parsableBitArray, this.subtitleService);
        }
        if (this.subtitleService.pageComposition == null) {
            return Collections.emptyList();
        }
        if (this.subtitleService.displayDefinition != null) {
            displayDefinition = this.subtitleService.displayDefinition;
        } else {
            displayDefinition = this.defaultDisplayDefinition;
        }
        if (!(this.bitmap != null && displayDefinition.width + 1 == this.bitmap.getWidth() && displayDefinition.height + 1 == this.bitmap.getHeight())) {
            this.bitmap = Bitmap.createBitmap(displayDefinition.width + 1, displayDefinition.height + 1, Bitmap.Config.ARGB_8888);
            this.canvas.setBitmap(this.bitmap);
        }
        ArrayList arrayList = new ArrayList();
        SparseArray<PageRegion> pageRegions = this.subtitleService.pageComposition.regions;
        for (int i = 0; i < pageRegions.size(); i++) {
            PageRegion pageRegion = pageRegions.valueAt(i);
            RegionComposition regionComposition = this.subtitleService.regions.get(pageRegions.keyAt(i));
            int baseHorizontalAddress = pageRegion.horizontalAddress + displayDefinition.horizontalPositionMinimum;
            int baseVerticalAddress = pageRegion.verticalAddress + displayDefinition.verticalPositionMinimum;
            this.canvas.clipRect((float) baseHorizontalAddress, (float) baseVerticalAddress, (float) Math.min(regionComposition.width + baseHorizontalAddress, displayDefinition.horizontalPositionMaximum), (float) Math.min(regionComposition.height + baseVerticalAddress, displayDefinition.verticalPositionMaximum), Region.Op.REPLACE);
            ClutDefinition clutDefinition = this.subtitleService.cluts.get(regionComposition.clutId);
            if (clutDefinition == null && (clutDefinition = this.subtitleService.ancillaryCluts.get(regionComposition.clutId)) == null) {
                clutDefinition = this.defaultClutDefinition;
            }
            SparseArray<RegionObject> regionObjects = regionComposition.regionObjects;
            for (int j = 0; j < regionObjects.size(); j++) {
                int objectId = regionObjects.keyAt(j);
                RegionObject regionObject = regionObjects.valueAt(j);
                ObjectData objectData = this.subtitleService.objects.get(objectId);
                if (objectData == null) {
                    objectData = this.subtitleService.ancillaryObjects.get(objectId);
                }
                if (objectData != null) {
                    paintPixelDataSubBlocks(objectData, clutDefinition, regionComposition.depth, regionObject.horizontalPosition + baseHorizontalAddress, regionObject.verticalPosition + baseVerticalAddress, objectData.nonModifyingColorFlag ? null : this.defaultPaint, this.canvas);
                }
            }
            if (regionComposition.fillFlag) {
                if (regionComposition.depth == 3) {
                    color = clutDefinition.clutEntries8Bit[regionComposition.pixelCode8Bit];
                } else if (regionComposition.depth == 2) {
                    color = clutDefinition.clutEntries4Bit[regionComposition.pixelCode4Bit];
                } else {
                    color = clutDefinition.clutEntries2Bit[regionComposition.pixelCode2Bit];
                }
                this.fillRegionPaint.setColor(color);
                this.canvas.drawRect((float) baseHorizontalAddress, (float) baseVerticalAddress, (float) (regionComposition.width + baseHorizontalAddress), (float) (regionComposition.height + baseVerticalAddress), this.fillRegionPaint);
            }
            arrayList.add(new Cue(Bitmap.createBitmap(this.bitmap, baseHorizontalAddress, baseVerticalAddress, regionComposition.width, regionComposition.height), ((float) baseHorizontalAddress) / ((float) displayDefinition.width), 0, ((float) baseVerticalAddress) / ((float) displayDefinition.height), 0, ((float) regionComposition.width) / ((float) displayDefinition.width), ((float) regionComposition.height) / ((float) displayDefinition.height)));
            this.canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        return arrayList;
    }

    private static void parseSubtitlingSegment(ParsableBitArray data, SubtitleService service) {
        int segmentType = data.readBits(8);
        int pageId = data.readBits(16);
        int dataFieldLength = data.readBits(16);
        int dataFieldLimit = data.getBytePosition() + dataFieldLength;
        if (dataFieldLength * 8 > data.bitsLeft()) {
            Log.w(TAG, "Data field length exceeds limit");
            data.skipBits(data.bitsLeft());
            return;
        }
        switch (segmentType) {
            case 16:
                if (pageId == service.subtitlePageId) {
                    PageComposition current = service.pageComposition;
                    PageComposition pageComposition = parsePageComposition(data, dataFieldLength);
                    if (pageComposition.state == 0) {
                        if (!(current == null || current.version == pageComposition.version)) {
                            service.pageComposition = pageComposition;
                            break;
                        }
                    } else {
                        service.pageComposition = pageComposition;
                        service.regions.clear();
                        service.cluts.clear();
                        service.objects.clear();
                        break;
                    }
                }
                break;
            case 17:
                PageComposition pageComposition2 = service.pageComposition;
                if (pageId == service.subtitlePageId && pageComposition2 != null) {
                    RegionComposition regionComposition = parseRegionComposition(data, dataFieldLength);
                    if (pageComposition2.state == 0) {
                        regionComposition.mergeFrom(service.regions.get(regionComposition.id));
                    }
                    service.regions.put(regionComposition.id, regionComposition);
                    break;
                }
            case 18:
                if (pageId != service.subtitlePageId) {
                    if (pageId == service.ancillaryPageId) {
                        ClutDefinition clutDefinition = parseClutDefinition(data, dataFieldLength);
                        service.ancillaryCluts.put(clutDefinition.id, clutDefinition);
                        break;
                    }
                } else {
                    ClutDefinition clutDefinition2 = parseClutDefinition(data, dataFieldLength);
                    service.cluts.put(clutDefinition2.id, clutDefinition2);
                    break;
                }
                break;
            case 19:
                if (pageId != service.subtitlePageId) {
                    if (pageId == service.ancillaryPageId) {
                        ObjectData objectData = parseObjectData(data);
                        service.ancillaryObjects.put(objectData.id, objectData);
                        break;
                    }
                } else {
                    ObjectData objectData2 = parseObjectData(data);
                    service.objects.put(objectData2.id, objectData2);
                    break;
                }
                break;
            case 20:
                if (pageId == service.subtitlePageId) {
                    service.displayDefinition = parseDisplayDefinition(data);
                    break;
                }
                break;
        }
        data.skipBytes(dataFieldLimit - data.getBytePosition());
    }

    private static DisplayDefinition parseDisplayDefinition(ParsableBitArray data) {
        int horizontalPositionMinimum;
        int horizontalPositionMaximum;
        int verticalPositionMinimum;
        int verticalPositionMaximum;
        data.skipBits(4);
        boolean displayWindowFlag = data.readBit();
        data.skipBits(3);
        int width = data.readBits(16);
        int height = data.readBits(16);
        if (displayWindowFlag) {
            horizontalPositionMinimum = data.readBits(16);
            horizontalPositionMaximum = data.readBits(16);
            verticalPositionMinimum = data.readBits(16);
            verticalPositionMaximum = data.readBits(16);
        } else {
            horizontalPositionMinimum = 0;
            horizontalPositionMaximum = width;
            verticalPositionMinimum = 0;
            verticalPositionMaximum = height;
        }
        return new DisplayDefinition(width, height, horizontalPositionMinimum, horizontalPositionMaximum, verticalPositionMinimum, verticalPositionMaximum);
    }

    private static PageComposition parsePageComposition(ParsableBitArray data, int length) {
        int timeoutSecs = data.readBits(8);
        int version = data.readBits(4);
        int state = data.readBits(2);
        data.skipBits(2);
        int remainingLength = length - 2;
        SparseArray<PageRegion> regions = new SparseArray<>();
        while (remainingLength > 0) {
            int regionId = data.readBits(8);
            data.skipBits(8);
            remainingLength -= 6;
            regions.put(regionId, new PageRegion(data.readBits(16), data.readBits(16)));
        }
        return new PageComposition(timeoutSecs, version, state, regions);
    }

    private static RegionComposition parseRegionComposition(ParsableBitArray data, int length) {
        int id = data.readBits(8);
        data.skipBits(4);
        boolean fillFlag = data.readBit();
        data.skipBits(3);
        int width = data.readBits(16);
        int height = data.readBits(16);
        int levelOfCompatibility = data.readBits(3);
        int depth = data.readBits(3);
        data.skipBits(2);
        int clutId = data.readBits(8);
        int pixelCode8Bit = data.readBits(8);
        int pixelCode4Bit = data.readBits(4);
        int pixelCode2Bit = data.readBits(2);
        data.skipBits(2);
        int remainingLength = length - 10;
        SparseArray<RegionObject> regionObjects = new SparseArray<>();
        while (remainingLength > 0) {
            int objectId = data.readBits(16);
            int objectType = data.readBits(2);
            int objectProvider = data.readBits(2);
            int objectHorizontalPosition = data.readBits(12);
            data.skipBits(4);
            int objectVerticalPosition = data.readBits(12);
            remainingLength -= 6;
            int foregroundPixelCode = 0;
            int backgroundPixelCode = 0;
            if (objectType == 1 || objectType == 2) {
                foregroundPixelCode = data.readBits(8);
                backgroundPixelCode = data.readBits(8);
                remainingLength -= 2;
            }
            regionObjects.put(objectId, new RegionObject(objectType, objectProvider, objectHorizontalPosition, objectVerticalPosition, foregroundPixelCode, backgroundPixelCode));
        }
        return new RegionComposition(id, fillFlag, width, height, levelOfCompatibility, depth, clutId, pixelCode8Bit, pixelCode4Bit, pixelCode2Bit, regionObjects);
    }

    private static ClutDefinition parseClutDefinition(ParsableBitArray data, int length) {
        int[] clutEntries;
        int y;
        int cr;
        int cb;
        int t;
        int clutId = data.readBits(8);
        data.skipBits(8);
        int remainingLength = length - 2;
        int[] clutEntries2Bit = generateDefault2BitClutEntries();
        int[] clutEntries4Bit = generateDefault4BitClutEntries();
        int[] clutEntries8Bit = generateDefault8BitClutEntries();
        while (remainingLength > 0) {
            int entryId = data.readBits(8);
            int entryFlags = data.readBits(8);
            int remainingLength2 = remainingLength - 2;
            if ((entryFlags & 128) != 0) {
                clutEntries = clutEntries2Bit;
            } else if ((entryFlags & 64) != 0) {
                clutEntries = clutEntries4Bit;
            } else {
                clutEntries = clutEntries8Bit;
            }
            if ((entryFlags & 1) != 0) {
                y = data.readBits(8);
                cr = data.readBits(8);
                cb = data.readBits(8);
                t = data.readBits(8);
                remainingLength = remainingLength2 - 4;
            } else {
                y = data.readBits(6) << 2;
                cr = data.readBits(4) << 4;
                cb = data.readBits(4) << 4;
                t = data.readBits(2) << 6;
                remainingLength = remainingLength2 - 2;
            }
            if (y == 0) {
                cr = 0;
                cb = 0;
                t = 255;
            }
            clutEntries[entryId] = getColor((byte) (255 - (t & 255)), Util.constrainValue((int) (((double) y) + (1.402d * ((double) (cr - 128)))), 0, 255), Util.constrainValue((int) ((((double) y) - (0.34414d * ((double) (cb - 128)))) - (0.71414d * ((double) (cr - 128)))), 0, 255), Util.constrainValue((int) (((double) y) + (1.772d * ((double) (cb - 128)))), 0, 255));
        }
        return new ClutDefinition(clutId, clutEntries2Bit, clutEntries4Bit, clutEntries8Bit);
    }

    private static ObjectData parseObjectData(ParsableBitArray data) {
        int objectId = data.readBits(16);
        data.skipBits(4);
        int objectCodingMethod = data.readBits(2);
        boolean nonModifyingColorFlag = data.readBit();
        data.skipBits(1);
        byte[] topFieldData = null;
        byte[] bottomFieldData = null;
        if (objectCodingMethod == 1) {
            data.skipBits(data.readBits(8) * 16);
        } else if (objectCodingMethod == 0) {
            int topFieldDataLength = data.readBits(16);
            int bottomFieldDataLength = data.readBits(16);
            if (topFieldDataLength > 0) {
                topFieldData = new byte[topFieldDataLength];
                data.readBytes(topFieldData, 0, topFieldDataLength);
            }
            if (bottomFieldDataLength > 0) {
                bottomFieldData = new byte[bottomFieldDataLength];
                data.readBytes(bottomFieldData, 0, bottomFieldDataLength);
            } else {
                bottomFieldData = topFieldData;
            }
        }
        return new ObjectData(objectId, nonModifyingColorFlag, topFieldData, bottomFieldData);
    }

    private static int[] generateDefault2BitClutEntries() {
        return new int[]{0, -1, -16777216, -8421505};
    }

    private static int[] generateDefault4BitClutEntries() {
        int i;
        int[] entries = new int[16];
        entries[0] = 0;
        for (int i2 = 1; i2 < entries.length; i2++) {
            if (i2 < 8) {
                if ((i2 & 1) != 0) {
                    i = 255;
                } else {
                    i = 0;
                }
                entries[i2] = getColor(255, i, (i2 & 2) != 0 ? 255 : 0, (i2 & 4) != 0 ? 255 : 0);
            } else {
                entries[i2] = getColor(255, (i2 & 1) != 0 ? 127 : 0, (i2 & 2) != 0 ? 127 : 0, (i2 & 4) != 0 ? 127 : 0);
            }
        }
        return entries;
    }

    private static int[] generateDefault8BitClutEntries() {
        int i;
        int[] entries = new int[256];
        entries[0] = 0;
        for (int i2 = 0; i2 < entries.length; i2++) {
            if (i2 >= 8) {
                switch (i2 & 136) {
                    case 0:
                        entries[i2] = getColor(255, ((i2 & 1) != 0 ? 85 : 0) + ((i2 & 16) != 0 ? 170 : 0), ((i2 & 2) != 0 ? 85 : 0) + ((i2 & 32) != 0 ? 170 : 0), ((i2 & 64) != 0 ? 170 : 0) + ((i2 & 4) != 0 ? 85 : 0));
                        break;
                    case 8:
                        entries[i2] = getColor(127, ((i2 & 1) != 0 ? 85 : 0) + ((i2 & 16) != 0 ? 170 : 0), ((i2 & 2) != 0 ? 85 : 0) + ((i2 & 32) != 0 ? 170 : 0), ((i2 & 64) != 0 ? 170 : 0) + ((i2 & 4) != 0 ? 85 : 0));
                        break;
                    case 128:
                        entries[i2] = getColor(255, ((i2 & 1) != 0 ? 43 : 0) + 127 + ((i2 & 16) != 0 ? 85 : 0), ((i2 & 2) != 0 ? 43 : 0) + 127 + ((i2 & 32) != 0 ? 85 : 0), ((i2 & 64) != 0 ? 85 : 0) + ((i2 & 4) != 0 ? 43 : 0) + 127);
                        break;
                    case 136:
                        entries[i2] = getColor(255, ((i2 & 1) != 0 ? 43 : 0) + ((i2 & 16) != 0 ? 85 : 0), ((i2 & 2) != 0 ? 43 : 0) + ((i2 & 32) != 0 ? 85 : 0), ((i2 & 64) != 0 ? 85 : 0) + ((i2 & 4) != 0 ? 43 : 0));
                        break;
                }
            } else {
                if ((i2 & 1) != 0) {
                    i = 255;
                } else {
                    i = 0;
                }
                entries[i2] = getColor(63, i, (i2 & 2) != 0 ? 255 : 0, (i2 & 4) != 0 ? 255 : 0);
            }
        }
        return entries;
    }

    private static int getColor(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void paintPixelDataSubBlocks(ObjectData objectData, ClutDefinition clutDefinition, int regionDepth, int horizontalAddress, int verticalAddress, Paint paint, Canvas canvas2) {
        int[] clutEntries;
        if (regionDepth == 3) {
            clutEntries = clutDefinition.clutEntries8Bit;
        } else if (regionDepth == 2) {
            clutEntries = clutDefinition.clutEntries4Bit;
        } else {
            clutEntries = clutDefinition.clutEntries2Bit;
        }
        paintPixelDataSubBlock(objectData.topFieldData, clutEntries, regionDepth, horizontalAddress, verticalAddress, paint, canvas2);
        paintPixelDataSubBlock(objectData.bottomFieldData, clutEntries, regionDepth, horizontalAddress, verticalAddress + 1, paint, canvas2);
    }

    private static void paintPixelDataSubBlock(byte[] pixelData, int[] clutEntries, int regionDepth, int horizontalAddress, int verticalAddress, Paint paint, Canvas canvas2) {
        byte[] clutMapTable4ToX;
        byte[] clutMapTable2ToX;
        ParsableBitArray data = new ParsableBitArray(pixelData);
        int column = horizontalAddress;
        int line = verticalAddress;
        byte[] clutMapTable2To4 = null;
        byte[] clutMapTable2To8 = null;
        while (data.bitsLeft() != 0) {
            switch (data.readBits(8)) {
                case 16:
                    if (regionDepth == 3) {
                        clutMapTable2ToX = clutMapTable2To8 == null ? defaultMap2To8 : clutMapTable2To8;
                    } else if (regionDepth == 2) {
                        clutMapTable2ToX = clutMapTable2To4 == null ? defaultMap2To4 : clutMapTable2To4;
                    } else {
                        clutMapTable2ToX = null;
                    }
                    column = paint2BitPixelCodeString(data, clutEntries, clutMapTable2ToX, column, line, paint, canvas2);
                    data.byteAlign();
                    break;
                case 17:
                    if (regionDepth != 3) {
                        clutMapTable4ToX = null;
                    } else if (0 == 0) {
                        clutMapTable4ToX = defaultMap4To8;
                    } else {
                        clutMapTable4ToX = null;
                    }
                    column = paint4BitPixelCodeString(data, clutEntries, clutMapTable4ToX, column, line, paint, canvas2);
                    data.byteAlign();
                    break;
                case 18:
                    column = paint8BitPixelCodeString(data, clutEntries, (byte[]) null, column, line, paint, canvas2);
                    break;
                case 32:
                    clutMapTable2To4 = buildClutMapTable(4, 4, data);
                    break;
                case 33:
                    clutMapTable2To8 = buildClutMapTable(4, 8, data);
                    break;
                case 34:
                    clutMapTable2To8 = buildClutMapTable(16, 8, data);
                    break;
                case 240:
                    column = horizontalAddress;
                    line += 2;
                    break;
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v8, resolved type: byte} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int paint2BitPixelCodeString(com.google.android.exoplayer2.util.ParsableBitArray r10, int[] r11, byte[] r12, int r13, int r14, android.graphics.Paint r15, android.graphics.Canvas r16) {
        /*
            r7 = 0
        L_0x0001:
            r9 = 0
            r6 = 0
            r0 = 2
            int r8 = r10.readBits(r0)
            if (r8 == 0) goto L_0x002b
            r9 = 1
            r6 = r8
        L_0x000c:
            if (r9 == 0) goto L_0x0027
            if (r15 == 0) goto L_0x0027
            if (r12 == 0) goto L_0x0014
            byte r6 = r12[r6]
        L_0x0014:
            r0 = r11[r6]
            r15.setColor(r0)
            float r1 = (float) r13
            float r2 = (float) r14
            int r0 = r13 + r9
            float r3 = (float) r0
            int r0 = r14 + 1
            float r4 = (float) r0
            r0 = r16
            r5 = r15
            r0.drawRect(r1, r2, r3, r4, r5)
        L_0x0027:
            int r13 = r13 + r9
            if (r7 == 0) goto L_0x0001
            return r13
        L_0x002b:
            boolean r0 = r10.readBit()
            if (r0 == 0) goto L_0x003e
            r0 = 3
            int r0 = r10.readBits(r0)
            int r9 = r0 + 3
            r0 = 2
            int r6 = r10.readBits(r0)
            goto L_0x000c
        L_0x003e:
            boolean r0 = r10.readBit()
            if (r0 == 0) goto L_0x0046
            r9 = 1
            goto L_0x000c
        L_0x0046:
            r0 = 2
            int r0 = r10.readBits(r0)
            switch(r0) {
                case 0: goto L_0x004f;
                case 1: goto L_0x0051;
                case 2: goto L_0x0053;
                case 3: goto L_0x0060;
                default: goto L_0x004e;
            }
        L_0x004e:
            goto L_0x000c
        L_0x004f:
            r7 = 1
            goto L_0x000c
        L_0x0051:
            r9 = 2
            goto L_0x000c
        L_0x0053:
            r0 = 4
            int r0 = r10.readBits(r0)
            int r9 = r0 + 12
            r0 = 2
            int r6 = r10.readBits(r0)
            goto L_0x000c
        L_0x0060:
            r0 = 8
            int r0 = r10.readBits(r0)
            int r9 = r0 + 29
            r0 = 2
            int r6 = r10.readBits(r0)
            goto L_0x000c
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.text.dvb.DvbParser.paint2BitPixelCodeString(com.google.android.exoplayer2.util.ParsableBitArray, int[], byte[], int, int, android.graphics.Paint, android.graphics.Canvas):int");
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v9, resolved type: byte} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int paint4BitPixelCodeString(com.google.android.exoplayer2.util.ParsableBitArray r10, int[] r11, byte[] r12, int r13, int r14, android.graphics.Paint r15, android.graphics.Canvas r16) {
        /*
            r7 = 0
        L_0x0001:
            r9 = 0
            r6 = 0
            r0 = 4
            int r8 = r10.readBits(r0)
            if (r8 == 0) goto L_0x002b
            r9 = 1
            r6 = r8
        L_0x000c:
            if (r9 == 0) goto L_0x0027
            if (r15 == 0) goto L_0x0027
            if (r12 == 0) goto L_0x0014
            byte r6 = r12[r6]
        L_0x0014:
            r0 = r11[r6]
            r15.setColor(r0)
            float r1 = (float) r13
            float r2 = (float) r14
            int r0 = r13 + r9
            float r3 = (float) r0
            int r0 = r14 + 1
            float r4 = (float) r0
            r0 = r16
            r5 = r15
            r0.drawRect(r1, r2, r3, r4, r5)
        L_0x0027:
            int r13 = r13 + r9
            if (r7 == 0) goto L_0x0001
            return r13
        L_0x002b:
            boolean r0 = r10.readBit()
            if (r0 != 0) goto L_0x003e
            r0 = 3
            int r8 = r10.readBits(r0)
            if (r8 == 0) goto L_0x003c
            int r9 = r8 + 2
            r6 = 0
            goto L_0x000c
        L_0x003c:
            r7 = 1
            goto L_0x000c
        L_0x003e:
            boolean r0 = r10.readBit()
            if (r0 != 0) goto L_0x0051
            r0 = 2
            int r0 = r10.readBits(r0)
            int r9 = r0 + 4
            r0 = 4
            int r6 = r10.readBits(r0)
            goto L_0x000c
        L_0x0051:
            r0 = 2
            int r0 = r10.readBits(r0)
            switch(r0) {
                case 0: goto L_0x005a;
                case 1: goto L_0x005c;
                case 2: goto L_0x005e;
                case 3: goto L_0x006b;
                default: goto L_0x0059;
            }
        L_0x0059:
            goto L_0x000c
        L_0x005a:
            r9 = 1
            goto L_0x000c
        L_0x005c:
            r9 = 2
            goto L_0x000c
        L_0x005e:
            r0 = 4
            int r0 = r10.readBits(r0)
            int r9 = r0 + 9
            r0 = 4
            int r6 = r10.readBits(r0)
            goto L_0x000c
        L_0x006b:
            r0 = 8
            int r0 = r10.readBits(r0)
            int r9 = r0 + 25
            r0 = 4
            int r6 = r10.readBits(r0)
            goto L_0x000c
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.text.dvb.DvbParser.paint4BitPixelCodeString(com.google.android.exoplayer2.util.ParsableBitArray, int[], byte[], int, int, android.graphics.Paint, android.graphics.Canvas):int");
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v7, resolved type: byte} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int paint8BitPixelCodeString(com.google.android.exoplayer2.util.ParsableBitArray r10, int[] r11, byte[] r12, int r13, int r14, android.graphics.Paint r15, android.graphics.Canvas r16) {
        /*
            r7 = 0
        L_0x0001:
            r9 = 0
            r6 = 0
            r0 = 8
            int r8 = r10.readBits(r0)
            if (r8 == 0) goto L_0x002c
            r9 = 1
            r6 = r8
        L_0x000d:
            if (r9 == 0) goto L_0x0028
            if (r15 == 0) goto L_0x0028
            if (r12 == 0) goto L_0x0015
            byte r6 = r12[r6]
        L_0x0015:
            r0 = r11[r6]
            r15.setColor(r0)
            float r1 = (float) r13
            float r2 = (float) r14
            int r0 = r13 + r9
            float r3 = (float) r0
            int r0 = r14 + 1
            float r4 = (float) r0
            r0 = r16
            r5 = r15
            r0.drawRect(r1, r2, r3, r4, r5)
        L_0x0028:
            int r13 = r13 + r9
            if (r7 == 0) goto L_0x0001
            return r13
        L_0x002c:
            boolean r0 = r10.readBit()
            if (r0 != 0) goto L_0x003e
            r0 = 7
            int r8 = r10.readBits(r0)
            if (r8 == 0) goto L_0x003c
            r9 = r8
            r6 = 0
            goto L_0x000d
        L_0x003c:
            r7 = 1
            goto L_0x000d
        L_0x003e:
            r0 = 7
            int r9 = r10.readBits(r0)
            r0 = 8
            int r6 = r10.readBits(r0)
            goto L_0x000d
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.text.dvb.DvbParser.paint8BitPixelCodeString(com.google.android.exoplayer2.util.ParsableBitArray, int[], byte[], int, int, android.graphics.Paint, android.graphics.Canvas):int");
    }

    private static byte[] buildClutMapTable(int length, int bitsPerEntry, ParsableBitArray data) {
        byte[] clutMapTable = new byte[length];
        for (int i = 0; i < length; i++) {
            clutMapTable[i] = (byte) data.readBits(bitsPerEntry);
        }
        return clutMapTable;
    }

    private static final class SubtitleService {
        public final SparseArray<ClutDefinition> ancillaryCluts = new SparseArray<>();
        public final SparseArray<ObjectData> ancillaryObjects = new SparseArray<>();
        public final int ancillaryPageId;
        public final SparseArray<ClutDefinition> cluts = new SparseArray<>();
        public DisplayDefinition displayDefinition;
        public final SparseArray<ObjectData> objects = new SparseArray<>();
        public PageComposition pageComposition;
        public final SparseArray<RegionComposition> regions = new SparseArray<>();
        public final int subtitlePageId;

        public SubtitleService(int subtitlePageId2, int ancillaryPageId2) {
            this.subtitlePageId = subtitlePageId2;
            this.ancillaryPageId = ancillaryPageId2;
        }

        public void reset() {
            this.regions.clear();
            this.cluts.clear();
            this.objects.clear();
            this.ancillaryCluts.clear();
            this.ancillaryObjects.clear();
            this.displayDefinition = null;
            this.pageComposition = null;
        }
    }

    private static final class DisplayDefinition {
        public final int height;
        public final int horizontalPositionMaximum;
        public final int horizontalPositionMinimum;
        public final int verticalPositionMaximum;
        public final int verticalPositionMinimum;
        public final int width;

        public DisplayDefinition(int width2, int height2, int horizontalPositionMinimum2, int horizontalPositionMaximum2, int verticalPositionMinimum2, int verticalPositionMaximum2) {
            this.width = width2;
            this.height = height2;
            this.horizontalPositionMinimum = horizontalPositionMinimum2;
            this.horizontalPositionMaximum = horizontalPositionMaximum2;
            this.verticalPositionMinimum = verticalPositionMinimum2;
            this.verticalPositionMaximum = verticalPositionMaximum2;
        }
    }

    private static final class PageComposition {
        public final SparseArray<PageRegion> regions;
        public final int state;
        public final int timeOutSecs;
        public final int version;

        public PageComposition(int timeoutSecs, int version2, int state2, SparseArray<PageRegion> regions2) {
            this.timeOutSecs = timeoutSecs;
            this.version = version2;
            this.state = state2;
            this.regions = regions2;
        }
    }

    private static final class PageRegion {
        public final int horizontalAddress;
        public final int verticalAddress;

        public PageRegion(int horizontalAddress2, int verticalAddress2) {
            this.horizontalAddress = horizontalAddress2;
            this.verticalAddress = verticalAddress2;
        }
    }

    private static final class RegionComposition {
        public final int clutId;
        public final int depth;
        public final boolean fillFlag;
        public final int height;
        public final int id;
        public final int levelOfCompatibility;
        public final int pixelCode2Bit;
        public final int pixelCode4Bit;
        public final int pixelCode8Bit;
        public final SparseArray<RegionObject> regionObjects;
        public final int width;

        public RegionComposition(int id2, boolean fillFlag2, int width2, int height2, int levelOfCompatibility2, int depth2, int clutId2, int pixelCode8Bit2, int pixelCode4Bit2, int pixelCode2Bit2, SparseArray<RegionObject> regionObjects2) {
            this.id = id2;
            this.fillFlag = fillFlag2;
            this.width = width2;
            this.height = height2;
            this.levelOfCompatibility = levelOfCompatibility2;
            this.depth = depth2;
            this.clutId = clutId2;
            this.pixelCode8Bit = pixelCode8Bit2;
            this.pixelCode4Bit = pixelCode4Bit2;
            this.pixelCode2Bit = pixelCode2Bit2;
            this.regionObjects = regionObjects2;
        }

        public void mergeFrom(RegionComposition otherRegionComposition) {
            if (otherRegionComposition != null) {
                SparseArray<RegionObject> otherRegionObjects = otherRegionComposition.regionObjects;
                for (int i = 0; i < otherRegionObjects.size(); i++) {
                    this.regionObjects.put(otherRegionObjects.keyAt(i), otherRegionObjects.valueAt(i));
                }
            }
        }
    }

    private static final class RegionObject {
        public final int backgroundPixelCode;
        public final int foregroundPixelCode;
        public final int horizontalPosition;
        public final int provider;
        public final int type;
        public final int verticalPosition;

        public RegionObject(int type2, int provider2, int horizontalPosition2, int verticalPosition2, int foregroundPixelCode2, int backgroundPixelCode2) {
            this.type = type2;
            this.provider = provider2;
            this.horizontalPosition = horizontalPosition2;
            this.verticalPosition = verticalPosition2;
            this.foregroundPixelCode = foregroundPixelCode2;
            this.backgroundPixelCode = backgroundPixelCode2;
        }
    }

    private static final class ClutDefinition {
        public final int[] clutEntries2Bit;
        public final int[] clutEntries4Bit;
        public final int[] clutEntries8Bit;
        public final int id;

        public ClutDefinition(int id2, int[] clutEntries2Bit2, int[] clutEntries4Bit2, int[] clutEntries8bit) {
            this.id = id2;
            this.clutEntries2Bit = clutEntries2Bit2;
            this.clutEntries4Bit = clutEntries4Bit2;
            this.clutEntries8Bit = clutEntries8bit;
        }
    }

    private static final class ObjectData {
        public final byte[] bottomFieldData;
        public final int id;
        public final boolean nonModifyingColorFlag;
        public final byte[] topFieldData;

        public ObjectData(int id2, boolean nonModifyingColorFlag2, byte[] topFieldData2, byte[] bottomFieldData2) {
            this.id = id2;
            this.nonModifyingColorFlag = nonModifyingColorFlag2;
            this.topFieldData = topFieldData2;
            this.bottomFieldData = bottomFieldData2;
        }
    }
}
