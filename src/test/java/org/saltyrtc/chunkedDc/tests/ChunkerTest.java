/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.chunkedDc.tests;

import org.junit.Test;
import org.saltyrtc.chunkedDc.Chunker;
import org.saltyrtc.chunkedDc.Common;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ChunkerTest {

    private static byte MORE = 0;
    private static byte END = 1;

    private static byte ID = 42;

    /**
     * Test chunking multiples of the chunk size.
     */
    @Test
    public void testChunkingMultiples() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5, 6});
        final Chunker chunker = new Chunker(ID, buf, Common.HEADER_LENGTH + 2);
        assertTrue(chunker.hasNext());
        ByteBuffer firstBuf = chunker.next();
        assertEquals(11, firstBuf.remaining());
        assertArrayEquals(
            new byte[] { MORE, /*Id*/0,0,0,ID, /*Serial*/0,0,0,0, /*Data*/1,2 },
            firstBuf.array()
        );
        assertTrue(chunker.hasNext());
        assertArrayEquals(
                new byte[] { MORE, /*Id*/0,0,0,ID, /*Serial*/0,0,0,1, /*Data*/3,4 },
                chunker.next().array()
        );
        assertTrue(chunker.hasNext());
        assertArrayEquals(
                new byte[] { END, /* Id */ 0,0,0,ID, /*Serial*/0,0,0,2, /*Data*/5,6 },
                chunker.next().array()
        );
        assertFalse(chunker.hasNext());
        assertNull(chunker.next());
    }

    /**
     * Test chunking non-multiples of the chunk size.
     */
    @Test
    public void testChunkingNonMultiples() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5, 6});
        final Chunker chunker = new Chunker(ID, buf, Common.HEADER_LENGTH + 4);
        assertTrue(chunker.hasNext());
        assertArrayEquals(
                new byte[] { MORE, /*Id*/0,0,0,ID, /*Serial*/0,0,0,0, /*Data*/1,2,3,4 },
                chunker.next().array()
        );
        assertTrue(chunker.hasNext());
        assertArrayEquals(
                new byte[] { END, /*Id*/0,0,0,ID, /*Serial*/0,0,0,1, /*Data*/5,6 },
                chunker.next().array()
        );
        assertFalse(chunker.hasNext());
        assertNull(chunker.next());
    }

    /**
     * Test chunking data smaller than chunk size.
     */
    @Test
    public void testChunkingSmallData() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2 });
        final Chunker chunker = new Chunker(ID, buf, Common.HEADER_LENGTH + 99);
        assertTrue(chunker.hasNext());
        assertArrayEquals(
                new byte[] { END, /*Id*/0,0,0,ID, /*Serial*/0,0,0,0, /*Data*/1,2 },
                chunker.next().array()
        );
        assertFalse(chunker.hasNext());
        assertNull(chunker.next());
    }

    /**
     * Allow chunk size of 1.
     */
    @Test
    public void testChunkSize1() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2 });
        final Chunker chunker = new Chunker(ID, buf, Common.HEADER_LENGTH + 1);
        assertArrayEquals(
                new byte[] { MORE, /*Id*/0,0,0,ID, /*Serial*/0,0,0,0, /*Data*/1 },
                chunker.next().array()
        );
        assertArrayEquals(
                new byte[] { END, /*Id*/0,0,0,ID, /*Serial*/0,0,0,1, /*Data*/2 },
                chunker.next().array()
        );
    }

    /**
     * Does not allow chunk size of 0.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChunkSize0() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2 });
        new Chunker(ID, buf, 0);
    }

    /**
     * Does not allow chunk size of 9.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChunkSize9() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2 });
        new Chunker(ID, buf, Common.HEADER_LENGTH);
    }

    /**
     * Does not allow negative chunk size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChunkSizeNegative() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2 });
        new Chunker(ID, buf, -2);
    }

    /**
     * Does not allow chunking of empty arrays.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChunkEmpty() {
        final ByteBuffer buf = ByteBuffer.allocate(0);
        new Chunker(ID, buf, Common.HEADER_LENGTH + 2);
    }

    /**
     * Does not allow an out-of-bounds message id.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeId() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2 });
        new Chunker(-1, buf, Common.HEADER_LENGTH + 2);
    }

}
