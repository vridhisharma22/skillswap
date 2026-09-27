package skillswap.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FrontendHandler implements HttpHandler {
    private final File webFolder;

    public FrontendHandler(File webFolder) {
        this.webFolder = webFolder;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            HttpSupport.sendJson(exchange, 405, HttpSupport.jsonMessage("error", "Only GET is allowed here."));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        Path webRoot = webFolder.toPath().toAbsolutePath().normalize();
        Path requested = webRoot.resolve(path.substring(1)).normalize();

        if (!requested.startsWith(webRoot) || !Files.isRegularFile(requested)) {
            HttpSupport.sendJson(exchange, 404, HttpSupport.jsonMessage("error", "Page not found."));
            return;
        }

        byte[] data = Files.readAllBytes(requested);
        HttpSupport.addCommonHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", HttpSupport.getContentType(requested.getFileName().toString()));
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
