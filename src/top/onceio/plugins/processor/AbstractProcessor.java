package top.onceio.plugins.processor;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.plugins.config.ConfigKey;
import top.onceio.plugins.config.OnceIOConfigDiscovery;
import top.onceio.plugins.util.OnceIOProcessorUtil;
import top.onceio.plugins.util.PsiAnnotationUtil;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Base onceio processor class
 *
 * @author Liar
 */
public abstract class AbstractProcessor implements Processor {
  /**
   * Annotation classes this processor supports
   */
  private final Class<? extends Annotation>[] supportedAnnotationClasses;
  /**
   * Kind of output elements this processor supports
   */
  private final Class<? extends PsiElement> supportedClass;
  /**
   * Instance of config discovery service to access onceio.config informations
   */
  protected final OnceIOConfigDiscovery onceIOConfigDiscovery;

  /**
   * Constructor for all OnceIO-Processors
   *  @param supportedClass           kind of output elements this processor supports
   * @param supportedAnnotationClass annotation this processor supports
   */
  @SuppressWarnings("unchecked")
  protected AbstractProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                              @NotNull Class<? extends Annotation> supportedAnnotationClass) {
    this.onceIOConfigDiscovery = OnceIOConfigDiscovery.getInstance();
    this.supportedClass = supportedClass;
    this.supportedAnnotationClasses = new Class[]{supportedAnnotationClass};
  }

  /**
   * Constructor for all OnceIO-Processors
   *  @param supportedClass            kind of output elements this processor supports
   * @param supportedAnnotationClass  annotation this processor supports
   * @param equivalentAnnotationClass another equivalent annotation
   */
  @SuppressWarnings("unchecked")
  protected AbstractProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                              @NotNull Class<? extends Annotation> supportedAnnotationClass,
                              @NotNull Class<? extends Annotation> equivalentAnnotationClass) {
    this.onceIOConfigDiscovery = OnceIOConfigDiscovery.getInstance();
    this.supportedClass = supportedClass;
    this.supportedAnnotationClasses = new Class[]{supportedAnnotationClass, equivalentAnnotationClass};
  }

  /**
   * Constructor for all OnceIO-Processors
   * @param supportedClass                  kind of output elements this processor supports
   * @param supportedAnnotationClass        annotation this processor supports
   * @param oneEquivalentAnnotationClass    another equivalent annotation
   * @param secondEquivalentAnnotationClass another equivalent annotation
   */
  @SuppressWarnings("unchecked")
  AbstractProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                    @NotNull Class<? extends Annotation> supportedAnnotationClass,
                    @NotNull Class<? extends Annotation> oneEquivalentAnnotationClass,
                    @NotNull Class<? extends Annotation> secondEquivalentAnnotationClass) {
    this.onceIOConfigDiscovery = OnceIOConfigDiscovery.getInstance();
    this.supportedClass = supportedClass;
    this.supportedAnnotationClasses = new Class[]{supportedAnnotationClass, oneEquivalentAnnotationClass, secondEquivalentAnnotationClass};
  }

  @NotNull
  public final Class<? extends Annotation>[] getSupportedAnnotationClasses() {
    return supportedAnnotationClasses;
  }

  @NotNull
  @Override
  public final Class<? extends PsiElement> getSupportedClass() {
    return supportedClass;
  }

  @NotNull
  public abstract Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass);

  protected boolean supportAnnotationVariant(@NotNull PsiAnnotation psiAnnotation) {
    return true;
  }

  protected boolean readAnnotationOrConfigProperty(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass,
                                                          @NotNull String annotationParameter, @NotNull ConfigKey configKey) {
    final boolean result;
    final Boolean declaredAnnotationValue = PsiAnnotationUtil.getDeclaredBooleanAnnotationValue(psiAnnotation, annotationParameter);
    if (null == declaredAnnotationValue) {
      result = onceIOConfigDiscovery.getBooleanOnceIOConfigProperty(configKey, psiClass);
    } else {
      result = declaredAnnotationValue;
    }
    return result;
  }

  protected static void addOnXAnnotations(@Nullable PsiAnnotation processedAnnotation,
                                          @NotNull PsiModifierList modifierList,
                                          @NotNull String onXParameterName) {
    if (processedAnnotation == null) {
      return;
    }

    Collection<String> annotationsToAdd = OnceIOProcessorUtil.getOnX(processedAnnotation, onXParameterName);
    for (String annotation : annotationsToAdd) {
      modifierList.addAnnotation(annotation);
    }
  }

  public OnceIOPsiElementUsage checkFieldUsage(@NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation) {
    return OnceIOPsiElementUsage.NONE;
  }

}
