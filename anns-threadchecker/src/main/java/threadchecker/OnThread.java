package threadchecker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating which thread a method should be called on.  Here are the rules
 * for applying the annotation:
 * 
 * - If the annotation is on a package, all classes in that package are considered to have
 *   the annotation.  Any annotation on a class overrides the package-wide annotation.
 * - If the annotations is on a class (directly, or via a package), all methods in that class
 *   are considered to have that annotation.  An annotation on a method overrides a class-wide
 *   annotation.
 * - If the annotation is on a method, it applies to that method.
 * 
 * So, the rules for finding an annotation on a method are: first check the method.  If that
 * is missing, check the class.  If that is missing, check the package.  Annotations on outer
 * classes are independent of those on inner classes (but both are affected by the package).
 * 
 * Once you have the rules for an annotation on a method, you can check them.  For each
 * method call to dest() from src():
 *   - If dest() is OnThread(X), then src must be as well.
 *   - If dest() in class Child is OnThread(X), and overrides dest() in class Parent,
 *     then dest() in Parent should also be OnThread(X) unless you specifically
 *     accept this situation in Child (TODO make a mechanism for this)
 *   - If dest() in class Parent is OnThread(X), then dest() in class Child is taken to be
 *     declare OnThread(X).  Declaring any conflicting OnThread annotation is an error.
 *
 */

@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.CLASS)
public @interface OnThread {
    Tag value();
    boolean ignoreParent() default false;
    boolean requireSynchronized() default false;
}
