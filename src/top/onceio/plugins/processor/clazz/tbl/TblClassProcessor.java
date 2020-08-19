package top.onceio.plugins.processor.clazz.tbl;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import de.plushnikov.intellij.plugin.problem.ProblemBuilder;
import de.plushnikov.intellij.plugin.processor.clazz.AbstractClassProcessor;
import org.jetbrains.annotations.NotNull;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.plugins.handler.TblHandler;
import top.onceio.plugins.settings.ProjectSettings;

import java.util.List;

public class TblClassProcessor extends AbstractClassProcessor {

    private final TblHandler tblHandler;

    public TblClassProcessor(@NotNull TblHandler tblHandler) {
        super(PsiClass.class, Tbl.class);
        this.tblHandler = tblHandler;
    }

    @Override
    public boolean isEnabled(@NotNull PropertiesComponent propertiesComponent) {
        return ProjectSettings.isEnabled(propertiesComponent, ProjectSettings.IS_BUILDER_ENABLED);
    }

    @Override
    protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
        return tblHandler.validate(psiClass, psiAnnotation, builder);
    }

    protected void generatePsiElements(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
        tblHandler.createBuilderClassIfNotExist(psiClass, null, psiAnnotation).ifPresent(target::add);
    }
}