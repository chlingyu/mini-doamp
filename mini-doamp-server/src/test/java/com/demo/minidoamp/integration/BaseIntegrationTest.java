package com.demo.minidoamp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试基类：
 * - SpringBootTest 随机端口
 * - 激活 test profile（H2 + 无 Redis/RabbitMQ）
 * - 导入 TestInfraConfig（Mock Redis + Mock RabbitMQ）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestInfraConfig.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 使用 admin/admin123 登录，返回 access token
     */
    protected String loginAndGetToken() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/auth/login", jsonEntity(body, null), String.class);
        assertEquals(200, resp.getStatusCodeValue());
        JsonNode json = objectMapper.readTree(resp.getBody());
        assertEquals(200, json.get("code").asInt());
        return json.get("data").get("token").asText();
    }

    /**
     * 登录并同时返回 token 和 refreshToken
     */
    protected String[] loginAndGetTokenPair() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/auth/login", jsonEntity(body, null), String.class);
        JsonNode data = objectMapper.readTree(resp.getBody()).get("data");
        return new String[]{data.get("token").asText(), data.get("refreshToken").asText()};
    }

    /**
     * 构造 JSON 请求体 + Authorization header
     */
    protected HttpEntity<String> jsonEntity(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return new HttpEntity<>(body, headers);
    }

    /**
     * 带 token 的 GET 请求
     */
    protected ResponseEntity<String> getWithToken(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    /**
     * 带 token 的 POST 请求
     */
    protected ResponseEntity<String> postWithToken(String url, String body, String token) {
        return restTemplate.exchange(url, HttpMethod.POST, jsonEntity(body, token), String.class);
    }

    /**
     * 带 token 的 PUT 请求
     */
    protected ResponseEntity<String> putWithToken(String url, String body, String token) {
        return restTemplate.exchange(url, HttpMethod.PUT, jsonEntity(body, token), String.class);
    }

    /**
     * 解析响应 JSON 的 code 字段
     */
    protected int getCode(ResponseEntity<String> resp) throws Exception {
        return objectMapper.readTree(resp.getBody()).get("code").asInt();
    }

    /**
     * 解析响应 JSON 的 data 字段
     */
    protected JsonNode getData(ResponseEntity<String> resp) throws Exception {
        return objectMapper.readTree(resp.getBody()).get("data");
    }
}
