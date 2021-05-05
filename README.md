# Binary Chunking for WebRTC DataChannels

[![CI](https://github.com/saltyrtc/chunked-dc-java/workflows/CI/badge.svg)](#)
[![Coverage](https://img.shields.io/coveralls/saltyrtc/chunked-dc-java/master.svg?maxAge=2592000)](https://coveralls.io/github/saltyrtc/chunked-dc-java)
[![Codacy](https://img.shields.io/codacy/grade/a9eed1d7dd24410fa522577e750c58a1/master.svg)](https://www.codacy.com/app/saltyrtc/chunked-dc-java/dashboard)
[![Java Version](https://img.shields.io/badge/java-8%2B-orange.svg)](https://github.com/saltyrtc/chunked-dc-java)
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

The package is available on Maven Central.

Gradle:

```groovy
compile 'org.saltyrtc:chunked-dc:1.0.1'
```

Maven:

```xml
<dependency>
  <groupId>org.saltyrtc</groupId>
  <artifactId>chunked-dc</artifactId>
  <version>1.0.1</version>
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

## Unit Testing

To test from the command line:

    ./gradlew test

To run tests and generate coverage reports:

    ./gradlew test jacocoTestReport

You'll find the reports at `build/reports/jacoco/test/html/index.html`.

## Manual testing

Create a local publication (usually at `$HOME/.m2/repository/`):

    ./gradlew publishToMavenLocal

Include it in your project like this:

    repositories {
        ...
        mavenLocal()
    }

## Signing

Releases are signed with the following PGP ED25519 public key:

    sec   ed25519 2021-05-05 [SC] [expires: 2025-05-04]
          27655CDD319B686A73661526DCD186BEB204C8FD
    uid           SaltyRTC (Release signing key)

## License

    Copyright (c) 2016-2021 Threema GmbH / SaltyRTC Contributors

    Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
    or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
    copied, modified, or distributed except according to those terms.
