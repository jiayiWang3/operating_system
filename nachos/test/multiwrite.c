#include "syscall.h"
#include "stdio.h"
#define N 4
int main() {
    printf("Running %d write10.coff processes.\n", N);
    int retureValues[N];
    int pids[N];
    pids[0] = exec("write10.coff", 0, 0);
    pids[1] = exec("write10_1.coff", 0, 0);
    pids[2] = exec("write10_2.coff", 0, 0);
    pids[3] = exec("write10_3.coff", 0, 0);
    for (int i = 0; i < N; i += 1) {
        join(pids[i], retureValues + i);
        if (retureValues[i] != 0) {
            exit(-1);
        }
    }
    exit(0);
}