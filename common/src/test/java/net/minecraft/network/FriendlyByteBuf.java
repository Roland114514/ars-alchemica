package net.minecraft.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FriendlyByteBuf {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private ByteArrayInputStream in;
    private DataInputStream dataIn;

    public void writeFloat(float f) {
        try {
            new DataOutputStream(out).writeFloat(f);
        } catch (IOException ignored) {}
    }

    public void writeByte(int b) {
        out.write(b);
    }

    public float readFloat() {
        ensureRead();
        try {
            return dataIn.readFloat();
        } catch (IOException e) {
            return 0.0f;
        }
    }

    public byte readByte() {
        ensureRead();
        try {
            return dataIn.readByte();
        } catch (IOException e) {
            return 0;
        }
    }

    private void ensureRead() {
        if (dataIn == null) {
            in = new ByteArrayInputStream(out.toByteArray());
            dataIn = new DataInputStream(in);
        }
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }
}
