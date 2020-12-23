package top.onceio.plugins.processor;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.plugins.problem.OnceIOProblem;
import top.onceio.plugins.problem.ProblemBuilder;
import top.onceio.plugins.problem.ProblemEmptyBuilder;
import top.onceio.plugins.problem.ProblemNewBuilder;
import top.onceio.plugins.psi.OnceIOLightClassBuilder;
import top.onceio.plugins.util.PsiAnnotationSearchUtil;
import top.onceio.plugins.util.PsiClassUtil;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Base onceio processor class for class annotations
 *
 * @author Liar
 */
public abstract class AbstractClassProcessor extends AbstractProcessor {

    protected AbstractClassProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                                     @NotNull Class<? extends Annotation> supportedAnnotationClass) {
        super(supportedClass, supportedAnnotationClass);
    }

    protected AbstractClassProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                                     @NotNull Class<? extends Annotation> supportedAnnotationClass,
                                     @NotNull Class<? extends Annotation> equivalentAnnotationClass) {
        super(supportedClass, supportedAnnotationClass, equivalentAnnotationClass);
    }

    @NotNull
    @Override
    public List<? super PsiElement> process(@NotNull PsiClass psiClass) {
        List<? super PsiElement> result = Collections.emptyList();

        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiClass, getSupportedAnnotationClasses());
        if (null != psiAnnotation) {
            if (supportAnnotationVariant(psiAnnotation) && validate(psiAnnotation, psiClass, ProblemEmptyBuilder.getInstance())) {
                result = new ArrayList<>();
                generatePsiElements(psiClass, psiAnnotation, result);
            }
        }
        return result;
    }

    @NotNull
    public Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass) {
        Collection<PsiAnnotation> result = new ArrayList<>();
        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiClass, getSupportedAnnotationClasses());
        if (null != psiAnnotation) {
            result.add(psiAnnotation);
        }
        return result;
    }

    protected void addClassAnnotation(Collection<PsiAnnotation> result, @NotNull PsiClass psiClass, String... annotationFQNs) {
        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiClass, annotationFQNs);
        if (null != psiAnnotation) {
            result.add(psiAnnotation);
        }
    }

    protected void addFieldsAnnotation(Collection<PsiAnnotation> result, @NotNull PsiClass psiClass, String... annotationFQNs) {
        for (PsiField psiField : PsiClassUtil.collectClassFieldsIntern(psiClass)) {
            PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, annotationFQNs);
            if (null != psiAnnotation) {
                result.add(psiAnnotation);
            }
        }
    }

    @NotNull
    @Override
    public Collection<OnceIOProblem> verifyAnnotation(@NotNull PsiAnnotation psiAnnotation) {
        Collection<OnceIOProblem> result = Collections.emptyList();
        // check first for fields, methods and filter it out, because PsiClass is parent of all annotations and will match other parents too
        @SuppressWarnings("unchecked")
        PsiElement psiElement = PsiTreeUtil.getParentOfType(psiAnnotation, PsiField.class, PsiMethod.class, PsiClass.class);
        if (psiElement instanceof PsiClass) {
            ProblemNewBuilder problemNewBuilder = new ProblemNewBuilder();
            validate(psiAnnotation, (PsiClass) psiElement, problemNewBuilder);
            result = problemNewBuilder.getProblems();
        }

        return result;
    }

    protected Optional<PsiClass> getSupportedParentClass(@NotNull PsiClass psiClass) {
        final PsiElement parentElement = psiClass.getParent();
        if (parentElement instanceof PsiClass && !(parentElement instanceof OnceIOLightClassBuilder)) {
            return Optional.of((PsiClass) parentElement);
        }
        return Optional.empty();
    }

    @Nullable
    protected PsiAnnotation getSupportedAnnotation(@NotNull PsiClass psiParentClass) {
        return PsiAnnotationSearchUtil.findAnnotation(psiParentClass, getSupportedAnnotationClasses());
    }

    protected abstract boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder);

    protected abstract void generatePsiElements(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target);

}