#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
    char *fileName = "output.txt";
    if (unlink(fileName) != 0) {
        printf("Unable to remove %s\n", fileName);
        return 1;
    } else {
        printf("Remove %s successfully\n", fileName);
    }
    return 0;
}
