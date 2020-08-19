package top.onceio.plugins.provider;


import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import de.plushnikov.intellij.plugin.processor.LombokPsiElementUsage;
import de.plushnikov.intellij.plugin.processor.Processor;
import top.onceio.core.util.Tuple2;

import java.util.Collection;

/**
 * Provides implicit usages of lombok fields
 */
public class OnceIOImplicitUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(PsiElement element) {
        return isImplicitWrite(element) || isImplicitRead(element);
    }

    @Override
    public boolean isImplicitRead(PsiElement element) {
        return checkUsage(element, LombokPsiElementUsage.READ);
    }

    @Override
    public boolean isImplicitWrite(PsiElement element) {
        return checkUsage(element, LombokPsiElementUsage.WRITE);
    }

    private boolean checkUsage(PsiElement element, LombokPsiElementUsage elementUsage) {
        boolean result = false;
        if (element instanceof PsiField) {
            final OnceIOProcessorProvider processorProvider = OnceIOProcessorProvider.getInstance(element.getProject());
            final Collection<Tuple2<Processor, PsiAnnotation>> applicableProcessors = processorProvider.getApplicableProcessors((PsiField) element);

            for (Tuple2<Processor, PsiAnnotation> processorData : applicableProcessors) {
                final LombokPsiElementUsage psiElementUsage = processorData.a.checkFieldUsage((PsiField) element, processorData.b);
                if (elementUsage == psiElementUsage || LombokPsiElementUsage.READ_WRITE == psiElementUsage) {
                    result = true;
                    break;
                }
            }

        }
        return result;
    }

}