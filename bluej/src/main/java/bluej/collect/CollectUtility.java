/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2014  Michael Kolling and John Rosenberg 
 
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.entity.mime.content.StringBody;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;

/**
 * A class with various utilities used by DataCollector
 * @author nccb
 *
 */
class CollectUtility
{
    /**
     * Cached UTF-8 charset:
     */
    private static final Charset utf8 = Charset.forName("UTF-8");
    
    /**
     * A class that stores details of the Project that are useful for constructing messages to
     * the Blackbox server.  This is needed so that we can safely pull the details from the Project
     * on the Swing thread, then use them later from the submitter thread
     *
     */
    // package-visible
    static class ProjectDetails
    {
        final Charset charset;
        final File projectDir;
        // package-visible
        @OnThread(Tag.FXPlatform)
        ProjectDetails(Project project)
        {
            this.projectDir = project.getProjectDir();
            this.charset = project.getProjectCharset();
        }
        
        
    }
    
    /**
     * Reads a source code file from the project, and anonymises it
     */
    @OnThread(Tag.FXPlatform)
    static String readFileAndAnonymise(ProjectDetails proj, File f)
    {
        try {
            StringBuilder sb = new StringBuilder();
            FileInputStream inputStream = new FileInputStream(f);
            InputStreamReader reader = new InputStreamReader(inputStream, proj.charset);
            char[] buf = new char[4096];
            
            int read = reader.read(buf);
            while (read != -1)
            {
                sb.append(buf, 0, read);
                read = reader.read(buf);
            }
            
            reader.close();
            inputStream.close();
            return CodeAnonymiser.anonymise(sb.toString());
        }
        catch (IOException ioe) {return null;}
    }

    /**
     * Gets the path of the given file, relative to the given project's base directory
     */
    static String toPath(ProjectDetails proj, File f)
    {
        return proj.projectDir.toURI().relativize(f.toURI()).getPath();
    }

    /**
     * Chains together toBody with toPath
     */
    static StringBody toBodyLocal(ProjectDetails project, File sourceFile)
    {
        return toBody(toPath(project, sourceFile));
    }

    /**
     * Performs an md5 hash of the given string
     */
    static String md5Hash(String src)
    {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(src.getBytes("UTF-8"));
        }
        catch (NoSuchAlgorithmException e) {
            //Oracle comes with MD5, unlikely that any other JDK wouldn't:
            Debug.reportError(e);
            return "";
        }
        catch (UnsupportedEncodingException e) {
            //Shouldn't happen -- no UTF-8?!
            Debug.reportError(e);
            return "";
        }
        StringBuilder s = new StringBuilder();
        for (byte b : hash)
        {
            s.append(String.format("%02X", b));
        }
        return s.toString();
    }

    /**
     * Converts the given String to a StringBody.  Null strings are sent the same as empty strings.
     */
    static StringBody toBody(String s)
    {
        try {
            return new StringBody(s == null ? "" : s, utf8);
        }
        catch (UnsupportedEncodingException e) {
            // Shouldn't happen, because UTF-8 is required to be supported
            return null;
        }
    }

    /**
     * Converts the integer to a StringBody
     */
    static StringBody toBody(int i)
    {
        return toBody(Integer.toString(i));
    }

    /**
     * Converts the long to a StringBody
     */
    static StringBody toBody(long l)
    {
        return toBody(Long.toString(l));
    }

    /**
     * Converts the boolean to a StringBody
     */
    static StringBody toBody(boolean b)
    {
        return toBody(Boolean.toString(b));
    }

}
