package top.onceio.plugins.activity;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.notification.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.awt.RelativePoint;
import top.onceio.plugins.OnceIOBundle;
import top.onceio.plugins.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.onceio.plugins.settings.ProjectSettings;

import javax.swing.event.HyperlinkEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnceIOProjectValidatorActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        // If plugin is not enabled - no point to continue
        if (!ProjectSettings.isOnceIOEnabledInProject(project)) {
            return;
        }

        final boolean hasOnceIOLibrary = hasOnceIOLibrary(project);

        NotificationGroup group = NotificationGroup.findRegisteredGroup(Version.PLUGIN_NAME);
        if (group == null) {
            group = new NotificationGroup(Version.PLUGIN_NAME, NotificationDisplayType.BALLOON, true);
        }

        // If dependency is missing and missing dependency notification setting is enabled (defaults to disabled)
        if (!hasOnceIOLibrary && ProjectSettings.isEnabled(project, ProjectSettings.IS_MISSING_ONCEIO_CHECK_ENABLED, false)) {
            Notification notification = group.createNotification(OnceIOBundle.message("config.warn.dependency.missing.title"),
                    OnceIOBundle.message("config.warn.dependency.missing.message", project.getName()),
                    NotificationType.ERROR, NotificationListener.URL_OPENING_LISTENER);

            Notifications.Bus.notify(notification, project);
        }

        // If dependency is present and out of date notification setting is enabled (defaults to disabled)
        if (hasOnceIOLibrary && ProjectSettings.isEnabled(project, ProjectSettings.IS_ONCEIO_VERSION_CHECK_ENABLED, false)) {
            final ModuleManager moduleManager = ModuleManager.getInstance(project);
            for (Module module : moduleManager.getModules()) {
                String onceioVersion = parseOnceIOVersion(findOnceIOEntry(ModuleRootManager.getInstance(module)));

                if (null != onceioVersion && compareVersionString(onceioVersion, Version.LAST_ONCEIO_VERSION) < 0) {
                    Notification notification = group.createNotification(OnceIOBundle.message("config.warn.dependency.outdated.title"),
                            OnceIOBundle.message("config.warn.dependency.outdated.message", project.getName(), module.getName(), onceioVersion, Version.LAST_ONCEIO_VERSION),
                            NotificationType.WARNING, NotificationListener.URL_OPENING_LISTENER);

                    Notifications.Bus.notify(notification, project);
                }
            }
        }

        // Annotation Processing check
        boolean annotationProcessorsEnabled = hasAnnotationProcessorsEnabled(project);
        if (hasOnceIOLibrary && !annotationProcessorsEnabled &&
                ProjectSettings.isEnabled(project, ProjectSettings.IS_ANNOTATION_PROCESSING_CHECK_ENABLED, true)) {

            suggestEnableAnnotations(project, group);
        }
    }

    private void suggestEnableAnnotations(Project project, NotificationGroup group) {
        Notification notification = group.createNotification(OnceIOBundle.message("config.warn.annotation-processing.disabled.title"),
                OnceIOBundle.message("config.warn.annotation-processing.disabled.message", project.getName()),
                NotificationType.ERROR,
                (not, e) -> {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        enableAnnotations(project);
                        not.expire();
                    }
                });

        Notifications.Bus.notify(notification, project);
    }

    private void enableAnnotations(Project project) {
        getCompilerConfiguration(project).getDefaultProcessorProfile().setEnabled(true);

        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(
                        "Java annotation processing has been enabled",
                        MessageType.INFO,
                        null
                )
                .setFadeoutTime(3000)
                .createBalloon()
                .show(RelativePoint.getNorthEastOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    private CompilerConfigurationImpl getCompilerConfiguration(Project project) {
        return (CompilerConfigurationImpl) CompilerConfiguration.getInstance(project);
    }

    private boolean hasAnnotationProcessorsEnabled(Project project) {
        CompilerConfigurationImpl compilerConfiguration = getCompilerConfiguration(project);
        return compilerConfiguration.getDefaultProcessorProfile().isEnabled();
    }

    private boolean hasOnceIOLibrary(Project project) {
        PsiPackage onceioPackage;
        try {
            onceioPackage = JavaPsiFacade.getInstance(project).findPackage("onceio");
        } catch (ProcessCanceledException ex) {
            onceioPackage = null;
        }
        return onceioPackage != null;
    }

    @Nullable
    private OrderEntry findOnceIOEntry(@NotNull ModuleRootManager moduleRootManager) {
        final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
        for (OrderEntry orderEntry : orderEntries) {
            if (orderEntry.getPresentableName().contains("onceio")) {
                return orderEntry;
            }
        }
        return null;
    }

    @Nullable
    String parseOnceIOVersion(@Nullable OrderEntry orderEntry) {
        String result = null;
        if (null != orderEntry) {
            final String presentableName = orderEntry.getPresentableName();
            Pattern pattern = Pattern.compile("(.*:)([\\d.]+)(.*)");
            final Matcher matcher = pattern.matcher(presentableName);
            if (matcher.find()) {
                result = matcher.group(2);
            }
        }
        return result;
    }

    int compareVersionString(@NotNull String firstVersionOne, @NotNull String secondVersion) {
        String[] firstVersionParts = firstVersionOne.split("\\.");
        String[] secondVersionParts = secondVersion.split("\\.");
        int length = Math.max(firstVersionParts.length, secondVersionParts.length);
        for (int i = 0; i < length; i++) {
            int firstPart = i < firstVersionParts.length && !firstVersionParts[i].isEmpty() ?
                    Integer.parseInt(firstVersionParts[i]) : 0;
            int secondPart = i < secondVersionParts.length && !secondVersionParts[i].isEmpty() ?
                    Integer.parseInt(secondVersionParts[i]) : 0;
            if (firstPart < secondPart) {
                return -1;
            }
            if (firstPart > secondPart) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Simple {@link NotificationListener.Adapter} that opens Settings Page for correct dialog.
     */
    private static class SettingsOpeningListener extends NotificationListener.Adapter {

        private final Project project;
        private final String nameToSelect;

        SettingsOpeningListener(Project project, String nameToSelect) {
            this.project = project;
            this.nameToSelect = nameToSelect;
        }

        @Override
        protected void hyperlinkActivated(@NotNull final Notification notification, @NotNull final HyperlinkEvent e) {

            if (!project.isDisposed()) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, nameToSelect);
            }
        }
    }
}
