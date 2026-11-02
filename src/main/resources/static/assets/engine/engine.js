// © 2026 Studio Isoo. All rights reserved.
(function () {
  const WORKER_URL = '/assets/engine/engine-worker.js';

  let worker = null;
  let nextId = 0;
  const pending = new Map();

  function rejectAll(error) {
    pending.forEach(({ reject }) => reject(error));
    pending.clear();
  }

  function discardWorker() {
    if (worker) {
      worker.terminate();
      worker = null;
    }
  }

  function start() {
    if (worker) {
      return;
    }
    worker = new Worker(WORKER_URL, { type: 'module' });
    worker.onmessage = (event) => {
      const { id, result, error } = event.data;
      const request = pending.get(id);
      if (!request) {
        return;
      }
      pending.delete(id);
      if (error === undefined) {
        request.resolve(result);
        return;
      }
      // An engine that failed once can't be trusted, so the next call starts a fresh worker
      discardWorker();
      request.reject(new Error(error));
      rejectAll(new Error(error));
    };
    worker.onerror = (event) => {
      discardWorker();
      rejectAll(new Error(event.message || 'Engine worker failed'));
    };
  }

  function call(type, board) {
    start();
    const id = ++nextId;
    return new Promise((resolve, reject) => {
      pending.set(id, { resolve, reject });
      worker.postMessage({ id, type, board });
    });
  }

  // A search can't be interrupted inside the worker, so cancelling throws the worker away
  function cancel() {
    if (!worker) {
      return;
    }
    discardWorker();
    const error = new Error('Engine call cancelled');
    error.name = 'EngineCancelled';
    rejectAll(error);
  }

  globalThis.renzzleEngine = {
    start,
    // 0-based cell index of the AI move, -1 when there is none
    nextMove: (board) => call('nextMove', board),
    // 0 ongoing, 1 the last move won, 2 black lost on a forbidden move
    checkWin: (board) => call('checkWin', board),
    cancel,
    isCancelled: (error) => error?.name === 'EngineCancelled'
  };
})();
