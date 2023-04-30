package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.Deque;
import java.util.ArrayDeque;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	/**
	 * Allocate a new user kernel.
	 */
	public UserKernel() {
		super();
	}

	/**
	 * Initialize this kernel. Creates a synchronized console and sets the
	 * processor's exception handler.
	 */
	public void initialize(String[] args) {
		super.initialize(args);

		console = new SynchConsole(Machine.console());

        lock = new Lock();
        pidLock = new Lock();
        numProcessLock = new Lock();

        int numPhysPages = Machine.processor().getNumPhysPages();
        for (int i = 0; i < numPhysPages; i += 1) {
            freePageList.addLast(i);
        }

		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() {
				exceptionHandler();
			}
		});
	}

	/**
	 * Test the console device.
	 */
	public void selfTest() {
		super.selfTest();

        /*
		System.out.println("Testing the console device. Typed characters");
		System.out.println("will be echoed until q is typed.");

		char c;

		do {
			c = (char) console.readByte(true);
			console.writeByte(c);
		} while (c != 'q');
        */
        
		System.out.println("");
	}

	/**
	 * Returns the current process.
	 * 
	 * @return the current process, or <tt>null</tt> if no process is current.
	 */
	public static UserProcess currentProcess() {
		if (!(KThread.currentThread() instanceof UThread))
			return null;

		return ((UThread) KThread.currentThread()).process;
	}

	/**
	 * The exception handler. This handler is called by the processor whenever a
	 * user instruction causes a processor exception.
	 * 
	 * <p>
	 * When the exception handler is invoked, interrupts are enabled, and the
	 * processor's cause register contains an integer identifying the cause of
	 * the exception (see the <tt>exceptionZZZ</tt> constants in the
	 * <tt>Processor</tt> class). If the exception involves a bad virtual
	 * address (e.g. page fault, TLB miss, read-only, bus error, or address
	 * error), the processor's BadVAddr register identifies the virtual address
	 * that caused the exception.
	 */
	public void exceptionHandler() {
		Lib.assertTrue(KThread.currentThread() instanceof UThread);

		UserProcess process = ((UThread) KThread.currentThread()).process;
		int cause = Machine.processor().readRegister(Processor.regCause);
		process.handleException(cause);
	}

	/**
	 * Start running user programs, by creating a process and running a shell
	 * program in it. The name of the shell program it must run is returned by
	 * <tt>Machine.getShellProgramName()</tt>.
	 * 
	 * @see nachos.machine.Machine#getShellProgramName
	 */
	public void run() {
		super.run();

		UserProcess process = UserProcess.newUserProcess();

		String shellProgram = Machine.getShellProgramName();
		if (!process.execute(shellProgram, new String[] {})) {
		    System.out.println ("Could not find executable '" +
					shellProgram + "', trying '" +
					shellProgram + ".coff' instead.");
		    shellProgram += ".coff";
		    if (!process.execute(shellProgram, new String[] {})) {
			System.out.println ("Also could not find '" +
					    shellProgram + "', aborting.");
			Lib.assertTrue(false);
		    }
		}
        
		KThread.currentThread().finish();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

    /**
     * @return the page number of allocated page
     */
    public static int allocate() {
        lock.acquire();
        if (freePageList.size() == 0) {
            lock.release();
            return -1;
        }
        int pageNum = freePageList.removeFirst();
        lock.release();
        return pageNum;
    }

    /**
     * @param pagenum the pagenum to be freed, add it at the end of freePageList.
     */
    public static void deallocate(int pagenum) {
        lock.acquire();
        freePageList.addLast(pagenum);
        lock.release();
    }

    /**
     * @return the PID of new process.
     */
    public static int allocatePID() {
        pidLock.acquire();
        int rst = PIDCount;
        PIDCount += 1;
        pidLock.release();
        return rst;
    }

    /**
     * increment the total num of proccesses.
     */
    public static void incrementProcess() {
        numProcessLock.acquire();
        numProcess += 1;
        numProcessLock.release();
    }
    
    /**
     * decrement the total num of proccesses.
     */
    public static void decrementProcess() {
        numProcessLock.acquire();
        numProcess -= 1;
        numProcessLock.release();
    }

    /**
     * @return the number of process.
     */
    public static int getNumProcess() {
        return numProcess;
    }

    /**
     * @return if there is only one process left.
     */
    public static boolean isLastProcess() {
        return numProcess == 0;
    }

	/** Globally accessible reference to the synchronized console. */
	public static SynchConsole console;

	// dummy variables to make javac smarter
	private static Coff dummy1 = null;

    protected static Deque<Integer> freePageList = new ArrayDeque<>();

    protected static Lock lock;

    private static Lock pidLock;

    private static Lock numProcessLock;

    private static int PIDCount = 0;

    private static int numProcess = 0;
}