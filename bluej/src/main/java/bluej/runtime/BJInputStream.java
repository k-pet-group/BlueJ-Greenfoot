/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.runtime;

import java.io.IOException;
import java.io.InputStream;

import bluej.terminal.InputBuffer;

/**
 * BlueJ input stream. An input stream filter to process "End of file"
 * signals (CTRL-Z or CTRL-D) from a terminal
 * 
 * @author Davin McCall
 * @version $Id: BJInputStream.java 7142 2010-02-17 23:47:00Z davmac $
 */
public class BJInputStream extends InputStream
{
    private InputStream source;
    int buffoffset = 0;
    
    boolean endOfLine = false;
    boolean exOnEOL = false;
    
    /**
     * Construct a BJ
     * @param source  The source input stream, generally System.in
     */
    BJInputStream(InputStream source)
    {
        this.source = source;
    }
    
    public int read() throws IOException {
        // show terminal on input
        ExecServer.showTerminalOnInput();

        if (exOnEOL && endOfLine)
            throw new IOException();
        
        int n = source.read();
        
        // Check for EOF signal
        if (n == InputBuffer.EOF_CHAR)
            return -1;
        
        // Are we line-buffering?
        if (exOnEOL && n == '\n')
            endOfLine = true;
        
        return n;
    }
    
    public int read(byte [] b, int off, int len) throws IOException
    {
        // show terminal on input
        ExecServer.showTerminalOnInput();

        // reads are line buffered. If a complete line is read, we don't
        // want to read any more. So we flag this and it is handled in the
        // read() method.
        exOnEOL = true;
        int n = super.read(b, off, len);
        exOnEOL = false;
        endOfLine = false;
        return n;
    }
    
    public int available() throws IOException
    {
        return source.available();
    }
}
