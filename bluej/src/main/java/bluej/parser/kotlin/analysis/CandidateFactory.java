/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.parser.kotlin.analysis;

import java.util.ArrayList;
import java.util.List;

import bluej.parser.AssistContent.Access;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate.JavaMember;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate.Parameter;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities;
import org.jetbrains.kotlin.descriptors.DescriptorVisibility;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities;
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement;
import org.jetbrains.kotlin.load.java.structure.JavaArrayType;
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType;
import org.jetbrains.kotlin.load.java.structure.JavaMethod;
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.kotlin.types.FlexibleTypesKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.SimpleType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeProjectionImpl;
import org.jetbrains.kotlin.types.TypeSubstitutionKt;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Converts analysis descriptors into {@link KotlinCompletionCandidate}s, capturing everything later
 * display and insertion need while the analysis is still valid.
 */
@OnThread(Tag.Worker)
final class CandidateFactory
{
    private CandidateFactory()
    {
    }

    /**
     * Describe a function, property, local variable or parameter.
     *
     * @param descriptor the declaration
     * @param prefix     text to show and insert before the name, or empty
     * @return the candidate
     */
    static KotlinCompletionCandidate callable(CallableDescriptor descriptor, String prefix)
    {
        CompletionKind kind;
        List<Parameter> parameters = null;
        if (descriptor instanceof FunctionDescriptor function) {
            kind = CompletionKind.METHOD;
            parameters = function.getValueParameters().stream().map(CandidateFactory::parameter).toList();
        }
        else if (descriptor instanceof ValueParameterDescriptor) {
            kind = CompletionKind.FORMAL_PARAM;
        }
        else if (descriptor instanceof PropertyDescriptor) {
            kind = CompletionKind.FIELD;
        }
        else {
            kind = CompletionKind.LOCAL_VAR;
        }
        DeclarationDescriptor declaration = original(descriptor);
        return new KotlinCompletionCandidate(prefix, descriptor.getName().asString(), kind,
            renderType(descriptor.getReturnType()), parameters, declaringClass(declaration),
            access(descriptor.getVisibility()), kdoc(declaration), javaMember(descriptor));
    }

    /**
     * Describe a Java getter as the Kotlin property it can be used as ({@code getWorld()} as {@code world}).
     *
     * @param getter       the Java getter
     * @param propertyName the property name
     * @param prefix       text to show and insert before the name, or empty
     * @return the candidate
     */
    static KotlinCompletionCandidate javaGetterProperty(FunctionDescriptor getter, String propertyName, String prefix)
    {
        DeclarationDescriptor declaration = original(getter);
        return new KotlinCompletionCandidate(prefix, propertyName, CompletionKind.FIELD,
            renderType(getter.getReturnType()), null, declaringClass(declaration), access(getter.getVisibility()),
            null, javaMember(getter));
    }

    /**
     * Describe an enum entry, which is used like a property of its enum class.
     *
     * @param entry the enum entry
     * @return the candidate
     */
    static KotlinCompletionCandidate enumEntry(ClassDescriptor entry)
    {
        DeclarationDescriptor enumClass = entry.getContainingDeclaration();
        return new KotlinCompletionCandidate("", entry.getName().asString(), CompletionKind.FIELD,
            enumClass.getName().asString(), null, declaringClass(entry), Access.PUBLIC, kdoc(entry), null);
    }

    /**
     * Render a type for display, using short names. Java platform types are shown in their non-null
     * form ({@code World} rather than {@code World!}), which is easier to read.
     *
     * @param type the type, or null
     * @return the rendered type, or null
     */
    static String renderType(KotlinType type)
    {
        return type == null ? null : DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(withoutPlatformTypes(type));
    }

    private static KotlinType withoutPlatformTypes(KotlinType type)
    {
        SimpleType simple = FlexibleTypesKt.lowerIfFlexible(type);
        if (simple.getArguments().isEmpty()) {
            return simple;
        }
        List<TypeProjection> arguments = new ArrayList<>();
        for (TypeProjection argument : simple.getArguments()) {
            arguments.add(argument.isStarProjection() ? argument
                : new TypeProjectionImpl(argument.getProjectionKind(), withoutPlatformTypes(argument.getType())));
        }
        return TypeSubstitutionKt.replace(simple, arguments);
    }

    private static Parameter parameter(ValueParameterDescriptor parameter)
    {
        KotlinType elementType = parameter.getVarargElementType();
        boolean vararg = elementType != null;
        KotlinType type = vararg ? elementType : parameter.getType();
        boolean optional = vararg || DescriptorUtilsKt.declaresOrInheritsDefaultValue(parameter);
        return new Parameter(parameter.getName().asString(), renderType(type), optional, vararg,
            FunctionTypesKt.isBuiltinFunctionalType(type));
    }

    /**
     * Find the real declaration behind a member seen through a subclass or a generic instantiation.
     *
     * @param descriptor a declaration, possibly an inherited or substituted copy
     * @return the original declaration
     */
    static DeclarationDescriptor original(DeclarationDescriptor descriptor)
    {
        if (descriptor instanceof CallableMemberDescriptor member) {
            return DescriptorUtils.unwrapFakeOverride(member).getOriginal();
        }
        return descriptor.getOriginal();
    }

    private static String declaringClass(DeclarationDescriptor declaration)
    {
        DeclarationDescriptor container = declaration.getContainingDeclaration();
        return container instanceof ClassDescriptor ? DescriptorUtils.getFqNameSafe(container).asString() : null;
    }

    private static Access access(DescriptorVisibility visibility)
    {
        if (visibility == DescriptorVisibilities.LOCAL) {
            return null;
        }
        if (DescriptorVisibilities.isPrivate(visibility)) {
            return Access.PRIVATE;
        }
        if (visibility == DescriptorVisibilities.PROTECTED || visibility == JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
                || visibility == JavaDescriptorVisibilities.PROTECTED_AND_PACKAGE) {
            return Access.PROTECTED;
        }
        if (visibility == JavaDescriptorVisibilities.PACKAGE_VISIBILITY) {
            return Access.PACKAGE;
        }
        // Public, and internal (visible throughout the module, i.e. the whole project).
        return Access.PUBLIC;
    }

    private static String kdoc(DeclarationDescriptor declaration)
    {
        PsiElement source = DescriptorToSourceUtils.descriptorToDeclaration(declaration);
        if (source instanceof KtDeclaration ktDeclaration && ktDeclaration.getDocComment() != null) {
            return ktDeclaration.getDocComment().getText();
        }
        return null;
    }

    private static JavaMember javaMember(CallableDescriptor descriptor)
    {
        DeclarationDescriptor declaration = original(descriptor);
        if (declaration instanceof FunctionDescriptor function
                && function.getSource() instanceof JavaSourceElement source
                && source.getJavaElement() instanceof JavaMethod method) {
            List<String> parameterTypes = method.getValueParameters().stream().map(p -> erasure(p.getType())).toList();
            return new JavaMember(DescriptorUtils.getFqNameSafe(function.getContainingDeclaration()).asString(),
                method.getName().asString(), parameterTypes);
        }
        return null;
    }

    // The JVM erasure of a Java type, as written in Java source (e.g. "int", "java.lang.String[]").
    private static String erasure(JavaType type)
    {
        if (type instanceof JavaPrimitiveType primitive) {
            return primitive.getType() == null ? "void" : JvmPrimitiveType.get(primitive.getType()).getJavaKeywordName();
        }
        if (type instanceof JavaArrayType array) {
            return erasure(array.getComponentType()) + "[]";
        }
        if (type instanceof JavaClassifierType classifierType) {
            if (classifierType.getClassifier() instanceof JavaTypeParameter typeParameter) {
                return typeParameter.getUpperBounds().stream().findFirst().map(CandidateFactory::erasure)
                    .orElse("java.lang.Object");
            }
            return classifierType.getClassifierQualifiedName();
        }
        // Wildcards only occur as type arguments, which erasure discards.
        return "java.lang.Object";
    }
}
