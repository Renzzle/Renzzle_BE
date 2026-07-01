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
        search: 'Search',
        packList: 'Pack List',
        puzzleList: 'Puzzle List',
        backToPack: 'Back to Pack',
        backToList: 'Back to List',
        responseEntryList: 'AI Response List',
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
        packList: 'Renzzle Admin - Training Puzzles',
        packForm: 'Renzzle Admin - Pack Form',
        packWorkspace: 'Renzzle Admin - Pack Workspace',
        puzzleAdd: 'Renzzle Admin - Add Puzzle',
        puzzleEdit: 'Renzzle Admin - Edit Puzzle',
        responseMoves: 'Renzzle - AI Response Entry',
        responseBoard: 'Renzzle - AI Response Board'
      },
      nav: {
        packs: 'Training Puzzles',
        newPack: 'New Pack',
        responseMoves: 'AI Responses'
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
        heading: 'Training Puzzle',
        description: 'Manage training puzzles.',
        selectPack: 'Select Pack',
        autoRefresh: 'The list refreshes automatically when filters change.',
        createHeading: 'Create Pack',
        createDescription: 'Fill one or more language sections.',
        translationOptional: 'Optional',
        creating: 'Creating pack...',
        created: 'Pack created. Opening the pack workspace.',
        createFailed: 'Failed to create pack',
        createNeedTranslation: 'Enter at least one language title and author.',
        createNeedTitleAuthor: 'For each language you use, enter both title and author.',
        noPacks: 'No packs found.',
        noPacksStrong: 'No packs found.',
        noPacksHelp: '',
        count: '{count} packs found',
        zero: '0 packs found',
        unknownCount: '- packs found',
        searchTitle: 'Search Title',
        titleSearchPlaceholder: 'Pack title',
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
        priceLabel: 'Price',
        difficultyLabel: 'Difficulty',
        createPack: 'Create Pack',
        updatePack: 'Update Pack',
        languageNumber: 'Language {index} ({code})',
        languageAdded: '{code} (added)',
        titleLabel: 'Title',
        titlePlaceholder: 'Pack Title',
        authorLabel: 'Author',
        authorPlaceholder: 'Author name',
        descriptionLabel: 'Description',
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
        puzzlesHeading: 'Puzzle List',
        puzzlesHelp: 'Select a row to open the puzzle editor.',
        noPuzzlesStrong: 'No puzzles yet.',
        noPuzzlesHelp: 'Add the first puzzle to this pack.',
        failedLoadPack: 'Failed to load pack.',
        puzzleNumber: 'No.{number}',
        openPuzzle: 'Open puzzle {number}',
        count: '{count} puzzles'
      },
      puzzleForm: {
        addHeading: 'Add Puzzle',
        addDescription: 'Build the initial puzzle board, then enter the answer sequence and save it.',
        editHeading: 'Edit Puzzle',
        editDescription: 'Review the existing board, answer sequence, and calculated values before saving changes.',
        packInfo: 'Pack Info',
        puzzleId: 'Puzzle ID',
        orderLabel: 'Puzzle Order',
        orderHelp: '(Leave empty to append as the last order)',
        orderPlaceholder: '(Leave empty to append as the last order)',
        boardVisualization: 'Board Visualization',
        boardHint: 'Click the board to place stones, or edit the fields below directly. Initial puzzle stones are unnumbered; answer stones show sequence numbers.',
        canvasLabel: 'Interactive Gomoku board',
        finishInitial: 'Go to Answer Input',
        clearAnswer: 'Clear Answer Only',
        backToInitial: 'Switch to Problem Input',
        initialBoard: 'Problem Input',
        answerEntry: 'Answer Input',
        undo: 'Back',
        previousMove: 'Previous Move',
        nextMove: 'Next Move',
        toggleMode: 'Change Mode',
        clearBoard: 'Reset',
        answerResetConfirm: 'Switching to problem input will clear the answer you entered. Continue?',
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
        boardStateLabel: 'Board State *',
        boardPlaceholder: 'h8h9i8...',
        boardFormat: 'h8h9i8...',
        answerLabel: 'Answer *',
        answerPlaceholder: 'h8h9i8...',
        answerHelp: '',
        depthLabel: 'Depth',
        winColorLabel: 'Win Color',
        calculatedPlaceholder: '',
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
        invalidAnswer: 'Invalid answer format. Example: h8h9i8.',
        addFailed: 'Failed to add puzzle',
        added: 'Puzzle added.{suffix}',
        appendedSuffix: ' (appended as the last order)',
        updateFailed: 'Failed to update puzzle',
        updated: 'Puzzle updated. Opening the pack workspace.',
        deleteConfirm: 'Delete this puzzle?',
        deleteFailed: 'Delete failed',
        failedLoadPuzzle: 'Failed to load puzzle.'
      },
      cacheList: {
        heading: 'AI Responses',
        description: 'Choose training or community puzzles first.',
        targetType: 'Target Type',
        trainingTab: 'Training',
        communityTab: 'Community',
        trainingHeading: 'Training Puzzles',
        trainingHelp: 'Select a pack, then choose a puzzle.',
        packSubheading: 'Pack',
        puzzlesSubheading: 'Puzzles in Selected Pack',
        communityHeading: 'Community Puzzles',
        communityHelp: 'Search by puzzle number, then open it to manage its AI response.',
        puzzleNumber: 'Puzzle Number',
        puzzleNumberPlaceholder: 'Puzzle number',
        authorNickname: 'Author Nickname',
        nicknamePlaceholder: 'Nickname',
        minDepth: 'Min Depth',
        maxDepth: 'Max Depth',
        cursor: 'Cursor',
        cursorPlaceholder: 'Select',
        noPacks: 'No packs found.',
        packMeta: 'Price {price} · {count} puzzles',
        responseEntry: 'AI Response Entry',
        openResponsePuzzle: 'Open AI response puzzle {number}',
        failedLoadList: 'Failed to load list',
        failedLoadPuzzleList: 'Failed to load puzzle list',
        noPuzzles: 'This pack has no puzzles.',
        puzzleMeta: 'depth {depth} · {winColor}{solved}',
        solved: ' · Solved',
        noCommunity: 'No community puzzles match the current filters.',
        communityMeta: '@{author} depth {depth} · {winColor}'
      },
      cacheTraining: {
        title: 'Renzzle - Training AI Responses',
        heading: 'Training AI Responses',
        description: 'Choose a puzzle from this pack.',
        noPuzzles: 'No puzzles in this pack.'
      },
      cacheBoard: {
        heading: 'AI Response Board',
        description: 'Save or look up the next AI response for the selected puzzle board state.',
        boardSave: 'Board · Answer Save',
        trainingMeta: 'Training · Pack {packId} · Puzzle {puzzleId}',
        communityMeta: 'Community · Puzzle {puzzleId}',
        boardVisualization: 'Board Visualization',
        boardHint: 'Initial puzzle stones are unnumbered, answer stones show sequence numbers, and the AI response being entered is marked with a red ring. After locking currentBoard, only one next move can be placed.',
        canvasLabel: 'Interactive Gomoku AI response board',
        resetBoard: 'Reset to Default Board',
        setCurrentBoard: 'Set Current Board',
        startResponseInput: 'Input AI Response',
        cancelCurrentBoard: 'Cancel Current Board',
        cancelResponseInput: 'Cancel Input',
        currentBoardString: 'Current Board String',
        filledByButton: 'Filled by button',
        responseMove: 'AI Response',
        responsePlaceholder: 'Click one move on the board',
        answerSave: 'Save',
        lookup: 'Lookup AI Response',
        aiResponse: 'AI Response',
        close: 'Close',
        locked: 'AI response input is active. Place exactly one response.',
        unlocked: 'Input activates automatically when the board reaches the AI response turn.',
        needCurrentBoard: 'Set currentBoard and enter an AI response.',
        saveFailed: 'Save failed',
        saved: 'Saved.',
        setCurrentBoardFirst: 'Set currentBoard first',
        responseTurnOnly: 'Set the current board after the user move. AI response can only be entered on the opponent turn.',
        lookupFailed: 'Failed to lookup',
        noResponse: 'No AI response'
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
        search: '검색',
        packList: '문제집 목록',
        puzzleList: '문제 목록',
        backToPack: '문제집으로',
        backToList: '목록으로',
        responseEntryList: 'AI 대응 목록',
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
        depth: '깊이',
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
        packList: 'Renzzle 관리자 - 트레이닝 문제',
        packForm: 'Renzzle 관리자 - 문제집 폼',
        packWorkspace: 'Renzzle 관리자 - 문제집 작업공간',
        puzzleAdd: 'Renzzle 관리자 - 문제 추가',
        puzzleEdit: 'Renzzle 관리자 - 문제 편집',
        responseMoves: 'Renzzle - AI 대응 입력',
        responseBoard: 'Renzzle - AI 대응 보드'
      },
      nav: {
        packs: '트레이닝 문제',
        newPack: '새 문제집',
        responseMoves: 'AI 대응'
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
        heading: '트레이닝 문제',
        description: '트레이닝 문제를 관리합니다.',
        selectPack: '문제집 선택',
        autoRefresh: '필터를 변경하면 목록이 자동으로 새로고침됩니다.',
        createHeading: '문제집 생성',
        createDescription: '사용할 언어의 제목과 작성자를 입력하세요.',
        translationOptional: '선택',
        creating: '문제집을 생성하는 중...',
        created: '문제집을 생성했습니다. 작업공간으로 이동합니다.',
        createFailed: '문제집 생성에 실패했습니다.',
        createNeedTranslation: '하나 이상의 언어에 제목과 작성자를 입력하세요.',
        createNeedTitleAuthor: '사용할 언어마다 제목과 작성자를 모두 입력하세요.',
        noPacks: '문제집이 없습니다.',
        noPacksStrong: '문제집이 없습니다.',
        noPacksHelp: '',
        count: '문제집 {count}개',
        zero: '문제집 0개',
        unknownCount: '문제집 -개',
        searchTitle: '제목 검색',
        titleSearchPlaceholder: '문제집 제목',
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
        priceLabel: '가격',
        difficultyLabel: '난이도',
        createPack: '문제집 생성',
        updatePack: '문제집 수정',
        languageNumber: '언어 {index} ({code})',
        languageAdded: '{code} (추가됨)',
        titleLabel: '제목',
        titlePlaceholder: '문제집 제목',
        authorLabel: '작성자',
        authorPlaceholder: '작성자 이름',
        descriptionLabel: '설명',
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
        puzzlesHeading: '문제 목록',
        puzzlesHelp: '행을 선택하면 문제 편집 화면으로 이동합니다.',
        noPuzzlesStrong: '아직 문제가 없습니다.',
        noPuzzlesHelp: '이 문제집에 첫 문제를 추가하세요.',
        failedLoadPack: '문제집을 불러오지 못했습니다.',
        puzzleNumber: '{number}번',
        openPuzzle: '{number}번 문제 편집',
        count: '문제 {count}개'
      },
      puzzleForm: {
        addHeading: '문제 추가',
        addDescription: '초기 문제 보드를 만든 뒤 정답 순서를 입력하고 저장합니다.',
        editHeading: '문제 편집',
        editDescription: '저장하기 전에 기존 보드, 정답 순서, 계산값을 확인합니다.',
        packInfo: '문제집 정보',
        puzzleId: '문제 ID',
        orderLabel: '문제 순서',
        orderHelp: '(비워두면 마지막 순서로 추가됩니다.)',
        orderPlaceholder: '(비워두면 마지막 순서로 추가됩니다.)',
        boardVisualization: '보드 시각화',
        boardHint: '보드를 클릭해 돌을 놓거나 아래 필드를 직접 편집하세요. 초기 문제 돌에는 번호가 없고, 정답 돌에는 순서 번호가 표시됩니다.',
        canvasLabel: '대화형 오목 보드',
        finishInitial: '정답 입력으로 이동',
        clearAnswer: '정답만 지우기',
        backToInitial: '문제 입력으로 전환',
        initialBoard: '문제 입력',
        answerEntry: '정답 입력',
        undo: '뒤로가기',
        previousMove: '이전 수',
        nextMove: '다음 수',
        toggleMode: '모드 변경',
        clearBoard: '초기화',
        answerResetConfirm: '문제 입력으로 전환하면 지금 입력해둔 정답이 삭제될 수 있습니다. 계속할까요?',
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
        boardStateLabel: '보드 상태 *',
        boardPlaceholder: 'h8h9i8...',
        boardFormat: 'h8h9i8...',
        answerLabel: '정답 *',
        answerPlaceholder: 'h8h9i8...',
        answerHelp: '',
        depthLabel: '깊이',
        winColorLabel: '승리 색상',
        calculatedPlaceholder: '',
        stepAdd1Title: '문제 보드',
        stepAdd1Text: '초기 배치를 입력',
        stepAdd2Title: '정답 순서',
        stepAdd2Text: '정답 수 순서 입력',
        stepAdd3Title: '자동 계산',
        stepAdd3Text: '깊이와 승리 색상 확인',
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
        invalidAnswer: '정답 형식이 올바르지 않습니다. 예: h8h9i8.',
        addFailed: '문제 추가에 실패했습니다.',
        added: '문제를 추가했습니다.{suffix}',
        appendedSuffix: ' (마지막 순서로 추가됨)',
        updateFailed: '문제 수정에 실패했습니다.',
        updated: '문제를 수정했습니다. 문제집 작업공간으로 이동합니다.',
        deleteConfirm: '이 문제를 삭제할까요?',
        deleteFailed: '삭제에 실패했습니다.',
        failedLoadPuzzle: '문제를 불러오지 못했습니다.'
      },
      cacheList: {
        heading: 'AI 대응',
        description: '트레이닝 문제 또는 커뮤니티 문제를 먼저 선택하세요.',
        targetType: '대상 유형',
        trainingTab: '트레이닝',
        communityTab: '커뮤니티',
        trainingHeading: '트레이닝 문제',
        trainingHelp: '문제집을 선택한 뒤 문제를 선택하세요.',
        packSubheading: '문제집',
        puzzlesSubheading: '선택한 문제집의 문제',
        communityHeading: '커뮤니티 문제',
        communityHelp: '문제 번호로 검색한 뒤 AI 대응을 입력하세요.',
        puzzleNumber: '문제 번호',
        puzzleNumberPlaceholder: '문제 번호',
        authorNickname: '작성자 닉네임',
        nicknamePlaceholder: '닉네임',
        minDepth: '최소 깊이',
        maxDepth: '최대 깊이',
        cursor: '커서',
        cursorPlaceholder: '선택',
        noPacks: '문제집이 없습니다.',
        packMeta: '가격 {price} · 문제 {count}개',
        responseEntry: 'AI 대응 입력',
        openResponsePuzzle: '{number}번 AI 대응 입력',
        failedLoadList: '목록을 불러오지 못했습니다.',
        failedLoadPuzzleList: '문제 목록을 불러오지 못했습니다.',
        noPuzzles: '이 문제집에는 문제가 없습니다.',
        puzzleMeta: '깊이 {depth} · {winColor}{solved}',
        solved: ' · 해결됨',
        noCommunity: '현재 필터에 맞는 커뮤니티 문제가 없습니다.',
        communityMeta: '@{author} 깊이 {depth} · {winColor}'
      },
      cacheTraining: {
        title: 'Renzzle - 트레이닝 AI 대응',
        heading: '트레이닝 AI 대응',
        description: '이 문제집에서 문제를 선택하세요.',
        noPuzzles: '이 문제집에는 문제가 없습니다.'
      },
      cacheBoard: {
        heading: 'AI 대응 보드',
        description: '선택한 문제 보드 상태의 다음 AI 대응을 저장하거나 조회합니다.',
        boardSave: '보드 · 정답 저장',
        trainingMeta: '트레이닝 · 문제집 {packId} · 문제 {puzzleId}',
        communityMeta: '커뮤니티 · 문제 {puzzleId}',
        boardVisualization: '보드 시각화',
        boardHint: '초기 문제 돌에는 번호가 없고, 정답 돌에는 순서 번호가 표시됩니다. 입력 중인 AI 대응은 빨간 원으로 표시됩니다. currentBoard를 잠그면 다음 수 하나만 둘 수 있습니다.',
        canvasLabel: '대화형 오목 AI 대응 보드',
        resetBoard: '기본 보드로 초기화',
        setCurrentBoard: '현재 보드 설정',
        startResponseInput: 'AI 대응 입력',
        cancelCurrentBoard: '현재 보드 취소',
        cancelResponseInput: '입력 취소',
        currentBoardString: '현재 보드 문자열',
        filledByButton: '버튼으로 채워집니다',
        responseMove: 'AI 대응',
        responsePlaceholder: '보드에서 수 하나를 클릭하세요',
        answerSave: '저장',
        lookup: 'AI 대응 조회',
        aiResponse: 'AI 대응',
        close: '닫기',
        locked: 'AI 대응 입력 중입니다. 대응 수 하나만 놓으세요.',
        unlocked: '보드가 AI 대응 차례가 되면 입력이 자동으로 활성화됩니다.',
        needCurrentBoard: 'currentBoard를 설정하고 AI 대응을 입력하세요.',
        saveFailed: '저장에 실패했습니다.',
        saved: '저장했습니다.',
        setCurrentBoardFirst: 'currentBoard를 먼저 설정하세요.',
        responseTurnOnly: '사용자 수를 둔 뒤의 보드만 설정할 수 있습니다. AI 대응은 상대 차례에서만 입력합니다.',
        lookupFailed: '조회에 실패했습니다.',
        noResponse: 'AI 대응이 없습니다.'
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
    syncAllCustomSelects();
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

  let customSelectId = 0;

  function getSelectedOption(select) {
    return select.selectedOptions?.[0] || select.options[select.selectedIndex] || select.options[0] || null;
  }

  function getOptionLabel(option) {
    return option ? (option.textContent || option.value || '').trim() : '';
  }

  function closeCustomSelect(wrapper) {
    if (!wrapper) return;
    wrapper.classList.remove('is-open');
    wrapper.querySelector('.admin-custom-select-button')?.setAttribute('aria-expanded', 'false');
  }

  function closeAllCustomSelects(except) {
    document.querySelectorAll('.admin-custom-select.is-open').forEach(function (wrapper) {
      if (wrapper !== except) {
        closeCustomSelect(wrapper);
      }
    });
  }

  function syncCustomSelect(select) {
    const wrapper = select.closest('.admin-custom-select');
    if (!wrapper) return;

    const button = wrapper.querySelector('.admin-custom-select-button');
    const menu = wrapper.querySelector('.admin-custom-select-menu');
    if (!button || !menu) return;

    const selectedOption = getSelectedOption(select);
    button.textContent = getOptionLabel(selectedOption) || select.getAttribute('aria-label') || '';
    button.disabled = select.disabled;

    menu.innerHTML = '';
    Array.from(select.options).forEach(function (option, index) {
      if (option.hidden) return;
      const optionButton = document.createElement('button');
      optionButton.type = 'button';
      optionButton.className = 'admin-custom-select-option';
      optionButton.role = 'option';
      optionButton.textContent = getOptionLabel(option);
      optionButton.disabled = option.disabled;
      optionButton.setAttribute('aria-selected', String(option.selected));
      if (option.selected) {
        optionButton.classList.add('is-selected');
      }
      optionButton.addEventListener('click', function () {
        if (option.disabled) return;
        select.selectedIndex = index;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        syncCustomSelect(select);
        closeCustomSelect(wrapper);
        button.focus();
      });
      optionButton.addEventListener('keydown', function (event) {
        const enabledOptions = Array.from(menu.querySelectorAll('.admin-custom-select-option:not(:disabled)'));
        const currentIndex = enabledOptions.indexOf(optionButton);
        if (event.key === 'Escape') {
          event.preventDefault();
          closeCustomSelect(wrapper);
          button.focus();
        } else if (event.key === 'ArrowDown') {
          event.preventDefault();
          enabledOptions[Math.min(currentIndex + 1, enabledOptions.length - 1)]?.focus();
        } else if (event.key === 'ArrowUp') {
          event.preventDefault();
          enabledOptions[Math.max(currentIndex - 1, 0)]?.focus();
        } else if (event.key === 'Home') {
          event.preventDefault();
          enabledOptions[0]?.focus();
        } else if (event.key === 'End') {
          event.preventDefault();
          enabledOptions.at(-1)?.focus();
        }
      });
      menu.appendChild(optionButton);
    });
  }

  function focusSelectedCustomOption(wrapper) {
    const selected = wrapper.querySelector('.admin-custom-select-option.is-selected:not(:disabled)');
    const firstEnabled = wrapper.querySelector('.admin-custom-select-option:not(:disabled)');
    (selected || firstEnabled)?.focus();
  }

  function openCustomSelect(wrapper, focusMenu) {
    const select = wrapper.querySelector('select');
    const button = wrapper.querySelector('.admin-custom-select-button');
    if (!select || !button || select.disabled) return;
    closeAllCustomSelects(wrapper);
    syncCustomSelect(select);
    wrapper.classList.add('is-open');
    button.setAttribute('aria-expanded', 'true');
    if (focusMenu) {
      requestAnimationFrame(function () {
        focusSelectedCustomOption(wrapper);
      });
    }
  }

  function installSelectValueSync(select) {
    const valueDescriptor = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value');
    const indexDescriptor = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'selectedIndex');
    if (valueDescriptor?.configurable && !Object.hasOwn(select, 'value')) {
      Object.defineProperty(select, 'value', {
        configurable: true,
        enumerable: true,
        get: function () {
          return valueDescriptor.get.call(this);
        },
        set: function (value) {
          valueDescriptor.set.call(this, value);
          queueMicrotask(() => syncCustomSelect(this));
        }
      });
    }
    if (indexDescriptor?.configurable && !Object.hasOwn(select, 'selectedIndex')) {
      Object.defineProperty(select, 'selectedIndex', {
        configurable: true,
        enumerable: true,
        get: function () {
          return indexDescriptor.get.call(this);
        },
        set: function (value) {
          indexDescriptor.set.call(this, value);
          queueMicrotask(() => syncCustomSelect(this));
        }
      });
    }
  }

  function enhanceCustomSelect(select) {
    if (!select || select.dataset.adminSelectEnhanced === 'true' || select.multiple) return;
    select.dataset.adminSelectEnhanced = 'true';
    installSelectValueSync(select);

    const wrapper = document.createElement('div');
    wrapper.className = 'admin-custom-select';
    if (Object.hasOwn(select.dataset, 'adminLanguageSelect')) {
      wrapper.classList.add('admin-custom-select-language');
    }
    if (select.classList.contains('pack-language-select')) {
      wrapper.classList.add('admin-custom-select-compact');
    }

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'admin-custom-select-button';
    button.setAttribute('aria-haspopup', 'listbox');
    button.setAttribute('aria-expanded', 'false');

    const menu = document.createElement('div');
    const menuId = 'adminCustomSelectMenu' + (++customSelectId);
    menu.id = menuId;
    menu.className = 'admin-custom-select-menu';
    menu.role = 'listbox';
    button.setAttribute('aria-controls', menuId);

    select.parentNode.insertBefore(wrapper, select);
    wrapper.appendChild(select);
    wrapper.appendChild(button);
    wrapper.appendChild(menu);
    select.classList.add('admin-native-select');
    if (select.id) {
      button.id = select.id + 'CustomButton';
    }

    button.addEventListener('click', function () {
      if (wrapper.classList.contains('is-open')) {
        closeCustomSelect(wrapper);
      } else {
        openCustomSelect(wrapper, false);
      }
    });
    button.addEventListener('keydown', function (event) {
      if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        openCustomSelect(wrapper, true);
      } else if (event.key === 'Escape') {
        closeCustomSelect(wrapper);
      }
    });
    select.addEventListener('change', function () {
      syncCustomSelect(select);
    });
    select.addEventListener('focus', function () {
      button.focus();
    });
    new MutationObserver(function () {
      syncCustomSelect(select);
    }).observe(select, { childList: true, subtree: true, attributes: true, attributeFilter: ['disabled', 'label', 'selected', 'value'] });
    syncCustomSelect(select);
  }

  function enhanceCustomSelects(root) {
    const scope = root || document;
    const selects = scope.querySelectorAll ? scope.querySelectorAll('select') : [];
    selects.forEach(enhanceCustomSelect);
  }

  function syncAllCustomSelects() {
    document.querySelectorAll('select[data-admin-select-enhanced="true"]').forEach(syncCustomSelect);
  }

  document.addEventListener('DOMContentLoaded', function () {
    enhanceCustomSelects(document);
    applyI18n(document);
    document.querySelectorAll('[data-admin-language-select]').forEach(function (select) {
      select.addEventListener('change', function () {
        setLanguage(select.value);
      });
    });
    document.addEventListener('click', function (event) {
      if (!event.target.closest('.admin-custom-select')) {
        closeAllCustomSelects();
      }
    });
    globalThis.addEventListener('resize', function () {
      closeAllCustomSelects();
    });
  });

  globalThis.adminMessages = messages;
  globalThis.adminT = t;
  globalThis.adminSetLanguage = setLanguage;
  globalThis.adminGetLanguage = function () { return currentLanguage; };
  globalThis.adminApplyI18n = applyI18n;
  globalThis.adminSetText = setText;
  globalThis.adminEnhanceSelects = enhanceCustomSelects;
  globalThis.adminRefreshSelects = syncAllCustomSelects;
})();
