package bluej.pkgmgr;

import bluej.utility.Comparer;

/**
 ** @version $Id: LayoutComparer.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** An ordering on targets to make layout nicer (reduce line intersections, etc.)
 **/
class LayoutComparer implements Comparer
{
	Target centre;
	boolean in;
	
	public LayoutComparer(Target centre, boolean in)
	{
		this.centre = centre;
		this.in = in;
	}
		
	/**
	 ** Order <a> and <b> depending on their relative positions
	 ** and their positions relative to the centre
	 **
	 ** Note: this is designed to reduce intersections when drawing lines.
	 **/
	public int cmp(Object a, Object b)
	{
		Target ta = in ? ((Dependency)a).getFrom() : ((Dependency)a).getTo();
		Target tb = in ? ((Dependency)b).getFrom() : ((Dependency)b).getTo();
			
		int ax = ta.x + ta.width/2;
		int ay = ta.y + ta.height/2;
		int bx = tb.x + tb.width/2;
		int by = tb.y + tb.height/2;
			
		if((ax == bx) && (ay == by))
			return 0;
				
		int cx = centre.x + centre.width/2;
		int cy = centre.y + centre.height/2;
			
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
