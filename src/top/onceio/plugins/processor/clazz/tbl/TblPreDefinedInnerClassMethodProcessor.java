package top.onceio.plugins.processor.clazz.tbl;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import de.plushnikov.intellij.plugin.processor.handler.BuilderInfo;
import de.plushnikov.intellij.plugin.util.PsiAnnotationSearchUtil;
import de.plushnikov.intellij.plugin.util.PsiClassUtil;
import lombok.experimental.Tolerate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.plugins.handler.TblHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates methods for a builder inner class if it is predefined.
 *
 * @author Michail Plushnikov
 */
public class TblPreDefinedInnerClassMethodProcessor extends AbstractTblPreDefinedInnerClassProcessor {

    public TblPreDefinedInnerClassMethodProcessor(@NotNull TblHandler tblHandler) {
        super(tblHandler, PsiMethod.class, Tbl.class);
    }

    protected Collection<? extends PsiElement> generatePsiElements(@NotNull PsiClass psiParentClass, @Nullable PsiMethod psiParentMethod, @NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiBuilderClass) {
        final Collection<PsiMethod> result = new ArrayList<>();

        final Collection<String> existedMethodNames = PsiClassUtil.collectClassMethodsIntern(psiBuilderClass).stream()
                .filter(psiMethod -> PsiAnnotationSearchUtil.isNotAnnotatedWith(psiMethod, Tolerate.class))
                .map(PsiMethod::getName).collect(Collectors.toSet());

        final List<BuilderInfo> builderInfos = tblHandler.createBuilderInfos(psiAnnotation, psiParentClass, psiParentMethod, psiBuilderClass);

        //create constructor
        result.addAll(tblHandler.createConstructors(psiBuilderClass, psiAnnotation));

        // create builder methods
        builderInfos.stream()
                .filter(info -> info.notAlreadyExistingMethod(existedMethodNames))
                .map(BuilderInfo::renderBuilderMethods)
                .forEach(result::addAll);

        return result;
    }

}