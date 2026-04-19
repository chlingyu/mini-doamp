package com.demo.minidoamp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 集成测试基类：
 * - SpringBootTest 随机端口
 * - 激活 test profile（无 H2，MySQL 由 Testcontainers 动态启动）
 * - 所有继承类共享同一个 MySQL 容器（singleton via static init）
 * - 导入 TestInfraConfig（Mock Redis + Mock RabbitMQ）
 *
 * 运行前提：本机 Docker Desktop / Docker Engine 可用
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestInfraConfig.class)
@Testcontainers
public abstract class BaseIntegrationTest {

    @SuppressWarnings("resource")
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("mini_doamp_test")
            .withUsername("root")
            .withPassword("root")
            .withCommand("--character-set-server=utf8mb4",
                         "--collation-server=utf8mb4_unicode_ci")
            .withReuse(true);

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:sql/schema.sql");
        registry.add("spring.sql.init.data-locations", () -> "classpath:sql/data.sql");
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected String loginAndGetToken() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/auth/login", jsonEntity(body, null), String.class);
        assertEquals(200, resp.getStatusCodeValue());
        JsonNode json = objectMapper.readTree(resp.getBody());
        assertEquals(200, json.get("code").asInt());
        return json.get("data").get("token").asText();
    }

    protected String[] loginAndGetTokenPair() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/auth/login", jsonEntity(body, null), String.class);
        JsonNode data = objectMapper.readTree(resp.getBody()).get("data");
        return new String[]{data.get("token").asText(), data.get("refreshToken").asText()};
    }

    protected HttpEntity<String> jsonEntity(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return new HttpEntity<>(body, headers);
    }

    protected ResponseEntity<String> getWithToken(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    protected ResponseEntity<String> postWithToken(String url, String body, String token) {
        return restTemplate.exchange(url, HttpMethod.POST, jsonEntity(body, token), String.class);
    }

    protected ResponseEntity<String> putWithToken(String url, String body, String token) {
        return restTemplate.exchange(url, HttpMethod.PUT, jsonEntity(body, token), String.class);
    }

    protected int getCode(ResponseEntity<String> resp) throws Exception {
        return objectMapper.readTree(resp.getBody()).get("code").asInt();
    }

    protected JsonNode getData(ResponseEntity<String> resp) throws Exception {
        return objectMapper.readTree(resp.getBody()).get("data");
    }
}
