/* test open a non-exist file */
#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

int main() {

    char *str = "nonexist.txt";
    int fd = open(str);
    if (fd == -1) { 
        printf("open a non-exist file failed, test pass. ");
    }
    else {
        printf("Test Failed.");
    }
    assert(fd == -1);
    return 0;
}
