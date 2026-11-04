// © 2026 Studio Isoo. All rights reserved.
import createRenzzleEngine from './renzzle-engine.mjs';

const FUNCTIONS = { nextMove: 'renzzle_next_move', checkWin: 'renzzle_check_win' };
const engine = createRenzzleEngine();

self.onmessage = async (event) => {
  const { id, type, board } = event.data;
  try {
    const module = await engine;
    const result = module.ccall(FUNCTIONS[type], 'number', ['string'], [board]);
    self.postMessage({ id, result });
  } catch (error) {
    self.postMessage({ id, error: String((error?.message) || error) });
  }
};
