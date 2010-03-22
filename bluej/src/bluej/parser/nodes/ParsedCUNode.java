/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.nodes;

import java.util.Iterator;
import java.util.List;

import javax.swing.text.Document;

import bluej.debugger.gentype.Reflective;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.EditorParser;
import bluej.parser.ImportsCollection;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;


/**
 * A parsed compilation unit node.
 * 
 * @author Davin McCall
 */
public class ParsedCUNode extends ParentParsedNode
{
    private EntityResolver parentResolver;
    private ImportsCollection imports = new ImportsCollection();

    private int size = 0;
    
    /**
     * Construct a parsed node for as yet unknown source.
     */
    public ParsedCUNode()
    {
    }
    
    /**
     * Construct a parsed node for the given document.
     */
    public ParsedCUNode(Document document)
    {
        size = document.getLength();
    }
    
    /**
     * Set the entity resolver used to resolve symbols.
     */
    public void setParentResolver(EntityResolver parentResolver)
    {
        this.parentResolver = parentResolver;
    }

    public ImportsCollection getImports()
    {
        return imports;
    }
    
    public EntityResolver getParentResolver()
    {
        return parentResolver;
    }
    
    /**
     * Overridden getSize() which returns the document size.
     * 
     * @see bluej.parser.nodes.ParsedNode#getSize()
     */
    public int getSize()
    {
        return size;
    }

    @Override
    public void resize(int newSize)
    {
        size = newSize;
    }
    
    /**
     * Reparse this node from the specified offset.
     */
    @Override
    protected void reparseNode(Document document, int nodePos, int offset, NodeStructureListener listener)
    {
        doReparse(document, 0, offset, listener);
    }
    
    protected void doReparse(Document document, int nodePos, int pos, NodeStructureListener listener)
    {
        clearNode(this, listener);
        size = document.getLength();
        
        EditorParser parser = new EditorParser(document);
        parser.parseCU(this);
	    
        ((MoeSyntaxDocument) document).documentChanged();
    }
    
    /**
     * Remove all subnodes from the given node.
     */
    private void clearNode(ParsedNode node, NodeStructureListener listener)
    {
        Iterator<NodeAndPosition> i = node.getChildren(0);
        while (i.hasNext()) {
            NodeAndPosition nap = i.next();
            clearNode(nap.getNode(), listener);
            listener.nodeRemoved(nap);
        }
        node.getNodeTree().clear();
    }
    
    @Override
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
    {
        PackageOrClass poc = super.resolvePackageOrClass(name, querySource);
        if (poc == null) {
            poc = imports.getTypeImport(name);
        }
        if (poc == null) {
            poc = imports.getTypeImportWC(name);
        }
        if (poc == null && parentResolver != null) {
            // Implicit "import java.lang.*"
            poc = parentResolver.resolveQualifiedClass("java.lang." + name);
        }
        if (poc == null && parentResolver != null) {
            return parentResolver.resolvePackageOrClass(name, querySource);
        }
        return poc;
    }
    
    @Override
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        // We may have static imports
        
        List<JavaEntity> simports = imports.getStaticImports(name);
        for (JavaEntity importType : simports) {
            importType = importType.resolveAsType();
            if (importType == null) {
                continue;
            }
            JavaEntity subEnt = importType.getSubentity(name, querySource);
            if (subEnt != null) {
                JavaEntity value = subEnt.resolveAsValue();
                if (value != null) {
                    return value;
                }
            }
        }
        
        simports = imports.getStaticWildcardImports();
        for (JavaEntity importType : simports) {
            importType = importType.resolveAsType();
            if (importType == null) {
                continue;
            }
            JavaEntity subEnt = importType.getSubentity(name, querySource);
            if (subEnt != null) {
                JavaEntity value = subEnt.resolveAsValue();
                if (value != null) {
                    return value;
                }
            }
        }
        
        return resolvePackageOrClass(name, querySource);
    }
    
    @Override
    public TypeEntity resolveQualifiedClass(String name)
    {
        if (parentResolver != null) {
            return parentResolver.resolveQualifiedClass(name);
        }
        return null;
    }
    
//    private static void printTree(ParsedNode node, int nodepos, int indent)
//    {
//        for (int i = 0; i < indent; i++) {
//            System.out.print("  ");
//        }
//        System.out.println("Node=" + node + " pos=" + nodepos + " size=" + node.getSize());
//        for (Iterator<NodeAndPosition> i = node.getChildren(nodepos); i.hasNext(); ) {
//            NodeAndPosition nap = i.next();
//            printTree(nap.getNode(), nap.getPosition(), indent+1);
//        }
//    }
}
