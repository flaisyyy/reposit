package com.google.android.exoplayer2.extractor.mp4;

import android.util.Log;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.nio.ByteBuffer;
import java.util.UUID;

public final class PsshAtomUtil {
    private static final String TAG = "PsshAtomUtil";

    private PsshAtomUtil() {
    }

    public static byte[] buildPsshAtom(UUID systemId, byte[] data) {
        return buildPsshAtom(systemId, (UUID[]) null, data);
    }

    public static byte[] buildPsshAtom(UUID systemId, UUID[] keyIds, byte[] data) {
        boolean buildV1Atom;
        int dataLength;
        int i;
        if (keyIds != null) {
            buildV1Atom = true;
        } else {
            buildV1Atom = false;
        }
        if (data != null) {
            dataLength = data.length;
        } else {
            dataLength = 0;
        }
        int psshBoxLength = dataLength + 32;
        if (buildV1Atom) {
            psshBoxLength += (keyIds.length * 16) + 4;
        }
        ByteBuffer psshBox = ByteBuffer.allocate(psshBoxLength);
        psshBox.putInt(psshBoxLength);
        psshBox.putInt(Atom.TYPE_pssh);
        if (buildV1Atom) {
            i = 16777216;
        } else {
            i = 0;
        }
        psshBox.putInt(i);
        psshBox.putLong(systemId.getMostSignificantBits());
        psshBox.putLong(systemId.getLeastSignificantBits());
        if (buildV1Atom) {
            psshBox.putInt(keyIds.length);
            for (UUID keyId : keyIds) {
                psshBox.putLong(keyId.getMostSignificantBits());
                psshBox.putLong(keyId.getLeastSignificantBits());
            }
        }
        if (dataLength != 0) {
            psshBox.putInt(data.length);
            psshBox.put(data);
        }
        return psshBox.array();
    }

    public static UUID parseUuid(byte[] atom) {
        PsshAtom parsedAtom = parsePsshAtom(atom);
        if (parsedAtom == null) {
            return null;
        }
        return parsedAtom.uuid;
    }

    public static int parseVersion(byte[] atom) {
        PsshAtom parsedAtom = parsePsshAtom(atom);
        if (parsedAtom == null) {
            return -1;
        }
        return parsedAtom.version;
    }

    public static byte[] parseSchemeSpecificData(byte[] atom, UUID uuid) {
        PsshAtom parsedAtom = parsePsshAtom(atom);
        if (parsedAtom == null) {
            return null;
        }
        if (uuid == null || uuid.equals(parsedAtom.uuid)) {
            return parsedAtom.schemeData;
        }
        Log.w(TAG, "UUID mismatch. Expected: " + uuid + ", got: " + parsedAtom.uuid + ".");
        return null;
    }

    private static PsshAtom parsePsshAtom(byte[] atom) {
        ParsableByteArray atomData = new ParsableByteArray(atom);
        if (atomData.limit() < 32) {
            return null;
        }
        atomData.setPosition(0);
        if (atomData.readInt() != atomData.bytesLeft() + 4 || atomData.readInt() != Atom.TYPE_pssh) {
            return null;
        }
        int atomVersion = Atom.parseFullAtomVersion(atomData.readInt());
        if (atomVersion > 1) {
            Log.w(TAG, "Unsupported pssh version: " + atomVersion);
            return null;
        }
        UUID uuid = new UUID(atomData.readLong(), atomData.readLong());
        if (atomVersion == 1) {
            atomData.skipBytes(atomData.readUnsignedIntToInt() * 16);
        }
        int dataSize = atomData.readUnsignedIntToInt();
        if (dataSize != atomData.bytesLeft()) {
            return null;
        }
        byte[] data = new byte[dataSize];
        atomData.readBytes(data, 0, dataSize);
        return new PsshAtom(uuid, atomVersion, data);
    }

    private static class PsshAtom {
        /* access modifiers changed from: private */
        public final byte[] schemeData;
        /* access modifiers changed from: private */
        public final UUID uuid;
        /* access modifiers changed from: private */
        public final int version;

        public PsshAtom(UUID uuid2, int version2, byte[] schemeData2) {
            this.uuid = uuid2;
            this.version = version2;
            this.schemeData = schemeData2;
        }
    }
}
