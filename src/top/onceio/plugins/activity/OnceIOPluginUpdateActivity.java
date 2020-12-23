package top.onceio.plugins.activity;

import com.intellij.notification.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import top.onceio.plugins.LombokBundle;
import top.onceio.plugins.Version;
import org.jetbrains.annotations.NotNull;
import top.onceio.plugins.settings.OnceIOSettings;

public class OnceIOPluginUpdateActivity implements StartupActivity, DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        final OnceIOSettings settings = OnceIOSettings.getInstance();
        boolean updated = !Version.PLUGIN_VERSION.equals(settings.getVersion());
        if (updated) {
            settings.setVersion(Version.PLUGIN_VERSION);

            NotificationGroup group = new NotificationGroup(Version.PLUGIN_NAME, NotificationDisplayType.STICKY_BALLOON, true);
            Notification notification = group.createNotification(
                    LombokBundle.message("daemon.donate.title", Version.PLUGIN_VERSION),
                    LombokBundle.message("daemon.donate.content"),
                    NotificationType.INFORMATION,
                    new NotificationListener.UrlOpeningListener(false)
            );

            Notifications.Bus.notify(notification, project);
        }
    }

}