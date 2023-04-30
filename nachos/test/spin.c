#include "syscall.h"
#include "stdio.h"

int
main() {
    printf("spinning!\n");
    for (int i = 0; i < 1000000; ++i) {

    }
    printf("finish spinning.\n");
    return 0;
}