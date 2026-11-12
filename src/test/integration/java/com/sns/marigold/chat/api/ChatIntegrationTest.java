package com.sns.marigold.chat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.service.AuthSessionService;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatMessageRequestDto;
import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.chat.enums.ChatRoomStatus;
import com.sns.marigold.chat.repository.ChatMessageRepository;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.global.tsid.TsidCodec;
import com.sns.marigold.support.BaseIntegrationTest;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ChatIntegrationTest extends BaseIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private AuthSessionService authSessionService;
  @Autowired private UserRepository userRepository;
  @Autowired private AdoptionPostRepository adoptionPostRepository;
  @Autowired private ChatRoomRepository chatRoomRepository;
  @Autowired private ChatMessageRepository chatMessageRepository;
  @Autowired private RoomParticipantRepository participantRepository;
  @Autowired private ChatService chatService;
  @Autowired private ObjectMapper objectMapper;

  private WebSocketStompClient stompClient;
  private StompSession stompSession;
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

    user1 =
        userRepository.save(
            Objects.requireNonNull(
                User.builder().nickname("user1").role(Role.ROLE_PERSON).build()));
    user2 =
        userRepository.save(
            Objects.requireNonNull(
                User.builder().nickname("user2").role(Role.ROLE_PERSON).build()));

    post =
        adoptionPostRepository.save(
            Objects.requireNonNull(
                AdoptionPost.builder()
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

    chatRoom = chatRoomRepository.save(Objects.requireNonNull(ChatRoom.create(post)));
    participantRepository.save(
        Objects.requireNonNull(RoomParticipant.builder().chatRoom(chatRoom).user(user1).build()));
    participantRepository.save(
        Objects.requireNonNull(RoomParticipant.builder().chatRoom(chatRoom).user(user2).build()));

    CustomPrincipal principal =
        new CustomPrincipal(
            user1.getId(),
            Collections.singletonList(new SimpleGrantedAuthority(user1.getRole().name())),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);
    accessToken = authSessionService.start(principal).accessToken();
  }

  @AfterEach
  void closeStompConnection() {
    if (stompSession != null && stompSession.isConnected()) {
      stompSession.disconnect();
    }
    if (stompClient != null) {
      stompClient.stop();
    }
  }

  @Test
  @DisplayName("채팅방 종료 후 별도 트랜잭션에서 CLOSED 상태가 조회된다.")
  void closeChatRoom_PersistsClosedStatus() {
    chatService.closeChatRoom(chatRoom.getId(), user1.getId());

    ChatRoom reloaded = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
  }

  @Test
  @DisplayName("상대가 메시지를 보내면 나간 사용자는 다시 참여해 답장할 수 있다.")
  void sendingMessageRejoinsLeftParticipant() {
    RoomParticipant participant =
        participantRepository.findByChatRoomAndUser(chatRoom, user1).orElseThrow();
    participant.leave();
    participantRepository.saveAndFlush(participant);

    chatService.saveMessage(
        ChatMessageRequestDto.builder().roomId(chatRoom.getId()).message("still there?").build(),
        user2.getId());

    assertThat(
            participantRepository.existsByChatRoom_IdAndUser_IdAndLeavedAtIsNull(
                chatRoom.getId(), user1.getId()))
        .isTrue();
    chatService.saveMessage(
        ChatMessageRequestDto.builder().roomId(chatRoom.getId()).message("yes").build(),
        user1.getId());
    assertThat(chatService.getRoomMessages(chatRoom.getId(), user1.getId()))
        .extracting(ChatMessageDto::getMessage)
        .containsExactly("still there?", "yes");
  }

  @Test
  @DisplayName("나간 사용자는 이전 대화를 조회할 수 없고 재참여하면 전체 이력을 조회할 수 있다.")
  void leavingBlocksHistoryUntilRejoining() {
    chatMessageRepository.saveAndFlush(
        Objects.requireNonNull(
            ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(user2)
                .message("before rejoin")
                .build()));
    RoomParticipant participant =
        participantRepository.findByChatRoomAndUser(chatRoom, user1).orElseThrow();
    participant.leave();
    participantRepository.saveAndFlush(participant);

    assertThat(
            participantRepository.existsByChatRoom_IdAndUser_IdAndLeavedAtIsNull(
                chatRoom.getId(), user1.getId()))
        .isFalse();
    assertThatThrownBy(() -> chatService.getRoomMessages(chatRoom.getId(), user1.getId()))
        .isInstanceOf(AuthException.class);
    assertThatThrownBy(
            () ->
                chatService.saveMessage(
                    ChatMessageRequestDto.builder()
                        .roomId(chatRoom.getId())
                        .message("blocked")
                        .build(),
                    user1.getId()))
        .isInstanceOf(AuthException.class);

    chatService.getOrCreateChatRoom(user1.getId(), user2.getId(), post.getId());

    assertThat(
            participantRepository.existsByChatRoom_IdAndUser_IdAndLeavedAtIsNull(
                chatRoom.getId(), user1.getId()))
        .isTrue();
    assertThat(chatService.getRoomMessages(chatRoom.getId(), user1.getId()))
        .extracting(ChatMessageDto::getMessage)
        .containsExactly("before rejoin");
  }

  @Test
  @DisplayName("WebSocket 메시지는 인증된 사용자를 발신자로 저장한다")
  void sendAndReceiveMessage() throws InterruptedException, ExecutionException, TimeoutException {
    ChatRoom room = Objects.requireNonNull(chatRoom);
    User sender = Objects.requireNonNull(user1);

    CompletableFuture<ChatMessageDto> resultKeeper = new CompletableFuture<>();
    StompSession session = connect();

    String roomIdStr = TsidCodec.encode(room.getId());

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
    Map<String, String> messageDto =
        Map.of(
            "roomId",
            roomIdStr,
            "senderId",
            TsidCodec.encode(user2.getId()),
            "message",
            "Hello WebSocket");

    StompHeaders sendHeaders = new StompHeaders();
    sendHeaders.setDestination("/pub/chat/message");

    session.send(sendHeaders, messageDto);

    ChatMessageDto receivedMessage = resultKeeper.get(5, TimeUnit.SECONDS);

    assertThat(receivedMessage.getMessage()).isEqualTo("Hello WebSocket");
    assertThat(receivedMessage.getSenderId()).isEqualTo(sender.getId());
  }

  @Test
  @DisplayName("잘못된 WebSocket 메시지는 세션 오류 queue로 응답하고 연결을 유지한다")
  void invalidMessageReturnsRecoverableError()
      throws InterruptedException, ExecutionException, TimeoutException {
    ChatRoom room = Objects.requireNonNull(chatRoom);
    StompSession session = connect();
    CompletableFuture<JsonNode> errorKeeper = new CompletableFuture<>();
    CompletableFuture<ChatMessageDto> messageKeeper = new CompletableFuture<>();

    session.subscribe(
        "/user/queue/errors",
        new StompFrameHandler() {
          @Override
          @NonNull
          public Type getPayloadType(@NonNull StompHeaders headers) {
            return JsonNode.class;
          }

          @Override
          public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
            if (payload instanceof JsonNode error) {
              errorKeeper.complete(error);
            }
          }
        });

    session.subscribe(
        "/sub/chat/room/" + TsidCodec.encode(room.getId()),
        new StompFrameHandler() {
          @Override
          @NonNull
          public Type getPayloadType(@NonNull StompHeaders headers) {
            return ChatMessageDto.class;
          }

          @Override
          public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
            if (payload instanceof ChatMessageDto messageDto
                && "valid after error".equals(messageDto.getMessage())) {
              messageKeeper.complete(messageDto);
            }
          }
        });

    session.send(
        "/pub/chat/message",
        ChatMessageRequestDto.builder().roomId(room.getId()).message(" ").build());

    JsonNode error = errorKeeper.get(5, TimeUnit.SECONDS);
    assertThat(error.get("errorCode").asText()).isEqualTo("INVALID_INPUT_VALUE");
    assertThat(error.get("fatal").asBoolean()).isFalse();
    assertThat(error.get("errors").get(0).get("field").asText()).isEqualTo("message");
    assertThat(session.isConnected()).isTrue();

    session.send(
        "/pub/chat/message",
        ChatMessageRequestDto.builder().roomId(room.getId()).message("valid after error").build());

    assertThat(messageKeeper.get(5, TimeUnit.SECONDS).getMessage()).isEqualTo("valid after error");
  }

  @Test
  @DisplayName("인증 세션이 폐기되면 그 세션의 WebSocket 연결을 끊고 같은 token으로 다시 연결할 수 없다")
  void revokedAuthSessionClosesConnection() throws Exception {
    StompSession session = connect();
    assertThat(session.isConnected()).isTrue();

    authSessionService.endAll(user1.getId());

    long deadline = System.currentTimeMillis() + 5_000;
    while (session.isConnected() && System.currentTimeMillis() < deadline) {
      Thread.sleep(50);
    }
    assertThat(session.isConnected()).isFalse();
    assertThatThrownBy(this::connect)
        .isInstanceOfAny(ExecutionException.class, TimeoutException.class);
  }

  private StompSession connect() throws InterruptedException, ExecutionException, TimeoutException {
    WebSocketStompClient client = Objects.requireNonNull(stompClient);
    String token = Objects.requireNonNull(accessToken);
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + token);

    // WebSocket Config에서 SockJS를 사용 중이므로 순수 WebSocket 경로에는 /websocket을 붙인다.
    String url = Objects.requireNonNull(String.format("ws://localhost:%d/ws/websocket", port));
    stompSession =
        client
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
    return stompSession;
  }
}
