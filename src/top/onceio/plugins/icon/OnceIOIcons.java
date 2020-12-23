package top.onceio.plugins.icon;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class OnceIOIcons {
  private static Icon load(String path) {
    return IconLoader.getIcon(path, OnceIOIcons.class);
  }

  public static final Icon CLASS_ICON = load("/icons/nodes/onceioClass.png");
  public static final Icon FIELD_ICON = load("/icons/nodes/onceioField.png");
  public static final Icon METHOD_ICON = load("/icons/nodes/onceioMethod.png");

}
