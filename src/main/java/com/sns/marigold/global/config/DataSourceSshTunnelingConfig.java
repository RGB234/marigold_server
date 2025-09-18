package com.sns.marigold.global.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
@RequiredArgsConstructor
public class DataSourceSshTunnelingConfig {

  private final SshTunnelingInitializer sshTunnelingInitializer;
  Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

  @Bean("dataSource")
  @Primary
  public DataSource dataSource(DataSourceProperties properties) {
    Integer forwardedPort = sshTunnelingInitializer.buildSshConnection();
    String url = properties.getUrl().replace("[forwardedPort]", forwardedPort.toString());
    properties.setUrl(url);
    logger.info("DataSource url : {}", url);
    return properties.initializeDataSourceBuilder().build();
  }
}
