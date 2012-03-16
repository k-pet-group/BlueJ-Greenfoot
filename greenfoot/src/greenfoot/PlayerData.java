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
 * The PlayerData class allows you to store and load data for Greenfoot scenarios,
 * even when the scenario has been uploaded to the Greenfoot website.
 * 
 * <p>You should always check if storage is currently available by PlayerData.isStorageAvailable().
 * Storage is not always available, and in particular, if the user is not logged in to
 * the Greenfoot website (a very common case), storage will not be available.</p>
 * 
 * <p>When storage is available, you can use PlayerData.getMyData() to get the data for the
 * current user, or PlayerData.getTop(10) or PlayerData.getNearby(10) to get items to show
 * on a scoreboard.  These give you back PlayerData items, where you can get the players' image or
 * get/set the current data.</p>
 * 
 * <p>A player's data consists of a score integer, 10 general integers and 5 strings.  getTop and getNearby
 * use the score for sorting.  You are free to use the general integers and strings as you
 * need for your scenario: things like which level the player reached last time they
 * were playing.</p>
 * 
 * @author Neil Brown
 * @version 2.3
 */
public class PlayerData
{
    // These may enlarge in future:
    
    /** The number of integers that can be stored */
    public static final int NUM_INTS = 10;
    /** The number of Strings that can be stored */
    public static final int NUM_STRINGS = 5;
    private int[] ints;
    private String[] strings;
    private String userName;
    private int score;
    private int rank;
    
    //package-visible:
    PlayerData(String userName, int rank)
    {
        this.userName = userName;
        this.rank = rank;
        score = 0;
        ints = new int[NUM_INTS];
        strings = new String[NUM_STRINGS];
    }
    
    /**
     * Gets the username of the user that this storage belongs to.
     */
    public String getUserName()
    {
        return userName;
    }
    
    /**
     * Gets the value of the int at the given index (0 to NUM_INTS-1, inclusive)
     * <p>
     * Default value is zero.
     */
    public int getInt(int index) { return ints[index]; }
    
    /**
     * Gets the value of the String at the given index (0 to NUM_STRINGS-1, inclusive)
     * <p>
     * Default value is the empty String.
     */
    public String getString(int index) { return strings[index] == null ? "" : strings[index]; }
    
    /**
     * Sets the value of the int at the given index (0 to NUM_INTS-1, inclusive)
     */
    public void setInt(int index, int value) { ints[index] = value; }

    /**
     * Gets the value of the String at the given index (0 to NUM_STRINGS-1, inclusive)
     * Passing null is treated as a blank string.
     */
    public void setString(int index, String value) { strings[index] = value; /* TODO throw an exception if it's over max length */ }
    
    /**
     * Gets the player's score
     */
    public int getScore() { return score; }
    
    /**
     * Sets the player's score.
     * <p>
     * Note that this really does set the player's score.  If you want to record only the player's highest
     * score, you must code that yourself, using something like:
     * <pre>
     *   if (latestScore > getScore())
     *   {
     *     setScore(latestScore);
     *   }
     * </pre>
     * Without some code like this, you'll always overwrite the player's previous score.
     */
    public void setScore(int score) { this.score = score; }
    
    /**
     * Gets the players overall rank for this scenario.
     * <p>
     * The player with the highest score will return 1, the player with the second highest score
     * will return 2, and so on.  Players with equal scores will get equal ranks,
     * so rank will not necessarily be unique.  To find the rank, scores are sorted
     * in descending order (highest score first).  If your scores need to be lowest-first,
     * one trick is to store them as negative numbers.
     * <p>
     * If the rank is unavailable (e.g. because the data hasn't been stored yet), this function will return -1
     */
    public int getRank() { return rank; }
    
    /**
     * A boolean indicating whether storage is available.
     * <p>
     * Storage is unavailable if it is an applet outside the Greenfoot website, or a stand-alone application,
     * or if the user is not logged in to the Greenfoot website.  This last case is very common,
     * so you must check this function before attempting to use the other static storage functions.
     * If this function returns false, your scenario must proceed without using storage.
     */
    public static boolean isStorageAvailable()
    {
        // Returns false for applets when not on the Gallery, and stand-alone applications
        // Returns true for inside Greenfoot, and applets on the gallery
        return GreenfootUtil.isStorageSupported();
    }
    
    /**
     * Gets the data stored for the current user.
     * 
     * Returns null if:
     * <ul>
     * <li>there is a problem reading the local file (for local scenarios), or</li>
     * <li>there is a problem connecting to the server (for scenarios on the greenfoot.org site), or</li>
     * <li>the user is not logged in (for scenarios on the greenfoot.org site).</li>
     * </ul>
     * 
     * The last case is very common, so you should always be ready to handle a null return from this function.
     * 
     * @return the user's data, or null if there was a problem.
     */
    public static PlayerData getMyData()
    {
        return GreenfootUtil.getCurrentUserData();
    }
    
    /**
     * Stores the data.
     * <p>
     * If you try to store data for any user other than the current user, it is guaranteed to fail.
     * 
     * @return true if stored successfully, false if there was a problem.
     */
    public boolean store()
    {
        boolean success = GreenfootUtil.storeCurrentUserData(this);
        
        if (success)
        {
            //Update the rank (not very efficient, but simple):
            rank = getMyData().rank;
        }
        
        return success;
    }
    
    /**
     * Gets a sorted list of the PlayerData items for this scenario, starting at the top.
     * 
     * <p>This will return one PlayerData item per user, and it will be sorted in descending order by the score
     * (i.e. the return of getScore()).  The parameter allows you to specify a limit
     * on the amount of users' data to retrieve.  If there is lots of data stored
     * for users in your app, this may take some time (and bandwidth) to retrieve all users' data,
     * and often you do not need all the users' data.</p>
     * 
     * <p>For example, if you want to show the high-scores, store the score with setScore(score),
     * and then use getTop(10) to get the users with the top ten scores.</p> 
     * 
     * <p>Returns null if:
     * <ul>
     * <li>there is a problem reading the local file (for local scenarios), or</li>
     * <li>there is a problem connecting to the server (for scenarios on the greenfoot.org site), or</li>
     * <li>the user is not logged in (for scenarios on the greenfoot.org site).</li>
     * </ul>
     * The last case is very common, so you should always be ready to handle a null return from this function.</p>
     * 
     * @param maxAmount The maximum number of data items to retrieve.
     * Passing zero or a negative number will get all the data, but see the note above.  
     * @return A list where each item is a PlayerData, or null if there was a problem
     */
    public static List getTop(int maxAmount)
    {
        // Will return an empty list if there is no previously stored data
        // Each item is a PlayerData
        return GreenfootUtil.getTopUserData(maxAmount);
    }
    
    /**
     * Gets a sorted list of the PlayerData items for this scenario surrounding the current user.
     * 
     * <p>This will be one item per user, and it will be sorted in descending order by the score
     * (i.e. the return of getScore()).  The parameter allows you to specify a limit
     * on the amount of users' data to retrieve.  If there is lots of data stored
     * for users in your app, this may take some time (and bandwidth) to retrieve all users' data,
     * and often you do not need all the users' data.</p>
     * 
     * <p>The items will be those surrounding the current user.  So for example, imagine that the player is 50th
     * of 100 total users (when sorted by getScore()).  Calling getNearby(5) will get the
     * 48th, 49th, 50th, 51st and 52nd users in that order.  Do not rely on the player being at a fixed
     * location in the middle of the list: calling getNearby(5) when the user is 2nd overall will get the
     * 1st, 2nd, 3rd, 4th and 5th users, so the user will be 2nd in the list, and a similar thing will happen
     * if the user is near the end of the list.</p>
     * 
     * <p>For example, if you want to show the high-scores surrounding the player, store the score with setScore(score),
     * and then use getNearby(10) to get the ten users with scores close to the current player.</p>
     * 
     * <p>Returns null if:
     * <ul>
     * <li>there is a problem reading the local file (for local scenarios), or</li>
     * <li>there is a problem connecting to the server (for scenarios on the greenfoot.org site), or</li>
     * <li>the user is not logged in (for scenarios on the greenfoot.org site).</li>
     * </ul>
     * The last case is very common, so you should always be ready to handle a null return from this function.</p>

     * 
     * @param maxAmount The maximum number of data items to retrieve.
     *            Passing zero or a negative number will get all the data, but see the note above.  
     * @return A list where each item is a PlayerData, or null if there was a problem
     */
    public static List getNearby(int maxAmount)
    {
        return GreenfootUtil.getNearbyUserData(maxAmount);
    }
    
    /**
     * Returns a 50x50 image of the user.
     * <p>
     * On the Greenfoot website, this is their profile picture. 
     * If running locally, always returns a dummy image, with their username drawn on the image.
     * 
     * <p>Returns null if:
     * <ul>
     * <li>there is a problem reading the local file (for local scenarios), or</li>
     * <li>there is a problem connecting to the server (for scenarios on the greenfoot.org site), or</li>
     * <li>the user is not logged in (for scenarios on the greenfoot.org site).</li>
     * </ul>
     * The last case is very common, so you should always be ready to handle a null return from this function.</p>
     * 
     * @return a 50x50 GreenfootImage, or null if there was a problem accessing the image
     */
    public GreenfootImage getUserImage()
    {
        return GreenfootUtil.getUserImage(userName);
    }
}
