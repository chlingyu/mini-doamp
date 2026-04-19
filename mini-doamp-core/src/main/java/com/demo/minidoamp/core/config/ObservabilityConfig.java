package com.demo.minidoamp.core.config;

import com.demo.minidoamp.core.filter.MdcRequestFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ObservabilityConfig {

    @Bean
    public FilterRegistrationBean<MdcRequestFilter> mdcRequestFilter() {
        FilterRegistrationBean<MdcRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MdcRequestFilter());
        registration.addUrlPatterns("/*");
        registration.setName("mdcRequestFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
