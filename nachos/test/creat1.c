/* creat test1: create a file correctly. */
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main() {

	char* filename="output.txt";
	int result = creat(filename);
	if (result != -1) { printf("Test Passed: create 'output.txt' in test directory.\n"); }
	else { printf("Test Failed."); }
	return 0;
}
