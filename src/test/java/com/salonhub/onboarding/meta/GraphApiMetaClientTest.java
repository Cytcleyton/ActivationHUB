package com.salonhub.onboarding.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salonhub.onboarding.config.MetaProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Runs the Graph API client against a small local server pretending to be Meta, so the requests
// we send and how we read the replies are checked without the internet.
class GraphApiMetaClientTest {

    private record Reply(int status, String body) {
    }

    private record Received(String path, String query, String auth, String body) {
    }

    private static final String MISMATCH = "{\"error\":{\"message\":\"Error validating verification code. "
            + "Please make sure your redirect_uri is identical to the one you used in the OAuth dialog request\","
            + "\"code\":100,\"error_subcode\":36008}}";

    private HttpServer server;
    // Replies are used in order; the last one repeats.
    private final Map<String, List<Reply>> replies = new HashMap<>();
    private final List<Received> received = new ArrayList<>();
    private GraphApiMetaClient client;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String key = exchange.getRequestMethod() + " " + path;
            synchronized (received) {
                received.add(new Received(path, exchange.getRequestURI().getRawQuery(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            }
            Reply reply;
            synchronized (replies) {
                List<Reply> queue = replies.get(key);
                reply = queue == null
                        ? new Reply(404, "{\"error\":{\"message\":\"no reply for " + key + "\",\"code\":803}}")
                        : (queue.size() > 1 ? queue.remove(0) : queue.get(0));
            }
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();

        MetaProperties props = new MetaProperties();
        props.setMode(MetaProperties.Mode.LIVE);
        props.setAppId("APP123");
        props.setAppSecret("SECRET456");
        props.setGraphBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        client = new GraphApiMetaClient(props, HttpClient.newHttpClient(), new ObjectMapper());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void reply(String methodAndPath, int status, String body) {
        replies.computeIfAbsent(methodAndPath, k -> new ArrayList<>()).add(new Reply(status, body));
    }

    private List<Received> calls(String path) {
        return received.stream().filter(r -> r.path().equals(path)).toList();
    }

    @Test
    void exchangesCodeForToken() {
        reply("GET /v26.0/oauth/access_token", 200, "{\"access_token\":\"BIZTOKEN\",\"token_type\":\"bearer\"}");

        assertEquals("BIZTOKEN", client.exchangeCodeForToken("THE-CODE", List.of()));

        String query = calls("/v26.0/oauth/access_token").get(0).query();
        assertTrue(query.contains("client_id=APP123"));
        assertTrue(query.contains("client_secret=SECRET456"));
        assertTrue(query.contains("code=THE-CODE"));
        assertFalse(query.contains("redirect_uri"));
    }

    @Test
    void acceptsABareTokenReply() {
        reply("GET /v26.0/oauth/access_token", 200, "BIZTOKEN");
        assertEquals("BIZTOKEN", client.exchangeCodeForToken("THE-CODE", List.of()));
    }

    @Test
    void triesEachRedirectAddressUntilOneMatches() {
        reply("GET /v26.0/oauth/access_token", 400, MISMATCH);
        reply("GET /v26.0/oauth/access_token", 400, MISMATCH);
        reply("GET /v26.0/oauth/access_token", 200, "{\"access_token\":\"BIZTOKEN\"}");

        String token = client.exchangeCodeForToken("THE-CODE", List.of("https://salon.example/portal.html?id=9"));

        assertEquals("BIZTOKEN", token);
        List<Received> attempts = calls("/v26.0/oauth/access_token");
        assertEquals(3, attempts.size());
        assertFalse(attempts.get(0).query().contains("redirect_uri"));
        assertTrue(attempts.get(1).query().contains("redirect_uri=&"));
        assertTrue(attempts.get(2).query().contains(
                "redirect_uri=https%3A%2F%2Fsalon.example%2Fportal.html%3Fid%3D9"));
    }

    @Test
    void stopsAtAnErrorThatIsntAMismatch() {
        reply("GET /v26.0/oauth/access_token", 400,
                "{\"error\":{\"message\":\"This authorization code has expired.\",\"code\":100,\"error_subcode\":36007}}");

        MetaApiException e = assertThrows(MetaApiException.class,
                () -> client.exchangeCodeForToken("OLD", List.of("https://salon.example/")));

        assertEquals(1, calls("/v26.0/oauth/access_token").size());
        assertEquals("(#100/36007) This authorization code has expired.", e.summary());
    }

    @Test
    void missingAppSecretIsReportedClearly() {
        MetaProperties props = new MetaProperties();
        props.setAppId("APP123");
        GraphApiMetaClient unconfigured = new GraphApiMetaClient(props, HttpClient.newHttpClient(), new ObjectMapper());

        MetaApiException e = assertThrows(MetaApiException.class,
                () -> unconfigured.exchangeCodeForToken("CODE", List.of()));
        assertEquals("META_APP_SECRET isn't set in .env", e.getMessage());
    }

    @Test
    void findsTheWhatsAppAccountFromTheToken() {
        reply("GET /v26.0/debug_token", 200, """
                {"data": {"granular_scopes": [
                  {"scope": "whatsapp_business_management", "target_ids": ["WABA1"]},
                  {"scope": "business_management", "target_ids": ["BIZ1"]}]}}""");
        reply("GET /v26.0/WABA1/phone_numbers", 200, "{\"data\": [{\"id\": \"PHONE1\"}]}");

        assertEquals(new MetaAssets("BIZ1", "WABA1", "PHONE1"), client.discoverAssets("BIZTOKEN"));
        assertTrue(calls("/v26.0/debug_token").get(0).query().contains("access_token=APP123%7CSECRET456"));
        assertEquals("Bearer BIZTOKEN", calls("/v26.0/WABA1/phone_numbers").get(0).auth());
    }

    @Test
    void subscribesAndRegistersWithTheSalonsToken() {
        reply("POST /v26.0/WABA1/subscribed_apps", 200, "{\"success\": true}");
        reply("POST /v26.0/PHONE1/register", 200, "{\"success\": true}");

        client.subscribeApp("WABA1", "BIZTOKEN");
        client.registerPhoneNumber("PHONE1", "246810", "BIZTOKEN");

        assertEquals("Bearer BIZTOKEN", calls("/v26.0/WABA1/subscribed_apps").get(0).auth());
        Received register = calls("/v26.0/PHONE1/register").get(0);
        assertEquals("Bearer BIZTOKEN", register.auth());
        assertTrue(register.body().contains("\"messaging_product\":\"whatsapp\""));
        assertTrue(register.body().contains("\"pin\":\"246810\""));
        // The token never goes in the address, where it could end up in logs.
        received.forEach(r -> assertTrue(r.query() == null || !r.query().contains("BIZTOKEN")));
    }

    @Test
    void readsStatusesFromTheNumberAndTheBusiness() {
        reply("GET /v26.0/PHONE1", 200, "{\"status\": \"CONNECTED\", \"id\": \"PHONE1\"}");
        reply("GET /v26.0/BIZ1", 200, "{\"verification_status\": \"pending\", \"id\": \"BIZ1\"}");

        MetaStatus status = client.fetchStatus(new MetaAssets("BIZ1", "WABA1", "PHONE1"), "BIZTOKEN");

        assertEquals(new MetaStatus("CONNECTED", "pending"), status);
        assertTrue(calls("/v26.0/WABA1").isEmpty());
    }

    @Test
    void fallsBackToTheWhatsAppAccountWhenTheBusinessCantBeRead() {
        reply("GET /v26.0/PHONE1", 200, "{\"status\": \"PENDING\"}");
        reply("GET /v26.0/BIZ1", 403,
                "{\"error\":{\"message\":\"Requires business_management permission\",\"code\":200}}");
        reply("GET /v26.0/WABA1", 200, "{\"business_verification_status\": \"not_verified\"}");

        MetaStatus status = client.fetchStatus(new MetaAssets("BIZ1", "WABA1", "PHONE1"), "BIZTOKEN");

        assertEquals(new MetaStatus("PENDING", "not_verified"), status);
    }

    @Test
    void verificationStaysUnknownWhenTheAccountDoesntHaveTheField() {
        reply("GET /v26.0/PHONE1", 200, "{\"status\": \"PENDING\"}");
        reply("GET /v26.0/WABA1", 400, "{\"error\":{\"message\":\"Tried accessing nonexisting field\",\"code\":100}}");

        MetaStatus status = client.fetchStatus(new MetaAssets(null, "WABA1", "PHONE1"), "BIZTOKEN");

        assertEquals("PENDING", status.phoneStatus());
        assertNull(status.businessVerificationStatus());
    }

    @Test
    void siteRootOfAPage() {
        assertEquals("https://salon.example/", GraphApiMetaClient.siteRoot("https://salon.example/portal.html?id=9"));
        assertNull(GraphApiMetaClient.siteRoot("not a url"));
    }
}
