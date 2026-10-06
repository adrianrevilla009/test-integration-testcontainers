package lab;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** A stubbed "payments" dependency of the Orders service: happy path, slow, and broken. */
class WireMockStubsTest {

    static WireMockServer wm;
    static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void start() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
    }

    @AfterAll
    static void stop() {
        wm.stop();
    }

    HttpRequest req(String path, Duration timeout) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + wm.port() + path)).timeout(timeout).build();
    }

    @Test
    void happyPath() throws Exception {
        wm.stubFor(get(urlEqualTo("/pay/1")).willReturn(aResponse().withStatus(200).withBody("PAID")));
        var res = HTTP.send(req("/pay/1", Duration.ofSeconds(2)), HttpResponse.BodyHandlers.ofString());
        assertEquals("PAID", res.body());
    }

    @Test
    void slowResponseTriggersClientTimeout() {
        wm.stubFor(get(urlEqualTo("/pay/slow")).willReturn(aResponse().withFixedDelay(1500).withStatus(200)));
        assertThrows(java.net.http.HttpTimeoutException.class,
                () -> HTTP.send(req("/pay/slow", Duration.ofMillis(300)), HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void connectionResetIsSurfacedAsIoError() {
        wm.stubFor(get(urlEqualTo("/pay/broken")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThrows(IOException.class,
                () -> HTTP.send(req("/pay/broken", Duration.ofSeconds(2)), HttpResponse.BodyHandlers.ofString()));
    }
}
