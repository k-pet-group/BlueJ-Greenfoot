package org.bluej.extensions.submitter;

import java.awt.event.*;
import java.awt.*;

import javax.swing.*;


import java.awt.geom.*;

/**
 * This behaves (or should do so) as much as possible like the 1.4.1
 * JProgressBar, when all is on 1.4.1 you can use that one.
 * Really, it is just a subset of JProgressBar, but it is what I need 
 */
public class SubmitterProgressBar extends JPanel implements ActionListener
  {
  private Rectangle2D.Double square = new Rectangle2D.Double(0, 0, 30, 10);
  private Timer aTimer;
  private double startX=0;
  private boolean isActive=false;

  /**
   * No params, a very easy one :-)
   */
  public SubmitterProgressBar()
    {
    super();
    aTimer = new Timer(0,this);
    aTimer.setDelay(200);
    }

  /**
   * to start the bar set it to true, to stop it set it to false.
   */
  void setIndeterminate(boolean status)
    {
    isActive = status;
    
    if ( status ) aTimer.start();
    else          aTimer.stop();

    repaint();
    }

  /**
   * For compatibility with JProgressBar
   */
  void setBorderPainted (boolean status )
    {
      
    }

  public void actionPerformed ( ActionEvent event )
    {
    double curWidth = getSize().getWidth();
//    System.out.println ("cur vidth"+curWidth);
    if ( startX+30 > curWidth ) startX = 0;

//    System.out.println ("startX="+startX);
    square.x = startX = startX+5;
    repaint();
    }


  /**
   * Called by the window system when you want to repaint this Component
   */
  public void paintComponent(Graphics g) 
    {
    clear(g);

    if ( ! isActive ) return;
    
    Graphics2D g2d = (Graphics2D)g;
    g2d.setPaint(Color.green);
    g2d.fill(square);
    }

  /**
   * Clearing this one means repainting my parent ....
   */
  protected void clear(Graphics g) 
    {
    super.paintComponent(g);
    }
    
  }