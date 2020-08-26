package top.onceio.plugins.processor.clazz.model;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import de.plushnikov.intellij.plugin.problem.ProblemBuilder;
import de.plushnikov.intellij.plugin.processor.clazz.AbstractClassProcessor;
import org.jetbrains.annotations.NotNull;
import top.onceio.core.db.annotation.Model;
import top.onceio.plugins.handler.ModelHandler;
import top.onceio.plugins.settings.ProjectSettings;

import java.util.List;

public class ModelClassProcessor extends AbstractClassProcessor {

    private final ModelHandler modelHandler;

    public ModelClassProcessor(@NotNull ModelHandler modelHandler) {
        super(PsiClass.class, Model.class);
        this.modelHandler = modelHandler;
    }

    @Override
    public boolean isEnabled(@NotNull PropertiesComponent propertiesComponent) {
        return ProjectSettings.isEnabled(propertiesComponent, ProjectSettings.IS_BUILDER_ENABLED);
    }

    @Override
    protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
        return modelHandler.validate(psiClass, psiAnnotation, builder);
    }

    protected void generatePsiElements(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
        modelHandler.createBuilderClassIfNotExist(psiClass, null, psiAnnotation).ifPresent(target::add);
    }
}