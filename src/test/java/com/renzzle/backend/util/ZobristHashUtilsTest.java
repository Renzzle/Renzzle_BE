package com.renzzle.backend.util;

import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZobristHashUtilsTest {

    @Test
    void parseCellIndexes_ShouldReturnZeroBasedCellsInMoveOrder() {
        // h8 == 7*15 + 8 - 1 == 112, h9 == 113, i10 == 8*15 + 10 - 1 == 129
        assertThat(ZobristHashUtils.parseCellIndexes("h8h9i10")).containsExactly(112, 113, 129);
        assertThat(ZobristHashUtils.parseCellIndexes("a1")).containsExactly(0);
        assertThat(ZobristHashUtils.parseCellIndexes("o15")).containsExactly(224);
    }

    @Test
    void applyMove_ShouldMatchFullRehash() {
        String boardState = "h8h9i10";
        long baseHash = ZobristHashUtils.hashFromBoardStatus(boardState);
        int nextMoveIndex = ZobristHashUtils.parseCellIndexes(boardState).length;

        // j11 == 9*15 + 11 - 1 == 145
        assertThat(ZobristHashUtils.applyMove(baseHash, 145, nextMoveIndex))
                .isEqualTo(ZobristHashUtils.hashFromBoardStatus(boardState + "j11"));
    }

    @Test
    void applyMove_ShouldMatchFullRehash_WhenAppliedRepeatedly() {
        String[] moves = {"h8", "h9", "i10", "j11", "g7"};

        long incrementalHash = 0L;
        StringBuilder boardState = new StringBuilder();

        for (int moveIndex = 0; moveIndex < moves.length; moveIndex++) {
            boardState.append(moves[moveIndex]);
            int cellIndex = ZobristHashUtils.parseCellIndexes(moves[moveIndex])[0];
            incrementalHash = ZobristHashUtils.applyMove(incrementalHash, cellIndex, moveIndex);

            assertThat(incrementalHash)
                    .isEqualTo(ZobristHashUtils.hashFromBoardStatus(boardState.toString()));
        }
    }

    @Test
    void hashFromBoardStatus_ShouldConvergeOnTransposition() {
        assertThat(ZobristHashUtils.hashFromBoardStatus("h8h9j10k11"))
                .isEqualTo(ZobristHashUtils.hashFromBoardStatus("j10k11h8h9"));
    }

    @Test
    void hashFromBoardStatus_ShouldDifferWhenColorsSwap() {
        assertThat(ZobristHashUtils.hashFromBoardStatus("h8i9"))
                .isNotEqualTo(ZobristHashUtils.hashFromBoardStatus("i9h8"));
    }

    @Test
    void parseCellIndexes_ShouldHandleFullBoard() {
        StringBuilder fullBoard = new StringBuilder();
        for (char letter = 'a'; letter <= 'o'; letter++) {
            for (int number = 1; number <= 15; number++) {
                fullBoard.append(letter).append(number);
            }
        }

        assertThat(ZobristHashUtils.parseCellIndexes(fullBoard.toString())).hasSize(225);
    }

    @Test
    void applyMove_ShouldThrow_WhenCellIndexOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> ZobristHashUtils.applyMove(0L, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> ZobristHashUtils.applyMove(0L, 225, 0));
    }

    @Test
    void parseCellIndexes_ShouldThrow_WhenBoardStateBlank() {
        assertThrows(IllegalArgumentException.class, () -> ZobristHashUtils.parseCellIndexes(null));
        assertThrows(IllegalArgumentException.class, () -> ZobristHashUtils.parseCellIndexes("  "));
    }

    @Test
    void parseCellIndexes_ShouldThrow_WhenBoardStateInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ZobristHashUtils.parseCellIndexes("z99"));
        assertThrows(IllegalArgumentException.class, () -> ZobristHashUtils.parseCellIndexes("h16"));
        assertThrows(IllegalArgumentException.class, () -> ZobristHashUtils.parseCellIndexes("h"));
    }
}
