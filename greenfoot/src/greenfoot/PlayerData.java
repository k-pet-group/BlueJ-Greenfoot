package greenfoot;

import greenfoot.util.GreenfootStorageException;
import greenfoot.util.GreenfootUtil;

import java.util.List;

public class PlayerData
{
    // These may enlarge in future:
    
    /**
     * The number of integers that can be stored
     */
    public static final int NUM_INTS = 10;
    /**
     * The number of Strings that can be stored
     */
    public static final int NUM_STRINGS = 5;
    private int[] ints;
    private String[] strings;
    private String userName;
    
    //package-visible:
    PlayerData(String userName)
    {
        this.userName = userName; 
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
     * 
     * Default value is zero.
     */
    public int getInt(int index) { return ints[index]; }
    /**
     * Gets the value of the String at the given index (0 to NUM_STRINGS-1, inclusive)
     * 
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
     * A boolean indicating whether storage is available.
     * 
     * Storage is unavailable if it is an applet outside the Greenfoot website, or a stand-alone application,
     * or if the user is not logged in to the Greenfoot website.  This last case is very common,
     * so you must check this function before attempting to use the other static storage functions.
     * If this function returns false, your scenario must proceed without using storage.
     */
    
    // Returns false for applets when not on the Gallery, and stand-alone applications
    // Returns true for inside Greenfoot, and applets on the gallery
    public static boolean isStorageAvailable()
    {
        return GreenfootUtil.isStorageSupported();
    }
    
    /**
     * Gets the data stored for the current user.
     * 
     * @return the user's data, or null if there was a problem.
     */
    public static PlayerData getMyData()
    {
        return GreenfootUtil.getCurrentUserData();
    }
    
    /**
     * Stores the data.
     * 
     * If you try to store data for any user other than the current user, it is guaranteed to fail.
     * 
     * @return true if stored successfully, false if there was a problem.
     */
    public boolean store()
    {
        return GreenfootUtil.storeCurrentUserData(this);
    }
    
    /**
     * Gets a list of all the GreenfootStorage items for this scenario.
     * 
     * This will be one item per user, and it will be sorted in descending order by the first integer value
     * (i.e. the return of getInt(0)).  The parameter allows you to specify a limit
     * on the amount of users' data to retrieve.  If there is lots of data stored
     * for users in your app, this may take some time (and bandwidth) to retrieve all users' data,
     * and often you do not need all the users' data.
     * 
     * For example, if you want to show the high-scores, store the score with setInt(0, score),
     * and then use getAllUserData(10) to get the users with the top ten scores. 
     * 
     * @param limit The maximum number of data items to retrieve.
     * Passing zero or a negative number will get all the data, but see the note above.  
     * @return A list where each item is a GreenfootStorage, or null if there was a problem
     * @throws GreenfootStorageException
     */
    // Will return an empty list if there is no previously stored data
    // Each item is a GreenfootStorage
    public static List getTop(int maxAmount)
    {
        return GreenfootUtil.getTopUserData(maxAmount);
    }
    
    public static List getNearby(int maxAmount)
    {
        return GreenfootUtil.getNearbyUserData(maxAmount);
    }
    
    /**
     * Returns a 50x50 image of the user.
     * 
     * On the Greenfoot website, this is their profile picture. 
     * If running locally, always returns a dummy image, with their username drawn on the image.
     * 
     * @return a 50x50 GreenfootImage, or null if there was a problem accessing the image
     */
    public GreenfootImage getUserImage()
    {
        return GreenfootUtil.getUserImage(userName);
    }
}
