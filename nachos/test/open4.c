#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

int main() {

    char *file = "output.txt";
    char *string = "This is open3 speaking.";
    int fd1 = open(file);
    int fd2 = open(file);
    if (fd1 == -1 || fd2 == -1) {
        printf("open a file failed, test failed.\n");
        return -1;
    } else {
        int written = write(fd1, string, strlen(string));
        printf("wrote %d bytes\n", written);
        string = "After unlinked.";
        written = write(fd2, string, strlen(string));
        printf("wrote %d bytes\n", written);
    }
}
