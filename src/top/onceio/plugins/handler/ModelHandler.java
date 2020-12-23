package top.onceio.plugins.handler;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import de.plushnikov.intellij.plugin.problem.ProblemBuilder;
import de.plushnikov.intellij.plugin.psi.LombokLightClassBuilder;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import top.onceio.plugins.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.core.db.model.BaseMeta;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Handler methods for Builder-processing
 *
 * @author Tomasz Kalkosiński
 * @author Michail Plushnikov
 */
public class ModelHandler {
    private final static String ANNOTATION_TABLE_META_CLASS_NAME = "tableMetaClassName";
    private static final String ANNOTATION_META_METHOD_NAME = "metaMethodName";
    private static final String ANNOTATION_SETTER_PREFIX = "setterPrefix";

    private final static String META_METHOD_NAME = "meta";

    private static final Collection<String> INVALID_ON_BUILDERS = Collections.unmodifiableSet(new HashSet<>());

    public ModelHandler() {
    }

    PsiSubstitutor getBuilderSubstitutor(@NotNull PsiTypeParameterListOwner classOrMethodToBuild, @NotNull PsiClass innerClass) {
        PsiSubstitutor substitutor = PsiSubstitutor.EMPTY;
        if (innerClass.hasModifierProperty(PsiModifier.STATIC)) {
            PsiTypeParameter[] typeParameters = classOrMethodToBuild.getTypeParameters();
            PsiTypeParameter[] builderParams = innerClass.getTypeParameters();
            if (typeParameters.length <= builderParams.length) {
                for (int i = 0; i < typeParameters.length; i++) {
                    PsiTypeParameter typeParameter = typeParameters[i];
                    substitutor = substitutor.put(typeParameter, PsiSubstitutor.EMPTY.substitute(builderParams[i]));
                }
            }
        }
        return substitutor;
    }

    public boolean validate(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull ProblemBuilder problemBuilder) {
        boolean result = validateAnnotationOnRightType(psiClass, psiAnnotation, problemBuilder);
        if (result) {
            final Project project = psiAnnotation.getProject();
            final String builderClassName = getBuilderClassName(psiClass, psiAnnotation);
            final String builderMethodName = getMetaMethodName(psiAnnotation);
            result = validateBuilderIdentifier(builderClassName, project, problemBuilder) &&
                    (builderMethodName.isEmpty() || validateBuilderIdentifier(builderMethodName, project, problemBuilder)) &&
                    validateExistingBuilderClass(builderClassName, psiClass, problemBuilder);
        }
        return result;
    }


    private boolean validateBuilderIdentifier(@NotNull String builderClassName, @NotNull Project project, @NotNull ProblemBuilder builder) {
        final PsiNameHelper psiNameHelper = PsiNameHelper.getInstance(project);
        if (!psiNameHelper.isIdentifier(builderClassName)) {
            builder.addError("%s is not a valid identifier", builderClassName);
            return false;
        }
        return true;
    }

    public boolean validateExistingBuilderClass(@NotNull String builderClassName, @NotNull PsiClass psiClass, @NotNull ProblemBuilder problemBuilder) {
        final Optional<PsiClass> optionalPsiClass = PsiClassUtil.getInnerClassInternByName(psiClass, builderClassName);

        return optionalPsiClass.map(builderClass -> validateInvalidAnnotationsOnBuilderClass(builderClass, problemBuilder)).orElse(true);
    }

    boolean validateInvalidAnnotationsOnBuilderClass(@NotNull PsiClass builderClass, @NotNull ProblemBuilder problemBuilder) {
        if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(builderClass, INVALID_ON_BUILDERS)) {
            problemBuilder.addError("Lombok annotations are not allowed on builder class.");
            return false;
        }
        return true;
    }

    private boolean validateAnnotationOnRightType(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull ProblemBuilder builder) {
        if (psiClass.isAnnotationType() || psiClass.isInterface() || psiClass.isEnum()) {
            builder.addError(String.format("@%s can be used on classes only", psiAnnotation.getQualifiedName()));
            return false;
        }
        return true;
    }


    public Optional<PsiClass> getExistInnerBuilderClass(@NotNull PsiClass psiClass, @Nullable PsiMethod psiMethod, @NotNull PsiAnnotation psiAnnotation) {
        final String builderClassName = getBuilderClassName(psiClass, psiAnnotation, psiMethod);
        return PsiClassUtil.getInnerClassInternByName(psiClass, builderClassName);
    }


    @NotNull
    String getMetaMethodName(@NotNull PsiAnnotation psiAnnotation) {
        final String builderMethodName = PsiAnnotationUtil.getStringAnnotationValue(psiAnnotation, ANNOTATION_META_METHOD_NAME);
        return null == builderMethodName ? META_METHOD_NAME : builderMethodName;
    }

    @NotNull
    private String getSetterPrefix(@NotNull PsiAnnotation psiAnnotation) {
        final String setterPrefix = PsiAnnotationUtil.getStringAnnotationValue(psiAnnotation, ANNOTATION_SETTER_PREFIX);
        return null == setterPrefix ? "" : setterPrefix;
    }

    @NotNull
    @PsiModifier.ModifierConstant
    private String getBuilderOuterAccessVisibility(@NotNull PsiAnnotation psiAnnotation) {
        final String accessVisibility = LombokProcessorUtil.getAccessVisibility(psiAnnotation);
        return null == accessVisibility ? PsiModifier.PUBLIC : accessVisibility;
    }

    @NotNull
    @PsiModifier.ModifierConstant
    private String getBuilderInnerAccessVisibility(@NotNull PsiAnnotation psiAnnotation) {
        final String accessVisibility = getBuilderOuterAccessVisibility(psiAnnotation);
        return PsiModifier.PROTECTED.equals(accessVisibility) ? PsiModifier.PUBLIC : accessVisibility;
    }

    @NotNull
    private String getBuilderClassName(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation) {
        return getBuilderClassName(psiClass, psiAnnotation, null);
    }

    @NotNull
    public String getBuilderClassName(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @Nullable PsiMethod psiMethod) {
        final String builderClassName = PsiAnnotationUtil.getStringAnnotationValue(psiAnnotation, ANNOTATION_TABLE_META_CLASS_NAME);
        if (!StringUtil.isEmptyOrSpaces(builderClassName)) {
            return builderClassName;
        }

        String relevantReturnType = psiClass.getName();

        if (null != psiMethod && !psiMethod.isConstructor()) {
            final PsiType psiMethodReturnType = psiMethod.getReturnType();
            if (null != psiMethodReturnType) {
                relevantReturnType = PsiNameHelper.getQualifiedClassName(psiMethodReturnType.getPresentableText(), false);
            }
        }

        return getBuilderClassName(psiClass, relevantReturnType);
    }

    @NotNull
    String getBuilderClassName(@NotNull PsiClass psiClass, String returnTypeName) {
        final String builderClassNamePattern = "*Meta";
        //return replace(builderClassNamePattern, "*", capitalize(returnTypeName));
        return "Meta";
    }

    boolean hasMethod(@NotNull PsiClass psiClass, @NotNull String builderMethodName) {
        final Collection<PsiMethod> existingMethods = PsiClassUtil.collectClassStaticMethodsIntern(psiClass);
        return existingMethods.stream().map(PsiMethod::getName).anyMatch(builderMethodName::equals);
    }

    public Optional<PsiMethod> createBuilderMethodIfNecessary(@NotNull PsiClass containingClass, @Nullable PsiMethod psiMethod, @NotNull PsiClass builderPsiClass, @NotNull PsiAnnotation psiAnnotation) {
        final String builderMethodName = getMetaMethodName(psiAnnotation);
        if (!builderMethodName.isEmpty() && !hasMethod(containingClass, builderMethodName)) {
            final PsiType psiTypeWithGenerics = PsiClassUtil.getTypeWithGenerics(builderPsiClass);

            final String blockText = String.format("return new %s();", psiTypeWithGenerics.getPresentableText());
            final LombokLightMethodBuilder methodBuilder = new LombokLightMethodBuilder(containingClass.getManager(), builderMethodName)
                    .withMethodReturnType(psiTypeWithGenerics)
                    .withContainingClass(containingClass)
                    .withNavigationElement(psiAnnotation)
                    .withModifier(getBuilderOuterAccessVisibility(psiAnnotation));
            methodBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(blockText, methodBuilder));

            addTypeParameters(builderPsiClass, psiMethod, methodBuilder);

            if (null == psiMethod || psiMethod.isConstructor() || psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                methodBuilder.withModifier(PsiModifier.STATIC);
            }
            return Optional.of(methodBuilder);
        }
        return Optional.empty();
    }


    @NotNull
    private Stream<TaleMetaInfo> createTableMetaInfos(@NotNull PsiClass psiClass, @Nullable PsiMethod psiClassMethod) {
        final Stream<TaleMetaInfo> result;
        if (null != psiClassMethod) {
            result = Arrays.stream(psiClassMethod.getParameterList().getParameters()).map(TaleMetaInfo::fromPsiParameter);
        } else {
            /*
            result = PsiClassUtil.collectClassFieldsIntern(psiClass).stream().map(TaleMetaInfo::fromPsiField)
                    .filter(TaleMetaInfo::useForBuilder);
            */
            Stream<TaleMetaInfo> all = Stream.empty();
            for (PsiClass cur = psiClass; cur != null && !cur.getName().equals(Object.class.getName()); cur = cur.getSuperClass()) {
                all = Stream.concat(all, PsiClassUtil.collectClassFieldsIntern(cur).stream().map(TaleMetaInfo::fromPsiField)
                        .filter(TaleMetaInfo::useForBuilder));
            }
            result = all;

        }

        return result;
    }

    public List<TaleMetaInfo> createTableMetaInfos(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass,
                                                   @Nullable PsiMethod psiClassMethod, @NotNull PsiClass builderClass) {
        final PsiSubstitutor builderSubstitutor = getBuilderSubstitutor(psiClass, builderClass);
        final String accessVisibility = getBuilderInnerAccessVisibility(psiAnnotation);
        final String setterPrefix = getSetterPrefix(psiAnnotation);

        return createTableMetaInfos(psiClass, psiClassMethod)
                .map(info -> info.withSubstitutor(builderSubstitutor))
                .map(info -> info.withBuilderClass(builderClass))
                .map(info -> info.withVisibilityModifier(accessVisibility))
                .map(info -> info.withSetterPrefix(setterPrefix))
                .collect(Collectors.toList());
    }

    @NotNull
    public String getBuilderClassName(@NotNull PsiClass psiClass) {
        return getBuilderClassName(psiClass, psiClass.getName());
    }

    @NotNull
    public PsiClass createBuilderClass(@NotNull PsiClass psiClass, @Nullable PsiMethod psiMethod, @NotNull PsiAnnotation psiAnnotation) {
        LombokLightClassBuilder builderClass;
        if (null != psiMethod) {
            builderClass = createEmptyBuilderClass(psiClass, psiMethod, psiAnnotation);
        } else {
            builderClass = createEmptyBuilderClass(psiClass, psiAnnotation);
        }
        final List<TaleMetaInfo> taleMetaInfos = createTableMetaInfos(psiAnnotation, psiClass, psiMethod, builderClass);

        final PsiClass parentBuilderClass = JavaFileManager.getInstance(psiClass.getProject()).findClass(BaseMeta.class.getName(), psiClass.getResolveScope());
        PsiType psiClassType = taleMetaInfos.stream().findFirst().get().getBuilderType();
        final PsiClassType extendsType = getTypeWithSpecificTypeParameters(parentBuilderClass, psiClassType);
        builderClass.withExtends(extendsType);

        builderClass.withMethods(createConstructors(builderClass, psiAnnotation));

        // create builder Fields
        taleMetaInfos.stream()
                .map(TaleMetaInfo::renderBuilderFields)
                .forEach(builderClass::withFields);

        return builderClass;
    }

    @NotNull
    private PsiClassType getTypeWithSpecificTypeParameters(@NotNull PsiClass psiClass, @NotNull PsiType... psiTypes) {
        final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        final PsiTypeParameter[] classTypeParameters = psiClass.getTypeParameters();
        final int substituteTypesCount = psiTypes.length;
        if (classTypeParameters.length >= substituteTypesCount) {
            PsiSubstitutor newSubstitutor = PsiSubstitutor.EMPTY;
            final int fromIndex = classTypeParameters.length - substituteTypesCount;
            for (int i = 0; i < fromIndex; i++) {
                newSubstitutor = newSubstitutor.put(classTypeParameters[i], elementFactory.createType(classTypeParameters[i]));
            }
            for (int i = fromIndex; i < classTypeParameters.length; i++) {
                newSubstitutor = newSubstitutor.put(classTypeParameters[i], psiTypes[i - fromIndex]);
            }
            return elementFactory.createType(psiClass, newSubstitutor);
        }
        return elementFactory.createType(psiClass);
    }

    @NotNull
    private LombokLightClassBuilder createEmptyBuilderClass(@NotNull PsiClass psiClass, @NotNull PsiMethod psiMethod, @NotNull PsiAnnotation psiAnnotation) {
        return createBuilderClass(psiClass, psiMethod,
                psiMethod.isConstructor() || psiMethod.hasModifierProperty(PsiModifier.STATIC), psiAnnotation);
    }

    @NotNull
    private LombokLightClassBuilder createEmptyBuilderClass(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation) {
        return createBuilderClass(psiClass, psiClass, true, psiAnnotation);
    }

    public Optional<PsiClass> createBuilderClassIfNotExist(@NotNull PsiClass psiClass, @Nullable PsiMethod psiMethod, @NotNull PsiAnnotation psiAnnotation) {
        PsiClass builderClass = null;
        if (!getExistInnerBuilderClass(psiClass, psiMethod, psiAnnotation).isPresent()) {
            builderClass = createBuilderClass(psiClass, psiMethod, psiAnnotation);
        }
        return Optional.ofNullable(builderClass);
    }

    @NotNull
    private LombokLightClassBuilder createBuilderClass(@NotNull PsiClass psiClass, @NotNull PsiTypeParameterListOwner psiTypeParameterListOwner, final boolean isStatic, @NotNull PsiAnnotation psiAnnotation) {
        PsiMethod psiMethod = null;
        if (psiTypeParameterListOwner instanceof PsiMethod) {
            psiMethod = (PsiMethod) psiTypeParameterListOwner;
        }

        final String builderClassName = getBuilderClassName(psiClass, psiAnnotation, psiMethod);
        final String builderClassQualifiedName = psiClass.getQualifiedName() + "." + builderClassName;

        final LombokLightClassBuilder classBuilder = new LombokLightClassBuilder(psiClass, builderClassName, builderClassQualifiedName)
                .withContainingClass(psiClass)
                .withNavigationElement(psiAnnotation)
                .withParameterTypes((null != psiMethod && psiMethod.isConstructor()) ? psiClass.getTypeParameterList() : psiTypeParameterListOwner.getTypeParameterList())
                .withModifier(getBuilderOuterAccessVisibility(psiAnnotation));
        if (isStatic) {
            classBuilder.withModifier(PsiModifier.STATIC);
        }
        return classBuilder;
    }

    @NotNull
    public Collection<PsiMethod> createConstructors(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation) {
        return Collections.emptySet();
    }


    void addTypeParameters(@NotNull PsiClass builderClass, @Nullable PsiMethod psiMethod, @NotNull LombokLightMethodBuilder methodBuilder) {
        final PsiTypeParameter[] psiTypeParameters;
        if (null == psiMethod || psiMethod.isConstructor()) {
            psiTypeParameters = builderClass.getTypeParameters();
        } else {
            psiTypeParameters = psiMethod.getTypeParameters();
        }

        for (PsiTypeParameter psiTypeParameter : psiTypeParameters) {
            methodBuilder.withTypeParameter(psiTypeParameter);
        }
    }
}