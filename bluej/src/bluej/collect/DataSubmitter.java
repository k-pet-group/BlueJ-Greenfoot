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

import java.io.File;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import bluej.pkgmgr.Project;

/**
 * This class that handles submitting compilation data to the remote server.
 * 
 * The class has nothing to do with collecting the data, and deliberately
 * does not depend on any other BlueJ classes.
 * 
 * @author Davin McCall
 */
public class DataSubmitter
{
    private static final String submitUrl = "http://localhost:3000/master_events";
    
    private static volatile boolean givenUp = false;
    
    private static boolean isRunning = false;
    
    private static List<Event> queue = new LinkedList<Event>();

    private static Map<FileKey, ArrayList<String> > fileVersions = new HashMap<FileKey, ArrayList<String> >();
    
    public static class FileKey
    {
        private File projDir;
        private String file;
        public FileKey(Project proj, String path)
        {
            this.projDir = proj.getProjectDir();
            this.file = path;
        }
        
        //Eclipse-generated hashCode and equals methods:
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((file == null) ? 0 : file.hashCode());
            result = prime * result
                    + ((projDir == null) ? 0 : projDir.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FileKey other = (FileKey) obj;
            if (file == null) {
                if (other.file != null)
                    return false;
            }
            else if (!file.equals(other.file))
                return false;
            if (projDir == null) {
                if (other.projDir != null)
                    return false;
            }
            else if (!projDir.equals(other.projDir))
                return false;
            return true;
        }
    }
    
    public static interface Event
    {
        MultipartEntity makeData(Map<FileKey, ArrayList<String> > fileVersions);
        
        void success(Map<FileKey, ArrayList<String> > fileVersions);
    }
    
    /**
     * Submit data to be posted to the server. The data is added to a queue which is processed by
     * another thread.
     * 
     * Package-visible, only used by DataCollector
     */
    static void submitEvent(Event evt)
    {
        //This thread only reads the boolean, and is an optimisation:
        if (givenUp)
            return;
        
        synchronized (queue) {
            queue.add(evt);
            
            if (! isRunning) {
                new Thread() {
                    public void run()
                    {
                        processQueue();
                    }
                }.start();
                isRunning = true;
            }
        }
    }
    
    /**
     * Process the queue of items to be posted to the server.
     */
    private static void processQueue()
    {
        while (true) {
            Event evt;
            synchronized (queue) {
                if (queue.isEmpty()) {
                    isRunning = false;
                    return;
                }
                evt = queue.remove(0);
            }
            

            if (!givenUp)
            {
                givenUp = !postData(evt);
            }
        }
    }
    
    /**
     * Actually post the data to the server.
     * 
     * Returns false if there was an error.
     */
    private static boolean postData(Event evt)
    {        
        HttpClient client = new DefaultHttpClient();
        
        try {
            HttpPost post = new HttpPost(submitUrl);

            
            /*
            if (errFilePath != null) {
                mpe.addPart("filepath", new StringBody(errFilePath, utf8));
            }
            if (errMsg != null) {
                mpe.addPart("errormsg", new StringBody(errMsg, utf8));
            }
            if (errline != 0) {
                mpe.addPart("errorline", new StringBody("" + errline, utf8));
            }
            
            int i = 0;
            for (SourceContent changedFile : changedFiles) {
                mpe.addPart("sourcefileName" + i, new StringBody(changedFile.getFilePath(), utf8));
                mpe.addPart("sourcefileContent" + i, new ByteArrayBody(changedFile.getFileContent(),
                        changedFile.getFilePath()));
            }
            */
            MultipartEntity mpe = evt.makeData(fileVersions);
            if (mpe == null)
                return true; // nothing to send, no error
            post.setEntity(mpe);
            HttpResponse response = client.execute(post);
            
            for (Header h : response.getAllHeaders())
            {
                if ("X-Status".equals(h.getName()) && !"Created".equals(h.getValue()))
                {
                    // Temporary printing:
                    System.err.println("Problem response: " + mpe.toString() + " " + response.toString());
                    return false;
                }
            }
            
            evt.success(fileVersions);
                
            EntityUtils.consume(response.getEntity());
        }
        catch (ClientProtocolException cpe) {
            return false;
        }
        catch (IOException ioe) {
            //For now:
            ioe.printStackTrace();
            
            return false;
        }
        
        return true;
    }
}
