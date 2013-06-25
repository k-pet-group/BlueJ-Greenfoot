package bluej.pkgmgr;

import java.util.ArrayList;

public class TestLayoutComparer extends junit.framework.TestCase
{
    private class Pos
    {
        private int x, y;
        public Pos(int x, int y) { this.x = x; this.y = y;}
        public int compareTo(Pos p) { return c.compare(x, y, p.x, p.y, 0, 0); }
        public String toString() { return "(" + x + ", " + y + ")"; }
    }
    
    ArrayList<Pos> items;
    LayoutComparer c;
        
    public void testSymmetry()
    {       
        for (Pos a : items)
            for (Pos b : items)
                assertEquals("Comparing " + a.toString() + " to " + b.toString(), a.compareTo(b), -b.compareTo(a));
    }
    
    public void testTransitivity()
    {       
        for (Pos a : items)
            for (Pos b : items)
                for (Pos c : items)
                {
                    // Transitivity is violated if:
                    // a < b and b <= c and a !< c
                    // a == b and b == c and a != c
                    
                    // Don't need to worry about a > b, etc, will be covered by other permutations
                    
                    int a_b = a.compareTo(b);
                    int b_c = b.compareTo(c);
                    int a_c = a.compareTo(c);
                    if (a_b == -1)
                    {
                        if (b_c == -1 || b_c == 0)
                        {
                            assertEquals("a < b and b <= c and a !< c, with " + a.toString() + ", " + b.toString() + ", " + c.toString(), -1, a_c);
                        }
                    }
                    else if (a_b == 0 && b_c == 0)
                    {
                        assertEquals("a == b and b == c and a != c, with " + a.toString() + ", " + b.toString() + ", " + c.toString(), 0, a_c);
                    }
                }
    }

    @Override
    protected void setUp() throws Exception
    {
        items = new ArrayList<Pos>();
        c = new LayoutComparer(null, true);
        
        // Created a grid of 7x7 items centred around 0, 0
        for (int i = -3; i <= 3; i++)
            for (int j = -3; j <= 3; j++)
                items.add(new Pos(i, j));
    }

}
