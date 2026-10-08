# 전역 코드 점검 결과 (2026-10-08)

`develop` 기준(PR #196 머지 이후)으로 도메인별 전체 코드를 점검한 결과입니다.
위치는 줄 번호가 바뀔 수 있어 클래스·메서드 이름으로 적었습니다.

## 진행 현황

| 구분 | 상태 |
|---|---|
| A. 수정 항목 (A1~A9) | **전부 완료** (`fix/minor-fixes`, 커밋 `aba8d41`) |
| B. 보안 | **B1·B2 완료** / B3 보류 (외부 레이트 리밋으로 대체). B1 배포 후 사용자 전원 1회 재로그인 |
| C. 확인 항목 | **완료** (조치 불필요 2건, 1건은 A9로 이동해 완료) |
| D. 설계 차원 | 미진행 (캐시 저장 API는 현 구조 유지로 결정) |
| E. 사소한 것 | 미진행 (여유 있을 때) |

---

## A. 수정 항목 — 전부 완료

### A1. 트레이닝 퍼즐 삭제 시 다른 팩의 순서까지 당겨짐 (높음) — ✅ 완료
- 위치: `TrainingPuzzleRepository.decreaseIndexesFrom`, 호출부 `TrainingService.deleteTrainingPuzzle`
- 문제: `WHERE training_index > :targetIdx`에 `pack_id` 조건과 `ORDER BY`가 없음 (`increaseIndexesFrom`에는 둘 다 있음)
- 영향: 다른 팩의 순서가 함께 당겨지다 `(pack_id, training_index)` 유니크 제약에 걸려 삭제가 롤백됨. 제약이 없다면 다른 팩 순서가 조용히 꼬임
- 조치: `decreaseIndexesFrom(packId, targetIdx)`로 바꾸고 `AND pack_id = :packId ORDER BY training_index ASC` 추가

### A2. 출석 보상 중복 지급 (높음) — ✅ 완료
- 위치: `NoticeService.getPersonalNotice`, `UserRepository`
- 문제: "오늘 이미 받았는지" 확인이 잠금 없는 조회이고, 그 뒤에 재화를 더함
- 영향: personal notice 요청이 동시에 N개 오면 출석 보상이 N번 지급됨
- 조치: `UserRepository.markFirstAccessToday` 추가. `UPDATE ... SET lastAccessedAt = :now WHERE id = :userId AND lastAccessedAt < CURRENT_DATE` 한 문장으로 처리하고, 1행이 바뀐 경우에만 지급. 기존 `isLastAccessBeforeToday`는 제거

### A3. 랭크 종료 보상 중복 지급 (높음) — ✅ 완료
- 위치: `RankService.endRankGame`, `RankService`의 결과 처리(`/api/rank/game/result`)
- 문제: Redis 세션을 유저 잠금 전에 읽고, 세션 삭제는 커밋 이후에 함. 푼 문제 기록(`LatestRankPuzzle`)은 지우지 않음
- 영향: 종료 버튼 연타나 재시도 시 두 요청이 모두 세션을 보고 `맞힌 수 × rank_reward`를 두 번 지급함. 종료 직후 결과 요청이 세션을 `set`으로 다시 써서 세션이 되살아나는 경로도 있음
- 조치:
  - 유저 잠금을 먼저 잡고 세션을 읽음. 세션 삭제에 성공한 요청만 보상하고, 이미 지워졌으면 `EMPTY_SESSION_DATA`
  - 보상 처리가 롤백되면 세션을 남은 TTL로 복구해 다시 종료할 수 있게 함
  - 결과 처리는 `setIfPresent`로 덮어쓰기만 해서 종료된 세션을 되살리지 않음

### A4. 트렌드 퍼즐이 싫어요 많은 순으로 정렬됨 (중간) — ✅ 완료
- 위치: `ContentService.trendComparator`
- 문제: 두 번째 `.reversed()`가 정렬 전체를 뒤집어 (좋아요 − 싫어요) 오름차순이 됨
- 조치: `.thenComparing(CommunityPuzzle::getView, Comparator.reverseOrder())`로 바꾸고 두 번째 `.reversed()` 제거. 정렬 순서를 검사하는 테스트 추가

### A5. 1번 팩 삭제 시 모든 회원가입 실패 (중간) — ✅ 완료
- 위치: `AccountService.signUp`, `TrainingService.deletePack`
- 문제: 가입 시 1번 팩을 무조건 지급하는데, 1번 팩 삭제를 막지 않음
- 영향: 1번 팩이 지워지면 가입 트랜잭션이 `NO_SUCH_TRAINING_PACK`으로 롤백됨
- 조치: `Pack.STARTER_PACK_ID` 상수 추가. 시작 팩 삭제 시 `CANNOT_DELETE_STARTER_PACK`(400, `P4007`)

### A6. 커뮤니티 퍼즐 업데이트가 다른 값을 덮어씀 (중간) — ✅ 완료
- 위치: `CommunityPuzzle` 엔티티(`increaseViews` 등), `CommunityPuzzleRepository.applyRankResult`
- 문제: 엔티티 변경 시 Hibernate가 모든 컬럼을 다시 씀
- 영향: 퍼즐 조회(조회수 증가)와 랭크 결과(레이팅 변경)가 동시에 일어나면 레이팅·시도 횟수 변경이 이전 값으로 덮어써짐
- 조치: `@DynamicUpdate` 추가. 조회수를 올려도 레이팅이 유지되는지 검사하는 테스트 추가
- 남은 부분: 같은 퍼즐에 좋아요·조회가 동시에 들어올 때 카운트 하나가 빠질 수 있음. 필요하면 카운터를 `SET x = x + 1` 쿼리로 변경

### A7. 입력 오류가 400이 아닌 500으로 나감 (낮음) — ✅ 완료
- 문제
  - `UserController`의 `/api/user/like`, `/api/user/puzzle`: `size` 범위 검증 없음. `size=-1`이면 `LIMIT -1`로 SQL 에러, 큰 값은 무제한 조회
  - `GetTrainingPackRequest.difficulty`: `@NotBlank`만 있어서 잘못된 값이면 `IllegalArgumentException` 발생
  - `GlobalExceptionHandler`: `IllegalArgumentException`이 500. 타입 불일치 에러 메시지에 Java 클래스명이 노출됨
- 조치
  - 두 목록 API는 `GetUserPuzzleRequest`(`id`, `size` 1~100, 생략 시 10)로 받음
  - 팩 조회·생성·수정 요청의 `difficulty`에 `@ValidEnum` 추가
  - `IllegalArgumentException` → 400 `VALIDATION_ERROR`
  - 타입 불일치는 `Invalid value for {파라미터명}`으로 응답

### A8. 커뮤니티 업로드 입력 범위 제한 없음 (낮음) — ✅ 완료
- 위치: `AddCommunityPuzzleRequest`
- 문제: `boardStatus`, `answer` 길이 제한이 없고(컬럼은 1023자) `depth` 범위도 없음
- 영향: 매우 긴 판 문자열이 중복 판정 계산까지 돈 뒤 DB에서 거절됨. 범위 밖 `depth`가 저장되면 목록에서는 안 보이지만 랭크 문제 풀에는 들어감
- 조치: `boardStatus`, `answer`에 `@Size(max = 1023)`, `depth`에 `@Min(1) @Max(225)`

### A9. 사지 않은 팩의 퍼즐이 보이고 풀 수 있음 (중간) — ✅ 완료
- 위치: `TrainingService.getTrainingPuzzleList`, `solveTrainingPuzzle`, `purchaseTrainingPuzzleAnswer`
- 문제: 세 경로 모두 팩 보유 여부(`UserPack`)를 확인하지 않음
- 영향: 잠긴 팩의 퍼즐 목록이 보이고, 풀면 트레이닝 보상을 받음. 나중에 팩을 사도 풀이 수가 맞지 않고, 추천 API가 `NO_USER_PROGRESS_FOR_PACK`을 낼 수 있음
- 조치: 세 경로 모두 팩 보유를 확인하고, 없으면 `PACK_NOT_OWNED`(403, `P4031`)
- 프론트 영향: 잠긴 팩의 퍼즐 목록을 호출하면 403이 나므로 앱 처리 필요

---

## B. 보안 — B1·B2 완료, B3 보류

### B1. 리프레시 토큰을 액세스 토큰처럼 사용 가능 — ✅ 완료
- 위치: `JwtProvider`(액세스·리프레시 토큰 claim 동일), `JwtAuthenticationFilter`, `AuthService.reissueToken`
- 문제
  - 필터가 토큰 종류를 구분하지 않아 14일짜리 리프레시 토큰으로 API 호출 가능
  - 재발급 시 저장된 토큰과 비교하지 않고 유저 기록 존재 여부만 확인. 오래된 리프레시 토큰이나 액세스 토큰으로도 재발급 가능
  - 비밀번호 변경(`AccountService.changePassword`), 탈퇴(`UserService.deleteUser`) 시 리프레시 토큰을 지우지 않음
- 조치 (기기별 세션 방식으로 결정)
  - 토큰에 종류(`typ`: access/refresh), 세션 id(`sid`), 고유 id(`jti`) 추가. 필터는 액세스 토큰만, 재발급은 리프레시 토큰만 받음
  - 로그인마다 새 세션을 만들어 여러 기기 동시 로그인 유지. Redis `refreshSession:{sid}`에 세션별 최신 리프레시 토큰, `refreshSessions:{userId}`에 사용자의 세션 목록 저장 (`RefreshSessionRepository`)
  - 재발급은 그 세션의 최신 토큰일 때만 성공하고, 같은 세션 안에서 토큰을 교체함. 이미 쓴 토큰은 거절
  - 로그아웃은 해당 기기 세션만, 비밀번호 변경은 현재 기기를 뺀 나머지, 비밀번호 재설정·탈퇴는 모든 세션 종료
  - 기존 `RefreshTokenEntity`·`RefreshTokenRedisRepository` 제거. Redis에 남은 예전 데이터는 TTL(14일) 뒤 자동 삭제
- 배포 영향: 예전 형식 토큰은 `typ`이 없어 거절됨. 필터는 `J4010`(만료)으로 응답해서 앱이 재발급을 시도하고, 재발급도 실패하면 앱이 토큰을 지우고 로그인 화면으로 보냄 → **배포 후 사용자 전원 1회 재로그인**

### B2. 이메일 대소문자 미정규화 — ✅ 완료
- 위치: `AccountService`, `EmailService`, `LoginAttemptService`
- 문제: DB 조회는 대소문자를 구분하지 않는데(MySQL 기본 collation), Redis 키(로그인 잠금, 인증메일 횟수, 코드 시도 횟수)는 입력값 그대로 사용
- 영향: `Victim@x.com`, `vIctim@x.com`처럼 대소문자만 바꾸면 제한이 각각 새로 적용됨. 인증 코드를 계속 맞혀 보면 비밀번호 재설정까지 가능
- 조치: `EmailUtils.normalize`(공백 제거 + 소문자) 추가. 이메일을 받는 요청 6개(`SignupRequest`, `LoginRequest`, `AuthEmailRequest`, `ConfirmCodeRequest`, `ResetPasswordRequest`, `TestTokenRequest`)가 생성 시 정규화. 관리자 로그인도 `LoginRequest`라 함께 적용

### B3. Redis 시도 횟수 카운터 경쟁 조건 — ⏸️ 보류 (외부 레이트 리밋으로 대체)
- 위치: `LoginAttemptService.recordFailure`, `EmailService.confirmCode`
- 문제: 조회 → 자바에서 증가 → 저장 방식이라 동시 요청이 같은 값을 읽음
- 수정 방향: 두 카운터를 Spring Data Redis 엔티티에서 별도 카운터 키로 분리하고 `INCR` 결과값으로 판정
- 보류 이유: 인증 API에 외부 레이트 리밋이 걸려 있어 동시 요청을 묶을 수 있는 규모가 제한됨. 대소문자 우회는 B2로 막힘
- 다시 볼 조건: 외부 레이트 리밋을 끄거나 완화할 때, 또는 원본 서버(80/443)로 직접 접근할 수 있게 될 때

---

## C. 확인 항목 — ✅ 완료

| 항목 | 결과 |
|---|---|
| `DOCS_ENABLED` 기본값 `true` → 운영에서 `/api/auth/test-token`, API 문서 노출 가능성 | 확인 완료, 조치 불필요 |
| `docker-compose.yml`의 9001 포트 전체 공개 → Caddy 우회로 `/actuator/prometheus` 접근 가능성 | 확인 완료, 조치 불필요 |
| 사지 않은 팩의 퍼즐 노출 | 노출되면 안 됨 → A9로 이동해 완료 |

---

## D. 설계 차원 (참고) — 미진행

- **캐시 저장 API (`POST /api/puzzle/cache/save`)**: 로그인한 사용자 누구나 AI 응답 캐시를 쓸 수 있어 오염 가능성이 있음. 앱이 계산한 응답을 직접 저장하는 구조라 **현 구조 유지로 결정**
- **랭크 결과 재전송**: `/api/rank/game/result` 요청에 퍼즐 식별값이 없어, 응답 시간 초과 후 재시도하면 사용자가 보지 않은 다음 문제에 결과가 적용됨. 요청에 퍼즐 id를 담고 불일치·중복 응답을 거절해야 하며 프론트 수정 필요
- **결제 환불 회수 없음**: 구매 후 환불받아도 지급한 재화를 회수하지 않음. App Store Server Notifications(REFUND), Google Voided Purchases API 처리 필요
- **샌드박스 결제 인정**: Apple 거래의 `environment`를 확인하지 않아 샌드박스(TestFlight) 결제가 운영에서도 재화로 인정됨

---

## E. 사소한 것 (여유 있을 때) — 미진행

- 없는 API를 호출하면 404 대신 401이 나감: `/error`가 인증 대상. `dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` 필요
- 관리자 세션이 만료되면 로그인 페이지 대신 401 JSON이 보임: `/admin/logout`이 JWT 필터 제외 목록에 없고, 필터가 401을 직접 응답함
- 팩 편집 폼에서 `"`가 들어간 제목·작성자가 잘림: `pack-create.html`의 `escapeHtml`이 따옴표를 이스케이프하지 않음
- 팩 번역 정보 검증이 동작하지 않음: `CreateTrainingPackRequest`/`UpdateTrainingPackRequest`의 `info`에 `@Valid`가 없음. 같은 언어를 두 번 넣으면 팩 목록과 추천 API가 500 (관리자 입력)
- 랭킹 100위 밖 사용자는 점수가 0으로 표시됨: 점수를 top 100 안에서만 찾음
- 매시간 랭킹 재계산 중 잠깐 빈 목록이 보임: 키를 지운 뒤 다시 채움. 임시 키에 만든 뒤 `RENAME` 필요
- 좋아요 목록 페이지가 일찍 끝남: 커서로 쓴 퍼즐의 좋아요를 취소하면 `liked_at`이 NULL이 되어 다음 페이지가 비어 보임
- 출석 리셋 기준이 한국시간 오전 9시: `CURRENT_DATE`가 DB 시간대(UTC) 기준
- 일일 업로드 제한을 동시 요청으로 몇 개 초과할 수 있음: 유저 잠금 없이 개수를 셈
- 닉네임 동시 변경 충돌 시 `DUPLICATE_NICKNAME` 대신 일반 409가 나감

E 항목 중 일부(랭킹, 페이지네이션, 관리자 세션)는 자동 점검 결과라 수정 전에 재확인이 필요합니다.
