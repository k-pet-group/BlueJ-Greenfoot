package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Graphics2D;
import java.util.Iterator;


import bluej.graph.*;
import bluej.pkgmgr.dependency.*;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.Package;

/**
 * Paints a Graph using TargetPainters
 * @author fisker
 * @version $Id: GraphPainterStdImpl.java 2475 2004-02-10 09:53:59Z fisker $
 */
public class GraphPainterStdImpl implements GraphPainter
{
    /**
     * Colors used for shadows. 
     * On white background Color(0,0,0,a) displayes as Color(255-a,255-a,255-a)
     */
    static final Color[] colours = {
            new Color(0,0,0,13),//on white background this is (242,242,242)
            new Color(0,0,0,44),//on white background this is (211,211,211)     
            new Color(0,0,0,66),//on white background this is (189,189,189)
            new Color(0,0,0,172)//on white background this is (83,83,83)
    };
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 4;
    static final float alpha = (float)0.5;
    static AlphaComposite alphaComposite = 
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
    
    private static final ClassTargetPainter classTargetPainter = new ClassTargetPainter();
    private static final ReadmeTargetPainter readmePainter = new ReadmeTargetPainter();
    private static final PackageTargetPainter packageTargetPainter = new PackageTargetPainter();
    private static final ExtendsDependencyPainter extendsDependencyPainter = new ExtendsDependencyPainter();
    private static final ImplementsDependencyPainter implementsDependencyPainter = new ImplementsDependencyPainter();
    private static final UsesDependencyPainter usesDependencyPainter = new UsesDependencyPainter();
	private static final GraphPainterStdImpl singleton = new GraphPainterStdImpl();
	
    private GraphPainterStdImpl(){} // prevent instantiation
    
    /**
     * Paint 'graph' on 'g'
     */
    public void paint(Graphics2D g, Graph graph){
        Edge edge;
        Vertex vertex;
        Target target;
        DependentTarget dependentTarget = null;
        
        paintEdges(g, graph);
        dependentTarget = paintVertices(g, graph, dependentTarget);
        paintGhosts(g, graph);
        
        //Paint the intermediate dependencies
        if (dependentTarget != null) {
            paintIntermediateDependency(g, dependentTarget);
        }
    }
    
    
    /**
     * Paint the egdes in 'graph' on 'g'
     * @param g
     * @param graph
     */
    private void paintEdges(Graphics2D g, Graph graph) {
        Edge edge;
        //Paint the edges
        for(Iterator it = graph.getEdges(); it.hasNext(); ) {
            edge = (Edge)it.next();
            paintEdge(g, edge);
        }
    }
    
    
    /**
     * Paint the vertices in 'graph' on 'g'. If one of the targets to be paintet
     * is in the process of drawing a dependency to another class, assign
     * that class to 'dependency'
     * @param g
     * @param graph
     * @param dependentTarget
     * @return the class from which a dependency is being drawn. Null if none.
     */
    private DependentTarget paintVertices(Graphics2D g, Graph graph, DependentTarget dependentTarget) {
        Vertex vertex;
        //Paint the vertices
        for(Iterator it = graph.getVertices(); it.hasNext(); ) {
            vertex = (Vertex)it.next();
            //Check if a dependency is being drawn from the current target.
            if (vertex instanceof DependentTarget && dependentTarget==null) { 
                dependentTarget = (DependentTarget) vertex;
                if (!dependentTarget.isDrawingDependency()) {
                    dependentTarget = null;
                }
            }
            paintVertex(g, vertex);
        }
        return dependentTarget;
    }
    
    
    /**
     * Paint the ghosts (transparent versions) of the vertices in 'graph' that
     * are being dragged in the diagram.
     * @param g
     * @param graph
     */
    private void paintGhosts(Graphics2D g, Graph graph) {
        Vertex vertex;
        Target target;
        //Paint the ghosts
        for(Iterator it = graph.getVertices(); it.hasNext(); ) {
            vertex = (Vertex)it.next();
            if( vertex instanceof Target){
                target = (Target) vertex;
                if (target.isMoving() && target.hasMoved()){
                    paintGhostVertex(g, vertex);
                }
            }
        }
    }


    /**
     * Paint 'edge' on 'g'
     * @param g
     * @param edge
     */
    private void paintEdge(Graphics2D g, Edge edge){
        if (edge instanceof ImplementsDependency){
            implementsDependencyPainter.paint(g, (ImplementsDependency) edge);
        }
        else if (edge instanceof ExtendsDependency){
            extendsDependencyPainter.paint(g, (ExtendsDependency) edge);
        }
        else if (edge instanceof UsesDependency){
            usesDependencyPainter.paint(g, (UsesDependency) edge);
        }
        else {
            //asserts false;
        }
    }
    
    
    /**
     * Paint 'vertex' on 'g' using the appropiate painter.
     * @param g
     * @param vertex
     */
    private void paintVertex(Graphics2D g, Vertex vertex){

        if (vertex instanceof ClassTarget){
            classTargetPainter.paint(g, (ClassTarget) vertex);        
        }
        else if (vertex instanceof ReadmeTarget){
            readmePainter.paint(g, (ReadmeTarget) vertex);
        }
        else if (vertex instanceof PackageTarget){
            packageTargetPainter.paint(g, (PackageTarget) vertex);
        }
        else {
            //asserts false;
        }
    }
    
    
    /**
     * Paint a ghostet (transparent) version of 'vertex' on 'g'
     * @param g
     * @param vertex
     */
    private void paintGhostVertex(Graphics2D g, Vertex vertex){

        if (vertex instanceof ClassTarget){
           classTargetPainter.paintGhost(g, (ClassTarget) vertex);        
        }
        else if (vertex instanceof PackageTarget){
            packageTargetPainter.paintGhost(g, (PackageTarget) vertex);
        }
        else {
            //asserts false;
        }
    }
    

    /**
     * Paint an arrow representing the intermediate dependency 'd', using the
     * appropiate painter, on 'g'
     * @param g
     * @param d
     */
    private void paintIntermediateDependency(Graphics2D g, DependentTarget d){
        
       if (d.getPackage().getState() == Package.S_CHOOSE_EXT_TO) {
           extendsDependencyPainter.paintIntermediateDependency(g, d);
       } else if (d.getPackage().getState() == Package.S_CHOOSE_USES_TO){
           usesDependencyPainter.paintIntermedateDependency(g, d);
       }
    }
    

    /**
     * Get reference to the singleton GraphPainterStdImpl
     * @return GraphPainterStdImpl
     */
    public static GraphPainter getInstance() {
        return singleton;
    }
    
}
