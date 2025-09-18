package com.sns.marigold.global.config;

import static java.lang.System.exit;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
//@ConfigurationProperties(prefix = "ssh") // application.properties 값 사용
public class SshTunnelingInitializer {

  // ** SSH host **
  @Value("${ssh.host}")
  private String host; // application.properties 의 ssh.host 값과 자동 매핑
  @Value("${ssh.port}")
  private int port;
  @Value("${ssh.user}")
  private String user;
  @Value("${ssh.private_key}")
  private String privateKey;

  // ** Remote host **
  @Value("${ssh.remote_host}")
  private String remoteHost;
  @Value("${ssh.remote_port}")
  private int remotePort;

  private Session session;
  private final Logger logger = LoggerFactory.getLogger(SshTunnelingInitializer.class);

  @PreDestroy
  public void closeSSH() {
    if (session.isConnected()) {
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
          System.out.println("JSch: " + message);
        }
      });

    try {
      logger.info("Ssh tunneling start");
      logger.info("{}@{} -> {}@{}", host, port, remoteHost, remotePort);

      JSch jsch = new JSch();

      if (!new File(privateKey).exists()) {
        throw new IllegalStateException("비공개 키 파일을 찾을 수 없습니다.");
      }
      jsch.addIdentity(privateKey);

      KeyPair kp = KeyPair.load(jsch, privateKey);
      if (kp == null) {
        logger.error("❌ 키 로드 실패");
      }

      logger.info("{}@{}:{}", user, host, port);
      session = jsch.getSession(user, host, port);
      Properties config = new Properties();
      // 최초 SSH 접속 시 서버의 호스트 키 신뢰
      config.put("StrictHostKeyChecking", "no");

      logger.info("setting config : {}", config);
      session.setConfig(config);

      logger.info("connecting ssh session");
      session.connect(10000);

      logger.info("successfully connected");
      logger.info("port forwarding start");
      // localhost@(lport)
      // -> SSH server (AWS EC2): ssh.host@ssh.port
      // -> Remote server (AWS RDS): ssh.remoteHost@ssh.remotePort
      forwardedPort =
        session.setPortForwardingL(3030, remoteHost, remotePort); // lport 0 : auto assigned port
      logger.info("port forwarding end");
    } catch (Exception e) {
      logger.error("SSH Tunneling Error");
      e.printStackTrace();
      this.closeSSH();
      exit(1);
    }
    return forwardedPort;
  }
}
