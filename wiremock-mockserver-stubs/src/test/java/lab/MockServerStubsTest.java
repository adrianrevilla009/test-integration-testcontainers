package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Delay;
import org.mockserver.model.HttpError;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Same idea as WireMock, but the stub server runs in a container and is driven through a client. */
@Testcontainers
class MockServerStubsTest {

    @Container
    static final MockServerContainer MOCK =
            new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

    static final HttpClient HTTP = HttpClient.newHttpClient();

    // One shared client: closing a MockServerClient per test tears down shared Netty state.
    static MockServerClient client;

    @BeforeAll
    static void connect() {
        client = new MockServerClient(MOCK.getHost(), MOCK.getServerPort());
    }

    @AfterAll
    static void disconnect() {
        client.close();
    }

    HttpRequest req(String path, Duration timeout) {
        return HttpRequest.newBuilder(URI.create(MOCK.getEndpoint() + path)).timeout(timeout).build();
    }

    @Test
    void stubbedResponse() throws Exception {
        client.when(request().withPath("/inventory/A-1")).respond(response().withStatusCode(200).withBody("12"));
        var res = HTTP.send(req("/inventory/A-1", Duration.ofSeconds(5)), HttpResponse.BodyHandlers.ofString());
        assertEquals("12", res.body());
    }

    @Test
    void delayedResponseTimesOut() {
        client.when(request().withPath("/inventory/slow"))
                .respond(response().withStatusCode(200).withDelay(new Delay(TimeUnit.MILLISECONDS, 2000)));
        assertThrows(HttpTimeoutException.class,
                () -> HTTP.send(req("/inventory/slow", Duration.ofMillis(500)), HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void droppedConnectionIsAnIoError() {
        client.when(request().withPath("/inventory/drop")).error(HttpError.error().withDropConnection(true));
        assertThrows(IOException.class,
                () -> HTTP.send(req("/inventory/drop", Duration.ofSeconds(5)), HttpResponse.BodyHandlers.ofString()));
    }
}
