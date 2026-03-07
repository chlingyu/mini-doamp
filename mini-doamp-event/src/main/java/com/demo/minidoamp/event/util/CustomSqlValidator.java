package com.demo.minidoamp.event.util;

import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义 SQL 校验器 — 多层结构化白名单校验。
 * <p>
 * 校验层次：
 * <ol>
 * <li>基础检查：非空、禁止分号/注释/模板占位符</li>
 * <li>语法子集：只允许单条 SELECT ... FROM ... [WHERE/GROUP BY/HAVING/ORDER
 * BY/LIMIT]</li>
 * <li>表白名单：仅允许指标数据相关的 6 张表</li>
 * <li>函数白名单：仅允许安全的聚合/日期/字符串/类型转换函数</li>
 * <li>结果列校验：必须包含 value 别名列</li>
 * <li>结构禁止：UNION、子查询(嵌套 SELECT)、INTO OUTFILE</li>
 * </ol>
 * 保存前（WarnIndexService）与执行前（CustomSqlWarnStrategy）双校验。
 */
public class CustomSqlValidator {

    // ── 表白名单：仅开放指标数据相关表 ──
    private static final Set<String> ALLOWED_TABLES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "T_INDEX_RUNNING", "T_INDEX_OPERATION", "T_INDEX_GROUP",
                    "T_WARN_INDEX", "T_WARN_THRESHOLD", "T_WARN_RECORD")));

    // ── 函数白名单：聚合 + 日期 + 字符串 + 类型转换 ──
    private static final Set<String> ALLOWED_FUNCTIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    // 聚合
                    "COUNT", "SUM", "AVG", "MAX", "MIN",
                    // 数值
                    "ROUND", "ABS", "CEIL", "FLOOR",
                    // 空值处理
                    "COALESCE", "IFNULL", "NULLIF",
                    // 日期（MySQL + H2 双方言）
                    "DATE_FORMAT", "FORMATDATETIME", "NOW", "CURRENT_DATE", "CURRENT_TIMESTAMP",
                    // 类型转换
                    "CAST", "CONVERT",
                    // 字符串
                    "CONCAT", "UPPER", "LOWER", "TRIM", "LENGTH", "SUBSTRING",
                    // 聚合字符串（MySQL + H2 双方言）
                    "GROUP_CONCAT", "LISTAGG")));

    // ── SQL 关键字 + 类型名（与函数名区分，不参与函数白名单校验） ──
    // 包含 CAST/CONVERT 内部常见类型名，防止 DECIMAL(10,2) 被误判为非法函数
    private static final Set<String> SQL_KEYWORDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "IS",
                    "NULL", "AS", "ON", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER",
                    "GROUP", "BY", "HAVING", "ORDER", "ASC", "DESC", "LIMIT", "OFFSET",
                    "BETWEEN", "LIKE", "EXISTS", "DISTINCT", "CASE", "WHEN", "THEN",
                    "ELSE", "END", "IF",
                    // CAST/CONVERT 内部类型名
                    "DECIMAL", "NUMERIC", "INT", "INTEGER", "BIGINT", "SMALLINT",
                    "FLOAT", "DOUBLE", "REAL",
                    "VARCHAR", "CHAR", "TEXT", "NVARCHAR",
                    "DATE", "TIME", "TIMESTAMP", "DATETIME",
                    "BOOLEAN", "SIGNED", "UNSIGNED")));

    // ── DML/DDL 黑名单关键字 ──
    private static final Pattern DML_DDL_PATTERN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE|"
                    + "REPLACE|MERGE|GRANT|REVOKE|CALL)\\b",
            Pattern.CASE_INSENSITIVE);

    // ── 表名提取：FROM/JOIN 后的标识符 ──
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "\\b(?:FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_]*)",
            Pattern.CASE_INSENSITIVE);

    // ── 函数调用提取：标识符紧跟左括号 ──
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    // ── 模板占位符（# 注释已在基础检查中拦截，这里只处理 ${} 和 ?） ──
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile(
            "\\$\\{|\\?");

    // ── value 别名精确检测 ──
    // 匹配: AS VALUE, ) VALUE, expr VALUE, a.VALUE（表别名限定列）
    // 不匹配: avg_value, value2, myvalue
    private static final Pattern VALUE_ALIAS_PATTERN = Pattern.compile(
            // Pattern 1: AS VALUE (with optional leading context)
            "\\bAS\\s+VALUE\\b"
                    // Pattern 2: ) VALUE or , VALUE or space VALUE (implicit alias after
                    // expression)
                    + "|(?<=[)\\s,])VALUE(?:\\s*,|\\s+FROM\\b|\\s*$)"
                    // Pattern 3: SELECT VALUE (bare column at start)
                    + "|\\bSELECT\\s+VALUE\\b"
                    // Pattern 4: table.VALUE (qualified column name like a.value)
                    + "|\\w+\\.VALUE\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * 校验自定义 SQL。校验失败抛出 {@link BusinessException}。
     *
     * @param sql 用户输入的 SQL
     */
    public static void validate(String sql) {
        // ── 第 1 层：基础检查 ──
        if (!StringUtils.hasText(sql)) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }

        // 禁止分号（多语句）
        if (sql.contains(";")) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        // 禁止注释（-- 行注释、/* 块注释、MySQL # 行注释）
        if (sql.contains("--") || sql.contains("/*") || sql.contains("#")) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        // 禁止模板占位符
        if (TEMPLATE_PATTERN.matcher(sql).find()) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }

        // ── 第 2 层：标准化 & 语法子集 ──
        String normalized = sql.replaceAll("\\s+", " ").trim().toUpperCase();

        // 必须以 SELECT 开头
        if (!normalized.startsWith("SELECT ")) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        // 禁止 DML/DDL
        if (DML_DDL_PATTERN.matcher(normalized).find()) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        // 禁止 UNION（防止拼接查询）
        if (Pattern.compile("\\bUNION\\b", Pattern.CASE_INSENSITIVE).matcher(normalized).find()) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        // 禁止 INTO（INTO OUTFILE / INTO DUMPFILE）
        if (Pattern.compile("\\bINTO\\s+(OUTFILE|DUMPFILE)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(normalized).find()) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
        // 禁止子查询：SELECT 只应出现一次（在开头）
        int firstSelect = normalized.indexOf("SELECT");
        int secondSelect = normalized.indexOf("SELECT", firstSelect + 1);
        if (secondSelect >= 0) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }

        // ── 第 3 层：表白名单 ──
        Matcher tableMatcher = TABLE_PATTERN.matcher(normalized);
        boolean hasTable = false;
        while (tableMatcher.find()) {
            hasTable = true;
            String table = tableMatcher.group(1).toUpperCase();
            if (!ALLOWED_TABLES.contains(table)) {
                throw new BusinessException(ErrorCode.CUSTOM_SQL_TABLE_DENIED);
            }
        }
        if (!hasTable) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }

        // ── 第 4 层：函数白名单 ──
        Matcher funcMatcher = FUNCTION_PATTERN.matcher(normalized);
        while (funcMatcher.find()) {
            String func = funcMatcher.group(1).toUpperCase();
            // 跳过 SQL 关键字（如 CASE, IF, EXISTS 后面可能跟括号）
            if (SQL_KEYWORDS.contains(func)) {
                continue;
            }
            if (!ALLOWED_FUNCTIONS.contains(func)) {
                throw new BusinessException(ErrorCode.CUSTOM_SQL_FUNC_DENIED);
            }
        }

        // ── 第 5 层：结果列必须包含精确的 value 别名 ──
        String selectClause = extractSelectClause(normalized);
        if (!VALUE_ALIAS_PATTERN.matcher(selectClause).find()) {
            throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
        }
    }

    /**
     * 提取 SELECT 和 FROM 之间的列定义子句。
     */
    private static String extractSelectClause(String normalizedSql) {
        int fromIdx = normalizedSql.indexOf(" FROM ");
        if (fromIdx < 0) {
            return normalizedSql;
        }
        return normalizedSql.substring(0, fromIdx);
    }
}
