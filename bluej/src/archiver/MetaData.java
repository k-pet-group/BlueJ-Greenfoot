/*
 * @(#)MetaData.java	1.12 99/11/09
 *
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package archiver;

import java.util.*;
import java.lang.reflect.*;
import java.beans.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.plaf.*;

/**
 * @version 1.12 11/09/99
 * @author Philip Milne
 * @author Steve Langley
 */

class Join implements Enumeration {
    Enumeration e1;
    Enumeration e2;

    public Join(Enumeration e1, Enumeration e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    public Object nextElement() {
        return (e1.hasMoreElements()) ? e1.nextElement() : e2.nextElement();
    }

    public boolean hasMoreElements() {
        return e1.hasMoreElements() || e2.hasMoreElements();
    }
}


public class MetaData {

    private Hashtable classToProperties;

    // Debugging info.
    private boolean showInstantiations = false;
    private int instantiations = 0;


    public void showInstantiations(boolean b) {
        showInstantiations = b;
    }

    private Object uq(Object o) {
        return new Object[]{"unquote", o};
    }

    private ClassInfo primitiveClassInfo(final Class type) {
        return new ClassInfo(type) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                // System.out.println("getConstructor: " + name + " " + newInstance);
                if (newInstance.equals(oldInstance)) {
                    return null;
                } 
                /*
                Note if we leave the following line of code in effect the 
                code generator ill carefully observe the identity of the 
                strings which are used for these constructors. In the case of 
                the Boolean class this means that the generator must 
                record the static "true" and "false" strings which are returned 
                from the toString() method, so that it uses exactly these 
                Strings in the constructors. For all of the primitive 
                classes in the JDK this is unnecessary and makes the 
                archives much larger. The identity of the Strings 
                supplied to the Constructors of the java.lang.* 
                classes (such as Booleans and Number derivatives) 
                is not important - only the string's value. 
                // return new Object[]{".", uq(type), new Object[]{"new", uq(newInstance.toString())}};
                */ 
                return new Object[]{".", uq(type), new Object[]{"new", new Object[]{"\"", newInstance.toString()}}};
            }
        };
    }

    private ClassInfo arrayClassInfo(final Class type) {
        return new ClassInfo(type) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                return new Object[]{".", uq(Array.class), new Object[]{"newInstance", uq(type.getComponentType()), uq(new Integer(Array.getLength(newInstance)))}}; 
            }

            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                int n = Array.getLength(newInstance);
                Object[] result = new Object[n];
                for (int i = 0; i < n; i++) {
                    result[i] = new Object[]{"=", 
                                    new Object[]{".", uq(newInstance), new Integer(i).toString()}, 
                                    uq(Array.get(newInstance, i))};
                }
                return result;
            }
        };
    }

    private ClassInfo proxyClassInfo(final Class type) {
        return new ClassInfo(type) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                // System.out.println("getConstructor: " + name + " " + newInstance);
                if (newInstance.equals(oldInstance)) {
                    return null;
                }
                Proxy p = (Proxy)newInstance;
                return new Object[]{".", uq(Proxy.class),
                           new Object[]{"newProxyInstance",
                                        uq(type.getClassLoader()),
                                        uq(type.getInterfaces()),
                                        uq(Proxy.getInvocationHandler(p))}};
            };
        };
    }

    // Duplicated in evaluator. 
    private Class typeToClass(Class type) {
        if (!type.isPrimitive()) return type;
        if (type == Boolean.TYPE) return Boolean.class;
        if (type == Byte.TYPE) return Byte.class;
        if (type == Character.TYPE) return Character.class;
        if (type == Short.TYPE) return Short.class;
        if (type == Integer.TYPE) return Integer.class;
        if (type == Long.TYPE) return Long.class;
        if (type == Float.TYPE) return Float.class;
        if (type == Double.TYPE) return Double.class;
        if (type == Void.TYPE) return Void.class;
        return null;
    }

    private Object getPrivateField(Object instance, Class declaringClass, String name) { 
        try {
            Field f = declaringClass.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(instance);
        }
        catch (Exception e) {
            System.out.println("Field '" + name  + "' couldn't be extracted from an object of class '" + declaringClass.getName() + "'");
            e.printStackTrace();
        }
        return null;
    }

    public MetaData() {
        classToProperties = new Hashtable();

        // Primitives, Arrays and Proxies are handled dynamically.

        // Strings
        classToProperties.put(String.class, new ClassInfo(String.class) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                return (newInstance.equals(oldInstance)) ? null
                                        : new Object[]{"\"", newInstance};
            }
        });

        // Classes
        classToProperties.put(Class.class, new ClassInfo(Class.class) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                Class c = (Class)newInstance; 
                // As of Kestrel, Class.forName("int") fails, so we have to 
                // generate different code for primitive types. This is needed 
                // for arrays whose subtype may be primitive. 
                if (c.isPrimitive()) { 
                    Class wrapper = typeToClass(c);   
                    if (wrapper == null) { 
                        System.err.println("Unknown primitive type: " + c); 
                        return null; 
                    }
                    return new Object[]{".", uq(wrapper), "TYPE"}; 
                }
                return new Object[]{".", uq(Class.class),
                           // new Object[]{"forName", uq(c.getName())}}; 
                           /* The line above can generate expressions of 
                              the form: 
                                  String7 = "Test"; 
                                  ... 
                                  Class.forName(String7); 
                              Which confuses the XMLOutputStream as it 
                              tries to write out the class definitions. 
                              Replace this with an always literal representation 
                              of the String. 
                           */ 
                           new Object[]{"forName", new Object[]{"\"", c.getName()}}}; 
                           
            }
        });

        // Methods
        classToProperties.put(Method.class, new ClassInfo(Method.class) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                Method m = (Method)newInstance;
                return new Object[]{".", uq(m.getDeclaringClass()),
                    new Object[]{"getMethod", uq(m.getName()), uq(m.getParameterTypes())}};
            }
        });

        // Dimension, Rectangle and Point
        
        /*
        1.2 introduces "public double getX()" et al. which return values 
        which cannot be used in the constructors (they are the wrong type). 
        Generate special case code for these classes. 
        */
        
        // Point
        classToProperties.put(Point.class, new ClassInfo(Point.class) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                if (newInstance.equals(oldInstance)) {
                    return null;
                }
                Point p = (Point)newInstance;
                return new Object[]{".", uq(Point.class),
                    new Object[]{"new", uq(new Integer(p.x)), uq(new Integer(p.y))}};
            }

        });
        
        // Dimension
        // Don't union Dimension properties. getSize() clones, creating an infinite graph.
        classToProperties.put(Dimension.class, new ClassInfo(Dimension.class) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                if (newInstance.equals(oldInstance)) {
                    return null;
                }
                Dimension d = (Dimension)newInstance;
                return new Object[]{".", uq(Dimension.class),
                    new Object[]{"new", uq(new Integer(d.width)), uq(new Integer(d.height))}};
            }
        });

        // Rectangle
        classToProperties.put(Rectangle.class, new ClassInfo(Rectangle.class) {
            public Object getConstructor(Object newInstance, Object oldInstance) {
                if (newInstance.equals(oldInstance)) {
                    return null;
                }
                Rectangle r = (Rectangle)newInstance;
                return new Object[]{".", uq(Rectangle.class),
                    new Object[]{"new", uq(new Integer(r.x)), uq(new Integer(r.y)),
                                        uq(new Integer(r.width)), uq(new Integer(r.height))}};
            }
        });

        // 1.1 style 'collections'

        // Vector
        classToProperties.put(Vector.class, new ClassInfo(Vector.class) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                Vector v = (Vector)newInstance;
                Object[] result = new Object[v.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Object[]{".", uq(newInstance),
                                    new Object[]{"addElement", uq(v.elementAt(i))}};
                }
                return result;
            }

        });

        // Hashtable
        classToProperties.put(Hashtable.class, new ClassInfo(Hashtable.class) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                Hashtable h = (Hashtable)newInstance;
                Hashtable old = (Hashtable)oldInstance;
                Enumeration keys = h.keys();
                Vector pairs = new Vector();
                while(keys.hasMoreElements()) {
                    Object key = keys.nextElement(); 
                    Object value = h.get(key); 
                    Object oldValue = (old == null) ? null : old.get(key); 
                    // Note we use equals here instead of "==".  
                    // The "tabStop" key of a JTextField.document.documentProperties 
                    // is, by default, set to new instances of the Integer("8") - so 
                    // we need to equals if we are to avoid writing out these 
                    // default values. 
                    if (!value.equals(oldValue)) { 
                        pairs.addElement(new Object[]{".", uq(newInstance),
                                             new Object[]{"put", uq(key), uq(value)}});
                    }
                }

                Object[] result = new Object[pairs.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = pairs.elementAt(i);
                }
                return result;
            }
        }); 
        
        // DefaultListModel
        classToProperties.put(DefaultListModel.class, new ClassInfo(DefaultListModel.class) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                DefaultListModel m = (DefaultListModel)newInstance;
                Object[] result = new Object[m.getSize()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Object[]{".", uq(newInstance),
                                    new Object[]{"addElement", uq(m.getElementAt(i))}};
                }
                return result;
            }
        });

        // DefaultComboBoxModel
        classToProperties.put(DefaultComboBoxModel.class, new ClassInfo(DefaultComboBoxModel.class) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                DefaultComboBoxModel m = (DefaultComboBoxModel)newInstance;
                Object[] result = new Object[m.getSize()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Object[]{".", uq(newInstance),
                                    new Object[]{"addElement", uq(m.getElementAt(i))}};
                }
                return result;
            }
        });

        // Component (ActionListeners). 
        classToProperties.put(Component.class, new ClassInfo(Component.class, defaultClassInfo(Component.class)) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                Component c = (Component)newInstance; 
                Vector result = new Vector(); 
                
                // The "background", "foreground" and "font" properties. 
                String[] fieldNames = new String[]{"background", "foreground", "font"}; 
                for(int i = 0; i < fieldNames.length; i++) { 
                    String name = fieldNames[i]; 
                    Object newValue = getPrivateField(newInstance, Component.class, name); 
                    Object oldValue = (oldInstance == null) ? null : getPrivateField(oldInstance, Component.class, name); 
                    if (newValue != null && !newValue.equals(oldValue)) { 
                        // System.out.println("new " + newValue + " old " + oldValue); 
                        result.addElement(new Object[]{"=", new Object[]{".", uq(newInstance), name}, uq(newValue)});
                    }
                } 
                
                // Listeners
                EventListener[] l = c.getListeners(ActionListener.class);
                for (int i = 0; i < l.length; i++) {
                    result.addElement(new Object[]{".", uq(newInstance),
                                    new Object[]{"addActionListener", uq(l[i])}});
                }
                return result.toArray();
            }
        }); 

        // JComponent (minimumSize, preferredSize & maximumSize). 
        // Note the "size" methods in JComponent calculate default values 
        // when their values are null. In Kestrel the new "isPreferredSizeSet" 
        // family of methods can be used to disambiguate this situation.  
        // We use the private fields here so that the code will work with 
        // Kestrel beta. 
        classToProperties.put(JComponent.class, new ClassInfo(JComponent.class, defaultClassInfo(JComponent.class)) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                int statementCount = 0; 
                JComponent c = (JComponent)newInstance; 
                Vector result = new Vector(); 
                String[] fieldNames = new String[]{"minimumSize", "preferredSize", "maximumSize"}; 
                for(int i = 0; i < fieldNames.length; i++) { 
                    String name = fieldNames[i]; 
                    Object value = getPrivateField(c, JComponent.class, name); 
                    if (value != null) { 
                        result.addElement(new Object[]{"=", new Object[]{".", uq(newInstance), name}, uq(value)});
                    }
                }
                return result.toArray();
            }
        });
        
        // JLayeredPane/JPanel (Components). 
        // Pending, this should be added to Component not JPanel and JLayeredPane. 
        ClassInfo containerClassInfo = new ClassInfo(Container.class) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                Container newC = (Container)newInstance;
                Component[] newChildren = newC.getComponents();
                Container oldC = (Container)oldInstance;
                Component[] oldChildren = (oldC == null) ? new Component[0] : oldC.getComponents(); 
                // Pending. Assume all the old children are unaltered.  
                Object[] result = new Object[newChildren.length - oldChildren.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Object[]{".", uq(newInstance),
                                    new Object[]{"add", uq(newChildren[oldChildren.length + i])}};
                }
                return result;
            }
        };
        // We need to finish the differencing code before these can be added to Conponent.
        classToProperties.put(JPanel.class, containerClassInfo);
        classToProperties.put(JLayeredPane.class, containerClassInfo);

        // JTabbedPane
        classToProperties.put(JTabbedPane.class, new ClassInfo(JTabbedPane.class, defaultClassInfo(JTabbedPane.class)) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                JTabbedPane p = (JTabbedPane)newInstance;
                Object[] result = new Object[p.getTabCount()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Object[]{".", uq(newInstance),
                                    new Object[]{"addTab",
                                                 uq(p.getTitleAt(i)),
                                                 uq(p.getIconAt(i)),
                                                 uq(p.getComponentAt(i))}};
                }
                return result;
            }
        });

        // JMenuBar
        classToProperties.put(JMenuBar.class, new ClassInfo(JMenuBar.class, defaultClassInfo(JMenuBar.class)) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                JMenuBar m = (JMenuBar)newInstance;
                MenuElement[] c = m.getSubElements();
                Object[] result = new Object[c.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Object[]{".", uq(newInstance),
                                    new Object[]{"add", uq(c[i])}};
                }
                return result;
            }
        });

        // JMenu
        classToProperties.put(JMenu.class, new ClassInfo(JMenu.class, defaultClassInfo(JMenu.class)) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                JMenu m = (JMenu)newInstance;
                Component[] c = m.getMenuComponents();
                Object[] result = new Object[c.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Object[]{".", uq(newInstance),
                                    new Object[]{"add", uq(c[i])}};
                }
                return result;
            }
        });
        
        // BorderLayout
        classToProperties.put(BorderLayout.class, new ClassInfo(BorderLayout.class) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                BorderLayout newBL = (BorderLayout)newInstance;
                BorderLayout oldBL = (BorderLayout)oldInstance;
                Object[]    result = null;
                int count = 0;
        
                String[] locations = {"north", "south", "east", "west", "center"}; 
                String[] names = {BorderLayout.NORTH, BorderLayout.SOUTH, BorderLayout.EAST, BorderLayout.WEST, BorderLayout.CENTER}; 
                int n = locations.length;
                Component[] components = new Component[n]; 
                for (int i = 0; i < n; i++) { 
                    Component newC = (Component)getPrivateField(newBL, BorderLayout.class, locations[i]);
                    Component oldC = (Component)getPrivateField(oldBL, BorderLayout.class, locations[i]);
                    // Pending, assume any existing elements are OK. 
                    if (newC != null && oldC == null) {
                        components[i] = newC; 
                        count++;
                    }
                }
                result = new Object[count]; 
                count = 0; 
                for (int i = 0; i < n; i++) { 
                    if (components[i] != null) {
                        result[count++] = new Object[]{".", uq(newInstance),
                                                  new Object[]{"addLayoutComponent", uq(components[i]), uq(names[i])}};
                    }
                }
                return result; 
            }
        });
        
        // CardLayout
        classToProperties.put(CardLayout.class, new ClassInfo(CardLayout.class) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                CardLayout cl = (CardLayout)newInstance;
                Object[] result = null;
        
                //  Get a pointer to the CardLayout's internal hash table. Given a
                //  child it can be used to look up the "name" supplied by the app. Since we don't
                //  know who the children (or their parent) are, we use this table to
                //  enumerate them.

                Hashtable tab = (Hashtable)getPrivateField(cl, CardLayout.class, "tab");

                if (tab != null) {
                    int count = tab.size();
                    if (count > 0) {
                        int i = 0;
                        result = new Object[count];
                        for (Enumeration e=tab.keys(); e.hasMoreElements();) {
                            Component child = (Component)e.nextElement();
                            String  name = (String)tab.get(child);

                            result[i++] = new Object[]{".", uq(newInstance),
                                              new Object[]{"addLayoutComponent", uq(child), uq(name)}};
                        }
                    }
                }
        
                return result;
            }
        });

        // GridBagLayout
        classToProperties.put(GridBagLayout.class, new ClassInfo(GridBagLayout.class) {
            public Object[] getInitializer(Object newInstance, Object oldInstance) {
                GridBagLayout gbl = (GridBagLayout)newInstance;
                Object[] result = null;
        
                //  Get a pointer to the GridBagLayout's internal hash table. Given a
                //  child it can be used to look up the GridBagContraints. Since we don't
                //  know who the children (or their parent) are, we use this table to
                //  enumerate them.

                Hashtable comptable = (Hashtable)getPrivateField(gbl, GridBagLayout.class, "comptable");

                if (comptable != null) {
                    int count = comptable.size();

                    if (count > 0) {
                        int         i = 0;
                        result = new Object[count];
                        for (Enumeration e=comptable.keys(); e.hasMoreElements();) {
                            Component child = (Component)e.nextElement();
                            GridBagConstraints  gbc = (GridBagConstraints)comptable.get(child);

                            result[i++] = new Object[]{".", uq(newInstance),
                                              new Object[]{"addLayoutComponent", uq(child), uq(gbc)}};
                        }
                    }
                }
        
                return result;
            }
        });
        
        // Constructors.
        registerClassInfo(Insets.class, new String[]{"top", "left", "bottom", "right"}, null);
        registerClassInfo(Color.class, new String[]{"RGB"}, null);
        registerClassInfo(ColorUIResource.class, new String[]{"RGB"}, null);
        registerClassInfo(Font.class, new String[]{"name", "style", "size"}, null);
        registerClassInfo(FontUIResource.class, new String[]{"name", "style", "size"}, null);
        registerClassInfo(BoxLayout.class, new String[]{"target", "axis"}, null);

        registerClassInfo(DefaultCellEditor.class, new String[]{"component"}, null);

        // registerClassInfo(javax.swing.tree.DefaultTreeModel.class, new String[]{"root"}, null);
        registerClassInfo(javax.swing.tree.TreePath.class, new String[]{"path"}, null);

        /*
        This is required because the JSplitPane reveals a private layout class
        called BasicSplitPaneUI$BasicVerticalLayoutManager which changes with 
        the orientation. To avoid the necessity for instantiating it we cause 
        the orientation attribute to get set before the layout manager - that 
        way the layout manager will be changed as a side effect. Unfortunately, 
        the layout property belongs to the superclass and therefore precedes 
        the orientation property. PENDING - we need to allow this kind of 
        modification. For now, put the property in the constructor. 
        */
        registerClassInfo(JSplitPane.class, new String[]{"orientation"}, defaultClassInfo(JSplitPane.class));

        // Borders
        registerClassInfo(BevelBorder.class, new String[]{"bevelType", "highlightOuter", "highlightInner", "shadowOuter", "shadowInner"}, null);
        registerClassInfo(BorderUIResource.BevelBorderUIResource.class, new String[]{"bevelType", "highlightOuter", "highlightInner", "shadowOuter", "shadowInner"}, null);
        registerClassInfo(CompoundBorder.class, new String[]{"outsideBorder", "insideBorder"}, null);
        registerClassInfo(BorderUIResource.CompoundBorderUIResource.class, new String[]{"outsideBorder", "insideBorder"}, null);
        registerClassInfo(EmptyBorder.class, new String[]{"top", "left", "bottom", "right"}, null);
        registerClassInfo(BorderUIResource.EmptyBorderUIResource.class, new String[]{"top", "left", "bottom", "right"}, null);
        registerClassInfo(EtchedBorder.class, new String[]{"etchType", "highlight", "shadow"}, null);
        registerClassInfo(BorderUIResource.EtchedBorderUIResource.class, new String[]{"etchType", "highlight", "shadow"}, null);
        registerClassInfo(LineBorder.class, new String[]{"lineColor", "thickness"}, null);
        registerClassInfo(BorderUIResource.LineBorderUIResource.class, new String[]{"lineColor", "thickness"}, null);
        // Note this should check to see which of "color" and "tileIcon" is non-null.
        registerClassInfo(MatteBorder.class, new String[]{"top", "left", "bottom", "right", "tileIcon"}, null);
        registerClassInfo(BorderUIResource.MatteBorderUIResource.class, new String[]{"top", "left", "bottom", "right", "tileIcon"}, null);
        registerClassInfo(SoftBevelBorder.class, new String[]{"bevelType", "highlightOuter", "highlightInner", "shadowOuter", "shadowInner"}, null);
        registerClassInfo(TitledBorder.class, new String[]{"border", "title", "titleJustification", "titlePosition", "titleFont", "titleColor"}, null);
        registerClassInfo(BorderUIResource.TitledBorderUIResource.class, new String[]{"border", "title", "titleJustification", "titlePosition", "titleFont", "titleColor"}, null);

        // Introspection anomaly.
        getClassInfo(JComponent.class).addProperty(new Property(JComponent.class, "bounds"));

        getClassInfo(GridBagConstraints.class).addProperty("gridx");
        getClassInfo(GridBagConstraints.class).addProperty("gridy");
        getClassInfo(GridBagConstraints.class).addProperty("gridwidth");
        getClassInfo(GridBagConstraints.class).addProperty("gridheight");
        getClassInfo(GridBagConstraints.class).addProperty("weightx");
        getClassInfo(GridBagConstraints.class).addProperty("weighty");
        getClassInfo(GridBagConstraints.class).addProperty("anchor");
        getClassInfo(GridBagConstraints.class).addProperty("fill");
        getClassInfo(GridBagConstraints.class).addProperty("insets");
        getClassInfo(GridBagConstraints.class).addProperty("ipadx");
        getClassInfo(GridBagConstraints.class).addProperty("ipady");


        // Synthetics.
        // getClassInfo(JPanel.class).addProperty(new ContainerConstraints());

        // Removals
        getClassInfo(RectangularShape.class).removeProperty("frame");
        getClassInfo(Rectangle2D.class).removeProperty("frame");
        getClassInfo(Rectangle.class).removeProperty("frame");
        Class r2d = Rectangle2D.Double.class; 
        getClassInfo(r2d).removeProperty("frame");

        // These properties have platform specific implementations 
        // and should not appear in archives. 
        getClassInfo(ImageIcon.class).removeProperty("image");
        getClassInfo(ImageIcon.class).removeProperty("imageObserver");
        
        // This property throws a "not implemented" exception.
        getClassInfo(JMenuBar.class).removeProperty("helpMenu");

        // Because BorderLayout does not do differencing, 
        // this property makes the archives larger than they need to be. 
        // getClassInfo(JFrame.class).removeProperty("layout");

        // The color and font properties in Component need special treatment, see above.
        getClassInfo(Component.class).removeProperty("backgroundColor");
        getClassInfo(Component.class).removeProperty("foregroundColor");
        getClassInfo(Component.class).removeProperty("font"); 
        
        // The size properties in JComponent need special treatment, see above.
        getClassInfo(JComponent.class).removeProperty("minimumSize");
        getClassInfo(JComponent.class).removeProperty("preferredSize");
        getClassInfo(JComponent.class).removeProperty("maximumSize");

	// Ordering

        // selectionStart after the text itself.
        getClassInfo(JTextComponent.class).removeProperty("selectionStart");
        getClassInfo(JTextComponent.class).addProperty("selectionStart"); 
        
        // The caret property throws errors when it it set beyond 
        // the extent of the text. We could just set it after the 
        // text, but this is probably not something we want to archive anyway. 
        getClassInfo(JTextComponent.class).removeProperty("caret");
        getClassInfo(JTextComponent.class).removeProperty("caretPosition");

	// All selection information should come after the JTabbedPane is built
        getClassInfo(JTabbedPane.class).removeProperty("model");
        getClassInfo(JTabbedPane.class).addProperty("model");
        getClassInfo(JTabbedPane.class).removeProperty("selectedIndex");
        getClassInfo(JTabbedPane.class).addProperty("selectedIndex");
        getClassInfo(JTabbedPane.class).removeProperty("selectedComponent");
        getClassInfo(JTabbedPane.class).addProperty("selectedComponent");

        // The scroll bars in a JScrollPane are dynamic and should not 
        // be archived. The row and columns headers are changed by 
        // components like JTable on "addNotify". 
        getClassInfo(JScrollPane.class).removeProperty("verticalScrollBar");
        getClassInfo(JScrollPane.class).removeProperty("horizontalScrollBar");
        getClassInfo(JScrollPane.class).removeProperty("rowHeader");
        getClassInfo(JScrollPane.class).removeProperty("columnHeader");
        
        // Renderers need special treatment, since their properties 
        // change during rendering. 
        getClassInfo(JTableHeader.class).removeProperty("defaultRenderer");
        
        // The lead and anchor selection indexes are best ignored.
        // Selection is rarely something that should persist from 
        // development to deployment.
        getClassInfo(DefaultListSelectionModel.class).removeProperty("leadSelectionIndex");
        getClassInfo(DefaultListSelectionModel.class).removeProperty("anchorSelectionIndex");

        // The "icon" property must come after the "disabledIcon" property.
        getClassInfo(AbstractButton.class).removeProperty("icon");
        getClassInfo(AbstractButton.class).addProperty("icon");
    }

    public ClassInfo getClassInfo(Class type) {
        // System.out.println("getClassInfo: " + type + " " + isPrimitive(type));
        ClassInfo info = (ClassInfo)classToProperties.get(type);
        if (info != null) {
            return info;
        }
        else {
            if (type.isArray()) {
                classToProperties.put(type, arrayClassInfo(type));
            }
            else if (isPrimitive(type)) {
                classToProperties.put(type, primitiveClassInfo(type));
            }
            else if (Proxy.isProxyClass(type)) {
                classToProperties.put(type, proxyClassInfo(type));
            }
            else {
                registerClassInfo(type, defaultClassInfo(type));
            }
            return (ClassInfo)classToProperties.get(type);
        }
    }

    private String[] defaultClassInfo(Class type) {
        try {
            BeanInfo info = Introspector.getBeanInfo(type);
            PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
            Vector vector = new Vector(propertyDescriptors.length);

            for (int i = 0; i < propertyDescriptors.length; ++i ) {
                PropertyDescriptor descriptor = propertyDescriptors[i];
                Method reader = descriptor.getReadMethod();
                Method writer = descriptor.getWriteMethod();

                if ( reader != null && writer != null &&
                     reader.getDeclaringClass() == type &&
                     writer.getDeclaringClass() == type) {
                    vector.addElement( descriptor.getName() );
                }
            }

            if (!Modifier.isPublic(type.getModifiers())) {
                for ( int i = 0; i < vector.size(); ++i ) {
                    // System.out.println("Discarding property: " + vector.elementAt(i) + " of " + type);
                }
		vector.removeAllElements();
            }

            String[] array = new String[vector.size()];
            // System.out.println( type.getName() );
            for ( int i = 0; i < vector.size(); ++i ) {
                array[i] = (String)vector.elementAt( i );
                // System.out.println( array[i] );
            }
            return array;
        }
        catch ( Exception e ) {
        }
	return null;
    }

    private void registerClassInfo(Class type, String[] properties) {
        classToProperties.put(type, new ClassInfo(type, properties));
    }

    private void registerClassInfo(Class type, String[] constructor, String[] properties) {
        classToProperties.put(type, new ClassInfo(type, constructor, properties));
    }

    private Enumeration append(Enumeration e1, Enumeration e2) {
        return (e1 == null) ? e2 : (e2 == null) ? e1 : new Join(e1, e2);
    }

    public Enumeration getProperties(Object node) {
        Class type = node.getClass();
        Enumeration result = null;
        for(; type != null; type = type.getSuperclass()) {
            ClassInfo info = getClassInfo(type);
            result = append((info == null) ? null : info.getProperties().elements(), result);
        }
        return result;
    }

    private Object[] appendArrays(Object[] a , Object[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        Object[] result = new Object[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public Object[] getInitializer(Object newInstance, Object oldInstance) {
        Class type = newInstance.getClass();
        Object[] result = null;
        for(; type != null; type = type.getSuperclass()) {
            ClassInfo info = getClassInfo(type);
            result = appendArrays(info.getInitializer(newInstance, oldInstance), result);
        }
        return result;
    }

    private static boolean isPrimitive(Class type) {
        return (type == String.class) ||
                type == Boolean.class ||
                Number.class.isAssignableFrom(type);
    }
}

