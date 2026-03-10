package com.jishan.securevault.config;

import org.springframework.context.annotation.Configuration;

// DataSource is configured via application.properties
// Spring Boot auto-configures MySQL connection from those properties
@Configuration
public class DataSourceConfig {
    // No manual bean needed — Spring Boot handles it automatically
}
