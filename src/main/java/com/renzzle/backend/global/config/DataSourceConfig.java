package com.renzzle.backend.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class DataSourceConfig {

    // --- 1. 메인 DB 설정 (기존 JPA가 사용할 DB) ---
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mainDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource mainDataSource() {
        return mainDataSourceProperties().initializeDataSourceBuilder().build();
    }

    // --- 2. 백업 DB 설정 (새로 추가한 Aiven DB) ---
    @Bean
    @ConfigurationProperties("backup.datasource")
    public DataSourceProperties backupDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "backupDataSource")
    public DataSource backupDataSource() {
        return backupDataSourceProperties().initializeDataSourceBuilder().build();
    }

    // 백업 DB에 쿼리를 날리기 위한 JdbcTemplate 등록
    @Bean(name = "backupJdbcTemplate")
    public JdbcTemplate backupJdbcTemplate(@Qualifier("backupDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
