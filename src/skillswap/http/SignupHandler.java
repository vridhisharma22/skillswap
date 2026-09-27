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

public class SignupHandler implements HttpHandler {
    private final UserStore userStore;
    private final SessionStore sessions;

    public SignupHandler(UserStore userStore, SessionStore sessions) {
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
        String name = TextUtil.clean(HttpSupport.getValue(form, "name"));
        String email = TextUtil.clean(HttpSupport.getValue(form, "email")).toLowerCase();
        String password = HttpSupport.getValue(form, "password");
        String skill = TextUtil.clean(HttpSupport.getValue(form, "skill"));
        String want = TextUtil.clean(HttpSupport.getValue(form, "want"));

        if (name.length() < 2 || email.isBlank() || password.isBlank() || skill.isBlank() || want.isBlank()) {
            HttpSupport.sendJson(exchange, 400, HttpSupport.jsonMessage("error", "Please fill in every field."));
            return;
        }

        if (!TextUtil.looksLikeEmail(email)) {
            HttpSupport.sendJson(exchange, 400, HttpSupport.jsonMessage("error", "That email does not look right."));
            return;
        }

        if (password.length() < 6) {
            HttpSupport.sendJson(exchange, 400, HttpSupport.jsonMessage("error", "Use at least 6 characters for your password."));
            return;
        }

        if (TextUtil.key(skill).equals(TextUtil.key(want))) {
            HttpSupport.sendJson(exchange, 400, HttpSupport.jsonMessage("error", "Pick different things to teach and learn, otherwise matching gets messy."));
            return;
        }

        List<User> users = userStore.readUsers();
        for (User user : users) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                HttpSupport.sendJson(exchange, 409, HttpSupport.jsonMessage("error", "This email is already registered."));
                return;
            }
        }

        User newUser = userStore.createUser(name, email, password, skill, want);
        users.add(newUser);
        userStore.saveUsers(users);

        HttpSupport.setSessionCookie(exchange, sessions.create(newUser.getEmail()));
        HttpSupport.sendJson(exchange, 200, HttpSupport.jsonUser("Welcome to SkillSwap.", newUser));
    }
}
