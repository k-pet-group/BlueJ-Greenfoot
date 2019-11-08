/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2019  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.Reflective;
import bluej.parser.ImportsCollection;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.utility.JavaNames;

import java.util.List;


/**
 * A parsed compilation unit node.
 * 
 * @author Davin McCall
 */
public class ParsedCUNode extends IncrementalParsingNode
{
    private final EntityResolver parentResolver;
    private final ImportsCollection imports = new ImportsCollection();

    private int size = 0;
    
    /**
     * Construct a parsed node for as yet unknown source.
     */
    public ParsedCUNode(EntityResolver parentResolver)
    {
        super(null);
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
    
    @Override
    public void setSize(int newSize)
    {
        size = newSize;
    }
    
    @Override
    protected int doPartialParse(ParseParams params, int state)
    {
        last = params.tokenStream.LA(1);
       
        if (checkBoundary(params, last)) {
            return PP_PULL_UP_CHILD;
        }
        
        params.parser.parseCUpart(state);
        return PP_OK;
    };
    
    @Override
    protected boolean isDelimitingNode(NodeAndPosition<ParsedNode> nap)
    {
        // All node types: package statement, import (Inner), type definition,
        // are all delimiting nodes. Only a comment is not.
        int nt = nap.getNode().getNodeType();
        return nt != ParsedNode.NODETYPE_COMMENT;
    }
    
    @Override
    protected boolean isNodeEndMarker(int tokenType)
    {
        return false;
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
    
    @Override
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource)
    {
        PackageOrClass poc = super.resolvePackageOrClass(name, querySource);
        if (poc == null) {
            poc = imports.getTypeImport(name);
        }
        if (poc == null && parentResolver != null && querySource != null) {
            String prefix = JavaNames.getPrefix(querySource.getName());
            String fqName = JavaNames.combineNames(prefix, name);
            poc = parentResolver.resolveQualifiedClass(fqName);
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
    
//    public static void printTree(ParsedNode node, int nodepos, int indent)
//    {
//        for (int i = 0; i < indent; i++) {
//            System.out.print("  ");
//        }
//        System.out.println("Node=" + node + " pos=" + nodepos + " size=" + node.getSize());
//        for (Iterator<NodeAndPosition<ParsedNode>> i = node.getChildren(nodepos); i.hasNext(); ) {
//            NodeAndPosition<ParsedNode> nap = i.next();
//            printTree(nap.getNode(), nap.getPosition(), indent+1);
//        }
//    }
}
