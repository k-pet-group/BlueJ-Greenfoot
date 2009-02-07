import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;
import java.awt.Font;
import java.util.Calendar;

/**
 * The ScoreBoard is used to display results on the screen. It can display some
 * text and several numbers.
 * 
 * @author M Kolling
 * @version 1.0
 */
public class ScoreBoard extends Actor
{
    private static final float FONT_SIZE = 48.0f;
    private static final int WIDTH = 700;
    private static final int HEIGHT = 500;
    
        
    public ScoreBoard()
    {
        makeImage("The Return of the Greeps", "This time it's personal", "nobody", 100, 100);
    }

    /**
     * Create a score board for an interim result.
     */
    public ScoreBoard(String[] authors, String text, String prefix, int map, int[][] scores)
    {
        makeImage(getAuthorString(authors), text, prefix, scores[0][map], scores[1][map]);
        addMapScores(map, scores);
    }
    
    /**
     * Create a score board for the final result.
     */
    public ScoreBoard(String[] authors, String text, String prefix, int[][] scores)
    {
        int total1 = 0;
        int total2 = 0;
        for (int i = 0; i < scores[0].length; i++) {
            total1 += scores[0][i];
            total2 += scores[1][i];
        }

        makeImage(getAuthorString(authors), text, prefix, total1, total2);
        addMapScores(scores[0].length-1, scores);
        printResultToTerminal(authors, scores);
    }
    
    private String getAuthorString(String[] authors)
    {
        String authorString = authors[0];
        for(int i = 1; i<authors.length; i++)
            authorString += " vs " + authors[i];
        return authorString;
    }
    
    /**
     * Make the score board image.
     */
    private void makeImage(String title, String text, String prefix, int score1, int score2)
    {
        GreenfootImage image = new GreenfootImage(WIDTH, HEIGHT);

        image.setColor(new Color(0, 0, 0, 128));
        image.fillRect(0, 0, WIDTH, HEIGHT);
        image.setColor(new Color(0, 0, 0, 128));
        image.fillRect(5, 5, WIDTH-10, HEIGHT-10);
        image.setColor(Color.WHITE);
        Font font = image.getFont();
        font = font.deriveFont(FONT_SIZE);
        image.setFont(font);
        image.drawString(title, 60, 100);
        font = font.deriveFont(80.0f);
        image.setFont(font);
        image.drawString(score1 + " - " + score2 , 80, 300);
        font = font.deriveFont(30.0f);
        image.setFont(font);
        image.drawString(text, 60, 460);
        setImage(image);
    }
    
    /**
     * Add the scores for the individual maps to the score board. 'scores' is an
     * array with all scores, 'mapNo' is the number of the current map (array entries
     * past this value have no valid value).
     */
    private void addMapScores(int mapNo, int[][] scores)
    {
        GreenfootImage image = getImage();
        Font font = image.getFont();
        font = font.deriveFont(20.0f);
        image.setFont(font);
        image.setColor(Color.WHITE);
        for(int i = 0; i <= mapNo; i++) {
            String score1 = "" + scores[0][i];
            score1 += "    ".substring(score1.length());
            image.drawString("Map " + (i+1) + ":   " + score1 + scores[1][i], 500, 180+(i*28));
        }
    }
    
    private void printResultToTerminal(String[] authors, int[][] scores)
    {
        Calendar now = Calendar.getInstance();
        String time = now.get(Calendar.HOUR_OF_DAY) + ":";
        int min = now.get(Calendar.MINUTE);
        if(min < 10)
            time += "0" + min;
        else
            time += min;
        System.out.println(time + ": ");
        
        for(int team = 0; team < 2; team++) {
            System.out.print("[");
            int total = 0;
            for(int i = 0; i < scores[team].length; i++) {
                int score = scores[team][i];
                total += score;
                if (score < 10)
                    System.out.print("   " + score);
                else if (score < 100) 
                    System.out.print("  " + score);
                else
                    System.out.print(" " + score);
            }
            System.out.println("]  " + total + "  -- author " + authors[team]);
        }
    }
}
