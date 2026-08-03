# API 공통 규약

DeunDeun 서버(Java 21 / Spring Boot / MyBatis / MariaDB)와
안드로이드 앱(Java / XML View / Retrofit) 사이의 공통 계약.

> 레포가 분리되어 있으므로 이 문서가 두 프로젝트를 잇는 유일한 계약이다.
> 백엔드 레포가 생기면 이 문서는 그쪽으로 옮기고, 앱 레포에서는 링크만 유지한다.

---

## 1. 기본 형식

| 항목 | 규칙 |
|---|---|
| Base URL | `https://api.example.com/v1` (도메인 확정 후 수정) |
| 경로 버저닝 | `/v1/...` — 앱은 구버전 사용자가 남으므로 필수 |
| 인코딩 | UTF-8 고정 |
| Content-Type | `application/json` |
| 경로 표기 | 소문자 + 하이픈, 복수형 명사 (`/users`, `/study-groups`) |
| JSON 키 | camelCase — DB 컬럼이 snake_case여도 응답은 camelCase |
| 날짜/시간 | ISO-8601 UTC (`2026-08-03T12:34:56Z`) |
| 금액/수량 | 정수 (원 단위). 부동소수점 사용 금지 |

### MyBatis 관련 주의

DB 컬럼은 snake_case, 응답 JSON은 camelCase다. 둘을 자동으로 잇는다:

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
```

단, **DB 스키마를 그대로 응답으로 내보내지 않는다.** Mapper가 반환하는 도메인 객체와
컨트롤러가 반환하는 Response DTO를 분리한다. MyBatis는 JPA와 달리 엔티티 개념이 약해서
방치하면 테이블 구조가 그대로 API에 노출되고, 이후 스키마 변경이 곧 API 변경이 된다.

---

## 2. 응답 봉투 (Envelope)

성공/실패 판정을 앱에서 한 곳으로 모으기 위해 모든 응답을 동일한 형태로 감싼다.

### 성공

```json
{
  "success": true,
  "data": { }
}
```

목록은 `data`를 배열이 아니라 객체로 감싼다. 나중에 페이징 정보를 추가할 때
응답 구조가 깨지지 않게 하기 위함이다.

```json
{
  "success": true,
  "data": {
    "items": [],
    "nextCursor": "eyJpZCI6MTIzfQ==",
    "hasNext": true
  }
}
```

### 실패

```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다.",
    "fields": {
      "email": "이메일 형식이 올바르지 않습니다."
    }
  }
}
```

- `code` — 앱이 **분기에 사용하는 값**. 대문자 스네이크. 절대 재사용/변경하지 않는다.
- `message` — 사용자에게 그대로 노출 가능한 한국어 문장.
- `fields` — 입력값 검증 실패 시에만 존재. 폼 필드별 에러 표시에 사용.

앱은 `message`를 파싱하거나 분기 조건으로 쓰지 않는다. 항상 `code`로 판단한다.

---

## 3. HTTP 상태 코드

| 코드 | 사용 시점 |
|---|---|
| 200 | 조회/수정 성공 |
| 201 | 생성 성공 (`Location` 헤더에 생성된 리소스 경로) |
| 204 | 삭제 성공 (본문 없음) |
| 400 | 요청 형식 오류, 검증 실패 |
| 401 | 토큰 없음/만료/유효하지 않음 → 앱은 재발급 시도 |
| 403 | 인증은 됐으나 권한 없음 → 재발급 시도하지 않음 |
| 404 | 리소스 없음 |
| 409 | 중복 (이메일 중복 등), 상태 충돌 |
| 422 | 형식은 맞으나 비즈니스 규칙 위반 |
| 429 | 요청 과다 |
| 500 | 서버 오류 — `message`에 내부 정보(스택트레이스, SQL) 절대 노출 금지 |

401과 403의 구분이 중요하다. 앱의 토큰 재발급 인터셉터가 이 둘로 동작을 나눈다.

---

## 4. 인증

앱 클라이언트이므로 세션이 아닌 **JWT** 기반.

| 토큰 | 만료 | 저장 위치(앱) |
|---|---|---|
| Access Token | 30분 | 메모리 |
| Refresh Token | 14일 | EncryptedSharedPreferences |

- 전달: `Authorization: Bearer <accessToken>`
- 재발급: `POST /v1/auth/reissue` — Refresh Token으로 Access Token 재발급
- Access Token 만료(401) 시 앱이 자동 재발급 후 원 요청 1회 재시도. 재발급도 실패하면 로그아웃.
- Refresh Token은 재발급 시 함께 교체한다(rotation). 이전 토큰은 즉시 무효화.

인증이 필요 없는 엔드포인트는 스펙 문서에 `🔓 인증 불필요`로 명시한다.

---

## 5. 페이지네이션

**커서 기반**을 기본으로 한다. 앱의 무한 스크롤에서 offset 방식은
새 데이터가 삽입될 때 항목이 중복되거나 누락된다.

```
GET /v1/posts?cursor=eyJpZCI6MTIzfQ==&size=20
```

- `size` 기본 20, 최대 100
- 첫 요청은 `cursor` 생략
- 응답의 `nextCursor`를 그대로 다음 요청에 전달
- `hasNext: false`면 더 이상 요청하지 않음

관리자 화면처럼 "몇 페이지 중 몇 페이지"가 필요한 경우에만 예외적으로
`page`/`size` + `totalCount` 방식을 쓴다.

---

## 6. 에러 코드 목록

전체 목록은 여기서 단일 관리한다. 새 코드 추가 시 이 표에 먼저 등록한다.

### 공통

| 코드 | HTTP | 설명 |
|---|---|---|
| `INVALID_INPUT` | 400 | 요청 값 검증 실패 (`fields` 포함) |
| `UNAUTHORIZED` | 401 | 토큰 없음 또는 유효하지 않음 |
| `TOKEN_EXPIRED` | 401 | Access Token 만료 → 재발급 필요 |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh Token 만료 → 재로그인 필요 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `TOO_MANY_REQUESTS` | 429 | 요청 과다 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

### 도메인별

*(서비스 기능 확정 후 작성)*

---

## 7. 스펙 문서 관리

- 스펙의 소유자는 **백엔드 레포**. 서버가 먼저 바뀌고 앱이 따라간다.
- `springdoc-openapi` 의존성을 넣어 `/swagger-ui.html`에서 실제 구현과 동기화된 문서를 확인한다.
- API 변경 시 앱 팀에 공유해야 하는 것: 추가된 필드는 무시 가능하지만,
  **필드 삭제 / 타입 변경 / 에러 코드 변경**은 구버전 앱을 깨뜨린다. 이 경우 `/v2`로 분기한다.

---

## 8. 로컬 개발 환경

| 항목 | 값 |
|---|---|
| 서버 포트 | 8080 |
| 에뮬레이터에서의 서버 주소 | `http://10.0.2.2:8080` (`localhost` 아님) |
| 실기기 | 같은 Wi-Fi의 PC 사설 IP |
| cleartext(http) 허용 | debug 빌드에만 적용 |

### 앱 네트워크 스택 (Java)

Retrofit은 원래 Java 라이브러리라 Java에서 그대로 쓴다. 응답 봉투(2번 항목) 처리는
`Call<ApiResponse<T>>` 형태로 제네릭 래퍼를 두고, 공통 콜백에서 `success` 분기와
`error.code` 처리를 한 번에 한다.

| 용도 | 선택 |
|---|---|
| HTTP | Retrofit 2 + OkHttp 3 |
| JSON | Gson (Java 친화적, Retrofit 기본 컨버터) |
| 비동기 | Retrofit `enqueue()` 콜백 → ViewModel + LiveData |
| 토큰 재발급 | OkHttp `Authenticator` 또는 `Interceptor`에서 401 처리 |
| 이미지 | Glide |
