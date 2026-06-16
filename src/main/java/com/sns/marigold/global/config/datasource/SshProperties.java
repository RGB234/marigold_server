package com.sns.marigold.global.config.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ssh")
public record SshProperties(
    String host,
    int port,
    String user,
    String privateKey,
    String remoteHost,
    int remotePort) {}
