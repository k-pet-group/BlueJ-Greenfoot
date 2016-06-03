package bluej.stride.framedjava.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import bluej.stride.framedjava.elements.CodeElement;

/**
 * Created by neil on 03/06/2016.
 */
class TryBuilder
{
    final List<CodeElement> tryContent = new ArrayList<>();
    final Stack<List<String>> catchTypes = new Stack<>();
    final List<String> catchNames = new ArrayList<>();
    final List<List<CodeElement>> catchBlocks = new ArrayList<>();
    List<CodeElement> finallyContents = null;
}
