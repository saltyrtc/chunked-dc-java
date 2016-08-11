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
import org.saltyrtc.chunkedDc.Unchunker;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UnchunkerTest {

    private static byte MORE = 0;
    private static byte END = 1;

    @Test
    public void testRegularUnchunking() {
        final Unchunker unchunker = new Unchunker();

        final List<byte[]> messages = new LinkedList<>();
        unchunker.onMessage(new Unchunker.MessageListener() {
            @Override
            public void onMessage(ByteBuffer message) {
                final byte[] data = new byte[message.remaining()];
                message.get(data);
                messages.add(data);
            }
        });

        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,0, 1,2,3 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { MORE, 0,0,0,0, 0,0,0,1, 4,5,6 }));
        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,2, 7,8 }));

        assertEquals(1, messages.size());
        assertArrayEquals(new byte[] { 1,2,3,4,5,6,7,8 }, messages.get(0));
    }

    /**
     * Test unchunking of a message consisting of a single chunk.
     */
    @Test
    public void testSingleChunkMessage() {
        final Unchunker unchunker = new Unchunker();

        final List<byte[]> messages = new LinkedList<>();
        unchunker.onMessage(new Unchunker.MessageListener() {
            @Override
            public void onMessage(ByteBuffer message) {
                final byte[] data = new byte[message.remaining()];
                message.get(data);
                messages.add(data);
            }
        });

        unchunker.add(ByteBuffer.wrap(new byte[] { END, 0,0,0,0, 0,0,0,0, 7,7,7 }));

        assertEquals(1, messages.size());
        assertArrayEquals(new byte[] { 7,7,7 }, messages.get(0));
    }

}