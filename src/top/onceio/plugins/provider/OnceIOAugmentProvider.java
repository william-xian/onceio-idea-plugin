package top.onceio.plugins.provider;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import de.plushnikov.intellij.plugin.processor.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.plugins.settings.ProjectSettings;

import java.util.*;

/**
 * Provides support for lombok generated elements
 *
 * @author Plushnikov Michail
 */
public class OnceIOAugmentProvider extends PsiAugmentProvider {
    private static final Logger log = Logger.getInstance(OnceIOAugmentProvider.class.getName());


    public OnceIOAugmentProvider() {
        log.debug("LombokAugmentProvider created");
    }

    @NotNull
    @Override
    protected Set<String> transformModifiers(@NotNull PsiModifierList modifierList, @NotNull final Set<String> modifiers) {
        // make copy of original modifiers
        Set<String> result = new HashSet<>(modifiers);
        return result;
    }

    @NotNull
    @Override
    public <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull final Class<Psi> type) {
        final List<Psi> emptyResult = Collections.emptyList();
        if ((type != PsiClass.class && type != PsiField.class && type != PsiMethod.class) || !(element instanceof PsiExtensibleClass)) {
            return emptyResult;
        }

        // Don't filter !isPhysical elements or code auto completion will not work
        if (!element.isValid()) {
            return emptyResult;
        }
        final PsiClass psiClass = (PsiClass) element;
        // Skip processing of Annotations and Interfaces
        if (psiClass.isAnnotationType() || psiClass.isInterface()) {
            return emptyResult;
        }
        // skip processing if plugin is disabled
        final Project project = element.getProject();
        if (!ProjectSettings.isLombokEnabledInProject(project)) {
            return emptyResult;
        }

        final List<Psi> cachedValue;
        if (type == PsiField.class) {
            cachedValue = CachedValuesManager.getCachedValue(element, new OnceIOAugmentProvider.FieldLombokCachedValueProvider<>(type, psiClass));
        } else if (type == PsiMethod.class) {
            cachedValue = CachedValuesManager.getCachedValue(element, new OnceIOAugmentProvider.MethodLombokCachedValueProvider<>(type, psiClass));
        } else {
            cachedValue = CachedValuesManager.getCachedValue(element, new OnceIOAugmentProvider.ClassLombokCachedValueProvider<>(type, psiClass));
        }
        return null != cachedValue ? cachedValue : emptyResult;
    }

    private static class FieldLombokCachedValueProvider<Psi extends PsiElement> extends OnceIOAugmentProvider.LombokCachedValueProvider<Psi> {
        private static final RecursionGuard<PsiClass> ourGuard = RecursionManager.createGuard("lombok.augment.field");

        FieldLombokCachedValueProvider(Class<Psi> type, PsiClass psiClass) {
            super(type, psiClass, ourGuard);
        }
    }

    private static class MethodLombokCachedValueProvider<Psi extends PsiElement> extends OnceIOAugmentProvider.LombokCachedValueProvider<Psi> {
        private static final RecursionGuard<PsiClass> ourGuard = RecursionManager.createGuard("lombok.augment.method");

        MethodLombokCachedValueProvider(Class<Psi> type, PsiClass psiClass) {
            super(type, psiClass, ourGuard);
        }
    }

    private static class ClassLombokCachedValueProvider<Psi extends PsiElement> extends OnceIOAugmentProvider.LombokCachedValueProvider<Psi> {
        private static final RecursionGuard<PsiClass> ourGuard = RecursionManager.createGuard("lombok.augment.class");

        ClassLombokCachedValueProvider(Class<Psi> type, PsiClass psiClass) {
            super(type, psiClass, ourGuard);
        }
    }

    private abstract static class LombokCachedValueProvider<Psi extends PsiElement> implements CachedValueProvider<List<Psi>> {
        private final Class<Psi> type;
        private final PsiClass psiClass;
        private final RecursionGuard<PsiClass> recursionGuard;

        LombokCachedValueProvider(Class<Psi> type, PsiClass psiClass, RecursionGuard<PsiClass> recursionGuard) {
            this.type = type;
            this.psiClass = psiClass;
            this.recursionGuard = recursionGuard;
        }

        @Nullable
        @Override
        public Result<List<Psi>> compute() {
//      return compute2();
            return recursionGuard.doPreventingRecursion(psiClass, true, this::computeIntern);
        }

        private Result<List<Psi>> computeIntern() {
//      final String message = String.format("Process call for type: %s class: %s", type.getSimpleName(), psiClass.getQualifiedName());
//      log.info(">>>" + message);
            final List<Psi> result = getPsis(psiClass, type);
//      log.info("<<<" + message);
            return Result.create(result, psiClass);
        }
    }

    @NotNull
    private static <Psi extends PsiElement> List<Psi> getPsis(PsiClass psiClass, Class<Psi> type) {
        final List<Psi> result = new ArrayList<>();
        final Collection<Processor> lombokProcessors = OnceIOProcessorProvider.getInstance(psiClass.getProject()).getLombokProcessors(type);
        for (Processor processor : lombokProcessors) {
            final List<? super PsiElement> generatedElements = processor.process(psiClass);
            for (Object psiElement : generatedElements) {
                result.add((Psi) psiElement);
            }
        }
        return result;
    }
}
