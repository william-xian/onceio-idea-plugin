package top.onceio.plugins.processor.clazz.tbl;


import com.intellij.ide.util.PropertiesComponent;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import de.plushnikov.intellij.plugin.problem.ProblemBuilder;
import de.plushnikov.intellij.plugin.processor.LombokPsiElementUsage;
import de.plushnikov.intellij.plugin.processor.clazz.AbstractClassProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.constructor.AllArgsConstructorProcessor;
import de.plushnikov.intellij.plugin.processor.handler.BuilderHandler;
import de.plushnikov.intellij.plugin.settings.ProjectSettings;
import de.plushnikov.intellij.plugin.util.PsiAnnotationSearchUtil;
import de.plushnikov.intellij.plugin.util.PsiClassUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import org.jetbrains.annotations.NotNull;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.plugins.handler.TblHandler;

import java.util.Collection;
import java.util.List;

/**
 * Inspect and validate @Builder lombok annotation on a class.
 * Creates methods for a builder pattern for initializing a class.
 *
 * @author Tomasz Kalkosiński
 * @author Michail Plushnikov
 */
public class TblProcessor extends AbstractClassProcessor {

    static final String SINGULAR_CLASS = Singular.class.getName();
    static final String BUILDER_DEFAULT_CLASS = Builder.Default.class.getCanonicalName();

    private final TblHandler builderHandler;

    public TblProcessor(@NotNull TblHandler builderHandler) {
        super(PsiMethod.class, Tbl.class);
        this.builderHandler = builderHandler;
    }

    @Override
    public boolean isEnabled(@NotNull PropertiesComponent propertiesComponent) {
        return ProjectSettings.isEnabled(propertiesComponent, ProjectSettings.IS_BUILDER_ENABLED);
    }

    @NotNull
    @Override
    public Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass) {
        final Collection<PsiAnnotation> result = super.collectProcessedAnnotations(psiClass);
        addFieldsAnnotation(result, psiClass, SINGULAR_CLASS, BUILDER_DEFAULT_CLASS);
        return result;
    }

    @Override
    protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
        // we skip validation here, because it will be validated by other BuilderClassProcessor
        return true;//builderHandler.validate(psiClass, psiAnnotation, builder);
    }

    protected void generatePsiElements(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
        final String builderClassName = builderHandler.getBuilderClassName(psiClass, psiAnnotation, null);
        final PsiClass builderClass = psiClass.findInnerClassByName(builderClassName, false);
        if (null != builderClass) {
            builderHandler.createBuilderMethodIfNecessary(psiClass, null, builderClass, psiAnnotation)
                    .ifPresent(target::add);
        }
    }

    @Override
    public LombokPsiElementUsage checkFieldUsage(@NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation) {
        return LombokPsiElementUsage.READ_WRITE;
    }
}
