## Implementation Description and Test
### Demand paged virtual memory with page replacement
#### Implementation
* VMProcess.pageFaultHandler(): handler page fault, read the content from the right source, i.e. from COFF or swapfile. If ppn is -1, read from COFF, otherwise, treat ppn as spn and read from the swap file.
* Override VMProcess.readVM, VMProcess.writeVM, updating entry bits, and pin a page while reading/writing.
* Override VMloadSection and unloadSection for demand paging and deallocate disk pages.
* VMKernel.allocate(): allocate a physical page, evict when physical memory is insufficient, update the invertedPageTable to keep track of the status of every each physical page.
* VMKernel.evict(): called by VMKernel.aloocate(), evict a page, write back when necessary and update the right process's pageTable. The method uses the clock algorithm to choose which page to be evicted.
* Some other methods in VMKernel, like deallocateSwapFilePage/allocateSwapFilePage to maintain swap file free page list, Pin/unPin pages in case a page is evicted while reading/writing. writeToSwapFile/readFromSwapFile to read or write a page from/to the swap file.
#### Test Cases
* Run some test cases from proj2, and swap4/swap5/matmult with different physical page numbers for testing page replacement. For tesing multiprogramming with page replacement, we run multiswap4/multiswap5/multiwrite10 (write to different files)/multimat (matmult) with different but reasonable physical page numbers.

