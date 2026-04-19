package com.sns.marigold.chat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.support.BaseIntegrationTest;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import io.hypersistence.tsid.TSID;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ChatIntegrationTest extends BaseIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private JwtManager jwtManager;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private AdoptionPostRepository adoptionPostRepository;
  @Autowired
  private ChatRoomRepository chatRoomRepository;
  @Autowired
  private RoomParticipantRepository participantRepository;
  @Autowired
  private ObjectMapper objectMapper;

  private WebSocketStompClient stompClient;
  private String accessToken;
  private User user1;
  private User user2;
  private AdoptionPost post;
  private ChatRoom chatRoom;

  @BeforeEach
  void setup() {
    stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setObjectMapper(Objects.requireNonNull(objectMapper));
    stompClient.setMessageConverter(converter);

    user1 = userRepository
        .save(Objects.requireNonNull(User.builder().nickname("user1").role(Role.ROLE_PERSON).build()));
    user2 = userRepository
        .save(Objects.requireNonNull(User.builder().nickname("user2").role(Role.ROLE_PERSON).build()));

    post = adoptionPostRepository.save(
        Objects.requireNonNull(AdoptionPost.builder()
            .writer(user1)
            .title("Adoption Post")
            .species(Species.DOG)
            .sex(Sex.MALE)
            .age(1)
            .weight(2.0)
            .area("Seoul")
            .neutering(Neutering.NO)
            .features("Features text string")
            .build()));

    chatRoom = chatRoomRepository.save(Objects.requireNonNull(ChatRoom.create(user1, user2, post)));
    participantRepository
        .save(Objects.requireNonNull(RoomParticipant.builder().chatRoom(chatRoom).user(user1).build()));
    participantRepository
        .save(Objects.requireNonNull(RoomParticipant.builder().chatRoom(chatRoom).user(user2).build()));

    CustomPrincipal principal = new CustomPrincipal(
        user1.getId(),
        Collections.singletonList(new SimpleGrantedAuthority(user1.getRole().name())),
        Map.of(),
        AuthStatus.LOGIN_SUCCESS);
    accessToken = jwtManager.createAccessToken(principal);
  }

  @Test
  @DisplayName("WebSocket을 통해 메시지를 전송하고 구독한 방에서 메시지를 수신한다")
  void sendAndReceiveMessage() throws InterruptedException, ExecutionException, TimeoutException {
    WebSocketStompClient client = Objects.requireNonNull(stompClient);
    String token = Objects.requireNonNull(accessToken);
    ChatRoom room = Objects.requireNonNull(chatRoom);
    User sender = Objects.requireNonNull(user1);

    CompletableFuture<ChatMessageDto> resultKeeper = new CompletableFuture<>();

    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + token);

    // WebSocket Config에서 SockJS를 사용 중
    // 따라서 순수 웹소켓 엔드포인트를 사용하려면 접속하는 경로인 /websocket을 URL 뒤에 추가해야 한다.
    String url = Objects.requireNonNull(String.format("ws://localhost:%d/ws/websocket", port));
    StompSession session = client
        .connectAsync(
            url,
            new WebSocketHttpHeaders(),
            connectHeaders,
            new StompSessionHandlerAdapter() {
              @Override
              public void handleException(
                  @NonNull StompSession session,
                  @Nullable StompCommand command,
                  @NonNull StompHeaders headers,
                  @NonNull byte[] payload,
                  @NonNull Throwable exception) {
                log.error("STOMP Exception: {}", exception.getMessage(), exception);
              }

              @Override
              public void handleTransportError(
                  @NonNull StompSession session, @NonNull Throwable exception) {
                log.error("STOMP Transport Error: {}", exception.getMessage(), exception);
              }
            })
        .get(5, TimeUnit.SECONDS);

    String roomIdStr = TSID.from(room.getId()).toString();

    // Subscribe
    StompHeaders subscribeHeaders = new StompHeaders();
    subscribeHeaders.setDestination("/sub/chat/room/" + roomIdStr);

    session.subscribe(
        subscribeHeaders,
        new StompFrameHandler() {
          @Override
          @NonNull
          public Type getPayloadType(@NonNull StompHeaders headers) {
            return ChatMessageDto.class;
          }

          @Override
          public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
            if (payload instanceof ChatMessageDto messageDto) {
              resultKeeper.complete(messageDto);
            }
          }
        });

    // Send Message
    ChatMessageDto messageDto = Objects.requireNonNull(ChatMessageDto.builder()
        .roomId(room.getId())
        .senderId(sender.getId())
        .message("Hello WebSocket")
        .build());

    StompHeaders sendHeaders = new StompHeaders();
    sendHeaders.setDestination("/pub/chat/message");

    session.send(sendHeaders, messageDto);

    ChatMessageDto receivedMessage = resultKeeper.get(5, TimeUnit.SECONDS);

    assertThat(receivedMessage.getMessage()).isEqualTo("Hello WebSocket");
    assertThat(receivedMessage.getSenderId()).isEqualTo(sender.getId());
  }
}
