package top.onceio.plugins.processor.clazz.tbl;

import com.intellij.psi.*;
import de.plushnikov.intellij.plugin.util.PsiClassUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.plugins.handler.TaleMetaInfo;
import top.onceio.plugins.handler.TblHandler;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates fields for a @Builder inner class if it is predefined.
 *
 * @author Michail Plushnikov
 */
public class TblPreDefinedInnerClassFieldProcessor extends AbstractTblPreDefinedInnerClassProcessor {

    public TblPreDefinedInnerClassFieldProcessor(@NotNull TblHandler tblHandler) {
        super(tblHandler, PsiField.class, Tbl.class);
    }

    @Override
    protected Collection<? extends PsiElement> generatePsiElements(@NotNull PsiClass psiParentClass, @Nullable PsiMethod psiParentMethod, @NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiBuilderClass) {
        final Collection<String> existedFieldNames = PsiClassUtil.collectClassFieldsIntern(psiBuilderClass).stream()
                .map(PsiField::getName)
                .collect(Collectors.toSet());

        final List<TaleMetaInfo> taleMetaInfos = tblHandler.createTableMetaInfos(psiAnnotation, psiParentClass, psiParentMethod, psiBuilderClass);
        return taleMetaInfos.stream()
                .filter(info -> info.notAlreadyExistingField(existedFieldNames))
                .map(TaleMetaInfo::renderBuilderFields)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
