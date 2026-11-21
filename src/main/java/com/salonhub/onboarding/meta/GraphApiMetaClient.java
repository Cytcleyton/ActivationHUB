package com.salonhub.onboarding.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.salonhub.onboarding.config.MetaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Calls Meta's Graph API with Java's built-in HTTP client. Tokens go in the Authorization header
// and are never logged; only the path of each call is.
public class GraphApiMetaClient implements MetaClient {

    private static final Logger log = LoggerFactory.getLogger(GraphApiMetaClient.class);

    // Meta's subcode for "redirect_uri isn't the one the dialog used".
    static final int REDIRECT_URI_MISMATCH = 36008;
    // Meta's code for an invalid parameter, e.g. a field this account doesn't have.
    static final int INVALID_PARAMETER = 100;
    // Each attempt is a round trip to Meta, and the code expires after 30 seconds.
    static final int MAX_ATTEMPTS = 8;

    private final MetaProperties props;
    private final HttpClient http;
    private final ObjectMapper json;

    public GraphApiMetaClient(MetaProperties props, HttpClient http, ObjectMapper json) {
        this.props = props;
        this.http = http;
        this.json = json;
    }

    @Override
    public String mode() {
        return "live";
    }

    // Meta ties the code to the redirect address its dialog used, and the exchange must send the
    // same one. With the JavaScript SDK that address isn't fixed, so try no address, an empty one,
    // then each address the browser saw. Only a mismatch moves on to the next; any other error stops.
    @Override
    public String exchangeCodeForToken(String code, List<String> redirectUris) {
        requireSet(props.getAppId(), "META_APP_ID");
        requireSet(props.getAppSecret(), "META_APP_SECRET");
        MetaApiException last = null;
        for (String redirectUri : exchangeAttempts(redirectUris)) {
            try {
                String token = exchangeOnce(code, redirectUri);
                log.info("Token exchange worked with redirect_uri {}", describe(redirectUri));
                return token;
            } catch (MetaApiException e) {
                if (!Integer.valueOf(REDIRECT_URI_MISMATCH).equals(e.getMetaSubcode())) {
                    throw e;
                }
                log.info("Token exchange: redirect_uri {} doesn't match", describe(redirectUri));
                last = e;
            }
        }
        throw last;
    }

    // null means "don't send redirect_uri at all", which Meta treats differently from an empty one.
    static List<String> exchangeAttempts(List<String> redirectUris) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(null);
        ordered.add("");
        if (redirectUris != null) {
            for (String uri : redirectUris) {
                if (uri == null || !(uri.startsWith("https://") || uri.startsWith("http://"))) {
                    continue;
                }
                ordered.add(uri.trim());
                ordered.add(withoutFragment(uri.trim()));
            }
        }
        List<String> list = new ArrayList<>(ordered);
        return list.size() > MAX_ATTEMPTS ? list.subList(0, MAX_ATTEMPTS) : list;
    }

    private String exchangeOnce(String code, String redirectUri) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("client_id", props.getAppId());
        query.put("client_secret", props.getAppSecret());
        if (redirectUri != null) {
            query.put("redirect_uri", redirectUri);
        }
        query.put("code", code);
        JsonNode body = send("GET", "oauth/access_token", query, null, null);
        // Normally {"access_token": "...", "token_type": "bearer"}, but some of Meta's docs show a bare string.
        String token = body.isTextual() ? body.asText() : body.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            throw new MetaApiException("Meta's reply didn't contain an access token", null);
        }
        return token;
    }

    // debug_token lists which WhatsApp account and business the token was granted.
    @Override
    public MetaAssets discoverAssets(String token) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("input_token", token);
        query.put("access_token", props.getAppId() + "|" + props.getAppSecret());
        JsonNode data = send("GET", "debug_token", query, null, null).path("data");

        String wabaId = null;
        String businessId = null;
        for (JsonNode scope : data.path("granular_scopes")) {
            JsonNode targets = scope.path("target_ids");
            if (!targets.isArray() || targets.isEmpty()) {
                continue;
            }
            String name = scope.path("scope").asText();
            if ("whatsapp_business_management".equals(name) && wabaId == null) {
                wabaId = targets.get(0).asText();
            } else if ("business_management".equals(name) && businessId == null) {
                businessId = targets.get(0).asText();
            }
        }

        String phoneNumberId = null;
        if (wabaId != null) {
            JsonNode phones = send("GET", wabaId + "/phone_numbers", Map.of("fields", "id"), null, token).path("data");
            if (phones.isArray() && !phones.isEmpty()) {
                phoneNumberId = phones.get(0).path("id").asText(null);
            }
        }
        return new MetaAssets(businessId, wabaId, phoneNumberId);
    }

    @Override
    public void subscribeApp(String wabaId, String token) {
        send("POST", wabaId + "/subscribed_apps", Map.of(), json.createObjectNode(), token);
    }

    @Override
    public void registerPhoneNumber(String phoneNumberId, String pin, String token) {
        ObjectNode body = json.createObjectNode();
        body.put("messaging_product", "whatsapp");
        body.put("pin", pin);
        send("POST", phoneNumberId + "/register", Map.of(), body, token);
    }

    // The phone's status comes from the number. Verification comes from the business portfolio
    // when we're allowed to read it, otherwise from the copy Meta keeps on the WhatsApp account.
    @Override
    public MetaStatus fetchStatus(MetaAssets assets, String token) {
        String phoneStatus = null;
        if (assets.phoneNumberId() != null) {
            phoneStatus = text(send("GET", assets.phoneNumberId(), Map.of("fields", "status"), null, token), "status");
        }

        String verification = null;
        if (assets.businessId() != null) {
            try {
                verification = text(send("GET", assets.businessId(), Map.of("fields", "verification_status"),
                        null, token), "verification_status");
            } catch (MetaApiException e) {
                log.info("Couldn't read business {}, using the WhatsApp account's copy: {}",
                        assets.businessId(), e.summary());
            }
        }
        if (verification == null && assets.wabaId() != null) {
            try {
                verification = text(send("GET", assets.wabaId(), Map.of("fields", "business_verification_status"),
                        null, token), "business_verification_status");
            } catch (MetaApiException e) {
                if (!Integer.valueOf(INVALID_PARAMETER).equals(e.getMetaCode())) {
                    throw e;
                }
                // Some accounts don't have the field; the status stays unknown.
            }
        }
        return new MetaStatus(phoneStatus, verification);
    }

    JsonNode send(String method, String path, Map<String, String> query, JsonNode body, String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(path, query)))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .header("Accept", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if ("POST".equals(method)) {
            request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body.toString()));
        } else {
            request.GET();
        }

        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new MetaApiException("Couldn't reach Meta: " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MetaApiException("Interrupted while calling Meta", null);
        }

        JsonNode parsed = parse(response.body());
        int status = response.statusCode();
        if (status >= 200 && status < 300 && !parsed.has("error")) {
            return parsed;
        }
        JsonNode error = parsed.path("error");
        String message = error.path("message").asText("HTTP " + status + " from Meta");
        Integer code = error.has("code") ? error.path("code").asInt() : null;
        Integer subcode = error.has("error_subcode") ? error.path("error_subcode").asInt() : null;
        log.warn("Meta {} /{} failed: HTTP {} code {} - {}", method, path, status, code, message);
        throw new MetaApiException(message, code, subcode);
    }

    String url(String path, Map<String, String> query) {
        String url = props.getGraphBaseUrl().replaceAll("/+$", "") + "/" + props.getGraphVersion() + "/" + path;
        if (query == null || query.isEmpty()) {
            return url;
        }
        return url + "?" + query.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    // "https://host/" for "https://host/any/page?x=1"; null if it isn't a URL.
    public static String siteRoot(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() == null || uri.getHost() == null
                    ? null : uri.getScheme() + "://" + uri.getRawAuthority() + "/";
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static String withoutFragment(String url) {
        int hash = url.indexOf('#');
        return hash >= 0 ? url.substring(0, hash) : url;
    }

    private static String describe(String redirectUri) {
        return redirectUri == null ? "(none)" : redirectUri.isEmpty() ? "(empty)" : redirectUri;
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return json.createObjectNode();
        }
        try {
            return json.readTree(body);
        } catch (IOException e) {
            return TextNode.valueOf(body.trim());
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static void requireSet(String value, String setting) {
        if (value == null || value.isBlank()) {
            throw new MetaApiException(setting + " isn't set in .env", null);
        }
    }
}
