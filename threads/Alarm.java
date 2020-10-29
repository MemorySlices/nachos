package nachos.threads;

import nachos.machine.*;

import java.util.*;


class cmp implements Comparator<KThread>{
    public int compare(KThread a, KThread b){
        if(a.getWakeTime() > b.getWakeTime())
            return 1;
        else
            return -1;
    }
}

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        Sleep = new TreeSet<KThread>(new cmp());
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() { timerInterrupt(); }
        });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        boolean intStatus = Machine.interrupt().disable();
        boolean awake = false;
        while(Sleep.isEmpty()!=true && Sleep.first().getWakeTime() < Machine.timer().getTime()){
            Sleep.pollFirst().ready();
            awake = true;
        }
        Machine.interrupt().restore(intStatus);
        if(awake){
            awake = false;
            KThread.currentThread().yield();
        }
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
        long wakeTime = Machine.timer().getTime() + x;
        boolean intStatus = Machine.interrupt().disable();
        KThread.currentThread().setWakeTime(wakeTime);
        Sleep.add(KThread.currentThread());
        KThread.sleep();
        Machine.interrupt().restore(intStatus);
    }
	public static void selfTest(){
		System.out.println("Alarm Test Begin!");
		Random r = new Random();
		for(int i=0;i<5;i++){
			int T = r.nextInt(1000);
			KThread a = new KThread(new AT(T, "Alarm" + i));
			a.fork();
			ThreadedKernel.alarm.waitUntil(r.nextInt((int)(T / 2) + 1));
		}
		System.out.println("Alarm Test End!");
	}
	private static class AT implements Runnable {
        String name;
		long time;
        public AT(int T, String nam) {
            name = nam;
			time = T;
        }
        public void run() {
            long sleepTime = Machine.timer().getTime();
            System.out.println(name + ": sleep at " + sleepTime + ", for " + time);
            ThreadedKernel.alarm.waitUntil(time);
            long wakeTime = Machine.timer().getTime();
            System.out.println(name + ": wake at " + wakeTime);
            if (wakeTime - sleepTime < time)
                System.out.println(name + ": Wrong <");
            if (wakeTime - sleepTime > time + 500)
                System.out.println(name + ": Wrong >");
        }
    }
    private TreeSet<KThread> Sleep;
}
