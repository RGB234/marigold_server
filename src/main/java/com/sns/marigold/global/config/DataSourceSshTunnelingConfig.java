package com.sns.marigold.global.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DataSourceSshTunnelingConfig {

  private final SshTunnelingInitializer sshTunnelingInitializer;
  private static final Logger logger = LoggerFactory.getLogger(DataSourceSshTunnelingConfig.class);

  @Bean("dataSource")
  @Primary
  public DataSource dataSource(DataSourceProperties properties) {
    Integer forwardedPort = sshTunnelingInitializer.buildSshConnection();
    String url = properties.getUrl().replace("[forwardedPort]", forwardedPort.toString());
    properties.setUrl(url);
    logger.debug("DataSource URL configured through SSH tunnel. forwardedPort={}", forwardedPort);
    return properties.initializeDataSourceBuilder().build();
  }
}
