/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.chunkedDc.tests;

import org.junit.Test;
import org.saltyrtc.chunkedDc.Chunk;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChunkTest {

    @Test
    public void testValidChunk() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] {
                // Options
                0,
                // Id (0xff, 0xff, 0xff, 0xfe)
                -1, -1, -1, -2,
                // Serial
                0, 0, 0, 1,
                // Data
                1, 2, 3, 4, 5, 6
        });
        final Chunk chunk = new Chunk(buf);
        assertFalse(buf.hasRemaining());

        assertFalse(chunk.isEndOfMessage());
        assertEquals(4294967294L, chunk.getId());
        assertEquals(1, chunk.getSerial());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, chunk.getData());
    }

    @Test
    public void testChunkNoData() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] {
                // Options
                1,
                // Id
                0, 0, 2, 0,
                // Serial
                0, 0, 0, 1,
        });
        final Chunk chunk = new Chunk(buf);
        assertTrue(chunk.isEndOfMessage());
        assertEquals(512, chunk.getId());
        assertEquals(1, chunk.getSerial());
        assertArrayEquals(new byte[] { }, chunk.getData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidChunk() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2, 3 });
        new Chunk(buf);
    }

    @Test
    public void testSorting() {
        final ByteBuffer buf1 = ByteBuffer.wrap(new byte[] {
                0, /**/ 0, 0, 0, 1, /**/ 0, 0, 0, 2
        });
        final ByteBuffer buf2 = ByteBuffer.wrap(new byte[] {
                0, /**/ 0, 0, 0, 1, /**/ 0, 0, 0, 1
        });
        final ByteBuffer buf3 = ByteBuffer.wrap(new byte[] {
                0, /**/ 0, 0, 0, 2, /**/ 0, 0, 0, 1, 1
        });
        final ByteBuffer buf4 = ByteBuffer.wrap(new byte[] {
                0, /**/ 0, 0, 0, 2, /**/ 0, 0, 0, 1, 2
        });
        final Chunk chunk1 = new Chunk(buf1);
        final Chunk chunk2 = new Chunk(buf2);
        final Chunk chunk3 = new Chunk(buf3);
        final Chunk chunk4 = new Chunk(buf4);

        assertEquals(1, chunk1.compareTo(chunk2));
        assertEquals(-1, chunk1.compareTo(chunk3));
        assertEquals(-1, chunk1.compareTo(chunk4));

        assertEquals(-1, chunk2.compareTo(chunk3));
        assertEquals(-1, chunk2.compareTo(chunk4));

        assertEquals(1, chunk3.compareTo(chunk1));
        assertEquals(0, chunk3.compareTo(chunk4));
    }

}
