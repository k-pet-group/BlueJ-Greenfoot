package bluej.groupwork;

import bluej.Config;

/**
 ** @version $Id: Sync.java 504 2000-05-24 04:44:14Z markus $
 ** @author Markus Ostman
 ** 
 **
 ** A synchronizer class
 **
 **
 **
 **/

public class Sync
{
    private boolean ok;

    public static Sync s = new Sync();

    public Sync()
    {
        this.ok = false;
    }

    public synchronized void callWait()
    {
        this.ok = false;
        try {
            wait();
        } catch(InterruptedException e) {
        }
    }

    public synchronized void callNotify(boolean ok)
    {
        this.ok = ok;
	notify();
    }

//     public synchronized void setOk(boolean ok)
//     {
//         this.ok = ok;
//     }

    public synchronized boolean getOk()
    {
        return ok;
    }
}


