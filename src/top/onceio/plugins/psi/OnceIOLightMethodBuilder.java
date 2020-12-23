package top.onceio.plugins.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.impl.light.LightTypeParameterListBuilder;
import com.intellij.util.IncorrectOperationException;
import top.onceio.plugins.icon.OnceIOIcons;
import top.onceio.plugins.util.ReflectionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @author Liar
 */
public class OnceIOLightMethodBuilder extends LightMethodBuilder {
  private PsiMethod myMethod;
  private ASTNode myASTNode;
  private PsiCodeBlock myBodyCodeBlock;
  // used to simplify comparing of returnType in equal method
  private String myReturnTypeAsText;

  public OnceIOLightMethodBuilder(@NotNull PsiManager manager, @NotNull String name) {
    super(manager, JavaLanguage.INSTANCE, name,
      new OnceIOLightParameterListBuilder(manager, JavaLanguage.INSTANCE),
      new OnceIOLightModifierList(manager, JavaLanguage.INSTANCE, Collections.emptySet()),
      new OnceIOLightReferenceListBuilder(manager, JavaLanguage.INSTANCE, PsiReferenceList.Role.THROWS_LIST),
      new LightTypeParameterListBuilder(manager, JavaLanguage.INSTANCE));
    setBaseIcon(OnceIOIcons.METHOD_ICON);
  }

  public OnceIOLightMethodBuilder withNavigationElement(PsiElement navigationElement) {
    setNavigationElement(navigationElement);
    return this;
  }

  public OnceIOLightMethodBuilder withModifier(@PsiModifier.ModifierConstant @NotNull @NonNls String modifier) {
    addModifier(modifier);
    return this;
  }

  public OnceIOLightMethodBuilder withModifier(@PsiModifier.ModifierConstant @NotNull @NonNls String... modifiers) {
    for (String modifier : modifiers) {
      addModifier(modifier);
    }
    return this;
  }

  public OnceIOLightMethodBuilder withMethodReturnType(PsiType returnType) {
    setMethodReturnType(returnType);
    return this;
  }

  @Override
  public LightMethodBuilder setMethodReturnType(PsiType returnType) {
    myReturnTypeAsText = returnType.getPresentableText();
    return super.setMethodReturnType(returnType);
  }

  public OnceIOLightMethodBuilder withFinalParameter(@NotNull String name, @NotNull PsiType type) {
    final OnceIOLightParameter onceIOLightParameter = createParameter(name, type);
    onceIOLightParameter.setModifiers(PsiModifier.FINAL);
    return withParameter(onceIOLightParameter);
  }

  public OnceIOLightMethodBuilder withParameter(@NotNull String name, @NotNull PsiType type) {
    return withParameter(createParameter(name, type));
  }

  @NotNull
  private OnceIOLightParameter createParameter(@NotNull String name, @NotNull PsiType type) {
    return new OnceIOLightParameter(name, type, this, JavaLanguage.INSTANCE);
  }

  public OnceIOLightMethodBuilder withParameter(@NotNull PsiParameter psiParameter) {
    addParameter(psiParameter);
    return this;
  }

  public OnceIOLightMethodBuilder withException(@NotNull PsiClassType type) {
    addException(type);
    return this;
  }

  public OnceIOLightMethodBuilder withContainingClass(@NotNull PsiClass containingClass) {
    setContainingClass(containingClass);
    return this;
  }

  public OnceIOLightMethodBuilder withTypeParameter(@NotNull PsiTypeParameter typeParameter) {
    addTypeParameter(typeParameter);
    return this;
  }

  public OnceIOLightMethodBuilder withConstructor(boolean isConstructor) {
    setConstructor(isConstructor);
    return this;
  }

  public OnceIOLightMethodBuilder withBody(@NotNull PsiCodeBlock codeBlock) {
    myBodyCodeBlock = codeBlock;
    return this;
  }

  public OnceIOLightMethodBuilder withAnnotation(@NotNull String annotation) {
    getModifierList().addAnnotation(annotation);
    return this;
  }

  public OnceIOLightMethodBuilder withAnnotations(Collection<String> annotations) {
    final PsiModifierList modifierList = getModifierList();
    annotations.forEach(modifierList::addAnnotation);
    return this;
  }

  // add Parameter as is, without wrapping with LightTypeParameter
  public LightMethodBuilder addTypeParameter(PsiTypeParameter parameter) {
    ((LightTypeParameterListBuilder) getTypeParameterList()).addParameter(parameter);
    return this;
  }

  @Override
  public PsiCodeBlock getBody() {
    return myBodyCodeBlock;
  }

  @Override
  public PsiIdentifier getNameIdentifier() {
    return new OnceIOLightIdentifier(myManager, getName());
  }

  @Override
  public PsiElement getParent() {
    PsiElement result = super.getParent();
    result = null != result ? result : getContainingClass();
    return result;
  }

  @Nullable
  @Override
  public PsiFile getContainingFile() {
    PsiClass containingClass = getContainingClass();
    return containingClass != null ? containingClass.getContainingFile() : null;
  }

  @Override
  public String getText() {
    ASTNode node = getNode();
    if (null != node) {
      return node.getText();
    }
    return "";
  }

  @Override
  public ASTNode getNode() {
    if (null == myASTNode) {
      final PsiElement myPsiMethod = getOrCreateMyPsiMethod();
      myASTNode = null == myPsiMethod ? null : myPsiMethod.getNode();
    }
    return myASTNode;
  }

  @Override
  public TextRange getTextRange() {
    TextRange r = super.getTextRange();
    return r == null ? TextRange.EMPTY_RANGE : r;
  }

  private String getAllModifierProperties(LightModifierList modifierList) {
    final StringBuilder builder = new StringBuilder();
    for (String modifier : modifierList.getModifiers()) {
      if (!PsiModifier.PACKAGE_LOCAL.equals(modifier)) {
        builder.append(modifier).append(' ');
      }
    }
    return builder.toString();
  }

  private PsiMethod rebuildMethodFromString() {
    PsiMethod result;
    try {
      final StringBuilder methodTextDeclaration = new StringBuilder();
      methodTextDeclaration.append(getAllModifierProperties((LightModifierList) getModifierList()));
      PsiType returnType = getReturnType();
      if (null != returnType && returnType.isValid()) {
        methodTextDeclaration.append(returnType.getCanonicalText()).append(' ');
      }
      methodTextDeclaration.append(getName());
      methodTextDeclaration.append('(');
      if (getParameterList().getParametersCount() > 0) {
        for (PsiParameter parameter : getParameterList().getParameters()) {
          methodTextDeclaration.append(parameter.getType().getCanonicalText()).append(' ').append(parameter.getName()).append(',');
        }
        methodTextDeclaration.deleteCharAt(methodTextDeclaration.length() - 1);
      }
      methodTextDeclaration.append(')');
      methodTextDeclaration.append('{').append("  ").append('}');

      final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(getManager().getProject());

      result = elementFactory.createMethodFromText(methodTextDeclaration.toString(), getContainingClass());
      if (null != getBody()) {
        result.getBody().replace(getBody());
      }
    } catch (Exception ex) {
      result = null;
    }
    return result;
  }

  @Override
  public PsiElement copy() {
    final PsiElement myPsiMethod = getOrCreateMyPsiMethod();
    return null == myPsiMethod ? null : myPsiMethod.copy();
  }

  private PsiElement getOrCreateMyPsiMethod() {
    if (null == myMethod) {
      myMethod = rebuildMethodFromString();
    }
    return myMethod;
  }

  @NotNull
  @Override
  public PsiElement[] getChildren() {
    final PsiElement myPsiMethod = getOrCreateMyPsiMethod();
    return null == myPsiMethod ? PsiElement.EMPTY_ARRAY : myPsiMethod.getChildren();
  }

  public String toString() {
    return "OnceIOLightMethodBuilder: " + getName();
  }

  @Override
  public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
    // just add new element to the containing class
    final PsiClass containingClass = getContainingClass();
    if (null != containingClass) {
      CheckUtil.checkWritable(containingClass);
      return containingClass.add(newElement);
    }
    return null;
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    ReflectionUtil.setFinalFieldPerReflection(LightMethodBuilder.class, this, String.class, name);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OnceIOLightMethodBuilder that = (OnceIOLightMethodBuilder) o;

    if (!getName().equals(that.getName())) {
      return false;
    }
    if (isConstructor() != that.isConstructor()) {
      return false;
    }
    final PsiClass containingClass = getContainingClass();
    final PsiClass thatContainingClass = that.getContainingClass();
    if (containingClass != null ? !containingClass.equals(thatContainingClass) : thatContainingClass != null) {
      return false;
    }
    if (!getModifierList().equals(that.getModifierList())) {
      return false;
    }
    if (!getParameterList().equals(that.getParameterList())) {
      return false;
    }
    return Objects.equals(myReturnTypeAsText, that.myReturnTypeAsText);
  }

  @Override
  public int hashCode() {
    // should be constant because of RenameJavaMethodProcessor#renameElement and fixNameCollisionsWithInnerClassMethod(...)
    return 1;
  }

  @Override
  public void delete() throws IncorrectOperationException {
    // simple do nothing
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    // simple do nothing
  }
}
