/*
 * Created on Sep 17, 2003
 *
 */
package bluej.graph;

import java.awt.*;
import java.awt.Graphics2D;
import java.util.Iterator;

import bluej.Config;

/**
 * @author fisker
 *
 */
public class Marquee
{
    static final Color graphbg = Config.getItemColour("colour.graph.background");
    static final float alpha = (float)0.2;
    private Graph graph;
    private GraphEditor graphEditor;
    private int drag_start_x, drag_start_y;
    private Rectangle oldRect;
    private GraphElementManager graphElementManger;
    private AlphaComposite alphaComposite;
    private Composite oldComposite;
    
    /**
     * Create a Marquee
     * @param graph
     * @param graphEditor
     */
    public Marquee(Graph graph, GraphEditor graphEditor){
        this.graph = graph;
        this.graphEditor = graphEditor;
        this.graphElementManger = new GraphElementManager(graphEditor);
        this.alphaComposite = 
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
    }
    
    /**
     * start the marquee at point x, y
     * @param x
     * @param y
     */
    public void start(int x, int y){
           drag_start_x = x;
           drag_start_y = y;
           graphElementManger.clear();
    }
    
    /**
     * Place the marquee from its starting point to the 
     * coordinate (drag_x, drag_y).
     * The marquee must have been started before this method is called.
     * @param drag_x
     * @param drag_y
     */
    public void move(int drag_x, int drag_y){
        int x = drag_start_x;
        int y = drag_start_y;
        int w = drag_x- drag_start_x;
        int h = drag_y - drag_start_y;
        //Rectangle can't handle negative numbers, modify coordinates
        if(w<0) x = x+w;
        if(h<0) y = y+h;
        w = Math.abs(w);
        h = Math.abs(h);
        Rectangle newRect = new Rectangle(x, y, w, h);  
        oldRect = newRect;
        //compute the two rectangles that make op the difference between new and
        //old
        int oldWidth = (int) oldRect.getWidth();
        int oldHeight = (int) oldRect.getHeight();
        Rectangle horizontalRect = new Rectangle(x, y + oldHeight, 
                                                 w, h - oldHeight);
        Rectangle verticalRect = new Rectangle(x + oldWidth, y,
                                               w - oldWidth, oldHeight);
                                               
        //graphEditor.repaint(horizontalRect);
        //graphEditor.repaint(verticalRect);
        graphEditor.repaint();
        findSelectedVertices(x,y,w,h);
    }
    
    
    private void findSelectedVertices(int x, int y, int w, int h){
        //clear the currently selected
       graphElementManger.clear();
        //find the intersecting vertices
       Vertex v;
       for (Iterator it = graph.getVertices(); it.hasNext();){
           v = (Vertex)it.next();
           if(v.getRectangle().intersects(x, y, w, h)){
               graphElementManger.add(v);
           }
       }
    }
    
    public void stop(){
        oldRect = null;
            graphEditor.repaint();
    }
        
    public GraphElementManager getGraphElementManager(){
        return graphElementManger;
    }
    
    public void draw(Graphics g){
        Graphics2D g2D = (Graphics2D) g.create();
        oldComposite = g2D.getComposite();
                
        if(oldRect != null){
            g2D.setColor(Color.black);
            g2D.draw(oldRect);
            g2D.setComposite(alphaComposite);
            g2D.setColor(Color.gray);
            g2D.fill(oldRect);
            g2D.setComposite(oldComposite);     
        }
    }
    
    /**
     * Get the GraphElementManger
     * @return
     */
    public GraphElementManager getGraphElementManger()
    {
        return graphElementManger;
    }
}
