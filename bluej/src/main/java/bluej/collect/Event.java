/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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
package bluej.collect;

import java.util.List;
import java.util.Map;

import org.apache.http.entity.mime.MultipartEntity;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An Event to be submitted to the server.
 */
interface Event
{
    /**
     * Given the current versions of the files as we have last sent them to the server,
     * forms a new record to be sent to the server
     * 
     * @param sequenceNum The sequence number to use for the event
     * @param fileVersions Our local version of the files, as we have last
     * successfully sent them to the server.  Maps a file identifier to a list
     * of lines in the file
     * @return A MultipartEntity to send to the server
     */
    @OnThread(Tag.Worker)
    MultipartEntity makeData(int sequenceNum, Map<FileKey, List<String> > fileVersions);
    
    /**
     * A callback that is called after the event has been successfully sent to
     * the server.  If necessary, it should update the passed-in map with the
     * file contents
     * @param fileVersions Map, to be modified by the method
     */
    void success(Map<FileKey, List<String> > fileVersions);
}