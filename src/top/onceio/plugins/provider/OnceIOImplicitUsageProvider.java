package top.onceio.plugins.provider;


import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import top.onceio.plugins.processor.OnceIOPsiElementUsage;
import top.onceio.plugins.processor.Processor;
import top.onceio.core.util.Tuple2;

import java.util.Collection;

/**
 * Provides implicit usages of onceio fields
 */
public class OnceIOImplicitUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(PsiElement element) {
        return isImplicitWrite(element) || isImplicitRead(element);
    }

    @Override
    public boolean isImplicitRead(PsiElement element) {
        return checkUsage(element, OnceIOPsiElementUsage.READ);
    }

    @Override
    public boolean isImplicitWrite(PsiElement element) {
        return checkUsage(element, OnceIOPsiElementUsage.WRITE);
    }

    private boolean checkUsage(PsiElement element, OnceIOPsiElementUsage elementUsage) {
        boolean result = false;
        if (element instanceof PsiField) {
            final OnceIOProcessorProvider processorProvider = OnceIOProcessorProvider.getInstance(element.getProject());
            final Collection<Tuple2<Processor, PsiAnnotation>> applicableProcessors = processorProvider.getApplicableProcessors((PsiField) element);

            for (Tuple2<Processor, PsiAnnotation> processorData : applicableProcessors) {
                final OnceIOPsiElementUsage psiElementUsage = processorData.a.checkFieldUsage((PsiField) element, processorData.b);
                if (elementUsage == psiElementUsage || OnceIOPsiElementUsage.READ_WRITE == psiElementUsage) {
                    result = true;
                    break;
                }
            }

        }
        return result;
    }

}