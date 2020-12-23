package top.onceio.plugins.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightReferenceListBuilder;

public class OnceIOLightReferenceListBuilder extends LightReferenceListBuilder {

  public OnceIOLightReferenceListBuilder(PsiManager manager, Language language, Role role) {
    super(manager, language, role);
  }

  @Override
  public TextRange getTextRange() {
    TextRange r = super.getTextRange();
    return r == null ? TextRange.EMPTY_RANGE : r;
  }
}
