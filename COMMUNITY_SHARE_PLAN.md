# 커뮤니티 퍼즐 공유 페이지 계획

> 2026-11-02 · 상태: 계획 · 예상 작업량 약 10시간 (스토어 심사 제외)

## 개요

커뮤니티 퍼즐을 링크로 공유하고, 받은 사람이 웹에서 바로 풀어볼 수 있게 한다. AI 대응은 앱과 똑같이 한다. 앱이 설치된 사용자는 링크를 누르면 앱이 바로 열린다.

- AI는 **브라우저에서 WASM으로** 돌린다. 서버에서 돌리면 한 번 탐색할 때 CPU 코어 하나를 10~20초씩 붙잡기 때문이다.

## 확정된 결정

- 공유 대상은 **커뮤니티 퍼즐만**이다. 유료 트레이닝 팩은 제외한다.
- 공유 페이지에서는 **풀어보기**까지 할 수 있다. 정답 보기, 검토, 기록, 랭킹, 로그인은 없다.
  - 검토(다 푼 뒤 이전/다음으로 다시 보기)는 처음에 넣었다가 2026-11-03 v3 디자인으로 바꾸면서 뺐다. 버튼은 새로 하기 하나만 둔다.
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
https://renzzle.com/puzzle/{token}
 ├─ 앱 설치됨 → Universal Link / App Link → 앱 퍼즐 화면
 └─ 웹 → 공유 페이지 (BE Thymeleaf)
      ├─ GET 퍼즐 정보        (공개, 정답 제외)
      ├─ GET next-moves       (공개, 난독화)
      └─ 캐시 miss → Web Worker 안의 WASM 엔진이 AI 수 계산
```

## 1. AI 엔진 (WASM)

✅ 구현됨: `engine-wasm/`, `static/assets/engine/`

- 엔진 소스는 `Renzzle/Renzzle_AI`에 있다. FE와 같은 방식으로, 브리지 코드는 BE가 갖고 엔진은 AI 레포의 고정 커밋에서 받아 온다.
  - FE는 `RenzzleJNI.cpp`를 갖고 있고, BE는 `engine-wasm/renzzle_web.cpp`를 갖는다.
- 엔진을 바꿀 때는 `engine-wasm/build.sh`의 `AI_COMMIT`를 올리고 스크립트를 실행한 뒤, 결과물 두 개(`renzzle-engine.mjs`, `renzzle-engine.wasm`)를 커밋한다. git과 Docker가 필요하다.
  - 빌드 도구는 `emscripten/emsdk:6.0.13` 이미지로 고정했다.
  - AI 레포를 비공개로 돌리면 클론할 때 인증이 필요하다.
- 엔진 코드는 수정 없이 빌드된다. 래퍼가 노출하는 함수는 아래와 같다.

| 함수                       | 원본                                           | 반환              |
|----------------------------|------------------------------------------------|-------------------|
| `renzzle_next_move(board)` | `engine.h`의 `findNextMove`                    | 수 인덱스         |
| `renzzle_check_win(board)` | FE `RenzzleJNI.cpp`의 `checkWin`을 그대로 이식 | 0/1/2 (의미 동일) |

- 빌드 옵션의 핵심은 아래와 같다.
  - **`-sSTACK_SIZE=8MB`는 필수다.** 기본 스택(64KB)에서는 깊은 탐색 중에 `table index is out of bounds` 오류로 크래시가 난다.
  - 메모리는 `-sINITIAL_MEMORY=32MB`로 시작해 최대 `-sMAXIMUM_MEMORY=128MB`까지 늘어난다. 실측 최고치는 97MB다. 상한을 걸면 초기 메모리를 자동으로 정하지 않으므로 직접 지정해야 한다.
  - `-sENVIRONMENT=worker -sMODULARIZE -sEXPORT_ES6`로 빌드해 모듈 워커(`type: 'module'`)에서 `import`로 불러온다. 모듈 워커는 iOS 15 이상이 필요하다.
- **`wasm-opt --all-features`를 따로 돌리면 안 된다.** import를 실험적인 형식으로 다시 써서 Chrome과 Safari 모두 로드에 실패한다. emcc가 이미 디버그 정보와 이름 섹션을 지워 주니 따로 돌릴 필요도 없다.
- 결과물 크기는 `.wasm` 149KB, gzip 56KB다. JS 첫 줄에 저작권 문구가 들어간다.
- 실행 구조
  - `engine-worker.js`가 엔진을 불러와 메시지로 함수를 호출한다.
  - `engine.js`는 전역 `renzzleEngine`을 노출한다: `start`, `nextMove`, `checkWin`, `cancel`, `isCancelled`.
  - 워커 1개를 계속 재사용한다. 취소하거나 오류가 나면 워커를 버리고, 다음 호출 때 새로 띄운다. 탐색 중에는 워커가 메시지를 받지 못하기 때문이다.
- 로컬 Chromium과 WebKit(Playwright)에서 확인한 결과는 아래와 같다.
  - 승패 판정, AI 수(앱 엔진과 같은 수), 10초 탐색, 취소 후 재시작 모두 통과했다.
  - 메모리 최고치는 97MB다.
- 별도 기기 확인 페이지는 두지 않는다. 엔진을 못 쓰는 기기는 공유 페이지가 알아서 직접 두어보기로 바꾼다(3번 참고).
- 최소 지원 버전은 Safari 15.4(iOS 15.4, 공유 페이지의 `Array.prototype.at`), Chrome 92, Firefox 114(모듈 워커)다.
  - gomocalc(웹 Rapfi)는 시작 메모리 64MB에 멀티스레드를 써서 우리보다 무겁다. 그래서 그게 돌아가는 기기면 우리 엔진도 돈다고 본다.

PoC 결과(x86, Node/V8, 네이티브 대비):

| 항목                | 결과                     |
|---------------------|--------------------------|
| 결과 일치           | 같은 수, 같은 노드 수    |
| 짧은 탐색           | 5~10% 느림               |
| 10초를 다 쓰는 탐색 | 초당 노드 수 20~35% 적음 |

## 2. BE

### 공개 API

`/api/**` 경로는 앱 키와 인증이 둘 다 필요하다(`SecurityConfig`). 그래서 브라우저용 API는 `/api` 밖의 `/puzzle/` 아래에 두고, `GET /puzzle/**`를 `permitAllRequestMatchers`에 추가했다. (✅ 구현됨: `SharedPuzzleController`)

| 경로                                    | 용도                                                                     |
|-----------------------------------------|--------------------------------------------------------------------------|
| `GET /puzzle/{token}`                   | 공유 페이지 (Thymeleaf, OG 태그). `SharedPuzzlePageController`           |
| `GET /puzzle/{token}/data`              | 보드, 작성자 닉네임, 설명, 수 깊이, 승리 색, 검증 여부. 정답과 ID는 없음 |
| `GET /puzzle/{token}/next-moves?board=` | 사용자 차례 보드의 캐시 후보를 난독화한 문자열(`response`)로 반환        |

- 토큰을 퍼즐 ID로 바꾸는 건 서버 안에서만 한다. 공개 응답에 ID를 넣지 않는다.
- 삭제된 퍼즐은 `@SQLRestriction`으로 걸러져 404(`P4040`)가 난다. 앱 상세 조회와 같은 조건이다.
- 조회수를 올리지 않는다.
- `/puzzle/**`는 GET만 열려 있다. 다른 메서드는 여전히 로그인이 필요하다.

### 공유 토큰

✅ 구현됨: `CommunityShareService`, `CommunityController`

- `community_puzzle.share_token` 컬럼(UUID v4, unique)이다. `ddl-auto: update`라서 컬럼은 자동으로 생긴다.
- 토큰은 **처음 공유할 때 발급**하고 이후 바뀌지 않는다. 기존 데이터를 채울(backfill) 필요가 없다.
  - 퍼즐 행을 잠그고 발급하므로, 동시에 처음 공유해도 토큰은 하나로 정해진다.
- 앱용 API(앱 키와 로그인 필요)
  - `POST /api/community/puzzle/{puzzleId}/share` → `{ sharePath }` (예: `/puzzle/{token}`). 앱이 이미 쓰는 서버 주소를 앞에 붙여 공유한다.
  - `GET /api/community/puzzle/share/{token}` → `{ puzzleId }`. 딥링크로 들어온 토큰을 퍼즐로 바꾼다.
- 서버에는 도메인 설정이 없다.
  - 앱은 받은 경로에 자기가 쓰는 서버 주소를 붙인다. 그래서 테스트 서버를 보는 앱 빌드는 테스트 서버 링크를 만든다.
  - 공유 페이지의 `og:url`과 `og:image`는 들어온 요청의 주소로 만든다. `server.forward-headers-strategy: native` 덕분에 Cloudflare와 Caddy 뒤에서도 `https://renzzle.com`으로 나온다.

### next-moves 난독화

✅ 구현됨: `SharedMovesEncoder`. 웹에서 풀 때는 테스트용 `SharedMovesTestDecoder`를 참고한다.

- 목적은 네트워크 탭에서 바로 읽히지 않게 하는 것이다. 보안 장치가 아니다.
- 캐시 조회는 기존 `PuzzleCacheService.getNextMoveCandidates`를 재사용한다.
- 인코딩 순서(디코딩은 역순):
  1. 후보마다 2바이트 `[사용자 수 칸, AI 응답 칸]`. 칸 번호는 0부터 시작하는 인덱스다.
  2. 키스트림과 XOR한다. 첫 블록은 `SHA-256(UTF-8(token + board))`, 다음 블록은 이전 블록의 SHA-256이다.
  3. base64url(패딩 없음)로 바꾼 뒤, 각 글자를 섞은 알파벳으로 치환한다(`SharedMovesEncoder.SHUFFLED_ALPHABET`).
- 후보가 없으면 빈 문자열이다.

### 기타

- **XSS**: 퍼즐 제목과 닉네임은 사용자가 입력한 값이다. `th:text`나 `th:content`만 쓰고 `th:utext`는 금지한다. 인라인 JS가 필요하면 `th:inline="javascript"`를 쓴다.
- **딥링크 검증 파일 (보류: 딥링크 작업 때 함께 한다)**: `/.well-known/apple-app-site-association`과 `/.well-known/assetlinks.json`을 `caddy/Caddyfile`에서 응답한다(`app-ads.txt`와 같은 방식).
  - 경로 패턴은 `/puzzle/*`이다. 내용에 애플 Team ID와 Play 앱 서명 키의 SHA-256 지문이 필요하다.
  - 앱이 도메인을 선언하기 전에는 OS가 이 파일을 쓰지 않으므로 먼저 올려 둬도 영향이 없다.
- **Cloudflare**: 지금은 `/api`와 `/admin` 하위를 뺀 경로를 막고 있다(규칙 확인 필요). 아래 경로를 열어야 한다.
  - `/puzzle/*`: 공유 페이지와 공개 API
  - `/.well-known/*`: 딥링크 검증 파일
  - `/assets/*`: 어드민이 이미 쓰고 있으니 열려 있을 가능성이 높다. 확인만 한다.
  - `/.well-known/*`과 `/puzzle/*`은 봇 챌린지에서도 빼야 한다. 애플 CDN과 카카오톡 링크 미리보기 크롤러는 챌린지를 통과하지 못한다.
  - 새로 연 공개 경로에 edge 속도 제한 규칙을 건다.

## 3. 웹 공유 페이지

✅ 구현됨: `templates/share/puzzle.html`, `static/assets/share/puzzle.js`, `static/assets/share/share.css`

- 디자인은 앱 v3 시안의 커뮤니티 풀이 화면(`Design/redesign-v3/screens/community-solve.html`)을 따른다(2026-11-03). 정답 보기, 뒤로가기, 별로예요는 빼고, 좋아요는 누르는 버튼이 아니라 수만 보여 준다. 색은 `share.css` 맨 위 변수에 모았다.
- 보드는 앱 v3 판과 같다.
  - 판 `#E7C99A`(가운데 위쪽이 살짝 밝다), 안쪽 선은 먹 26%, 바깥 선과 화점은 먹 62%, 흑돌 `#2C2A25`, 백돌 `#FFFEFB`, 좌표 표시
  - 공유 페이지는 `puzzle.js`의 `COLORS`와 `board.js`로 이 색을 쓴다.
- 화면 구성(모바일 우선, 한 열)
  - 맨 위: 휴대폰이면 앱 배너(앱 아이콘, "Renzzle", 한 줄 소개, "앱으로 보기")
  - 목표: 둘 돌 색과 "흑으로 오목을 만드세요", 같은 줄 오른쪽에 새로 하기 버튼(유일한 버튼), 아래 줄에 출제자, 깊이, 인증 여부
  - 오목판, 그 아래 좋아요 수와 풀이·조회 수, 출제자 설명
  - 화면 맨 아래 상태 줄: 착수 안내, AI가 생각하는 중, 결과(성공·실패)
  - 한 화면에 다 들어가고 페이지는 스크롤되지 않는다. 화면이 짧으면 판이 작아지고(설명이 있으면 한 줄은 보이게), 긴 설명은 판 아래 영역만 스크롤된다.
- 언어는 `LangCode`의 7개(한국어, 영어, 일본어, 중국어 간체·번체, 러시아어, 에스토니아어)를 지원한다(2026-11-03).
  - 링크의 `?lang=`(예: `?lang=ja`, `zh-Hant`)이 가장 먼저다. 앱이 공유 링크에 사용자의 앱 언어를 붙이면, 헤더를 안 보내는 크롤러도 그 언어로 미리보기(제목, 설명, 이미지)를 만든다. 지원하지 않는 값이면 무시한다.
  - 그다음은 `Accept-Language`에서 지원하는 첫 언어이고, 없거나 헤더가 없으면 영어다.
  - `?lang=`이 있으면 `og:url`에도 남긴다. 일부 크롤러는 `og:url`을 다시 읽기 때문이다.
  - 중국어는 대만·홍콩·마카오(`zh-TW`, `zh-HK`, `zh-MO`, `zh-Hant`)면 번체, 나머지는 간체다.
  - 게임 이름은 그 나라에서 렌주룰 오목을 부르는 말을 쓴다(2026-11-04 조사).
    - 한국어 오목, 영어 Renju, 일본어 連珠(五目並べ는 금수 없는 놀이), 중국어 五子棋, 러시아어 рэндзю(정식 스포츠 종목명), 에스토니아어 rendžu
    - 퍼즐은 일본어 問題, 중국어 残局(殘局), 러시아어 задача, 에스토니아어 ülesanne로 부른다. 일본어 목표 문구의 "다섯"은 연주 용어 五連이다.
  - 페이지 글은 `messages/share_*.properties`(`SharedPuzzlePageText`가 읽는다), 상태 줄 문구는 `puzzle.js`의 `MESSAGES`에 있다. 한국어·영어는 앱 문구와 같다.
- 스토어 링크는 앱 설정(`AppInfo`)의 `ios_store_url`과 `android_store_url`을 쓴다. 서버가 `User-Agent`를 보고 아이폰·아이패드면 App Store, 안드로이드면 Google Play 링크 하나만 배너에 넣는다. 컴퓨터이거나 그 기기의 링크 값이 없으면 배너를 숨긴다. 데스크톱 모드의 iPad Safari는 Mac으로 보여 배너가 안 나온다.
- 검색엔진 노출은 막는다(`noindex`). 공유 토큰이 검색 결과에 쌓이면 대량 수집을 막는 의미가 없어지기 때문이다.
- 없는 토큰이나 삭제된 퍼즐은 404 상태로 "퍼즐을 찾을 수 없어요" 화면을 보여 주고, 엔진은 불러오지 않는다.
- 착수는 앱처럼 두 번 탭한다. 첫 탭은 선택만 하고, 같은 칸을 한 번 더 누르면 놓는다.
- 수 제한(깊이)은 앱처럼 표시만 하고 강제하지 않는다. AI가 둘 곳이 없으면(-1) 앱과 같이 실패로 처리한다.
- **링크 미리보기 이미지**: 퍼즐과 상관없는 고정 이미지 `static/assets/share/og-image-{lang}.png`(1200×600, 2:1)를 페이지 언어별로 쓴다(2026-11-04).
  - 디자인: 먹색(`#26241F`) 바탕 왼쪽에 앱 워드마크(`Renzzle_FE`의 `ico-logo-text.svg`)를 작은 라벨로 두고, 그 아래 큰 문구 한 줄("오목 문제가 도착했어요")을 두었다. 오른쪽에는 v3 나뭇결 판 모서리에 흑돌 넷이 대각선으로 서 있고, 다섯째 자리를 초록 선택 링으로 표시했다.
  - 근거: 밝은 채팅 화면에서 어두운 카드가 눈에 띄고, 작은 말풍선에서도 읽히는 굵은 문구 하나와 큰 돌만 남겼다.
  - 판 패널은 둥근 모서리 없이 오른쪽 절반(600×600)을 채운다. 칸이 100px이라 네 변이 모두 격자선에 맞아 잘린 칸이 없고, 돌은 안쪽 5×5 교차점에 놓인다.
  - 앱 아이콘은 넣지 않는다. 밝은 아이콘 타일이 헤드라인과 시선을 나눠 가졌고, 왼쪽 아래 서명 배치도 어색했다. 워드마크는 높이 30px이고, 판의 초록 링과 같은 계열의 밝은 초록(`#4CAF8C`, 어두운 바탕용으로 `point-strong`을 밝힌 색)으로 칠해 좌우를 잇는다. 헤드라인과 20px 간격으로 붙여 한 덩어리로 읽히게 했고, 왼쪽 여백은 72px다. 초록 링은 공유 페이지의 선택 표시와 같은 굵기(`1.75 * PX`)다.
  - 문구는 실력을 묻는 질문("풀 수 있나요?")은 도발적이고, 권유("퍼즐 한 판 어때요?")는 어색해서 메신저로 받은 링크에 맞는 "도착했어요"로 정했다.
  - 번역이 길면 판과 겹치지 않게 글자 크기를 줄였다(일본어, 영어, 러시아어). 에스토니아어는 "gomoku"를 빼고 "Sulle saabus mõistatus"로 줄였다.
  - 언어는 페이지와 같이 `Accept-Language`로 고른다. 헤더 없는 크롤러(카카오톡 등)는 영어 이미지를 받는다.
  - 처음에는 퍼즐마다 판을 그려 넣었다. 하지만 2:1 칸에서는 판 전체가 보이지 않아 의미가 없어서 뺐다. 그때 쓰던 서버 렌더러(`SharedPuzzlePreviewRenderer`)와 `/puzzle/{token}/preview.png`도 지웠다.
  - 카카오톡은 1:1 이미지도 말풍선에서 잘라서 2:1로 만들었다.
  - 카카오톡은 미리보기를 URL별로 캐시한다. 그래서 이미지 주소에 `?v=1`을 붙였고, 이미지를 바꾸면 `SharedPuzzlePageController.PREVIEW_VERSION`을 올린다.
  - 없는 퍼즐 페이지에도 같은 이미지를 넣는다.
- 로컬 Chromium과 WebKit(Playwright)에서 아래 시나리오가 모두 통과했다.
  - 캐시 응답, 엔진 응답, 성공, 검토(이전/다음, 지금은 뺐다), 금수 패배
  - wasm을 막았을 때 직접 두어보기로 전환, 없는 토큰 404
- 판의 선, 기보 읽기, 오목 판정은 `static/assets/share/board.js`에 있다. 어드민 보드에서 가져왔고, 어드민 페이지를 지우며 공유 페이지 쪽으로 옮겼다(2026-11-04).
- 대국 흐름은 FE `Board/index.tsx`(`prefetchNextMoves`, `getCachedAiAnswer`)를 옮겨 온다. 캐시 저장 부분만 뺀다.
  1. 사용자 차례가 되면 그 보드의 next-moves를 미리 받아 둔다.
  2. 사용자가 두면 `check_win`으로 승패를 확인한다.
  3. 캐시에 있으면 그 수로 즉시 응답한다. 없으면 워커에서 `next_move`로 계산한다.
  4. AI가 둔 뒤 다시 `check_win`으로 확인한다.
- 검토는 2026-11-03 디자인 변경 때 뺐다. 다 풀면 결과만 보여 주고, 다시 하려면 새로 하기를 누른다.
- **엔진을 못 쓰는 기기에서는 직접 두어보기로 바꾼다.** 흑백이 번갈아 놓이고, AI는 응답하지 않는다.
  - 페이지를 열 때 엔진을 미리 띄우고 가벼운 호출(`checkWin`)을 하나 보낸다. 로드 실패, 오류, 일정 시간 무응답이면 처음부터 이 모드로 시작한다.
  - 첫 AI 수에서 실패하면(메모리 부족 등은 실제 탐색에서야 드러난다) 그 자리에서 안내 문구를 띄우고 이 모드로 바꾼다.
  - 승패 판정도 엔진 함수라서 금수 표시는 할 수 없다. 오목 완성은 `board.js`의 `findFiveLine`으로 표시한다.
  - 캐시만으로 AI 응답을 흉내 내지는 않는다. 캐시에 없는 수에서 막히는 경우까지 처리해야 해서, 드문 기기를 위해 하기엔 복잡하다.
- 좌표 형식은 기존과 같다.
  - 보드 문자열은 `h8i9...`처럼 문자와 숫자를 이어 붙인다.
  - 수 인덱스는 `(문자-'a')*15 + (숫자-1)`이고, BE의 `moveIndexToPosition`과 같다.
- **"앱으로 보기" 버튼 (보류: 딥링크 작업 때 넣는다)**은 같은 도메인 링크로는 앱이 열리지 않는다. iOS에서는 같은 도메인 안의 이동에 유니버설 링크가 발동하지 않기 때문이다. 그래서 커스텀 스킴을 쓴다.
  - iOS: `renzzle://puzzle/{token}`
  - Android: `intent://puzzle/{token}#Intent;scheme=renzzle;package=...;end`
  - 카카오톡 인앱 브라우저에서도 이 방식이 가장 잘 동작한다.
  - **앱이 반영될 때까지는 버튼을 숨긴다.** 스킴이 등록되지 않은 상태에서 열면 iOS Safari가 오류 창을 띄운다.
  - 지금은 맨 위 앱 배너의 "앱으로 보기"가 그 기기의 스토어로 간다(앱이 깔려 있으면 스토어에서 열기). 딥링크가 반영되면 이 버튼이 스킴으로 먼저 앱을 열도록 바꾼다.
- Safari 상단 앱 배너(Smart App Banner) 메타태그는 처음에 넣었다가 뺐다. 페이지 맨 위 앱 배너와 겹쳐 두 개가 쌓이기 때문이다.
- **저작권 표시**는 배포하는 JS·CSS와 워커 파일 상단 주석에만 둔다. 페이지 하단의 저작권 줄과 이용약관·개인정보처리방침 링크는 한 화면에 맞추면서 뺐다(2026-11-03). 분석 도구나 광고를 붙이면 개인정보처리방침 링크는 다시 필요하다.
- 처음 내려가는 HTML과 OG 태그에는 정답이나 수순을 넣지 않는다. OG에는 제목과 이미지 정도만 넣는다.

## 4. FE (앱)

**딥링크는 나중으로 미뤘다.** 서버 쪽 작업을 모두 끝낸 뒤에 반영한다. 그 전까지 공유 링크는 항상 웹으로 열린다.

현재 딥링크 설정이 전혀 없다. 매니페스트에는 앱 실행용(launcher) 설정만 있고, iOS URL 스킴, Associated Domains, RN `linking` 설정도 없다.

- **iOS**
  - Associated Domains에 `applinks:renzzle.com`을 추가한다. 개발자 포털에서 기능을 켜고 프로비저닝 프로파일을 갱신해야 한다.
  - 커스텀 스킴 `renzzle`을 등록한다.
- **Android**
  - `https://renzzle.com/puzzle/*`용 intent-filter를 추가한다(`autoVerify="true"`).
  - 커스텀 스킴 `renzzle`을 등록한다.
- **RN**
  - `linking` 설정으로 `/puzzle/:token`을 커뮤니티 퍼즐 화면에 연결한다. 토큰은 조회 API로 퍼즐로 바꾼다.
  - 로그인이 안 된 상태도 처리한다.
- **공유 버튼**: 발급 API를 호출한 뒤 RN `Share`로 공유 시트를 띄운다. 링크는 `서버 주소 + sharePath + ?lang={앱 언어}`로 만든다(BE는 사용자 언어를 모른다).
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

| 작업                                          | 시간  | 상태                            |
|-----------------------------------------------|-------|---------------------------------|
| WASM 빌드와 래퍼, 워커                        | ~1h   | ✅ 완료                         |
| BE 공개 API, 공유 토큰, 난독화, 페이지 라우트 | ~2h   | ✅ 완료                         |
| 웹 풀이 화면                                  | ~4h   | ✅ 완료 (v3 디자인)             |
| 딥링크 (FE 스킴과 링크 설정, BE 검증 파일)    | ~1.5h | 보류                            |
| 실기기와 브라우저 검증                        | ~2–3h | 로컬 브라우저 완료, 실기기 남음 |

## 7. 검증 체크리스트

- [ ] iOS Safari, Android Chrome, 카카오톡 인앱 브라우저에서 공유 페이지의 AI 대응이 동작하는지
- [ ] 엔진 로드를 일부러 실패시켰을 때(예: wasm 경로를 틀리게) 직접 두어보기로 바뀌는지
- [ ] 캐시 hit와 miss 양쪽에서 AI 응답 확인
- [ ] 앱 설치 상태에서 링크를 누르면 앱이 열리는지(iOS, Android). 애플 CDN이 검증 파일을 캐싱해서 반영이 늦을 수 있다
- [ ] 앱 미설치 상태에서 웹이 정상 동작하는지
- [ ] 응답에 정답이나 퍼즐 ID가 없는지
- [ ] 소스맵이 배포되지 않았는지
- [ ] 배포 JS에 저작권 표시가 들어갔는지

## 8. 후속 / 범위 밖

- `Renzzle_AI`와 `Renzzle_FE`를 비공개로 전환한다(현재 둘 다 공개, 포크 0개). 전환 후에는 WASM 빌드 때 레포를 받아오는 데 인증이 필요하다.
- LICENSE 파일을 추가한다.
- 디퍼드 딥링크(설치 후 해당 퍼즐로 이동)는 하지 않는다.

## 참고 파일

| 위치 | 파일                                                                                                 |
|------|------------------------------------------------------------------------------------------------------|
| BE   | `global/config/SecurityConfig.java` (공개 경로 목록, 앱 키 필터)                                     |
| BE   | `domain/puzzle/community/service/CommunityShareService.java`, `api/SharedPuzzleController.java`      |
| BE   | `domain/puzzle/community/util/SharedMovesEncoder.java`, 테스트 `support/SharedMovesTestDecoder.java` |
| BE   | `domain/puzzle/cache/api/PuzzleCacheController.java`, `service/PuzzleCacheService.java`              |
| BE   | `domain/puzzle/community/service/CommunityService.java` (`increaseView`)                             |
| BE   | `resources/static/assets/share/board.js`, `caddy/Caddyfile`                                          |
| FE   | `src/components/features/Board/index.tsx`, `src/apis/puzzleCache.ts`                                 |
| FE   | `src/screens/PuzzleReview/index.tsx`, `src/hooks/usePuzzleReviewNavigation/index.ts`                 |
| FE   | `android/app/src/main/java/com/renzzle_fe/cpp/jni/RenzzleJNI.cpp` (`checkWin`)                       |
| AI   | `engine/engine.h` (`findNextMove`)                                                                   |
| BE   | `domain/puzzle/community/web/SharedPuzzlePageController.java`, `templates/share/puzzle.html`         |
| BE   | `static/assets/share/puzzle.js`, `static/assets/share/share.css`                                     |
| BE   | `engine-wasm/`, `static/assets/engine/`                                                              |
