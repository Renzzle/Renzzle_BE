(function () {
  const STORAGE_KEY = 'renzzle.admin.language';
  const DEFAULT_LANGUAGE = 'en';

  const messages = {
    en: {
      common: {
        appName: 'Renzzle Admin',
        language: 'Language',
        english: 'English',
        korean: 'Korean',
        logout: 'Log out',
        cancel: 'Cancel',
        check: 'Check',
        delete: 'Delete',
        open: 'Open',
        edit: 'Edit',
        editInfo: 'Edit Info',
        refresh: 'Refresh',
        loadList: 'Load List',
        packList: 'Pack List',
        puzzleList: 'Puzzle List',
        backToPack: 'Back to Pack',
        backToList: 'Back to List',
        responseEntryList: 'Response Entry List',
        training: 'Training',
        community: 'Community',
        newPack: 'New Pack',
        addPuzzle: 'Add Puzzle',
        updatePuzzle: 'Update Puzzle',
        puzzleDelete: 'Puzzle Delete',
        none: '(None)',
        noDescription: '(No description)',
        descriptionNone: '(Description None)',
        untitled: '(Untitled)',
        noAuthor: 'No author',
        all: '(All)',
        select: 'Select',
        id: 'ID',
        pack: 'Pack',
        puzzle: 'Puzzle',
        difficulty: 'Difficulty',
        price: 'Price',
        puzzles: 'Puzzles',
        actions: 'Actions',
        order: 'Order',
        title: 'Title',
        author: 'Author',
        description: 'Description',
        packId: 'Pack ID',
        depth: 'Depth',
        board: 'Board',
        answer: 'Answer',
        answerMoves: 'Answer Moves',
        winColor: 'Win Color',
        size: 'Size',
        displayLanguage: 'Display Language',
        boardState: 'Board State',
        currentTurn: 'Current Turn',
        currentMode: 'Current Mode',
        black: 'Black',
        white: 'White',
        loading: 'Loading...',
        errorPrefix: 'Error: {message}'
      },
      title: {
        login: 'Renzzle Admin Login',
        error: 'Renzzle Admin - Error',
        packList: 'Renzzle Admin - Packs',
        packForm: 'Renzzle Admin - Pack Form',
        packWorkspace: 'Renzzle Admin - Pack Workspace',
        puzzleAdd: 'Renzzle Admin - Add Puzzle',
        puzzleEdit: 'Renzzle Admin - Edit Puzzle',
        responseMoves: 'Renzzle - Response Move Entry',
        responseBoard: 'Renzzle - Response Board Entry'
      },
      nav: {
        packs: 'Pack',
        newPack: 'New Pack',
        responseMoves: 'Response Moves'
      },
      login: {
        heading: 'Renzzle Admin',
        email: 'Email',
        emailPlaceholder: 'Enter email',
        password: 'Password',
        passwordPlaceholder: 'Enter password',
        login: 'Log in',
        loggingIn: 'Logging in...',
        required: 'Enter both email and password.',
        failed: 'Login failed',
        error: 'An error occurred during login.'
      },
      errorPage: {
        heading: 'Access Error',
        body: 'An error occurred.',
        backToLogin: 'Back to Login'
      },
      packList: {
        heading: 'Packs',
        description: 'Select a pack first, then create or edit puzzles inside it.',
        selectPack: 'Select Pack',
        autoRefresh: 'The list refreshes automatically when filters change.',
        noPacksStrong: 'No packs match the current filters.',
        noPacksHelp: 'Create a pack, then add puzzles from the pack workspace.',
        count: '{count} packs found',
        zero: '0 packs found',
        failedLoad: 'Failed to load packs',
        metaAuthor: 'Author: {author}',
        deleteConfirm: 'Delete pack "{title}"? This also removes its puzzles.',
        deleteConfirmNoTitle: 'Delete pack? This also removes its puzzles.',
        deleteFailed: 'Failed to delete pack',
        deleted: 'Pack deleted.'
      },
      packCreate: {
        headingCreate: 'Create Pack',
        headingEdit: 'Edit Pack',
        headerEdit: 'Renzzle Admin - Edit Pack',
        description: 'Save the pack basics and per-language display text first.',
        basicInfo: 'Basic Info',
        afterSaving: 'After saving, you will move to the pack workspace.',
        languageHelpPrefix: "Select a language code, then use 'Add Translation' to add each language title, author, and description. Available language codes:",
        languageCode: 'Language Code',
        addTranslation: 'Add Translation',
        priceLabel: 'Price (price)',
        difficultyLabel: 'Difficulty (difficulty)',
        createPack: 'Create Pack',
        updatePack: 'Update Pack',
        languageNumber: 'Language {index} ({code})',
        languageAdded: '{code} (added)',
        titleLabel: 'Title (title)',
        titlePlaceholder: 'Pack Title',
        authorLabel: 'Author (author)',
        authorPlaceholder: 'Author name',
        descriptionLabel: 'Description (description, optional)',
        descriptionPlaceholder: 'Pack Description',
        deleteLanguage: 'Delete Language',
        selectLanguageCode: 'Select a language code.',
        languageAlreadyAdded: 'Language already added: {code}',
        deleteLanguageConfirm: 'Delete this language translation?\n({code})',
        loadFailed: 'Could not load pack info.',
        returningToList: 'Returning to the list.',
        needLanguage: 'Add at least one language with the Add Translation button.',
        needTitleAuthor: 'Enter a title and author for at least one language.',
        updateFailed: 'Failed to update pack',
        updated: 'Pack updated. Opening the pack workspace.',
        createFailed: 'Failed to create pack',
        created: 'Pack created. Opening the pack workspace.'
      },
      packDetail: {
        heading: 'Pack Workspace',
        description: 'Review pack information and manage the puzzles inside this pack.',
        infoHeading: 'Pack Info',
        infoHelp: 'Title and description are shown in the selected language.',
        deletePack: 'Delete Pack',
        puzzlesHeading: 'Puzzles',
        puzzlesHelp: 'Select a row to open the puzzle editor.',
        noPuzzlesStrong: 'No puzzles yet.',
        noPuzzlesHelp: 'Add the first puzzle to this pack.',
        failedLoadPack: 'Failed to load pack.',
        count: '{count} puzzles'
      },
      puzzleForm: {
        addHeading: 'Add Puzzle',
        addDescription: 'Build the initial puzzle board, then enter the answer sequence and save it.',
        editHeading: 'Edit Puzzle',
        editDescription: 'Review the existing board, answer sequence, and calculated values before saving changes.',
        packInfo: 'Pack Info',
        puzzleId: 'Puzzle ID',
        orderLabel: 'Puzzle Order (puzzleIndex)',
        orderHelp: 'Leave empty to append as the last order',
        orderPlaceholder: 'Leave empty to append as the last order',
        boardVisualization: 'Board Visualization',
        boardHint: 'Click the board to place stones, or edit the fields below directly. Initial puzzle stones are unnumbered; answer stones show sequence numbers.',
        canvasLabel: 'Interactive Gomoku board',
        finishInitial: 'Finish Initial Board and Enter Answer',
        clearAnswer: 'Clear Answer Only',
        backToInitial: 'Clear Answer and Edit Initial Board',
        initialBoard: 'Initial Board',
        answerEntry: 'Answer Entry',
        undo: 'Undo Last Move',
        clearBoard: 'Clear Board',
        answerNumbers: 'Answer Sequence Numbers',
        highlightLine: 'Highlight Five-in-a-Row',
        replay: 'Replay Answer Sequence',
        stopReplay: '■ Stop Replay',
        first: 'First',
        previous: 'Previous',
        next: 'Next',
        last: 'Last',
        moves: 'moves',
        playback: 'Answer {step} / {total} moves',
        boardStateLabel: 'Board State (boardStatus) *',
        boardPlaceholder: 'Example: h8i7i5h5... (15x15 board positions, a-o + number)',
        boardFormat: 'Format: column a-o plus row 1-15 for each stone, for example h8i7i5h5. Board clicks update this automatically.',
        answerLabel: 'Answer (answer) *',
        answerPlaceholder: 'Example: h8i7j6... (lowercase letter + number pairs for answer moves)',
        answerHelp: 'Depth equals the number of answer moves. Win color is determined by the initial board move count: even -> BLACK, odd -> WHITE.',
        depthLabel: 'Depth (depth)',
        winColorLabel: 'Win Color (winColor)',
        calculatedPlaceholder: 'Calculated after answer entry',
        stepAdd1Title: 'Puzzle Board',
        stepAdd1Text: 'Enter the initial position',
        stepAdd2Title: 'Answer Sequence',
        stepAdd2Text: 'Enter answer move order',
        stepAdd3Title: 'Auto Calculation',
        stepAdd3Text: 'Check depth and win color',
        stepAdd4Title: 'Save',
        stepAdd4Text: 'Add to pack',
        stepEdit1Title: 'Review Existing Puzzle',
        stepEdit1Text: 'Load board and answer',
        stepEdit2Title: 'Edit Board',
        stepEdit2Text: 'Adjust the initial position',
        stepEdit3Title: 'Edit Answer',
        stepEdit3Text: 'Check sequence and values',
        stepEdit4Title: 'Save or Delete',
        stepEdit4Text: 'Apply changes',
        needBoardAnswer: 'Enter both board state and answer.',
        invalidAnswer: 'Invalid answer format. Use lowercase letter + number pairs, for example h8i7.',
        addFailed: 'Failed to add puzzle',
        added: 'Puzzle added. Puzzle ID: {id}{suffix}',
        appendedSuffix: ' (appended as the last order)',
        updateFailed: 'Failed to update puzzle',
        updated: 'Puzzle updated. Opening the pack workspace.',
        deleteConfirm: 'Delete this puzzle?',
        deleteFailed: 'Delete failed',
        failedLoadPuzzle: 'Failed to load puzzle.'
      },
      cacheList: {
        heading: 'Response Moves',
        description: 'Select a training or community puzzle, then save response moves from the board screen.',
        trainingHeading: 'Training Pack · Puzzle',
        trainingHelp: 'The pack list uses the same filters as the admin pack list. Select a pack, then choose a puzzle below and open response entry.',
        packSubheading: 'Pack',
        puzzlesSubheading: 'Puzzles in Selected Pack',
        communityHeading: 'Community Puzzle Response Entry',
        communityHelp: 'Author filters by exact nickname match. The board screen opens by puzzle ID.',
        authorNickname: 'Author Nickname (exact match, optional)',
        nicknamePlaceholder: 'Nickname',
        minDepth: 'Min Depth',
        maxDepth: 'Max Depth',
        cursor: 'Cursor ID (next page)',
        cursorPlaceholder: 'Select',
        noPacks: 'No packs match the current filters.',
        packMeta: 'Price: {price} / Puzzle Size: {count}',
        responseEntry: 'Response Entry',
        failedLoadList: 'Failed to load list',
        failedLoadPuzzleList: 'Failed to load puzzle list',
        noPuzzles: 'This pack has no puzzles.',
        puzzleMeta: 'depth {depth} · {winColor}{solved}',
        solved: ' · Solved',
        noCommunity: 'No community puzzles match the current filters.',
        communityMeta: '@{author} depth {depth} · {winColor}'
      },
      cacheBoard: {
        heading: 'Response Board',
        description: 'Save or look up the next response for the selected puzzle board state.',
        boardSave: 'Board · Answer Save',
        trainingMeta: 'Training · Pack {packId} · Puzzle {puzzleId}',
        communityMeta: 'Community · Puzzle {puzzleId}',
        boardVisualization: 'Board Visualization',
        boardHint: 'Initial puzzle stones are unnumbered, answer stones show sequence numbers, and the response being entered is marked with a red ring. After locking currentBoard, only one next move can be placed.',
        canvasLabel: 'Interactive Gomoku response board',
        resetBoard: 'Reset to Default Board',
        setCurrentBoard: 'Set Current Board',
        cancelCurrentBoard: 'Cancel Current Board',
        currentBoardString: 'Current Board String',
        filledByButton: 'Filled by button',
        responseMove: 'Response Move',
        responsePlaceholder: 'Click one move on the board',
        answerSave: 'Answer Save',
        lookup: 'Lookup Board Response',
        aiResponse: 'AI Response',
        close: 'Close',
        locked: 'Current board is set. Place exactly one response move.',
        unlocked: 'Not set. Match the board, then click Set Current Board.',
        needCurrentBoard: 'Set currentBoard and enter a response move.',
        saveFailed: 'Save failed',
        saved: 'Saved. The board remains unchanged.',
        setCurrentBoardFirst: 'Set currentBoard first',
        lookupFailed: 'Failed to lookup',
        noResponse: 'No response move'
      },
      msg: {
        sessionExpired: 'Your session has expired or you do not have permission. Please log in again.'
      }
    },
    ko: {
      common: {
        appName: 'Renzzle Admin',
        language: '언어',
        english: '영어',
        korean: '한국어',
        logout: '로그아웃',
        cancel: '취소',
        check: '확인',
        delete: '삭제',
        open: '열기',
        edit: '편집',
        editInfo: '정보 수정',
        refresh: '새로고침',
        loadList: '목록 불러오기',
        packList: '문제집 목록',
        puzzleList: '문제 목록',
        backToPack: '문제집으로',
        backToList: '목록으로',
        responseEntryList: '응답 입력 목록',
        training: '트레이닝',
        community: '커뮤니티',
        newPack: '새 문제집',
        addPuzzle: '문제 추가',
        updatePuzzle: '문제 수정',
        puzzleDelete: '문제 삭제',
        none: '(없음)',
        noDescription: '(설명 없음)',
        descriptionNone: '(설명 없음)',
        untitled: '(제목 없음)',
        noAuthor: '작성자 없음',
        all: '(전체)',
        select: '선택',
        id: 'ID',
        pack: '문제집',
        puzzle: '문제',
        difficulty: '난이도',
        price: '가격',
        puzzles: '문제',
        actions: '작업',
        order: '순서',
        title: '제목',
        author: '작성자',
        description: '설명',
        packId: '문제집 ID',
        depth: '수순',
        board: '보드',
        answer: '정답',
        answerMoves: '정답 수',
        winColor: '승리 색상',
        size: '개수',
        displayLanguage: '표시 언어',
        boardState: '보드 상태',
        currentTurn: '현재 차례',
        currentMode: '현재 모드',
        black: '흑',
        white: '백',
        loading: '불러오는 중...',
        errorPrefix: '오류: {message}'
      },
      title: {
        login: 'Renzzle 관리자 로그인',
        error: 'Renzzle 관리자 - 오류',
        packList: 'Renzzle 관리자 - 문제집',
        packForm: 'Renzzle 관리자 - 문제집 폼',
        packWorkspace: 'Renzzle 관리자 - 문제집 작업공간',
        puzzleAdd: 'Renzzle 관리자 - 문제 추가',
        puzzleEdit: 'Renzzle 관리자 - 문제 편집',
        responseMoves: 'Renzzle - 응답 수 입력',
        responseBoard: 'Renzzle - 응답 보드 입력'
      },
      nav: {
        packs: '문제집',
        newPack: '새 문제집',
        responseMoves: '응답 수'
      },
      login: {
        heading: 'Renzzle Admin',
        email: '이메일',
        emailPlaceholder: '이메일 입력',
        password: '비밀번호',
        passwordPlaceholder: '비밀번호 입력',
        login: '로그인',
        loggingIn: '로그인 중...',
        required: '이메일과 비밀번호를 모두 입력하세요.',
        failed: '로그인에 실패했습니다.',
        error: '로그인 중 오류가 발생했습니다.'
      },
      errorPage: {
        heading: '접근 오류',
        body: '오류가 발생했습니다.',
        backToLogin: '로그인으로 돌아가기'
      },
      packList: {
        heading: '문제집',
        description: '먼저 문제집을 선택한 뒤, 그 안에서 문제를 생성하거나 편집합니다.',
        selectPack: '문제집 선택',
        autoRefresh: '필터를 변경하면 목록이 자동으로 새로고침됩니다.',
        noPacksStrong: '현재 필터에 맞는 문제집이 없습니다.',
        noPacksHelp: '문제집을 만든 뒤 작업공간에서 문제를 추가하세요.',
        count: '문제집 {count}개',
        zero: '문제집 0개',
        failedLoad: '문제집을 불러오지 못했습니다.',
        metaAuthor: '작성자: {author}',
        deleteConfirm: '"{title}" 문제집을 삭제할까요? 포함된 문제도 함께 삭제됩니다.',
        deleteConfirmNoTitle: '문제집을 삭제할까요? 포함된 문제도 함께 삭제됩니다.',
        deleteFailed: '문제집 삭제에 실패했습니다.',
        deleted: '문제집이 삭제되었습니다.'
      },
      packCreate: {
        headingCreate: '문제집 생성',
        headingEdit: '문제집 편집',
        headerEdit: 'Renzzle 관리자 - 문제집 편집',
        description: '문제집 기본 정보와 언어별 표시 문구를 먼저 저장합니다.',
        basicInfo: '기본 정보',
        afterSaving: '저장 후 문제집 작업공간으로 이동합니다.',
        languageHelpPrefix: "언어 코드를 선택한 뒤 '번역 추가'로 각 언어의 제목, 작성자, 설명을 추가하세요. 사용 가능한 언어 코드:",
        languageCode: '언어 코드',
        addTranslation: '번역 추가',
        priceLabel: '가격 (price)',
        difficultyLabel: '난이도 (difficulty)',
        createPack: '문제집 생성',
        updatePack: '문제집 수정',
        languageNumber: '언어 {index} ({code})',
        languageAdded: '{code} (추가됨)',
        titleLabel: '제목 (title)',
        titlePlaceholder: '문제집 제목',
        authorLabel: '작성자 (author)',
        authorPlaceholder: '작성자 이름',
        descriptionLabel: '설명 (description, 선택)',
        descriptionPlaceholder: '문제집 설명',
        deleteLanguage: '언어 삭제',
        selectLanguageCode: '언어 코드를 선택하세요.',
        languageAlreadyAdded: '이미 추가된 언어입니다: {code}',
        deleteLanguageConfirm: '이 언어 번역을 삭제할까요?\n({code})',
        loadFailed: '문제집 정보를 불러오지 못했습니다.',
        returningToList: '목록으로 돌아갑니다.',
        needLanguage: '번역 추가 버튼으로 언어를 하나 이상 추가하세요.',
        needTitleAuthor: '언어 하나 이상에 제목과 작성자를 입력하세요.',
        updateFailed: '문제집 수정에 실패했습니다.',
        updated: '문제집을 수정했습니다. 작업공간으로 이동합니다.',
        createFailed: '문제집 생성에 실패했습니다.',
        created: '문제집을 생성했습니다. 작업공간으로 이동합니다.'
      },
      packDetail: {
        heading: '문제집 작업공간',
        description: '문제집 정보를 확인하고 이 문제집의 문제를 관리합니다.',
        infoHeading: '문제집 정보',
        infoHelp: '제목과 설명은 선택한 언어 기준으로 표시됩니다.',
        deletePack: '문제집 삭제',
        puzzlesHeading: '문제',
        puzzlesHelp: '행을 선택하면 문제 편집 화면으로 이동합니다.',
        noPuzzlesStrong: '아직 문제가 없습니다.',
        noPuzzlesHelp: '이 문제집에 첫 문제를 추가하세요.',
        failedLoadPack: '문제집을 불러오지 못했습니다.',
        count: '문제 {count}개'
      },
      puzzleForm: {
        addHeading: '문제 추가',
        addDescription: '초기 문제 보드를 만든 뒤 정답 순서를 입력하고 저장합니다.',
        editHeading: '문제 편집',
        editDescription: '저장하기 전에 기존 보드, 정답 순서, 계산값을 확인합니다.',
        packInfo: '문제집 정보',
        puzzleId: '문제 ID',
        orderLabel: '문제 순서 (puzzleIndex)',
        orderHelp: '비워두면 마지막 순서로 추가됩니다.',
        orderPlaceholder: '비워두면 마지막 순서로 추가됩니다.',
        boardVisualization: '보드 시각화',
        boardHint: '보드를 클릭해 돌을 놓거나 아래 필드를 직접 편집하세요. 초기 문제 돌에는 번호가 없고, 정답 돌에는 순서 번호가 표시됩니다.',
        canvasLabel: '대화형 오목 보드',
        finishInitial: '초기 보드 완료 후 정답 입력',
        clearAnswer: '정답만 지우기',
        backToInitial: '정답 지우고 초기 보드 편집',
        initialBoard: '초기 보드',
        answerEntry: '정답 입력',
        undo: '마지막 수 되돌리기',
        clearBoard: '보드 지우기',
        answerNumbers: '정답 순서 번호',
        highlightLine: '5목 라인 강조',
        replay: '정답 순서 재생',
        stopReplay: '■ 재생 중지',
        first: '처음',
        previous: '이전',
        next: '다음',
        last: '마지막',
        moves: '수',
        playback: '정답 {step} / {total}수',
        boardStateLabel: '보드 상태 (boardStatus) *',
        boardPlaceholder: '예: h8i7i5h5... (15x15 보드 위치, a-o + 숫자)',
        boardFormat: '형식: 각 돌은 a-o 열과 1-15 행으로 입력합니다. 예: h8i7i5h5. 보드 클릭 시 자동으로 갱신됩니다.',
        answerLabel: '정답 (answer) *',
        answerPlaceholder: '예: h8i7j6... (정답 수는 소문자 열 + 숫자 쌍)',
        answerHelp: '수순은 정답 수 개수입니다. 승리 색상은 초기 보드 수 개수로 결정됩니다: 짝수 -> BLACK, 홀수 -> WHITE.',
        depthLabel: '수순 (depth)',
        winColorLabel: '승리 색상 (winColor)',
        calculatedPlaceholder: '정답 입력 후 계산됩니다',
        stepAdd1Title: '문제 보드',
        stepAdd1Text: '초기 배치를 입력',
        stepAdd2Title: '정답 순서',
        stepAdd2Text: '정답 수 순서 입력',
        stepAdd3Title: '자동 계산',
        stepAdd3Text: '수순과 승리 색상 확인',
        stepAdd4Title: '저장',
        stepAdd4Text: '문제집에 추가',
        stepEdit1Title: '기존 문제 확인',
        stepEdit1Text: '보드와 정답 불러오기',
        stepEdit2Title: '보드 편집',
        stepEdit2Text: '초기 배치 조정',
        stepEdit3Title: '정답 편집',
        stepEdit3Text: '순서와 계산값 확인',
        stepEdit4Title: '저장 또는 삭제',
        stepEdit4Text: '변경사항 적용',
        needBoardAnswer: '보드 상태와 정답을 모두 입력하세요.',
        invalidAnswer: '정답 형식이 올바르지 않습니다. 예: h8i7처럼 소문자 열 + 숫자 쌍을 사용하세요.',
        addFailed: '문제 추가에 실패했습니다.',
        added: '문제를 추가했습니다. 문제 ID: {id}{suffix}',
        appendedSuffix: ' (마지막 순서로 추가됨)',
        updateFailed: '문제 수정에 실패했습니다.',
        updated: '문제를 수정했습니다. 문제집 작업공간으로 이동합니다.',
        deleteConfirm: '이 문제를 삭제할까요?',
        deleteFailed: '삭제에 실패했습니다.',
        failedLoadPuzzle: '문제를 불러오지 못했습니다.'
      },
      cacheList: {
        heading: '응답 수',
        description: '트레이닝 또는 커뮤니티 문제를 선택한 뒤 보드 화면에서 응답 수를 저장합니다.',
        trainingHeading: '트레이닝 문제집 · 문제',
        trainingHelp: '문제집 목록과 같은 필터를 사용합니다. 문제집을 선택한 뒤 아래에서 문제를 고르고 응답 입력을 엽니다.',
        packSubheading: '문제집',
        puzzlesSubheading: '선택한 문제집의 문제',
        communityHeading: '커뮤니티 문제 응답 입력',
        communityHelp: '작성자 필터는 닉네임 완전 일치로 검색합니다. 보드 화면은 문제 ID 기준으로 열립니다.',
        authorNickname: '작성자 닉네임 (완전 일치, 선택)',
        nicknamePlaceholder: '닉네임',
        minDepth: '최소 수순',
        maxDepth: '최대 수순',
        cursor: '커서 ID (다음 페이지)',
        cursorPlaceholder: '선택',
        noPacks: '현재 필터에 맞는 문제집이 없습니다.',
        packMeta: '가격: {price} / 문제 수: {count}',
        responseEntry: '응답 입력',
        failedLoadList: '목록을 불러오지 못했습니다.',
        failedLoadPuzzleList: '문제 목록을 불러오지 못했습니다.',
        noPuzzles: '이 문제집에는 문제가 없습니다.',
        puzzleMeta: '수순 {depth} · {winColor}{solved}',
        solved: ' · 해결됨',
        noCommunity: '현재 필터에 맞는 커뮤니티 문제가 없습니다.',
        communityMeta: '@{author} 수순 {depth} · {winColor}'
      },
      cacheBoard: {
        heading: '응답 보드',
        description: '선택한 문제 보드 상태의 다음 응답 수를 저장하거나 조회합니다.',
        boardSave: '보드 · 정답 저장',
        trainingMeta: '트레이닝 · 문제집 {packId} · 문제 {puzzleId}',
        communityMeta: '커뮤니티 · 문제 {puzzleId}',
        boardVisualization: '보드 시각화',
        boardHint: '초기 문제 돌에는 번호가 없고, 정답 돌에는 순서 번호가 표시됩니다. 입력 중인 응답 수는 빨간 원으로 표시됩니다. currentBoard를 잠그면 다음 수 하나만 둘 수 있습니다.',
        canvasLabel: '대화형 오목 응답 보드',
        resetBoard: '기본 보드로 초기화',
        setCurrentBoard: '현재 보드 설정',
        cancelCurrentBoard: '현재 보드 취소',
        currentBoardString: '현재 보드 문자열',
        filledByButton: '버튼으로 채워집니다',
        responseMove: '응답 수',
        responsePlaceholder: '보드에서 수 하나를 클릭하세요',
        answerSave: '정답 저장',
        lookup: '보드 응답 조회',
        aiResponse: 'AI 응답',
        close: '닫기',
        locked: '현재 보드가 설정되었습니다. 응답 수 하나만 놓으세요.',
        unlocked: '설정되지 않았습니다. 보드를 맞춘 뒤 현재 보드 설정을 클릭하세요.',
        needCurrentBoard: 'currentBoard를 설정하고 응답 수를 입력하세요.',
        saveFailed: '저장에 실패했습니다.',
        saved: '저장했습니다. 보드는 그대로 유지됩니다.',
        setCurrentBoardFirst: 'currentBoard를 먼저 설정하세요.',
        lookupFailed: '조회에 실패했습니다.',
        noResponse: '응답 수가 없습니다.'
      },
      msg: {
        sessionExpired: '세션이 만료되었거나 권한이 없습니다. 다시 로그인하세요.'
      }
    }
  };

  function normalizeLanguage(lang) {
    return String(lang || '').toLowerCase().startsWith('ko') ? 'ko' : 'en';
  }

  function getStoredLanguage() {
    try {
      return normalizeLanguage(localStorage.getItem(STORAGE_KEY) || navigator.language || DEFAULT_LANGUAGE);
    } catch {
      return DEFAULT_LANGUAGE;
    }
  }

  let currentLanguage = getStoredLanguage();

  function lookup(lang, key) {
    return key.split('.').reduce((obj, part) => obj?.[part], messages[lang]);
  }

  function interpolate(template, vars) {
    return String(template).replace(/\{([^}]+)}/g, function (_, name) {
      const value = vars && Object.hasOwn(vars, name) ? vars[name] : '';
      return value == null ? '' : String(value);
    });
  }

  function parseVars(raw) {
    if (!raw) return {};
    try {
      return JSON.parse(raw);
    } catch {
      return {};
    }
  }

  function t(key, vars) {
    const value = lookup(currentLanguage, key) || lookup(DEFAULT_LANGUAGE, key) || key;
    return interpolate(value, vars);
  }

  function applyText(el, attr, setter) {
    const key = el.getAttribute(attr);
    if (!key) return;
    setter(t(key, parseVars(el.dataset.i18nVars)));
  }

  function applyI18n(root) {
    const scope = root || document;
    const elements = scope.querySelectorAll
      ? scope.querySelectorAll('[data-i18n], [data-i18n-placeholder], [data-i18n-title], [data-i18n-aria-label]')
      : [];
    elements.forEach(function (el) {
      applyText(el, 'data-i18n', function (value) { el.textContent = value; });
      applyText(el, 'data-i18n-placeholder', function (value) { el.setAttribute('placeholder', value); });
      applyText(el, 'data-i18n-title', function (value) { el.setAttribute('title', value); });
      applyText(el, 'data-i18n-aria-label', function (value) { el.setAttribute('aria-label', value); });
    });
    if (scope.nodeType === 1) {
      applyText(scope, 'data-i18n', function (value) { scope.textContent = value; });
      applyText(scope, 'data-i18n-placeholder', function (value) { scope.setAttribute('placeholder', value); });
      applyText(scope, 'data-i18n-title', function (value) { scope.setAttribute('title', value); });
      applyText(scope, 'data-i18n-aria-label', function (value) { scope.setAttribute('aria-label', value); });
    }
    document.documentElement.lang = currentLanguage;
    document.querySelectorAll('[data-admin-language-select]').forEach(function (select) {
      select.value = currentLanguage;
    });
  }

  function setLanguage(lang) {
    const nextLanguage = normalizeLanguage(lang);
    if (nextLanguage === currentLanguage) return;
    currentLanguage = nextLanguage;
    try {
      localStorage.setItem(STORAGE_KEY, currentLanguage);
    } catch {
      // Ignore storage errors.
    }
    applyI18n(document);
    document.dispatchEvent(new CustomEvent('admin:language-change', { detail: { language: currentLanguage } }));
  }

  function setText(el, key, vars) {
    if (!el) return;
    el.dataset.i18n = key;
    if (vars) {
      el.dataset.i18nVars = JSON.stringify(vars);
    } else {
      delete el.dataset.i18nVars;
    }
    el.textContent = t(key, vars);
  }

  document.addEventListener('DOMContentLoaded', function () {
    applyI18n(document);
    document.querySelectorAll('[data-admin-language-select]').forEach(function (select) {
      select.addEventListener('change', function () {
        setLanguage(select.value);
      });
    });
  });

  globalThis.adminMessages = messages;
  globalThis.adminT = t;
  globalThis.adminSetLanguage = setLanguage;
  globalThis.adminGetLanguage = function () { return currentLanguage; };
  globalThis.adminApplyI18n = applyI18n;
  globalThis.adminSetText = setText;
})();
