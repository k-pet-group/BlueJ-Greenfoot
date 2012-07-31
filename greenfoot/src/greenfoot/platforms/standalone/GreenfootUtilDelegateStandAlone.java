/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms.standalone;

import greenfoot.GreenfootImage;
import greenfoot.UserInfoVisitor;
import greenfoot.UserInfo;
import greenfoot.platforms.GreenfootUtilDelegate;
import greenfoot.util.GreenfootStorageException;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of GreenfootUtilDelegate for standalone applications.
 */
public class GreenfootUtilDelegateStandAlone implements GreenfootUtilDelegate
{
    private SocketChannel socket;
    private boolean failedLastConnection;
    private boolean firstStorageException = true;
    private boolean storageStandalone;
    private String storageHost;
    private String storagePort;
    private String storagePasscode;
    private String storageScenarioId;
    private String storageUserId;
    private String storageUserName;
    
    public GreenfootUtilDelegateStandAlone(boolean storageStandalone,
            String storageHost, String storagePort, String storagePasscode,
            String storageScenarioId, String storageUserId,
            String storageUserName)
    {
        this.storageStandalone = storageStandalone;
        this.storageHost = storageHost;
        this.storagePort = storagePort;
        this.storagePasscode = storagePasscode;
        this.storageScenarioId = storageScenarioId;
        this.storageUserId = storageUserId;
        this.storageUserName = storageUserName;
    }
    
    @Override
    public URL getResource(String path)
    {
        // Resources from the standalone should always be in a jar, which means
        // they should contain the character "!". If we do get a URL back, and
        // it doesn't contain a ! it is probably because it didn't exists, but
        // the webserver produced an error page at the given URL instead of
        // returning a fail. Therefore, we need to explicitly test for the
        // existence of a ! in the returned URL.
        URL res = this.getClass().getClassLoader().getResource(path);
        if (res != null && res.toString().contains("!")) {  
            return res;
        }
        else {
            if (path.indexOf('\\') != -1) {
                // Looks suspiciously like a Windows path.
                path = path.replace('\\', '/');
                res = this.getClass().getClassLoader().getResource(path);
                if (res != null && res.toString().contains("!")) {  
                    return res;
                }
            }
            return null;
        }
    }
    
    @Override
    public Iterable<String> getSoundFiles()
    {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("soundindex.list");
        ArrayList<String> r = new ArrayList<String>();
        
        if (is != null)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            try
            {
                while ((line = reader.readLine()) != null)
                {
                    r.add(line);
                }
            }
            catch (IOException e)
            {
                //Silently stop
            }
        }
        
        // May just be blank if there's a problem:
        return r;
    }    

    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    @Override
    public String getGreenfootLogoPath()
    {    
        return this.getClass().getClassLoader().getResource("greenfoot.png").toString();
    }
    
    @Override
    public void displayMessage(Component parent, String messageText)
    {
        System.err.println(messageText);
    }
    
    /**
     * Closes the connection (well, silently drops it), but allows
     * a subsequent connection attempt
     */
    private void closeConnection(Exception e)
    {
        e.printStackTrace();
        socket = null;
        failedLastConnection = false;
    }

    @Override
    public boolean isStorageSupported()
    {
        try
        {
            ensureStorageConnected();
            return getCurrentUserInfo() != null;
        }
        catch (GreenfootStorageException e)
        {
            // Let the user know why it didn't connect.  This will go to the Java console,
            // which is only shown if the user has specifically turned it on.
            // Make sure we only print one exception from this method, otherwise
            // someone who calls it a lot will see lots of console spam (esp in standalone
            // applets):
            if (firstStorageException)
            {
                e.printStackTrace();
            }
            firstStorageException = false;
            return false;
        }
    }
    
    /**
     * Tries to connect to the server, if not already connected.
     * 
     * If it returns without throwing an exception, you can assume you are connected.
     * 
     * @throws GreenfootStorageException if there is a problem
     */
    private void ensureStorageConnected() throws GreenfootStorageException
    {
        if (socket != null && socket.isConnected())
            return; //Already connected
        
        if ((socket == null || !socket.isConnected()) && failedLastConnection)
            throw new GreenfootStorageException("Already failed to connect to storage server on last attempt");
            // We don't continually try to reconnect -- probably a firewall blocked us
        
        if (!storageStandalone)
            throw new GreenfootStorageException("Standalone storage not supported");
            // This means the gallery didn't give us the go-ahead via an applet param
        
        System.err.println("Attempting to reconnect to storage server");
        
        int userId;
        try
        {
            userId = Integer.parseInt(storageUserId);
            if (userId < 0)
                throw new GreenfootStorageException("User not logged in");
        }
        catch (NumberFormatException e)
        {
            throw new GreenfootStorageException("Invalid user ID");
        }
        
        short port;
        try
        {
            port = Short.parseShort(storagePort); 
        }
        catch (NumberFormatException e)
        {
            throw new GreenfootStorageException("Error connecting to storage server -- invalid port: " + e.getMessage());
        }
        
        try
        {
            if (storagePasscode == null)
                throw new GreenfootStorageException("Could not find passcode to send back to server");
            
            failedLastConnection = true; // True unless we reach the end
            
            socket = SocketChannel.open();
            if (!socket.connect(new InetSocketAddress(storageHost, port)))
            {
                socket = null;
                throw new GreenfootStorageException("Could not connect to storage server");
            }
            
            ByteBuffer buffer = makeRequest((storagePasscode.length() / 2) + 4 + 4);
            for (int i = 0; i < storagePasscode.length() / 2; i++)
            {
                // Because bytes are parsed as signed, we must use short to be able to pass bytes above 0x80
                byte b = (byte)(0xFF & Short.parseShort(storagePasscode.substring(i * 2, i * 2 + 2), 16));
                buffer.put(b);
            }
            try
            {
                buffer.putInt(Integer.parseInt(storageScenarioId));
                buffer.putInt(userId);
            }
            catch (NumberFormatException e)
            {
                socket = null;
                throw new GreenfootStorageException("Invalid scenario ID: " + e.getMessage());
            }
            buffer.flip();
            socket.write(buffer);
            
            failedLastConnection = false; // We succeeded, so didn't fail!
        }
        catch (IOException e)
        {
            socket = null;
            throw new GreenfootStorageException("Error connecting to storage server: " + e.getMessage());
        }
    }
    
    private ByteBuffer makeRequest(int plusBytes)
    {
        ByteBuffer buf = ByteBuffer.allocate(4 + plusBytes);
        buf.putInt(plusBytes); // bytes after this point
        
        return buf;
    }
    
    private void readFullBuffer(ByteBuffer buf, int amount) throws IOException
    {
        int totalBytes = 0;
        while (totalBytes < amount)
        {
            int bytesRead = socket.read(buf);
            if (bytesRead > 0)
                totalBytes += bytesRead;
            else
                throw new IOException("Zero or negative bytes read from socket");
        }
        buf.flip();
    }
    
    private ByteBuffer readResponse() throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate(4);
        readFullBuffer(buf, 4);
        int size = buf.getInt();
        buf = ByteBuffer.allocate(size);
        readFullBuffer(buf, size);
        return buf;
    }

    @Override
    public UserInfo getCurrentUserInfo()
    {
        try
        {
            ensureStorageConnected();
            ByteBuffer buf = makeRequest(1);
            buf.put((byte) 1);
            buf.flip();
            socket.write(buf);
            
            buf = readResponse();
            if (1 != buf.getInt()) // Should be exactly one user
                return null; // Error, or we're not logged in
            return readLines(buf, 1, true)[0];
        }
        catch (IOException e)
        {
            closeConnection(e);
            //throw new GreenfootStorageException("Error communicating with server: " + e.getMessage());
            return null;
        }
        catch (BufferUnderflowException e)
        {
            closeConnection(e);
            //throw new GreenfootStorageException("Server sent aborted message");
            return null;
        }
        catch (GreenfootStorageException e)
        {
            closeConnection(e);
            return null;
        }
    }
    
    private static String getString(ByteBuffer buf) throws BufferUnderflowException
    {
        int len = buf.getShort(); //2-bytes for length
        if (len == -1)
            return null;
        
        char[] cs = new char[len];
        for (int i = 0; i < len; i++)
        {
            cs[i] = buf.getChar();
        }
        return new String(cs);
    }
    
    private static void putString(ByteBuffer buf, String value)
    {
        if (value == null)
        {
            buf.putShort((short) -1);
        }
        else
        {
            buf.putShort((short)value.length()); //2-bytes for length
            for (int i = 0; i < value.length(); i++)
            {
                buf.putChar(value.charAt(i));
            }
        }
    }

    private UserInfo[] readLines(ByteBuffer buf, int numLines, boolean useSingleton) throws BufferUnderflowException
    {
        // Read number of ints then number of Strings (username not included)
        int numInts = buf.getInt();
        int numStrings = buf.getInt();
        
        UserInfo[] r = new UserInfo[numLines]; 
        
        for (int line = 0; line < numLines; line++)
        {
            String userName = getString(buf);
            int score = buf.getInt();
            int rank = buf.getInt();
            r[line] = UserInfoVisitor.allocate(userName, rank, useSingleton ? getUserName() : null);
            r[line].setScore(score);
            for (int i = 0; i < numInts; i++)
            {
                int x = buf.getInt();
                if (i < UserInfo.NUM_INTS)
                    r[line].setInt(i, x);
            }
            for (int i = 0; i < numStrings; i++)
            {
                String s = getString(buf);
                if (i < UserInfo.NUM_STRINGS)
                    r[line].setString(i, s);
            }
        }
        return r;
    }

    @Override
    public boolean storeCurrentUserInfo(UserInfo data)
    {
        try
        {
            ensureStorageConnected();
            int payloadLength = 0;
            payloadLength += 4 + 4 + (1 + UserInfo.NUM_INTS) * 4;
            for (int i = 0; i < UserInfo.NUM_STRINGS; i++)
                payloadLength += stringSize(data.getString(i));
            
            ByteBuffer buf = makeRequest(1 + payloadLength);
            buf.put((byte) 2);
            buf.putInt(data.getScore());
            buf.putInt(UserInfo.NUM_INTS);
            buf.putInt(UserInfo.NUM_STRINGS);
            for (int i = 0; i < UserInfo.NUM_INTS; i++)
                buf.putInt(data.getInt(i));
            for (int i = 0; i < UserInfo.NUM_STRINGS; i++)
                putString(buf, data.getString(i));
            buf.flip();
            socket.write(buf);
            
            buf = readResponse();
            byte code = buf.get();
            if (code != 0)
            {
                // Connection will be closed in catch block beneath:
                throw new GreenfootStorageException("Error storing data, code: " + Byte.toString(code));
            }
            
            return true;
        }
        catch (IOException e)
        {
            closeConnection(e);
            //throw new GreenfootStorageException("Error communicating with server: " + e.getMessage());
            return false;
        }
        catch (BufferUnderflowException e)
        {
            closeConnection(e);
            //throw new GreenfootStorageException("Server sent aborted message");
            return false;
        }
        catch (GreenfootStorageException e)
        {
            closeConnection(e);
            return false;
        }
    }

    private static int stringSize(String string)
    {
        if (string == null)
            return 2;
        else
            return 2 + (2 * string.length());
    }

    @Override
    public List<UserInfo> getTopUserInfo(int limit)
    {
        try
        {
            ensureStorageConnected();
            ByteBuffer buf = makeRequest(1 + 4);
            buf.put((byte) 3);
            buf.putInt(limit);
            buf.flip();
            socket.write(buf);
            
            buf = readResponse();
            int numUsers = buf.getInt();
            UserInfo[] storage = readLines(buf, numUsers, false);
            
            List<UserInfo> r = new ArrayList<UserInfo>();
            for (UserInfo s : storage)
            {
                r.add(s);
            }
            return r;
        }
        catch (IOException e)
        {
            closeConnection(e);
            //System.err.println("Error communicating with server: " + e.getMessage());
            return null;
        }
        catch (BufferUnderflowException e)
        {
            closeConnection(e);
            //throw new GreenfootStorageException("Server sent aborted message");
            return null;
        }
        catch (GreenfootStorageException e)
        {
            closeConnection(e);
            return null;
        }
    }
    
    @Override
    public List<UserInfo> getNearbyUserInfo(int limit)
    {
        try
        {
            ensureStorageConnected();
            ByteBuffer buf = makeRequest(1 + 4);
            buf.put((byte) 5);
            buf.putInt(limit);
            buf.flip();
            socket.write(buf);
            
            buf = readResponse();
            int numUsers = buf.getInt();
            if (numUsers < 0)
                return null; // Error, or we're not logged in
            
            UserInfo[] storage = readLines(buf, numUsers, false);
            
            List<UserInfo> r = new ArrayList<UserInfo>();
            for (UserInfo s : storage)
            {
                r.add(s);
            }
            return r;
        }
        catch (IOException e)
        {
            closeConnection(e);
            //System.err.println("Error communicating with server: " + e.getMessage());
            return null;
        }
        catch (BufferUnderflowException e)
        {
            closeConnection(e);
            //throw new GreenfootStorageException("Server sent aborted message");
            return null;
        }
        catch (GreenfootStorageException e)
        {
            closeConnection(e);
            return null;
        }
    }

    @Override
    public GreenfootImage getUserImage(String userName)
    {
        if (userName == null || userName.equals(""))
            userName = storageUserName;
        
        try
        {
            ensureStorageConnected();
            ByteBuffer buf = makeRequest(1 + 2 + (2*userName.length()));
            buf.put((byte) 4);
            putString(buf, userName);
            buf.flip();
            socket.write(buf);
            
            buf = readResponse();
            int numBytes = buf.getInt();
            byte[] fileData = new byte[numBytes];
            buf.get(fileData);
            
            // We can't create a temporary file and read that back in,
            // because we are in an applet, so we must pass the file contents
            // directly to a hidden constructor:
        
            try
            {
                return UserInfoVisitor.readImage(fileData);
            }
            catch (IllegalArgumentException e)
            {
                // We can't read the image, not a permanent failure:
                return null;
            }
        }
        catch (IOException e)
        {
            closeConnection(e);
            //throw new GreenfootStorageException("Error communicating with server: " + e.getMessage());
            return null;
        }
        catch (GreenfootStorageException e)
        {
            closeConnection(e);
            return null;
        }
    }

    @Override
    public String getUserName()
    {
        return storageUserName;
    }
    
    
}
