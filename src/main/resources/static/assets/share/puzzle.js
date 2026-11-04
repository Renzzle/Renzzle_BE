// © 2026 Studio Isoo. All rights reserved.
(function () {
  const LINES = 15;
  const LOGICAL_SIZE = 480;
  // As the app's board: margins of 0.6 cell, stones 94% of a cell
  const CELL = LOGICAL_SIZE / (LINES - 1 + 1.2);
  const ORIGIN = CELL * 0.6;
  const STONE_RADIUS = CELL * 0.47;
  // One pixel of the app design, which draws a cell 24px wide
  const PX = CELL / 24;
  const FONT = '"Pretendard Variable", Pretendard, system-ui, sans-serif';
  const ENGINE_READY_TIMEOUT_MS = 8000;
  const NEXT_MOVES_TIMEOUT_MS = 5000;
  const THINKING_DELAY_MS = 400;
  const BASE64_URL_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
  // Must match SharedMovesEncoder on the server
  const SHUFFLED_ALPHABET = 'WyR9_QfXoZ7S1kBUMiOes4dacthv0rj3gYVJz5GAmNEIwCD-682TbnHqLFuxpKlP';
  // The app's board
  const COLORS = {
    board: '#E7C99A',
    label: 'rgba(38, 36, 31, 0.42)',
    black: '#2C2A25',
    white: '#FFFEFB',
    last: '#269573',
    selected: '#268064',
    win: '#268064',
    lose: '#9C412D',
    cross: '#AF5744'
  };
  // Phosphor Icons (MIT)
  const LEADS = {
    idle: '<svg width="16" height="16" viewBox="0 0 256 256" fill="currentColor" aria-hidden="true"><path d="M196,84a31.94,31.94,0,0,0-11.22,2A32,32,0,0,0,148,69V44a32,32,0,0,0-64,0v66.83A32,32,0,0,0,32.25,148l4.68,8.24C71.11,216.48,86.72,244,136,244a92.1,92.1,0,0,0,92-92V116A32,32,0,0,0,196,84Zm8,68a68.08,68.08,0,0,1-68,68c-34,0-43.49-14.45-78.2-75.65l-4.69-8.28a.16.16,0,0,1,0-.07,8,8,0,0,1,13.86-8c.06.12.13.23.2.35l18.68,30A12,12,0,0,0,108,152V44a8,8,0,0,1,16,0v68a12,12,0,0,0,24,0V100a8,8,0,0,1,16,0v20a12,12,0,0,0,24,0v-4a8,8,0,0,1,16,0Z"/></svg>',
    thinking: '<span class="dots" aria-hidden="true"><i></i><i></i><i></i></span>',
    success: '<svg width="16" height="16" viewBox="0 0 256 256" fill="currentColor" aria-hidden="true"><path d="M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm45.66,85.66-56,56a8,8,0,0,1-11.32,0l-24-24a8,8,0,0,1,11.32-11.32L112,148.69l50.34-50.35a8,8,0,0,1,11.32,11.32Z"/></svg>',
    danger: '<svg width="16" height="16" viewBox="0 0 256 256" fill="currentColor" aria-hidden="true"><path d="M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm37.66,130.34a8,8,0,0,1-11.32,11.32L128,139.31l-26.34,26.35a8,8,0,0,1-11.32-11.32L116.69,128,90.34,101.66a8,8,0,0,1,11.32-11.32L128,116.69l26.34-26.35a8,8,0,0,1,11.32,11.32L139.31,128Z"/></svg>'
  };

  // Keyed by the page's lang, which the server picks from Accept-Language (SharedPuzzlePageText). Results stay short,
  // as the line or X on the board already shows what happened
  const MESSAGES = {
    ko: {
      loading: 'AI를 준비하고 있어요',
      turn: '두 번 눌러 돌을 둬요',
      confirm: '한 번 더 누르면 둬요',
      thinking: '다음 수를 생각하고 있어요',
      solved: '성공했어요',
      failed: '실패했어요',
      free: 'AI를 쓸 수 없어 직접 두어 볼 수 있어요'
    },
    en: {
      loading: 'Getting the AI ready',
      turn: 'Tap twice to place a stone',
      confirm: 'Tap again to place it',
      thinking: 'Thinking of a move',
      solved: 'Solved',
      failed: 'Failed',
      free: 'The AI is unavailable, so place stones freely'
    },
    ja: {
      loading: 'AIを準備しています',
      turn: '2回タップして石を置きます',
      confirm: 'もう一度タップすると置きます',
      thinking: '次の手を考えています',
      solved: '成功しました',
      failed: '失敗しました',
      free: 'AIが使えないため、自由に石を置けます'
    },
    'zh-Hans': {
      loading: '正在准备 AI',
      turn: '点两下落子',
      confirm: '再点一下落子',
      thinking: '正在思考下一步',
      solved: '解题成功',
      failed: '解题失败',
      free: 'AI 无法使用，可以自由落子'
    },
    'zh-Hant': {
      loading: '正在準備 AI',
      turn: '點兩下落子',
      confirm: '再點一下落子',
      thinking: '正在思考下一步',
      solved: '解題成功',
      failed: '解題失敗',
      free: 'AI 無法使用，可以自由落子'
    },
    ru: {
      loading: 'Готовим ИИ',
      turn: 'Коснитесь дважды, чтобы поставить камень',
      confirm: 'Коснитесь ещё раз, чтобы поставить',
      thinking: 'Обдумывает ход',
      solved: 'Решено',
      failed: 'Не решено',
      free: 'ИИ недоступен, камни можно ставить свободно'
    },
    et: {
      loading: 'Tehisintellekt valmistub',
      turn: 'Kivi asetamiseks puuduta kaks korda',
      confirm: 'Asetamiseks puuduta veel kord',
      thinking: 'Mõtleb järgmise käigu üle',
      solved: 'Lahendatud',
      failed: 'Ei õnnestunud',
      free: 'Tehisintellekt ei tööta, kive saab vabalt asetada'
    }
  };
  const text = MESSAGES[document.documentElement.lang] || MESSAGES.en;

  const token = document.body.dataset.token;
  const puzzleBoard = document.body.dataset.board;
  const puzzleMoveCount = shareBoard.parseMoves(puzzleBoard).length;

  const canvas = document.getElementById('board');
  const ctx = canvas.getContext('2d');
  const statusLine = document.getElementById('status');
  const retryButton = document.getElementById('retryButton');

  // mode: loading, solve, thinking, over, free
  const state = {
    mode: 'loading',
    sequence: puzzleBoard,
    selected: null,
    mark: null,
    over: false,
    notice: null,
    turn: 0
  };
  let engineAvailable = true;
  const checkWinRequests = new Map();
  const nextMovesRequests = new Map();

  function movesOf(sequence) {
    return shareBoard.parseMoves(sequence);
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

  function toX(col) {
    return ORIGIN + col * CELL;
  }

  function toY(row) {
    return ORIGIN + (LINES - 1 - row) * CELL;
  }

  function render() {
    paintWood();
    shareBoard.paintBoard(ctx, { origin: ORIGIN, cell: CELL });
    paintLabels();

    const moves = movesOf(state.sequence);
    const fiveLine = state.mark?.fiveLine;
    const forbidden = state.mark?.forbidden;
    const numbered = state.mode === 'free';
    moves.forEach((move, index) => {
      const number = numbered && index >= puzzleMoveCount ? index - puzzleMoveCount + 1 : null;
      paintStone(toX(move.col), toY(move.row), move.player, number);
      if (index === moves.length - 1 && number == null && !fiveLine && !forbidden) {
        paintLastMove(move);
      }
    });

    if (fiveLine) {
      paintFiveLine(fiveLine, state.mark.lost ? COLORS.lose : COLORS.win);
    }
    if (forbidden && moves.length > 0) {
      paintCross(moves.at(-1));
    }
    if (state.selected) {
      paintSelection(state.selected);
    }
  }

  // A faint light a little above the middle, so the flat wood doesn't look heavy
  function paintWood() {
    ctx.fillStyle = COLORS.board;
    ctx.fillRect(0, 0, LOGICAL_SIZE, LOGICAL_SIZE);
    ctx.save();
    ctx.translate(LOGICAL_SIZE / 2, LOGICAL_SIZE * 0.3);
    ctx.scale(1.2, 0.9);
    const light = ctx.createRadialGradient(0, 0, 0, 0, 0, LOGICAL_SIZE);
    light.addColorStop(0, 'rgba(255, 255, 255, 0.2)');
    light.addColorStop(0.62, 'rgba(255, 255, 255, 0)');
    ctx.fillStyle = light;
    ctx.fillRect(-LOGICAL_SIZE, -LOGICAL_SIZE, LOGICAL_SIZE * 2, LOGICAL_SIZE * 2);
    ctx.restore();
  }

  function paintLabels() {
    ctx.fillStyle = COLORS.label;
    ctx.font = '600 ' + 8 * PX + 'px ' + FONT;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    for (let i = 0; i < LINES; i += 1) {
      ctx.fillText(String.fromCodePoint(65 + i), toX(i), LOGICAL_SIZE - ORIGIN / 2);
      ctx.fillText(String(i + 1), ORIGIN / 2, toY(i));
    }
  }

  function circle(x, y, r) {
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
  }

  // Flat, with depth only at the edges as in the app: a 1.5px drop, a 2px darker bottom, a light top on black
  function paintStone(x, y, player, number) {
    const black = player === 'black';
    const r = STONE_RADIUS;
    ctx.fillStyle = black ? 'rgba(30, 24, 16, 0.32)' : 'rgba(30, 24, 16, 0.24)';
    circle(x, y + 1.5 * PX, r);
    ctx.fill();
    ctx.fillStyle = black ? COLORS.black : COLORS.white;
    circle(x, y, r);
    ctx.fill();

    ctx.save();
    circle(x, y, r);
    ctx.clip();
    paintEdge(x, y, r, 2 * PX, black ? 'rgba(0, 0, 0, 0.42)' : 'rgba(38, 36, 31, 0.16)');
    if (black) {
      paintEdge(x, y, r, -2 * PX, 'rgba(255, 255, 255, 0.12)');
    } else {
      ctx.strokeStyle = 'rgba(38, 36, 31, 0.66)';
      ctx.lineWidth = 2 * PX;
      circle(x, y, r);
      ctx.stroke();
    }
    ctx.restore();

    if (number != null) {
      ctx.fillStyle = black ? '#F7F5F0' : '#26241F';
      ctx.font = '700 ' + 12 * PX + 'px ' + FONT;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(String(number), x, y);
    }
  }

  // The part of the stone that the same circle moved up by shift leaves bare: the bottom edge, or the top for a negative shift
  function paintEdge(x, y, r, shift, color) {
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.arc(x, y - shift, r, 0, Math.PI * 2);
    ctx.fillStyle = color;
    ctx.fill('evenodd');
  }

  function paintLastMove(move) {
    ctx.fillStyle = COLORS.last;
    circle(toX(move.col), toY(move.row), STONE_RADIUS * 0.3);
    ctx.fill();
  }

  // The first tap marks the spot with a ring as large as a stone
  function paintSelection({ row, col }) {
    ctx.strokeStyle = COLORS.selected;
    ctx.lineWidth = 1.75 * PX;
    circle(toX(col), toY(row), STONE_RADIUS - 0.875 * PX);
    ctx.stroke();
  }

  // One line over the five stones, reaching a little past both ends
  function paintFiveLine(line, color) {
    const from = { x: toX(line[0].col), y: toY(line[0].row) };
    const to = { x: toX(line.at(-1).col), y: toY(line.at(-1).row) };
    const length = Math.hypot(to.x - from.x, to.y - from.y);
    const reachX = ((to.x - from.x) / length) * CELL * 0.35;
    const reachY = ((to.y - from.y) / length) * CELL * 0.35;
    ctx.strokeStyle = color;
    ctx.lineWidth = 4 * PX;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(from.x - reachX, from.y - reachY);
    ctx.lineTo(to.x + reachX, to.y + reachY);
    ctx.stroke();
  }

  function paintCross(move) {
    const x = toX(move.col);
    const y = toY(move.row);
    const reach = CELL * 0.24;
    ctx.strokeStyle = COLORS.cross;
    ctx.lineWidth = 3 * PX;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(x - reach, y - reach);
    ctx.lineTo(x + reach, y + reach);
    ctx.moveTo(x + reach, y - reach);
    ctx.lineTo(x - reach, y + reach);
    ctx.stroke();
  }

  // tone: idle, thinking, success, danger, or muted for a plain line
  function setStatus(message, tone) {
    statusLine.dataset.tone = tone;
    statusLine.innerHTML = LEADS[tone] || '';
    statusLine.append(message);
  }

  function updateControls() {
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
    const timer = setTimeout(() => {
      if (turn === state.turn) {
        setStatus(text.thinking, 'thinking');
      }
    }, THINKING_DELAY_MS);
    return () => clearTimeout(timer);
  }

  function finish(message, tone, mark) {
    state.mode = 'over';
    state.mark = mark;
    setStatus(message, tone);
    updateControls();
    render();
  }

  function fiveLineOf(sequence) {
    return shareBoard.findFiveLine(movesOf(sequence));
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
        finish(text.failed, 'danger', { fiveLine: null, forbidden: true });
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
        finish(text.failed, 'danger', { fiveLine: fiveLineOf(afterAi), forbidden: false, lost: true });
        return;
      }
      if (aiResult === 2) {
        finish(text.solved, 'success', { fiveLine: null, forbidden: true });
        return;
      }

      prefetchNextMoves(afterAi);
      state.mode = 'solve';
      setStatus(text.turn, 'idle');
      updateControls();
      render();
    } catch (error) {
      if (turn === state.turn && !renzzleEngine.isCancelled(error)) {
        startFree(text.free, state.sequence);
      }
    } finally {
      stopThinking();
    }
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

  function showFree(sequence) {
    state.turn += 1;
    state.sequence = sequence;
    state.selected = null;
    state.mark = null;
    state.over = false;
    setStatus(state.notice, 'muted');
    updateControls();
    render();
    void markPosition(state.turn, sequence);
  }

  // judgePosition never rejects; a result for a position the user already left is dropped
  async function markPosition(turn, sequence) {
    const mark = await judgePosition(sequence);
    if (turn !== state.turn || !mark) {
      return;
    }
    state.mark = mark;
    state.over = true;
    render();
  }

  function startSolve() {
    state.turn += 1;
    state.mode = 'solve';
    state.sequence = puzzleBoard;
    state.selected = null;
    state.mark = null;
    state.over = false;
    prefetchNextMoves(puzzleBoard);
    setStatus(text.turn, 'idle');
    updateControls();
    render();
  }

  function startFree(notice, sequence) {
    engineAvailable = false;
    state.mode = 'free';
    state.notice = notice;
    showFree(sequence);
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
    return state.mode === 'solve' || (state.mode === 'free' && !state.over);
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
        setStatus(text.confirm, 'muted');
      }
      render();
      return;
    }

    state.selected = null;
    if (state.mode === 'solve') {
      void playUserMove(cellIndex(cell.row, cell.col));
    } else {
      showFree(state.sequence + moveString(cell.row, cell.col));
    }
  });

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
    shareBoard.watch(canvas, LOGICAL_SIZE, render);
    setStatus(text.loading, 'thinking');
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
