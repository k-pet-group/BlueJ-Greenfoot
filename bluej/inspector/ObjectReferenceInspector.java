import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.event.*;
import com.sun.jdi.*;
import bluej.debugger.*;
import bluej.debugger.jdi.*;
import bluej.Config;

/**
 *  An Inspector plugin that displays the an object and its references (within
 *  an "Inspect window") and also allows method calls on those objects.
 *
 *@author     Duane Buck
 */

public class ObjectReferenceInspector extends InspectorPanel
         implements Runnable, MouseListener, MouseMotionListener
{
    boolean initialized = false;
    boolean refreshFlag = true;
    DebuggerObject[] objGot = new DebuggerObject[10];
    int objGotCnt = 0;

    int nnodes;
    GNode nodes[] = new GNode[10];

    int nedges;
    Edge edges[] = new Edge[20];

    JPanel panel;

    JButton inspectBtn, getBtn;
    JTextField primitiveText = null;
    JComboBox primitiveType = null;
    final String[] ptypes = {
            "boolean", "byte", "char", "double", "float",
            "int", "long", "short", "String", "null"};

    Thread relaxer;
    boolean random;

    GNode pick = null, select = null;
    Set pickExtended = null;
    boolean pickAll = false;

    boolean popped = false, dragged = false;
    GNode active;
    int mthdIdx;
    int fldIdx;
    java.util.Vector parmList;
    Image offscreen;
    Dimension offscreensize;
    Graphics offgraphics;
    double offsetx = 0, offsety = 0;

    final Color fixedColor = Color.red;
    final Color pickColor = new Color(120, 200, 160);
    final Color activeColor = Color.pink;
    final Color selectColor = Color.red;
    final Color edgeColor = Color.black;
    final Color nodeColor = new Color(250, 220, 100);
    final Color stressColor = Color.darkGray;
    final Color arcColor1 = Color.black;
    final Color arcColor2 = Color.pink;
    final Color arcColor3 = Color.red;
    static Value copiedObjectReference = null;

    private final static int MAX_IDX = 11;

    private static String inspectLabel = Config.getString("debugger.objectviewer.inspect");
    private static String getLabel = Config.getString("debugger.objectviewer.get");
    private static String referenceLabel = Config.getString("debugger.objectviewer.ori.reference");
    private static String copyLabel = Config.getString("debugger.objectviewer.ori.copy");
    private static String pasteLabel = Config.getString("debugger.objectviewer.ori.paste");
    private static String methodLabel = Config.getString("debugger.objectviewer.ori.method");
    private static String methodDialogLabel = Config.getString("debugger.objectviewer.ori.methodDialog");
    private static String assignDialogLabel = Config.getString("debugger.objectviewer.ori.assignDialog");
    private static String dialogLabel = Config.getString("debugger.objectviewer.ori.dialog");
    private static String selectedLabel = Config.getString("debugger.objectviewer.ori.selected");
    private static String primitiveLabel = Config.getString("debugger.objectviewer.ori.primitive");
    private static String invokeLabel = Config.getString("debugger.objectviewer.ori.invoke");
    private static String invokeMethodLabel = Config.getString("debugger.objectviewer.ori.invokeMethod");
    private static String assignLabel = Config.getString("debugger.objectviewer.ori.assign");
    private static String primitiveOrStringLabel = Config.getString("debugger.objectviewer.ori.primitiveOrString");
    private static String errorLabel = Config.getString("debugger.objectviewer.ori.error");
    private static String primInspectErrorLabel = Config.getString("debugger.objectviewer.ori.primInspectError");
    private static String nothingInspectedErrorLabel = Config.getString("debugger.objectviewer.ori.nothingInspectedError");
    private static String printLabel = Config.getString("debugger.objectviewer.ori.print");
    private static String printScaleLabel1 = Config.getString("debugger.objectviewer.ori.printScale.1");
    private static String printScaleLabel2 = Config.getString("debugger.objectviewer.ori.printScale.2");
    private static String printScaleLabel3 = Config.getString("debugger.objectviewer.ori.printScale.3");
    private static String numberErrorLabel = Config.getString("debugger.objectviewer.ori.numberError");
    private static String datatypeErrorLabel = Config.getString("debugger.objectviewer.ori.datatypeError");
    private static String noSelectionErrorLabel = Config.getString("debugger.objectviewer.ori.noSelectionError");
    private static String noAssignmentErrorLabel = Config.getString("debugger.objectviewer.ori.noAssignmentError");
    private static String noInvokeErrorLabel = Config.getString("debugger.objectviewer.ori.noInvokeError");
    private static String moreLabel = Config.getString("debugger.objectviewer.ori.more");
    private static String topLabel = Config.getString("debugger.objectviewer.ori.top");
    private static String bottomLabel = Config.getString("debugger.objectviewer.ori.bottom");
    private static String nextLabel = Config.getString("debugger.objectviewer.ori.next");
    private static String entryLabel = Config.getString("debugger.objectviewer.ori.entry");
    private static String elementLabel = Config.getString("debugger.objectviewer.ori.element");
    private static String keyLabel = Config.getString("debugger.objectviewer.ori.key");
    private static String valueLabel = Config.getString("debugger.objectviewer.ori.value");
    private static String alreadyShownLabel = Config.getString("debugger.objectviewer.ori.alreadyShown");
    private static String cannotPastePrimLabel = Config.getString("debugger.objectviewer.ori.cannotPastePrim");
    private static String parmToolTipLabel = Config.getString("debugger.objectviewer.ori.parmToolTip");
    private static String assignToolTipLabel = Config.getString("debugger.objectviewer.ori.assignToolTip");
    private static String closeLabel = Config.getString("debugger.objectviewer.ori.close");
    private static String methodResultLabel = Config.getString("debugger.objectviewer.ori.methodResult");

    public String[] getInspectedClassnames()
    {
        String[] ic = {
                "java.lang.Object"};
        return ic;
    }

    public String getInspectorTitle()
    {
        return "Object References";
    }

    public boolean initialize(DebuggerObject obj)
    {
        super.initialize(obj);
        if (copiedObjectReference == null)
        {
            copiedObjectReference = obj.getObjectReference();
        }
        panel =
            new JPanel(null, false)
            {
                public void paint(Graphics g)
                {
                    super.paint(g);
                    if (!initialized)
                    {
                        initialized = true;
                        GNode root = addNode(
                                ObjectReferenceInspector.this.obj.getObjectReference());
                        extend(root);
                        Dimension initialDimension = initializeGraph();
                        panel.setPreferredSize(initialDimension);
                        panel.revalidate();
                        select = root;
                        refreshFlag = false;
                    }
                    else if (refreshFlag)
                    {
                        refreshFlag = false;
                        doRefresh();
                    }
                    updateGraphics(g, false);
                }
            };
        clear();
        panel.addMouseListener(this);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(Config.generalBorder);
        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel classNameLabel = new JLabel(obj.getObjectReference().toString());
        titlePanel.add(classNameLabel, BorderLayout.CENTER);
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel(null);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // Create panel with "inspect" and "get" buttons
        JPanel inspectGetPanel = new JPanel(null);
        inspectGetPanel.setLayout(new GridLayout(0, 1));

        inspectBtn = new JButton(inspectLabel);
        inspectBtn.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    if (select != null)
                    {
                        if (select.obj instanceof ObjectReference)
                        {
                            fireInspectEvent(JdiObject.getDebuggerObject(((ObjectReference) select.obj)));
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(getJFrame(),
                                    primInspectErrorLabel, errorLabel, JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(getJFrame(),
                                nothingInspectedErrorLabel, errorLabel, JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        inspectGetPanel.add(inspectBtn);

        getBtn = new JButton(getLabel);
        getBtn.setEnabled(false);
        getBtn.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                {
                }
            }
                );
        inspectGetPanel.add(getBtn);

        JButton printWorldButton = new JButton(printLabel);
        ActionListener printWorldButtonListener =
            new ActionListener()
            {
                double scale = 1.0;
                PageFormat pf = null;

                public void actionPerformed(ActionEvent e)
                {
                    Dimension current = panel.getSize();
                    Dimension newSize = getGraphicsSize();
                    if (!current.equals(newSize))
                    {
                        panel.setPreferredSize(newSize);
                        panel.revalidate();
                    }
                    PrinterJob printJob = PrinterJob.getPrinterJob();
                    if (pf == null)
                    {
                        pf = printJob.defaultPage();
                    }
                    PageFormat savePf = pf;
                    pf = printJob.pageDialog(pf);
                    if (pf == savePf)
                    {
                        return;
                    }
                    scale = Math.min(pf.getImageableWidth() / panel.getWidth(),
                            pf.getImageableHeight() / panel.getHeight());
                    double otherScale = Math.min(pf.getImageableHeight() / panel.getWidth(),
                            pf.getImageableWidth() / panel.getHeight());
                    while (true)
                    {
                        String inputValue = JOptionPane.showInputDialog(getJFrame(),
                                printScaleLabel1 + " " + (int) Math.floor(scale * 100) + "):"
                                 + printScaleLabel2
                                 + (otherScale > scale ? printScaleLabel3 + (int) Math.floor(otherScale * 100) + ")" : ""));
                        if (inputValue == null)
                        {
                            return;
                        }
                        if (inputValue.equals(""))
                        {
                            break;
                        }
                        try
                        {
                            scale = Double.parseDouble(inputValue) / 100.;
                            break;
                        }
                        catch (NumberFormatException nfe)
                        {
                            JOptionPane.showMessageDialog(getJFrame(),
                                    numberErrorLabel, errorLabel,
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    printJob.setPrintable(
                        new Printable()
                        {
                            public int print(Graphics g_parm, PageFormat pf, int pi)
                                throws PrinterException
                            {
                                Graphics2D g = (Graphics2D) g_parm;
                                g.translate(pf.getImageableX(),
                                        pf.getImageableY());
                                g.scale(scale, scale);
                                if (0 < pi)
                                {
                                    return Printable.NO_SUCH_PAGE;
                                }
                                ObjectReferenceInspector.this.updateGraphics(g, true);
                                return Printable.PAGE_EXISTS;
                            }
                        }, pf);
                    if (printJob.printDialog())
                    {
                        try
                        {
                            printJob.print();
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                }
            };
        printWorldButton.addActionListener(printWorldButtonListener);
        inspectGetPanel.add(printWorldButton);
        buttonPanel.add(inspectGetPanel);

        JPanel primitivePanel = new JPanel(new GridLayout(0, 1));
        primitivePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), primitiveOrStringLabel));

        primitiveText = new JTextField();
        primitivePanel.add(primitiveText);

        primitiveType = new JComboBox(ptypes);
        primitiveType.setPreferredSize(new Dimension(90, 0));
        primitivePanel.add(primitiveType);
        buttonPanel.add(primitivePanel);
        JPanel buttonFramePanel = new JPanel();
        buttonFramePanel.setLayout(new BorderLayout(0, 0));
        buttonFramePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(buttonFramePanel, BorderLayout.EAST);
        JPanel boxpanel = new JPanel(null);
        boxpanel.setLayout(new BoxLayout(boxpanel, BoxLayout.X_AXIS));
        boxpanel.add(panel);
        JScrollPane scrollPanel = new JScrollPane(boxpanel);
        scrollPanel.getVerticalScrollBar().setUnitIncrement(12);
        scrollPanel.getHorizontalScrollBar().setUnitIncrement(12);
        mainPanel.add(scrollPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        return true;
    }

    public void refresh()
    {
        refreshFlag = true;
    }

    public void doRefresh()
    {
        for (int i = 0; i < nnodes; i++)
        {
            nodes[i].lbl = null;
            nodes[i].group = null;
        }
        for (int i = 0; i < nedges; i++)
        {
            edges[i] = null;
        }
        nedges = 0;
        GNode root = findGNode(obj.getObjectReference());
        if (root == null)
        {
            root = addNode(obj.getObjectReference());
        }
        else
        {
            root.setLbl();
        }
        extend(root);
        for (int i = 0; i < objGotCnt; i++)
        {
            root = findGNode(objGot[i].getObjectReference());
            if (root == null)
            {
                root = addNode(objGot[i].getObjectReference());
                extend(root);
            }
            else if (root.lbl == null)
            {
                root.setLbl();
                extend(root);
            }
        }
        int j = 0;
        for (int i = 0; i < nnodes; i++)
        {
            if (nodes[i].lbl == null)
            {
                j++;
            }
            else
            {
                if (j > 0)
                {
                    nodes[i - j] = nodes[i];
                }
            }
        }
        for (int i = nnodes - j; i < nnodes; i++)
        {
            nodes[i] = null;
        }
        nnodes -= j;
    }

    public void clear()
    {
        for (int i = 0; i < nodes.length; i++)
        {
            nodes[i] = null;
        }
        nnodes = 0;
        for (int i = 0; i < edges.length; i++)
        {
            edges[i] = null;
        }
        nedges = 0;
        initialized = false;
    }

    public GNode addNode(Value obj)
    {
        GNode n = new GNode(obj);
        if (nnodes >= nodes.length)
        {
            allocateNodes();
        }
        nodes[nnodes] = n;
        nnodes++;
        return n;
    }

    public GNode addNode(Value obj, String lbl)
    {
        GNode n = new GNode(obj);
        n.lbl = lbl;
        if (nnodes >= nodes.length)
        {
            allocateNodes();
        }
        nodes[nnodes] = n;
        nnodes++;
        return n;
    }

    public GNode addNode(String lbl)
    {
        GNode n = new GNode(lbl);
        if (nnodes >= nodes.length)
        {
            allocateNodes();
        }
        nodes[nnodes] = n;
        nnodes++;
        return n;
    }

    public Edge addEdge(GNode from, GNode to)
    {
        if (to.x == Double.MIN_VALUE)
        {
            to.x = from.x + 10;
        }
        if (to.y == Double.MIN_VALUE)
        {
            to.y = from.y + 10;
        }
        Edge e = new Edge();
        e.from = from;
        e.to = to;
        if (nedges >= edges.length)
        {
            allocateEdges();
        }
        edges[nedges++] = e;
        return e;
    }

    public Dimension initializeGraph()
    {
        int across = 80;
        int down = 20;
        for (int i = 0; i < nnodes; i++)
        {
            GNode n = nodes[i];
            n.x = across;
            n.y = down;
            if (((i + 1) / 8) % 2 == 0)
            {
                across += 40;
            }
            else
            {
                across -= 40;
            }
            down += 40;
        }
        across += 40;
        down += 40;
        return new Dimension(400, down);
    }

    public void run()
    {
        Thread me = Thread.currentThread();

        for (int i = 0; i < nnodes; i++)
        {
            nodes[i].relaxing = false;
        }
        pick.relaxing = true;

        while (relaxer == me)
        {
            relax();
            if (random && (Math.random() < 0.03))
            {
                GNode n = nodes[(int) (Math.random() * nnodes)];
                if (!n.fixed)
                {
                    n.x += 100 * Math.random() - 50;
                    n.y += 100 * Math.random() - 50;
                }
                //graph.play(graph.getCodeBase(), "audio/drip.au");
            }
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                break;
            }
        }
    }

    public void paintNode(Graphics g, GNode n, FontMetrics fm)
    {
        int x = (int) n.x;
        int y = (int) n.y;
        switch (n.type)
        {
            case GNode.THRU:
                if (n == pick)
                {
                    g.setColor(pickColor);
                }
                else if (pickAll && (n.group == null || n.group == n))
                {
                    g.setColor(pickColor);
                }
                else if (pickExtended != null && pickExtended.contains(n))
                {
                    g.setColor(pickColor);
                }
                // else if (n == active)
                // g.setColor(activeColor);
                else if (n == select)
                {
                    g.setColor(selectColor);
                }
                else
                {
                    g.setColor(nodeColor);
                }
                g.fillRect(x - n.w / 2, y - n.h / 2, n.w, n.h);
                g.setColor(Color.black);
                g.drawRect(x - n.w / 2, y - n.h / 2, n.w - 1, n.h - 1);
                g.drawString(n.lbl, x - (n.w - 10) / 2, (y - (n.h - 4) / 2) + fm.getAscent());
                if (n.group == null && n.value != null)
                {
                    g.drawString("-toString(): " + n.value, x - (n.w - 10) / 2, (y - (n.h - 4) / 2) + fm.getAscent() * 2 + 4);
                }
                n.xleft = n.x - ((double) n.w) / 2;
                n.xright = n.x + ((double) n.w) / 2;
                n.ytop = n.y - ((double) n.h) / 2;
                n.ybottom = n.y + ((double) n.h) / 2;
                n.q1 = Math.atan2(n.y - n.ytop, n.xright - n.x);
                n.q2 = Math.atan2(n.y - n.ytop, n.xleft - n.x);
                n.q3 = Math.atan2(n.y - n.ybottom, n.xleft - n.x);
                n.q4 = Math.atan2(n.y - n.ybottom, n.xright - n.x);
                break;
        }
    }

    public synchronized void updateGraphics(Graphics g, boolean printing)
    {
        if (!printing)
        {
            Dimension d = panel.getSize();
            if ((offscreen == null) || (d.width != offscreensize.width) || (d.height != offscreensize.height))
            {
                offscreen = createImage(d.width, d.height);
                offscreensize = d;
                offgraphics = offscreen.getGraphics();
                offgraphics.setFont(getFont());
            }

            offgraphics.setColor(getBackground());
            offgraphics.fillRect(0, 0, d.width, d.height);
        }
        else
        {
            offgraphics = g;
            offgraphics.setFont(getFont());
        }
        FontMetrics fm = offgraphics.getFontMetrics();
        for (int i = 0; i < nnodes; i++)
        {
            nodes[i].groupIdx = i;
        }
        for (int i = 0; i < nnodes; i++)
        {
            nodes[i].groupIdx = 0;
            if (nodes[i].group != null
                     && nodes[i].group != nodes[i]
                     && nodes[i].group.groupIdx != 0)
            {
                GNode temp = nodes[i];
                nodes[i] = temp.group;
                nodes[temp.group.groupIdx] = temp;
                temp.group.groupIdx = 0;
            }
        }
        for (int i = 0; i < nnodes; i++)
        {
            GNode n = nodes[i];
            if (n.lbl != null)
            {
                n.w = fm.stringWidth(n.lbl) + 10;
                if (n.group != n)
                {
                    if (n.group != null || n.value == null)
                    {
                        n.h = fm.getHeight() + 4;
                    }
                    else
                    {
                        n.h = fm.getHeight() * 2 + 8;
                        n.w = Math.max(n.w, fm.stringWidth("-toString(): " + n.value) + 10);
                    }
                }
                else
                {
                    n.h = (fm.getHeight() + 9) * (n.groupCnt + 1);
                }
            }
            else
            {
                n.w = 12;
                n.h = 12;
            }
            if (n.group != null && n.group != n)
            {
                n.groupIdx = n.group.groupIdx;
                n.group.groupIdx++;
                n.x = n.group.x;
                n.y = (n.group.y - n.group.h / 2) + n.group.groupIdx * (fm.getHeight() + 9) + 8;
                n.group.w = Math.max(n.group.w, n.w + 12);
            }
        }
        for (int i = 0; i < nnodes; i++)
        {
            paintNode(offgraphics, nodes[i], fm);
        }

        for (int i = 0; i < nedges; i++)
        {
            Edge e = edges[i];
            int x1;
            int y1;
            int x2;
            int y2;
            double eang = Math.atan2(e.from.y - e.to.y, e.to.x - e.from.x);  //Source node
            if (eang <= e.from.q1 && eang > e.from.q4)
            {  //Right
                y1 = ((int) Math.round(e.from.y - Math.tan(eang) * e.from.w / 2d));
                x1 = ((int) Math.round(e.from.x + e.from.w / 2d));
            }
            else if (eang > e.from.q1 && eang <= e.from.q2)
            {  //Top
                y1 = ((int) Math.round(e.from.y - e.from.h / 2d));
                x1 = ((int) Math.round(e.from.x + (e.from.h / 2d) / Math.tan(eang)));
            }
            else if (eang <= e.from.q4 && eang > e.from.q3)
            {  //Bottom
                y1 = ((int) Math.round(e.from.y + e.from.h / 2d));
                x1 = ((int) Math.round(e.from.x - (e.from.h / 2d) / Math.tan(eang)));
            }
            else
            {  //Left
                y1 = ((int) Math.round(e.from.y + Math.tan(eang) * e.from.w / 2d));
                x1 = ((int) Math.round(e.from.x - e.from.w / 2d));
            }
            eang = Math.atan2(e.to.y - e.from.y, e.from.x - e.to.x);  //Target node
            if (eang <= e.to.q1 && eang > e.to.q4)
            {  //Right
                y2 = ((int) Math.round(e.to.y - Math.tan(eang) * e.to.w / 2d));
                x2 = ((int) Math.round(e.to.x + e.to.w / 2d));
            }
            else if (eang > e.to.q1 && eang <= e.to.q2)
            {  //Top
                y2 = ((int) Math.round(e.to.y - e.to.h / 2d));
                x2 = ((int) Math.round(e.to.x + (e.to.h / 2d) / Math.tan(eang)));
            }
            else if (eang <= e.to.q4 && eang > e.to.q3)
            {  //Bottom
                y2 = ((int) Math.round(e.to.y + e.to.h / 2d));
                x2 = ((int) Math.round(e.to.x - (e.to.h / 2d) / Math.tan(eang)));
            }
            else
            {  //Left
                y2 = ((int) Math.round(e.to.y + Math.tan(eang) * e.to.w / 2d));
                x2 = ((int) Math.round(e.to.x - e.to.w / 2d));
            }
            //int len = (int) Math.abs(Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) - e.len);
            offgraphics.setColor(edgeColor);
            offgraphics.drawLine(x1, y1, x2, y2);
            double ang = Math.atan2(-(y1 - y2), (x1 - x2));
            offgraphics.drawLine((int) (x2 + Math.cos(ang + 0.3) * 12),
                    (int) -(-y2 + Math.sin(ang + 0.3) * 12), x2, y2);
            offgraphics.drawLine((int) (x2 + Math.cos(ang - 0.3) * 12),
                    (int) -(-y2 + Math.sin(ang - 0.3) * 12), x2, y2);
            if (e.label != null)
            {
                String lbl = e.label;
                offgraphics.setColor(stressColor);
                offgraphics.drawString(lbl, x1 + (x2 - x1) / 3, y1 + (y2 - y1) / 3);
                offgraphics.setColor(edgeColor);
            }
        }
        if (!printing)
        {
            g.drawImage(offscreen, 0, 0, null);
        }
        else
        {
            offscreen = null;
        }
    }

    public void mouseClicked(MouseEvent e)
    {
        //System.out.println(e.toString());
        if (popped)
        {
            pick = null;
        }
        else if (e.getClickCount() > 1)
        {
            active = select;
            pick = null;
            if (active != null && (active.obj instanceof ObjectReference))
            {
                fireInspectEvent(JdiObject.getDebuggerObject(((ObjectReference) active.obj)));
            }
        }
        else if (!popped)
        {  //if (pick!=null) {
            select = pick;
            pick = null;
        }
        dragged = false;
        popped = false;
        panel.repaint();
    }

    public void mousePressed(MouseEvent e)
    {
        //System.out.println(e.toString());
        int x = e.getX();
        int y = e.getY();
        pick = null;
        for (int i = 0; i < nnodes; i++)
        {
            GNode n = nodes[i];
            if (x >= n.xleft && x <= n.xright && y >= n.ytop && y <= n.ybottom)
            {
                pick = n;
            }
        }
        popped = maybeShowPopup(e);
        dragged = false;
        if (pick != null && !popped)
        {
            extendPick(e);
            panel.addMouseMotionListener(this);
            offsetx = pick.x - x;
            offsety = pick.y - y;
        }
        panel.repaint();
    }

    public void mouseReleased(MouseEvent e)
    {
        //System.out.println(e.toString());
        panel.removeMouseMotionListener(this);
        if (pick != null)
        {
            adjustPos(e);
            if (dragged)
            {
                Dimension current = panel.getSize();
                Dimension newSize = getGraphicsSize();
                if (!current.equals(newSize))
                {
                    panel.setPreferredSize(newSize);
                    panel.revalidate();
                }
            }
        }
        popped = maybeShowPopup(e);
        if (dragged || popped)
        {
            pick = null;
        }
        pickExtended = null;
        pickAll = false;
        panel.repaint();
    }

    public void mouseEntered(MouseEvent e)
    {
        //System.out.println(e.toString());
    }

    public void mouseExited(MouseEvent e)
    {
        //System.out.println(e.toString());
    }

    public void mouseDragged(MouseEvent e)
    {
        //System.out.println(e.toString());
        if (pick != null)
        {
            dragged = true;
        }
        {
            if (pick.group != null
                     && pick.group != pick)
            {
                offsetx -= (pick.x - pick.group.x);
                offsety -= (pick.y - pick.group.y);
                pick = pick.group;
            }
            adjustPos(e);
            panel.repaint();
        }
    }

    public void mouseMoved(MouseEvent e)
    {
        //System.out.println(e.toString());
    }

    public void start()
    {
        relaxer = new Thread(this);
        relaxer.start();
    }

    public void stop()
    {
        relaxer = null;
    }

    synchronized void relax()
    {
        for (int i = 0; i < nedges; i++)
        {
            Edge e = edges[i];
            double vx = e.to.x - e.from.x;
            double vy = e.to.y - e.from.y;
            double len = Math.sqrt(vx * vx + vy * vy);
            len = (len == 0) ? .0001 : len;
            double f = 0;  //(edges[i].len - len) / (len * 3);
            double dx = f * vx;
            double dy = f * vy;

            e.to.dx += dx;
            e.to.dy += dy;
            e.from.dx += -dx;
            e.from.dy += -dy;
        }

        for (int i = 0; i < nnodes; i++)
        {
            GNode n1 = nodes[i];
            double dx = 0;
            double dy = 0;

            for (int j = 0; j < nnodes; j++)
            {
                if (i == j)
                {
                    continue;
                }
                GNode n2 = nodes[j];
                double vx = n1.x - n2.x;
                double vy = n1.y - n2.y;
                double len = vx * vx + vy * vy;
                if (len == 0)
                {
                    dx += Math.random();
                    dy += Math.random();
                }
                else if (len < 100 * 100)
                {
                    dx += vx / len;
                    dy += vy / len;
                }
            }
            double dlen = dx * dx + dy * dy;
            if (dlen > 0)
            {
                dlen = Math.sqrt(dlen) / 2;
                n1.dx += dx / dlen;
                n1.dy += dy / dlen;
            }
        }

        for (int i = 0; i < nedges; i++)
        {
            if (edges[i].from == pick)
            {
                edges[i].to.relaxing = true;
            }
            if (edges[i].to == pick)
            {
                edges[i].from.relaxing = true;
            }
        }

        Dimension d = panel.getSize();
        for (int i = 0; i < nnodes; i++)
        {
            if (nodes[i].relaxing && nodes[i] != pick)
            {
                GNode n = nodes[i];
                if (!n.fixed)
                {
                    n.x += Math.max(-5, Math.min(5, n.dx));
                    n.y += Math.max(-5, Math.min(5, n.dy));
                }
                if (n.x < 0)
                {
                    n.x = 0;
                }
                else if (n.x > d.width)
                {
                    n.x = d.width;
                }
                if (n.y < 0)
                {
                    n.y = 0;
                }
                else if (n.y > d.height)
                {
                    n.y = d.height;
                }
                n.dx /= 2;
                n.dy /= 2;
            }
        }
        panel.repaint();
    }

    private Value getPrimitiveValue(VirtualMachine vm)
    {
        return getPrimitiveValue(vm, primitiveType.getSelectedIndex(), primitiveText.getText());
    }

    private Value getPrimitiveValue(VirtualMachine vm, int typeIdx, String primitiveString)
    {
        try
        {
            switch (typeIdx)
            {
                case 0:
                    //          BooleanValue
                    return vm.mirrorOf(Boolean.valueOf(primitiveString).booleanValue());
                case 1:
                    //           ByteValue
                    return vm.mirrorOf(Byte.valueOf(primitiveString).byteValue());
                case 2:
                    //           CharValue
                    return vm.mirrorOf(new Character(primitiveString.charAt(0)).charValue());
                case 3:
                    //         DoubleValue
                    return vm.mirrorOf(Double.valueOf(primitiveString).doubleValue());
                case 4:
                    //          FloatValue
                    return vm.mirrorOf(Float.valueOf(primitiveString).floatValue());
                case 5:
                    //        IntegerValue
                    return vm.mirrorOf(Integer.parseInt(primitiveString));
                case 6:
                    //           LongValue
                    return vm.mirrorOf(Long.parseLong(primitiveString));
                case 7:
                    //          ShortValue
                    return vm.mirrorOf(Short.parseShort(primitiveString));
                case 8:
                    //      StringReference
                    return vm.mirrorOf(primitiveString);
                case 9:
                    //      NullReference
                    return null;
            }
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(getJFrame(),
                    datatypeErrorLabel + " " + ptypes[typeIdx] + ":\n" + e,
                    errorLabel, JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private Value getSelectedObject(String kicker)
    {
        if (select != null && select.obj != null)
        {
            return select.obj;
        }
        else
        {
            JOptionPane.showMessageDialog(getJFrame(),
                    noSelectionErrorLabel + ":\n" + kicker, errorLabel, JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void getObj(DebuggerObject obj)
    {
        int objGotIdx = objGotCnt;
        objGotCnt++;
        if (objGotCnt >= objGot.length)
        {
            DebuggerObject[] temp = new DebuggerObject[objGot.length * 2];
            System.arraycopy(objGot, 0, temp, 0, objGot.length);
            objGot = temp;
        }
        objGot[objGotIdx] = obj;
        refresh();
        panel.repaint();
    }

    private Dimension getGraphicsSize()
    {
        int maxX = 0;
        int maxY = 0;
        for (int i = 0; i < nnodes; i++)
        {
            maxX = (int) Math.max(nodes[i].xright, maxX);
            maxY = (int) Math.max(nodes[i].ybottom, maxY);
        }
        return new Dimension(maxX + 8, maxY + 8);
    }

    private void saveFlowChart(FileOutputStream fos)
        throws IOException
    {
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        GNode saveNodes[] = new GNode[nnodes];
        System.arraycopy(nodes, 0, saveNodes, 0, nnodes);
        Edge saveEdges[] = new Edge[nedges];
        System.arraycopy(edges, 0, saveEdges, 0, nedges);
        oos.writeObject(saveNodes);
        oos.writeObject(saveEdges);
    }

    private void loadFlowChart(FileInputStream fis)
        throws IOException, ClassNotFoundException
    {
        ObjectInputStream ois = new ObjectInputStream(fis);
        nodes = (GNode[]) ois.readObject();
        nnodes = nodes.length;
        edges = (Edge[]) ois.readObject();
        nedges = edges.length;
        offscreen = null;
        offscreensize = null;
        offgraphics = null;
    }

    private void adjustPos(MouseEvent e)
    {
        double deltax = Math.max(offsetx, e.getX() + offsetx) - pick.x;
        double deltay = Math.max(offsety, e.getY() + offsety) - pick.y;
        extendPick(e);
        if (pickExtended != null)
        {
            for (Iterator i = pickExtended.iterator(); i.hasNext(); )
            {
                GNode theNode = ((GNode) i.next());
                theNode.x += deltax;
                theNode.y += deltay;
            }
        }
        else if (pickAll)
        {
            for (int i = 0; i < nnodes; i++)
            {
                GNode theNode = nodes[i];
                theNode.x += deltax;
                theNode.y += deltay;
            }
        }
        else
        {
            pick.x += deltax;
            pick.y += deltay;
        }
    }

    private void extendPick(MouseEvent e)
    {
        if (e.isShiftDown() || e.isControlDown())
        {
            pickExtended = new HashSet();
            pickExtended.add(pick);
            for (int i = 0; i < nedges; i++)
            {
                if (e.isShiftDown())
                {
                    if (edges[i].from == pick || edges[i].from.group == pick)
                    {
                        pickExtended.add(edges[i].to);
                    }
                }
                if (e.isControlDown())
                {
                    if (edges[i].to == pick)
                    {
                        if (edges[i].from.group == null)
                        {
                            pickExtended.add(edges[i].from);
                        }
                        else
                        {
                            pickExtended.add(edges[i].from.group);
                        }
                    }
                }
            }
            if (e.isAltDown())
            {
                java.util.Set nextConnected = new HashSet(pickExtended);
                for (int i = 0; i < nedges; i++)
                {
                    if (e.isShiftDown())
                    {
                        if (pickExtended.contains(edges[i].from) || pickExtended.contains(edges[i].from.group))
                        {
                            nextConnected.add(edges[i].to);
                        }
                    }
                    if (e.isControlDown())
                    {
                        if (pickExtended.contains(edges[i].to))
                        {
                            if (edges[i].from.group == null)
                            {
                                nextConnected.add(edges[i].from);
                            }
                            else
                            {
                                nextConnected.add(edges[i].from.group);
                            }
                        }
                    }
                }
                pickExtended = nextConnected;
                pickAll = false;
            }
        }
        else if (e.isAltDown())
        {
            pickExtended = null;
            pickAll = true;
        }
        else
        {
            pickExtended = null;
            pickAll = false;
        }
    }

    private void allocateNodes()
    {
        GNode tempNodes[] = new GNode[nodes.length * 2];
        System.arraycopy(nodes, 0, tempNodes, 0, nodes.length);
        nodes = tempNodes;
    }

    private void allocateEdges()
    {
        Edge tempEdges[] = new Edge[edges.length * 2];
        System.arraycopy(edges, 0, tempEdges, 0, edges.length);
        edges = tempEdges;
    }

    private GNode findGNode(ObjectReference obj)
    {
        for (int j = 0; j < nnodes; j++)
        {
            GNode theNode = nodes[j];
            if (obj != null && theNode.obj != null && theNode.obj.equals(obj)
                     && (theNode.group == null || theNode.group == theNode))
            {
                return theNode;
            }
        }
        return null;
    }

    private void createPullDown(final GNode root, java.util.List methodList)
    {
        createPullDown(root, false, -1, methodList, null);
    }

    private void createPullDown(final GNode root, boolean assignmentMenu, java.util.List methodList)
    {
        createPullDown(root, assignmentMenu, -1, methodList, null);
    }

    private JMenu createAssignMenu(final int idx, final Field theField, final GNode root)
    {
        JMenu assignMenu;
        if (theField != null)
        {
            assignMenu = new JMenu(theField.name() + " =");
        }
        else
        {
            assignMenu = new JMenu("[" + idx + "] =");
        }
        assignMenu.addMenuListener(
            new MenuListener()
            {
                public void
                        menuCanceled(MenuEvent e)
                {
                }

                public void
                        menuDeselected(MenuEvent e)
                {
                }

                public void
                        menuSelected(MenuEvent e)
                {
                    JMenu theParameterMenu = ((JMenu) e.getSource());
                    for (int i = 0; i < theParameterMenu.getItemCount(); i++)
                    {
                        JMenuItem theItem = theParameterMenu.getItem(i);
                        String itemText = theItem.getText();
                        if (itemText.endsWith(selectedLabel))
                        {
                            if (select != null)
                            {
                                theItem.setText("" + select.obj + "; //" + selectedLabel);
                                theItem.setEnabled(true);
                            }
                            else
                            {
                                theItem.setText(selectedLabel);
                                theItem.setEnabled(false);
                            }
                        }
                        else if (itemText.endsWith(pasteLabel))
                        {
                            theItem.setText(copiedObjectReference + "; //" + pasteLabel);
                        }
                        else if (itemText.endsWith(primitiveLabel))
                        {
                            theItem.setText("(" + primitiveType.getSelectedItem() + ") " + primitiveText.getText() + "; //" + primitiveLabel);
                        }
                    }
                }
            }
                );
        JMenuItem assignItem = new JMenuItem(selectedLabel);
        assignItem.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    active = root;
                    Value theValue = getSelectedObject(noAssignmentErrorLabel + "!");
                    if (theValue != null)
                    {
                        try
                        {
                            if (active.obj instanceof ArrayReference)
                            {
                                ((ArrayReference) active.obj).setValue(idx, theValue);
                            }
                            else
                            {
                                ((ObjectReference) active.obj).setValue(theField, theValue);
                            }
                        }
                        catch (Exception ex)
                        {
                            JOptionPane.showMessageDialog(getJFrame(), noAssignmentErrorLabel + ":\n" + ex,
                                    errorLabel, JOptionPane.ERROR_MESSAGE);
                        }
                        bluej.debugger.ObjectViewer.updateViewers();
                    }
                }
            }
                );
        assignMenu.add(assignItem);
        assignItem = new JMenuItem(pasteLabel);
        assignItem.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    active = root;
                    Value theValue = copiedObjectReference;
                    try
                    {
                        if (active.obj instanceof ArrayReference)
                        {
                            ((ArrayReference) active.obj).setValue(idx, theValue);
                        }
                        else
                        {
                            ((ObjectReference) active.obj).setValue(theField, theValue);
                        }
                    }
                    catch (Exception ex)
                    {
                        JOptionPane.showMessageDialog(getJFrame(), noAssignmentErrorLabel + ":\n" + ex,
                                errorLabel, JOptionPane.ERROR_MESSAGE);
                    }
                    bluej.debugger.ObjectViewer.updateViewers();
                }
            }
                );
        assignMenu.add(assignItem);

        assignItem = new JMenuItem(primitiveLabel);
        assignItem.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    active = root;
                    Value theValue = getPrimitiveValue(active.obj.virtualMachine());
                    if (theValue != null)
                    {
                        try
                        {
                            if (active.obj instanceof ArrayReference)
                            {
                                ((ArrayReference) active.obj).setValue(idx, theValue);
                            }
                            else
                            {
                                ((ObjectReference) active.obj).setValue(theField, theValue);
                            }
                        }
                        catch (Exception ex)
                        {
                            JOptionPane.showMessageDialog(getJFrame(), noAssignmentErrorLabel + ":\n" + ex,
                                    errorLabel, JOptionPane.ERROR_MESSAGE);
                        }
                        bluej.debugger.ObjectViewer.updateViewers();
                    }
                }
            }
                );
        assignMenu.add(assignItem);

        assignItem = new JMenuItem(dialogLabel + "...");
        assignItem.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    active = root;
                    new AssignDialog(getJFrame(), root, idx, theField).show();
                    panel.repaint();
                }
            }
                );
        assignMenu.add(assignItem);
        return assignMenu;
    }

    private void addMethod(JMenu theMenu, final Method theMethod, final GNode root)
    {
        String[] atn = ((String[]) theMethod.argumentTypeNames().toArray(new String[0]));
        if (atn.length > 0)
        {
            ActionListener methodActionListener =
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        active = root;
                        new MethodDialog(getJFrame(), active, theMethod).show();
                        panel.repaint();
                    }
                };
            if (atn.length > 1)
            {
                JMenuItem methodDialogItem = new JMenuItem(theMethod.name() + "...");
                String tt = "(";
                tt += atn[0];
                for (int j = 1; j < atn.length; j++)
                {
                    tt += ", " + atn[j];
                }
                tt += ")";
                methodDialogItem.setToolTipText(tt);
                methodDialogItem.addActionListener(methodActionListener);
                theMenu.add(methodDialogItem);
            }
            else
            {
                JMenu parameterMenu = new JMenu(theMethod.name() + " (");
                parameterMenu.setToolTipText("(" + atn[0] + ")");
                parameterMenu.addMenuListener(
                    new MenuListener()
                    {
                        public void
                                menuCanceled(MenuEvent e)
                        {
                        }

                        public void
                                menuDeselected(MenuEvent e)
                        {
                        }

                        public void
                                menuSelected(MenuEvent e)
                        {
                            JMenu theParameterMenu = ((JMenu) e.getSource());
                            for (int i = 0; i < theParameterMenu.getItemCount(); i++)
                            {
                                JMenuItem theItem = theParameterMenu.getItem(i);
                                String itemText = theItem.getText();
                                if (itemText.endsWith(selectedLabel))
                                {
                                    if (select != null)
                                    {
                                        theItem.setText("" + select.obj + "); //" + selectedLabel);
                                        theItem.setEnabled(true);
                                    }
                                    else
                                    {
                                        theItem.setText(selectedLabel);
                                        theItem.setEnabled(false);
                                    }
                                }
                                else if (itemText.endsWith(pasteLabel))
                                {
                                    theItem.setText(copiedObjectReference + "); //" + pasteLabel);
                                }
                                else if (itemText.endsWith(primitiveLabel))
                                {
                                    theItem.setText("(" + primitiveType.getSelectedItem() + ") " + primitiveText.getText() + "); //" + primitiveLabel);
                                }
                            }
                        }
                    }
                        );
                JMenuItem parameterItem = new JMenuItem(selectedLabel);
                parameterItem.addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            active = root;
                            Value theParm = getSelectedObject(noInvokeErrorLabel);
                            if (theParm != null)
                            {
                                java.util.List parm = new ArrayList();
                                parm.add(theParm);
                                invoke(((ObjectReference) active.obj), theMethod, parm);
                            }
                        }
                    }
                        );
                parameterMenu.add(parameterItem);
                parameterItem = new JMenuItem(pasteLabel);
                parameterItem.addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            active = root;
                            java.util.List parm = new ArrayList();
                            parm.add(copiedObjectReference);
                            invoke(((ObjectReference) active.obj), theMethod, parm);
                        }
                    }
                        );
                parameterMenu.add(parameterItem);
                parameterItem = new JMenuItem(primitiveLabel);
                parameterItem.addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            active = root;
                            java.util.List parm = new ArrayList();
                            parm.add(getPrimitiveValue(((ObjectReference) active.obj).virtualMachine()));
                            invoke(((ObjectReference) active.obj), theMethod, parm);
                        }
                    }
                        );
                parameterMenu.add(parameterItem);
                parameterItem = new JMenuItem(dialogLabel + "...");
                parameterItem.addActionListener(methodActionListener);
                parameterMenu.add(parameterItem);
                theMenu.add(parameterMenu);
            }
        }
        else
        {
            ActionListener methodActionListener =
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        active = root;
                        invoke(((ObjectReference) active.obj), theMethod, new ArrayList());
                    }
                };
            JMenuItem methodInvokeItem = new JMenuItem(theMethod.name() + "()");
            methodInvokeItem.addActionListener(methodActionListener);
            theMenu.add(methodInvokeItem);
        }
    }

    private void createPullDown(final GNode root, boolean assignmentMenu, int assignmentIdx,
            java.util.List methodList, Field theField)
    {
        root.popup = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem(copyLabel);
        copyItem.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    //System.out.println("copyItem.addActionListener: "+root.obj);
                    Value theObj = root.obj;
                    if (theObj != null)
                    {
                        copiedObjectReference = theObj;
                    }
                    // else
                    // JOptionPane.showMessageDialog(getJFrame(),
                    // "Selected node is not an object:\nReference not copied!", errorLabel, JOptionPane.ERROR_MESSAGE);
                }
            }
                );
        root.popup.add(copyItem);
        if (assignmentMenu)
        {
            if (assignmentIdx < 0)
            {
                JMenu aMenu = new JMenu(assignLabel + "...");
                if (root.obj instanceof ArrayReference)
                {
                    for (int j = 0; j < Math.min(MAX_IDX, ((ArrayReference) root.obj).length()); j++)
                    {
                        aMenu.add(createAssignMenu(j, null, root));
                    }
                }
                else
                {
                    Field[] field = ((Field[]) ((ObjectReference) root.obj).
                            referenceType().visibleFields().toArray(new Field[0]));
                    for (int j = 0; j < field.length; j++)
                    {
                        aMenu.add(createAssignMenu(0, field[j], root));
                    }
                }
                root.popup.add(aMenu);
            }
            else
            {
                JMenu assignEltMenu;
                if (root.group.obj instanceof ArrayReference)
                {
                    assignEltMenu = createAssignMenu(assignmentIdx, null, root.group);
                }
                else
                {
                    assignEltMenu = createAssignMenu(assignmentIdx, theField, root.group);
                }
                assignEltMenu.setText(assignLabel + " " + assignEltMenu.getText());
                root.popup.add(assignEltMenu);
            }
        }
        if (methodList != null)
        {
            JMenu methodMenu = new JMenu(invokeMethodLabel);
            Method[] method = ((Method[]) methodList.toArray(new Method[0]));
            int i = 0;
            int mthdCnt = 0;
            boolean justAddedSeparator = true;
            for (i = 0; mthdCnt < 15 && i < method.length; i++)
            {
                if (!method[i].isConstructor() && !method[i].isStaticInitializer())
                {
                    addMethod(methodMenu, method[i], root);
                    mthdCnt++;
                    justAddedSeparator = false;
                }
                else if (!justAddedSeparator && i + 1 < method.length)
                {
                    methodMenu.addSeparator();
                    justAddedSeparator = true;
                }
            }
            JMenu pjMenu = null;
            if (i < method.length)
            {
                pjMenu = new JMenu(moreLabel);
                for (boolean first = true; ((mthdCnt % 15) > 0 || first) && i < method.length; first = false, i++)
                {
                    if (!method[i].isConstructor() && !method[i].isStaticInitializer())
                    {
                        addMethod(pjMenu, method[i], root);
                        mthdCnt++;
                        justAddedSeparator = false;
                    }
                    else if (!justAddedSeparator && i + 1 < method.length)
                    {
                        pjMenu.addSeparator();
                        justAddedSeparator = true;
                    }
                }
                methodMenu.add(pjMenu);
            }
            while (i < method.length)
            {
                JMenu lastMenu = pjMenu;
                pjMenu = new JMenu(moreLabel);
                for (boolean first = true; ((mthdCnt % 15) > 0 || first) && i < method.length; first = false, i++)
                {
                    if (!method[i].isConstructor() && !method[i].isStaticInitializer())
                    {
                        addMethod(pjMenu, method[i], root);
                        mthdCnt++;
                        justAddedSeparator = false;
                    }
                    else if (!justAddedSeparator && i + 1 < method.length)
                    {
                        pjMenu.addSeparator();
                        justAddedSeparator = true;
                    }
                }
                lastMenu.add(pjMenu);
            }
            root.popup.add(methodMenu);
        }
    }

    private void extend(GNode root)
    {
        // Handle Array, List, Map, and Set in a custom way!
        ReferenceType rt = ((ObjectReference) root.obj).referenceType();
        if (root.obj instanceof ArrayReference)
        {
            if (true || root.expand == GNode.TRUE)
            {
                root.group = root;
                root.groupCnt = 0;
                Value[] valuearray;
                if (((ArrayReference) root.obj).length() == 0)
                {
                    valuearray = new Value[0];
                }
                else
                {
                    valuearray = ((Value[]) ((ArrayReference) root.obj).
                            getValues().toArray(new Value[0]));
                }
                int i;
                for (i = 0; i < Math.min(MAX_IDX, valuearray.length); i++)
                {
                    root.groupCnt++;
                    GNode entry;
                    if (valuearray[i] instanceof ObjectReference
                             && !valuearray[i].type().name().equals("java.lang.String"))
                    {
                        ObjectReference next = ((ObjectReference) valuearray[i]);
                        GNode leaf = findGNode(next);
                        if (leaf == null)
                        {
                            leaf = addNode(next);
                            extend(leaf);
                        }
                        else if (leaf.lbl == null)
                        {
                            leaf.setLbl();
                            extend(leaf);
                        }
                        entry = addNode("[" + i + "]");
                        addEdge(entry, leaf);
                        entry.group = root;
                        createPullDown(entry, true, i, null, null);
                    }
                    else if ((valuearray[i] instanceof ObjectReference)
                             && valuearray[i].type().name().equals("java.lang.String"))
                    {
                        entry = addNode(valuearray[i], "[" + i + "]" + ": " + valuearray[i].toString());
                        entry.group = root;
                        createPullDown(entry, true, i, ((ObjectReference) entry.obj).referenceType().visibleMethods(), null);
                    }
                    else if (valuearray[i] instanceof PrimitiveValue)
                    {
                        entry = addNode(valuearray[i], "[" + i + "]" + ": " + valuearray[i].toString());
                        entry.group = root;
                        createPullDown(entry, true, i, null, null);
                    }
                    else
                    {
                        entry = addNode(valuearray[i], "[" + i + "]" + ": " + null);
                        entry.group = root;
                    }
                }
                if (i < valuearray.length)
                {
                    root.groupCnt++;
                    addNode("[" + i + ".." + (valuearray.length - 1) + "] elided").group = root;
                }
            }
            createPullDown(root, true, rt.visibleMethods());
        }
        else if (JdiObject.getDebuggerObject(((ObjectReference) root.obj)).isAssignableTo("java.util.Stack"))
        {
            if (true || root.expand == GNode.TRUE)
            {
                root.group = root;
                root.groupCnt = 0;
                ArrayReference theList = ((ArrayReference) JdiObject.getDebuggerObject(((ObjectReference) root.obj)).invokeMethod(
                        "toArray", "()[Ljava/lang/Object;", new java.util.Vector()));
                ObjectReference[] listarray;
                if (theList.length() == 0)
                {
                    listarray = new ObjectReference[0];
                }
                else
                {
                    listarray = ((ObjectReference[]) theList.getValues().toArray(new ObjectReference[0]));
                }
                for (int i = 0; i < listarray.length; i++)
                {
                    root.groupCnt++;
                    ObjectReference next = listarray[listarray.length - i - 1];  //Stack, so reverse
                    GNode entry;
                    if (next != null && !next.referenceType().name().equals("java.lang.String"))
                    {
                        GNode leaf = findGNode(next);
                        if (leaf == null)
                        {
                            leaf = addNode(next);
                            extend(leaf);
                        }
                        else if (leaf.lbl == null)
                        {
                            leaf.setLbl();
                            extend(leaf);
                        }
                        else
                        {
                            //addEdge(root,leaf).label=field[i].name();
                        }
                        entry = addNode((i == 0) ? topLabel : (i < listarray.length - 1) ? nextLabel : bottomLabel);
                        addEdge(entry, leaf);
                    }
                    else if (next != null)
                    {
                        entry = addNode(next, ((i == 0) ? topLabel + ": " : (i < listarray.length - 1) ? nextLabel + ": " : bottomLabel + ": ") + next.toString());
                        createPullDown(entry, ((ObjectReference) entry.obj).referenceType().visibleMethods());
                    }
                    else
                    {
                        entry = addNode(next, ((i == 0) ? topLabel + ": " : (i < listarray.length - 1) ? nextLabel + ": " : bottomLabel + ": ") + "null");
                    }
                    entry.group = root;
                }
            }
            createPullDown(root, rt.visibleMethods());
        }
        else if (JdiObject.getDebuggerObject(((ObjectReference) root.obj)).isAssignableTo("java.util.List"))
        {
            if (true || root.expand == GNode.TRUE)
            {
                root.group = root;
                root.groupCnt = 0;
                ArrayReference theList = ((ArrayReference) JdiObject.getDebuggerObject(((ObjectReference) root.obj)).invokeMethod(
                        "toArray", "()[Ljava/lang/Object;", new java.util.Vector()));
                ObjectReference[] listarray;
                if (theList.length() == 0)
                {
                    listarray = new ObjectReference[0];
                }
                else
                {
                    listarray = ((ObjectReference[]) theList.getValues().toArray(new ObjectReference[0]));
                }
                for (int i = 0; i < listarray.length; i++)
                {
                    GNode entry;
                    root.groupCnt++;
                    ObjectReference next = listarray[i];
                    if (next != null && !next.referenceType().name().equals("java.lang.String"))
                    {
                        GNode leaf = findGNode(next);
                        if (leaf == null)
                        {
                            leaf = addNode(next);
                            extend(leaf);
                        }
                        else if (leaf.lbl == null)
                        {
                            leaf.setLbl();
                            extend(leaf);
                        }
                        else
                        {
                            //addEdge(root,leaf).label=field[i].name();
                        }
                        entry = addNode("Entry " + i);
                        addEdge(entry, leaf);
                    }
                    else
                    {
                        if (next != null)
                        {
                            entry = addNode(next, entryLabel + " " + i + ": " + next.toString());
                            createPullDown(entry, ((ObjectReference) entry.obj).referenceType().visibleMethods());
                        }
                        else
                        {
                            entry = addNode(next, entryLabel + " " + i + ": " + "null");
                        }
                    }
                    entry.group = root;
                }
            }
            createPullDown(root, rt.visibleMethods());
        }
        else if (JdiObject.getDebuggerObject(((ObjectReference) root.obj)).isAssignableTo("java.util.Set"))
        {
            if (true || root.expand == GNode.TRUE)
            {
                root.group = root;
                root.groupCnt = 0;
                ArrayReference theList = ((ArrayReference) JdiObject.getDebuggerObject(((ObjectReference) root.obj)).invokeMethod(
                        "toArray", "()[Ljava/lang/Object;", new java.util.Vector()));
                ObjectReference[] listarray;
                if (theList.length() == 0)
                {
                    listarray = new ObjectReference[0];
                }
                else
                {
                    listarray = ((ObjectReference[]) theList.getValues().toArray(new ObjectReference[0]));
                }
                for (int i = 0; i < listarray.length; i++)
                {
                    root.groupCnt++;
                    ObjectReference next = listarray[i];
                    GNode entry;
                    if (next != null && !next.referenceType().name().equals("java.lang.String"))
                    {
                        GNode leaf = findGNode(next);
                        if (leaf == null)
                        {
                            leaf = addNode(next);
                            extend(leaf);
                        }
                        else if (leaf.lbl == null)
                        {
                            leaf.setLbl();
                            extend(leaf);
                        }
                        else
                        {
                            //addEdge(root,leaf).label=field[i].name();
                        }
                        entry = addNode(elementLabel + " " + i);
                        addEdge(entry, leaf);
                    }
                    else if (next != null)
                    {
                        entry = addNode(next, elementLabel + " " + i + ": " + next.toString());
                        createPullDown(entry, ((ObjectReference) entry.obj).referenceType().visibleMethods());
                    }
                    else
                    {
                        entry = addNode(elementLabel + " " + i + ": " + "null");
                    }
                    entry.group = root;
                }
            }
            createPullDown(root, rt.visibleMethods());
        }
        else if (JdiObject.getDebuggerObject(((ObjectReference) root.obj)).isAssignableTo("java.util.Map"))
        {
            if (true || root.expand == GNode.TRUE)
            {
                root.group = root;
                root.groupCnt = 0;
                ObjectReference set = ((ObjectReference) JdiObject.getDebuggerObject(
                        ((ObjectReference) root.obj)).invokeMethod(
                        "entrySet", "()Ljava/util/Set;", new java.util.Vector()));
                ArrayReference theList = ((ArrayReference) JdiObject.getDebuggerObject(set).invokeMethod(
                        "toArray", "()[Ljava/lang/Object;", new java.util.Vector()));
                ObjectReference[] setarray;
                if (theList.length() == 0)
                {
                    setarray = new ObjectReference[0];
                }
                else
                {
                    setarray = ((ObjectReference[]) theList.getValues().toArray(new ObjectReference[0]));
                }
                for (int i = 0; i < setarray.length; i++)
                {
                    ObjectReference leaf = setarray[i];
                    root.groupCnt++;
                    ObjectReference key = ((ObjectReference) JdiObject.getDebuggerObject(leaf).invokeMethod(
                            "getKey", "()Ljava/lang/Object;", new java.util.Vector()));
                    GNode keyleaf;
                    GNode entry;
                    if (!key.referenceType().name().equals("java.lang.String"))
                    {
                        keyleaf = findGNode(key);
                        if (keyleaf == null || keyleaf.lbl == null)
                        {
                            if (keyleaf == null)
                            {
                                keyleaf = addNode(key);
                            }
                            else
                            {
                                keyleaf.setLbl();
                            }
                            extend(keyleaf);
                        }
                        entry = addNode(entryLabel);
                        addEdge(entry, keyleaf).label = keyLabel;
                        //extend(keyleaf);
                    }
                    else
                    {
                        entry = addNode(key, keyLabel + ": " + key.toString());
                        createPullDown(entry, ((ObjectReference) entry.obj).referenceType().visibleMethods());
                    }
                    ObjectReference value = ((ObjectReference) JdiObject.getDebuggerObject(leaf).invokeMethod(
                            "getValue", "()Ljava/lang/Object;", new java.util.Vector()));
                    if (value != null)
                    {
                        GNode valueleaf = findGNode(value);
                        if (valueleaf == null || valueleaf.lbl == null)
                        {
                            if (valueleaf == null)
                            {
                                valueleaf = addNode(value);
                            }
                            else
                            {
                                valueleaf.setLbl();
                            }
                            extend(valueleaf);
                        }
                        addEdge(entry, valueleaf).label = valueLabel;
                        entry.group = root;
                    }
                    else
                    {
                        entry.lbl += ", " + valueLabel + ": null";
                    }
                }
            }
            createPullDown(root, rt.visibleMethods());
        }
        else if ((rt.name().startsWith("com.sun.")
                 || rt.name().startsWith("java.")
                 || rt.name().startsWith("javax.")))
        {  // System classes
            if (root.expand == GNode.TRUE)
            {
            }
            createPullDown(root, rt.visibleMethods());
        }
        else if (rt instanceof ClassType)
        {
            if (root.expand == GNode.TRUE || root.expand == GNode.DEFAULT)
            {
                root.groupCnt = 0;
                Field[] field = ((Field[]) ((ObjectReference) root.obj).
                        referenceType().visibleFields().toArray(new Field[0]));
                for (int i = 0; i < field.length; i++)
                {
                    Value val = ((ObjectReference) root.obj).getValue(field[i]);
                    if (val instanceof ObjectReference
                             && !val.type().name().equals("java.lang.String"))
                    {
                        ObjectReference next = ((ObjectReference) val);
                        if (next != null)
                        {
                            GNode leaf = findGNode(next);
                            if (leaf == null)
                            {
                                leaf = addNode(next);
                                addEdge(root, leaf).label = field[i].name();
                                extend(leaf);
                            }
                            else if (leaf.lbl == null)
                            {
                                leaf.setLbl();
                                addEdge(root, leaf).label = field[i].name();
                                extend(leaf);
                            }
                            else
                            {
                                addEdge(root, leaf).label = field[i].name();
                            }
                        }
                    }
                    else if ((val instanceof ObjectReference)
                             && val.type().name().equals("java.lang.String"))
                    {
                        GNode entry = addNode(val, field[i].name() + ": " + val.toString());
                        entry.group = root;
                        root.groupCnt++;
                        root.group = root;
                        createPullDown(entry, true, i, ((ObjectReference) entry.obj).referenceType().visibleMethods(), field[i]);
                    }
                    else if (val instanceof PrimitiveValue)
                    {
                        GNode theGNode = addNode(val, field[i].name() + ": " + val.toString());
                        theGNode.group = root;
                        root.groupCnt++;
                        root.group = root;
                        createPullDown(theGNode, true, i, null, field[i]);
                    }
                    else
                    {
                        GNode entry = addNode(val, field[i].name() + ": " + "null");
                        entry.group = root;
                        root.groupCnt++;
                    }
                }
            }
            createPullDown(root, true, rt.visibleMethods());
        }
    }

    private boolean maybeShowPopup(final MouseEvent e)
    {
        if (e.isPopupTrigger())
        {
            active = pick;
            if (active != null && active.popup != null)
            {
                active.popup.show(e.getComponent(),
                        e.getX(), e.getY());
                return true;
            }
            else if (active == null)
            {
                JPopupMenu backgroundPopup = new JPopupMenu();
                if (select != null && select.obj != null)
                {
                    JMenuItem copyMenuItem = new JMenuItem(copyLabel + ": " + select.obj);
                    copyMenuItem.addActionListener(
                        new ActionListener()
                        {
                            public void actionPerformed(ActionEvent evt)
                            {
                                if (select != null)
                                {
                                    Value theObj = select.obj;
                                    if (theObj != null)
                                    {
                                        copiedObjectReference = theObj;
                                    }
                                }
                            }
                        }
                            );
                    backgroundPopup.add(copyMenuItem);
                }
                JMenuItem pasteMenuItem = new JMenuItem(pasteLabel + ": " + copiedObjectReference);
                pasteMenuItem.addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent evt)
                        {
                            if (copiedObjectReference != null)
                            {
                                if (copiedObjectReference instanceof ObjectReference)
                                {
                                    GNode pasted = findGNode((ObjectReference) copiedObjectReference);
                                    if (pasted == null)
                                    {
                                        pasted = addNode((ObjectReference) copiedObjectReference);
                                        pasted.x = e.getX();
                                        pasted.y = e.getY();
                                        getObj(JdiObject.getDebuggerObject(((ObjectReference) copiedObjectReference)));
                                    }
                                    else
                                    {
                                        JOptionPane.showMessageDialog(getJFrame(),
                                                alreadyShownLabel,
                                                errorLabel, JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(getJFrame(),
                                            cannotPastePrimLabel,
                                            errorLabel, JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    }
                        );
                backgroundPopup.add(pasteMenuItem);
                backgroundPopup.show(e.getComponent(),
                        e.getX(), e.getY());
                return true;
            }
        }
        return false;
    }

    private void invoke(ObjectReference or, Method mthd, java.util.List arg)
    {
        Object rtrn = JdiObject.getDebuggerObject(or).invokeMethod(
                mthd.name(), mthd.signature(), arg);
        bluej.debugger.ObjectViewer.updateViewers();
        if (rtrn != null && rtrn instanceof ObjectReference)
        {
            fireInspectEvent(JdiObject.getDebuggerObject(((ObjectReference) rtrn)));
        }
        else if (rtrn != null && rtrn instanceof String)
        {
            JOptionPane.showMessageDialog(getJFrame(), noInvokeErrorLabel + ":\n" + rtrn,
                    errorLabel, JOptionPane.ERROR_MESSAGE);
        }
        else if (rtrn == null)
        {
            JOptionPane.showMessageDialog(getJFrame(), "Null " + referenceLabel,
                    methodResultLabel + ":", JOptionPane.INFORMATION_MESSAGE);
        }
        else if (rtrn != null && !(rtrn instanceof VoidValue))
        {
            JOptionPane.showMessageDialog(getJFrame(), ((Value) rtrn).type().toString() + ": " + rtrn.toString(),
                    methodResultLabel + ":", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static int sqr(int x)
    {
        return x * x;
    }

    class MethodDialog extends JDialog
    {
        JTextField[] argLabel;
        Value[] arg;
        GNode active;
        Method theMethod;
        String[] atn;

        MethodDialog(JFrame parent, GNode activeParm, Method theMethodParm)
        {
            super(parent, methodDialogLabel, false);
            active = activeParm;
            theMethod = theMethodParm;
            atn = ((String[]) theMethod.argumentTypeNames().toArray(new String[0]));
            JPanel lines = new JPanel(null);
            lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
            lines.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JPanel line1 = new JPanel(null);
            line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
            line1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JLabel objLabel = new JLabel(((ObjectReference) active.obj).toString());
            line1.add(objLabel);
            line1.add(new JLabel("." + theMethod.name() + " ("));
            lines.add(line1);
            argLabel = new JTextField[atn.length];
            arg = new Value[atn.length];
            for (int i = 0; i < atn.length; i++)
            {
                arg[i] = null;
                JPanel linen = new JPanel(null);
                linen.setLayout(new BoxLayout(linen, BoxLayout.X_AXIS));
                linen.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                linen.add(Box.createHorizontalStrut(40));
                argLabel[i] = new JTextField("null");
                argLabel[i].setToolTipText(parmToolTipLabel + " " + (i + 1));
                argLabel[i].setEditable(false);
                final int idx = i;
                argLabel[i].addMouseListener(
                    new MouseAdapter()
                    {
                        public void mouseClicked(MouseEvent e)
                        {
                            JPopupMenu popup = new JPopupMenu();
                            if (select != null)
                            {
                                JMenuItem copyMenuItem = new JMenuItem(selectedLabel + ": " + select.obj);
                                copyMenuItem.addActionListener(
                                    new ActionListener()
                                    {
                                        public void actionPerformed(ActionEvent ae)
                                        {
                                            arg[idx] = select.obj;
                                            argLabel[idx].setText("" + arg[idx]);
                                            pack();
                                        }
                                    });
                                popup.add(copyMenuItem);
                            }
                            JMenuItem pasteMenuItem = new JMenuItem(pasteLabel + ": " + copiedObjectReference);
                            pasteMenuItem.addActionListener(
                                new ActionListener()
                                {
                                    public void actionPerformed(ActionEvent ae)
                                    {
                                        arg[idx] = copiedObjectReference;
                                        argLabel[idx].setText("" + arg[idx]);
                                        pack();
                                    }
                                });
                            popup.add(pasteMenuItem);
                            JMenuItem primitiveMenuItem = new JMenuItem(primitiveLabel + ": (" + primitiveType.getSelectedItem() + ") " + primitiveText.getText());
                            primitiveMenuItem.addActionListener(
                                new ActionListener()
                                {
                                    public void actionPerformed(ActionEvent ae)
                                    {
                                        arg[idx] = getPrimitiveValue(active.obj.virtualMachine(), primitiveType.getSelectedIndex(), primitiveText.getText());
                                        argLabel[idx].setText("" + arg[idx]);
                                        pack();
                                    }
                                });
                            popup.add(primitiveMenuItem);
                            popup.show(argLabel[idx], 8, 8);
                        }
                    });
                linen.add(argLabel[i]);
                if (i < atn.length - 1)
                {
                    linen.add(new JLabel(", "));
                }
                else
                {
                    linen.add(new JLabel("); "));
                }
                linen.add(new JLabel("// " + atn[i]));
                arg[i] = null;
                lines.add(linen);
            }
            getContentPane().add(lines, BorderLayout.CENTER);

            JPanel methodInvokePanel = new JPanel(null);
            methodInvokePanel.setLayout(new BoxLayout(methodInvokePanel, BoxLayout.X_AXIS));
            methodInvokePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            methodInvokePanel.add(Box.createHorizontalStrut(40));
            methodInvokePanel.add(Box.createHorizontalGlue());
            JButton invkButton = new JButton(invokeLabel);
            invkButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent evt)
                    {
                        Object rtrn = JdiObject.getDebuggerObject(((ObjectReference) active.obj)).invokeMethod(
                                theMethod.name(), theMethod.signature(), Arrays.asList(arg));
                        bluej.debugger.ObjectViewer.updateViewers();
                        boolean ok = true;
                        if (rtrn != null && rtrn instanceof ObjectReference)
                        {
                            fireInspectEvent(JdiObject.getDebuggerObject(((ObjectReference) rtrn)));
                        }
                        else if (rtrn != null && rtrn instanceof String)
                        {
                            JOptionPane.showMessageDialog(getJFrame(), noInvokeErrorLabel + ":\n" + rtrn,
                                    errorLabel, JOptionPane.ERROR_MESSAGE);
                            ok = false;
                        }
                        else if (rtrn == null)
                        {
                            JOptionPane.showMessageDialog(getJFrame(), "Null " + referenceLabel,
                                    "Returned Result:", JOptionPane.INFORMATION_MESSAGE);
                        }
                        else if (rtrn != null && !(rtrn instanceof VoidValue))
                        {
                            JOptionPane.showMessageDialog(getJFrame(), ((Value) rtrn).type().toString() + ": " + rtrn.toString(),
                                    "Returned Result:", JOptionPane.INFORMATION_MESSAGE);
                        }
                        if (ok)
                        {
                            MethodDialog.this.setVisible(false);
                            bluej.debugger.ObjectViewer.updateViewers();
                        }
                    }
                });
            invkButton.setEnabled(true);
            methodInvokePanel.add(invkButton);
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent evt)
                    {
                        MethodDialog.this.setVisible(false);
                    }
                });
            closeButton.setEnabled(true);
            methodInvokePanel.add(closeButton);
            getContentPane().add(methodInvokePanel, BorderLayout.SOUTH);
            pack();

        }
    }

    class AssignDialog extends JDialog
    {
        JTextField sourceLabel;
        Value source;
        GNode active;
        int fldIdx;
        Field theField;

        AssignDialog(JFrame parent, GNode activeParm, int fldIdxParm, Field theFieldParm)
        {
            super(parent, assignDialogLabel, false);
            active = activeParm;
            fldIdx = fldIdxParm;
            theField = theFieldParm;
            JPanel lines = new JPanel(null);
            lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
            lines.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JPanel line1 = new JPanel(null);
            line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
            line1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JLabel objLabel = new JLabel(((ObjectReference) active.obj).toString());
            line1.add(objLabel);
            if (active.obj instanceof ArrayReference)
            {
                line1.add(new JLabel(" [" + fldIdx + "] = "));
            }
            else
            {
                line1.add(new JLabel("."));
                JLabel methodLabel = new JLabel(theField.name() + " = ");
                line1.add(methodLabel);
            }
            lines.add(line1);
            JPanel linen = new JPanel(null);
            linen.setLayout(new BoxLayout(linen, BoxLayout.X_AXIS));
            linen.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            linen.add(Box.createHorizontalStrut(40));
            sourceLabel = new JTextField("null");
            sourceLabel.setEditable(false);
            sourceLabel.setToolTipText(assignToolTipLabel);
            sourceLabel.addMouseListener(
                new MouseAdapter()
                {
                    public void mouseClicked(MouseEvent e)
                    {
                        JPopupMenu popup = new JPopupMenu();
                        if (select != null)
                        {
                            JMenuItem copyMenuItem = new JMenuItem(selectedLabel + ": " + select.obj);
                            copyMenuItem.addActionListener(
                                new ActionListener()
                                {
                                    public void actionPerformed(ActionEvent ae)
                                    {
                                        source = select.obj;
                                        sourceLabel.setText("" + source);
                                        pack();
                                    }
                                });
                            popup.add(copyMenuItem);
                        }
                        JMenuItem pasteMenuItem = new JMenuItem(pasteLabel + ": " + copiedObjectReference);
                        pasteMenuItem.addActionListener(
                            new ActionListener()
                            {
                                public void actionPerformed(ActionEvent ae)
                                {
                                    source = copiedObjectReference;
                                    sourceLabel.setText("" + source);
                                    pack();
                                }
                            });
                        popup.add(pasteMenuItem);
                        JMenuItem primitiveMenuItem = new JMenuItem(primitiveLabel + ": (" + primitiveType.getSelectedItem() + ") " + primitiveText.getText());
                        primitiveMenuItem.addActionListener(
                            new ActionListener()
                            {
                                public void actionPerformed(ActionEvent ae)
                                {
                                    source = getPrimitiveValue(active.obj.virtualMachine(), primitiveType.getSelectedIndex(), primitiveText.getText());
                                    sourceLabel.setText("" + source);
                                    pack();
                                }
                            });
                        popup.add(primitiveMenuItem);
                        popup.show(sourceLabel, 8, 8);
                    }
                });
            linen.add(sourceLabel);
            linen.add(new JLabel("; "));
            if (active.obj instanceof ArrayReference)
            {
                try
                {
                    linen.add(new JLabel("// " + ((ArrayType) ((ArrayReference) active.obj).referenceType()).componentType().name()));
                }
                catch (ClassNotLoadedException cnle)
                {
                }
            }
            else
            {
                linen.add(new JLabel("// " + theField.typeName()));
            }
            lines.add(linen);

            getContentPane().add(lines, BorderLayout.CENTER);

            JPanel methodInvokePanel = new JPanel(null);
            methodInvokePanel.setLayout(new BoxLayout(methodInvokePanel, BoxLayout.X_AXIS));
            methodInvokePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            methodInvokePanel.add(Box.createHorizontalStrut(40));
            methodInvokePanel.add(Box.createHorizontalGlue());
            JButton invkButton = new JButton(assignLabel);
            invkButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent evt)
                    {
                        boolean ok = true;
                        try
                        {
                            if (active.obj instanceof ArrayReference)
                            {
                                ((ArrayReference) active.obj).setValue(fldIdx, source);
                            }
                            else
                            {
                                ((ObjectReference) active.obj).setValue(theField, source);
                            }
                        }
                        catch (Exception e)
                        {
                            ok = false;
                            JOptionPane.showMessageDialog(getJFrame(), noAssignmentErrorLabel + ":\n" + e,
                                    errorLabel, JOptionPane.ERROR_MESSAGE);
                        }
                        if (ok)
                        {
                            AssignDialog.this.setVisible(false);
                            bluej.debugger.ObjectViewer.updateViewers();
                        }
                    }
                });
            invkButton.setEnabled(true);
            methodInvokePanel.add(invkButton);
            JButton closeButton = new JButton(closeLabel);
            closeButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent evt)
                    {
                        AssignDialog.this.setVisible(false);
                    }
                });
            closeButton.setEnabled(true);
            methodInvokePanel.add(closeButton);
            getContentPane().add(methodInvokePanel, BorderLayout.SOUTH);
            pack();

        }
    }

    static class GNode implements Serializable
    {

        double x = Double.MIN_VALUE;
        double y = Double.MIN_VALUE;

        double dx;
        double dy;

        boolean fixed;
        boolean relaxing = false;
        String lbl = null;
        String value = null;
        Value obj = null;
        JPopupMenu popup = null;
        int expand = DEFAULT;
        GNode group = null;
        int groupCnt = 0;
        int groupIdx;
        int type = THRU;

        double q1, q2, q3, q4;
        double xleft, xright, ytop, ybottom;
        int w;
        int h;
        public final static int THRU = 0;

        public final static int DEFAULT = 5;
        public final static int TRUE = 6;
        public final static int FALSE = 7;

        GNode(Value obj)
        {
            this.obj = obj;
            setLbl();
        }

        GNode(String lbl)
        {
            this.lbl = lbl;
        }

        void setLbl()
        {
            if (obj != null)
            {
                this.lbl = obj.toString();
                if (this.lbl.startsWith("instance of "))
                {
                    this.lbl = this.lbl.substring(12);
                    if (obj.type() instanceof ClassType)
                    {
                        value = JdiObject.getDebuggerObject(((ObjectReference) obj)).invokeMethod(
                                "toString", "()Ljava/lang/String;", new java.util.Vector()).toString();
                    }
                }
            }
        }

    }

    static class Edge implements Serializable
    {

        GNode from;
        GNode to;
        String label;
    }

}
