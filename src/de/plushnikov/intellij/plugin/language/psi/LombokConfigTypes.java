package de.plushnikov.intellij.plugin.language.psi;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;

public class LombokConfigTypes  {
    public static final IElementType KEY = new IElementType("KEY",Language.ANY);
    public static final IElementType VALUE = new IElementType("VALUE",Language.ANY);
    public static final IElementType COMMENT = new IElementType("COMMENT",Language.ANY);
}
