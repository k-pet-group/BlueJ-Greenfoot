package greenfoot;

public class GreenfootStorageVisitor
{
    public static PlayerData allocate(String userName)
    {
        return new PlayerData(userName);
    }
    
    public static GreenfootImage readImage(byte[] imageFileContents)
    {
        return new GreenfootImage(imageFileContents);
    }
}
