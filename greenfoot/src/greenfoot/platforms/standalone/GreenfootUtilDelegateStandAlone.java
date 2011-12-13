/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.GreenfootStorageVisitor;
import greenfoot.PlayerData;
import greenfoot.platforms.GreenfootUtilDelegate;
import greenfoot.util.GreenfootStorageException;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GreenfootUtilDelegateStandAlone implements GreenfootUtilDelegate
{
    private SocketChannel socket;
    private boolean attemptedConnection;
    public static boolean storageStandalone = false;
    public static String storageHost;
    public static String storagePort;
    public static String storagePasscode;
    public static String storageScenarioId;
    public static String storageUserId;
    public static String storageUserName;
    
    /** Holds images for classes. Avoids loading the same image twice. Key is the filename */
    public static Map<String, GreenfootImage> classImages = new HashMap<String, GreenfootImage>();
    
    @Override
    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
    throws IOException
    {
        // Not needed in stand alone
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
    public void removeCachedImage(String fileName)
    {
        synchronized (classImages) {
            classImages.remove(fileName);
        }
    }

    @Override
    public boolean addCachedImage(String fileName, GreenfootImage image)
    {
        synchronized (classImages) {
            classImages.put(fileName, image);
        }
        return true;
    }
    
    @Override
    public GreenfootImage getCachedImage(String fileName)
    {
        synchronized (classImages) {
            return classImages.get(fileName);
        }
    }
    
    @Override
    public boolean isNullCachedImage(String fileName)
    {
        if (classImages.containsKey(fileName) && classImages.get(fileName)==null){
            return true;
        }
        return false;
    }
    
    @Override
    public void displayMessage(Component parent, String messageText)
    {
        System.err.println(messageText);
    }

    @Override
    public boolean isStorageSupported()
    {
        try
        {
            ensureStorageConnected();
            return true;
        }
        catch (GreenfootStorageException e)
        {
            return false;
        }
    }
    
    private void ensureStorageConnected() throws GreenfootStorageException
    {
        if (socket != null && socket.isConnected())
            return; //Already connected
        
        if (socket == null && attemptedConnection)
            throw new GreenfootStorageException("Already failed to connect to storage server");
            // We don't continually try to reconnect -- probably a firewall blocked us
        
        if (!storageStandalone)
            throw new GreenfootStorageException("Standalone storage not supported");
            // This means the gallery didn't give us the go-ahead via an applet param
        
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
            
            attemptedConnection = true;
            
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
    
    private ByteBuffer readResponse() throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate(4);
        socket.read(buf);
        buf.flip();
        buf = ByteBuffer.allocate(buf.getInt());
        socket.read(buf);
        buf.flip();
        return buf;
    }

    @Override
    public PlayerData getCurrentUserData()
    {
        try
        {
            ensureStorageConnected();
            ByteBuffer buf = makeRequest(1);
            buf.put((byte) 1);
            buf.flip();
            socket.write(buf);
            
            buf = readResponse();
            return readLines(buf, 1)[0];
        }
        catch (IOException e)
        {
            socket = null;
            //throw new GreenfootStorageException("Error communicating with server: " + e.getMessage());
            return null;
        }
        catch (BufferUnderflowException e)
        {
            socket = null;
            //throw new GreenfootStorageException("Server sent aborted message");
            return null;
        }
        catch (GreenfootStorageException e)
        {
            socket = null;
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

    private PlayerData[] readLines(ByteBuffer buf, int numLines) throws BufferUnderflowException
    {
        // Read number of ints then number of Strings (username not included)
        int numInts = buf.getInt();
        int numStrings = buf.getInt();
        
        PlayerData[] r = new PlayerData[numLines]; 
        
        for (int line = 0; line < numLines; line++)
        {
            String userName = getString(buf);
            r[line] = GreenfootStorageVisitor.allocate(userName);
            r[line].setScore(buf.getInt());
            for (int i = 0; i < numInts; i++)
            {
                int x = buf.getInt();
                if (i < PlayerData.NUM_INTS)
                    r[line].setInt(i, x);
            }
            for (int i = 0; i < numStrings; i++)
            {
                String s = getString(buf);
                if (i < PlayerData.NUM_STRINGS)
                    r[line].setString(i, s);
            }
        }
        return r;
    }

    @Override
    public boolean storeCurrentUserData(PlayerData data)
    {
        try
        {
            ensureStorageConnected();
            int payloadLength = 0;
            payloadLength += 4 + 4 + (1 + PlayerData.NUM_INTS) * 4;
            for (int i = 0; i < PlayerData.NUM_STRINGS; i++)
                payloadLength += stringSize(data.getString(i));
            
            ByteBuffer buf = makeRequest(1 + payloadLength);
            buf.put((byte) 2);
            buf.putInt(data.getScore());
            buf.putInt(PlayerData.NUM_INTS);
            buf.putInt(PlayerData.NUM_STRINGS);
            for (int i = 0; i < PlayerData.NUM_INTS; i++)
                buf.putInt(data.getInt(i));
            for (int i = 0; i < PlayerData.NUM_STRINGS; i++)
                putString(buf, data.getString(i));
            buf.flip();
            socket.write(buf);
            
            buf = readResponse();
            byte code = buf.get();
            if (code != 0)
            {
                socket = null;
                throw new GreenfootStorageException("Error storing data, code: " + Byte.toString(code));
            }
            
            return true;
        }
        catch (IOException e)
        {
            socket = null;
            //throw new GreenfootStorageException("Error communicating with server: " + e.getMessage());
            return false;
        }
        catch (BufferUnderflowException e)
        {
            socket = null;
            //throw new GreenfootStorageException("Server sent aborted message");
            return false;
        }
        catch (GreenfootStorageException e)
        {
            socket = null;
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
    public List<PlayerData> getTopUserData(int limit)
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
            PlayerData[] storage = readLines(buf, numUsers);
            
            List<PlayerData> r = new ArrayList<PlayerData>();
            for (PlayerData s : storage)
            {
                r.add(s);
            }
            return r;
        }
        catch (IOException e)
        {
            socket = null;
            //System.err.println("Error communicating with server: " + e.getMessage());
            return null;
        }
        catch (BufferUnderflowException e)
        {
            socket = null;
            //throw new GreenfootStorageException("Server sent aborted message");
            return null;
        }
        catch (GreenfootStorageException e)
        {
            socket = null;
            return null;
        }
    }
    
    @Override
    public List<PlayerData> getNearbyUserData(int limit)
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
            PlayerData[] storage = readLines(buf, numUsers);
            
            List<PlayerData> r = new ArrayList<PlayerData>();
            for (PlayerData s : storage)
            {
                r.add(s);
            }
            return r;
        }
        catch (IOException e)
        {
            socket = null;
            //System.err.println("Error communicating with server: " + e.getMessage());
            return null;
        }
        catch (BufferUnderflowException e)
        {
            socket = null;
            //throw new GreenfootStorageException("Server sent aborted message");
            return null;
        }
        catch (GreenfootStorageException e)
        {
            socket = null;
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
                return GreenfootStorageVisitor.readImage(fileData);
            }
            catch (IllegalArgumentException e)
            {
                // We can't read the image, not a permanent failure:
                return null;
            }
        }
        catch (IOException e)
        {
            socket = null;
            //throw new GreenfootStorageException("Error communicating with server: " + e.getMessage());
            return null;
        }
        catch (GreenfootStorageException e)
        {
            socket = null;
            return null;
        }
    }

    @Override
    public String getUserName()
    {
        return storageUserName;
    }
    
    
}
