#include <jni.h>
#include <string>
#include "search/search_win.h"
#include "search/search.h"
#include "test/util.h"
#include "game/board.h"
#include "engine/engine.h"

using namespace std;

extern "C" {

// JNI 메서드: findVCF 메소드를 호출하여 결과를 반환
JNIEXPORT jstring JNICALL
Java_com_renzzle_1fe_SearchJNI_findWin(JNIEnv *env, jobject obj, jstring javaBoardData) {
    // Java String을 C++의 string으로 변환
    const char *nativeBoardData = env->GetStringUTFChars(javaBoardData, 0);
    string boardDataStr(nativeBoardData);
    env->ReleaseStringUTFChars(javaBoardData, nativeBoardData);

    return env->NewStringUTF(validatePuzzle(boardDataStr).c_str());
}

// 새로 추가한 메서드
JNIEXPORT jint JNICALL
Java_com_renzzle_1fe_UserAgainstActionJNI_reactUserMove(JNIEnv *env, jobject obj, jstring javaBoardData) {
    // Java String을 C++의 string으로 변환
    const char *nativeBoardData = env->GetStringUTFChars(javaBoardData, 0);
    string boardDataStr(nativeBoardData);
    env->ReleaseStringUTFChars(javaBoardData, nativeBoardData);

    int result = findNextMove(boardDataStr);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_renzzle_1fe_CheckWinJNI_checkWin(JNIEnv *env, jobject obj, jstring javaBoardData) {
    const char *nativeBoardData = env->GetStringUTFChars(javaBoardData, 0);
    string boardDataStr(nativeBoardData);
    env->ReleaseStringUTFChars(javaBoardData, nativeBoardData);

    Board board = getBoard(boardDataStr);
    if (board.getResult() == BLACK_WIN) {
        if (board.isBlackTurn()) return 0;
        else return 1;
    } else if (board.getResult() == WHITE_WIN) {
        if (board.isBlackTurn()) return 1;
        else return 0;
    } else {
        return 0;
    }
}

}
