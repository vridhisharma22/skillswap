package skillswap.store;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private final Map<String, String> tokenToEmail = new ConcurrentHashMap<>();

    public String create(String email) {
        String token = UUID.randomUUID().toString();
        tokenToEmail.put(token, email.toLowerCase());
        return token;
    }

    public String emailFor(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return tokenToEmail.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            tokenToEmail.remove(token);
        }
    }
}
