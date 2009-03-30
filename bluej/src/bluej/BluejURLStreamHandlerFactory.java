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
package bluej;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import sun.net.www.protocol.file.Handler;

/**
 * Work around java's broken handling of UNC pathnames in java 1.4/1.5
 * (apparently will be fixed in Mustang). This is a URL stream handler
 * factory which creates a special handler for "file://" URLs.
 * 
 * See for details:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5086147
 * 
 * @author Davin McCall
 * @version $Id: BluejURLStreamHandlerFactory.java 6215 2009-03-30 13:28:25Z polle $
 */
public class BluejURLStreamHandlerFactory implements URLStreamHandlerFactory
{
    public URLStreamHandler createURLStreamHandler(String proto)
    {
        if (proto.equals("file"))
            return new BluejFileUrlHandler();
        else
            return null;
    }
}

class BluejFileUrlHandler extends Handler
{
    protected void parseURL(URL u, String spec, int start, int limit)
    {
        // first check existing context in the url
        String urlpath = u.getPath();
        if (urlpath != null)
            urlpath += spec.substring(start, limit);
        else
            urlpath = spec.substring(start, limit);
        
        // if a file URL path begins with a double-slash (ie. UNC notation)
        // then make sure that an empty "authority" section of the URL is
        // present.
        if (u.getAuthority() == null) {
            if (urlpath.startsWith("//")) {
                if (! urlpath.startsWith("///")) {
                    // give it an authority
                    setURL(u, "file", "", 0, "", null, u.getPath(), null, null);
                }
            }
        }
        
        super.parseURL(u, spec, start, limit);
    }
    
    protected String toExternalForm(URL u)
    {
        String s = super.toExternalForm(u);
        
        // If the resulting external form has a missing (empty) authority
        // component then re-insert the component.
        if (s.charAt(5) == '/' && s.charAt(6) == '/') {
            if (s.charAt(7) != '/') {
                s = "file://" + s.substring(5);
                // System.out.println("Corrected external form to: " + s);
            }
        }
        return s;
    }
}
