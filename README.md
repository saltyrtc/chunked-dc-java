# Binary Chunking for WebRTC DataChannels

[![Travis](https://img.shields.io/travis/saltyrtc/chunked-dc-java/master.svg)](https://travis-ci.org/saltyrtc/chunked-dc-java)
[![Coverage](https://img.shields.io/coveralls/saltyrtc/chunked-dc-java/master.svg?maxAge=2592000)](https://coveralls.io/github/saltyrtc/chunked-dc-java)
[![Codacy](https://img.shields.io/codacy/grade/a9eed1d7dd24410fa522577e750c58a1/master.svg)](https://www.codacy.com/app/saltyrtc/chunked-dc-java/dashboard)
[![Bintray](https://img.shields.io/bintray/v/saltyrtc/maven/chunked-dc.svg?maxAge=2592000)](https://bintray.com/saltyrtc/maven/chunked-dc)
[![Java Version](https://img.shields.io/badge/java-7%2B-orange.svg)](https://github.com/saltyrtc/chunked-dc-java)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20Apache%202.0-blue.svg)](https://github.com/saltyrtc/chunked-dc-java)

This library allows you to split up large binary messages into multiple chunks
of a certain size.

When converting data to chunks, a 9 byte header is prepended to each chunk.
This allows you to send the chunks to the receiver in any order.

While the library was written for use with WebRTC DataChannels, it can also be
used outside of that scope.

The full specification for the chunking format can be found
[here](https://github.com/saltyrtc/saltyrtc-meta/blob/master/Chunking.md).

## Installing

The package is available [on Bintray](https://bintray.com/saltyrtc/maven/chunked-dc)
and on jcenter.

Gradle:

```groovy
compile 'org.saltyrtc.chunked-dc:chunked-dc:0.2.0'
```

Maven:

```xml
<dependency>
  <groupId>org.saltyrtc.chunked-dc</groupId>
  <artifactId>chunked-dc</artifactId>
  <version>0.2.0</version>
  <type>pom</type>
</dependency>
```

## Usage

### Chunking

For each message that you want to split into chunks, pass it to a `Chunker`.

```java
long messageId = 1337;
ByteBuffer message = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
int chunkSize = 12;
Chunker chunker = new Chunker(messageId, message, chunkSize);
```

You can then process all chunks:

```java
while (chunker.hasNext()) {
    ByteBuffer chunk = chunker.next();
    // Send chunk to peer
}
```

The example above will return 3 chunks: `[1, 2, 3], [4, 5, 6], [7, 8]`.

### Unchunking

This library works both if chunks are sent in ordered or unordered manner.
Because ordering is not guaranteed, the `Unchunker` instance accepts chunks
and stores them in an internal data structure. As soon as all chunks of a
message have arrived, a listener will be notified. Repeated chunks with the
same serial will be ignored.

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

### Cleanup

Because the `Unchunker` instance needs to keep track of arrived chunks, it's
possible that incomplete messages add up and use a lot of memory without ever
being freed.

To avoid this, simply call the `Unchunker.gc(long maxAge)` method regularly.
It will remove all incomplete messages that haven't been updated for more than
`maxAge` milliseconds.

## Thread Safety

All classes exposed by this library should be thread safe.

## Format

The chunking format is described
[in the specification](https://github.com/saltyrtc/saltyrtc-meta/blob/master/Chunking.md).

## Testing

To test from the command line:

    ./gradlew test

To run tests and generate coverage reports:

    ./gradlew test jacocoTestReport

You'll find the reports at `build/reports/jacoco/test/html/index.html`.


## Hashes

Hash verification can be enabled in a project using tools like [Gradle
Witness](https://github.com/WhisperSystems/gradle-witness). Here are the SHA256
sums for the released versions:

- 0.2.0: `e32ae9c0df9b2b3e5b3d2c8699c966b1d0fad89f76d2cab4be60268567415631`
- 0.1.5: `6ff2ab686c94de4d84c221c3094e755a2f40e991cf1197f10b8575bdda370720`
- 0.1.4: `6fa77f2e80f63316ab12a3e64ffd1eeab12100cb4ea690b46d81de62d33e64c4`

## License

    Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors

    Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
    or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
    copied, modified, or distributed except according to those terms.
