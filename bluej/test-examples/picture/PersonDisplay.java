/**
 ** Class PersonDisplay - write a description of the class here
 ** 
 ** Author: John Rosenberg
 ** Date: 
 **/

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class imagePanel extends JPanel
{
    private Image image;

    public imagePanel(Image theImage)
    {
	image = theImage;
    }

    public void paintComponent(Graphics g)
    {
	super.paintComponent(g);
	g.drawImage(image, 0, 0, this);
    }
}

class imageFrame extends JFrame
{
    private Image image;

    public imageFrame(String fileName)
    {
	setTitle(fileName);
	image = Toolkit.getDefaultToolkit().getImage(fileName);
	MediaTracker tracker = new MediaTracker(this);
	tracker.addImage(image, 0);
	try {tracker.waitForID(0); }
	catch (InterruptedException e) {}

	setSize(image.getWidth(this), image.getHeight(this));
	addWindowListener(new WindowAdapter()
	    {  public void windowClosing(WindowEvent e)
	       {  
		dispose();
		System.exit(0);
	       }
	    }  );
	Container contentPane = getContentPane();
	contentPane.add(new imagePanel(image));
	show();
    }
}

public class PersonDisplay
{
    /**
     ** Constructor for objects of class PersonDisplay
     **/
    public PersonDisplay(String name)
    {
	String fileName = name + ".gif";
	if ((new File(fileName)).exists())
	{
	    JFrame theFrame = new imageFrame(fileName);
	} else
	{
	    System.out.println("Could not find an image with the name " +
			name + ".gif");
	}
    }

}
