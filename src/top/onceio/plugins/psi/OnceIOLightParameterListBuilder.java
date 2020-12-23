package top.onceio.plugins.psi;

import com.intellij.lang.Language;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightParameterListBuilder;

import java.util.Arrays;

public class OnceIOLightParameterListBuilder extends LightParameterListBuilder {
  public OnceIOLightParameterListBuilder(PsiManager manager, Language language) {
    super(manager, language);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OnceIOLightParameterListBuilder that = (OnceIOLightParameterListBuilder) o;

    if (getParametersCount() != that.getParametersCount()) {
      return false;
    }

    return Arrays.equals(getParameters(), that.getParameters());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(getParameters());
  }
}
