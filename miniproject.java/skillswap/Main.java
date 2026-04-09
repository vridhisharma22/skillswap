import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    static File baseFolder = new File(".");
    static File usersFile = new File(baseFolder, "users.csv");
    static File webFolder = new File(baseFolder, "web");

    public static void main(String[] args) throws Exception {
        createUsersFileIfNeeded();

        int port = 8080;
String portEnv = System.getenv("PORT");
if (portEnv != null) {
    port = Integer.parseInt(portEnv);
}
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new FrontendHandler());
        server.createContext("/signup", new SignupHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/matches", new MatchesHandler());
        server.createContext("/update", new UpdateHandler());
        server.start();

        System.out.println("SkillSwap is running at http://localhost:" + port);
    }

    static void createUsersFileIfNeeded() throws IOException {
        if (!usersFile.exists()) {
            FileWriter writer = new FileWriter(usersFile);
            writer.write("Name,Email,Password,Skill,Want\n");
            writer.close();
        }
    }

    static class FrontendHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, "{\"status\":\"error\",\"message\":\"Only GET is allowed here.\"}");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            File file = new File(webFolder, path.substring(1));

            if (!file.exists() || file.isDirectory()) {
                sendJson(exchange, 404, "{\"status\":\"error\",\"message\":\"Page not found.\"}");
                return;
            }

            byte[] data = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", getContentType(file.getName()));
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        }
    }

    static class SignupHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "{\"status\":\"error\",\"message\":\"Only POST is allowed here.\"}");
                return;
            }

            Map<String, String> form = readForm(exchange);
            String name = getValue(form, "name");
            String email = getValue(form, "email").toLowerCase();
            String password = getValue(form, "password");
            String skill = getValue(form, "skill");
            String want = getValue(form, "want");

            if (name.isBlank() || email.isBlank() || password.isBlank() || skill.isBlank() || want.isBlank()) {
                sendJson(exchange, 400, "{\"status\":\"error\",\"message\":\"Please fill in every field.\"}");
                return;
            }

            List<User> users = readUsers();
            for (User user : users) {
                if (user.email.equalsIgnoreCase(email)) {
                    sendJson(exchange, 409, "{\"status\":\"error\",\"message\":\"This email is already registered.\"}");
                    return;
                }
            }

            User newUser = new User(name, email, password, skill, want);
            users.add(newUser);
            saveUsers(users);

            sendJson(exchange, 200,
                "{\"status\":\"ok\",\"message\":\"Your account has been created.\",\"user\":" + newUser.toJson() + "}");
        }
    }

    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "{\"status\":\"error\",\"message\":\"Only POST is allowed here.\"}");
                return;
            }

            Map<String, String> form = readForm(exchange);
            String email = getValue(form, "email").toLowerCase();
            String password = getValue(form, "password");

            List<User> users = readUsers();
            for (User user : users) {
                if (user.email.equalsIgnoreCase(email) && user.password.equals(password)) {
                    sendJson(exchange, 200, "{\"status\":\"ok\",\"user\":" + user.toJson() + "}");
                    return;
                }
            }

            sendJson(exchange, 401, "{\"status\":\"error\",\"message\":\"That email and password do not match.\"}");
        }
    }

    static class MatchesHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, "{\"status\":\"error\",\"message\":\"Only GET is allowed here.\"}");
                return;
            }

            Map<String, String> query = readQuery(exchange.getRequestURI().getQuery());
            String email = getValue(query, "email").toLowerCase();

            List<User> users = readUsers();
            User currentUser = null;

            for (User user : users) {
                if (user.email.equalsIgnoreCase(email)) {
                    currentUser = user;
                    break;
                }
            }

            if (currentUser == null) {
                sendJson(exchange, 404, "{\"status\":\"error\",\"message\":\"User not found.\"}");
                return;
            }

            StringBuilder matchesJson = new StringBuilder();
            matchesJson.append("[");
            boolean firstMatch = true;

            for (User user : users) {
                if (user.email.equalsIgnoreCase(currentUser.email)) {
                    continue;
                }

                boolean perfectSwap =
                    currentUser.skill.equalsIgnoreCase(user.want) &&
                    currentUser.want.equalsIgnoreCase(user.skill);

                boolean similarInterest =
                    currentUser.want.equalsIgnoreCase(user.want) ||
                    currentUser.skill.equalsIgnoreCase(user.skill);

                if (perfectSwap || similarInterest) {
                    if (!firstMatch) {
                        matchesJson.append(",");
                    }

                    String reason = perfectSwap ? "Great two-way skill swap" : "You have a similar learning path";
                    matchesJson.append("{")
                        .append("\"name\":\"").append(escapeJson(user.name)).append("\",")
                        .append("\"email\":\"").append(escapeJson(user.email)).append("\",")
                        .append("\"skill\":\"").append(escapeJson(user.skill)).append("\",")
                        .append("\"want\":\"").append(escapeJson(user.want)).append("\",")
                        .append("\"reason\":\"").append(escapeJson(reason)).append("\"")
                        .append("}");

                    firstMatch = false;
                }
            }

            matchesJson.append("]");
            sendJson(exchange, 200, "{\"status\":\"ok\",\"matches\":" + matchesJson + "}");
        }
    }

    static class UpdateHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "{\"status\":\"error\",\"message\":\"Only POST is allowed here.\"}");
                return;
            }

            Map<String, String> form = readForm(exchange);
            String email = getValue(form, "email").toLowerCase();
            String skill = getValue(form, "skill");
            String want = getValue(form, "want");

            if (email.isBlank() || skill.isBlank() || want.isBlank()) {
                sendJson(exchange, 400, "{\"status\":\"error\",\"message\":\"Please enter both skill fields.\"}");
                return;
            }

            List<User> users = readUsers();
            User updatedUser = null;

            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                if (user.email.equalsIgnoreCase(email)) {
                    updatedUser = new User(user.name, user.email, user.password, skill, want);
                    users.set(i, updatedUser);
                    break;
                }
            }

            if (updatedUser == null) {
                sendJson(exchange, 404, "{\"status\":\"error\",\"message\":\"User not found.\"}");
                return;
            }

            saveUsers(users);
            sendJson(exchange, 200,
                "{\"status\":\"ok\",\"message\":\"Your profile has been updated.\",\"user\":" + updatedUser.toJson() + "}");
        }
    }

    static List<User> readUsers() throws IOException {
        List<User> users = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(usersFile));
        String line = reader.readLine();

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(",", -1);
            if (parts.length < 5) {
                continue;
            }

            users.add(new User(parts[0], parts[1], parts[2], parts[3], parts[4]));
        }

        reader.close();
        return users;
    }

    static void saveUsers(List<User> users) throws IOException {
        FileWriter writer = new FileWriter(usersFile);
        writer.write("Name,Email,Password,Skill,Want\n");

        for (User user : users) {
            writer.write(user.name + "," + user.email + "," + user.password + "," + user.skill + "," + user.want + "\n");
        }

        writer.close();
    }

    static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return splitData(body);
    }

    static Map<String, String> readQuery(String query) throws IOException {
        if (query == null) {
            query = "";
        }
        return splitData(query);
    }

    static Map<String, String> splitData(String text) throws IOException {
        Map<String, String> data = new HashMap<>();

        if (text.isBlank()) {
            return data;
        }

        String[] pairs = text.split("&");
        for (String pair : pairs) {
            String[] item = pair.split("=", 2);
            String key = URLDecoder.decode(item[0], "UTF-8");
            String value = item.length > 1 ? URLDecoder.decode(item[1], "UTF-8") : "";
            data.put(key, value);
        }

        return data;
    }

    static String getValue(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    static void sendJson(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] response = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    static String getContentType(String fileName) {
        if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        return "text/html; charset=UTF-8";
    }

    static String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    static class User {
        String name;
        String email;
        String password;
        String skill;
        String want;

        User(String name, String email, String password, String skill, String want) {
            this.name = name;
            this.email = email;
            this.password = password;
            this.skill = skill;
            this.want = want;
        }

        String toJson() {
            return "{"
                + "\"name\":\"" + escapeJson(name) + "\","
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"skill\":\"" + escapeJson(skill) + "\","
                + "\"want\":\"" + escapeJson(want) + "\""
                + "}";
        }
    }
}
