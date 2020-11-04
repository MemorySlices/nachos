package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

import java.util.Random;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    
    waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        conditionLock.release();
        boolean intStatus = Machine.interrupt().disable();
        value = value + 1;
        waitQueue.waitForAccess(KThread.currentThread());
        KThread.sleep();
        Machine.interrupt().restore(intStatus);
        conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();
        KThread thread = waitQueue.nextThread();
        if (thread != null) {
            value = value - 1;
            thread.ready();
        }
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();
        while (true){
            KThread thread = waitQueue.nextThread();
            if (thread != null)
                thread.ready();
            else
                break;
        }
        Machine.interrupt().restore(intStatus);
    }

    public static void selfTest() {
        System.out.println("Condition2 Test Begin!");
        Lock lock = new Lock();
        Condition2 Q = new Condition2(lock);
        KThread[] a = new KThread[5];
        KThread[] b = new KThread[5];
        Random r = new Random();
        int ca=0, cb=0, op;
        for(int i=0;i<10;i++){
            while(true){
                op = r.nextInt(2);
                if(op==0&&ca<5) break;
                if(op==1&&cb<5) break;
            }
            if(op==0){
                a[ca] = new KThread(new slp("slp"+ca, Q, lock));
                a[ca].fork();
                ca ++;
            }
            else{
                b[cb] = new KThread(new wk("wk"+cb, Q, lock));
                b[cb].fork();
                cb ++;
            }
        }
        ThreadedKernel.alarm.waitUntil(10000);
        System.out.println("Condition2 Test End!");
    }
     private static class slp implements Runnable {
         String name;
         Condition2 cond;
         Lock lock;
         public slp(String nam, Condition2 con, Lock loc) {
             name = nam;
             cond = con;
             lock = loc;
         }
         public void run(){
            lock.acquire();
            long sleepTime = Machine.timer().getTime();
            System.out.println(name + ": sleep at " + sleepTime);
            cond.sleep();
            lock.release();
            long wakeTime = Machine.timer().getTime();
            System.out.println(name + ": wake at " + wakeTime);
         }
     }
     private static class wk implements Runnable {
         String name;
         Condition2 cond;
         Lock lock;
         public wk(String nam, Condition2 con, Lock loc) {
             name = nam;
             cond = con;
             lock = loc;
         }
         public void run(){
            lock.acquire();
            long comeTime = Machine.timer().getTime();
            System.out.println(name + ": come at " + comeTime);
            cond.wake();
            lock.release();
         }
     }
    private Lock conditionLock;
    public int value = 0;
    private ThreadQueue waitQueue = null;
}
