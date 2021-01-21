package top.onceio.plugins.processor.clazz.model;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import top.onceio.plugins.problem.ProblemBuilder;
import top.onceio.plugins.processor.AbstractClassProcessor;
import org.jetbrains.annotations.NotNull;
import top.onceio.core.db.annotation.Model;
import top.onceio.plugins.handler.ModelHandler;
import top.onceio.plugins.settings.ProjectSettings;

import java.util.List;

public class ModelClassProcessor extends AbstractClassProcessor {

    private final ModelHandler modelHandler;

    public ModelClassProcessor() {
        super(PsiClass.class, Model.class);
        this.modelHandler = ApplicationManager.getApplication().getService(ModelHandler.class);
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