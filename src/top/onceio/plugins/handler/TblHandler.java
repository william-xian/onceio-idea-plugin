package top.onceio.plugins.handler;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightTypeParameterBuilder;
import de.plushnikov.intellij.plugin.problem.ProblemBuilder;
import de.plushnikov.intellij.plugin.processor.clazz.ToStringProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.constructor.NoArgsConstructorProcessor;
import de.plushnikov.intellij.plugin.psi.LombokLightClassBuilder;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import de.plushnikov.intellij.plugin.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.plugins.processor.clazz.tbl.TblClassProcessor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.util.text.StringUtil.capitalize;
import static com.intellij.openapi.util.text.StringUtil.replace;


/**
 * Handler methods for Builder-processing
 *
 * @author Tomasz Kalkosiński
 * @author Michail Plushnikov
 */
public class TblHandler {
    private final static String ANNOTATION_TABLE_META_CLASS_NAME = "tableMetaClassName";
    private static final String ANNOTATION_META_METHOD_NAME = "metaMethodName";
    private static final String ANNOTATION_SETTER_PREFIX = "setterPrefix";

    private final static String META_METHOD_NAME = "meta";

    private static final Collection<String> INVALID_ON_BUILDERS = Collections.unmodifiableSet(new HashSet<>());

    public TblHandler() {
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
            if (result) {
                final Collection<TaleMetaInfo> taleMetaInfos = createTableMetaInfos(psiClass, null).collect(Collectors.toList());
                result = validateSingular(taleMetaInfos, problemBuilder) &&
                        validateBuilderDefault(taleMetaInfos, problemBuilder) &&
                        validateObtainViaAnnotations(taleMetaInfos.stream(), problemBuilder);
            }
        }
        return result;
    }

    private boolean validateBuilderDefault(@NotNull Collection<TaleMetaInfo> taleMetaInfos, @NotNull ProblemBuilder problemBuilder) {
        final Optional<TaleMetaInfo> anyBuilderDefaultAndSingulars = taleMetaInfos.stream()
                .filter(TaleMetaInfo::hasBuilderDefaultAnnotation).findAny();
        anyBuilderDefaultAndSingulars.ifPresent(taleMetaInfo -> {
                    problemBuilder.addError("@Builder.Default and @Singular cannot be mixed.");
                }
        );

        return !anyBuilderDefaultAndSingulars.isPresent();
    }

    public boolean validate(@NotNull PsiMethod psiMethod, @NotNull PsiAnnotation psiAnnotation, @NotNull ProblemBuilder problemBuilder) {
        final PsiClass psiClass = psiMethod.getContainingClass();
        boolean result = null != psiClass;
        if (result) {
            final String builderClassName = getBuilderClassName(psiClass, psiAnnotation, psiMethod);

            final Project project = psiAnnotation.getProject();
            final String builderMethodName = getMetaMethodName(psiAnnotation);
            result = validateBuilderIdentifier(builderClassName, project, problemBuilder) &&
                    (builderMethodName.isEmpty() || validateBuilderIdentifier(builderMethodName, project, problemBuilder)) &&
                    validateExistingBuilderClass(builderClassName, psiClass, problemBuilder);
            if (result) {
                final Stream<TaleMetaInfo> tableMetaInfos = createTableMetaInfos(psiClass, psiMethod);
                result = validateObtainViaAnnotations(tableMetaInfos, problemBuilder);
            }
        }
        return result;
    }

    private boolean validateSingular(Collection<TaleMetaInfo> taleMetaInfos, @NotNull ProblemBuilder problemBuilder) {
        AtomicBoolean result = new AtomicBoolean(true);

        return result.get();
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

    private boolean validateObtainViaAnnotations(Stream<TaleMetaInfo> tableMetaInfos, @NotNull ProblemBuilder problemBuilder) {
        AtomicBoolean result = new AtomicBoolean(true);
        tableMetaInfos.map(TaleMetaInfo::withObtainVia).filter(TaleMetaInfo::hasObtainViaAnnotation).forEach(taleMetaInfo ->
        {
            if (StringUtil.isEmpty(taleMetaInfo.getViaFieldName()) == StringUtil.isEmpty(taleMetaInfo.getViaMethodName())) {
                problemBuilder.addError("The syntax is either @ObtainVia(field = \"fieldName\") or @ObtainVia(method = \"methodName\").");
                result.set(false);
            }

            if (StringUtil.isEmpty(taleMetaInfo.getViaMethodName()) && taleMetaInfo.isViaStaticCall()) {
                problemBuilder.addError("@ObtainVia(isStatic = true) is not valid unless 'method' has been set.");
                result.set(false);
            }
        });
        return result.get();
    }

    public Optional<PsiClass> getExistInnerBuilderClass(@NotNull PsiClass psiClass, @Nullable PsiMethod psiMethod, @NotNull PsiAnnotation psiAnnotation) {
        final String builderClassName = getBuilderClassName(psiClass, psiAnnotation, psiMethod);
        return PsiClassUtil.getInnerClassInternByName(psiClass, builderClassName);
    }

    PsiType getReturnTypeOfBuildMethod(@NotNull PsiClass psiClass, @Nullable PsiMethod psiMethod) {
        final PsiType result;
        if (null == psiMethod || psiMethod.isConstructor()) {
            result = PsiClassUtil.getTypeWithGenerics(psiClass);
        } else {
            result = psiMethod.getReturnType();
        }
        return result;
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

    private PsiType calculateResultType(@NotNull List<TaleMetaInfo> taleMetaInfos, PsiClass builderPsiClass, PsiClass psiClass) {
        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        final PsiType[] psiTypes = taleMetaInfos.stream()
                .map(TaleMetaInfo::getObtainViaFieldVariableType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toArray(PsiType[]::new);
        return factory.createType(builderPsiClass, psiTypes);
    }

    @NotNull
    private Stream<TaleMetaInfo> createTableMetaInfos(@NotNull PsiClass psiClass, @Nullable PsiMethod psiClassMethod) {
        final Stream<TaleMetaInfo> result;
        if (null != psiClassMethod) {
            result = Arrays.stream(psiClassMethod.getParameterList().getParameters()).map(TaleMetaInfo::fromPsiParameter);
        } else {
            result = PsiClassUtil.collectClassFieldsIntern(psiClass).stream().map(TaleMetaInfo::fromPsiField)
                    .filter(TaleMetaInfo::useForBuilder).filter((t) -> {
                        t.setPsiClass(psiClass);
                        return t.useForBuilder();
                    });

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
    private String selectNonClashingNameFor(String classGenericName, Collection<String> typeParamStrings) {
        String result = classGenericName;
        if (typeParamStrings.contains(classGenericName)) {
            int counter = 2;
            do {
                result = classGenericName + counter++;
            } while (typeParamStrings.contains(result));
        }
        return result;
    }
    @NotNull
    public PsiClass createBuilderClass(@NotNull PsiClass psiClass, @Nullable PsiMethod psiMethod, @NotNull PsiAnnotation psiAnnotation) {
        String builderClassName = getBuilderClassName(psiClass);
        String builderClassQualifiedName = psiClass.getQualifiedName() + "." + builderClassName;

        final LombokLightClassBuilder baseClassBuilder = new LombokLightClassBuilder(psiClass, builderClassName, builderClassQualifiedName)
                .withContainingClass(psiClass)
                .withNavigationElement(psiAnnotation)
                .withParameterTypes(psiClass.getTypeParameterList())
                .withModifier(PsiModifier.PUBLIC)
                .withModifier(PsiModifier.STATIC)
                .withModifier(PsiModifier.ABSTRACT);

        final List<String> typeParamNames = Stream.of(psiClass.getTypeParameters()).map(PsiTypeParameter::getName).collect(Collectors.toList());

        final LightTypeParameterBuilder c = new LightTypeParameterBuilder(selectNonClashingNameFor("C", typeParamNames), baseClassBuilder, 0);
        c.getExtendsList().addReference(PsiClassUtil.getTypeWithGenerics(psiClass));
        baseClassBuilder.withParameterType(c);

        final LightTypeParameterBuilder b = new LightTypeParameterBuilder(selectNonClashingNameFor("B", typeParamNames), baseClassBuilder, 1);
        baseClassBuilder.withParameterType(b);
        b.getExtendsList().addReference(PsiClassUtil.getTypeWithGenerics(baseClassBuilder));

        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        final PsiClassType bType = factory.createType(b);
        final PsiClassType cType = factory.createType(c);

        LombokLightClassBuilder builderClass;
        if (null != psiMethod) {
            builderClass = createEmptyBuilderClass(psiClass, psiMethod, psiAnnotation);
        } else {
            builderClass = createEmptyBuilderClass(psiClass, psiAnnotation);
        }
        final PsiClass superClass = psiClass.getSuperClass();
        if (null != superClass && !"Object".equals(superClass.getName())) {
            final PsiClass parentBuilderClass = superClass.findInnerClassByName(getBuilderClassName(superClass), false);
            if (null != parentBuilderClass) {
                final PsiType[] explicitTypes = Stream.concat(
                        Stream.of(psiClass.getExtendsListTypes()).map(PsiClassType::getParameters).flatMap(Stream::of),
                        Stream.of(cType, bType))
                        .toArray(PsiType[]::new);

                final PsiClassType extendsType = getTypeWithSpecificTypeParameters(parentBuilderClass, explicitTypes);
                builderClass.withExtends(extendsType);
            }
        }

        builderClass.withMethods(createConstructors(builderClass, psiAnnotation));

        final List<TaleMetaInfo> taleMetaInfos = createTableMetaInfos(psiAnnotation, psiClass, psiMethod, builderClass);

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


    private boolean sameParameters(PsiParameter[] parameters, List<TaleMetaInfo> taleMetaInfos) {
        if (parameters.length != taleMetaInfos.size()) {
            return false;
        }

        final Iterator<TaleMetaInfo> tableMetaInfoIterator = taleMetaInfos.iterator();
        for (PsiParameter psiParameter : parameters) {
            final TaleMetaInfo taleMetaInfo = tableMetaInfoIterator.next();
            if (!psiParameter.getType().isAssignableFrom(taleMetaInfo.getFieldType())) {
                return false;
            }
        }
        return true;
    }


    @NotNull
    private String calculateCallExpressionForMethod(@NotNull PsiMethod psiMethod, @NotNull PsiClass builderClass) {
        final PsiClass containingClass = psiMethod.getContainingClass();

        StringBuilder className = new StringBuilder();
        if (null != containingClass) {
            className.append(containingClass.getName()).append(".");
            if (!psiMethod.isConstructor() && !psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                className.append("this.");
            }
            if (builderClass.hasTypeParameters()) {
                className.append(Arrays.stream(builderClass.getTypeParameters()).map(PsiTypeParameter::getName).collect(Collectors.joining(",", "<", ">")));
            }
        }
        return className + psiMethod.getName();
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