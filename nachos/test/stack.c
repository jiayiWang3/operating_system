#include "syscall.h"

//* test for reading
int main (int argc, char *argv[])
{
    char *fileName = "alice_in_wonderland.txt"; // fileExists
    int fd = open(fileName);
    printf ("open file (fd = %d)\n", fd);

    char buffer[50];
    int r = read(fd, buffer, 30);
    // close(fd);
    if (r == -1) {
        printf ("failed to read\n");
        exit (-1);
    }
    printf("read: %s\n", buffer);

    char* target = "                ALICE'S ADVENTURES IN WONDERLAND";
    for(int i = 0; i < 30; i++) {
        if(buffer[i] != target[i]) {
            printf ("failed to validate, offset: %d, want %c get %c\n", i, target[i], buffer[i]);
            exit (-1);
        }
    }

    //* read again to test continuous test
    r = read(fd, buffer+30, 10);
   if (r == -1) {
        printf ("failed to read\n");
        exit (-1);
    }
    for(int i = 30; i < 40; i++) {
        if(buffer[i] != target[i]) {
            printf ("failed to validate, offset: %d, want %c get %c\n", i, target[i], buffer[i]);
            exit (-1);
        }
    }
    close(fd);
    printf("read: %s\n", buffer);


    char* invalidFileName = "alice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txtalice_in_wonderland.txt";
    int fd1 = open(invalidFileName);
    if(fd1 != -1) {
        printf ("should not open invalid filename\n");
        close(fd1); 
        exit (-1);
    }

    int fd2 = creat("testing2.txt");
    if(fd2 == -1) {
        printf("failed to create file\n");
        exit(1);
    }
    char* str = "for read1 testing";
    r = write (fd2, str, 17);
    if (r != 17) {
        printf("write count not accurate, want: %d, r %d\n", 17, r);
        exit(1);
    }

    //!!! buffer overflow????
    int fd3 = open("testing2.txt");
    r = read(fd3, buffer, 100);
    if (r != 17) {
        printf("read count not accurate, want: %d, r %d\n", 17, r);
        exit(1);
    }

    r = write (fd2, str, 100);
    if (r != 100) {
        printf("write count not accurate, want: %d, r %d\n", 100, r);
        exit(1);
    }


    r = write (fd2, str, 1);
    if (r != 1) {
        printf("write count not accurate, want: %d, r %d\n", 100, r);
        exit(1);
    }

    close(fd2);
    return 0;
}