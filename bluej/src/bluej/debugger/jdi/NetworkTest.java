/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kölling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugger.jdi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import bluej.utility.Debug;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A network test which logs diagnostics information.
 *  
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class NetworkTest
{
    public static void doTest()
    {
        Debug.message("Commencing network test...");
        
        InetAddress lhost = null;
        InetAddress[] lhostByName = new InetAddress[0];
        
        try {
            lhost = InetAddress.getLocalHost();
            Debug.message("Local host address = " + lhost.getHostAddress());
            Debug.message("Local host ip = " + lhost.getHostAddress());

            lhostByName = InetAddress.getAllByName("localhost");
            Debug.message("Addresses for 'localhost':");
            for (InetAddress name : lhostByName) {
                Debug.message(" -> " + name.getHostAddress());
            }
            Debug.message("(end of list).");
        }
        catch (UnknownHostException uhe) {
            Debug.message("(!!) UnknownHostException when getting local host address!");
        }
        
        Debug.message("Creating unbound server socket...");
        try {
            ServerSocket ss = new ServerSocket();
            Debug.message("Successful.");
            try {
                ss.close();
            }
            catch (IOException ioe) {}
        }
        catch (IOException ioe) {
            Debug.message("(!!) Creation of server socket failed; message=" + ioe.getMessage());
            Debug.message("(!!) Exception class: " + ioe.getClass().getName());
        }

        InetAddress loop4addr = null;
        InetAddress loop6addr = null;
        
        try {
            loop4addr = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
            testServerAddress(loop4addr);
        }
        catch (UnknownHostException uhe) {
            Debug.message("(!!) 127.0.0.1 is unknown host: " + uhe.getMessage());
        }

        try {
            loop6addr = InetAddress.getByAddress(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1});
            testServerAddress(loop6addr);
        }
        catch (UnknownHostException uhe) {
            Debug.message("(!!) ::1 is unknown host: " + uhe.getMessage());
        }
        
        if (lhost != null && !lhost.equals(loop4addr) && !lhost.equals(loop6addr)) {
            testServerAddress(lhost);
        }
        
        for (InetAddress name : lhostByName) {
            if (!name.equals(loop4addr) && !name.equals(loop6addr) && !name.equals(lhost)) {
                testServerAddress(name);
            }
        }        
        
        Debug.message("Network test complete.");
    }
    
    /**
     * Try to listen on a particular address, and connect to the server socket.
     */
    private static void testServerAddress(final InetAddress loopAddr)
    {
        Debug.message("Creating server socket bound to " + loopAddr.getHostAddress() + "...");
        try {
            final ServerSocket ss = new ServerSocket(0, 50, loopAddr);
            Debug.message("Successful.");
            
            Thread t = new Thread() {
                @Override
                @OnThread(Tag.Worker)
                public void run()
                {
                    try {
                        Debug.message("Attempting to connect to " + loopAddr.getHostAddress() + ":" + ss.getLocalPort() + " with NO_PROXY...");
                        Socket s = new Socket(Proxy.NO_PROXY);
                        s.setSoTimeout(300);
                        s.connect(new InetSocketAddress(loopAddr, ss.getLocalPort()));
                        Debug.message("Successful.");
                        
                        Debug.message("Attempting to connect to " + loopAddr.getHostAddress() + ":" + ss.getLocalPort() + "...");
                        s = new Socket();
                        s.setSoTimeout(300);
                        s.connect(new InetSocketAddress(loopAddr, ss.getLocalPort()));
                        Debug.message("Successful.");
                    }
                    catch (IOException ioe) {
                        Debug.message("(!!) Couldn't connect to local address: " + ioe.getMessage());
                        Debug.message("(!!) Exception class: " + ioe.getClass().getName());
                    }
                }
            };
            
            t.start();
            ss.setSoTimeout(500); // wait for half a second max
            try {
                ss.accept();
                ss.accept();
            }
            catch (IOException ioe) {
                Debug.message("(!!) Couldn't accept connection: " + ioe.getMessage());
                Debug.message("(!!) Exception class: " + ioe.getClass().getName());
            }
            
            try {
                t.join(500);
            }
            catch (InterruptedException ie) {}
            
            try {
                ss.close();
            }
            catch (IOException ioe) {}
        }
        catch (IOException ioe) {
            Debug.message("(!!) Creation of server socket failed; message=" + ioe.getMessage());
            Debug.message("(!!) Exception class: " + ioe.getClass().getName());
        }
    }
}
