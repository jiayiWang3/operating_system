#include "syscall.h"

int main() {
    char *prog = "write10.coff";
    exec(prog, 0, 0);
    return 0;
}

