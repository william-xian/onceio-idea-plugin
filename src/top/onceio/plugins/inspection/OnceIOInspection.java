package top.onceio.plugins.inspection;


import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import top.onceio.plugins.problem.OnceIOProblem;
import top.onceio.plugins.processor.Processor;
import top.onceio.plugins.psi.OnceIOLightMethodBuilder;
import org.jetbrains.annotations.NotNull;
import top.onceio.plugins.provider.OnceIOProcessorProvider;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Plushnikov Michail
 */
public class OnceIOInspection extends AbstractBaseJavaLocalInspectionTool {



    public OnceIOInspection() {
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new OnceIOInspection.OnceIOElementVisitor(holder);
    }

    private class OnceIOElementVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        public OnceIOElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitLocalVariable(PsiLocalVariable variable) {
            super.visitLocalVariable(variable);

        }

        @Override
        public void visitParameter(PsiParameter parameter) {
            super.visitParameter(parameter);
        }

        @Override
        public void visitAnnotation(PsiAnnotation annotation) {
            super.visitAnnotation(annotation);

            final Collection<OnceIOProblem> problems = new HashSet<>();

            final OnceIOProcessorProvider processorProvider = OnceIOProcessorProvider.getInstance(annotation.getProject());
            for (Processor inspector : processorProvider.getProcessors(annotation)) {
                problems.addAll(inspector.verifyAnnotation(annotation));
            }

            for (OnceIOProblem problem : problems) {
                holder.registerProblem(annotation, problem.getMessage(), problem.getHighlightType(), problem.getQuickFixes());
            }
        }

        /**
         * Check MethodCallExpressions for calls for default (argument less) constructor
         * Produce an error if resolved constructor method is build by lombok and contains some arguments
         */
        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression methodCall) {
            super.visitMethodCallExpression(methodCall);

            PsiExpressionList list = methodCall.getArgumentList();
            PsiReferenceExpression referenceToMethod = methodCall.getMethodExpression();

            boolean isThisOrSuper = referenceToMethod.getReferenceNameElement() instanceof PsiKeyword;
            final int parameterCount = list.getExpressions().length;
            if (isThisOrSuper && parameterCount == 0) {

                JavaResolveResult[] results = referenceToMethod.multiResolve(true);
                JavaResolveResult resolveResult = results.length == 1 ? results[0] : JavaResolveResult.EMPTY;
                PsiElement resolved = resolveResult.getElement();

                if (resolved instanceof OnceIOLightMethodBuilder && ((OnceIOLightMethodBuilder) resolved).getParameterList().getParameters().length != 0) {
                    holder.registerProblem(methodCall, "Default constructor doesn't exist", ProblemHighlightType.ERROR);
                }
            }
        }
    }
}