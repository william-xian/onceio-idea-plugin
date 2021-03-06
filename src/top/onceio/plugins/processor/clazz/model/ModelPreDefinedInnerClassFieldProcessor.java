package top.onceio.plugins.processor.clazz.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.core.db.annotation.Model;
import top.onceio.plugins.handler.ModelHandler;
import top.onceio.plugins.handler.TaleMetaInfo;
import top.onceio.plugins.util.PsiClassUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates fields for a @Builder inner class if it is predefined.
 *
 * @author Liar
 */
public class ModelPreDefinedInnerClassFieldProcessor extends AbstractModelPreDefinedInnerClassProcessor {

    public ModelPreDefinedInnerClassFieldProcessor() {
        super(PsiField.class, Model.class);
    }

    @Override
    protected Collection<? extends PsiElement> generatePsiElements(@NotNull PsiClass psiParentClass, @Nullable PsiMethod psiParentMethod, @NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiBuilderClass) {
        final Collection<String> existedFieldNames = PsiClassUtil.collectClassFieldsIntern(psiBuilderClass).stream()
                .map(PsiField::getName)
                .collect(Collectors.toSet());

        final List<TaleMetaInfo> taleMetaInfos = modelHandler.createTableMetaInfos(psiAnnotation, psiParentClass, psiParentMethod, psiBuilderClass);
        return taleMetaInfos.stream()
                .filter(info -> info.notAlreadyExistingField(existedFieldNames))
                .map(TaleMetaInfo::renderBuilderFields)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
