/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011,2012  Poul Henriksen and Michael Kolling 
 
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
package rmiextension;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

/**
 * A socket factory to prevent the use of the system socks server, and force connection
 * only through the loopback interface (127.0.0.1 address).
 */
public class LocalSocketFactory implements RMIClientSocketFactory, RMIServerSocketFactory
{
    @Override
    public Socket createSocket(String host, int port) throws IOException
    {
        Socket s = new Socket(Proxy.NO_PROXY);
        // Note we ignore the provided host and use 127.0.0.1 instead
        SocketAddress sa = new InetSocketAddress("127.0.0.1", port);
        s.connect(sa, 5000); // 5 second timeout
        return s;
    }
    
    @Override
    public ServerSocket createServerSocket(int port) throws IOException
    {
        InetAddress ia = InetAddress.getByAddress(new byte[] {127,0,0,1});
        return new ServerSocket(port, 50, ia); // 50 is the documented default
    }
    
    // Surprisingly, we need to override these;
    // see http://docs.oracle.com/javase/7/docs/technotes/guides/rmi/faq.html#customsocketreuse 
    
    @Override
    public int hashCode()
    {
        return 0xDEED0000;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        return obj != null && getClass() == obj.getClass();
    }
}
