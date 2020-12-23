package top.onceio.plugins.processor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import top.onceio.plugins.problem.OnceIOProblem;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Liar
 */
public interface Processor {
  @NotNull
  Class<? extends Annotation>[] getSupportedAnnotationClasses();

  @NotNull
  Class<? extends PsiElement> getSupportedClass();

  @NotNull
  Collection<OnceIOProblem> verifyAnnotation(@NotNull PsiAnnotation psiAnnotation);

  default boolean isEnabled(@NotNull PropertiesComponent propertiesComponent) {
    return true;
  }

  @NotNull
  default List<? super PsiElement> process(@NotNull PsiClass psiClass) {
    return Collections.emptyList();
  }

  OnceIOPsiElementUsage checkFieldUsage(@NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation);

}
