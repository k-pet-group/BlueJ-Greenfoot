package bluej.pkgmgr.graphPainter;

import java.awt.Graphics2D;
import java.awt.Point;

import bluej.pkgmgr.dependency.Dependency;

/**
 * @author fisker
 *
 */
public interface DependencyPainter
{
    public Point getPopupMenuPosition(Dependency dependency);
    public void paint(Graphics2D g, Dependency d, boolean hasFocus);
}
