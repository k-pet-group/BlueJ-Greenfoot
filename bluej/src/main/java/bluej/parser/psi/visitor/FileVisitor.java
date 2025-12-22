/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
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
package bluej.parser.psi.visitor;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.psi.JavaParserCallbacksAdapter;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.psi.*;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI visitor for file-level constructs only.
 *
 * <h2>Architectural Role (Phase 3: Visitor Separation)</h2>
 * <p>FileVisitor handles ONLY top-level file constructs:</p>
 * <ul>
 *   <li>File traversal (visitKtFile)</li>
 *   <li>Top-level class/interface/enum declarations (header only, body delegated)</li>
 *   <li>Top-level function declarations</li>
 *   <li>Top-level property declarations</li>
 *   <li>Object declarations (header only, body delegated)</li>
 * </ul>
 *
 * <h2>Context-Aware Design (CRITICAL)</h2>
 * <p><b>FileVisitor KNOWS it is handling file-level constructs.</b></p>
 * <ul>
 *   <li>Top-level functions use {@code callbacks.endDecl()} - NOT {@code endMethodDecl()}</li>
 *   <li>Class body processing is delegated to {@link ClassVisitor}</li>
 *   <li><b>NO</b> {@code PsiTreeUtil.getParentOfType()} calls to detect context</li>
 * </ul>
 *
 * <h2>Delegation Pattern</h2>
 * <pre>{@code
 * // FileVisitor handles class HEADER, then delegates BODY to ClassVisitor:
 * @Override
 * public void visitClass(KtClass ktClass) {
 *     // ... process class header (modifiers, name, supertypes) ...
 *
 *     // Delegate body processing to ClassVisitor
 *     ClassVisitor classVisitor = new ClassVisitor(callbacks);
 *     for (KtDeclaration decl : body.getDeclarations()) {
 *         decl.accept(classVisitor);
 *     }
 * }
 * }</pre>
 *
 * <h2>What FileVisitor Does NOT Handle</h2>
 * <ul>
 *   <li>Class member methods - handled by {@link ClassVisitor}</li>
 *   <li>Class member properties/fields - handled by {@link ClassVisitor}</li>
 *   <li>Constructors (primary/secondary) - handled by {@link ClassVisitor}</li>
 *   <li>Init blocks - handled by {@link ClassVisitor}</li>
 *   <li>Statements and expressions - handled by {@link MethodBodyVisitor}</li>
 * </ul>
 *
 * @see ClassVisitor Visitor for class-level member declarations
 * @see MethodBodyVisitor Visitor for method body statements/expressions
 * @see BaseVisitor Base class with context-agnostic helper methods
 */
public class FileVisitor extends BaseVisitor {
    /**
     * Creates a new file visitor for top-level constructs.
     *
     * @param callbacks The callback adapter for parser integration (must not be null)
     */
    public FileVisitor(JavaParserCallbacksAdapter callbacks) {
        super(callbacks);
    }

    @Override
    public void visitKtFile(@NotNull KtFile file) {
        // Phase 2: Log file visit
        String fileName = file.getName();

        // TODO: no package statements, so let's assume 1
        callbacks.reachedCUstate(1);

        // Explicitly visit all declarations in the file
        // Note: Kotlin PSI visitor requires explicit iteration over children
        for (KtDeclaration declaration : file.getDeclarations()) {
            // TODO: pick correct visitor
            var visitor = switch (declaration) {
                case KtClass ignored -> new ClassVisitor(callbacks);
                case KtFunction ignored -> new FunctionVisitor(callbacks, true);
                default -> new KtVisitorVoid();
//                default -> throw new UnsupportedOperationException("Declaration " + declaration + " not supported yet");
            };

            declaration.accept(visitor);
        }

        // TODO: hack
        var lastToken = getLastToken();

        if (lastToken != null && (lastToken.getType() != JavaTokenTypes.EOF && lastToken.getType() != JavaTokenTypes.LCURLY)) {
            callbacks.finishedCU(2); /// who the hell knows what that means xD
        }
    }
}
