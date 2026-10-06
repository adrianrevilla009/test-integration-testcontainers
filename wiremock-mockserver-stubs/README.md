# wiremock-mockserver-stubs
Two test classes that stub an HTTP dependency, one with an in-process WireMock server and one with a MockServer container, including delays and broken connections.

## Goal
Show how to test a client against a dependency that is slow or failing. Both tools run the same three scenarios so you can compare them.

## Run it
```
cd wiremock-mockserver-stubs
mvn -q test
```
Expected: `WireMockStubsTest` and `MockServerStubsTest` run 3 tests each, 6 in total, 0 failures (about 4 s and 15 s here). Docker must be running for the MockServer class only.

## What it proves
- A stub returns a body (`PAID` from WireMock, `12` from MockServer) and the `java.net.http` client reads it.
- A stub delayed by 1500 ms (WireMock) or 2000 ms (MockServer) makes a client with a 300 or 500 ms timeout throw `HttpTimeoutException`.
- WireMock `Fault.CONNECTION_RESET_BY_PEER` and MockServer `withDropConnection(true)` both surface as `IOException`.
- `WireMockStubsTest.java` uses a dynamic port; `MockServerStubsTest.java` uses `mockserver/mockserver:5.15.0` through Testcontainers.

## Trade-offs
- WireMock runs in-process and starts fast; MockServer needs Docker but can serve non-JVM clients too.
- Stubs are never reset between tests; each test uses a distinct path to avoid clashes.
- Timeout tests depend on wall-clock timing.

## When not to use it
- When the real dependency offers a sandbox you can call safely.
- For contract testing between teams; stubs written by the consumer can drift from the provider.
