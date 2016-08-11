# Binary Chunking for WebRTC DataChannels

[![Travis](https://img.shields.io/travis/saltyrtc/chunked-dc-java/master.svg)](https://travis-ci.org/saltyrtc/chunked-dc-java)
[![Java Version](https://img.shields.io/badge/java-7%2B-orange.svg)](https://github.com/saltyrtc/chunked-dc-java)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20Apache%202.0-blue.svg)](https://github.com/saltyrtc/chunked-dc-java)

This library allows you to split up large binary messages into multiple chunks
of a certain size.

When converting data to chunks, a 9 byte header is prepended to each chunk.
This allows you to send the chunks to the receiver in any order.

While the library was written for use with WebRTC DataChannels, it can also be
used outside of that scope.

## Format

A chunker instance splits up a ByteBuffer into multiple chunks.

A header is added to each chunk:

    |C|IIII|SSSS|

    - C: Configuration bitfield (1 byte)
    - I: Id (4 bytes)
    - S: Serial number (4 bytes)

The configuration bitfield looks as follows:

    |000000E|
           ^---- End-of-message

The Id can be any number, but it's recommended to start at 0 and
increment the counter for each message.

The Serial must start at 0 and be incremented after every message.

No chunk may contain more bytes than the first one.

## Usage

### Chunking

For each message that you want to split into chunks, pass it to a `Chunker`.

```java
long messageId = 1337;
ByteBuffer message = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
int chunkSize = 2;
Chunker chunker = new Chunker(messageId, buf, chunkSize);
```

You can then process all chunks:

```java
while (chunker.hasNext()) {
    ByteBuffer chunk = chunker.next();
    // Send chunk to peer
}
```

### Unchunking

This library works both if chunks are sent in ordered or unordered manner.
Because ordering is not guaranteed, the `Unchunker` instance accepts chunks
and stores them in an internal data structure. As soon as all chunks of a
message have arrived, a listener will be notified.

Create the `Unchunker` instance:

```java
Unchunker unchunker = new Unchunker();
```

Register a message listener:

```java
unchunker.onMessage(new Unchunker.MessageListener() {
    @Override
    public void onMessage(ByteBuffer message) {
        // Do something with the received message
    }
});
```

Finally, when new chunks arrive, simply add them to the `Unchunker` instance:

```java
ByteBuffer chunk = ...;
unchunker.add(chunk);
```

## Thread Safety

All classes exposed by this library should be thread safe.

## License

    Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
    
    Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
    or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
    copied, modified, or distributed except according to those terms.
