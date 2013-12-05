/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms.ide;

import greenfoot.GreenfootImage;
import greenfoot.UserInfo;
import greenfoot.UserInfoVisitor;
import greenfoot.platforms.GreenfootUtilDelegate;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import bluej.Config;
import bluej.runtime.ExecServer;
import bluej.utility.BlueJFileReader;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * GreenfootUtilDelegate implementation for the Greenfoot IDE.
 */
public class GreenfootUtilDelegateIDE implements GreenfootUtilDelegate
{
    private static GreenfootUtilDelegateIDE instance;
    
    static {
        instance = new GreenfootUtilDelegateIDE();
    }
    
    /**
     * Get the GreenfootUtilDelegateIDE instance.
     */
    public static GreenfootUtilDelegateIDE getInstance()
    {
        return instance;
    }
    
    private GreenfootUtilDelegateIDE()
    {
        // Nothing to do.
    }
    
    /**
     * Creates the skeleton for a new class
     */
    public void createSkeleton(String className, String superClassName, File file,
            String templateFileName, String projCharsetName)
        throws IOException
    {
        Charset projCharset;
        try
        {
            projCharset = Charset.forName(projCharsetName);
        }
        catch (UnsupportedCharsetException uce) {
            projCharset = Charset.forName("UTF-8");
        }
        catch (IllegalCharsetNameException icne) {
            projCharset = Charset.forName("UTF-8");
        }
        
        Dictionary<String, String> translations = new Hashtable<String, String>();
        translations.put("CLASSNAME", className);
        if(superClassName != null) {
            translations.put("EXTENDSANDSUPERCLASSNAME", "extends " + superClassName);
        } else {
            translations.put("EXTENDSANDSUPERCLASSNAME", "");
        }
        String baseName = "greenfoot/templates/" +  templateFileName;
        File template = Config.getLanguageFile(baseName);
        
        if(!template.canRead()) {
            template = Config.getDefaultLanguageFile(baseName);
        }
        BlueJFileReader.translateFile(template, file, translations, Charset.forName("UTF-8"), projCharset);
    }
    
    @Override
    public URL getResource(String path) 
    {
        return ExecServer.getCurrentClassLoader().getResource(path);
    }
    
    @Override
    public Iterable<String> getSoundFiles()
    {
        ArrayList<String> files = new ArrayList<String>();
        try
        {
            URL url = getResource("sounds");
            if (url != null && "file".equals(url.getProtocol()))
            {
                for (String file : new File(url.toURI()).list())
                {
                    files.add(file);
                }
            }
        }
        catch (URISyntaxException e)
        {
            Debug.reportError("Bad URI in getResources", e);
        }
        // May be a blank list if something went wrong:
        return files;
    }
    
    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    @Override
    public  String getGreenfootLogoPath()
    {        
        File libDir = Config.getGreenfootLibDir();
        return libDir.getAbsolutePath() + "/imagelib/other/greenfoot.png";        
    }
    
    @Override
    public void displayMessage(Component parent, String messageText)
    {
        DialogManager.showText(parent, messageText);
    }

    @Override
    public boolean isStorageSupported()
    {
        return getUserName() != null && !getUserName().isEmpty();
    }
    
    public String getUserName()
    {
        return Config.getPropString("greenfoot.player.name", "Player1");
    }

    @Override
    public UserInfo getCurrentUserInfo()
    {
        if (getUserName() == null || getUserName().isEmpty())
            return null;
        
        ArrayList<UserInfo> all = getAllDataSorted(true);
        
        if (all == null)
            return null; // Error reading file
        
        for (int i = 0; i < all.size();i++)
        {
            if (getUserName().equals(all.get(i).getUserName()))
            {
                return all.get(i);
            }
        }
        
        // Couldn't find them anywhere, return blank:
        return UserInfoVisitor.allocate(getUserName(), -1, getUserName());
    }

    private UserInfo makeStorage(String[] line, int rank, boolean useSingleton)
    {
        UserInfo r = null;
        try
        {
            int column = 0; 
            r = UserInfoVisitor.allocate(line[column++], rank, useSingleton ? getUserName() : null);
            r.setScore(Integer.parseInt(line[column++]));
            for (int i = 0; i < UserInfo.NUM_INTS; i++)
            {
                r.setInt(i, Integer.parseInt(line[column++]));
            }
            
            for (int i = 0; i < UserInfo.NUM_STRINGS; i++)
            {
                r.setString(i, line[column++]);
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            // If we run out of the line, just stop setting the values in storage
        }
        
        return r;
    }
    

    private String[] makeLine(String userName, UserInfo data)
    {
        String[] line = new String[1 + 1 + UserInfo.NUM_INTS + UserInfo.NUM_STRINGS];
        int column = 0;
        line[column++] = userName;
        line[column++] = Integer.toString(data.getScore());
        try
        {
            for (int i = 0; i < UserInfo.NUM_INTS; i++)
            {
                line[column++] = Integer.toString(data.getInt(i));
            }
            
            for (int i = 0; i < UserInfo.NUM_STRINGS; i++)
            {
                line[column++] = data.getString(i);
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            // Can't happen
        }        
        return line;
    }

    @Override
    public boolean storeCurrentUserInfo(UserInfo data)
    {
        if (getUserName() == null || getUserName().isEmpty())
            return false;
        
        List<String[]> all;
        try
        {
            CSVReader csv = new CSVReader(new InputStreamReader(new FileInputStream("storage.csv"), "UTF-8"));
            all = csv.readAll();
            csv.close();
        }
        catch (FileNotFoundException e)
        {
            // No previous storage, make a new blank one:
            all = new ArrayList<String[]>();
        }
        catch (IOException e)
        {
            Debug.message("Error reading user data: " + e.getMessage());
            return false;
        }
        
        // First, remove any existing line:
        Iterator<String[]> lineIt = all.iterator();
        while (lineIt.hasNext())
        {
            String[] line = lineIt.next();
            if (line.length > 1 && getUserName().equals(line[0]))
            {
                lineIt.remove();
                break;
            }
        }
        
        // Then add a line on to the end:
        if (data != null)
        {
            // No line for that user, add a new one:
            all.add(makeLine(getUserName(), data));
        }
        
        try
        {
            CSVWriter csvOut = new CSVWriter(new OutputStreamWriter(new FileOutputStream("storage.csv"), "UTF-8"));
            csvOut.writeAll(all);
            csvOut.close();
            
            return true;
        }
        catch (IOException e)
        {
            Debug.message("Error storing user data: " + e.getMessage());
            return false;
        }
    }
    
    private ArrayList<UserInfo> getAllDataSorted(boolean useSingleton)
    {
        try
        {
            ArrayList<UserInfo> ret = new ArrayList<UserInfo>();
            
            CSVReader csv = new CSVReader(new InputStreamReader(new FileInputStream("storage.csv"), "UTF-8"));
            
            List<String[]> all = csv.readAll();
            
            Collections.sort(all, new Comparator<String[]>() {
                @Override
                public int compare(String[] o1, String[] o2)
                {
                    // Sort in reverse order:
                    return -(Integer.parseInt(o1[1]) - Integer.parseInt(o2[1]));
                }
            });
            
            int rank = 1;
            for (String[] line : all)
            {
                ret.add(makeStorage(line, rank, useSingleton));
                rank++;
            }
            
            csv.close();
            return ret;
        }
        catch (FileNotFoundException e)
        {
            // No previous storage, return the blank list:
            return new ArrayList<UserInfo>();
        }
        catch (IOException e)
        {
            Debug.message("Error reading user data: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<UserInfo> getTopUserInfo(int limit)
    {
        ArrayList<UserInfo> ret = getAllDataSorted(false);
        if (ret == null)
            return null;
        else if (ret.size() <= limit || limit <= 0)
            return ret;
        else
            return ret.subList(0, limit);
    }

    @Override
    public GreenfootImage getUserImage(String userName)
    {
        // GreenfootUtil will take care of making a dummy image:
        return null;
    }

    @Override
    public List<UserInfo> getNearbyUserInfo(int maxAmount)
    {
        if (getUserName() == null || getUserName().isEmpty())
            return null;
        
        ArrayList<UserInfo> all = getAllDataSorted(false);
        
        if (all == null)
            return null;
        
        int index = -1;
        
        for (int i = 0; i < all.size();i++)
        {
            if (getUserName() != null && getUserName().equals(all.get(i).getUserName()))
            {
                index = i;
                break;
            }
        }
        
        if (index == -1 || maxAmount == 0)
            return new ArrayList<UserInfo>();
        
        int availableBefore = index;
        int availableAfter = all.size() - 1 - index;
        
        int desiredBefore = maxAmount / 2;
        int desiredAfter = Math.max(0, maxAmount - 1) / 2;
        
        // maxAmount | desiredBefore | desiredAfter | before+after+1
        // 1 | 0 | 0 | 1
        // 2 | 1 | 0 | 2
        // 3 | 1 | 1 | 3
        // 4 | 2 | 1 | 4
        // 5 | 2 | 2 | 5
        // 6 | 3 | 2 | 6
        // and so on...
        
        if (availableAfter + availableBefore + 1 <= maxAmount)
        {
            //Less overall that we want, use everything:
            return all;
        }
        else if (availableBefore <= desiredBefore)
        {
            // Not enough available before-hand, but must be enough in total:
            return all.subList(index - availableBefore, index - availableBefore + maxAmount + 1);
        }
        else if (availableAfter <= desiredAfter)
        {
            // Not enough available after, but must be enough in total:
            return all.subList(index + availableAfter - maxAmount, index + availableAfter + 1);
        }
        else
        {
            // Must have enough available before and after:
            return all.subList(index - desiredBefore, index + desiredAfter + 1);
        }
    }
}
