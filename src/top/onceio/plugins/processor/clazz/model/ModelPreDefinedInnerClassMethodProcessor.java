package top.onceio.plugins.processor.clazz.model;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import top.onceio.plugins.util.PsiAnnotationSearchUtil;
import top.onceio.plugins.util.PsiClassUtil;
import lombok.experimental.Tolerate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.core.db.annotation.Model;
import top.onceio.plugins.handler.TaleMetaInfo;
import top.onceio.plugins.handler.ModelHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates methods for a builder inner class if it is predefined.
 *
 * @author Michail Plushnikov
 */
public class ModelPreDefinedInnerClassMethodProcessor extends AbstractModelPreDefinedInnerClassProcessor {

    public ModelPreDefinedInnerClassMethodProcessor(@NotNull ModelHandler modelHandler) {
        super(modelHandler, PsiMethod.class, Model.class);
    }

    protected Collection<? extends PsiElement> generatePsiElements(@NotNull PsiClass psiParentClass, @Nullable PsiMethod psiParentMethod, @NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiBuilderClass) {
        final Collection<PsiMethod> result = new ArrayList<>();

        final Collection<String> existedMethodNames = PsiClassUtil.collectClassMethodsIntern(psiBuilderClass).stream()
                .filter(psiMethod -> PsiAnnotationSearchUtil.isNotAnnotatedWith(psiMethod, Tolerate.class))
                .map(PsiMethod::getName).collect(Collectors.toSet());

        final List<TaleMetaInfo> taleMetaInfos = modelHandler.createTableMetaInfos(psiAnnotation, psiParentClass, psiParentMethod, psiBuilderClass);

        //create constructor
        result.addAll(modelHandler.createConstructors(psiBuilderClass, psiAnnotation));

        return result;
    }

}