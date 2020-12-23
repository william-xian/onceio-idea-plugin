package top.onceio.plugins.processor.field;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiVariable;
import top.onceio.plugins.config.ConfigKey;
import top.onceio.plugins.util.PsiAnnotationSearchUtil;
import top.onceio.plugins.util.PsiAnnotationUtil;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.plugins.config.OnceIOConfigDiscovery;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Plushnikov Michail
 */
public class OnceIOAccessorsInfo {
    public static final OnceIOAccessorsInfo EMPTY = new OnceIOAccessorsInfo(false, false, false);

    private final boolean fluent;
    private final boolean chain;
    private final String[] prefixes;
    private final boolean doNotUseIsPrefix;

    private OnceIOAccessorsInfo(boolean fluentValue, boolean chainValue, boolean doNotUseIsPrefix, String... prefixes) {
        this.fluent = fluentValue;
        this.chain = chainValue;
        this.doNotUseIsPrefix = doNotUseIsPrefix;
        this.prefixes = null == prefixes ? new String[0] : prefixes;
    }

    @NotNull
    public static OnceIOAccessorsInfo build(boolean fluentValue, boolean chainValue, boolean doNotUseIsPrefix, String... prefixes) {
        return new OnceIOAccessorsInfo(fluentValue, chainValue, doNotUseIsPrefix, prefixes);
    }

    @NotNull
    public static OnceIOAccessorsInfo build(@NotNull PsiField psiField) {
        return build(psiField, psiField.getContainingClass());
    }

    @NotNull
    public static OnceIOAccessorsInfo build(@NotNull PsiVariable psiVariable, @Nullable PsiClass containingClass) {
        final PsiAnnotation accessorsFieldAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiVariable, Accessors.class);
        if (null != accessorsFieldAnnotation) {
            return buildFromAnnotation(accessorsFieldAnnotation, containingClass);
        } else {
            return build(containingClass);
        }
    }

    @NotNull
    public static OnceIOAccessorsInfo build(@NotNull PsiField psiField, @NotNull OnceIOAccessorsInfo classAccessorsInfo) {
        final PsiAnnotation accessorsFieldAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, Accessors.class);
        if (null != accessorsFieldAnnotation) {
            return buildFromAnnotation(accessorsFieldAnnotation, psiField.getContainingClass());
        } else {
            return classAccessorsInfo;
        }
    }

    @NotNull
    public static OnceIOAccessorsInfo build(@Nullable PsiClass psiClass) {
        PsiClass containingClass = psiClass;
        while (null != containingClass) {
            final PsiAnnotation accessorsClassAnnotation = PsiAnnotationSearchUtil.findAnnotation(containingClass, Accessors.class);
            if (null != accessorsClassAnnotation) {
                return buildFromAnnotation(accessorsClassAnnotation, containingClass);
            }
            containingClass = containingClass.getContainingClass();
        }

        return buildAccessorsInfo(psiClass, null, null, Collections.emptySet());
    }

    @NotNull
    private static OnceIOAccessorsInfo buildFromAnnotation(@NotNull PsiAnnotation accessorsAnnotation, @Nullable PsiClass psiClass) {
        Boolean chainDeclaredValue = PsiAnnotationUtil.getDeclaredBooleanAnnotationValue(accessorsAnnotation, "chain");
        Boolean fluentDeclaredValue = PsiAnnotationUtil.getDeclaredBooleanAnnotationValue(accessorsAnnotation, "fluent");
        Collection<String> prefixes = PsiAnnotationUtil.getAnnotationValues(accessorsAnnotation, "prefix", String.class);

        return buildAccessorsInfo(psiClass, chainDeclaredValue, fluentDeclaredValue, prefixes);
    }

    @NotNull
    private static OnceIOAccessorsInfo buildAccessorsInfo(@Nullable PsiClass psiClass, @Nullable Boolean chainDeclaredValue,
                                                          @Nullable Boolean fluentDeclaredValue, @NotNull Collection<String> prefixDeclared) {
        final boolean isFluent;
        final boolean isChained;
        final boolean doNotUseIsPrefix;
        final String[] prefixes;

        if (null != psiClass) {
            final OnceIOConfigDiscovery onceIOConfigDiscovery = OnceIOConfigDiscovery.getInstance();
            if (null == fluentDeclaredValue) {
                isFluent = onceIOConfigDiscovery.getBooleanOnceIOConfigProperty(ConfigKey.ACCESSORS_FLUENT, psiClass);
            } else {
                isFluent = fluentDeclaredValue;
            }

            if (null == chainDeclaredValue) {
                isChained = onceIOConfigDiscovery.getBooleanOnceIOConfigProperty(ConfigKey.ACCESSORS_CHAIN, psiClass);
            } else {
                isChained = chainDeclaredValue;
            }

            if (prefixDeclared.isEmpty()) {
                prefixes = onceIOConfigDiscovery.getMultipleValueOnceIOConfigProperty(ConfigKey.ACCESSORS_PREFIX, psiClass);
            } else {
                prefixes = prefixDeclared.toArray(new String[0]);
            }

            doNotUseIsPrefix = onceIOConfigDiscovery.getBooleanOnceIOConfigProperty(ConfigKey.GETTER_NO_IS_PREFIX, psiClass);

        } else {
            isFluent = null == fluentDeclaredValue ? false : fluentDeclaredValue;
            isChained = null == chainDeclaredValue ? false : chainDeclaredValue;
            prefixes = prefixDeclared.toArray(new String[0]);
            doNotUseIsPrefix = false;
        }

        boolean isChainDeclaredOrImplicit = isChained || (isFluent && null == chainDeclaredValue);
        return new OnceIOAccessorsInfo(isFluent, isChainDeclaredOrImplicit, doNotUseIsPrefix, prefixes);
    }

    public boolean isFluent() {
        return fluent;
    }

    public OnceIOAccessorsInfo withFluent(boolean fluentValue) {
        if (fluent == fluentValue) {
            return this;
        }
        return new OnceIOAccessorsInfo(fluentValue, chain, doNotUseIsPrefix, prefixes);
    }

    public boolean isChain() {
        return chain;
    }

    public boolean isDoNotUseIsPrefix() {
        return doNotUseIsPrefix;
    }

    public String[] getPrefixes() {
        return prefixes;
    }

    public boolean isPrefixUnDefinedOrNotStartsWith(String fieldName) {
        if (prefixes.length == 0) {
            return false;
        }

        for (String prefix : prefixes) {
            if (canPrefixApply(fieldName, prefix)) {
                return false;
            }
        }
        return true;
    }

    public String removePrefix(String fieldName) {
        for (String prefix : prefixes) {
            if (canPrefixApply(fieldName, prefix)) {
                return prefix.isEmpty() ? fieldName : decapitalizeLikeOnceIO(fieldName.substring(prefix.length()));
            }
        }
        return fieldName;
    }

    private boolean canPrefixApply(String fieldName, String prefix) {
        final int prefixLength = prefix.length();
        // we can use digits and upper case letters after a prefix, but not lower case letters
        return prefixLength == 0 ||
                fieldName.startsWith(prefix) && fieldName.length() > prefixLength &&
                        (!Character.isLetter(prefix.charAt(prefix.length() - 1)) || !Character.isLowerCase(fieldName.charAt(prefixLength)));
    }

    private String decapitalizeLikeOnceIO(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
}