/* Copyright(c) 2005 Sun Microsystems, Inc.  All Rights Reserved. */

package greenfoot.export.gameserver;
import java.io.*;
import java.net.*;

/**
 * @author James Gosling
 * @created March 22, 2006
 */
public abstract class Endpoint {
    public final UTCL utcl;
    private StreamOfThingsWriter out;
    private IOException lastError = null;
    Socket sock;
    CommandSet commands;
    public Endpoint(Object target) {
        utcl = new UTCL();
        utcl.target("t", target!=null ? target : this);
    }
    public Endpoint() {
        utcl = new UTCL();
        utcl.target("t",this);
    }
    public Endpoint(Socket sock, Object target) {
        this(target);
        connect(sock);
    }
    public Endpoint(String host, int port, boolean persist, Object target) throws UnknownHostException {
        this(target);
        connect(host, port, persist);
    }
    public IOException getError() { return lastError; }
    public boolean isBroken() { return lastError!=null; }
    public final synchronized StreamOfThingsWriter getOut() throws IOException {
        StreamOfThingsWriter o;
        if(closed && bePersistant && chost!=null)
            reconnect();
        while((o=out)==null && !closed && lastError==null) 
            try {
//                System.err.println("Waiting for 'out'");
                wait();
            } catch(InterruptedException ie) {
            }
        if(o==null)
            throw lastError==null
                    ? new IOException("Connection closed")
                    : lastError;
        return o;
    }
    private boolean closed = false;
    private final synchronized void setOut(StreamOfThingsWriter o) {
        out = o;
        notifyAll();
    }
    public final String target(Object v) { return utcl.target(v); }
    public final boolean isConnected() { return out!=null; }
    protected void onReconnect() {}
    public void gotResponse() {}
    public synchronized void connect(Socket s) {
        if(sock==s) return;
        try {
            if(sock!=null) {
                System.err.println("**DUPLICATE CONNECT");
                sock.close();
            }
            sock = s;
//            System.err.println("Connect "+s);
            new Thread() {
                public void run() {
                    try {
                        setName("Endpoint "+this);
                        utcl.connect(new BufferedInputStream(sock.getInputStream()));
                        utcl.readExec();
                        setState(null,"closed");
                    } catch (IOException ex) {
                        lastError = ex;
//                        ex.printStackTrace();
                        setState(ex,"read error");
                    }
                }
            }.start();
            setOut(new StreamOfThingsWriter(sock.getOutputStream()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    public void connectionBroken() {
        setState(null, "BROKEN");
    }
    public synchronized void setState(Exception e, String s) {
//        System.err.println(s);
        closed = true;
        if(utcl!=null)
            try {
                utcl.close();
            } catch(Throwable t) {
            }
        if(out!=null)
            try {
                out.close();
            } catch(Throwable t) {
            }
        if(sock!=null)
            try {
                sock.close();
            } catch(Throwable t) {
            }
        sock = null;
        out = null;
        notifyAll();
        fireBreakage(e);
    }
    private BreakageListener[] breakage = null;
    private int nbreakage = 0;
    public void addBreakageListener(BreakageListener bl) {
        if(breakage==null) {
            breakage = new BreakageListener[]{bl};
            nbreakage = 1;
        }
        if(findBreakage(bl)>=0) return;
        if(nbreakage>=breakage.length) {
            BreakageListener[] nbl = new BreakageListener[nbreakage*2];
            System.arraycopy(breakage,0,nbl,0,nbreakage);
            breakage = nbl;
        }
        breakage[nbreakage++] = bl;
    }
    public void removeBreakageListener(BreakageListener bl) {
        int slot = findBreakage(bl);
        if(slot<0) return;
        System.arraycopy(breakage,slot+1,breakage,slot,nbreakage-slot);
        nbreakage--;
        breakage[nbreakage] = null;
    }
    private int findBreakage(BreakageListener bl) {
        for(int i = nbreakage; --i>=0; )
            if(breakage[i]==bl) return i;
        return -1;
    }
    private void fireBreakage(Throwable t) {
        for(int i = 0; i<nbreakage; i++)
            breakage[i].connectionBroken(t, this);
    }
    private String chost;
    private int cport;
    private boolean bePersistant;
    public synchronized void connect(String host, final int port, final boolean persist) throws UnknownHostException {
        if(host==null) host = System.getenv("TARGETHOST");
       
            chost = host; cport = port; bePersistant = persist;
            closed = false;
            lastError = null;
            sock = null;
            out = null;
            final InetAddress addr = host==null
                    ? InetAddress.getLocalHost()
                    : InetAddress.getByName(host);
            new Thread() {
                public void run() {
                    setName("connect "+addr);
                    try {
                        connectLoop(addr,port);
                    }
                    catch (IOException e) {
                       // lastError = e;
                        //setState(e,"connect error");
                        error("Cannot connect to server. Cause: " + e.getMessage());
                    }
                }
            }.start();
        
    }
    static final long initialRetryWait = 1000;
    static final long maxRetryWait = 5000;//5*60*1000;
    static final double retryExponential = 1.5;
    
    private synchronized final void connectLoop(InetAddress addr, int port) throws IOException 
    {
        long retrywait = initialRetryWait;
        //while (true)
           // try {
                connect(new Socket(addr, port));
               // break; // TODO make persist work
           /* }
            catch (IOException ioe) {
                System.err.println("***Can't connect to server: " + ioe + "\n\tRetrying " + retrywait);
                try {
                    Thread.currentThread().sleep(retrywait);
                    retrywait = retrywait < (int) (maxRetryWait / retryExponential)
                            ? (int) (retrywait * retryExponential)
                            : maxRetryWait;
                }
                catch (Throwable t) {}
            }*/
    }

    public void reconnect() throws UnknownHostException
    {
        if (out == null) {
            connect(chost, cport, bePersistant);
            onReconnect();
        }
    }
    
    public abstract void error(String s);
}
