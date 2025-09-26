package com.sns.marigold.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum Province {
  // Metropolitan City / Province (시/도)

  // Metropolitan City
  SEOUL("서울특별시", SeoulDistrict.values()),
  BUSAN("부산광역시", BusanDistrict.values()),
  DAEGU("대구광역시", DaeguDistrict.values()),
  INCHEON("인천광역시", IncheonDistrict.values()),
  GWANGJU("광주광역시", GwangjuDistrict.values()),
  DAEJEON("대전광역시", DaejeonDistrict.values()),
  ULSAN("울산광역시", UlsanDistrict.values()),
  SEJONG("세종특별자치시", null),
  // Province
  GYENGGI("경기도", GyeonggiDistrict.values()),
  GANGWON("강원도", GangwonDistrict.values()),
  CHUNGBUK("충청북도", ChungbukDistrict.values()),
  CHUNGNAM("충청남도", ChungnamDistrict.values()),
  GYEONGBUK("경상북도", GyeongbukDistrict.values()),
  GYEONGNAM("경상남도", GyeongnamDistrict.values()),
  JEONBUK("전라북도", JeonbukDistrict.values()),
  JEONNAM("전라남도", JeonnamDistrict.values()),
  JEJU("제주특별자치도", JejuDistrict.values());


  private final String name;
  private final Enum<?>[] children;

  Province(String name, Enum<?>[] children) {
    this.name = name;
    this.children = children;
  }

  // Metrolpolitan City

  @Getter
  @RequiredArgsConstructor
  public enum SeoulDistrict {
    JONGNO("종로구"),
    JUNG("중구"),
    YONGSAN("용산구"),
    SEONGDONG("성동구"),
    GWANGJIN("광진구"),
    DONGDAEMUN("동대문구"),
    JUNGRANG("중랑구"),
    SEONGBUK("성북구"),
    GANGBUK("강북구"),
    DOBONG("도봉구"),
    NOWON("노원구"),
    EUNPYEONG("은평구"),
    SEOMUN("서대문구"),
    MAPO("마포구"),
    YANGCHEON("양천구"),
    GANGSEO("강서구"),
    GURO("구로구"),
    GEUMCHEON("금천구"),
    YEONGDEUNGPO("영등포구"),
    DONGJAK("동작구"),
    GWANAK("관악구"),
    SEOCHO("서초구"),
    GANGNAM("강남구"),
    SONGPA("송파구"),
    GANGDONG("강동구");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum BusanDistrict {
    JUNG("중구"),
    SEO("서구"),
    DONG("동구"),
    YEONGDO("영도구"),
    BUSANJIN("부산진구"),
    DONGRAE("동래구"),
    NAM("남구"),
    BUK("북구"),
    HAEUNDAE("해운대구"),
    SAHA("사하구"),
    GEUMJEONG("금정구"),
    GANGSEO("강서구"),
    YEONJE("연제구"),
    SUYEONG("수영구"),
    SASANG("사상구"),
    GIJANG("기장군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum DaeguDistrict {
    JUNG("중구"),
    DONG("동구"),
    SEO("서구"),
    NAM("남구"),
    BUK("북구"),
    SUSEONG("수성구"),
    DALSEO("달서구"),
    DALSEONG("달성군"),
    GUNWI("군위군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum IncheonDistrict {
    JUNG("중구"),
    DONG("동구"),
    MICHUHOL("미추홀구"),
    YUNSU("연수구"),
    NAMDONG("남동구"),
    BUPYEONG("부평구"),
    GYAEYANG("계양구"),
    SEO("서구"),
    GANGHWA("강화군"),
    ONGJIN("옹진군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum GwangjuDistrict {
    DONG("동구"),
    SEO("서구"),
    NAM("남구"),
    BUK("북구"),
    GWANGSAN("광산구");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum DaejeonDistrict {
    DONG("동구"),
    JUNG("중구"),
    SEO("서구"),
    YUSEONG("유성구"),
    DAEDEOK("대덕구");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum UlsanDistrict {
    JUNG("중구"),
    NAM("남구"),
    DONG("동구"),
    BUK("북구"),
    ULJU("울주군");

    private final String name;
  }

  // Province

  @Getter
  @RequiredArgsConstructor
  public enum GyeonggiDistrict {
    SUWON("수원시"),
    YONGIN("용인시"),
    GOYANG("고양시"),
    HWASEONG("화성시"),
    SEONGNAM("성남시"),
    BUCHEON("부천시"),
    NAMYANGJU("남양주시"),
    ANSAN("안산시"),
    PYEONGTAEK("평택시"),
    ANYANG("안양시"),

    SIHEUNG("시흥시"),
    PAJU("파주시"),
    GIMPO("김포시"),
    UIJEONGBU("의정부시"),
    GWANGJU("광주시"),
    HANAM("하남시"),
    YANGJU("양주시"),
    GWANGMYEONG("광명시"),
    GUNPO("군포시"),
    OSAN("오산시"),

    ICHEON("이천시"),
    ANSEONG("안성시"),
    GURI("구리시"),
    POCHEON("포천시"),
    UIWANG("의왕시"),
    YANGPYEONG("양평군"),
    YEOJU("여주시"),
    DONGDUCHEON("동두천시"),
    GWACHEON("과천시"),
    GAPYEONG("가평군"),

    YEONCHEON("연천군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum GangwonDistrict {
    CHUNCHEON("춘천시"),
    WONJU("원주시"),
    GANGNEUNG("강릉시"),
    DONGHAE("동해시"),
    TAEBAEK("태백시"),
    SOKCHO("속초시"),
    SAMCHEOCK("삼척시"),
    HONGCHEON("흥천군"),
    HOENGSEONG("횡성군"),
    YEONGWOL("영월군"),

    PYEONGCHANG("평창군"),
    JEONGSEON("정선군"),
    CHEORWON("철원군"),
    HWACHEON("화천군"),
    YANGGU("양구군"),
    INJE("인제군"),
    GOSEONG("고성군"),
    YANGYANG("양양군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum ChungbukDistrict {
    CHEONGJU("청주시"),
    CHUNGJU("충주시"),
    JECHEON("제천시"),
    BOEUN("보은군"),
    OKCHEON("옥천군"),
    YEONGDONG("영동군"),
    JEUNGPYEONG("증평군"),
    JINCHEON("진천군"),
    GOESAN("괴산군"),
    EUMSEONG("음성군"),

    DANYANG("단양군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum ChungnamDistrict {
    CHEONAN("천안시"),
    GONGJU("공주시"),
    BORYEONG("보령시"),
    ASAN("아산시"),
    SEOSAN("서산시"),
    NONSAN("논산시"),
    GYERYONG("계룡시"),
    DANGJIN("당진시"),
    GEUMSAN("금산군"),
    BUYEO("부여군"),

    SEOCHEON("서천군"),
    CHEONGYANG("청양군"),
    HONGSEONG("홍성군"),
    YESAN("예산군"),
    TAEAN("태안군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum GyeongbukDistrict {
    POHANG("포항시"),
    GYEONGJU("경주시"),
    GIMCHEON("김천시"),
    ANDONG("안동시"),
    GUMI("구미시"),
    YEONGJU("영주시"),
    YEONGCHEON("영천시"),
    SANGJU("상주시"),
    MUNGEONG("문경시"),
    GYEONGSAN("경산시"),

    UISEONG("의성군"),
    CHEONGSONG("청송군"),
    YEONGYANG("영양군"),
    YEONGDEOK("영덕군"),
    CHEONGDO("청도군"),
    GORYEONG("고령군"),
    SEONGJU("성주군"),
    CHILGOK("칠곡군"),
    YEOCHEON("예천군"),
    BONGHWA("봉화군"),

    ULJIN("울진군"),
    ULLUNG("울릉군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum GyeongnamDistrict {
    CHANGWON("창원시"),
    JINJU("진주시"),
    TONGYEONG("통영시"),
    SACHEON("사천시"),
    GIMHAE("김해시"),
    MILYANG("밀양시"),
    GEOJE("거제시"),
    YANGSAN("양산시"),
    UIRYEONG("의령군"),
    HAMAN("함안군"),

    CHANGNUNG("창녕군"),
    GOSEONG("고성군"),
    NAMHAE("남해군"),
    HADONG("하동군"),
    SANCHEONG("산청군"),
    HAMYANG("함양군"),
    GEOCHEANG("거창군"),
    HAPCHEONG("합천군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum JeonbukDistrict {
    JEONJU("전주시"),
    GUNSAN("군산시"),
    IKSAN("익산시"),
    JEONGEUP("정읍시"),
    NAMWON("남원시"),
    GIMJE("김제시"),
    WANJU("완주군"),
    JINAN("진안군"),
    MUJU("무주군"),
    JANGSU("장수군"),

    IMSIL("임실군"),
    SUNCHANG("순창군"),
    GOCHANG("고창군"),
    BUAN("부안군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum JeonnamDistrict {
    MOKPO("목포시"),
    YEOSU("여수시"),
    SUNCHEON("순천시"),
    NAJU("나주시"),
    GWANGYANG("광양시"),
    DAMYANG("담양군"),
    GOKSEONG("곡성군"),
    GURYE("구례군"),
    GOHEUNG("고흥군"),
    BOSEONG("보성군"),

    HWASUN("화순군"),
    JANGHEUNG("장흥군"),
    GANGJIN("강진군"),
    HAENAM("해남군"),
    YEONGAM("영암군"),
    MUAN("무안군"),
    HAMPYEONG("함평군"),
    YEONGGWANG("영광군"),
    JANGSEONG("장성군"),
    WANDO("완도군"),

    JINDO("진도군"),
    SINAN("신안군");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum JejuDistrict {
    JEJU("제주시"),
    SEOGWIPO("서귀포시");

    private final String name;
  }
}

