/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.export.mygame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.multipart.FilePart;

/**
 * A FilePart which tracks upload progress.
 * 
 * @author Davin McCall
 */
public class ProgressTrackingPart extends FilePart
{
    private MyGameClient listener;
    
    public ProgressTrackingPart(String partName, File file, MyGameClient listener)
        throws FileNotFoundException
    {
        super(partName, file);
        this.listener = listener;
    }
    
    @Override
    protected void sendData(OutputStream output) throws IOException
    {
        if (lengthOfData() == 0) {
            return;
        }
        
        byte [] buf = new byte[4096];
        InputStream istream = getSource().createInputStream();
        try {
            int len = istream.read(buf);
            while (len != -1) {
                output.write(buf, 0, len);
                listener.progress(len);
                len = istream.read(buf);
            }
        }
        finally {
            istream.close();
        }
    }
}
