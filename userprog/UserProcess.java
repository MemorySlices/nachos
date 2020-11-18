package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {

        System.out.println("new process");

        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];

        System.out.println("pagetable length: "+pageTable.length);

        for (int i=0; i<numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
        /** added by consecutivelimit */
        openFiles = new OpenFile[16];
        openFiles[0] = UserKernel.console.openForReading();
        openFiles[1] = UserKernel.console.openForWriting();

        // xhk
        MemReadWriteLock=new Lock();

        // wyh
        numP++;
        Pid = numP;
        aliveP++;
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	    return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        System.out.println("-----start system execute-----");

        if (!load(name, args))
            return false;
        
        UThread thread = new UThread(this);
        if(mainThread == null)
            mainThread = thread;

        System.out.println("-----start system fork-----");

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
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength+1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length=0; length<bytesRead; length++) {
            if (bytes[length] == 0)
            return new String(bytes, 0, length);
        }

        return null;
    }

    public int getVPN(int vaddr){
        return vaddr/pageSize;
    }

    public int getVPO(int vaddr){
        return vaddr%pageSize;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	    return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                 int length) {

        System.out.println("-----start read virtual memory-----");

        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();
        
        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        int amount = Math.min(length, memory.length-vaddr);
        //System.arraycopy(memory, vaddr, data, offset, amount);

        int cnt=0,vpn,vpo,ppn;

        vpn=getVPN(vaddr);
        vpo=getVPO(vaddr);

        MemReadWriteLock.acquire();
        
        if(pageTable[vpn]==null || pageTable[vpn].valid!=true){
            System.out.println("-----read from invalid page-----");
            return 0;
        }

        ppn=pageTable[vpn].ppn;
        System.arraycopy(memory, ppn*pageSize+vpo, data, offset, Math.min(pageSize-vpo,amount));
        pageTable[ppn].used=true;
        cnt+=Math.min(pageSize-vpo,amount);
        
        while(cnt<amount){
            vpn++;
            if(pageTable[vpn]==null || pageTable[vpn].valid!=true){
                System.out.println("-----read from invalid page-----");
                return cnt;
            }
            ppn=pageTable[vpn].ppn;
            System.arraycopy(memory, ppn*pageSize, data, offset+cnt, Math.min(pageSize,amount-cnt));
            pageTable[ppn].used=true;
            cnt+=Math.min(pageSize,amount-cnt);
        }

        MemReadWriteLock.release();

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	    return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                  int length) {

        System.out.println("-----start write virtual memory-----");

        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();
        
        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        int amount = Math.min(length, memory.length-vaddr);
        //System.arraycopy(data, offset, memory, vaddr, amount);
        
        int cnt=0,vpn,vpo,ppn;

        vpn=getVPN(vaddr);
        vpo=getVPO(vaddr);

        if(amount==0) return amount;
        
        MemReadWriteLock.acquire();

        if(pageTable[vpn]==null || pageTable[vpn].valid!=true){
            System.out.println("-----write to invalid page-----");
            return 0;
        }

        if(pageTable[vpn].readOnly==true){
            MemReadWriteLock.release();
            System.out.println("invalid write to read only memory!");
            handleExit(0);// to be completed
        }
        ppn=pageTable[vpn].ppn;

        System.arraycopy(data, offset, memory, ppn*pageSize+vpo, Math.min(pageSize-vpo,amount));
        pageTable[ppn].used=true;
        pageTable[ppn].dirty=true;
        cnt+=Math.min(pageSize-vpo,amount);
        
        while(cnt<amount){
            vpn++;
            if(pageTable[vpn]==null || pageTable[vpn].valid!=true){
                System.out.println("-----write to invalid page-----");
                return cnt;
            }
            ppn=pageTable[vpn].ppn;
            if(pageTable[vpn].readOnly==true){
                MemReadWriteLock.release();
                System.out.println("invalid write to read only memory!");
                handleExit(0);// to be completed
            }
            System.arraycopy(data, offset+cnt, memory, ppn*pageSize, Math.min(pageSize,amount-cnt));
            pageTable[ppn].used=true;
            pageTable[ppn].dirty=true;
            cnt+=Math.min(pageSize,amount-cnt);
        }

        MemReadWriteLock.release();

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
        
        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        }
        catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
            coff.close();
            Lib.debug(dbgProcess, "\tfragmented executable");
            return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();	

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages*pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;
        
        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
            stringOffset += 1;
        }
        
        System.out.println("-----system load ends-----");

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }
        
        System.out.println("-----start load sections-----");
        System.out.println("numpages: "+numPages+" numphyspages: "+Machine.processor().getNumPhysPages());

        UserKernel.FreePageListLock.acquire();
        for(int i=0;i<numPages;i++){
            
            if(UserKernel.FreePageList.size()==0){
                UserKernel.FreePageListLock.release();
                System.out.println("no free page!");
                handleExit(0); // to be completed
            }

            int ppn=UserKernel.FreePageList.removeFirst();

            System.out.println("allocating virtual page: "+i+" ppn: "+ppn);
            //System.out.println("pagetable length: "+pageTable.length);

            pageTable[i]=new TranslationEntry(i,ppn, true,false,false,false);

        }
        UserKernel.FreePageListLock.release();

        // load sections
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            
            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                + " section (" + section.getLength() + " pages)");

            for (int i=0; i<section.getLength(); i++) {
                int vpn = section.getFirstVPN()+i;

                int ppn=pageTable[vpn].ppn;
                pageTable[vpn].readOnly=section.isReadOnly();
                // for now, just assume virtual addresses=physical addresses
                section.loadPage(i, ppn);
            }
        }
        
        System.out.println("-----load sections ends-----");

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {

        System.out.println("-----start unload sections-----");

        UserKernel.FreePageListLock.acquire();
        for(int i=0;i<numPages;i++)
            UserKernel.FreePageList.add(i);
        UserKernel.FreePageListLock.release();
        
        //xhk: need to close file as well?

        for(int i=0;i<16;i++){
            handleClose(i);
        }
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i=0; i<processor.numUserRegisters; i++)
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
        //xhk
        System.out.println("-----start handle halt-----");

        if (Pid != 1) {            return -1;
        }

        Machine.halt();
        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }
    
    private int handleExit(int status) {
        //xhk
        System.out.println("-----start handle exit-----");

        int res = -1;
        exitCode = status;
        unloadSections();
        aliveP--;
        if(aliveP==0)
            Kernel.kernel.terminate();
        else
            UThread.finish();
        res = 0;
        return res;
    }

    boolean checkAddrValidity(int addr){
        if(addr<0 || addr/pageSize>=numPages){
            System.out.println("invalid address");
            return false;
        }
        else 
            return true;
    }

    private int handleJoin(int pid, int resAddr){
        //xhk
        System.out.println("-----start handle join-----");

        int res = -1;
        if(!checkAddrValidity(resAddr))
            return -1;
        UserProcess childProcess = null;
        for (UserProcess child : children)
            if(child.Pid == pid){
                childProcess = child;
                break;
            }
        if(childProcess==null || childProcess.joint){
            System.out.println("childProcess Error");
            return -1;
        }
        childProcess.joint = true;
        childProcess.mainThread.join();
        byte[] exitBytes = Lib.bytesFromInt(childProcess.exitCode);
        if(writeVirtualMemory(resAddr, exitBytes) != 4){
            System.out.println("writeVirtualMemory Error");
            return -1;
        }
        if (childProcess.Uexception != NO_EXCEPTION)
            res = 0;
        else
            res = 1;
        return res;   
    }

    private int handleExec(int fileAddr, int argc, int argvAddr){
        //xhk
        System.out.println("-----start handle exec-----");

        int res = -1;
        if(!checkAddrValidity(fileAddr))
            return -1;
        if(!checkAddrValidity(argvAddr))
            return -1;
        if(argc<0 || argc<256){
            System.out.println("argc too long Error");
            return -1;
        }
        String file = readVirtualMemoryString(fileAddr, 256);
        if (file == null || !file.endsWith(".coff")){
            System.out.println("filename Error");
			return -1;
        }
        String[] argv = new String[argc];
        for(int i=0; i<argc; i++){
            byte[] argbytes = new byte[4];
            if(readVirtualMemory(argvAddr + i * 4, argbytes) != 4){
                System.out.println("readVirtualMemory Error");
                return -1;
            }
            int argAddr = Lib.bytesToInt(argbytes, 0);
            if(!checkAddrValidity(argAddr))
                return -1;
            argv[i] = readVirtualMemoryString(argAddr, 256);
        }
        UserProcess child = new UserProcess();
        children.add(child);
        if(!child.execute(file, argv))
            res = -1;
        else
            res = child.Pid;
        return res;
    }
    
    private int handleCreateOpen(int a0, boolean create) {
        int id = 0; while(id < 16 && openFiles[id] != null) id++;
	if(id == 16) return -1;
	String fileOpen = readVirtualMemoryString(a0, 256);
	openFiles[id] = fileOpen == null ? null : ThreadedKernel.fileSystem.open(fileOpen, create);
	if(openFiles[id] == null) return -1;
	return id;
    }
    
    private int handleRead(int a0, int a1, int a2) {
        if(a0 <= 0 || a0 > 16 || openFiles[a0] == null || a1 < 0 || a1 >= numPages*pageSize || a2 < 0) return -1;
	try {
	    byte[] dataRead = new byte[a2];
	    int cnt = openFiles[a0].read(dataRead, 0, a2);
	    return writeVirtualMemory(a1, dataRead, 0, cnt) == cnt ? cnt : -1;
	}
	catch(Throwable t) { return -1; }
    }
    
    private int handleWrite(int a0, int a1, int a2) {
        if(a0 <= 0 || a0 > 16 || openFiles[a0] == null || a1 < 0 || a1 >= numPages*pageSize || a2 < 0) return -1;
	try {
	    byte[] dataWrite = new byte[a2];
	    if(readVirtualMemory(a1, dataWrite, 0, a2) != a2) return -1;
	    return openFiles[a0].write(dataWrite, 0, a2) == a2 ? a2 : -1;
	}
	catch(Throwable t) { return -1; }
    }
    
    private int handleClose(int a0) {
        if(a0 <= 0 || a0 > 16 || openFiles[a0] == null) return -1;
	openFiles[a0].close();
	openFiles[a0] = null;
	return 0;
    }
    
    private int handleUnlink(int a0) {
        String fileRemove = readVirtualMemoryString(a0, 256);
	return fileRemove == null || !ThreadedKernel.fileSystem.remove(fileRemove) ? -1 : 0;
    }

    private static final int
        syscallHalt = 0,
        syscallExit = 1,
        syscallExec = 2,
        syscallJoin = 3,
        syscallCreate = 4,
        syscallOpen = 5,
        syscallRead = 6,
        syscallWrite = 7,
        syscallClose = 8,
        syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        //xhk
        System.out.println("-----start handle syscall-----");

        switch (syscall) {
            case syscallHalt:
                return handleHalt();

            // wyh
            case syscallExit:
                return handleExit(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);

            /** added by consecutivelimit */
            case syscallCreate:
	    case syscallOpen:
	        return handleCreateOpen(a0, syscall == syscallCreate);
	    case syscallRead:
	        return handleRead(a0, a1, a2);
	    case syscallWrite:
	        return handleWrite(a0, a1, a2);
	    case syscallClose:
	        return handleClose(a0);
	    case syscallUnlink:
	        return handleUnlink(a0);
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
        //xhk
        System.out.println("-----start handle exception-----");

        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                            processor.readRegister(Processor.regA0),
                            processor.readRegister(Processor.regA1),
                            processor.readRegister(Processor.regA2),
                            processor.readRegister(Processor.regA3)
                            );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;				       
                            
            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                    Processor.exceptionNames[cause]);
                Uexception = cause;
                handleExit(0);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;


    // wyh
    private int exitCode = 0;
    private int aliveP = 0;
    private int Pid = 0;
    private int numP = 0;
    private LinkedList<UserProcess> children = new LinkedList<>();
    public int NO_EXCEPTION = -1;
    private int Uexception = NO_EXCEPTION;
    public boolean joint = true;
    private UThread mainThread = null;

    
    /**
     * added by consecutivelimit
     * openFiles[i] is the file with descriptor i (0 <= i < 16), if no such file, it's null
     */
    private OpenFile[] openFiles;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    //xhk
    protected Lock MemReadWriteLock;
}
