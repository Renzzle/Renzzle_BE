#include <emscripten/emscripten.h>
#include "engine/engine.h"

extern "C" {

EMSCRIPTEN_KEEPALIVE
int renzzle_next_move(const char* boardData) {
    return findNextMove(boardData);
}

// Same results as checkWin in the app's RenzzleJNI.cpp: 1 when the last move won, 2 when black lost on a forbidden move
EMSCRIPTEN_KEEPALIVE
int renzzle_check_win(const char* boardData) {
    Board board = getBoard(boardData);
    if (board.getResult() == BLACK_WIN) {
        return board.isBlackTurn() ? 0 : 1;
    }
    if (board.getResult() == WHITE_WIN) {
        return board.isBlackTurn() ? 1 : 2;
    }
    return 0;
}

}
