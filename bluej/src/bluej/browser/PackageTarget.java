package bluej.browser;

import bluej.Config;
import bluej.utility.Debug;
import bluej.graph.Vertex;
import bluej.graph.GraphEditor;
import bluej.utility.MultiEnumeration;
import bluej.utility.SortableVector;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;

import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: PackageTarget.java 532 2000-06-08 07:46:08Z ajp $
 ** @author Michael Cahill
 **
 ** A general target for the browser
 **/
public class PackageTarget extends Target
{
    static final Color defaultbg = Config.getItemColour("colour.package.bg.default");
    static final Color ribboncolour = defaultbg.darker().darker();
    static final Color bordercolour = Config.getItemColour("colour.target.border");
    static final Color textbg = Config.getItemColour("colour.text.bg");
    static final Color textfg = Config.getItemColour("colour.text.fg");

    protected String packageName;

    public PackageTarget(String packageName)
    {
        super(JavaNames.stripPrefix(packageName));

        this.packageName = packageName;

        setBorder(BorderFactory.createEmptyBorder(0,0, SHAD_SIZE, SHAD_SIZE));

        enableEvents(AWTEvent.MOUSE_EVENT_MASK);

        MouseListener ml = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setSelected(!getSelected());
            }
        };
        addMouseListener(ml);
    }

    protected Color getBackgroundColour()
    {
        return defaultbg;
    }

    public void paintComponent(Graphics g)
    {
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        int x = insets.left;
        int y = insets.top;

        g.setColor(getBackgroundColour());
        g.fillRect(insets.left, insets.top, width, height);

        // draw "ribbon"
        g.setColor(ribboncolour);
        int rx = insets.left + 2 * TEXT_BORDER;
        int ry = insets.top + height - 10 + 5;
        g.drawLine(rx, insets.top, rx, y + height);
        g.drawLine(x, ry, x + width, ry);

        g.drawLine(rx -10, ry, rx - 10, ry - 3);
        g.drawLine(rx - 10, ry - 3, rx - 8, ry - 5);
        g.drawLine(rx - 8, ry - 5, rx - 5, ry - 5);
        g.drawLine(rx - 5, ry - 5, rx, ry);
        g.drawLine(rx, ry, rx + 10, ry + 10);

        g.drawLine(rx + 10, ry, rx + 10, ry - 3);
        g.drawLine(rx + 10, ry - 3, rx + 8, ry - 5);
        g.drawLine(rx + 8, ry - 5, rx + 5, ry - 5);
        g.drawLine(rx + 5, ry - 5, rx, ry);
        g.drawLine(rx, ry, rx - 10, ry + 10);

        g.setColor(textbg);
        g.fillRect(x + TEXT_BORDER, y + TEXT_BORDER,
        width - 2*TEXT_BORDER, TEXT_HEIGHT);

        g.setColor(getBorderColour());
        g.setFont(getFont());
        Utility.drawCentredText(g, displayName,
                                    x + TEXT_BORDER, y + TEXT_BORDER,
                                    width - 2*TEXT_BORDER, TEXT_HEIGHT);
        g.drawRect(x + TEXT_BORDER, y + TEXT_BORDER,
                    width - 2*TEXT_BORDER, TEXT_HEIGHT);
        drawBorders(g);

        g.setColor(shadowCol);
        drawShadow(g);
    }
    protected void popupMenu(int x, int y)
    {
        JPopupMenu menu = new JPopupMenu();

        PkgMgrFrame[] openFrames = PkgMgrFrame.getAllFrames();

        if(openFrames != null) {
            for(int i=0; i<openFrames.length; i++) {

                Action useAction = new UseAction("Use in package " +
                                          openFrames[i].getPackage().getId(),
                                          openFrames[i].getPackage());
            	useAction.setEnabled(true);
                menu.add(useAction);
            }

             menu.show(this,x,y);
        }
    }

    private class UseAction extends AbstractAction
    {
        private Package pkg;

        public UseAction(String menu, Package pkg)
        {
            super(menu);

            this.pkg = pkg;
        }

        public void actionPerformed(ActionEvent e) {
//            pkg.insertLibClass(cl.getName());
        }
    }
}
