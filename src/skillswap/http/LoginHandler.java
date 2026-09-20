package skillswap.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import skillswap.model.User;
import skillswap.store.SessionStore;
import skillswap.store.UserStore;
import skillswap.util.PasswordUtil;

public class LoginHandler implements HttpHandler {
    private final UserStore userStore;
    private final SessionStore sessions;

    public LoginHandler(UserStore userStore, SessionStore sessions) {
        this.userStore = userStore;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            HttpSupport.sendJson(exchange, 405, HttpSupport.jsonMessage("error", "Only POST is allowed here."));
            return;
        }

        Map<String, String> form = HttpSupport.readForm(exchange);
        String email = HttpSupport.getValue(form, "email").toLowerCase();
        String password = HttpSupport.getValue(form, "password");

        List<User> users = userStore.readUsers();
        for (User user : users) {
            if (user.getEmail().equalsIgnoreCase(email)
                && PasswordUtil.matches(password, user.getSalt(), user.getPasswordHash())) {
                HttpSupport.setSessionCookie(exchange, sessions.create(user.getEmail()));
                HttpSupport.sendJson(exchange, 200, HttpSupport.jsonUser(null, user));
                return;
            }
        }

        HttpSupport.sendJson(exchange, 401, HttpSupport.jsonMessage("error", "That email and password do not match."));
    }
}
