package bluej.stride.framedjava.errors;

import java.io.File;

import bluej.collect.DiagnosticWithShown;
import bluej.compiler.CompilerAPICompiler;
import bluej.compiler.Diagnostic;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.StringSlotFragment;
import bluej.stride.slots.EditableSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

import javafx.application.Platform;

/**
 * A class for errors which are directly targeted at a given slot,
 * like our own unknown-variable errors or extra semi-colon error
 * (as distinct from JavaCompileError, which is targeted at Java code, which maps to a slot)
 */
public abstract class DirectSlotError extends CodeError
{
    private final int identifier;

    public DirectSlotError(SlotFragment code)
    {
        super(code);
        this.identifier = CompilerAPICompiler.getNewErrorIdentifer();
    }

    @Override
    @OnThread(Tag.Any)
    public int getIdentifier()
    {
        return identifier;
    }

    @OnThread(Tag.Any)
    public synchronized DiagnosticWithShown toDiagnostic(String javaFileName, File strideFileName)
    {
        final Diagnostic diagnostic = new Diagnostic(Diagnostic.ERROR, getMessage(), javaFileName, -1, -1, -1, -1, getIdentifier());
        diagnostic.setXPath(path, -1, -1);
        return new DiagnosticWithShown(diagnostic, false, strideFileName);
    }
}
