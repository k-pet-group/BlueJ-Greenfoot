
package bluej.graph;

import java.awt.Graphics2D;
import java.util.Iterator;

import bluej.pkgmgr.dependency.*;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.ClassTarget;


public class GraphPainter
{
    ClassTargetPainter classTargetPainter = new ClassTargetPainter();
	ReadmeTargetPainter readmePainter = new ReadmeTargetPainter();
	PackageTargetPainter packageTargetPainter = new PackageTargetPainter();


    public GraphPainter(){
    }
    
    public void paint(Graphics2D g, Graph graph){
        Edge edge;
        Vertex vertex;
        Target target;
        //Paint the edges
        for(Iterator it = graph.getEdges(); it.hasNext(); ) {
            edge = (Edge)it.next();
            //edge.draw(g);
            edgePainter(g, edge);
        }
        
        //Paint the vertices
        for(Iterator it = graph.getVertices(); it.hasNext(); ) {
            vertex = (Vertex)it.next();
            vertexPainter(g, vertex);
        }
        
        //Paint the ghosts
        for(Iterator it = graph.getVertices(); it.hasNext(); ) {
            vertex = (Vertex)it.next();
            if( vertex instanceof Target){
                target = (Target) vertex;
                if (target.isMoving() && target.hasMoved()){
                    vertexGhostPainter(g, vertex);
                }
            }
        }
        
    }
    
    private void vertexPainter(Graphics2D g, Vertex vertex){

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
            //vertex.draw(g);
        }
    }
    
    private void vertexGhostPainter(Graphics2D g, Vertex vertex){

        if (vertex instanceof ClassTarget){
           classTargetPainter.paintGhost(g, (ClassTarget) vertex);        
        }
        else if (vertex instanceof PackageTarget){
            packageTargetPainter.paintGhost(g, (PackageTarget) vertex);
        }
        else {
            //vertex.draw(g);
        }
    }
    
    private void edgePainter(Graphics2D g, Edge edge){
        if (edge instanceof ImplementsDependency){
            ((ImplementsDependency) edge).draw(g);
        }
        else if (edge instanceof ExtendsDependency){
            ((ExtendsDependency) edge).draw(g);
        }
        else if (edge instanceof UsesDependency){
            ((UsesDependency) edge).draw(g);
        }
    }
    
}
