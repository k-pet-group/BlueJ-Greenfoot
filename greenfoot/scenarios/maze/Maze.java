import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A maze.
 * 
 * @author Poul Henriksen
 */
public class Maze extends World
{
    public static int getWallSize() {
        return 10;
    }
    
    public Maze() {
        super(41, 41, Maze.getWallSize());
        buildWalls();
        addCarver();
        addAgents(6);     
    }

    private void buildWalls() {
       for(int x = 0; x < getWidth(); x++) {            
            for(int y = 0; y < getHeight(); y++) {    
                addObject(new Wall(), x, y);
            }
        }        
    }
    
    private void addCarver() {
        MazeCarver mazeCarver = new MazeCarver();
        addObject(mazeCarver, getRanmdomX(), getRanmdomY());    
    }
    
    
    public void addAgents(int number) {        
        for(int i = 0; i < number; i++) {
            addAgent();
        }
    }
    
    private void addAgent() {        
        Agent agent = new Agent();
        addObject(agent, getRanmdomX(), getRanmdomY());      
    }
    
    private int getRanmdomX() {
        int width = (int) Math.floor(getWidth() / 2.);        
        return Greenfoot.getRandomNumber(width) * 2 + 1;
    }
    
    private int getRanmdomY() {
        int height = (int) Math.floor(getHeight() / 2.);        
        return Greenfoot.getRandomNumber(height) * 2 + 1;
    }
   
}
