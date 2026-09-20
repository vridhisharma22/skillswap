package skillswap.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import skillswap.store.SessionStore;

public class LogoutHandler implements HttpHandler {
    private final SessionStore sessions;

    public LogoutHandler(SessionStore sessions) {
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            HttpSupport.sendJson(exchange, 405, HttpSupport.jsonMessage("error", "Only POST is allowed here."));
            return;
        }

        sessions.remove(HttpSupport.readCookie(exchange, HttpSupport.COOKIE_NAME));
        HttpSupport.clearSessionCookie(exchange);
        HttpSupport.sendJson(exchange, 200, HttpSupport.jsonMessage("ok", "You are logged out."));
    }
}
