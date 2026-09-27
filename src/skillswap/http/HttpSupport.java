package skillswap.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import skillswap.store.SessionStore;

public final class HttpSupport {
    public static final String COOKIE_NAME = "skillswap_session";

    private HttpSupport() {
    }

    public static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return splitData(body);
    }

    public static Map<String, String> splitData(String text) {
        Map<String, String> data = new HashMap<>();

        if (text == null || text.isBlank()) {
            return data;
        }

        String[] pairs = text.split("&");
        for (String pair : pairs) {
            String[] item = pair.split("=", 2);
            String key = URLDecoder.decode(item[0], StandardCharsets.UTF_8);
            String value = item.length > 1 ? URLDecoder.decode(item[1], StandardCharsets.UTF_8) : "";
            data.put(key, value);
        }

        return data;
    }

    public static String getValue(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static void addCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
    }

    public static void sendJson(HttpExchange exchange, int statusCode, String text) throws IOException {
        addCommonHeaders(exchange);
        byte[] response = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    public static String jsonMessage(String status, String message) {
        return "{\"status\":\"" + status + "\",\"message\":\"" + escapeJson(message) + "\"}";
    }

    public static String jsonUser(String message, skillswap.model.User user) {
        String extra = message == null ? "" : ",\"message\":\"" + escapeJson(message) + "\"";
        return "{\"status\":\"ok\"" + extra + ",\"user\":" + user.toJson() + "}";
    }

    public static String getContentType(String fileName) {
        if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        return "text/html; charset=UTF-8";
    }

    public static String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    public static void setSessionCookie(HttpExchange exchange, String token) {
        exchange.getResponseHeaders().add("Set-Cookie",
            COOKIE_NAME + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");
    }

    public static void clearSessionCookie(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Set-Cookie",
            COOKIE_NAME + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
    }

    public static String readCookie(HttpExchange exchange, String name) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return null;
        }

        for (String header : cookies) {
            String[] pieces = header.split(";");
            for (String piece : pieces) {
                String[] pair = piece.trim().split("=", 2);
                if (pair.length == 2 && pair[0].equals(name)) {
                    return pair[1];
                }
            }
        }
        return null;
    }

    public static String requireLogin(HttpExchange exchange, SessionStore sessions) throws IOException {
        String token = readCookie(exchange, COOKIE_NAME);
        String email = sessions.emailFor(token);
        if (email == null) {
            sendJson(exchange, 401, jsonMessage("error", "Please log in first."));
            return null;
        }
        return email;
    }
}
