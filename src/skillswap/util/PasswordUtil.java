package skillswap.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class PasswordUtil {
    private static final SecureRandom random = new SecureRandom();

    public static String newSalt() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return toHex(bytes);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (Exception error) {
            throw new RuntimeException("Could not hash the password.", error);
        }
    }

    public static boolean matches(String password, String salt, String storedHash) {
        if (password == null || salt == null || storedHash == null) {
            return false;
        }
        return hash(password, salt).equalsIgnoreCase(storedHash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte value : bytes) {
            text.append(String.format("%02x", value));
        }
        return text.toString();
    }
}
