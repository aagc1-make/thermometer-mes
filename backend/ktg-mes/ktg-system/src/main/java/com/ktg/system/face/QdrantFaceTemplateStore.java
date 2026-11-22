package com.ktg.system.face;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ktg.common.exception.ServiceException;
import com.ktg.system.domain.SysUserFace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stores account-addressed, encrypted face templates in a payload-only collection. */
@Component
public class QdrantFaceTemplateStore {
    private static final String[] METADATA = {"status", "algorithm", "samples", "createdAt", "updatedAt"};
    private static final String WRITE_OPTIONS = "?wait=true&ordering=strong&timeout=5";
    private static final String CIPHERTEXT_PREFIX = "gcm:v1:";

    private final ObjectMapper json = new ObjectMapper();
    private final boolean configured;
    private final String collectionUrl;
    private final String apiKey;
    private volatile boolean ready;

    public QdrantFaceTemplateStore(@Value("${mes.face.qdrant-url:}") String url,
                                   @Value("${mes.face.qdrant-key:}") String key,
                                   @Value("${mes.face.qdrant-collection:mes_face_templates}") String collection) {
        String address = setting("MES_FACE_QDRANT_URL", url);
        configured = !address.isEmpty();
        collectionUrl = collectionUrl(address, setting("MES_FACE_QDRANT_COLLECTION", collection));
        apiKey = setting("MES_FACE_QDRANT_KEY", key);
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isReady() {
        return ready;
    }

    public void markReady() {
        ready = true;
    }

    public void initialize() {
        JsonNode details = request("GET", "", null, true);
        if (details == null) {
            ObjectNode definition = json.createObjectNode();
            definition.putObject("vectors");
            try {
                request("PUT", "?timeout=5", definition, false);
            } catch (ServiceException ignored) {
                // Another instance may have created the collection first. Verify its schema below.
            }
            details = request("GET", "", null, false);
        }
        JsonNode params = details.path("config").path("params");
        if (!params.isObject() || !isEmptyConfig(params.path("vectors"))
                || !isEmptyConfig(params.path("sparse_vectors"))) {
            throw unavailable();
        }
    }

    public SysUserFace find(long userId, boolean includeDescriptor) {
        requireUserId(userId);
        ObjectNode body = json.createObjectNode();
        body.putArray("ids").add(userId);
        body.put("with_vector", false);
        ArrayNode fields = body.putArray("with_payload");
        for (String name : METADATA) {
            fields.add(name);
        }
        if (includeDescriptor) {
            fields.add("descriptor");
        }
        JsonNode points = request("POST", "/points", body, false);
        if (!points.isArray() || points.size() > 1) {
            throw unavailable();
        }
        if (points.size() == 0) {
            return null;
        }
        JsonNode point = points.get(0);
        JsonNode id = point.path("id");
        JsonNode payload = point.path("payload");
        if (!id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() != userId || !payload.isObject()) {
            throw unavailable();
        }
        SysUserFace face = new SysUserFace();
        face.setUserId(userId);
        face.setStatus(payload.path("status").asText(null));
        face.setAlgorithm(payload.path("algorithm").asText(null));
        face.setSamples(payload.path("samples").asInt());
        face.setCreateTime(readDate(payload.path("createdAt")));
        face.setUpdateTime(readDate(payload.path("updatedAt")));
        if (includeDescriptor) {
            face.setDescriptor(requireCiphertext(payload.path("descriptor").asText(null)));
        }
        return face;
    }

    public void put(SysUserFace face) {
        long userId = face.getUserId();
        requireUserId(userId);
        ObjectNode body = json.createObjectNode();
        ObjectNode point = body.putArray("points").addObject();
        point.put("id", userId);
        point.putObject("vector");
        ObjectNode payload = point.putObject("payload");
        payload.put("descriptor", requireCiphertext(face.getDescriptor()));
        payload.put("status", face.getStatus());
        payload.put("algorithm", face.getAlgorithm());
        payload.put("samples", face.getSamples());
        payload.put("createdAt", timestamp(face.getCreateTime()));
        payload.put("updatedAt", timestamp(face.getUpdateTime()));
        requireCompleted(request("PUT", "/points" + WRITE_OPTIONS, body, false));
    }

    public void delete(long userId) {
        requireUserId(userId);
        ObjectNode body = json.createObjectNode();
        body.putArray("points").add(userId);
        requireCompleted(request("POST", "/points/delete" + WRITE_OPTIONS, body, false));
    }

    private JsonNode request(String method, String path, ObjectNode body, boolean allowMissingCollection) {
        if (collectionUrl == null || apiKey.isEmpty()) {
            throw unavailable();
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(collectionUrl + path).openConnection(Proxy.NO_PROXY);
            connection.setRequestMethod(method);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(6500);
            connection.setRequestProperty("api-key", apiKey);
            connection.setRequestProperty("Accept", "application/json");
            if (body != null) {
                byte[] contents = json.writeValueAsBytes(body);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setFixedLengthStreamingMode(contents.length);
                try (OutputStream stream = connection.getOutputStream()) {
                    stream.write(contents);
                }
            }
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND && allowMissingCollection) {
                return null;
            }
            if (status < 200 || status >= 300) {
                throw unavailable();
            }
            try (InputStream stream = connection.getInputStream()) {
                JsonNode response = json.readTree(stream);
                if (response == null || !"ok".equals(response.path("status").asText())) {
                    throw unavailable();
                }
                return response.path("result");
            }
        } catch (IOException | IllegalArgumentException ignored) {
            throw unavailable();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String collectionUrl(String url, String collection) {
        if (!collection.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,99}")) {
            return null;
        }
        try {
            URI address = new URI(url);
            String host = address.getHost();
            String scheme = address.getScheme();
            boolean loopback = "127.0.0.1".equals(host) || "[::1]".equals(host) || "::1".equals(host);
            if (host == null || address.getUserInfo() != null || address.getQuery() != null || address.getFragment() != null
                    || !("https".equalsIgnoreCase(scheme) || ("http".equalsIgnoreCase(scheme) && loopback))) {
                return null;
            }
            return url.replaceAll("/+$", "") + "/collections/" + collection;
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private static String setting(String variable, String defaultValue) {
        String environment = System.getenv(variable);
        return (environment == null ? defaultValue : environment).trim();
    }

    private static boolean isEmptyConfig(JsonNode config) {
        return config.isMissingNode() || config.isNull() || (config.isObject() && config.size() == 0);
    }

    private static String requireCiphertext(String value) {
        if (value == null || !value.startsWith(CIPHERTEXT_PREFIX) || value.length() == CIPHERTEXT_PREFIX.length()) {
            throw unavailable();
        }
        return value;
    }

    private static void requireUserId(long userId) {
        if (userId <= 0) {
            throw unavailable();
        }
    }

    private static void requireCompleted(JsonNode result) {
        if (!"completed".equals(result.path("status").asText())) {
            throw unavailable();
        }
    }

    private static Date readDate(JsonNode value) {
        return value.isIntegralNumber() && value.canConvertToLong() ? new Date(value.longValue()) : null;
    }

    private static Long timestamp(Date value) {
        return value == null ? null : value.getTime();
    }

    private static ServiceException unavailable() {
        return new ServiceException("人脸模板库暂不可用，请稍后重试");
    }
}
