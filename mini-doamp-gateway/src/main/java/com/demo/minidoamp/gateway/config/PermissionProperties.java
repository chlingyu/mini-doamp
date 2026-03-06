package com.demo.minidoamp.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "security.permission")
public class PermissionProperties {

    private List<String> defaults = new ArrayList<String>();

    private Map<String, List<String>> roleMappings = new LinkedHashMap<String, List<String>>();
}
