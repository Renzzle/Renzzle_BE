(function () {
  const LINES = 15;
  const STAR_POINTS = [[3, 3], [3, 11], [7, 7], [11, 3], [11, 11]];
  const STONE_RATIO = 0.46;
  const FONT = '"Pretendard Variable", Pretendard, system-ui, sans-serif';
  const COLORS = {
    woodFrom: '#f3e2b9',
    woodTo: '#e8cf9c',
    line: 'rgba(98, 70, 38, 0.42)',
    frame: 'rgba(98, 70, 38, 0.78)',
    star: 'rgba(88, 62, 32, 0.85)',
    label: 'rgba(88, 62, 32, 0.72)',
    black: '#18181b',
    white: '#ffffff',
    whiteEdge: 'rgba(24, 24, 27, 0.45)',
    mark: '#a63a32',
    band: 'rgba(166, 58, 50, 0.22)',
    preview: '#16a34a'
  };
  // Previews have no coordinate labels, so the margin only needs to fit edge stones
  const PREVIEW_SIZE = 240;
  const PREVIEW_CELL = PREVIEW_SIZE / (LINES - 1 + 1.6);

  const redrawByCanvas = new WeakMap();
  const resizeObserver = typeof ResizeObserver === 'function'
    ? new ResizeObserver(entries => entries.forEach(entry => redrawByCanvas.get(entry.target)?.()))
    : null;

  // Two-digit rows first, or "i10" would read as i1
  function parseMoves(boardStatus, startMoveIndex) {
    const moves = [];
    const regex = /([a-o])(1[0-5]|[1-9])/gi;
    let moveIndex = startMoveIndex || 0;
    let match;
    while ((match = regex.exec(boardStatus || '')) !== null) {
      moves.push({
        row: Number(match[2]) - 1,
        col: match[1].toLowerCase().charCodeAt(0) - 97,
        player: moveIndex % 2 === 0 ? 'black' : 'white'
      });
      moveIndex += 1;
    }
    return moves;
  }

  // Backing store follows the displayed size, so drawing keeps logical units but stays sharp
  function fitCanvas(canvas, logicalSize) {
    const dpr = globalThis.devicePixelRatio || 1;
    const pixels = Math.max(1, Math.round((canvas.clientWidth || logicalSize) * dpr));
    if (canvas.width !== pixels || canvas.height !== pixels) {
      canvas.width = pixels;
      canvas.height = pixels;
    }
    const ctx = canvas.getContext('2d');
    const scale = pixels / logicalSize;
    ctx.setTransform(scale, 0, 0, scale, 0, 0);
    return ctx;
  }

  // Fits now and again on every resize; redraw runs only from the observer, after the caller's state exists
  function watch(canvas, logicalSize, redraw) {
    fitCanvas(canvas, logicalSize);
    redrawByCanvas.set(canvas, () => {
      fitCanvas(canvas, logicalSize);
      redraw();
    });
    resizeObserver?.observe(canvas);
  }

  function paintBoard(ctx, layout) {
    const { size, origin, cell, labels } = layout;
    const end = origin + cell * (LINES - 1);

    const wood = ctx.createLinearGradient(0, 0, size, size);
    wood.addColorStop(0, COLORS.woodFrom);
    wood.addColorStop(1, COLORS.woodTo);
    ctx.fillStyle = wood;
    ctx.fillRect(0, 0, size, size);

    // Lines go down in device pixels, snapped so they never smear across two pixels
    const scale = ctx.getTransform().a;
    const dpr = globalThis.devicePixelRatio || 1;
    const lineWidth = Math.max(1, Math.floor(dpr));
    const frameWidth = lineWidth + Math.max(1, Math.round(dpr * 0.5));
    const snapFor = width => value => Math.round(value * scale) + (width % 2 ? 0.5 : 0);
    const snapLine = snapFor(lineWidth);
    const snapFrame = snapFor(frameWidth);

    ctx.save();
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.strokeStyle = COLORS.line;
    ctx.lineWidth = lineWidth;
    ctx.beginPath();
    for (let i = 1; i < LINES - 1; i += 1) {
      const pos = snapLine(origin + i * cell);
      ctx.moveTo(pos, snapLine(origin));
      ctx.lineTo(pos, snapLine(end));
      ctx.moveTo(snapLine(origin), pos);
      ctx.lineTo(snapLine(end), pos);
    }
    ctx.stroke();

    ctx.strokeStyle = COLORS.frame;
    ctx.lineWidth = frameWidth;
    const frameStart = snapFrame(origin);
    const frameEnd = snapFrame(end);
    ctx.strokeRect(frameStart, frameStart, frameEnd - frameStart, frameEnd - frameStart);

    ctx.fillStyle = COLORS.star;
    const starRadius = Math.max(1.5 * dpr, cell * scale * 0.09);
    STAR_POINTS.forEach(([row, col]) => {
      ctx.beginPath();
      ctx.arc(snapLine(origin + col * cell), snapLine(origin + (LINES - 1 - row) * cell), starRadius, 0, Math.PI * 2);
      ctx.fill();
    });
    ctx.restore();

    if (labels) {
      ctx.fillStyle = COLORS.label;
      ctx.font = '500 ' + Math.round(cell * 0.42) + 'px ' + FONT;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      for (let i = 0; i < LINES; i += 1) {
        ctx.fillText(String.fromCharCode(65 + i), origin + i * cell, size - origin / 2);
        ctx.fillText(String(i + 1), origin / 2, origin + (LINES - 1 - i) * cell);
      }
    }
  }

  function paintStone(ctx, x, y, r, player, number) {
    const isBlack = player === 'black';
    ctx.fillStyle = isBlack ? COLORS.black : COLORS.white;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.fill();

    if (!isBlack) {
      ctx.strokeStyle = COLORS.whiteEdge;
      ctx.lineWidth = r * 0.07;
      ctx.stroke();
    }

    if (number != null) {
      ctx.fillStyle = isBlack ? COLORS.white : COLORS.black;
      ctx.font = '650 ' + Math.round(r * 0.95) + 'px ' + FONT;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(String(number), x, y + r * 0.04);
    }
  }

  function paintRing(ctx, x, y, r, color) {
    ctx.strokeStyle = color;
    ctx.lineWidth = r * 0.16;
    ctx.beginPath();
    ctx.arc(x, y, r + r * 0.12, 0, Math.PI * 2);
    ctx.stroke();
  }

  // stones: [{ row, col, player }]; returns the first same-color run of five or more, ends in order
  function findFiveLine(stones) {
    const occupied = new Map();
    stones.forEach(stone => occupied.set(stone.row + ',' + stone.col, stone.player));
    const directions = [[0, 1], [1, 0], [1, 1], [1, -1]];
    for (const stone of stones) {
      for (const [dr, dc] of directions) {
        const line = [{ row: stone.row, col: stone.col }];
        let r = stone.row + dr, c = stone.col + dc;
        while (occupied.get(r + ',' + c) === stone.player) { line.push({ row: r, col: c }); r += dr; c += dc; }
        r = stone.row - dr; c = stone.col - dc;
        while (occupied.get(r + ',' + c) === stone.player) { line.unshift({ row: r, col: c }); r -= dr; c -= dc; }
        if (line.length >= 5) return line;
      }
    }
    return null;
  }

  // Painted before the stones, so the band groups them without covering their numbers
  function paintLineBand(ctx, from, to, r) {
    const angle = Math.atan2(to.y - from.y, to.x - from.x);
    const halfWidth = r * 1.3;
    ctx.fillStyle = COLORS.band;
    ctx.beginPath();
    ctx.arc(to.x, to.y, halfWidth, angle - Math.PI / 2, angle + Math.PI / 2);
    ctx.arc(from.x, from.y, halfWidth, angle + Math.PI / 2, angle + Math.PI * 1.5);
    ctx.closePath();
    ctx.fill();
  }

  // stones: [{ row, col, player: 'black' | 'white', number? }]
  function drawPreview(canvas, stones) {
    const render = () => {
      const ctx = fitCanvas(canvas, PREVIEW_SIZE);
      const origin = PREVIEW_CELL * 0.8;
      paintBoard(ctx, { size: PREVIEW_SIZE, origin, cell: PREVIEW_CELL, labels: false });
      (stones || []).forEach(stone => paintStone(ctx,
        origin + stone.col * PREVIEW_CELL,
        origin + (LINES - 1 - stone.row) * PREVIEW_CELL,
        PREVIEW_CELL * STONE_RATIO, stone.player, stone.number));
    };
    redrawByCanvas.set(canvas, render);
    resizeObserver?.observe(canvas);
    if (canvas.clientWidth) render();
    if (stones?.some(stone => stone.number != null) && document.fonts?.status !== 'loaded') {
      document.fonts.ready.then(render);
    }
  }

  globalThis.adminBoard = {
    COLORS,
    STONE_RATIO,
    parseMoves,
    watch,
    paintBoard,
    paintStone,
    paintRing,
    findFiveLine,
    paintLineBand,
    drawPreview
  };
})();
