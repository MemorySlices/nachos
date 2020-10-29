package nachos.threads;

import nachos.machine.*;

import java.util.Random;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        boolean intStatus = Machine.interrupt().disable();
        lock.acquire();
        while(flag){
            speaker.sleep();
        }
        this.word = word;
        flag = true;
        listener.wakeAll();
        lock.release();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        boolean intStatus = Machine.interrupt().disable();
        lock.acquire();
        while(!flag){
            listener.sleep();
        }
        int word = this.word;
        flag = false;
        speaker.wakeAll();
        lock.release();
        Machine.interrupt().restore(intStatus);
        return word;
    }
    public static void selfTest(){
        System.out.println("Communicator Test Begin!");
        KThread[] speaker = new KThread[5];
        KThread[] listener = new KThread[5];
        Communicator com = new Communicator();
        Random r = new Random();
        int cnt_s=0, cnt_l=0;
        for(int i=0;i<10;i++){
            int op = 0;
            while(true){
                op = r.nextInt(2);
                if(op==0&&cnt_s<5)
                    break;
                if(op==1&&cnt_l<5)
                    break;
            }
            if(op==0){
                speaker[cnt_s] = new KThread(new Speaker("speaker" + cnt_s, com));
                speaker[cnt_s].fork();
                cnt_s++;
            }
            else{
                listener[cnt_l] = new KThread(new Listener("listener" + cnt_l, com));
                listener[cnt_l].fork();
                cnt_l++;
            }
        }
        ThreadedKernel.alarm.waitUntil(10000);
        System.out.println("Communicator Test End!");
    }
    private static class Speaker implements Runnable {
        String name;
        Random r;
        Communicator com;
        public Speaker(String nam, Communicator comm) {
            name = nam;
            r = new Random();
            com = comm;
        }
        public void run() {
            int sig = r.nextInt(1000);
            System.out.println(name + ":" + sig);
            com.speak(sig);
        }
    }
    private static class Listener implements Runnable {
        String name;
        Communicator com;
        public Listener(String nam, Communicator comm) {
            name = nam;
            com = comm;
        }
        public void run() {
            int key = com.listen();
            System.out.println(name + ":" + key);
            
        }
    }
    private int word;
    private boolean flag = false;
    private Lock lock = new Lock();
    private Condition2 listener = new Condition2(lock);
    private Condition2 speaker = new Condition2(lock);
}
