/*
The test is used for check multi process status and after all processes done, the kernel need to be terminated
*/

#include "syscall.h"
#include "stdio.h"

int
main() {
    int status = 0;
    int pid;
    for (int i = 0; i < 5; ++i) {
        pid = exec("hello.coff", 0, 0);
        if (pid <= 0) {
            printf("exec failed.\n");
            break;
        }
        printf("i = %d, status = %d\n", i, status);
    }
    exit(0);
    return 0;
}
