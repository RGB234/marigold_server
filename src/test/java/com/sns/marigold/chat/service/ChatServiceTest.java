package com.sns.marigold.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.chat.enums.ChatRoomStatus;
import com.sns.marigold.chat.enums.ChatRoomType;
import com.sns.marigold.chat.repository.ChatMessageRepository;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  @Mock private ChatRoomRepository chatRoomRepository;

  @Mock private ChatMessageRepository chatMessageRepository;

  @Mock private UserRepository userRepository;

  @Mock private AdoptionPostRepository adoptionPostRepository;

  @Mock private RoomParticipantRepository participantRepository;

  @Mock private S3Service storageService;

  @InjectMocks private ChatService chatService;

  private User user1;
  private User user2;
  private AdoptionPost post;
  private ChatRoom chatRoom;

  @BeforeEach
  void setUp() {
    user1 = mock(User.class);
    lenient().when(user1.getId()).thenReturn(1L);
    lenient().when(user1.getDisplayNickname()).thenReturn("User1");

    user2 = mock(User.class);
    lenient().when(user2.getId()).thenReturn(2L);
    lenient().when(user2.getDisplayNickname()).thenReturn("User2");

    post = mock(AdoptionPost.class);
    lenient().when(post.getId()).thenReturn(10L);
    lenient().when(post.getTitle()).thenReturn("Test Post");
    lenient().when(post.getWriter()).thenReturn(user1);
    lenient().when(post.getDeletedAt()).thenReturn(null);

    chatRoom = mock(ChatRoom.class);
    lenient().when(chatRoom.getId()).thenReturn(100L);
    lenient().when(chatRoom.getAdoptionPost()).thenReturn(post);
    lenient().when(chatRoom.getStatus()).thenReturn(ChatRoomStatus.ACTIVE);
    lenient().when(chatRoom.getCreatedAt()).thenReturn(LocalDateTime.now());
  }

  private RoomParticipant createParticipant(ChatRoom room, User user) {
    RoomParticipant participant = mock(RoomParticipant.class);
    lenient().when(participant.getChatRoom()).thenReturn(room);
    lenient().when(participant.getUser()).thenReturn(user);
    return participant;
  }

  @Test
  @DisplayName("존재하는 채팅방 ID로 조회 시 정상 반환한다.")
  void getChatRoom_Success() {
    // given
    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));

    RoomParticipant p1 = createParticipant(chatRoom, user1);
    RoomParticipant p2 = createParticipant(chatRoom, user2);
    given(participantRepository.findAllByChatRoom(chatRoom)).willReturn(List.of(p1, p2));

    // when
    ChatRoomDto result = chatService.getChatRoom(100L);

    // then
    assertThat(result.getId()).isEqualTo(100L);
    assertThat(result.getPostId()).isEqualTo(10L);
    assertThat(result.getPostTitle()).isEqualTo("Test Post");
    assertThat(result.getUser1Id()).isEqualTo(1L);
    assertThat(result.getUser2Id()).isEqualTo(2L);
  }

  @Test
  @DisplayName("존재하지 않는 채팅방 ID로 조회 시 예외가 발생한다.")
  void getChatRoom_NotFound() {
    // given
    given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatService.getChatRoom(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Chat room not found: 999");
  }

  @Test
  @DisplayName("삭제된 게시글의 채팅방 조회 시 제목이 '삭제된 게시글입니다'로 표시된다.")
  void getChatRoom_PostDeleted() {
    // given
    given(post.getDeletedAt()).willReturn(LocalDateTime.now());
    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));

    RoomParticipant p1 = createParticipant(chatRoom, user1);
    RoomParticipant p2 = createParticipant(chatRoom, user2);
    given(participantRepository.findAllByChatRoom(chatRoom)).willReturn(List.of(p1, p2));

    // when
    ChatRoomDto result = chatService.getChatRoom(100L);

    // then
    assertThat(result.getPostTitle()).isEqualTo("삭제된 게시글입니다");
  }

  @Test
  @DisplayName("참가자가 2명이 아닌 채팅방 조회 시 예외가 발생한다.")
  void getChatRoom_InvalidParticipantCount() {
    // given
    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));

    RoomParticipant p1 = createParticipant(chatRoom, user1);
    given(participantRepository.findAllByChatRoom(chatRoom)).willReturn(List.of(p1)); // 1명만 참가

    // when & then
    assertThatThrownBy(() -> chatService.getChatRoom(100L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("1:1 채팅방은 참가자 2명이어야 합니다");
  }

  @Test
  @DisplayName("새로운 1:1 채팅방을 생성하고 반환한다.")
  void getOrCreateChatRoom_CreateNew() {
    // given
    given(userRepository.getReferenceById(1L)).willReturn(user1);
    given(userRepository.getReferenceById(2L)).willReturn(user2);
    given(adoptionPostRepository.getReferenceById(10L)).willReturn(post);

    given(chatRoomRepository.findByUsersAndAdoptionPost(user1, user2, post))
        .willReturn(Optional.empty());

    ChatRoom newRoom = mock(ChatRoom.class);
    lenient().when(newRoom.getId()).thenReturn(100L);
    lenient().when(newRoom.getAdoptionPost()).thenReturn(post);
    lenient().when(newRoom.getStatus()).thenReturn(ChatRoomStatus.ACTIVE);

    given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(newRoom);

    given(participantRepository.findByChatRoomAndUser(newRoom, user1)).willReturn(Optional.empty());
    given(participantRepository.findByChatRoomAndUser(newRoom, user2)).willReturn(Optional.empty());

    RoomParticipant p1 = createParticipant(newRoom, user1);
    RoomParticipant p2 = createParticipant(newRoom, user2);
    given(participantRepository.findAllByChatRoom(newRoom)).willReturn(List.of(p1, p2));

    // when
    ChatRoomDto result = chatService.getOrCreateChatRoom(1L, 2L, 10L);

    // then
    assertThat(result.getId()).isEqualTo(100L);
    verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    verify(participantRepository, times(2)).save(any(RoomParticipant.class)); // 2명 생성
  }

  @Test
  @DisplayName("이미 존재하는 채팅방이 있을 경우 해당 채팅방을 반환한다.")
  void getOrCreateChatRoom_ReturnExisting() {
    // given
    given(userRepository.getReferenceById(1L)).willReturn(user1);
    given(userRepository.getReferenceById(2L)).willReturn(user2);
    given(adoptionPostRepository.getReferenceById(10L)).willReturn(post);

    given(chatRoomRepository.findByUsersAndAdoptionPost(user1, user2, post))
        .willReturn(Optional.of(chatRoom));

    RoomParticipant p1 = mock(RoomParticipant.class);
    RoomParticipant p2 = mock(RoomParticipant.class);
    given(p1.getUser()).willReturn(user1);
    given(p2.getUser()).willReturn(user2);

    given(participantRepository.findByChatRoomAndUser(chatRoom, user1)).willReturn(Optional.of(p1));
    given(participantRepository.findByChatRoomAndUser(chatRoom, user2)).willReturn(Optional.of(p2));

    given(participantRepository.findAllByChatRoom(chatRoom)).willReturn(List.of(p1, p2));

    // when
    ChatRoomDto result = chatService.getOrCreateChatRoom(1L, 2L, 10L);

    // then
    assertThat(result.getId()).isEqualTo(100L);
    verify(chatRoomRepository, times(0)).save(any(ChatRoom.class));
    verify(p1, times(1)).reJoin(); // 재입장 메서드 호출 검증
    verify(p2, times(1)).reJoin();
  }

  @Test
  @DisplayName("유저의 채팅방 목록을 조회한다. (모든 방)")
  void getUserRooms_All() {
    // given
    given(userRepository.findById(1L)).willReturn(Optional.of(user1));
    Pageable pageable = PageRequest.of(0, 10);

    given(chatRoomRepository.findAllActiveByUser(user1, pageable))
        .willReturn(new PageImpl<>(List.of(chatRoom)));

    RoomParticipant p1 = createParticipant(chatRoom, user1);
    RoomParticipant p2 = createParticipant(chatRoom, user2);
    given(participantRepository.findAllByChatRoom(chatRoom)).willReturn(List.of(p1, p2));

    // when
    Page<ChatRoomDto> result = chatService.getUserRooms(1L, ChatRoomType.ALL, pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
  }

  @Test
  @DisplayName("유저의 채팅방 목록을 작성자(writer)로서 조회한다.")
  void getUserRooms_Writer() {
    // given
    given(userRepository.findById(1L)).willReturn(Optional.of(user1));
    Pageable pageable = PageRequest.of(0, 10);

    given(chatRoomRepository.findAllActiveByUserAsWriter(user1, pageable))
        .willReturn(new PageImpl<>(List.of(chatRoom)));

    RoomParticipant p1 = createParticipant(chatRoom, user1);
    RoomParticipant p2 = createParticipant(chatRoom, user2);
    given(participantRepository.findAllByChatRoom(chatRoom)).willReturn(List.of(p1, p2));

    // when
    Page<ChatRoomDto> result = chatService.getUserRooms(1L, ChatRoomType.WRITER, pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    verify(chatRoomRepository, times(1)).findAllActiveByUserAsWriter(user1, pageable);
  }

  @Test
  @DisplayName("유저의 채팅방 목록을 문의자(inquirer)로서 조회한다.")
  void getUserRooms_Inquirer() {
    // given
    given(userRepository.findById(1L)).willReturn(Optional.of(user1));
    Pageable pageable = PageRequest.of(0, 10);

    given(chatRoomRepository.findAllActiveByUserAsInquirer(user1, pageable))
        .willReturn(new PageImpl<>(List.of(chatRoom)));

    RoomParticipant p1 = createParticipant(chatRoom, user1);
    RoomParticipant p2 = createParticipant(chatRoom, user2);
    given(participantRepository.findAllByChatRoom(chatRoom)).willReturn(List.of(p1, p2));

    // when
    Page<ChatRoomDto> result = chatService.getUserRooms(1L, ChatRoomType.INQUIRER, pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    verify(chatRoomRepository, times(1)).findAllActiveByUserAsInquirer(user1, pageable);
  }

  @Test
  @DisplayName("채팅방의 메시지 목록을 시간순으로 조회한다.")
  void getRoomMessages_Success() {
    // given
    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));

    ChatMessage msg = mock(ChatMessage.class);
    given(msg.getChatRoom()).willReturn(chatRoom);
    given(msg.getSender()).willReturn(user1);
    given(msg.getMessage()).willReturn("Hello");
    given(msg.getCreatedAt()).willReturn(LocalDateTime.now());

    given(chatMessageRepository.findAllByChatRoomOrderByCreatedAtAsc(chatRoom))
        .willReturn(List.of(msg));

    // when
    List<ChatMessageDto> result = chatService.getRoomMessages(100L);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMessage()).isEqualTo("Hello");
    assertThat(result.get(0).getSenderId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("메시지를 정상적으로 저장한다.")
  void saveMessage_Success() {
    // given
    ChatMessageDto reqDto =
        ChatMessageDto.builder().roomId(100L).senderId(1L).message("Test Msg").build();

    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
    given(userRepository.findById(1L)).willReturn(Optional.of(user1));

    RoomParticipant p1 = mock(RoomParticipant.class);
    RoomParticipant p2 = mock(RoomParticipant.class);

    given(p1.getUser()).willReturn(user1);
    given(p2.getUser()).willReturn(user2);

    given(participantRepository.findAllByChatRoom(chatRoom)).willReturn(List.of(p1, p2));
    given(participantRepository.findByChatRoomAndUser(chatRoom, user1)).willReturn(Optional.of(p1));
    given(participantRepository.findByChatRoomAndUser(chatRoom, user2)).willReturn(Optional.of(p2));

    // when
    ChatMessageDto result = chatService.saveMessage(reqDto);

    // then
    assertThat(result.getMessage()).isEqualTo("Test Msg");
    verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    verify(p1, times(1)).reJoin(); // 방 활성화 검증
    verify(p2, times(1)).reJoin();
  }

  @Test
  @DisplayName("종료된 채팅방에 메시지 전송 시도 시 예외가 발생한다.")
  void saveMessage_ClosedRoom() {
    // given
    ChatMessageDto reqDto =
        ChatMessageDto.builder().roomId(100L).senderId(1L).message("Test Msg").build();

    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getStatus()).willReturn(ChatRoomStatus.CLOSED);

    // when & then
    assertThatThrownBy(() -> chatService.saveMessage(reqDto))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("종료된 채팅방에는 메시지를 보낼 수 없습니다");
  }

  @Test
  @DisplayName("채팅방에서 성공적으로 나간다(RoomParticipant leave 처리).")
  void leaveRoom_Success() {
    // given
    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
    given(userRepository.findById(1L)).willReturn(Optional.of(user1));

    RoomParticipant p1 = mock(RoomParticipant.class);
    given(participantRepository.findByChatRoomAndUser(chatRoom, user1)).willReturn(Optional.of(p1));

    // when
    chatService.leaveRoom(100L, 1L);

    // then
    verify(p1, times(1)).leave();
  }
}
