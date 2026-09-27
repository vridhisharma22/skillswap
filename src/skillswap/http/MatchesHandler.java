package skillswap.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import skillswap.model.User;
import skillswap.store.SessionStore;
import skillswap.store.UserStore;

public class MatchesHandler implements HttpHandler {
    private final UserStore userStore;
    private final SessionStore sessions;

    public MatchesHandler(UserStore userStore, SessionStore sessions) {
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

        User currentUser = userStore.findByEmail(email);
        if (currentUser == null) {
            HttpSupport.sendJson(exchange, 404, HttpSupport.jsonMessage("error", "User not found."));
            return;
        }

        List<User> users = userStore.readUsers();
        StringBuilder matchesJson = new StringBuilder();
        matchesJson.append("[");
        boolean firstMatch = true;

        for (User user : users) {
            if (user.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
                continue;
            }

            boolean perfectSwap = currentUser.isPerfectSwapWith(user);
            boolean similarInterest = currentUser.hasSimilarInterest(user);

            if (perfectSwap || similarInterest) {
                if (!firstMatch) {
                    matchesJson.append(",");
                }

                String reason = perfectSwap
                    ? "You can swap skills both ways"
                    : "You overlap on a skill or a goal";

                matchesJson.append("{")
                    .append("\"name\":\"").append(HttpSupport.escapeJson(user.getName())).append("\",")
                    .append("\"email\":\"").append(HttpSupport.escapeJson(user.getEmail())).append("\",")
                    .append("\"skill\":\"").append(HttpSupport.escapeJson(user.getSkill())).append("\",")
                    .append("\"want\":\"").append(HttpSupport.escapeJson(user.getWant())).append("\",")
                    .append("\"perfect\":").append(perfectSwap).append(",")
                    .append("\"reason\":\"").append(HttpSupport.escapeJson(reason)).append("\"")
                    .append("}");

                firstMatch = false;
            }
        }

        matchesJson.append("]");
        HttpSupport.sendJson(exchange, 200, "{\"status\":\"ok\",\"matches\":" + matchesJson + "}");
    }
}
