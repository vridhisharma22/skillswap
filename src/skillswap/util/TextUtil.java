package skillswap.util;

public class TextUtil {
    public static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static String key(String value) {
        return clean(value).toLowerCase();
    }

    public static boolean looksLikeEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
