package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
        /*
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i += 1) {
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
        }
        */
        fileTable[0] = UserKernel.console.openForReading();
        fileTable[1] = UserKernel.console.openForWriting();
        for (int i = 2; i < 16; i += 1) {
            nextIndexQueue.offer(i);
        }
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
	    String name = Machine.getProcessClassName();
        UserKernel.incrementProcess();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.
		if (name.equals ("nachos.userprog.UserProcess")) {
		    return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
		    return new VMProcess ();
		} else {
		    return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {

	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
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
                return totalRead;
            }
			int ppn = pageTable[vpn].ppn;
			int addrOffset = Processor.offsetFromAddress(currentVa);
			int paddr = pageSize * ppn + addrOffset;
			int nextVa = pageSize * (vpn + 1);
            //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\tvirtual address " + vaddr + ", physical address " + paddr + ", offset " + addrOffset);
			if (nextVa < vaddr + amount) { // reach the end of page
				int toRead = pageSize - addrOffset;
				System.arraycopy(memory, paddr, data, offset, toRead);
				offset += toRead;
				totalRead += toRead;
                //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\tread from vpn " + vpn + " / ppn " + ppn + " to buffer " + toRead + " bytes");
			} else { // will not reach the end of page
                int toRead = vaddr + amount - currentVa;
				System.arraycopy(memory, paddr, data, offset, toRead);
				offset += toRead;
				totalRead += toRead;
                //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\tread from vpn " + vpn + " / ppn " + ppn + " to buffer " + toRead + " bytes");
			}
			currentVa = nextVa;
		}
		return totalRead;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
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
            if (!pageTable[vpn].valid || pageTable[vpn].readOnly) {
                return totalWrite;
            }
			int ppn = pageTable[vpn].ppn;
			int addrOffset = Processor.offsetFromAddress(currentVa);
			int paddr = pageSize * ppn + addrOffset;
			int nextVa = pageSize * (vpn + 1);
            //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\tvirtual address " + vaddr + ", physical address " + paddr + ", offset " + addrOffset);
			if (nextVa < vaddr + amount) { // reach the end of page
				int toWrite = pageSize - addrOffset;
				System.arraycopy(data, offset, memory, paddr, toWrite);
				offset += toWrite;
				totalWrite += toWrite;
                //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\twrite from vpn " + vpn + " / ppn " + ppn + " to buffer " + toWrite + " bytes");
			} else { // will not reach the end of page
                int toWrite = vaddr + amount - currentVa;
				System.arraycopy(data, offset, memory, paddr, toWrite);
				offset += toWrite;
				totalWrite += toWrite;
                //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\twrite from vpn " + vpn + " / ppn " + ppn + " to buffer " + toWrite + " bytes");
			}
			currentVa = nextVa;
		}
		return totalWrite;
	}


	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i += 1) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tinsufficient physical memory");
			return false;
		}

        pageTable = new TranslationEntry[numPages];

		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				//section.loadPage(i, vpn);
                int ppn = UserKernel.allocate();
                if (ppn < 0) {
			        Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tinsufficient physical memory");
                    return false;
                }
                section.loadPage(i, ppn);
                pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tloaded a page, vpn " + vpn + ", ppn " + ppn);
			}
		}
        //load pages for the stack and args
        CoffSection lastSection = coff.getSection(coff.getNumSections() - 1);
        int nextVPN = lastSection.getFirstVPN() + lastSection.getLength();
        for (int i = 0; i <= stackPages; i += 1) {
            int vpn = nextVPN + i;
            int ppn = UserKernel.allocate();
            if (ppn < 0) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tinsufficient physical memory");
                return false;
            }
            pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tloaded a page, vpn " + vpn + ", ppn " + ppn);
        }
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
        // deacllocate memory
        for (int i = 0; i < numPages; i += 1) {
            if (pageTable[i] == null) break;
            int ppn = pageTable[i].ppn;
            pageTable[i] = null;
            UserKernel.deallocate(ppn);
        }
	}

    /**
     * Release any resources belongs to the process.
     * Clear file table and set children's parent to null
     */
    protected void cleanup() {
         // close files
         for (int i = 0; i < 16; i += 1) {
             if (fileTable[i] != null) {
                 OpenFile file = fileTable[i];
                 fileTable[i] = null;
                 nextIndexQueue.offer(i);
                 file.close();
             }
         }
         // set children's parent to null
         for (UserProcess child : children.values()) {
             child.parent = null;
         }
         if (coff != null) {
            coff.close();
         }
    }

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleHalt()");
        if (PID != 0) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:" +  "\tUserProcess.handleHalt() failed, only can be called by root process");
            return -1;
        }
		Machine.halt();

		Lib.assertNotReached("PID[" + PID + "]:" + "Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exit() system call.
	 */
	private int handleExit(int status) {
	    // Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.
		Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExit(" + status + ")");

        unloadSections();
        cleanup();
        UserKernel.decrementProcess();
        Lib.debug(dbgProcess, "Number of live processes:" + UserKernel.getNumProcess());
        if (this.parent != null) {
            this.parent.childStatus.put(this, status);
        }
        if (UserKernel.isLastProcess()) {
		    Kernel.kernel.terminate();
        }
        KThread.finish();
		return 0;
	}

    /**
     * Handle the create(char *name) system call.
     */
    private int handleCreate(int addr) {
        String name = readVirtualMemoryString(addr, 256);
        if (name == null) {
		    Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleCreate() failed");
            return -1;
        }
		Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleCreate(" + name + ")");
        OpenFile file = ThreadedKernel.fileSystem.open(name, true);
        if (file == null) {
            return -1;
        }
        if (nextIndexQueue.isEmpty()) {
            return -1;
        }
        int index = nextIndexQueue.poll();
        //Lib.assertTrue(fileTable[index] == null, "file object at " + index + " should be null.");
        fileTable[index] = file;
        return index;
    }

    /**
     * Handle the open(char *name) system call.
     */
    private int handleOpen(int addr) {
        String name = readVirtualMemoryString(addr, 256);
        if (name == null) {
		    Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleOpen() failed");
            return -1;
        }
		Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleOpen(" + name + ")");
        OpenFile file = ThreadedKernel.fileSystem.open(name, false);
        if (file == null) {
            return -1;
        }
        if (nextIndexQueue.isEmpty()) {
            return -1;
        }
        int index = nextIndexQueue.poll();
        //Lib.assertTrue(fileTable[index] == null, "file object at " + index + " should be null.");
        fileTable[index] = file;
        return index;
    }

    /**
     * Handle the read(int fileDescriptor, void *buffer, int count) system call.
     */
    private int handleRead(int fileDescriptor, int addr, int count) {
		//Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleRead(" + fileDescriptor + ")");
        if (addr == 0 || count < 0) {
            return -1;
        }
        if (fileDescriptor < 0 || fileDescriptor >= 16) {
            return -1;
        }
        if (count == 0) {
            return 0;
        }
        OpenFile file = fileTable[fileDescriptor];
        if (file == null) { // wrong file descriptor
            return -1;
        }
        int total = 0;

        while (total < count) {
            byte[] buffer = new byte[pageSize];
            int readBytes = file.read(buffer, 0, Math.min(count, pageSize));
            if (readBytes < 0) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleRead() failed, read failed");
                return -1;
            }
            if (readBytes == 0) {
                //Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleRead() reach end of file");
                return total;
            }
            if (readBytes < pageSize) { // reach end of file or count < pageSize
                total += readBytes;
                int writeBytes = writeVirtualMemory(addr, buffer, 0, readBytes);
                if (writeBytes == 0) {
                    Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleRead() failed, invalid address reference");
                    return -1;
                }
                if (writeBytes < readBytes) {
                    Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleRead() failed, run out of memory");
                    return -1;
                }
                return total;
            }
            total += readBytes;
            int writeBytes = writeVirtualMemory(addr, buffer,0 , readBytes);
            if (writeBytes == 0) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleRead() failed, invalid address reference");
                return -1;
            }
            if (writeBytes < readBytes) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleRead() failed, run out of memory");
                return -1;
            }
            addr += writeBytes;
        }
        return total;
    }

    /**
     * Handle the write(int fileDescriptor, void *buffer, int count) system call.
     */
    private int handleWrite(int fileDescriptor, int addr, int count) {
		Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleWrite(" + fileDescriptor + ")");
        if (addr == 0 || count < 0) {
            return -1;
        }
        if (fileDescriptor < 0 || fileDescriptor >= 16) {
            return -1;
        }
        if (count == 0) {
            return 0;
        }
        OpenFile file = fileTable[fileDescriptor];
        if (file == null) {
            return -1;
        }
        int total = 0;

        while (total < count) {
            byte[] buffer = new byte[Math.min(count - total, pageSize)];
            int readBytes = readVirtualMemory(addr, buffer, 0, buffer.length);
            if (readBytes == 0) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleWrite() failed, invalid address reference");
                return -1;
            }
            if (readBytes != buffer.length) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleWrite() failed, run out of memory");
                return -1;
            }
            addr += readBytes;
            int writeBytes = file.write(buffer, 0, readBytes);
            if (writeBytes == -1) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleWrite() failed, write file failed");
                return -1;
            }
            if (writeBytes != readBytes) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "read " + readBytes + " but write " + writeBytes);
                return -1;
            }
            total += writeBytes;
        }
        if (total != count) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "should write " + count + " but write " + total);
        }
        return total;
    }

    /**
     * Handle the close() system call.
     */
    private int handleClose(int fileDescriptor) {
		Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleClose(" + fileDescriptor + ")");
        if (fileDescriptor < 0 || fileDescriptor >= 16) {
            return -1;
        }
        OpenFile file = fileTable[fileDescriptor];
        if (file == null) {
            return -1;
        }
        fileTable[fileDescriptor] = null;
        nextIndexQueue.offer(fileDescriptor);
        file.close();
        return 0;
    }

    /**
     * Handle the unlink() system call.
     */
    private int handleUnlink(int addr) {
        String name = readVirtualMemoryString(addr, 256);
        if (name == null) {
		    Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleUnlink() failed");
            return -1;
        }
		Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleUnlink(" + name + ")");
        // unlink delete the file, but does not close it
        /* 
        for (int i = 0; i < 16; i += 1) {
            if (fileTable[i] != null && fileTable[i].getName().equals(name)) {
                nextIndexQueue.offer(i);
                //fileTable[i].close();
                fileTable[i] = null;
		        Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleUnlink(" + name + "), unlinked fd " + i + " successfully");
                break;
            }
        }
        */
        if (ThreadedKernel.fileSystem.remove(name)) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:\tRemoved " + name + " successfully");
            return 0;
        }
        return -1;
    }

    /**
     * Handle the exec(char *file, int argc, char *argv[]) system call.
     */
    private int handleExec(int fileNameAddr, int argc, int argvAddr) {
        Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExec()");
        UserProcess child = newUserProcess();

        if (children.containsKey(child.PID)) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExec() failed, should have unique PIDs");
            child.cleanup();
            UserKernel.decrementProcess();
            Lib.debug(dbgProcess, "Number of live processes:" + UserKernel.getNumProcess());
            return -1;
        }

        if (fileNameAddr == 0 || fileNameAddr >= numPages * pageSize || argvAddr >= numPages * pageSize) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExec() failed, invalid address reference");
            child.cleanup();
            UserKernel.decrementProcess();
            Lib.debug(dbgProcess, "Number of live processes:" + UserKernel.getNumProcess());
            return -1;
        }
        String name = readVirtualMemoryString(fileNameAddr, 256);
        if (name == null) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExec() failed, invalid file name");
            child.cleanup();
            UserKernel.decrementProcess();
            Lib.debug(dbgProcess, "Number of live processes:" + UserKernel.getNumProcess());
            return -1;
        }
        byte[] data = new byte[4];
        String[] args = new String[argc];

        for (int i = 0; i < argc; i += 1) {
            if (argvAddr == 0 || argvAddr >= numPages * pageSize) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExec() failed, invalid address reference");
                child.cleanup();
                UserKernel.decrementProcess();
                Lib.debug(dbgProcess, "Number of live processes:" + UserKernel.getNumProcess());
                return -1;
            }

            int read = readVirtualMemory(argvAddr, data, 0, 4);
            if (read != 4) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExec() failed, read failed");
                return -1;
            }
            int argumentAddress = Lib.bytesToInt(data, 0);
            args[i] = readVirtualMemoryString(argumentAddress, 256);

            if (args[i] == null) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExec() failed, invalid argument");
                child.cleanup();
                UserKernel.decrementProcess();
                Lib.debug(dbgProcess, "Number of live processes:" + UserKernel.getNumProcess());
                return -1;
            }
            argvAddr += 4;
        }
        if (!child.execute(name, args)) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleExec() failed, execute failed");
            child.unloadSections();
            child.cleanup();
            UserKernel.decrementProcess();
            Lib.debug(dbgProcess, "Number of live processes:" + UserKernel.getNumProcess());
            return -1;
        }
        child.parent = this;
        children.put(child.PID, child);
        return child.PID;
    }

    /**
     * Handle the join(int processID, int *status) system call.
     */
    private int handleJoin(int pid, int statusAddr) {
        Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleJoin()");
        if (statusAddr >= numPages * pageSize) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleJoin() failed, invalid address reference");
            return -1;
        }
        if (!children.containsKey(pid)) {
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleJoin() failed, invalid pid, not a child");
            return -1;
        }

        UserProcess child = children.get(pid);
        child.thread.join();

        if (childStatus.containsKey(child)) {
            int status = childStatus.get(child);
            childStatus.remove(child);
            if (statusAddr == 0) {
                children.remove(pid);
                return 1;
            }
            byte[] statusBytes = new byte[4];
            Lib.bytesFromInt(statusBytes, 0, status);
            int writtenBytes = writeVirtualMemory(statusAddr, statusBytes);
            if (writtenBytes != 4) {
                Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tUserProcess.handleJoin(), write failed");
                children.remove(pid); 
                return -1;
            }
            children.remove(pid);
            return 1;
        }

        children.remove(pid);
        return 0;
    }

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			return handleExit(a0);
        case syscallCreate:
            return handleCreate(a0);
        case syscallOpen:
            return handleOpen(a0);
        case syscallRead:
            return handleRead(a0, a1, a2);
        case syscallWrite:
            return handleWrite(a0, a1, a2);
        case syscallClose:
            return handleClose(a0);
        case syscallUnlink:
            return handleUnlink(a0);
        case syscallExec:
            return handleExec(a0, a1, a2);
        case syscallJoin:
            return handleJoin(a0, a1);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
            
            unloadSections();
            cleanup();
            UserKernel.decrementProcess();
            Lib.debug(dbgProcess, "Number of live processes:" + UserKernel.getNumProcess());
            if (UserKernel.isLastProcess()) {
                Kernel.kernel.terminate();
            }
            KThread.finish();

			Lib.assertNotReached("Unexpected exception");
		}
	}

    /**
     * @return the PID of process.
     */
    public int getPID() {
        return PID;
    }

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
    protected UThread thread;
    
	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

    private OpenFile[] fileTable = new OpenFile[16];

    private PriorityQueue<Integer> nextIndexQueue = new PriorityQueue<>();

    protected final int PID = UserKernel.allocatePID();

    private Map<Integer, UserProcess> children = new HashMap<>();

    private Map<UserProcess, Integer> childStatus = new HashMap<>();

    private UserProcess parent = null;

	private static final char dbgProcess = 'a';
}