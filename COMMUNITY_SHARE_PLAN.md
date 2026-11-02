# 커뮤니티 퍼즐 공유 페이지 계획

> 2026-11-02 · 상태: 계획 · 예상 작업량 약 10시간 (스토어 심사 제외)

## 개요

커뮤니티 퍼즐을 링크로 공유하고, 받은 사람이 웹에서 바로 풀어보고 검토까지 할 수 있게 한다. AI 대응은 앱과 똑같이 한다. 앱이 설치된 사용자는 링크를 누르면 앱이 바로 열린다.

- AI는 **브라우저에서 WASM으로** 돌린다. 서버에서 돌리면 한 번 탐색할 때 CPU 코어 하나를 10~20초씩 붙잡기 때문이다.

## 확정된 결정

- 공유 대상은 **커뮤니티 퍼즐만**이다. 유료 트레이닝 팩은 제외한다.
- 공유 페이지에서는 **풀어보기와 검토**까지 할 수 있다. 정답 보기, 기록, 랭킹, 로그인은 없다.
  - 검토는 앱과 같다. 사용자가 직접 둔 수순을 다시 보는 것이라 정답이 드러나지 않는다.
- 웹에서는 **캐시에 저장하지 않는다**. 캐시는 읽기만 한다.
- 웹 조회는 **조회수를 올리지 않는다**.
- URL에는 퍼즐 ID 대신 **추측할 수 없는 공유 토큰**(UUID v4)을 쓴다.
- 브라우저 전용 **공개 읽기 API를 따로** 만든다. 앱 키는 웹 어디에도 넣지 않는다.
- 엔진 상수(시간 제한, TT 크기)는 **앱과 동일하게 유지**한다. 웹용으로 바꾸지 않는다.
- 정답 비밀 유지는 목표가 아니다. 렌주 전용 엔진에 넣으면 어차피 풀린다. 보호는 가볍게만 한다.
  - WASM은 심볼을 빼고 빌드한다.
  - next-moves 응답은 XOR + base64로 바꿔 사람이 바로 못 읽게 한다.
- 딥링크는 **이미 설치한 사용자가 앱으로 바로 열리는 것까지만** 한다. 설치 후 해당 퍼즐로 이동하는 것(디퍼드 딥링크)은 범위 밖이다.

## 흐름

```
https://renzzle.com/p/{token}
 ├─ 앱 설치됨 → Universal Link / App Link → 앱 퍼즐 화면
 └─ 웹 → 공유 페이지 (BE Thymeleaf)
      ├─ GET 퍼즐 정보        (공개, 정답 제외)
      ├─ GET next-moves       (공개, 난독화)
      └─ 캐시 miss → Web Worker 안의 WASM 엔진이 AI 수 계산
```

## 1. AI 엔진 (WASM)

- 소스는 `Renzzle/Renzzle_AI`의 `main` 브랜치다. FE도 여기서 복사해 간다(2026-11-02 기준 FE `develop`과 동일).
- 빌드는 AI 레포에서 한다. BE는 빌드 결과물(`.mjs`, `.wasm`)만 `src/main/resources/static/assets/engine/`에 복사한다.
- 엔진 코드는 수정 없이 빌드된다(PoC로 확인). 래퍼에서 노출할 함수는 아래와 같다.

| 함수                       | 원본                                           | 반환              |
|----------------------------|------------------------------------------------|-------------------|
| `renzzle_next_move(board)` | `engine.h`의 `findNextMove`                    | 수 인덱스         |
| `renzzle_check_win(board)` | FE `RenzzleJNI.cpp`의 `checkWin`을 그대로 이식 | 0/1/2 (의미 동일) |

```bash
em++ -O3 -std=c++17 -I<Renzzle_AI> web.cpp -o renzzle-engine.mjs \
  -sMODULARIZE -sEXPORT_ES6 -sENVIRONMENT=web,worker \
  -sSTACK_SIZE=8MB -sALLOW_MEMORY_GROWTH=1 -sMAXIMUM_MEMORY=128MB \
  -sEXPORTED_FUNCTIONS=_renzzle_next_move,_renzzle_check_win \
  -sEXPORTED_RUNTIME_METHODS=ccall,cwrap
```

- **`-sSTACK_SIZE=8MB`는 필수다.** 기본 스택(64KB)에서는 깊은 탐색 중에 `table index is out of bounds` 오류로 크래시가 난다.
- `-g`나 `--profiling-funcs` 없이 빌드한다. 필요하면 `wasm-opt --strip-debug --strip-producers`를 추가로 돌린다. 소스맵은 배포하지 않는다.
- 엔진은 Web Worker 1개를 계속 재사용한다. 취소는 워커를 `terminate()`한 뒤 새로 띄우는 방식으로 한다. 탐색 중에는 워커가 메시지를 받지 못하기 때문이다.
- 빌드 도구는 `emscripten/emsdk` Docker 이미지다. 결과물 크기는 `.wasm` 155KB, gzip 58KB다.

PoC 결과(x86, Node/V8, 네이티브 대비):

| 항목                | 결과                     |
|---------------------|--------------------------|
| 결과 일치           | 같은 수, 같은 노드 수    |
| 짧은 탐색           | 5~10% 느림               |
| 10초를 다 쓰는 탐색 | 초당 노드 수 20~35% 적음 |

## 2. BE

### 공개 API

`/api/**` 경로는 앱 키와 인증이 둘 다 필요하다(`SecurityConfig`). 그래서 브라우저용 API는 `/api` 밖 경로에 두고 `permitAllRequestMatchers`에 추가한다. 아래 경로는 예시다.

| 경로                                               | 용도                                                         |
|----------------------------------------------------|--------------------------------------------------------------|
| `GET /p/{token}`                                   | 공유 페이지 (Thymeleaf, OG 태그)                             |
| `GET /share-api/puzzles/{token}`                   | 풀이와 화면 표시에 필요한 필드만 반환. 정답과 내부 ID는 제외 |
| `GET /share-api/puzzles/{token}/next-moves?board=` | 캐시 후보를 난독화해서 반환                                  |

- 토큰을 퍼즐 ID로 바꾸는 건 서버 안에서만 한다. 응답에 `puzzleId`를 넣지 않는다.
- 삭제된 퍼즐은 제외한다. 엔티티의 `@SQLRestriction`으로 이미 걸러지지만, 앱 목록과 같은 공개 조건인지 확인한다.
- 조회수를 올리는 `CommunityService`(`increaseView`, 397행)를 재사용하지 않는다.
- 저장이나 삭제를 하는 공개 API는 만들지 않는다.

### 공유 토큰

- `community_puzzle`에 `share_token` 컬럼(UUID v4, unique)을 추가한다. `ddl-auto: update`라서 컬럼은 자동으로 생긴다.
- 토큰은 **처음 공유할 때 발급**한다. 그러면 기존 데이터를 채울(backfill) 필요가 없다.
- 앱용 API가 두 개 필요하다.
  - 공유 링크 발급: 예) `POST /api/community/puzzle/{id}/share` → URL 반환
  - 딥링크로 들어온 토큰을 퍼즐로 바꾸는 조회 API

### next-moves 난독화

- 목적은 네트워크 탭에서 바로 읽히지 않게 하는 것이다. 보안 장치가 아니다.
- XOR 키는 토큰과 보드 상태로 만든다. 좌표 문자열 대신 인덱스로 보내고, base64인 티가 나지 않게 한다.
- 캐시 조회는 기존 `PuzzleCacheService.getNextMoveCandidates`를 재사용한다.

### 기타

- **XSS**: 퍼즐 제목과 닉네임은 사용자가 입력한 값이다. `th:text`나 `th:content`만 쓰고 `th:utext`는 금지한다. 인라인 JS가 필요하면 `th:inline="javascript"`를 쓴다.
- **딥링크 검증 파일**: `/.well-known/apple-app-site-association`과 `/.well-known/assetlinks.json`을 `caddy/Caddyfile`에서 응답한다(`app-ads.txt`와 같은 방식).
  - 내용에 애플 Team ID와 Play 앱 서명 키의 SHA-256 지문이 필요하다.
  - Cloudflare 봇 차단이 이 파일 요청을 막지 않는지 확인한다.
- **속도 제한**: edge 쪽 속도 제한 규칙에 새 공개 경로가 포함되는지 확인한다.

## 3. 웹 공유 페이지

- 보드 그리기는 `static/assets/admin/board.js`를 재사용한다.
- 대국 흐름은 FE `Board/index.tsx`(`prefetchNextMoves`, `getCachedAiAnswer`)를 옮겨 온다. 캐시 저장 부분만 뺀다.
  1. 사용자 차례가 되면 그 보드의 next-moves를 미리 받아 둔다.
  2. 사용자가 두면 `check_win`으로 승패를 확인한다.
  3. 캐시에 있으면 그 수로 즉시 응답한다. 없으면 워커에서 `next_move`로 계산한다.
  4. AI가 둔 뒤 다시 `check_win`으로 확인한다.
- 검토는 FE `PuzzleReview` 화면과 `Board`의 `makeMode="review"`를 옮겨 온다. 앱의 `viewAnswer`(정답 보기) 경로는 가져오지 않는다.
  1. 풀이가 끝나면(성공이든 실패든) "검토" 버튼으로 들어간다.
  2. 검토할 수순은 문제 보드 뒤에 사용자가 둔 수를 붙인 것이다. 클라이언트에 이미 있으니 서버 호출이 필요 없다.
  3. 마지막 수부터 보여 주고, 이전/다음 버튼으로 한 수씩 이동한다.
  4. 어느 지점에서든 직접 수를 둬 볼 수 있다. 흑백이 번갈아 놓이고 AI는 응답하지 않는다.
  5. 각 단계는 `check_win`으로 미리 판정해서 오목 완성이나 금수(X 표시)를 바로 보여 준다. 이미 끝난 판이면 더 둘 수 없다.
- 좌표 형식은 기존과 같다.
  - 보드 문자열은 `h8i9...`처럼 문자와 숫자를 이어 붙인다.
  - 수 인덱스는 `(문자-'a')*15 + (숫자-1)`이고, BE의 `moveIndexToPosition`과 같다.
- **"앱으로 보기" 버튼**은 같은 도메인 링크로는 앱이 열리지 않는다. iOS에서는 같은 도메인 안의 이동에 유니버설 링크가 발동하지 않기 때문이다. 그래서 커스텀 스킴을 쓴다.
  - iOS: `renzzle://p/{token}`
  - Android: `intent://p/{token}#Intent;scheme=renzzle;package=...;end`
  - 카카오톡 인앱 브라우저에서도 이 방식이 가장 잘 동작한다.
- Safari 상단 앱 배너(Smart App Banner)를 메타태그 한 줄로 추가한다.
- **페이지 하단**
  - 저작권 표시 `© 2026 Studio Isoo. All rights reserved.`를 넣는다. 배포하는 JS와 워커 파일 상단에도 같은 문구를 주석으로 넣는다.
  - `Renzzle_Policy`에 있는 이용약관과 개인정보처리방침 링크는 선택이다. 분석 도구나 광고를 붙이면 개인정보처리방침 링크는 필수가 된다.
- 처음 내려가는 HTML과 OG 태그에는 정답이나 수순을 넣지 않는다. OG에는 제목과 이미지 정도만 넣는다.

## 4. FE (앱)

현재 딥링크 설정이 전혀 없다. 매니페스트에는 앱 실행용(launcher) 설정만 있고, iOS URL 스킴, Associated Domains, RN `linking` 설정도 없다.

- **iOS**
  - Associated Domains에 `applinks:renzzle.com`을 추가한다. 개발자 포털에서 기능을 켜고 프로비저닝 프로파일을 갱신해야 한다.
  - 커스텀 스킴 `renzzle`을 등록한다.
- **Android**
  - `https://renzzle.com/p/*`용 intent-filter를 추가한다(`autoVerify="true"`).
  - 커스텀 스킴 `renzzle`을 등록한다.
- **RN**
  - `linking` 설정으로 `/p/:token`을 커뮤니티 퍼즐 화면에 연결한다. 토큰은 조회 API로 퍼즐로 바꾼다.
  - 로그인이 안 된 상태도 처리한다.
- **공유 버튼**: 발급 API를 호출한 뒤 RN `Share`로 공유 시트를 띄운다.
- 앱 업데이트 배포가 필요하다. 새 버전 사용자부터 동작한다.

## 5. 보안 시나리오

| 시나리오                            | 대응                                                           |
|-------------------------------------|----------------------------------------------------------------|
| 유료 트레이닝 팩이 공유로 풀림      | 커뮤니티 퍼즐만 공유                                           |
| 앱 키 노출 → 앱 API 악용, 캐시 오염 | 공개 API 분리, 웹에는 앱 키 없음, 웹은 저장 안 함              |
| 삭제된 퍼즐이나 불필요한 필드 노출  | 공개 조건 동일하게 적용, 필요한 필드만 응답                    |
| 사용자 입력값으로 XSS               | Thymeleaf 이스케이프만 사용                                    |
| 조회수 부풀리기                     | 웹은 조회수 증가 없음                                          |
| 퍼즐 대량 수집                      | 공유 토큰 사용, edge 속도 제한                                 |
| 정답 노출                           | **받아들임** (렌주 전용 엔진으로 어차피 풀림). 가벼운 난독화만 |
| 엔진 도용                           | **받아들임**. 레포 비공개 전환과 LICENSE로 대응                |

## 6. 작업량

| 작업                                          | 시간  |
|-----------------------------------------------|-------|
| WASM 빌드와 래퍼, 워커                        | ~1h   |
| BE 공개 API, 공유 토큰, 난독화, 페이지 라우트 | ~2h   |
| 웹 풀이와 검토 화면                           | ~4h   |
| 딥링크 (FE 스킴과 링크 설정, BE 검증 파일)    | ~1.5h |
| 실기기와 브라우저 검증                        | ~2–3h |

## 7. 검증 체크리스트

- [ ] Chrome, Safari, iOS Safari, Android Chrome, 카카오톡 인앱 브라우저에서 WASM 로드, 메모리(~70MB), 스택 확인
- [ ] 캐시 hit와 miss 양쪽에서 AI 응답 확인
- [ ] 검토에서 이전/다음 이동, 직접 착수, 오목·금수 표시가 앱과 같게 동작하는지
- [ ] 앱 설치 상태에서 링크를 누르면 앱이 열리는지(iOS, Android). 애플 CDN이 검증 파일을 캐싱해서 반영이 늦을 수 있다
- [ ] 앱 미설치 상태에서 웹이 정상 동작하는지
- [ ] 응답에 정답이나 퍼즐 ID가 없는지
- [ ] 소스맵이 배포되지 않았는지
- [ ] 페이지 하단과 배포 JS에 저작권 표시가 들어갔는지

## 8. 후속 / 범위 밖

- `Renzzle_AI`와 `Renzzle_FE`를 비공개로 전환한다(현재 둘 다 공개, 포크 0개). 전환 후에는 WASM 빌드 때 레포를 받아오는 데 인증이 필요하다.
- LICENSE 파일을 추가한다.
- 디퍼드 딥링크(설치 후 해당 퍼즐로 이동)는 하지 않는다.

## 참고 파일

| 위치 | 파일                                                                                    |
|------|-----------------------------------------------------------------------------------------|
| BE   | `global/config/SecurityConfig.java` (공개 경로 목록, 앱 키 필터)                        |
| BE   | `domain/puzzle/cache/api/PuzzleCacheController.java`, `service/PuzzleCacheService.java` |
| BE   | `domain/puzzle/community/service/CommunityService.java` (`increaseView`)                |
| BE   | `resources/static/assets/admin/board.js`, `caddy/Caddyfile`                             |
| FE   | `src/components/features/Board/index.tsx`, `src/apis/puzzleCache.ts`                    |
| FE   | `src/screens/PuzzleReview/index.tsx`, `src/hooks/usePuzzleReviewNavigation/index.ts`    |
| FE   | `android/app/src/main/java/com/renzzle_fe/cpp/jni/RenzzleJNI.cpp` (`checkWin`)          |
| AI   | `engine/engine.h` (`findNextMove`)                                                      |
