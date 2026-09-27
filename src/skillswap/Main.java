package skillswap;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.net.InetSocketAddress;
import skillswap.http.FrontendHandler;
import skillswap.http.LoginHandler;
import skillswap.http.LogoutHandler;
import skillswap.http.MatchesHandler;
import skillswap.http.MeHandler;
import skillswap.http.SignupHandler;
import skillswap.http.UpdateHandler;
import skillswap.store.SessionStore;
import skillswap.store.UserStore;

public class Main {
    public static void main(String[] args) throws Exception {
        File baseFolder = new File(".");
        UserStore userStore = new UserStore(new File(baseFolder, "data/users.csv"));
        SessionStore sessions = new SessionStore();
        File webFolder = new File(baseFolder, "web");

        userStore.createFileIfNeeded();

        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            port = Integer.parseInt(portEnv);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new FrontendHandler(webFolder));
        server.createContext("/signup", new SignupHandler(userStore, sessions));
        server.createContext("/login", new LoginHandler(userStore, sessions));
        server.createContext("/logout", new LogoutHandler(sessions));
        server.createContext("/me", new MeHandler(userStore, sessions));
        server.createContext("/matches", new MatchesHandler(userStore, sessions));
        server.createContext("/update", new UpdateHandler(userStore, sessions));
        server.start();

        System.out.println("SkillSwap is running at http://localhost:" + port);
    }
}
