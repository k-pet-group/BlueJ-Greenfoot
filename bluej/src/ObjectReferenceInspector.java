   import java.awt.*;
   import java.awt.event.*;
   import javax.swing.*;
   import javax.swing.event.*;
   import java.util.*;
   import java.io.*;
   import bluej.debugger.*;
   import com.sun.jdi.*;
   import bluej.debugger.jdi.*;
   import bluej.Config;


    public class ObjectReferenceInspector extends Inspector
    implements Runnable, MouseListener, MouseMotionListener {
      boolean initialized=false;
      boolean dirty=false;
      int nnodes;
      GNode nodes[] = new GNode[10];
   
      int nedges;
      Edge edges[] = new Edge[20];
   
      JPanel panel; 
      JButton inspectBtn, getBtn;
      private static String inspectLabel = Config.getString("debugger.objectviewer.inspect");
      private static String getLabel = Config.getString("debugger.objectviewer.get");
   
       public  String getInspectedClassname() {
         return "java.lang.Object";
      }
   
       public  String getInspectorTitle() {
         return "Object References";
      }
   
       public void saveFlowChart(FileOutputStream fos) 
       throws IOException {
         ObjectOutputStream oos= new ObjectOutputStream(fos);
         GNode saveNodes[] = new GNode[nnodes];
         System.arraycopy(nodes,0,saveNodes,0,nnodes);
         Edge saveEdges[] = new Edge[nedges];
         System.arraycopy(edges,0,saveEdges,0,nedges);
         oos.writeObject(saveNodes);
         oos.writeObject(saveEdges);
         oos.writeObject(new Boolean(random));
         oos.writeObject(new Integer(goodguess));
         oos.writeObject(new Integer(badguess));
         oos.writeObject(pick);
         oos.writeObject(select);
         oos.writeObject(new Boolean(pickfixed));
         oos.writeObject(new Boolean(justSelectedPick));
         dirty=false;
      }
   
       public void loadFlowChart(FileInputStream fis) 
       throws IOException, ClassNotFoundException {
         ObjectInputStream ois= new ObjectInputStream(fis);
         nodes=(GNode[])ois.readObject();
         nnodes=nodes.length;
         edges=(Edge[])ois.readObject();
         nedges=edges.length;
         random=((Boolean)ois.readObject()).booleanValue();
         goodguess=((Integer)ois.readObject()).intValue();
         badguess=((Integer)ois.readObject()).intValue();
         pick=((GNode)ois.readObject());
         select=((GNode)ois.readObject());
         pickfixed=((Boolean)ois.readObject()).booleanValue();
         justSelectedPick=((Boolean)ois.readObject()).booleanValue();
         offscreen=null;
         offscreensize=null;
         offgraphics=null;
         initialized=true;
         dirty=false;
      }
   
       public boolean isDirty() {
         return dirty;}
   
       private void allocateNodes() {
         GNode tempNodes[] = new GNode[nodes.length*2];
         System.arraycopy(nodes,0,tempNodes,0,nodes.length);
         nodes=tempNodes;
      }
   
       private void allocateEdges() {
         Edge tempEdges[] = new Edge[edges.length*2];
         System.arraycopy(edges,0,tempEdges,0,edges.length);
         edges=tempEdges;
      }
   
      Thread relaxer;
      boolean random;
   
      int goodguess=0,badguess=0;
      boolean justSelectedPick=false;
   
       public boolean initialize(DebuggerObject obj) {
         super.initialize(obj);
         panel = 
                new JPanel(null,false) {
                   public void paint(Graphics g) {
                     super.paint(g);
                     updateGraphics(g,false);
                  }
               };
         clear();
         panel.addMouseListener(this);
         panel.addMouseMotionListener(this);
         JPanel mainPanel=new JPanel(new BorderLayout());
         mainPanel.setBorder(Config.generalBorder);
         JPanel titlePanel = new JPanel();
         titlePanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
         JLabel classNameLabel = new JLabel(obj.getObjectReference().toString());
      //"( "+getInspectedClassname() + " )  " + ((ClassType)obj.getObjectReference().referenceType()).name());
         titlePanel.add(classNameLabel, BorderLayout.CENTER);
         mainPanel.add(titlePanel, BorderLayout.NORTH);
            // Create panel with "inspect" and "get" buttons
         JPanel buttonPanel = new JPanel();
         buttonPanel.setLayout(new GridLayout(0, 1));
      
         inspectBtn = new JButton(inspectLabel);
         inspectBtn.addActionListener(
                new ActionListener() {
                   public void actionPerformed(ActionEvent evt) {
                     if (select!=null)
                        fireInspectEvent(JdiObject.getDebuggerObject(((ObjectReference)select.obj)));
                  }
               });
         buttonPanel.add(inspectBtn);
      
         getBtn = new JButton(getLabel);
         getBtn.setEnabled(false);
         //getBtn.addActionListener(this);
         buttonPanel.add(getBtn);
      
         JPanel buttonFramePanel = new JPanel();
         buttonFramePanel.setLayout(new BorderLayout(0,0));
         buttonFramePanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
         buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
         mainPanel.add(buttonFramePanel, BorderLayout.EAST);
         JPanel boxpanel=new JPanel(null);
         boxpanel.setLayout(new BoxLayout(boxpanel,BoxLayout.X_AXIS));
         boxpanel.add(panel);
         mainPanel.add(new JScrollPane(boxpanel), BorderLayout.CENTER);
         add(mainPanel, BorderLayout.CENTER);
         clear();
         GNode root=addNode(obj.getObjectReference());
         extend(root);
         createCollectors();
         return true;
      }
   
       public void refresh() {
         //System.out.println(obj.getObjectReference());
         for (int i=0;i<nnodes;i++) {
            nodes[i].lbl=null;
         }
         for (int i=0;i<nedges;i++) {
            edges[i]=null;
         }
         nedges=0;
         GNode root=findGNode(obj.getObjectReference());
         if (root==null)
            root=addNode(obj.getObjectReference());
         else 
            root.setLbl();
         extend(root);
         int j=0;
         for (int i=0;i<nnodes;i++) {
            if (nodes[i].lbl==null) {
               j++;
            }
            else {
               if (j>0) 
                  nodes[i-j]=nodes[i];
            }
         }
         for (int i=nnodes-j;i<nnodes;i++)
            nodes[i]=null;
         nnodes-=j;
         System.out.println("Fewer GNodes="+j);
      }
   
       private GNode findGNode(ObjectReference obj) {
         for (int j=0;j<nnodes;j++)
            if (obj!=null&&nodes[j].obj!=null&&nodes[j].obj.equals(obj))
               return nodes[j];
         return null;
      }
   
       private void extend(GNode root) {
      // Handle Array, List, Map, and Set in a custom way!
         ReferenceType rt=root.obj.referenceType();
         if (rt instanceof ArrayType) {
            if (root.expand==GNode.TRUE) {
            }
         }
         else if(JdiObject.getDebuggerObject(root.obj).isAssignableTo("java.util.List")) {
            if (true||root.expand==GNode.TRUE) {
               root.group=root;
               root.groupCnt=0;
               ObjectReference[]listarray=((ObjectReference[])((ArrayReference)JdiObject.getDebuggerObject(root.obj).invokeMethod(
               "toArray", "()[Ljava/lang/Object;",new java.util.Vector())).
               getValues().toArray(new ObjectReference[0]));
               for(int i=0;i<listarray.length;i++) {
                  root.groupCnt++;
                  ObjectReference next=listarray[i];
                  GNode leaf=findGNode(next);
                  if (leaf==null) {
                     leaf=addNode(next);
                     extend(leaf);
                  }
                  else if (leaf.lbl==null) {
                     leaf.setLbl();
                     extend(leaf);
                  }
                  else {
                     //addEdge(root,leaf).label=field[i].name();
                  }
                  GNode entry=addNode("Entry",0);
                  addEdge(entry,leaf);
                  entry.group=root;
               }
            }
         }
         else if(JdiObject.getDebuggerObject(root.obj).isAssignableTo("java.util.Set")) {
            if (root.expand==GNode.TRUE) {
            }
         }
         else if(JdiObject.getDebuggerObject(root.obj).isAssignableTo("java.util.Map")) {
            if (true||root.expand==GNode.TRUE) {
               System.out.println("Map being expanded: "+root);
               root.group=root;
               root.groupCnt=0;
               ObjectReference set=((ObjectReference)JdiObject.getDebuggerObject(root.obj).invokeMethod(
               "entrySet", "()Ljava/util/Set;",new java.util.Vector()));
               ObjectReference[]setarray=((ObjectReference[])((ArrayReference)JdiObject.getDebuggerObject(set).invokeMethod(
               "toArray", "()[Ljava/lang/Object;",new java.util.Vector())).
               getValues().toArray(new ObjectReference[0]));
               for(int i=0;i<setarray.length;i++) {
                  ObjectReference leaf=setarray[i];
                  System.out.println("Map being expanded: "+root+", i="+i);
                  root.groupCnt++;
                  ObjectReference key=((ObjectReference)JdiObject.getDebuggerObject(leaf).invokeMethod(
                  "getKey", "()Ljava/lang/Object;",new java.util.Vector()));
                  GNode keyleaf=findGNode(key);
                  if (keyleaf==null||keyleaf.lbl==null) {
                     if (keyleaf==null) {
                        keyleaf=addNode(key);
                     }
                     else  {
                        keyleaf.setLbl();
                     }
                     extend(keyleaf);
                  }
                  extend(keyleaf);
                  ObjectReference value=((ObjectReference)JdiObject.getDebuggerObject(leaf).invokeMethod(
                  "getValue", "()Ljava/lang/Object;",new java.util.Vector()));
                  GNode valueleaf=findGNode(value);
                  if (valueleaf==null||valueleaf.lbl==null) {
                     if (valueleaf==null) {
                        valueleaf=addNode(value);
                     }
                     else  {
                        valueleaf.setLbl();
                     }
                     extend(valueleaf);
                  }
                  GNode entry=addNode("Entry",0);
                  addEdge(entry,keyleaf).label="Key";
                  addEdge(entry,valueleaf).label="Value";
                  entry.group=root;
               }
            }
         }
         else if((rt.name().startsWith("com.sun.") 
         || rt.name().startsWith("java.")
         || rt.name().startsWith("javax."))
         && ! 
         (rt.name().equals("java.util.TreeMap$Entry"))) { // System classes
            if (root.expand==GNode.TRUE) {
            }
         }
         else if (rt instanceof ClassType) {
            if (root.expand==GNode.TRUE || root.expand==GNode.DEFAULT) {
               Field[]field=((Field[])root.obj.referenceType().visibleFields().toArray(new Field[0]));
               for(int i=0;i<field.length;i++) {
                  System.out.println(i);
                  try {
                     if (field[i].type() instanceof ReferenceType) {
                        System.out.println(root.obj.getValue(field[i]));
                        ObjectReference next=((ObjectReference)root.obj.getValue(field[i]));
                        if (next!=null) {
                           GNode leaf=findGNode(next);
                           if (leaf==null) {
                              leaf=addNode(next);
                              addEdge(root,leaf).label=field[i].name();
                              extend(leaf);
                           }
                           else if (leaf.lbl==null) {
                              leaf.setLbl();
                              addEdge(root,leaf).label=field[i].name();
                              extend(leaf);
                           }
                           else {
                              addEdge(root,leaf).label=field[i].name();
                           }
                        }
                     }
                  }
                      catch (ClassNotLoadedException e) {
                        System.out.println(e);
                     }
               }
            }
         }
      }
   
       public boolean isInitialized() {
         return initialized;
      }
   
       public void clear() {
         for (int i=0;i<nodes.length;i++) {
            nodes[i]=null;
         }
         nnodes=0;
         for (int i=0;i<edges.length;i++) {
            edges[i]=null;
         }
         nedges=0;
      }
   
       GNode findNode(String lbl) {
         for (int i = 0 ; i < nnodes ; i++) {
            if (nodes[i].lbl.equals(lbl)) {
               return nodes[i];
            }
         }
         return addNode(lbl,0);
      }
   
       public GNode addNode(ObjectReference obj) {
         GNode n = new GNode(obj);
         // n.x = 10 + 380*Math.random();
         // n.y = 10 + 380*Math.random();
         if (nnodes>=nodes.length) 
            allocateNodes();
         nodes[nnodes] = n;
         nnodes++;
         return n;
      }
   
       public GNode addNode(String lbl, int sourceLineNo) {
         GNode n = new GNode(lbl, sourceLineNo);
         n.x = 10 + 380*Math.random();
         n.y = 10 + 380*Math.random();
         if (nnodes>=nodes.length) 
            allocateNodes();
         nodes[nnodes] = n;
         nnodes++;
         return n;
      }
   
       public GNode addNode(String lbl, int type, int sourceLineNo) {
         GNode n = new GNode(lbl, type, sourceLineNo);
         n.x = 10 + 380*Math.random();
         n.y = 10 + 380*Math.random();
         if (nnodes>=nodes.length) 
            allocateNodes();
         nodes[nnodes] = n;
         nnodes++;
         return n;
      }
   
       public GNode addNodeCollector(GNode theGNode) {
         GNode n = new GNode(null, GNode.COLLECTOR,0);
         n.x = 10 + 380*Math.random();
         n.y = 10 + 380*Math.random();
         Edge e = new Edge();
         e.from = n;
         e.to = theGNode;
         e.label=null;
         e.len = 10;
         if (nedges>=edges.length) 
            allocateEdges();
         edges[nedges++] = e;
         theGNode.collector=n;
         if (nnodes>=nodes.length) 
            allocateNodes();
         nodes[nnodes] = n;
         nnodes++;
         return n;
      }
   
       public Edge addBendEdge(Edge theEdge) {
         GNode n = new GNode(null, GNode.BEND);
         n.x = 10 + 380*Math.random();
         n.y = 10 + 380*Math.random();
         Edge e = new Edge();
         e.from = n;
         e.to = theEdge.to;
         e.label=null;
         e.len = 10;
         theEdge.to=n;
         if (nedges>=edges.length) 
            allocateEdges();
         edges[nedges++] = e;
         if (nnodes>=nodes.length) 
            allocateNodes();
         nodes[nnodes] = n;
         nnodes++;
         return e;
      }
   
       public GNode addNode(String lbl, int type, String fromLabel, int sourceLineNo) {
         GNode n = new GNode(lbl, type, fromLabel, sourceLineNo);
         n.x = 10 + 380*Math.random();
         n.y = 10 + 380*Math.random();
         if (nnodes>=nodes.length) 
            allocateNodes();
         nodes[nnodes] = n;
         nnodes++;
         return n;
      }
   
       public void addNode(GNode n, int sourceLineNo) {
         n.sourceLineNo=sourceLineNo;
         n.x = 10 + 380*Math.random();
         n.y = 10 + 380*Math.random();
         if (nnodes>=nodes.length) 
            allocateNodes();
         nodes[nnodes] = n;
         nnodes++;
      }
   
       public void addEdge(String from, String to, int len) {
         Edge e = new Edge();
         e.from = findNode(from);
         e.to = findNode(to);
         e.len = len;
         if (nedges>=edges.length) 
            allocateEdges();
         edges[nedges++] = e;
      }
   
       public Edge addEdge(GNode from, GNode to) {
         if (to.x==Double.MIN_VALUE)
            to.x=from.x+10;
         if (to.y==Double.MIN_VALUE)
            to.y=from.y+10;
         Edge e = new Edge();
         e.from = from;
         e.to = to;
         e.len = 10;
         if (nedges>=edges.length) 
            allocateEdges();
         edges[nedges++] = e;
         return e;
      }
   
       public void addEdges(Vector from, GNode to) {
         for (Enumeration en = from.elements() ; en.hasMoreElements() ;) {
            GNode n=(GNode)en.nextElement();
            Edge e = new Edge();
            e.from = n;
            e.to = to;
            e.label=n.fromLabel;
            e.len = 10;
            if (nedges>=edges.length) 
               allocateEdges();
            edges[nedges++] = e;
         }
      }
   
       public void addBendEdges(Vector from, GNode to) {
         for (Enumeration en = from.elements() ; en.hasMoreElements() ;) {
            GNode n=(GNode)en.nextElement();
            Edge e = new Edge();
            e.from = n;
            e.to = to;
            e.label=n.fromLabel;
            e.len = 10;
            if (nedges>=edges.length) 
               allocateEdges();
            edges[nedges++] = e;
            addBendEdge(addBendEdge(addBendEdge(e)));
         }
      }
   
       public void createCollectors() {
         dirty=false;
         goodguess=0;
         badguess=0;
         initialized=true;
         int across=80;
         int down=20;
         for (int i = 0 ; i < nnodes ; i++) {
            GNode n = nodes[i];
            n.impinge=0;
            n.x=across;
            n.y=down;
            if (((i+1)/8)%2==0)
               across+=40;
            else 
               across-=40;
            down+=40;
         }
         for (int i = 0 ; i < nedges ; i++) {
            Edge e = edges[i];
            e.to.impinge++;
         }
         // int nnodesStart=nnodes;
         // int nedgesStart=nedges;
         // for (int i = 0 ; i < nnodesStart ; i++) {
            // GNode n = nodes[i];
            // if (n.impinge>1) {
               // GNode collect=addNodeCollector(n);
               // collect.x=n.x-20;
               // collect.y=n.y-20;
            // }
         // }
         // for (int i = 0 ; i < nedgesStart ; i++) {
            // Edge e = edges[i];
         // // if (e.from.type!=GNode.THRU
         // // ||e.to.type!=GNode.THRU) {
            // e.visible=true;//BlueJ
         // //}
            // if (e.to.collector!=null) {
               // e.to=e.to.collector;
            // //addBendEdge(addBendEdge(addBendEdge(e)));
            // //addBendEdge(e);
            // }
         // }
         across+=40;
         down+=40;
         panel.setSize(400,down);
         panel.setPreferredSize(new Dimension(400,down));
         //panel.setMinimumSize(new Dimension(400,down));
      }
   
       public void run() {
         Thread me = Thread.currentThread();
         while (relaxer == me) {
            relax();
            if (random && (Math.random() < 0.03)) {
               GNode n = nodes[(int)(Math.random() * nnodes)];
               if (!n.fixed) {
                  n.x += 100*Math.random() - 50;
                  n.y += 100*Math.random() - 50;
               }
            //graph.play(graph.getCodeBase(), "audio/drip.au");
            }
            try {
               Thread.sleep(100);
            } 
                catch (InterruptedException e) {
                  break;
               }
         }
      }
   
       synchronized void relax() {
         for (int i = 0 ; i < nedges ; i++) {
            Edge e = edges[i];
            double vx = e.to.x - e.from.x;
            double vy = e.to.y - e.from.y;
            double len = Math.sqrt(vx * vx + vy * vy);
            len = (len == 0) ? .0001 : len;
            double f = (edges[i].len - len) / (len * 3);
            double dx = f * vx;
            double dy = f * vy;
         
            e.to.dx += dx;
            e.to.dy += dy;
            e.from.dx += -dx;
            e.from.dy += -dy;
         }
      
         for (int i = 0 ; i < nnodes ; i++) {
            GNode n1 = nodes[i];
            double dx = 0;
            double dy = 0;
         
            for (int j = 0 ; j < nnodes ; j++) {
               if (i == j) {
                  continue;
               }
               GNode n2 = nodes[j];
               double vx = n1.x - n2.x;
               double vy = n1.y - n2.y;
               double len = vx * vx + vy * vy;
               if (len == 0) {
                  dx += Math.random();
                  dy += Math.random();
               } 
               else if (len < 100*100) {
                  dx += vx / len;
                  dy += vy / len;
               }
            }
            double dlen = dx * dx + dy * dy;
            if (dlen > 0) {
               dlen = Math.sqrt(dlen) / 2;
               n1.dx += dx / dlen;
               n1.dy += dy / dlen;
            }
         }
      
         Dimension d = getSize();
         for (int i = 0 ; i < nnodes ; i++) {
            GNode n = nodes[i];
            if (!n.fixed) {
               n.x += Math.max(-5, Math.min(5, n.dx));
               n.y += Math.max(-5, Math.min(5, n.dy));
            }
            if (n.x < 0) {
               n.x = 0;
            } 
            else if (n.x > d.width) {
               n.x = d.width;
            }
            if (n.y < 0) {
               n.y = 0;
            } 
            else if (n.y > d.height) {
               n.y = d.height;
            }
            n.dx /= 2;
            n.dy /= 2;
         }
         panel.repaint();
      }
   
      GNode pick,select;
      boolean pickfixed;
      Image offscreen;
      Dimension offscreensize;
      Graphics offgraphics;
      double offsetx=0,offsety=0;
   
      final Color fixedColor = Color.red;
      final Color selectColor = Color.red;
      final Color edgeColor = Color.black;
      final Color nodeColor = new Color(250, 220, 100);
      final Color stressColor = Color.darkGray;
      final Color arcColor1 = Color.black;
      final Color arcColor2 = Color.pink;
      final Color arcColor3 = Color.red;
   
       public void paintNode(Graphics g, GNode n, FontMetrics fm) {
         int x = (int)n.x;
         int y = (int)n.y;
         n.p1=GNode.UNUSED;
         n.p2=GNode.UNUSED;
         n.p3=GNode.UNUSED;
         n.p4=GNode.UNUSED;
         switch(n.type) {
            case GNode.THRU:
               if (n == pick) {
                  g.setColor(selectColor);
               }
               else if (n == select) {
                  g.setColor(selectColor);
               }
               else {
                  g.setColor(nodeColor);
               }
               g.fillRect(x - n.w/2, y - n.h / 2, n.w, n.h);
               g.setColor(Color.black);
               g.drawRect(x - n.w/2, y - n.h / 2, n.w-1, n.h-1);
               g.drawString(n.lbl, x - (n.w-10)/2, (y - (n.h-4)/2) + fm.getAscent());
               n.x1=x;
               n.y1=y - n.h / 2;
               n.x2=x;
               n.y2=y + n.h / 2;
               n.x3=x+n.w/2;
               n.y3=y;
               n.x4=x-n.w/2;
               n.y4=y;
               break;
            case GNode.CHOICE:
               if (n == pick) {
                  g.setColor(selectColor);
               }
               else if (n == select) {
                  g.setColor(selectColor);
               }
               else {
                  g.setColor(Color.pink);
               }
               int newh=n.h+n.w/5;
               int neww=n.w+n.w/7;
               int[] polyx={
                  x,x+neww/2,x,x-neww/2};
               int[] polyy={
                  y-newh/2,y,y+newh/2,y};
               g.fillPolygon(polyx,polyy,4);
            //g.fillRect(x - w/2, y - h / 2, w, h);
               g.setColor(Color.black);
               g.drawPolygon(polyx,polyy,4);
            //g.drawRect(x - w/2, y - h / 2, w-1, h-1);
            //g.drawString(n.lbl, x - w/2 + 15, y /*+ h/2*/ + 4 /*+ fm.getAscent()*/);
               g.drawString(n.lbl, x - (n.w-10)/2, (y - (n.h-4)/2) + fm.getAscent());
               n.x1=x;
               n.y1=y - newh / 2;
               n.x2=x;
               n.y2=y + newh / 2;
               n.x3=x+neww/2;
               n.y3=y;
               n.x4=x-neww/2;
               n.y4=y;
               break;
            case GNode.CONNECT:
               if (n == pick) {
                  g.setColor(selectColor);
               }
               else if (n == select) {
                  g.setColor(selectColor);
               }
               else {
                  g.setColor(Color.green);
               }
               g.fillOval(x - n.w/2, y - n.h / 2, n.w, n.h);
               g.setColor(Color.black);
               g.drawOval(x - n.w/2, y - n.h / 2, n.w-1, n.h-1);
               g.drawString(n.lbl, x - (n.w-10)/2, (y - (n.h-4)/2) + fm.getAscent());
               n.x1=x;
               n.y1=y - n.h / 2;
               n.x2=x;
               n.y2=y + n.h / 2;
               n.x3=x+n.w/2;
               n.y3=y;
               n.x4=x-n.w/2;
               n.y4=y;
               break;
            case GNode.COLLECTOR:
               if (n == pick) {
                  g.setColor(selectColor);
                  g.fillOval(x - n.w/2, y - n.h / 2, n.w, n.h);
               }
               else if (n == select) {
                  g.setColor(selectColor);
                  g.fillOval(x - n.w/2, y - n.h / 2, n.w, n.h);
               }
               g.setColor(Color.black);
               g.drawOval(x - n.w/2, y - n.h / 2, n.w-1, n.h-1);
               n.x1=x;
               n.y1=y - n.h / 2;
               n.x2=x;
               n.y2=y + n.h / 2;
               n.x3=x+n.w/2;
               n.y3=y;
               n.x4=x-n.w/2;
               n.y4=y;
               break;
            case GNode.BEND:
               g.setColor((n == pick) ? selectColor : (n.fixed ? fixedColor : arcColor3));
               g.fillOval(x - n.w/2, y - n.h / 2, n.w, n.h);
               g.setColor(Color.black);
               g.drawOval(x - n.w/2, y - n.h / 2, n.w-1, n.h-1);
               n.x1=x;
               n.y1=y - n.h / 2;
               n.x2=x;
               n.y2=y + n.h / 2;
               n.x3=x+n.w/2;
               n.y3=y;
               n.x4=x-n.w/2;
               n.y4=y;
               break;
         }
      }
   
       public synchronized void updateGraphics(Graphics g, boolean printing) {
         if (!printing) {
            Dimension d = panel.getSize();
            if ((offscreen == null) || (d.width != offscreensize.width) || (d.height != offscreensize.height)) {
               offscreen = createImage(d.width, d.height);
               offscreensize = d;
               offgraphics = offscreen.getGraphics();
               offgraphics.setFont(getFont());
            }
         
            offgraphics.setColor(getBackground());
            offgraphics.fillRect(0, 0, d.width, d.height);
         }
         else {
            offgraphics=g;
            offgraphics.setFont(getFont());
         }
         FontMetrics fm = offgraphics.getFontMetrics();
         for (int i = 0 ; i < nnodes ; i++) {
            nodes[i].groupIdx=i;
         }
         for (int i = 0 ; i < nnodes ; i++) {
            nodes[i].groupIdx=0;
            if(nodes[i].group!=null 
            && nodes[i].group!=nodes[i] 
            && nodes[i].group.groupIdx!=0) {
               GNode temp=nodes[i];
               nodes[i]=temp.group;
               nodes[temp.group.groupIdx]=temp;
               temp.group.groupIdx=0;
            }
         }
         for (int i = 0 ; i < nnodes ; i++) {
            GNode n=nodes[i];
            if (n.lbl!=null) {
               n.w = fm.stringWidth(n.lbl) + 10;
               if (n.groupCnt==0)
                  n.h = fm.getHeight() + 4;
               else
                  n.h = (fm.getHeight() + 9)*(n.groupCnt+1);
            }
            else {
               n.w=12;
               n.h=12;
            }
            if (n.group!=null && n.group!=n) {
               n.group.groupIdx++;
               n.x=n.group.x;
               n.y=(n.group.y-n.group.h/2)+n.group.groupIdx*(fm.getHeight() + 9)+8;
               n.group.w=Math.max(n.group.w,n.w+12);
            }
         }
         for (int i = 0 ; i < nnodes ; i++) {
            paintNode(offgraphics, nodes[i], fm);
         }
      
         for (int i = 0 ; i < nedges ; i++) {
            Edge e = edges[i];
            if (e.visible) {
               int x1;
               int y1;
               int x2;
               int y2;
               int dist;
               int dist2;
               x1 = (int)e.from.x;
               y1 = (int)e.from.y;
               x2 = (int)e.to.x;
               y2 = (int)e.to.y;
               int p=0;
               dist=Integer.MAX_VALUE;
               dist2=sqr((int)e.from.x1-x2)+sqr((int)e.from.y1-y2);
               if (dist2<dist && e.from.p1==GNode.UNUSED) {
                  x1 = (int)e.from.x1;
                  y1 = (int)e.from.y1;
                  p=1;
                  dist=dist2;
               }
               dist2=sqr((int)e.from.x2-x2)+sqr((int)e.from.y2-y2);
               if (dist2<dist && e.from.p2==GNode.UNUSED) {
                  x1 = (int)e.from.x2;
                  y1 = (int)e.from.y2;
                  p=2;
                  dist=dist2;
               }
               dist2=sqr((int)e.from.x3-x2)+sqr((int)e.from.y3-y2);
               if (dist2<dist && e.from.p3==GNode.UNUSED) {
                  x1 = (int)e.from.x3;
                  y1 = (int)e.from.y3;
                  p=3;
                  dist=dist2;
               }
               dist2=sqr((int)e.from.x4-x2)+sqr((int)e.from.y4-y2);
               if (dist2<dist && e.from.p4==GNode.UNUSED) {
                  x1 = (int)e.from.x4;
                  y1 = (int)e.from.y4;
                  p=4;
                  dist=dist2;
               }
               switch (p) {
                  case 1:
                     e.from.p1=GNode.OUTPUT;
                     break;
                  case 2:
                     e.from.p2=GNode.OUTPUT;
                     break;
                  case 3:
                     e.from.p3=GNode.OUTPUT;
                     break;
                  case 4:
                     e.from.p4=GNode.OUTPUT;
                     break;
               }
               dist=Integer.MAX_VALUE;
               dist2=sqr((int)e.to.x1-x1)+sqr((int)e.to.y1-y1);
               if (dist2<dist && e.to.p1!=GNode.OUTPUT) {
                  x2 = (int)e.to.x1;
                  y2 = (int)e.to.y1;
                  p=1;
                  dist=dist2;
               }
               dist2=sqr((int)e.to.x2-x1)+sqr((int)e.to.y2-y1);
               if (dist2<dist && e.to.p2!=GNode.OUTPUT) {
                  x2 = (int)e.to.x2;
                  y2 = (int)e.to.y2;
                  p=2;
                  dist=dist2;
               }
               dist2=sqr((int)e.to.x3-x1)+sqr((int)e.to.y3-y1);
               if (dist2<dist && e.to.p3!=GNode.OUTPUT) {
                  x2 = (int)e.to.x3;
                  y2 = (int)e.to.y3;
                  p=3;
                  dist=dist2;
               }
               dist2=sqr((int)e.to.x4-x1)+sqr((int)e.to.y4-y1);
               if (dist2<dist && e.to.p4!=GNode.OUTPUT) {
                  x2 = (int)e.to.x4;
                  y2 = (int)e.to.y4;
                  p=4;
                  dist=dist2;
               }
               switch (p) {
                  case 1:
                     e.to.p1=GNode.INPUT;
                     break;
                  case 2:
                     e.to.p2=GNode.INPUT;
                     break;
                  case 3:
                     e.to.p3=GNode.INPUT;
                     break;
                  case 4:
                     e.to.p4=GNode.INPUT;
                     break;
               }
               int len = (int)Math.abs(Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)) - e.len);
               offgraphics.setColor(edgeColor) ;
               offgraphics.drawLine(x1, y1, x2, y2);
               double ang=Math.atan2(-(y1-y2),(x1-x2));
               offgraphics.drawLine((int)(x2+Math.cos(ang+0.3)*12), 
                  (int)-(-y2+Math.sin(ang+0.3)*12), x2, y2);
               offgraphics.drawLine((int)(x2+Math.cos(ang-0.3)*12), 
                  (int)-(-y2+Math.sin(ang-0.3)*12), x2, y2);
               if (e.label!=null) {
                  String lbl = e.label;
                  offgraphics.setColor(stressColor);
                  offgraphics.drawString(lbl, x1 + (x2-x1)/3, y1 + (y2-y1)/3);
                  offgraphics.setColor(edgeColor);
               }
            }
         }
         if (!printing)
            g.drawImage(offscreen, 0, 0, null);
      }
   
       GNode getGNode(int pick) {
         GNode nearest=nodes[0];
         for (int i = 1 ; i < nnodes ; i++) {
            GNode n = nodes[i];
            if (n.sourceLineNo<=pick
            &&n.sourceLineNo>nearest.sourceLineNo) {
               nearest=n;
            }
         }
         return nearest;
      }
   
       void setSelect(GNode newSelect) {
         select=newSelect;
      }
   
            //1.1 event handling
       public void mouseClicked(MouseEvent e) {
         //panel.setPreferredSize(getSize());
         dirty=true;
      //System.out.println(e.toString());
         if (isInitialized()) {
            if (e.getClickCount() > 1 && select!=null)
               fireInspectEvent(JdiObject.getDebuggerObject(((ObjectReference)select.obj)));
         } 
         if (justSelectedPick) {
            justSelectedPick=false;
            return;
         }
         double bestdist = Double.MAX_VALUE;
         int x = e.getX();
         int y = e.getY();
         GNode oldSelect=select;
         for (int i = 0 ; i < nnodes ; i++) {
            GNode n = nodes[i];
            double dist = (n.x - x) * (n.x - x) + (n.y - y) * (n.y - y);
            if (dist < bestdist) {
               select = n;
               bestdist = dist;
            }
         }
         if (oldSelect==select) {
            select=null;
         }
         else if (oldSelect!=null) {
            for (int i = 0 ; i < nedges ; i++) {
               Edge edg = edges[i];
               if (edg.from==oldSelect
               && (edg.to==select
               ||edg.to==select.collector)) {
                  if (!edg.visible) {
                     goodguess++;
                     edg.visible=true;
                     panel.repaint();
                  }
                  select=null;
                  e.consume();
                  return;
               }
            }
            select=null;
            badguess++;
            e.consume();
         }
      }
   
       public void mousePressed(MouseEvent e) {
      //System.out.println(e.toString());
         setPreferredSize(getSize());
         dirty=true;
         if (isInitialized()) {
            addMouseMotionListener(this);
            justSelectedPick=false;
            double bestdist = Double.MAX_VALUE;
            int x = e.getX();
            int y = e.getY();
            for (int i = 0 ; i < nnodes ; i++) {
               GNode n = nodes[i];
               double dist = (n.x - x) * (n.x - x) + (n.y - y) * (n.y - y);
               if (dist < bestdist) {
                  pick = n;
                  bestdist = dist;
               }
            }
            pickfixed = pick.fixed;
            pick.fixed = true;
            // pick.x = x;
            // pick.y = y;
            offsetx=pick.x - x;
            offsety=pick.y - y;
            if (pick==select) {
            }
            else if (select!=null) {
               for (int i = 0 ; i < nedges ; i++) {
                  Edge edg = edges[i];
                  if (edg.from==select
                  && (edg.to==pick
                  ||edg.to==pick.collector)) {
                     select=null;
                     if (!edg.visible) {
                        goodguess++;
                        edg.visible=true;
                        justSelectedPick=true;
                        boolean noneOut=true;
                        for (int j = 0 ; j < nedges ; j++) {
                           Edge outedg = edges[j];
                           if (outedg.from==pick && !outedg.visible) {
                              noneOut=false;
                              break;
                           }
                        }
                        if (!noneOut) {
                           select=pick;
                        }
                        else {
                           int j;
                           for (j = 0 ; j < nedges && edges[j].visible; j++) {}
                           if (!(j < nedges)) {
                              SwingUtilities.invokeLater(
                                     new Runnable() {
                                        public void run() { 
                                          JOptionPane.showMessageDialog(null, 
                                             "All "+goodguess+" flowlines have been added.\nThere were "
                                             +badguess+" incorrect additions that were rejected.\n"
                                             +"The percentage of success was "
                                             +(goodguess*100/(goodguess+badguess))+" percent.");
                                       }
                                    }
                                 );                
                           }
                        }
                     }
                     panel.repaint();
                     e.consume();
                     return;
                  }
               }
               select=null;
               badguess++;
            } 
            panel.repaint();
            e.consume();
         }
      }
   
       public void mouseReleased(MouseEvent e) {
      //System.out.println(e.toString());
         if (isInitialized()) {
            removeMouseMotionListener(this);
            if (pick != null) {
               pick.x = e.getX()+offsetx;
               pick.y = e.getY()+offsety;
               pick.fixed = !pickfixed;
               pick = null;
            }
            panel.repaint();
            e.consume();
         }
      }
   
       public void mouseEntered(MouseEvent e) {
      //System.out.println(e.toString());
      }
   
       public void mouseExited(MouseEvent e) {
      //System.out.println(e.toString());
      }
   
       public void mouseDragged(MouseEvent e) {
      //System.out.println(e.toString());
         if (isInitialized()) {
            if (pick.group!=null
            && pick.group!=pick) {
               offsetx -= (pick.x - pick.group.x);
               offsety -= (pick.y - pick.group.y);
               pick=pick.group;
            }
            pick.x = e.getX()+offsetx;
            pick.y = e.getY()+offsety;
            panel.repaint();
            e.consume();
         }
      }
   
       public void mouseMoved(MouseEvent e) {
      //System.out.println(e.toString());
      }
   
       public void start() {
         relaxer = new Thread(this);
         relaxer.start();
      }
   
       public void stop() {
         relaxer = null;
      }
   
       static private int sqr(int x) {
         return x*x;
      }
   }

    public class GNode implements Serializable {
      public final static int THRU=0;
      public final static int CHOICE=1;
      public final static int CONNECT=2; 
      public final static int COLLECTOR=3; 
      public final static int BEND=4;
   
      public final static int DEFAULT=5;
      public final static int TRUE=6;
      public final static int FALSE=7;
   
      double x=Double.MIN_VALUE;
      double y=Double.MIN_VALUE;
   
      double dx;
      double dy;
   
      boolean fixed;
   
      String lbl=null;
      ObjectReference obj=null;
      int expand=DEFAULT;
      GNode group=null;
      int groupCnt=0;
      int groupIdx;
      int impinge;
      GNode collector;
      int type=THRU;
      String fromLabel=null;
      int sourceLineNo=0;
   
      double x1,y1,x2,y2,x3,y3,x4,y4; // top and bottom connects
      int p1,p2,p3,p4;
      int w;
      int h;
      public final static int UNUSED=0;
      public final static int INPUT=1;
      public final static int OUTPUT=2;
   
       GNode (ObjectReference obj) {
         this.type=THRU;
         this.obj=obj;
         setLbl();
      }
   
       void setLbl() {
         this.lbl=obj.toString();
         if (this.lbl.startsWith("instance of "))
            this.lbl=this.lbl.substring(12);
      }
   
       GNode (String lbl, int sourceLineNo) {
         this.lbl=lbl;
         this.type=THRU;
         this.sourceLineNo=sourceLineNo;
      }
   
       GNode (String lbl, int type, int sourceLineNo) {
         this.lbl=lbl;
         this.type=type;
         this.sourceLineNo=sourceLineNo;
      }
   
       GNode (String lbl, int type, String fromLabel, int sourceLineNo) {
         this.lbl=lbl;
         this.type=type;
         this.fromLabel=fromLabel;
         this.sourceLineNo=sourceLineNo;
      }
   
       public void setFromLabel (String fromLabel) {
         this.fromLabel=fromLabel;
      }
   }

    class Edge  implements Serializable {
      GNode from;
      GNode to;
      String label;
      boolean visible=true;
      double len;
   }
