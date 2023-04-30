/* write2.c
** write str into an exist file with correct arguments -address
*/
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main() {
    char *str = "output.txt";
    char *inputStr = "write something here";
    int fd = open(str);
    int written = write(fd, inputStr, strlen(inputStr));
    if (written == -1) {
        printf("error, test failed\n");
        exit(-1);
    } else {
        printf("wrote %d bytes, pass\n", written);
    }
    return 0;
}