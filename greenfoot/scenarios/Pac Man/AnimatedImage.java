import greenfoot.*;

/**
 * AnimatedImage Class
 * @author Joseph Lenton
 * @version 12/01/07
 * 
 * This class is for creating a collection of GreenfootImages to appear
 * as frames. The frames are created from using a single greenfoot image.
 * 
 * The class will create an image for each frame,
 * and draw the image for that frame from the image supplied.
 * It will then be stored as a Greenfoot Image in a collection.
 */
public class AnimatedImage
{
    // the frames
    private GreenfootImage[] frames;
    // the number of frames in the image
    private int noFrames;
    // the width of each frame
    private int frameWidth;
    // the height of each frame
    private int frameHeight;
    
    /**
     * The constructor, which creates all the indevidual frames
     * from the image supplied.
     * The image should contain enough space to house each frame
     * exactly. Otherwise frames may not be drawn
     * correctly.
     * 
     * @param fileName the name of the image
     * @param noFrames the number of frames in the image
     * @param frameWidth the width of each frame
     * @param frameHeight the height of each frame
     */
    public AnimatedImage(String fileName, int noFrames, int frameWidth, int frameHeight)
    {
        GreenfootImage image = new GreenfootImage( fileName );
        
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.noFrames = noFrames;
        
        frames = new GreenfootImage[noFrames];
        int frame = 0;
        for (int y = 0; y < image.getHeight()-1; y += frameHeight) {
            for (int x = 0; x < image.getWidth()-1; x += frameWidth) {
                frames[frame] = new GreenfootImage( frameWidth, frameHeight );
                frames[frame].drawImage( image, -x, -y );
                frame++;
            }
        }
    }
    
    /**
     * Returns a frame based on the index value used.
     * @param frame the index number of the frame you require
     * @return the Greenfoot Image for that frame
     */
    public GreenfootImage getFrame(int frame)
    {
        if (frame < 0 || frame >= noFrames) {
            return null;
        }
        else {
            return frames[frame];
        }
    }
    
    /**
     * Returns the number of frames in this animated image type
     * @return the number of frames
     */
    public int getNumberOfFrames()
    {
        return noFrames;
    }
    
    /**
     * Returns the width of each frame. This width
     * is the same for all frames.
     * @return the width of every frame.
     */
    public int getFrameWidth()
    {
        return frameWidth;
    }
    
    /**
     * Returns the height of each frame. This height
     * is the same for all frames.
     * @return the height of every frame.
     */
    public int getFrameHeight()
    {
        return frameHeight;
    }
}