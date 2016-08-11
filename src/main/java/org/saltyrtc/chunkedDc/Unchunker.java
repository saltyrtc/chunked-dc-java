/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.chunkedDc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An Unchunker instance merges multiple chunks into a single ByteBuffer.
 *
 * It keeps track of IDs, so only one Unchunker instance is necessary
 * to receive multiple messages.
 */
public class Unchunker {

    /**
     * Interface for message listeners.
     */
    public interface MessageListener {
        void onMessage(ByteBuffer message);
    }

    /**
     * Inner class to hold chunks and an "end-arrived" flag.
     */
    private static class ChunkCollector {
        private boolean endArrived = false;
        private Long messageLength = null;
        private final SortedSet<Chunk> chunks = new TreeSet<>();
        private long lastUpdate = System.nanoTime();

        public void addChunk(Chunk chunk) {
            this.chunks.add(chunk);
            this.lastUpdate = System.nanoTime();
            if (chunk.isEndOfMessage()) {
                this.endArrived = true;
                this.messageLength = chunk.getSerial() + 1;
            }
        }

        /**
         * Return whether the message is complete, meaning that all chunks of the message arrived.
         */
        public boolean isComplete() {
            return this.endArrived && this.chunks.size() == this.messageLength;
        }

        /**
         * Merge the messages.
         *
         * Note: This implementation assumes that no chunk will be larger than the first one!
         * If this is not the case, a `BufferOverflowException` may be thrown.
         *
         * TODO: Catch that problem early on!
         *
         * @return A `ByteBuffer` containing the assembled message.
         * @throws IllegalStateException if message is not yet complete.
         */
        public ByteBuffer merge() {
            // Preconditions
            if (!this.isComplete()) {
                throw new IllegalStateException("Not all messages have arrived yet.");
            }

            // Allocate buffer
            final long capacity = this.chunks.first().getData().length * this.messageLength;
            final ByteBuffer buf = ByteBuffer.allocate(
                    capacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity);

            // Add chunks to buffer
            for (Chunk chunk : this.chunks) {
                buf.put(chunk.getData());
            }

            buf.flip();
            return buf;
        }

        /**
         * Return whether last chunk is older than the specified number of miliseconds.
         */
        public boolean isOlderThan(long maxAge) {
            final long age = (System.nanoTime() - this.lastUpdate) / 1000 / 1000;
            return age > maxAge;
        }
    }

    private Map<Long, ChunkCollector> chunks = new HashMap<>();
    private MessageListener listener = null;

    /**
     * Register an onMessage listener.
     */
    public void onMessage(MessageListener listener) {
        this.listener = listener;
    }

    /**
     * Add a chunk.
     *
     * @param buf ByteBuffer containing chunk with 9 byte header.
     * @throws IllegalArgumentException if message is smaller than the header length
     */
    public synchronized void add(ByteBuffer buf) {
        final Chunk chunk = new Chunk(buf);

        // If this is the only chunk in the message, return it immediately.
        if (chunk.isEndOfMessage() && chunk.getSerial() == 0) {
            this.notifyListener(ByteBuffer.wrap(chunk.getData()));
            this.chunks.remove(chunk.getId());
            return;
        }

        // Otherwise, add chunk to chunks list
        final long id = chunk.getId();
        final ChunkCollector collector;
        if (this.chunks.containsKey(id)) {
            collector = this.chunks.get(id);
        } else {
            collector = new ChunkCollector();
            this.chunks.put(id, collector);
        }
        collector.addChunk(chunk);

        // Check if message is complete
        if (collector.isComplete()) {
            // Merge and notify listener...
            this.notifyListener(collector.merge());
            // ...then delete the chunks.
            this.chunks.remove(id);
        }
    }

    /**
     * If a listener is set, notify it about a finished message.
     */
    private void notifyListener(ByteBuffer message) {
        if (this.listener != null) {
            this.listener.onMessage(message);
        }
    }

    /**
     * Run garbage collection, remove incomplete messages that haven't been
     * updated for more than the specified number of milliseconds.
     *
     * If you want to make sure that invalid chunks don't fill up memory, call
     * this method regularly.
     *
     * @param maxAge Remove incomplete messages that haven't been updated for
     *               more than the specified number of milliseconds.
     * @return the number of removed chunks.
     */
    public synchronized int gc(long maxAge) {
        final Iterator<Map.Entry<Long, ChunkCollector>> it = this.chunks.entrySet().iterator();
        int removedItems = 0;
        while (it.hasNext()) {
            Map.Entry<Long, ChunkCollector> entry = it.next();
            if (entry.getValue().isOlderThan(maxAge)) {
                removedItems += entry.getValue().chunks.size();
                it.remove();
            }
        }
        return removedItems;
    }

}
