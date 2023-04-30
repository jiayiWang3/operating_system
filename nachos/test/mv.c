#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int src, dst, amount;

  if (argc!=3) {
    printf("Usage: mv <src> <dst>\n");
    return 1;
  }
  printf("mv.c: argv[0] is %s\n", argv[0]);
  printf("mv.c: src is %s\n", argv[1]);
  printf("mv.c: dst is %s\n", argv[2]);

  src = open(argv[1]);
  if (src==-1) {
    printf("Unable to open %s\n", argv[1]);
    return 1;
  }

  creat(argv[2]);
  dst = open(argv[2]);
  if (dst==-1) {
    printf("Unable to create %s\n", argv[2]);
    return 1;
  }

  while ((amount = read(src, buf, BUFSIZE))>0) {
    write(dst, buf, amount);
  }

  close(src);
  close(dst);
  unlink(argv[1]);

  return 0;
}
