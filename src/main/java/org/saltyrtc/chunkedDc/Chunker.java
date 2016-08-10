/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.chunkedDc;

import org.slf4j.Logger;
import java.nio.ByteBuffer;

/**
 * A chunker instance splits up a ByteBuffer into multiple chunks.
 *
 * A header is added to each chunk:
 *
 * |C|IIII|SSSS|
 *
 * - C: Configuration bitfield (1 byte)
 * - I: Id (4 bytes)
 * - S: Serial number (4 bytes)
 *
 * The configuration bitfield looks as follows:
 *
 * |000000E|
 *        ^---- End-of-message
 */
public class Chunker {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("ChunkedDC");

    private final long id;
    private final ByteBuffer buf;
    private final int chunkSize;
    private int chunkId;

    @SuppressWarnings("FieldCanBeLocal")
    public static int HEADER_LENGTH = 9;

    /**
     * Create a Chunker instance.
     *
     * @param id An identifier for the chunk. Must be betwen 0 and 2**32-1.
     * @param buf The ByteBuffer containing the data that should be chunked.
     * @param chunkSize The chunk size *excluding* header data.
     */
    public Chunker(long id, ByteBuffer buf, int chunkSize) {
        this.id = id;
        this.buf = buf;
        this.chunkSize = chunkSize;
        this.chunkId = 0;
    }

    /**
     * Return the next chunk.
     */
    public ByteBuffer next() {
        // Allocate chunk buffer
        final int remaining = this.buf.remaining();
        final int chunkBytes = remaining < this.chunkSize ? remaining : this.chunkSize;
        final ByteBuffer chunk = ByteBuffer.allocate(chunkBytes + HEADER_LENGTH);

        // Create header
        final byte config = remaining > chunkBytes ? (byte) 1 : (byte) 0;
        final int id = UnsignedHelper.getUnsignedInt(this.id);
        final int serial = UnsignedHelper.getUnsignedInt(this.nextSerial());

        // Write to chunk buffer
        chunk.put(config);
        chunk.putInt(id);
        chunk.putInt(serial);
        for (int i = 0; i < chunkBytes; i++) {
            chunk.put(this.buf.get());
        }
        return (ByteBuffer) chunk.flip();
    }

    /**
     * Return and post-increment the id of the next block
     */
    private int nextSerial() {
        return this.chunkId++;
    }

}
