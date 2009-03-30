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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * This is a small program to get the size of the jar file which is extracted during the
 * BlueJ installation (unix installer), and write the size to a property in a properties
 * file.
 * 
 * @author Davin McCall
 */
public class InstallerProps
{
    public static void main(String [] args)
    {
        if (args.length != 1) {
            System.err.println("Must include properties template file on command line.");
            System.exit(1);
        }
        
        Properties props = new Properties();
        String propsTemplate = args[0];
        
        try {
            FileInputStream is = new FileInputStream(new File(propsTemplate));
            props.load(is);
            
            File installTmp = new File("install_tmp");
            File distJar = new File(props.getProperty("install.pkgJar"));
            long length = distJar.length();
            
            props.put("install.pkgJarSize", Long.toString(length));
            
            File newProps = new File(installTmp, "installer.props");
            FileOutputStream os = new FileOutputStream(newProps);
            props.store(os, "Installer properties");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
