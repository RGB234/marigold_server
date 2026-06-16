package com.sns.marigold.global.config.datasource;

import static java.lang.System.exit;

import java.io.File;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class SshTunnelingInitializer {

  private final SshProperties sshProperties;
  private Session session;
  private final Logger logger = LoggerFactory.getLogger(SshTunnelingInitializer.class);

  @PreDestroy
  public void closeSSH() {
    if (session != null && session.isConnected()) {
      session.disconnect();
    }
  }

  public Integer buildSshConnection() {
    Integer forwardedPort = null;

    JSch.setLogger(
        new com.jcraft.jsch.Logger() {
          public boolean isEnabled(int level) {
            return true;
          }

          public void log(int level, String message) {
            logger.debug("JSch: {}", message);
          }
        });

    try {
      logger.info("Ssh tunneling start");
      logger.debug(
          "SSH tunnel target configured. sshPort={}, remotePort={}",
          sshProperties.port(),
          sshProperties.remotePort());

      JSch jsch = new JSch();
      String privateKey = sshProperties.privateKey();

      if (!new File(privateKey).exists()) {
        throw new IllegalStateException("비공개 키 파일을 찾을 수 없습니다.");
      }
      jsch.addIdentity(privateKey);

      KeyPair kp = KeyPair.load(jsch, privateKey);
      if (kp == null) {
        logger.error("SSH private key load failed");
      }

      logger.debug("Creating SSH session");
      session = jsch.getSession(sshProperties.user(), sshProperties.host(), sshProperties.port());
      Properties config = new Properties();
      // 최초 SSH 접속 시 서버의 호스트 키 신뢰
      config.put("StrictHostKeyChecking", "no");

      logger.debug("Setting SSH config");
      session.setConfig(config);

      logger.info("connecting ssh session");
      session.connect(10000);

      logger.info("successfully connected");
      logger.info("port forwarding start");
      // localhost@(lport)
      // -> SSH server (AWS EC2): ssh.host@ssh.port
      // -> Remote server (AWS RDS): ssh.remoteHost@ssh.remotePort
      forwardedPort =
          session.setPortForwardingL(
              3030, sshProperties.remoteHost(), sshProperties.remotePort());
      logger.info("port forwarding end");
    } catch (Exception e) {
      logger.error("SSH tunneling failed", e);
      this.closeSSH();
      exit(1);
    }
    return forwardedPort;
  }
}
