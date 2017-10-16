import java.io.*;
import java.util.*;

/**
 * A cat.
 */
public class Cat
{
    private String name;
    private String color;
    private boolean fed;

    /**
     * Creates a cat with a random name and color.
     */
    public Cat()
    {
        Random r = new Random();
        List<String> possibleNames = Arrays.asList("Bob", "Cole", "Larry", "Marmalade");
        List<String> possibleColors = Arrays.asList("white", "tabby", "ginger", "black");
        name = possibleNames.get(r.nextInt(possibleNames.size()));
        color = possibleColors.get(r.nextInt(possibleColors.size()));
    }

    /**
     * Gets a brief description of the cat.
     */
    public String getDescription()
    {
        return "A " + color + " cat named " + name;
    }

    /**
     * Sets a new color for the cat.
     */
    public void setColor(String color)
    {
        this.color = color;
    }

    /**
     * Prints a message on the terminal relating to the cat's current state.
     */
    public void listen()
    {
        if (fed)
        {
            System.out.println(name + " purrs.");
        }
        else
        {
            System.out.println(name + " meows at you.");
        }
    }

    /**
     * Plays with the cat
     */
    public void play() throws IOException
    {
        System.out.println("What will you get " + name + " to chase?");
        String item = new BufferedReader(new InputStreamReader(System.in)).readLine();
        System.out.println(name + " chases the " + item);
    }
}
