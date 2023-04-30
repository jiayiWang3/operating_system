package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.Arrays;
/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		super.restoreState();
	}

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     * 
     * @return <tt>true</tt> if successful.
     */
    @Override
    protected boolean loadSections() {
        pageTable = new TranslationEntry[numPages];

		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			Lib.debug(dbgVM, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
                pageTable[vpn] = new TranslationEntry(vpn, -1, false, section.isReadOnly(), false, false);
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tcreate a PTE, vpn " + vpn + ", ppn " + -1);
			}
		}
        //load pages for the stack and args
        CoffSection lastSection = coff.getSection(coff.getNumSections() - 1);
        int nextVPN = lastSection.getFirstVPN() + lastSection.getLength();
        for (int i = 0; i <= stackPages; i += 1) {
            int vpn = nextVPN + i;
            pageTable[vpn] = new TranslationEntry(vpn, -1, false, false, false, false);
            Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tcreate a PTE, vpn " + vpn + ", ppn " + -1);
        }
		return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    @Override
    protected void unloadSections() {
        // deacllocate memory
        for (int i = 0; i < numPages; i += 1) {
            int ppn = pageTable[i].ppn;
            if (pageTable[i].valid) {
                VMKernel.deallocate(ppn);
            } else if (pageTable[i].ppn != -1) {
                VMKernel.deallocateSwapFilePage(ppn);
            }
            pageTable[i] = null;
        }
    }

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
    @Override
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

        if (vaddr < 0 || vaddr >= numPages * pageSize) {
            return 0;
        }

		int amount = Math.min(length, numPages * pageSize - vaddr);

		return readVMWithPT(memory, vaddr, data, offset, amount);
	}

    private int readVMWithPT(byte[] memory, int vaddr, byte[] data, int offset, int amount) {
		int currentVa = vaddr;
		int totalRead = 0;
		while (currentVa < vaddr + amount) {
			int vpn = Processor.pageFromAddress(currentVa);
            if (!pageTable[vpn].valid) {
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\treadVMWithPT Page Fault on vpn " + vpn);
                handlePageFault(currentVa);
            }
			int ppn = pageTable[vpn].ppn;
            VMKernel.pinPage(ppn);
            Lib.debug(dbgVM, "PID[" + PID + "]:" + "\treading a page, ppn " + ppn);
            setUsed(vpn);
			int addrOffset = Processor.offsetFromAddress(currentVa);
			int paddr = pageSize * ppn + addrOffset;
			int nextVa = pageSize * (vpn + 1);
			if (nextVa < vaddr + amount) { // reach the end of page
				int toRead = pageSize - addrOffset;
				System.arraycopy(memory, paddr, data, offset, toRead);
				offset += toRead;
				totalRead += toRead;
			} else { // will not reach the end of page
                int toRead = vaddr + amount - currentVa;
				System.arraycopy(memory, paddr, data, offset, toRead);
				offset += toRead;
				totalRead += toRead;
			}
			currentVa = nextVa;
            VMKernel.unpinPage(ppn);
            Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tend reading a page, ppn " + ppn);
		}
		return totalRead;
	}

    /**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
    @Override
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		if (vaddr < 0 || vaddr >= numPages * pageSize) {
			return 0;
        }

		int amount = Math.min(length, numPages * pageSize - vaddr);

		return writeVMWithPT(data, offset, memory, vaddr, amount);
	}

	private int writeVMWithPT(byte[] data, int offset, byte[] memory, int vaddr, int amount) {
		int currentVa = vaddr;
		int totalWrite = 0;
		while (currentVa < vaddr + amount) {
			int vpn = Processor.pageFromAddress(currentVa);
            if (pageTable[vpn].readOnly) {
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\twriteVMWithPT on a readOnly page vpn " + vpn);
                return totalWrite;
            }
            if (!pageTable[vpn].valid) {
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\twriteVMWithPT Page Fault on vpn " + vpn);
                handlePageFault(currentVa);
            }
			int ppn = pageTable[vpn].ppn;
            Lib.debug(dbgVM, "PID[" + PID + "]:" + "\twriting a page, ppn " + ppn);
            VMKernel.pinPage(ppn);
            setUsed(vpn);
            setDirty(vpn);
			int addrOffset = Processor.offsetFromAddress(currentVa);
			int paddr = pageSize * ppn + addrOffset;
			int nextVa = pageSize * (vpn + 1);
			if (nextVa < vaddr + amount) { // reach the end of page
				int toWrite = pageSize - addrOffset;
				System.arraycopy(data, offset, memory, paddr, toWrite);
				offset += toWrite;
				totalWrite += toWrite;
			} else { // will not reach the end of page
                int toWrite = vaddr + amount - currentVa;
				System.arraycopy(data, offset, memory, paddr, toWrite);
				offset += toWrite;
				totalWrite += toWrite;
			}
			currentVa = nextVa;
            VMKernel.unpinPage(ppn);
            Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tend writing a page, ppn " + ppn);
		}
		return totalWrite;
	}
    
    /**
     * Page Fault Handler.
     * @param vaddr the invalid virtual address.
     */
    private void handlePageFault(int vaddr) {
        pageFaultLock.acquire();
        Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tpage fault on vaddr 0x" + Lib.toHexString(vaddr) + " vpn " + Processor.pageFromAddress(vaddr));
        int vpn = Processor.pageFromAddress(vaddr);
        if (pageTable[vpn] == null) {
            return;
        }
        int ppn = VMKernel.allocate(this, vpn);
        if (pageTable[vpn].ppn == -1) {
            if (vpn < numPages - stackPages - 1) {
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tpage fault, reading from COFF");
                for (int s = 0; s < coff.getNumSections(); s += 1) {
                    CoffSection section = coff.getSection(s);
                    int firstVPN = section.getFirstVPN(), lastVPN = section.getFirstVPN() + section.getLength() - 1;
                    if (vpn >= firstVPN && vpn <= lastVPN) {
                        section.loadPage(vpn - section.getFirstVPN(), ppn);
                        break;
                    }
                }
            } else {
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tpage fault, initializing stack");
                byte[] memory = Machine.processor().getMemory();
                Arrays.fill(memory, ppn * pageSize, (ppn + 1) * pageSize, (byte) 0);
            }
        } else {
            Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tpage fault, reading from swap file, spn " + pageTable[vpn].ppn);
            VMKernel.readFromSwapFile(ppn, pageTable[vpn].ppn);
        }
        setValid(vpn);
        setUsed(vpn);
        pageTable[vpn].ppn = ppn;
        pageFaultLock.release();
        Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tload a page" + " vpn " + vpn + " ppn " + pageTable[vpn].ppn + "\n");
    }

    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
     * . The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     * 
     * @param cause the user exception that occurred.
     */
    @Override
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionPageFault:
                handlePageFault(processor.readRegister(Processor.regBadVAddr));
                break;
            default:
                super.handleException(cause);
                break;
        }
    }

    /**
     * Return pageTable[vpn].used.
     */
    protected boolean isUsed(int vpn) {
        return pageTable[vpn].used;
    }

    /**
     * Return pageTable[vpn].dirty.
     */
    protected boolean isDirty(int vpn) {
        return pageTable[vpn].dirty;
    }

    /**
     * Set pageTable[vpn].ppn to ppn.
     * @param vpn the virtual page number.
     * @param ppn the new ppn, could be the swap page number or -1
     */
    protected void setPPN(int vpn, int ppn) {
        pageTable[vpn].ppn = ppn;
    }

    /**
     * Set pageTable[vpn].used to true.
     * @param vpn the virtual page number.
     */
    private void setUsed(int vpn) {
        pageTable[vpn].used = true;
    }

    /**
     * Set pageTable[vpn].unsed to false.
     */
    protected void unsetUsed(int vpn) {
        pageTable[vpn].used = false;
    }

    /**
     * Set pageTable[vpn].dirty to true, called by writeVM.
     * @param vpn the virtual page number.
     */
    private void setDirty(int vpn) {
        pageTable[vpn].dirty = true;
    }

    /**
     * Set pageTable[vpn].valid to true.
     */
    private void setValid(int vpn) {
        pageTable[vpn].valid = true;
    }

    /**
     * Unset pageTable[vpn].valid to false.
     */
    protected void unsetValid(int vpn) {
        pageTable[vpn].valid = false;
    }

    private static Lock pageFaultLock = new Lock();

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
