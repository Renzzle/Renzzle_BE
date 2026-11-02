// © 2026 Studio Isoo. All rights reserved.
(function () {
  const LINES = 15;
  const LOGICAL_SIZE = 480;
  const CELL = LOGICAL_SIZE / (LINES - 1 + 1.6);
  const ORIGIN = CELL * 0.8;
  const STONE_RADIUS = CELL * adminBoard.STONE_RATIO;
  const ENGINE_READY_TIMEOUT_MS = 8000;
  const NEXT_MOVES_TIMEOUT_MS = 5000;
  const THINKING_DELAY_MS = 400;
  const THINKING_DEEP_MS = 3000;
  const BASE64_URL_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
  // Must match SharedMovesEncoder on the server
  const SHUFFLED_ALPHABET = 'WyR9_QfXoZ7S1kBUMiOes4dacthv0rj3gYVJz5GAmNEIwCD-682TbnHqLFuxpKlP';
  // Flat board as in the app; SharedPuzzlePreviewRenderer on the server draws the link preview in these colors
  const BOARD_COLORS = {
    woodFrom: '#FBEFE1',
    woodTo: '#FBEFE1',
    line: '#767D86',
    frame: '#767D86',
    star: '#767D86',
    black: '#111111',
    white: '#FFFFFF',
    whiteEdge: '#767D86'
  };
  Object.assign(adminBoard.COLORS, BOARD_COLORS);

  const MESSAGES = {
    ko: {
      loading: 'AI를 준비하고 있어요',
      turn: { black: '흑 차례예요. 오목을 완성해 보세요', white: '백 차례예요. 오목을 완성해 보세요' },
      confirm: '한 번 더 누르면 돌을 놓아요',
      thinking: '다음 수를 생각하고 있어요',
      thinkingDeep: '신중하게 두는 중이에요',
      solved: '퍼즐 풀기 성공! 오목을 완성했어요',
      failed: '퍼즐 풀기 실패',
      failedFive: '퍼즐 풀기 실패. 상대가 오목을 완성했어요',
      failedForbidden: '퍼즐 풀기 실패. 금수 자리예요',
      review: '검토 중이에요. 자유롭게 두어 볼 수 있어요',
      five: '오목 완성',
      forbidden: '금수',
      free: '이 브라우저에서는 AI를 쓸 수 없어서 직접 두어 보기로 열었어요',
      aiStopped: 'AI가 멈춰서 직접 두어 보기로 바꿨어요'
    },
    en: {
      loading: 'Getting the AI ready',
      turn: { black: 'Black to move. Make five in a row', white: 'White to move. Make five in a row' },
      confirm: 'Tap again to place the stone',
      thinking: 'Thinking of a move',
      thinkingDeep: 'Taking its time',
      solved: 'Puzzle solved! Five in a row',
      failed: 'Puzzle failed',
      failedFive: 'Puzzle failed. The opponent made five',
      failedForbidden: 'Puzzle failed. That is a forbidden move',
      review: 'Reviewing. Place stones freely',
      five: 'Five in a row',
      forbidden: 'Forbidden move',
      free: 'The AI is not available in this browser, so you can place stones freely',
      aiStopped: 'The AI stopped, so you can now place stones freely'
    }
  };
  const text = document.documentElement.lang === 'ko' ? MESSAGES.ko : MESSAGES.en;

  const token = document.body.dataset.token;
  const puzzleBoard = document.body.dataset.board;
  const puzzleMoveCount = adminBoard.parseMoves(puzzleBoard).length;

  const canvas = document.getElementById('board');
  const ctx = canvas.getContext('2d');
  const statusLine = document.getElementById('status');
  const prevButton = document.getElementById('prevButton');
  const nextButton = document.getElementById('nextButton');
  const retryButton = document.getElementById('retryButton');
  const reviewButton = document.getElementById('reviewButton');

  // mode: loading, solve, thinking, over, review, free
  const state = {
    mode: 'loading',
    sequence: puzzleBoard,
    selected: null,
    mark: null,
    over: false,
    history: [],
    historyIndex: 0,
    notice: null,
    turn: 0
  };
  let engineAvailable = true;
  const checkWinRequests = new Map();
  const nextMovesRequests = new Map();

  function movesOf(sequence) {
    return adminBoard.parseMoves(sequence);
  }

  function moveString(row, col) {
    return String.fromCodePoint(97 + col) + (row + 1);
  }

  // Engine and cache cells are column-major: col * 15 + row
  function cellIndex(row, col) {
    return col * LINES + row;
  }

  function cellOf(index) {
    return { row: index % LINES, col: Math.floor(index / LINES) };
  }

  function nextPlayer(sequence) {
    return movesOf(sequence).length % 2 === 0 ? 'black' : 'white';
  }

  function toX(col) {
    return ORIGIN + col * CELL;
  }

  function toY(row) {
    return ORIGIN + (LINES - 1 - row) * CELL;
  }

  function render() {
    adminBoard.paintBoard(ctx, { size: LOGICAL_SIZE, origin: ORIGIN, cell: CELL, labels: false });
    const moves = movesOf(state.sequence);

    const fiveLine = state.mark?.fiveLine;
    if (fiveLine) {
      const first = fiveLine[0];
      const last = fiveLine.at(-1);
      adminBoard.paintLineBand(ctx, { x: toX(first.col), y: toY(first.row) },
        { x: toX(last.col), y: toY(last.row) }, STONE_RADIUS);
    }

    const numbered = state.mode === 'review' || state.mode === 'free';
    moves.forEach((move, index) => {
      const number = numbered && index >= puzzleMoveCount ? index - puzzleMoveCount + 1 : null;
      adminBoard.paintStone(ctx, toX(move.col), toY(move.row), STONE_RADIUS, move.player, number);
    });

    const last = moves.at(-1);
    if (last && state.mark?.forbidden) {
      paintCross(last);
    } else if (last && !numbered && moves.length > puzzleMoveCount) {
      adminBoard.paintRing(ctx, toX(last.col), toY(last.row), STONE_RADIUS, adminBoard.COLORS.mark);
    }

    if (state.selected) {
      const { row, col } = state.selected;
      ctx.save();
      ctx.globalAlpha = 0.45;
      adminBoard.paintStone(ctx, toX(col), toY(row), STONE_RADIUS, nextPlayer(state.sequence), null);
      ctx.restore();
      adminBoard.paintRing(ctx, toX(col), toY(row), STONE_RADIUS, adminBoard.COLORS.preview);
    }
  }

  function paintCross(move) {
    const x = toX(move.col);
    const y = toY(move.row);
    const reach = STONE_RADIUS * 0.6;
    ctx.strokeStyle = adminBoard.COLORS.mark;
    ctx.lineWidth = STONE_RADIUS * 0.22;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(x - reach, y - reach);
    ctx.lineTo(x + reach, y + reach);
    ctx.moveTo(x + reach, y - reach);
    ctx.lineTo(x - reach, y + reach);
    ctx.stroke();
  }

  function setStatus(message, tone, thinking) {
    statusLine.textContent = message;
    statusLine.dataset.tone = tone || '';
    statusLine.dataset.thinking = thinking ? 'true' : 'false';
  }

  function updateControls() {
    const browsing = state.mode === 'review' || state.mode === 'free';
    prevButton.hidden = !browsing;
    nextButton.hidden = !browsing;
    prevButton.disabled = state.historyIndex <= 0;
    nextButton.disabled = state.historyIndex >= state.history.length - 1;
    reviewButton.hidden = state.mode !== 'over';
    retryButton.disabled = state.mode === 'loading';
  }

  function withTimeout(promise, ms) {
    let timer;
    const timeout = new Promise((_, reject) => {
      timer = setTimeout(() => reject(new Error('Timed out')), ms);
    });
    return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
  }

  // 0 ongoing, 1 the last move won, 2 black lost on a forbidden move
  function judge(sequence) {
    if (!checkWinRequests.has(sequence)) {
      const request = renzzleEngine.checkWin(sequence);
      checkWinRequests.set(sequence, request);
      request.catch(() => checkWinRequests.delete(sequence));
    }
    return checkWinRequests.get(sequence);
  }

  function prefetchJudge(sequence) {
    if (engineAvailable) {
      judge(sequence).catch(() => {});
    }
  }

  function nextMovesFor(userTurnBoard) {
    if (!nextMovesRequests.has(userTurnBoard)) {
      nextMovesRequests.set(userTurnBoard, fetchNextMoves(userTurnBoard).catch(() => new Map()));
    }
    return nextMovesRequests.get(userTurnBoard);
  }

  // Fire and forget: nextMovesFor already turns a failed request into an empty map
  function prefetchNextMoves(userTurnBoard) {
    void nextMovesFor(userTurnBoard);
  }

  async function fetchNextMoves(userTurnBoard) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), NEXT_MOVES_TIMEOUT_MS);
    try {
      const url = '/puzzle/' + encodeURIComponent(token) + '/next-moves?board=' + encodeURIComponent(userTurnBoard);
      const response = await fetch(url, { signal: controller.signal });
      if (!response.ok) {
        return new Map();
      }
      const body = await response.json();
      return decodeCandidates(body.response || '', userTurnBoard);
    } finally {
      clearTimeout(timer);
    }
  }

  // Undoes SharedMovesEncoder: shuffled base64url, XORed byte pairs of (user cell, AI cell)
  async function decodeCandidates(encoded, userTurnBoard) {
    const candidates = new Map();
    if (!encoded || !globalThis.crypto?.subtle) {
      return candidates;
    }
    let base64 = '';
    for (const char of encoded) {
      base64 += BASE64_URL_ALPHABET.charAt(SHUFFLED_ALPHABET.indexOf(char));
    }
    const padded = base64.replaceAll('-', '+').replaceAll('_', '/') + '='.repeat((4 - (base64.length % 4)) % 4);
    const bytes = Uint8Array.from(atob(padded), (char) => char.codePointAt(0));
    const keystream = await keystreamOf(new TextEncoder().encode(token + userTurnBoard), bytes.length);
    for (let i = 0; i + 1 < bytes.length; i += 2) {
      candidates.set(bytes[i] ^ keystream[i], bytes[i + 1] ^ keystream[i + 1]);
    }
    return candidates;
  }

  // SHA-256 of the seed, then of each previous block
  async function keystreamOf(seed, length, blocks = []) {
    if (blocks.length * 32 >= length) {
      const keystream = new Uint8Array(blocks.length * 32);
      blocks.forEach((block, index) => keystream.set(block, index * 32));
      return keystream;
    }
    const previous = blocks.at(-1) ?? seed;
    const block = new Uint8Array(await crypto.subtle.digest('SHA-256', previous));
    return keystreamOf(seed, length, blocks.concat([block]));
  }

  async function aiReply(userTurnBoard, userCell, sequence) {
    const candidates = await nextMovesFor(userTurnBoard);
    if (candidates.has(userCell)) {
      return candidates.get(userCell);
    }
    return renzzleEngine.nextMove(sequence);
  }

  function showThinkingSoon(turn) {
    const shortTimer = setTimeout(() => {
      if (turn === state.turn) {
        setStatus(text.thinking, 'muted', true);
      }
    }, THINKING_DELAY_MS);
    const deepTimer = setTimeout(() => {
      if (turn === state.turn) {
        setStatus(text.thinkingDeep, 'muted', true);
      }
    }, THINKING_DEEP_MS);
    return () => {
      clearTimeout(shortTimer);
      clearTimeout(deepTimer);
    };
  }

  function finish(message, tone, mark) {
    state.mode = 'over';
    state.mark = mark;
    setStatus(message, tone, false);
    updateControls();
    render();
  }

  function fiveLineOf(sequence) {
    return adminBoard.findFiveLine(movesOf(sequence));
  }

  async function playUserMove(userCell) {
    const turn = state.turn;
    const userTurnBoard = state.sequence;
    const { row, col } = cellOf(userCell);
    const sequence = userTurnBoard + moveString(row, col);
    state.sequence = sequence;
    state.mode = 'thinking';
    updateControls();
    render();

    let stopThinking = () => {};
    try {
      const userResult = await judge(sequence);
      if (turn !== state.turn) {
        return;
      }
      if (userResult === 1) {
        finish(text.solved, 'success', { fiveLine: fiveLineOf(sequence), forbidden: false });
        return;
      }
      if (userResult === 2) {
        finish(text.failedForbidden, 'danger', { fiveLine: null, forbidden: true });
        return;
      }

      stopThinking = showThinkingSoon(turn);
      const aiCell = await aiReply(userTurnBoard, userCell, sequence);
      if (turn !== state.turn) {
        return;
      }
      if (aiCell < 0) {
        finish(text.failed, 'danger', null);
        return;
      }

      const aiMove = cellOf(aiCell);
      const afterAi = sequence + moveString(aiMove.row, aiMove.col);
      const aiResult = await judge(afterAi);
      if (turn !== state.turn) {
        return;
      }
      state.sequence = afterAi;
      if (aiResult === 1) {
        finish(text.failedFive, 'danger', { fiveLine: fiveLineOf(afterAi), forbidden: false });
        return;
      }
      if (aiResult === 2) {
        finish(text.solved, 'success', { fiveLine: null, forbidden: true });
        return;
      }

      prefetchNextMoves(afterAi);
      state.mode = 'solve';
      setStatus(text.turn[nextPlayer(afterAi)], '', false);
      updateControls();
      render();
    } catch (error) {
      if (turn === state.turn && !renzzleEngine.isCancelled(error)) {
        startFree(text.aiStopped, state.sequence);
      }
    } finally {
      stopThinking();
    }
  }

  function placeInHistory(sequence) {
    state.history = state.history.slice(0, state.historyIndex + 1).concat([sequence]);
    state.historyIndex = state.history.length - 1;
    showPosition();
  }

  async function judgePosition(sequence) {
    if (engineAvailable) {
      try {
        const result = await judge(sequence);
        if (result === 1) {
          return { fiveLine: fiveLineOf(sequence), forbidden: false };
        }
        return result === 2 ? { fiveLine: null, forbidden: true } : null;
      } catch (error) {
        if (renzzleEngine.isCancelled(error)) {
          return null;
        }
      }
    }
    const fiveLine = fiveLineOf(sequence);
    return fiveLine ? { fiveLine, forbidden: false } : null;
  }

  function showPosition() {
    state.turn += 1;
    state.sequence = state.history[state.historyIndex];
    state.selected = null;
    state.mark = null;
    state.over = false;
    setStatus(state.mode === 'free' ? state.notice : text.review, 'muted', false);
    updateControls();
    render();
    void markPosition(state.turn, state.sequence);
  }

  // judgePosition never rejects; a result for a position the user already left is dropped
  async function markPosition(turn, sequence) {
    const mark = await judgePosition(sequence);
    if (turn !== state.turn || !mark) {
      return;
    }
    state.mark = mark;
    state.over = true;
    setStatus(mark.forbidden ? text.forbidden : text.five, mark.forbidden ? 'danger' : 'success', false);
    render();
  }

  function historyFrom(sequence) {
    const history = [puzzleBoard];
    movesOf(sequence).slice(puzzleMoveCount).forEach((move) => {
      history.push(history.at(-1) + moveString(move.row, move.col));
    });
    return history;
  }

  function startSolve() {
    state.turn += 1;
    state.mode = 'solve';
    state.sequence = puzzleBoard;
    state.selected = null;
    state.mark = null;
    state.over = false;
    prefetchNextMoves(puzzleBoard);
    setStatus(text.turn[nextPlayer(puzzleBoard)], '', false);
    updateControls();
    render();
  }

  function startReview() {
    state.mode = 'review';
    state.history = historyFrom(state.sequence);
    state.historyIndex = state.history.length - 1;
    state.history.forEach(prefetchJudge);
    showPosition();
  }

  function startFree(notice, sequence) {
    engineAvailable = false;
    state.mode = 'free';
    state.notice = notice;
    state.history = historyFrom(sequence);
    state.historyIndex = state.history.length - 1;
    showPosition();
  }

  function cellAt(event) {
    const rect = canvas.getBoundingClientRect();
    const scale = LOGICAL_SIZE / rect.width;
    const col = Math.round(((event.clientX - rect.left) * scale - ORIGIN) / CELL);
    const row = LINES - 1 - Math.round(((event.clientY - rect.top) * scale - ORIGIN) / CELL);
    if (row < 0 || row >= LINES || col < 0 || col >= LINES) {
      return null;
    }
    return { row, col };
  }

  function canPlace() {
    return state.mode === 'solve' || ((state.mode === 'review' || state.mode === 'free') && !state.over);
  }

  // The first tap only selects, as in the app, so a slip on a small board doesn't place a stone
  canvas.addEventListener('click', (event) => {
    const cell = cellAt(event);
    if (!cell || !canPlace()) {
      return;
    }
    if (movesOf(state.sequence).some((move) => move.row === cell.row && move.col === cell.col)) {
      return;
    }

    const selected = state.selected;
    if (selected?.row !== cell.row || selected?.col !== cell.col) {
      state.selected = cell;
      prefetchJudge(state.sequence + moveString(cell.row, cell.col));
      if (state.mode === 'solve') {
        setStatus(text.confirm, 'muted', false);
      }
      render();
      return;
    }

    state.selected = null;
    if (state.mode === 'solve') {
      void playUserMove(cellIndex(cell.row, cell.col));
    } else {
      placeInHistory(state.sequence + moveString(cell.row, cell.col));
    }
  });

  prevButton.addEventListener('click', () => {
    if (state.historyIndex > 0) {
      state.historyIndex -= 1;
      showPosition();
    }
  });

  nextButton.addEventListener('click', () => {
    if (state.historyIndex < state.history.length - 1) {
      state.historyIndex += 1;
      showPosition();
    }
  });

  reviewButton.addEventListener('click', startReview);

  retryButton.addEventListener('click', () => {
    if (state.mode === 'thinking') {
      renzzleEngine.cancel();
    }
    if (engineAvailable) {
      startSolve();
    } else {
      startFree(state.notice, puzzleBoard);
    }
  });

  function boot() {
    adminBoard.watch(canvas, LOGICAL_SIZE, render);
    setStatus(text.loading, 'muted', true);
    updateControls();
    render();
    void document.fonts?.ready.then(render);
    prefetchNextMoves(puzzleBoard);
    withTimeout(judge(puzzleBoard), ENGINE_READY_TIMEOUT_MS).then(() => startSolve(), () => {
      renzzleEngine.cancel();
      startFree(text.free, puzzleBoard);
    });
  }

  boot();
})();
