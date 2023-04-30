#include "syscall.h"
#include "stdio.h"
#define N 2
int main() {
    printf("Running write1.coff and write10.coff processes.\n", N);
    int retureValues[N];
    int pids[N];
    pids[0] = exec("write1.coff", 0, 0);
    pids[1] = exec("write10.coff", 0, 0);
    for (int i = 0; i < N; i += 1) {
        join(pids[i], retureValues + i);
        if (retureValues[i] != 0) {
            exit(-1);
        }
    }
    exit(0);
}