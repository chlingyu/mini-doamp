package com.demo.minidoamp.event.util;

import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomSqlValidator 单元测试 — 纯 JUnit 5，无需 Spring 上下文。
 */
class CustomSqlValidatorTest {

    // ═══════════════ 合法 SQL ═══════════════

    @Test
    @DisplayName("合法：简单 SELECT 带 WHERE")
    void validSimpleSelect() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT value, group_key FROM t_index_running WHERE date = '2024-01'"));
    }

    @Test
    @DisplayName("合法：聚合函数 + GROUP BY")
    void validAggregateGroupBy() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT COUNT(*) AS value FROM t_index_operation GROUP BY dept_id"));
    }

    @Test
    @DisplayName("合法：AVG + 别名")
    void validAvgWithAlias() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT AVG(amount) AS value FROM t_index_group"));
    }

    @Test
    @DisplayName("合法：多行 SQL（含换行符）")
    void validMultiLine() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT SUM(amount) AS value,\n  group_key\nFROM t_index_running\nWHERE status = 1\nGROUP BY group_key"));
    }

    @Test
    @DisplayName("合法：大小写混写")
    void validMixedCase() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "select Value from T_INDEX_RUNNING where id > 0"));
    }

    @Test
    @DisplayName("合法：JOIN 两张白名单表")
    void validJoin() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT a.value FROM t_index_running a LEFT JOIN t_warn_index b ON a.index_id = b.id"));
    }

    @Test
    @DisplayName("合法：CASE WHEN 表达式")
    void validCaseWhen() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT CASE WHEN amount > 100 THEN 1 ELSE 0 END AS value FROM t_index_running"));
    }

    @Test
    @DisplayName("合法：COALESCE 空值处理")
    void validCoalesce() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT COALESCE(amount, 0) AS value FROM t_index_operation"));
    }

    // ═══════════════ 非法 SQL ═══════════════

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t\n" })
    @DisplayName("非法：空值/空白")
    void invalidEmpty(String sql) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate(sql));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：非 SELECT 语句（UPDATE）")
    void invalidUpdate() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("UPDATE t_index_running SET value = 0"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：多语句（分号）")
    void invalidMultiStatement() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT value FROM t_index_running; DROP TABLE t_sys_user"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：行注释")
    void invalidLineComment() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT value FROM t_index_running -- comment"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：块注释")
    void invalidBlockComment() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT /* bypass */ value FROM t_index_running"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：未授权系统表")
    void invalidSystemTable() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT value FROM t_sys_user"));
        assertEquals(ErrorCode.CUSTOM_SQL_TABLE_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：未授权函数（LOAD_FILE）")
    void invalidDangerousFunction() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT LOAD_FILE('/etc/passwd') AS value FROM t_index_running"));
        assertEquals(ErrorCode.CUSTOM_SQL_FUNC_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：UNION 注入")
    void invalidUnion() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator
                        .validate("SELECT value FROM t_index_running UNION SELECT password FROM t_sys_user"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：子查询")
    void invalidSubquery() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator
                        .validate("SELECT (SELECT password FROM t_sys_user) AS value FROM t_index_running"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：INTO OUTFILE")
    void invalidIntoOutfile() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT value INTO OUTFILE '/tmp/x' FROM t_index_running"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：模板占位符 ${}")
    void invalidTemplateExpression() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT value FROM t_index_running WHERE id = ${id}"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：MyBatis 占位符 #{}")
    void invalidMybatisPlaceholder() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT value FROM t_index_running WHERE id = #{id}"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：JDBC 占位符 ?")
    void invalidJdbcPlaceholder() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT value FROM t_index_running WHERE id = ?"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：缺少 value 别名列")
    void invalidMissingValueAlias() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT amount, group_key FROM t_index_running"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：DELETE 伪装")
    void invalidDeleteDisguised() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("DELETE FROM t_index_running WHERE 1=1"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法：无 FROM 子句（SELECT 1）")
    void invalidNoFromClause() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT 1 AS value"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    // ═══════════════ GPT 复审补充用例 ═══════════════

    @Test
    @DisplayName("非法：MySQL # 行注释")
    void invalidMysqlHashComment() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT value FROM t_index_running # comment"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("合法：CAST(amount AS DECIMAL(10,2)) 不误杀")
    void validCastDecimal() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT CAST(amount AS DECIMAL(10,2)) AS value FROM t_index_running"));
    }

    @Test
    @DisplayName("非法：AS avg_value 不应通过（精确别名校验）")
    void invalidAvgValueAlias() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CustomSqlValidator.validate("SELECT amount AS avg_value FROM t_index_running"));
        assertEquals(ErrorCode.CUSTOM_SQL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("合法：COUNT(*) value（无 AS 关键字的别名）")
    void validCountValueWithoutAs() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT COUNT(*) value FROM t_index_operation"));
    }

    @Test
    @DisplayName("合法：带表别名的 a.value 列")
    void validTableAliasValue() {
        assertDoesNotThrow(() -> CustomSqlValidator.validate(
                "SELECT a.value, a.group_key FROM t_index_running a WHERE a.status = 1"));
    }
}
