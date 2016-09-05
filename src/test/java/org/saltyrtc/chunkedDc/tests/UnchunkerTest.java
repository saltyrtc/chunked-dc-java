/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.chunkedDc.tests;

import org.junit.Assert;
import org.junit.Test;
import org.saltyrtc.chunkedDc.Unchunker;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UnchunkerTest {

    private static byte MORE = 0;
    private static byte END = 1;

    private static class LoggingUnchunker {
        public List<byte[]> messages = new LinkedList<>();
        public LoggingUnchunker(Unchunker unchunker) {
            unchunker.onMessage(new Unchunker.MessageListener() {
                @Override
                public void onMessage(ByteBuffer message) {
                    final byte[] data = new byte[message.remaining()];
                    message.get(data);
                    LoggingUnchunker.this.messages.add(data);
                }
            });
        }
    }

    @Test
    public void testRegularUnchunking() {
        final Unchunker unchunker = new Unchunker();
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);

        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0, 1,2,3 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,1, 4,5,6 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,2, 7,8 }));

        assertEquals(1, logger.messages.size());
        assertArrayEquals(new byte[] { 1,2,3,4,5,6,7,8 }, logger.messages.get(0));
    }

    @Test
    public void testOutOfOrder() {
        final Unchunker unchunker = new Unchunker();
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);

        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,1, 3,4 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0, 1,2 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,3, 7,8 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,2, 5,6 }));

        assertEquals(1, logger.messages.size());
        assertArrayEquals(new byte[] { 1,2,3,4,5,6,7,8 }, logger.messages.get(0));
    }

    /**
     * Test unchunking of a message consisting of a single chunk.
     */
    @Test
    public void testSingleChunkMessage() {
        final Unchunker unchunker = new Unchunker();
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);

        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,0, 7,7,7 }));

        assertEquals(1, logger.messages.size());
        assertArrayEquals(new byte[] { 7,7,7 }, logger.messages.get(0));
    }

    /**
     * If a message is missing, no listener is notified.
     */
    @Test
    public void testMissingMessages() {
        final Unchunker unchunker = new Unchunker();
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);

        // End chunk with serial 1, no chunk with serial 0
        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,1, 7,7,7 }));

        assertEquals(0, logger.messages.size());
    }

    /**
     * Add invalid messages.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidChunk() {
        final Unchunker unchunker = new Unchunker();
        unchunker.add(ByteBuffer.wrap(new byte[] { 1, 2, 3 }));
    }

    /**
     * Add a single empty chunk. This should work.
     */
    @Test
    public void testEmptySingleChunk() {
        final Unchunker unchunker = new Unchunker();
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);

        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,0 }));

        assertEquals(1, logger.messages.size());
        assertArrayEquals(new byte[] { }, logger.messages.get(0));
    }

    /**
     * Add a first empty chunk. This will throw an exception.
     */
    @Test
    public void testFirstSingleChunk() {
        final Unchunker unchunker = new Unchunker();
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);

        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,1, 1,2 }));
        try {
            unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,2, 3 }));
            Assert.fail("No BufferOverflowException thrown");
        } catch (BufferOverflowException e) {
            // expected
        }

        assertEquals(0, logger.messages.size());
    }

    /**
     * Add two chunks with same serial. Ignore the second.
     */
    @Test
    public void testDuplicateSerial() {
        final Unchunker unchunker = new Unchunker();
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);

        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0, 1,2 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0, 3,4 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { END,  0,0,0,0, 0,0,0,1, 5,6 }));

        assertEquals(1, logger.messages.size());
        assertArrayEquals(new byte[] { 1, 2, 5, 6 }, logger.messages.get(0));
    }

    /**
     * Ignore chunks with same serial even if they're end chunks.
     */
    @Test
    public void testDuplicateSerialSingleMsg() {
        final Unchunker unchunker = new Unchunker();
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);

        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0, 1,2 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { END,  0,0,0,0, 0,0,0,0, 3,4 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { END,  0,0,0,0, 0,0,0,1, 5,6 }));

        assertEquals(1, logger.messages.size());
        assertArrayEquals(new byte[] { 1, 2, 5, 6 }, logger.messages.get(0));
    }

    /**
     * If there is no listener registered, nothing should break.
     */
    @Test
    public void testNoListenerSingleMsg() {
        final Unchunker unchunker = new Unchunker();
        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,0, 7,7,7 }));
    }

    /**
     * If a listener is added after receiving some messages,
     * it should still be notified when a message is complete.
     */
    @Test
    public void testLateListener() {
        final Unchunker unchunker = new Unchunker();

        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0, 1,2,3 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,1, 4,5,6 }));
        // Add listener only after two chunks have arrived
        final LoggingUnchunker logger = new LoggingUnchunker(unchunker);
        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,2, 7,8 }));

        assertEquals(1, logger.messages.size());
        assertArrayEquals(new byte[] { 1,2,3,4,5,6,7,8 }, logger.messages.get(0));
    }

    @Test
    public void testGarbageCollection() throws InterruptedException {
        final Unchunker unchunker = new Unchunker();
        assertEquals(0, unchunker.gc(1000));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0, 1,2,3 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,1, 4,5,6 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,1, 0,0,0,0, 1,2,3 }));
        Thread.sleep(20);
        assertEquals(0, unchunker.gc(1000));
        assertEquals(3, unchunker.gc(10));
        assertEquals(0, unchunker.gc(10));
    }

}
