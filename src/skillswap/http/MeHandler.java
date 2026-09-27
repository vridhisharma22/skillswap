package skillswap.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import skillswap.model.User;
import skillswap.store.SessionStore;
import skillswap.store.UserStore;

public class MeHandler implements HttpHandler {
    private final UserStore userStore;
    private final SessionStore sessions;

    public MeHandler(UserStore userStore, SessionStore sessions) {
        this.userStore = userStore;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            HttpSupport.sendJson(exchange, 405, HttpSupport.jsonMessage("error", "Only GET is allowed here."));
            return;
        }

        String email = HttpSupport.requireLogin(exchange, sessions);
        if (email == null) {
            return;
        }

        User user = userStore.findByEmail(email);
        if (user == null) {
            HttpSupport.sendJson(exchange, 404, HttpSupport.jsonMessage("error", "User not found."));
            return;
        }

        HttpSupport.sendJson(exchange, 200, HttpSupport.jsonUser(null, user));
    }
}
