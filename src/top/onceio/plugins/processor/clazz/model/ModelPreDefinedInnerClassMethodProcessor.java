package top.onceio.plugins.processor.clazz.model;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.core.db.annotation.Model;
import top.onceio.plugins.handler.TaleMetaInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Creates methods for a builder inner class if it is predefined.
 *
 * @author Liar
 */
public class ModelPreDefinedInnerClassMethodProcessor extends AbstractModelPreDefinedInnerClassProcessor {

    public ModelPreDefinedInnerClassMethodProcessor() {
        super(PsiMethod.class, Model.class);
    }

    protected Collection<? extends PsiElement> generatePsiElements(@NotNull PsiClass psiParentClass, @Nullable PsiMethod psiParentMethod, @NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiBuilderClass) {
        final Collection<PsiMethod> result = new ArrayList<>();


        final List<TaleMetaInfo> taleMetaInfos = modelHandler.createTableMetaInfos(psiAnnotation, psiParentClass, psiParentMethod, psiBuilderClass);

        //create constructor
        result.addAll(modelHandler.createConstructors(psiBuilderClass, psiAnnotation));

        return result;
    }

}