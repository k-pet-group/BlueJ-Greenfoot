package bluej.pkgmgr;

import java.util.Comparator;

import bluej.pkgmgr.target.*;

/**
 * An ordering on targets to make layout nicer (reduce line intersections, etc.)
 *
 * @author Michael Cahill
 * @version $Id: LayoutComparer.java 1954 2003-05-15 06:06:01Z ajp $
 */
public class LayoutComparer implements Comparator
{
	DependentTarget centre;
	boolean in;

	public LayoutComparer(DependentTarget centre, boolean in)
	{
		this.centre = centre;
		this.in = in;
	}

	/**
     * Order <a> and <b> depending on their relative positions
     * and their positions relative to the centre
     *
     * Note: this is designed to reduce intersections when drawing lines.
     */
	public int compare(Object a, Object b)
	{
		DependentTarget ta = in ? ((Dependency)a).getFrom() : ((Dependency)a).getTo();
		DependentTarget tb = in ? ((Dependency)b).getFrom() : ((Dependency)b).getTo();

        int ax = ta.getX() + ta.getWidth()/2;
        int ay = ta.getY() + ta.getHeight()/2;
        int bx = tb.getX() + tb.getWidth()/2;
        int by = tb.getY() + tb.getHeight()/2;

		if((ax == bx) && (ay == by))
			return 0;

        int cx = centre.getX() + centre.getWidth()/2;
        int cy = centre.getY() + centre.getHeight()/2;

		boolean a_above = (ay < cy);
		boolean a_left = (ax < cx);
		int a_quad = (a_above ? 0 : 2) + (a_left ? 0 : 1);
		boolean b_above = (by < cy);
		boolean b_left = (bx < cx);
		int b_quad = (b_above ? 0 : 2) + (b_left ? 0 : 1);

		if(a_quad != b_quad) // different quadrants
			return (a_quad > b_quad) ? 1 : -1;
		// otherwise, we're in the same quadrant
		int result = in ? ((ax < bx) ? -1 : 1) : ((ay < by) ? -1 : 1);
		return (a_above == a_left) ? -result : result;
	}
}
