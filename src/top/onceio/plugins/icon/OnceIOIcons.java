package top.onceio.plugins.icon;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class OnceIOIcons {
  private static Icon load(String path) {
    return IconLoader.getIcon(path, OnceIOIcons.class);
  }

  public static final Icon CLASS_ICON = load("/icons/nodes/lombokClass.png");
  public static final Icon FIELD_ICON = load("/icons/nodes/lombokField.png");
  public static final Icon METHOD_ICON = load("/icons/nodes/lombokMethod.png");

  public static final Icon CONFIG_FILE_ICON = load("/icons/config.png");
}
