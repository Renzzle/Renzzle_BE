#include "engine/engine.h"
#include <iostream>

int main(int argc, char* argv[]) {
    if (argc < 2) {
        std::cerr << "Usage: renzzle_ai_engine <boardState>" << std::endl;
        return 1;
    }

    std::string boardState(argv[1]);
    int result = findNextMove(boardState);
    std::cout << result << std::endl;
    return 0;
}
