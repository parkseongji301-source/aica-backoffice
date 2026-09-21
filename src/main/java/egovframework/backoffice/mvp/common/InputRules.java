package egovframework.backoffice.mvp.common;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class InputRules {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private InputRules() {}

    public static String email(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL.matcher(normalized).matches())
            throw new BusinessException("올바른 이메일을 입력하세요.");
        return normalized;
    }

    public static String text(String value, int max, String label) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new BusinessException(label + "은(는) 1~" + max + "자로 입력하세요.");
        return value.strip();
    }

    public static void password(String value) {
        if (value == null || value.isBlank() || value.length() < 12
                || value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("비밀번호는 12자 이상, UTF-8 기준 72바이트 이하로 입력하세요.");
    }
}
