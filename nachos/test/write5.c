/* write2.c
** write str into an exist file with invalid arguments -count.
*/
#include "syscall.h"
#include "stdio.h"
int main() {
    char *str = "output.txt";
    char *inputStr = "write something here";
    int fd = open(str);
    int written = write(fd, inputStr, -1);
    if (written == -1) {
        printf("error, test pass\n");
        exit(-1);
    } else {
        printf("test failed\n", written);
    }
    return 0;
}