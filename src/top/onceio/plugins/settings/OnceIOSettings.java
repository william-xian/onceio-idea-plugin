package top.onceio.plugins.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.Nullable;

@State(
        name = "OnceIOSettings",
        storages = @Storage("onceio-plugin.xml")
)
public class OnceIOSettings implements PersistentStateComponent<OnceIOPluginState> {

    /**
     * Get the instance of this service.
     *
     * @return the unique {@link OnceIOSettings} instance.
     */
    public static OnceIOSettings getInstance() {
        return ServiceManager.getService(OnceIOSettings.class);
    }

    private OnceIOPluginState myState = new OnceIOPluginState();

    @Nullable
    @Override
    public OnceIOPluginState getState() {
        return myState;
    }

    @Override
    public void loadState(OnceIOPluginState element) {
        myState = element;
    }

    public String getVersion() {
        return myState.getPluginVersion();
    }

    public void setVersion(String version) {
        myState.setPluginVersion(version);
    }

}
