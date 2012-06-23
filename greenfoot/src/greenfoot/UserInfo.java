/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011, 2012  Poul Henriksen and Michael Kolling 
 
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
package greenfoot;

import greenfoot.util.GreenfootUtil;

import java.util.List;

/**
 * <p>The UserInfo class can be used to store data permanently on a server, and
 * to share this data between different users, when the scenario runs on the
 * Greenfoot web site. This can be used to implement shared high score tables
 * or other examples of shared data.</p>
 *
 * <p>Storage is only available when the current user is logged in on the Greenfoot
 * site, so for some users storage will not be available. Always use
 * UserInfo.isStorageAvailable() to check before accessing the user data.</p>
 *
 * <p>A typical code snippet for storing a high score is as follows:</p>
 *
 * <pre>
 *     if (UserInfo.isStorageAvailable()) {
 *         UserInfo myInfo = UserInfo.getMyInfo();
 *         if (newScore > myInfo.getScore()) {
 *             myInfo.setScore(newScore);
 *             myInfo.store();  // write back to server
 *         }
 *     }
 * </pre>
 * <p>Methods to retrieve user data include getting data for the current user
 * (getMyInfo()), the top scorers (e.g. getTop(10) for the top 10), and data
 * for users with scores near my own score (e.g. getNearby(10)).</p>
 *
 * <p>The data that can be stored for each user consists of a score, 10
 * additional general purpose integers, and 5 strings (limited to 50 characters
 * in length). In addition, the user name and user's image can be retrieved from
 * the user data.</p>
 *
 * <p>For testing purposes, while running within Greenfoot (not on the web site),
 * the user name can be set in the preferences (CTRL-SHIFT-P / CMD-SHIFT-P).
 * This allows to simulate different users during development. When running
 * on the web site, the user name is the name used to log in to the site.</p>
 * 
 * @author Neil Brown
 * @version 2.4
 */
public class UserInfo
{
    // These may enlarge in future:
    
    /** The number of integers that can be stored */
    public static final int NUM_INTS = 10;
    /** The number of Strings that can be stored */
    public static final int NUM_STRINGS = 5;
    /** The maximum number of characters that can be stored in each String */    
    public static final int STRING_LENGTH_LIMIT = 50;
    // NB the above limit matches the database schema in the gallery storage
    // so don't alter it!
    private int[] ints;
    private String[] strings;
    private String userName;
    private int score;
    private int rank;
    
    //package-visible:
    UserInfo(String userName, int rank)
    {
        this.userName = userName;
        this.rank = rank;
        score = 0;
        ints = new int[NUM_INTS];
        strings = new String[NUM_STRINGS];
    }
    
    //package-visible:
    void setRank(int n)
    {
        rank = n;
    }
    
    /**
     * Get the username of the user that this storage belongs to.
     */
    public String getUserName()
    {
        return userName;
    }
    
    /**
     * Get the value of the int at the given index (0 to NUM_INTS-1, inclusive).
     * <p>
     * The default value is zero.
     */
    public int getInt(int index)
    {
        return ints[index];
    }
    
    /**
     * Get the value of the String at the given index (0 to NUM_STRINGS-1, inclusive).
     * <p>
     * The default value is the empty String.
     */
    public String getString(int index)
    {
        return strings[index] == null ? "" : strings[index];
    }
    
    /**
     * Set the value of the int at the given index (0 to NUM_INTS-1, inclusive).
     * 
     * <p>Note that to store this value permanently, you must later call store().
     */
    public void setInt(int index, int value)
    {
        ints[index] = value;
    }

    /**
     * Get the value of the String at the given index (0 to NUM_STRINGS-1, inclusive).
     * Passing null is treated as a blank string.  The given String must be of STRING_LENGTH_LIMIT
     * characters or less (or else the method will fail).
     * 
     * <p>Note that to store this value permanently, you must later call store().
     */
    public void setString(int index, String value)
    {
        if (value != null && value.length() > STRING_LENGTH_LIMIT)
        {
            System.err.println("Error: tried to store a String of length " + value.length() + " in UserInfo, which is longer than UserInfo.STRING_LENGTH_LIMIT (" + STRING_LENGTH_LIMIT + ")");
        }
        else
        {
            strings[index] = value;
        }
    }
    
    /**
     * Get the user's score.  By default, this is zero.
     */
    public int getScore()
    {
        return score;
    }
    
    /**
     * Set the user's score.
     * <p>
     * Note that this really does set the user's score.  If you want to record only the user's highest
     * score, you must code that yourself, using something like:
     * <pre>
     *   if (latestScore > userData.getScore())
     *   {
     *     userData.setScore(latestScore);
     *   }
     * </pre>
     * Without some code like this, you'll always overwrite the user's previous score.
     * 
     * <p>Note that to store this value permanently, you must later call store().
     */
    public void setScore(int score)
    {
        this.score = score;
    }
    
    /**
     * Get the users overall rank for this scenario.
     * <p>
     * The user with the highest score will return 1, the user with the second highest score
     * will return 2, and so on.  Players with equal scores will get equal ranks,
     * so rank will not necessarily be unique.  To find the rank, scores are sorted
     * in descending order (highest score first).  If your scores need to be lowest-first,
     * one trick is to store them as negative numbers.
     * <p>
     * If the rank is unavailable (e.g. because the data hasn't been stored yet), this function will return -1.
     */
    public int getRank()
    {
        return rank;
    }
    
    /**
     * Indicate whether storage is available.
     * <p>
     * Storage is unavailable if the scenario is run as an applet outside the Greenfoot website,
     * or as a stand-alone application,
     * or if the user is not logged in to the Greenfoot website.  This last case is very common,
     * so you should check this function before attempting to use the other static storage functions.
     * If this function returns false, your scenario should proceed without using storage.
     */
    public static boolean isStorageAvailable()
    {
        // Returns false for applets when not on the Gallery, and stand-alone applications
        // Returns true for inside Greenfoot, and applets on the gallery
        return GreenfootUtil.isStorageSupported();
    }
    
    /**
     * Get the data stored for the current user.
     * 
     * This method returns null if:
     * <ul>
     * <li>there is a problem reading the local storage.csv file (for local scenarios), or</li>
     * <li>the scenario is running as a stand-alone application, or applet on your own website, or</li>
     * <li>there is a problem connecting to the server (for scenarios on the greenfoot.org site), or</li>
     * <li>the user is not logged in (for scenarios on the greenfoot.org site).</li>
     * </ul>
     * 
     * The last case is very common, so you should always be ready to handle a null return from this function.
     * 
     * @return the user's data, or null if there was a problem.
     */
    public static UserInfo getMyInfo()
    {
        return GreenfootUtil.getCurrentUserInfo();
    }
    
    /**
     * Store the data to the server.
     * <p>
     * You can only store data for the current user (that is, data retrieved using getMyData).
     * If you try to store data for any user other than the current user, it is guaranteed to fail.
     * 
     * @return true if stored successfully, false if there was a problem.
     */
    public boolean store()
    {
        boolean success = GreenfootUtil.storeCurrentUserInfo(this);
        
        if (success)
        {
            //Update the rank (not very efficient, but simple):
            rank = getMyInfo().rank;
        }
        
        return success;
    }
    
    /**
     * Get a sorted list of the UserInfo items for this scenario, starting at the top.
     * 
     * <p>This will return one UserInfo item per user, and it will be sorted in descending order by the score
     * (i.e. the return of getScore()).  The parameter allows you to specify a limit
     * on the amount of users' data to retrieve.  If there is lots of data stored
     * for users in your app, it may take some time (and bandwidth) to retrieve all users' data,
     * and often you do not need all the users' data.</p>
     * 
     * <p>For example, if you want to show the high-scores, store the score with setScore(score) and store(),
     * and then use getTop(10) to get the users with the top ten scores.</p> 
     * 
     * <p>Returns null if:
     * <ul>
     * <li>there is a problem reading the local file (for local scenarios), or</li>
     * <li>the scenario is running as a stand-alone application, or applet on your own website, or</li>
     * <li>there is a problem connecting to the server (for scenarios on the greenfoot.org site).</li>
     * </ul>
     * You should always be ready to handle a null return from this function.</p>
     * 
     * @param maxAmount The maximum number of data items to retrieve.
     * Passing zero or a negative number will get all the data, but see the note above.  
     * @return A list where each item is a UserInfo, or null if there was a problem
     */
    public static List getTop(int maxAmount)
    {
        // Will return an empty list if there is no previously stored data
        // Each item is a UserInfo
        return GreenfootUtil.getTopUserInfo(maxAmount);
    }
    
    /**
     * Get a sorted list of the UserInfo items for this scenario surrounding the current user.
     * 
     * <p>This will be one item per user, and it will be sorted in descending order by the score
     * (i.e. the return of getScore()).  The parameter allows you to specify a limit
     * on the amount of users' data to retrieve.  If there is lots of data stored
     * for users in your app, this may take some time (and bandwidth) to retrieve all users' data,
     * and often you do not need all the users' data.</p>
     * 
     * <p>The items will be those surrounding the current user.  So for example, imagine that the user is 50th
     * of 100 total users (when sorted by getScore()).  Calling getNearby(5) will get the
     * 48th, 49th, 50th, 51st and 52nd users in that order.  Do not rely on the user being at a fixed
     * location in the middle of the list: calling getNearby(5) when the user is 2nd overall will get the
     * 1st, 2nd, 3rd, 4th and 5th users, so the user will be 2nd in the list, and a similar thing will happen
     * if the user is near the end of the list.</p>
     * 
     * <p>For example, if you want to show the high-scores surrounding the user, store the score with setScore(score) and store(),
     * and then use getNearby(10) to get the ten users with scores close to the current user.</p>
     * 
     * <p>Returns null if:
     * <ul>
     * <li>there is a problem reading the local file (for local scenarios), or</li>
     * <li>the scenario is running as a stand-alone application, or applet on your own website, or</li>
     * <li>there is a problem connecting to the server (for scenarios on the greenfoot.org site), or</li>
     * <li>the user is not logged in (for scenarios on the greenfoot.org site).</li>
     * </ul>
     * The last case is very common, so you should always be ready to handle a null return from this function.</p>

     * 
     * @param maxAmount The maximum number of data items to retrieve.
     *            Passing zero or a negative number will get all the data, but see the note above.  
     * @return A list where each item is a UserInfo, or null if there was a problem
     */
    public static List getNearby(int maxAmount)
    {
        return GreenfootUtil.getNearbyUserData(maxAmount);
    }
    
    /**
     * Return an image of the user. The image size is 50x50 pixels.
     * <p>
     * On the Greenfoot website, this is their profile picture. 
     * If running locally (or a profile picture is unavailable), this method returns a dummy image with the username drawn on the image.
     *
     * @return A 50x50 pixel GreenfootImage
     */
    public GreenfootImage getUserImage()
    {
        return GreenfootUtil.getUserImage(userName);
    }
}
