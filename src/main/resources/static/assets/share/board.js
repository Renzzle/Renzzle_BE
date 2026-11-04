(function () {
  const LINES = 15;
  const STAR_POINTS = [[3, 3], [3, 11], [7, 7], [11, 3], [11, 11]];
  // The app's board lines; the share page paints the wood and the labels itself
  const COLORS = {
    line: 'rgba(38, 36, 31, 0.26)',
    frame: 'rgba(38, 36, 31, 0.62)',
    star: 'rgba(38, 36, 31, 0.62)'
  };

  const redrawByCanvas = new WeakMap();
  const resizeObserver = typeof ResizeObserver === 'function'
    ? new ResizeObserver(entries => entries.forEach(entry => redrawByCanvas.get(entry.target)?.()))
    : null;

  // Two-digit rows first, or "i10" would read as i1
  function parseMoves(boardStatus, moveIndex = 0) {
    const moves = [];
    const regex = /([a-o])(1[0-5]|[1-9])/gi;
    let match;
    while ((match = regex.exec(boardStatus || '')) !== null) {
      moves.push({
        row: Number(match[2]) - 1,
        col: match[1].toLowerCase().codePointAt(0) - 97,
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

  // Grid, frame and star points
  function paintBoard(ctx, layout) {
    const { origin, cell } = layout;
    const end = origin + cell * (LINES - 1);

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

  globalThis.shareBoard = {
    parseMoves,
    watch,
    paintBoard,
    findFiveLine
  };
})();
