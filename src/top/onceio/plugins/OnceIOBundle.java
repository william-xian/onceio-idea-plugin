package top.onceio.plugins;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

/**
 * {@link java.util.ResourceBundle}/localization utils for the onceio plugin.
 */
public class OnceIOBundle {
  /**
   * The {@link java.util.ResourceBundle} path.
   */
  @NonNls
  private static final String BUNDLE_NAME = "messages.onceioBundle";

  /**
   * The {@link java.util.ResourceBundle} instance.
   */
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  private OnceIOBundle() {
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
    return AbstractBundle.message(BUNDLE, key, params);
  }
}
