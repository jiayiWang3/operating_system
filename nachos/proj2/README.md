## Group Members
* Wentao Huang
* Siran Ma
* Jiayi Wang

## Implementation Description and Test
### System  Calls - File
#### Implementation
* handleCreate(): initialize a fileTable array to store the fileDescriptor, create a file using the API provided by the filesystem
* handleOpen(): open a file using the API provided by the filesystem
* handleRead(): read the data from file using the API provided by the filesystem, first transfer the data from file to buffer, then to memory
* handleWrite(): write the data to file using the API provided by the filesystem, first transfer the data from memory to buffer, then to file
* handleClose(): remove the file from the fileTable of this process
* handleUnlink(): remove the file from the fileTable of this process and remove the file using the API provided by the filesystem
#### Test
* create(), open(), read(), write(), close(), unlink(): test the basic operations and error cases 

### Virtual Memory
#### Implementation
* UserKernel.java: initialize a freePageList to keep track of the first free physical page
* loadSection(): allocate the memory for current process, allocate free physical page and set up the pageTable
* readVirtualMemory() and readVMWithPT(): determine the physical address based on pageTable and virtual address, handle address translation
* writeVirtualMemory() and writeVMWithPT(): determine the physical address based on pageTable and virtual address, handle address translation
#### Test
* use the testing cases for the first part

### System Calls - Multiprogramming
#### Implementation
* UserProcess.java: initialize a map (children) to store the information of children of this process, with PID as key and process as value. initialize another map (childStatus) to store the status of each of child of current process, with process as key and status as value, initialize an attribute as parent
* handleExec(): create a new Process and initialize the process with the given parameter
* handleJoin(): handle the join system call by using thread.join(), store the status to the specified place
* handleExit(): terminate the current process, close all file descriptors belonging to the class and upate the information of its children
#### Test
* exec(), join(), exit(): test the basic operations and error case as specified in the manual

## Contributions
* Wentao Huang: group discussion, implementation of file system calls, virtual memory and multiprogramming system calls, testing
* Siran Ma: group discussion, implementation of file system calls and virtual memory, testing
* Jiayi Wang: group discussion and testing