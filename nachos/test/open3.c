#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

int main() {

    char *file = "output.txt";
    char *string = "This is open3 speaking.";
    int fd = open(file);
    if (fd == -1) {
        printf("open a file failed, test failed.\n");
        return -1;
    } else {
        int written = write(fd, string, strlen(string));
        printf("wrote %d bytes\n", written);
        unlink(file);
        string = "After unlinked.";
        written = write(fd, string, strlen(string));
        printf("wrote %d bytes\n", written);
    }
}
