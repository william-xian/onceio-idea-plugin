package top.onceio.plugins.provider;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import top.onceio.plugins.processor.Processor;
import top.onceio.plugins.util.PsiAnnotationSearchUtil;
import top.onceio.plugins.util.PsiClassUtil;
import org.jetbrains.annotations.NotNull;
import top.onceio.core.util.Tuple2;
import top.onceio.plugins.processor.OnceIOProcessorManager;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OnceIOProcessorProvider {

    public static OnceIOProcessorProvider getInstance(@NotNull Project project) {
        final OnceIOProcessorProvider service = ServiceManager.getService(project, OnceIOProcessorProvider.class);
        service.checkInitialized();
        return service;
    }

    private final PropertiesComponent myPropertiesComponent;

    private final Map<Class, Collection<Processor>> lombokTypeProcessors;
    private final Map<String, Collection<Processor>> lombokProcessors;
    private final Collection<String> registeredAnnotationNames;

    private boolean alreadyInitialized;

    public OnceIOProcessorProvider(@NotNull PropertiesComponent propertiesComponent) {
        myPropertiesComponent = propertiesComponent;

        lombokProcessors = new ConcurrentHashMap<>();
        lombokTypeProcessors = new ConcurrentHashMap<>();
        registeredAnnotationNames = ConcurrentHashMap.newKeySet();
    }

    private void checkInitialized() {
        if (!alreadyInitialized) {
            initProcessors();
            alreadyInitialized = true;
        }
    }

    public void initProcessors() {
        lombokProcessors.clear();
        lombokTypeProcessors.clear();
        registeredAnnotationNames.clear();

        for (Processor processor : OnceIOProcessorManager.getOnceIOProcessors()) {
            if (processor.isEnabled(myPropertiesComponent)) {

                Class<? extends Annotation>[] annotationClasses = processor.getSupportedAnnotationClasses();
                for (Class<? extends Annotation> annotationClass : annotationClasses) {
                    putProcessor(lombokProcessors, annotationClass.getName(), processor);
                    putProcessor(lombokProcessors, annotationClass.getSimpleName(), processor);
                }

                putProcessor(lombokTypeProcessors, processor.getSupportedClass(), processor);
            }
        }

        registeredAnnotationNames.addAll(lombokProcessors.keySet());
    }

    @NotNull
    Collection<Processor> getOnceIOProcessors(@NotNull Class supportedClass) {
        return lombokTypeProcessors.computeIfAbsent(supportedClass, k -> ConcurrentHashMap.newKeySet());
    }

    @NotNull
    public Collection<Processor> getProcessors(@NotNull PsiAnnotation psiAnnotation) {
        final String qualifiedName = psiAnnotation.getQualifiedName();
        final Collection<Processor> result = qualifiedName == null ? null : lombokProcessors.get(qualifiedName);
        return result == null ? Collections.emptySet() : result;
    }

    @NotNull
    Collection<Tuple2<Processor, PsiAnnotation>> getApplicableProcessors(@NotNull PsiMember psiMember) {
        Collection<Tuple2<Processor, PsiAnnotation>> result = Collections.emptyList();
        if (verifyOnceIOAnnotationPresent(psiMember)) {
            result = new ArrayList<>();

            addApplicableProcessors(psiMember, result);
            final PsiClass psiClass = psiMember.getContainingClass();
            if (null != psiClass) {
                addApplicableProcessors(psiClass, result);
            }
        }
        return result;
    }

    private <K, V> void putProcessor(final Map<K, Collection<V>> map, final K key, final V value) {
        Collection<V> valueList = map.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        valueList.add(value);
    }

    private boolean verifyOnceIOAnnotationPresent(@NotNull PsiClass psiClass) {
        if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(psiClass, registeredAnnotationNames)) {
            return true;
        }
        Collection<PsiField> psiFields = PsiClassUtil.collectClassFieldsIntern(psiClass);
        for (PsiField psiField : psiFields) {
            if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(psiField, registeredAnnotationNames)) {
                return true;
            }
        }
        Collection<PsiMethod> psiMethods = PsiClassUtil.collectClassMethodsIntern(psiClass);
        for (PsiMethod psiMethod : psiMethods) {
            if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(psiMethod, registeredAnnotationNames)) {
                return true;
            }
        }
        final PsiElement psiClassParent = psiClass.getParent();
        if (psiClassParent instanceof PsiClass) {
            return verifyOnceIOAnnotationPresent((PsiClass) psiClassParent);
        }

        return false;
    }

    private boolean verifyOnceIOAnnotationPresent(@NotNull PsiMember psiMember) {
        if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(psiMember, registeredAnnotationNames)) {
            return true;
        }

        final PsiClass psiClass = psiMember.getContainingClass();
        return null != psiClass && verifyOnceIOAnnotationPresent(psiClass);
    }

    private void addApplicableProcessors(@NotNull PsiMember psiMember, @NotNull Collection<Tuple2<Processor, PsiAnnotation>> target) {
        final PsiModifierList psiModifierList = psiMember.getModifierList();
        if (null != psiModifierList) {
            for (PsiAnnotation psiAnnotation : psiModifierList.getAnnotations()) {
                for (Processor processor : getProcessors(psiAnnotation)) {
                    target.add(new Tuple2(processor, psiAnnotation));
                }
            }
        }
    }
}
