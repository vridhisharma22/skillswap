package skillswap.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import skillswap.model.User;
import skillswap.store.SessionStore;
import skillswap.store.UserStore;
import skillswap.util.TextUtil;

public class UpdateHandler implements HttpHandler {
    private final UserStore userStore;
    private final SessionStore sessions;

    public UpdateHandler(UserStore userStore, SessionStore sessions) {
        this.userStore = userStore;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            HttpSupport.sendJson(exchange, 405, HttpSupport.jsonMessage("error", "Only POST is allowed here."));
            return;
        }

        String email = HttpSupport.requireLogin(exchange, sessions);
        if (email == null) {
            return;
        }

        Map<String, String> form = HttpSupport.readForm(exchange);
        String skill = TextUtil.clean(HttpSupport.getValue(form, "skill"));
        String want = TextUtil.clean(HttpSupport.getValue(form, "want"));

        if (skill.isBlank() || want.isBlank()) {
            HttpSupport.sendJson(exchange, 400, HttpSupport.jsonMessage("error", "Please enter both skill fields."));
            return;
        }

        if (TextUtil.key(skill).equals(TextUtil.key(want))) {
            HttpSupport.sendJson(exchange, 400, HttpSupport.jsonMessage("error", "Pick different things to teach and learn."));
            return;
        }

        List<User> users = userStore.readUsers();
        User updatedUser = null;

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.getEmail().equalsIgnoreCase(email)) {
                updatedUser = user.withSkills(skill, want);
                users.set(i, updatedUser);
                break;
            }
        }

        if (updatedUser == null) {
            HttpSupport.sendJson(exchange, 404, HttpSupport.jsonMessage("error", "User not found."));
            return;
        }

        userStore.saveUsers(users);
        HttpSupport.sendJson(exchange, 200, HttpSupport.jsonUser("Your profile has been updated.", updatedUser));
    }
}
