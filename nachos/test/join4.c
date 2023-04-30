/*
This test is used for test join and waiting program execute
*/

#include "syscall.h"
#include "stdio.h"

int 
main() {
    char* argv1[1];
    argv1[0] = "hello.coff";
    char* argv2[1];
    argv2[0] = "spin.coff";
    int pid1 = exec("spin.coff", 1, argv1);
    int pid2 = exec("hello.coff", 1, argv2);
    int status = 0;
    int result = join(pid2, &status);
    if (result == 1) {
        printf("join pid2 success!\n");
    } else {
        printf("join pid2 fail. result = %d\n", result);
    }
    result = join(pid1, &status);
    if (result == 1) {
        printf("join pid1 success!\n");
    } else {
        printf("join pid1 fail. result = %d\n", result);
    }   
}