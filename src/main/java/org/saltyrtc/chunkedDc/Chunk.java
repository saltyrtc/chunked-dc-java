/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.chunkedDc;

import java.nio.ByteBuffer;

/**
 * A chunk. Must be initialized with a ByteBuffer.
 */
public class Chunk implements Comparable<Chunk> {

    private boolean endOfMessage;
    private long id;
    private long serial;
    private byte[] data;

    /**
     * Consume and parse the byte buffer.
     * @param bytes Raw chunk data.
     */
    public Chunk(ByteBuffer bytes) {
        if (bytes.remaining() < Common.HEADER_LENGTH) {
            throw new IllegalArgumentException("Invalid chunk: Too short");
        }

        // Read header
        final byte options = bytes.get();
        this.endOfMessage = (options & 0x01) == 1;
        this.id = UnsignedHelper.readUnsignedInt(bytes.getInt());
        this.serial = UnsignedHelper.readUnsignedInt(bytes.getInt());

        // Read data
        this.data = new byte[bytes.remaining()];
        bytes.get(this.data);
    }

    public boolean isEndOfMessage() {
        return endOfMessage;
    }

    public long getId() {
        return id;
    }

    public long getSerial() {
        return serial;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public int compareTo(Chunk other) {
        if (this.id == other.id) {
            return Long.compare(this.serial, other.serial);
        } else {
            return Long.compare(this.id, other.id);
        }
    }
}
