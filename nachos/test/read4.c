/* read1.c
** read a exist file with invalid arguments -count
*/
#include "syscall.h"
#include "stdio.h"
int main() {
    char *str = "output.txt";
    int fd = open(str);
    char buff[100] = "";
    int recv = 0;
    while ((recv = read(fd, buff, 100)) != 0) {
        if (recv == -1) {
            printf("read Failed. Exit");
            exit(-1);
        }
        printf("%s", buff);
    }
    printf("\n");
    return 0;
}