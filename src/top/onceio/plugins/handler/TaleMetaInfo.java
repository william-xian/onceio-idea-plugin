package top.onceio.plugins.handler;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import top.onceio.plugins.psi.LombokLightFieldBuilder;
import top.onceio.plugins.util.LombokUtils;
import top.onceio.plugins.util.PsiAnnotationSearchUtil;
import top.onceio.plugins.util.PsiClassUtil;
import top.onceio.plugins.util.PsiTypeUtil;
import org.jetbrains.annotations.NotNull;
import top.onceio.core.db.annotation.Col;
import top.onceio.plugins.processor.field.AccessorsInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class TaleMetaInfo {
    private static final String BUILDER_OBTAIN_VIA_FIELD = "field";
    private static final String BUILDER_OBTAIN_VIA_METHOD = "method";
    private static final String BUILDER_OBTAIN_VIA_STATIC = "isStatic";
    private PsiAnnotation colAnnotation;
    private PsiVariable variableInClass;
    private PsiType fieldInBuilderType;
    private boolean deprecated;
    private String visibilityModifier;
    private String setterPrefix;

    private String builderChainResult = "this";
    private PsiClass builderClass;
    private PsiType builderClassType;

    private String fieldInBuilderName;
    private PsiExpression fieldInitializer;
    private boolean hasBuilderDefaultAnnotation;

    private PsiAnnotation obtainViaAnnotation;
    private String viaFieldName;
    private String viaMethodName;
    private boolean viaStaticCall;
    private String instanceVariableName = "this";

    public static TaleMetaInfo fromPsiParameter(@NotNull PsiParameter psiParameter) {
        final TaleMetaInfo result = new TaleMetaInfo();

        result.variableInClass = psiParameter;
        result.fieldInBuilderType = psiParameter.getType();
        result.deprecated = hasDeprecatedAnnotation(psiParameter);
        result.fieldInitializer = null;
        result.hasBuilderDefaultAnnotation = false;
        result.fieldInBuilderName = psiParameter.getName();
        return result;
    }

    private static boolean hasDeprecatedAnnotation(@NotNull PsiModifierListOwner modifierListOwner) {
        return PsiAnnotationSearchUtil.isAnnotatedWith(modifierListOwner, Deprecated.class);
    }

    public static TaleMetaInfo fromPsiField(@NotNull PsiField psiField) {
        final TaleMetaInfo result = new TaleMetaInfo();
        result.colAnnotation = psiField.getAnnotation(Col.class.getName());
        result.variableInClass = psiField;
        result.deprecated = isDeprecated(psiField);
        result.fieldInBuilderType = psiField.getType();
        result.fieldInitializer = psiField.getInitializer();
        result.hasBuilderDefaultAnnotation = false;

        final AccessorsInfo accessorsInfo = AccessorsInfo.build(psiField);
        result.fieldInBuilderName = accessorsInfo.removePrefix(psiField.getName());
        return result;
    }

    private static boolean isDeprecated(@NotNull PsiField psiField) {
        return PsiImplUtil.isDeprecatedByDocTag(psiField) || hasDeprecatedAnnotation(psiField);
    }

    public TaleMetaInfo withSubstitutor(@NotNull PsiSubstitutor builderSubstitutor) {
        fieldInBuilderType = builderSubstitutor.substitute(fieldInBuilderType);
        return this;
    }

    public TaleMetaInfo withVisibilityModifier(String visibilityModifier) {
        this.visibilityModifier = visibilityModifier;
        return this;
    }

    public TaleMetaInfo withSetterPrefix(String setterPrefix) {
        this.setterPrefix = setterPrefix;
        return this;
    }

    public TaleMetaInfo withBuilderClass(@NotNull PsiClass builderClass) {
        this.builderClass = builderClass;
        this.builderClassType = PsiClassUtil.getTypeWithGenerics(builderClass);
        return this;
    }

    public TaleMetaInfo withBuilderClassType(@NotNull PsiClassType builderClassType) {
        this.builderClassType = builderClassType;
        return this;
    }

    public TaleMetaInfo withBuilderChainResult(@NotNull String builderChainResult) {
        this.builderChainResult = builderChainResult;
        return this;
    }

    public TaleMetaInfo withObtainVia() {
        return this;
    }

    public boolean useForBuilder() {
        boolean result = true;
        PsiModifierList modifierList = variableInClass.getModifierList();
        if (null != modifierList) {
            //Skip static fields.
            result = !modifierList.hasModifierProperty(PsiModifier.STATIC);

            // skip initialized final fields unless annotated with @Builder.Default
            final boolean isInitializedFinalField = null != fieldInitializer && modifierList.hasModifierProperty(PsiModifier.FINAL);
            if (isInitializedFinalField && !hasBuilderDefaultAnnotation) {
                result = false;
            }
        }
        if(colAnnotation == null) {
            result = false;
        }

        //Skip fields that start with $
        result &= !fieldInBuilderName.startsWith(LombokUtils.LOMBOK_INTERN_FIELD_MARKER);

        return result;
    }

    public boolean notAlreadyExistingField(Collection<String> alreadyExistingFieldNames) {
        return !alreadyExistingFieldNames.contains(fieldInBuilderName);
    }

    public boolean notAlreadyExistingMethod(Collection<String> existedMethodNames) {
        return notAlreadyExistingField(existedMethodNames);
    }

    public Project getProject() {
        return variableInClass.getProject();
    }

    public PsiManager getManager() {
        return variableInClass.getManager();
    }

    public String getFieldName() {
        return fieldInBuilderName;
    }

    public PsiType getFieldType() {
        return fieldInBuilderType;
    }

    public PsiVariable getVariable() {
        return variableInClass;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    @PsiModifier.ModifierConstant
    public String getVisibilityModifier() {
        return visibilityModifier;
    }

    public String getSetterPrefix() {
        return setterPrefix;
    }

    public PsiClass getBuilderClass() {
        return builderClass;
    }

    public PsiType getBuilderType() {
        return builderClassType;
    }

    public String getBuilderChainResult() {
        return builderChainResult;
    }

    public boolean hasBuilderDefaultAnnotation() {
        return hasBuilderDefaultAnnotation;
    }

    public boolean hasObtainViaAnnotation() {
        return null != obtainViaAnnotation;
    }

    public String getViaFieldName() {
        return viaFieldName;
    }

    public String getViaMethodName() {
        return viaMethodName;
    }

    public boolean isViaStaticCall() {
        return viaStaticCall;
    }

    public String getInstanceVariableName() {
        return instanceVariableName;
    }

    public Collection<String> getAnnotations() {
        if (deprecated) {
            return Collections.singleton(CommonClassNames.JAVA_LANG_DEPRECATED);
        }
        return Collections.emptyList();
    }

    public Collection<PsiField> renderBuilderFields() {
        return renderBuilderFields(this);
    }

    public String renderBuildCall() {
        return fieldInBuilderName;
    }


    private PsiClass getPsiClass() {
        return builderClass.getContainingClass();
    }

    public Optional<PsiType> getObtainViaFieldVariableType() {
        PsiVariable psiVariable = variableInClass;

        if (StringUtil.isNotEmpty(viaFieldName)) {
            final PsiField fieldByName = getPsiClass().findFieldByName(viaFieldName, false);
            if (fieldByName != null) {
                psiVariable = fieldByName;
            }
        }

        final PsiType psiVariableType = psiVariable.getType();

        if (psiVariableType instanceof PsiClassReferenceType) {
            final PsiClass resolvedPsiVariableClass = ((PsiClassReferenceType) psiVariableType).resolve();
            if (resolvedPsiVariableClass instanceof PsiTypeParameter) {
                return Optional.of(psiVariableType);
            }
        }
        return Optional.empty();
    }

    public void withInstanceVariableName(String instanceVariableName) {
        this.instanceVariableName = instanceVariableName;
    }

    Collection<PsiField> renderBuilderFields(@NotNull TaleMetaInfo info) {
        final PsiType builderFieldType = getBuilderFieldType(info.getFieldType(), info.getProject());
        return Collections.singleton(
                new LombokLightFieldBuilder(info.getManager(), info.getFieldName(), builderFieldType)
                        .withContainingClass(info.getBuilderClass())
                        .withModifier(PsiModifier.PUBLIC)
                        .withNavigationElement(info.getVariable()));
    }

    PsiType getBuilderFieldType(@NotNull PsiType psiFieldType, @NotNull Project project) {
        final PsiManager psiManager = PsiManager.getInstance(project);
        PsiType co = getBuilderType();
        if (PsiType.BOOLEAN.isAssignableFrom(psiFieldType)||PsiType.BYTE.isAssignableFrom(psiFieldType)
                || PsiType.SHORT.isAssignableFrom(psiFieldType)
                || PsiType.INT.isAssignableFrom(psiFieldType)|| PsiType.FLOAT.isAssignableFrom(psiFieldType)
                || PsiType.LONG.isAssignableFrom(psiFieldType)|| PsiType.DOUBLE.isAssignableFrom(psiFieldType)) {
            return PsiTypeUtil.createCollectionType(psiManager, "top.onceio.core.db.model.BaseCol", co);
        } else {
            return PsiTypeUtil.createCollectionType(psiManager, "top.onceio.core.db.model.StringCol", co);
        }
    }

}
