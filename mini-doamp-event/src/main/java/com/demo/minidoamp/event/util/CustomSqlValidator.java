package com.demo.minidoamp.event.util;

import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class CustomSqlValidator {

    private static final Pattern SELECT_PATTERN =
            Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern DANGEROUS_KEYWORDS =
            Pattern.compile("\\b(DROP|DELETE|UPDATE|INSERT|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE)\\b",
                    Pattern.CASE_INSENSITIVE);

    public static void validate(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        if (!SELECT_PATTERN.matcher(sql).find()) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        if (DANGEROUS_KEYWORDS.matcher(sql).find()) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        if (sql.contains(";")) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        if (sql.contains("--") || sql.contains("/*")) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
    }
}
