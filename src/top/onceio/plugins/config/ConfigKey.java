package top.onceio.plugins.config;

public enum ConfigKey {
  ACCESSORS_PREFIX("onceio.accessors.prefix", ""),
  ACCESSORS_CHAIN("onceio.accessors.chain", "false"),
  ACCESSORS_FLUENT("onceio.accessors.fluent", "false"),
  GETTER_NO_IS_PREFIX("onceio.getter.noIsPrefix", "false");

  private final String configKey;
  private final String configDefaultValue;

  ConfigKey(String configKey, String configDefaultValue) {
    this.configKey = configKey.toLowerCase();
    this.configDefaultValue = configDefaultValue;
  }

  public String getConfigKey() {
    return configKey;
  }

  public String getConfigDefaultValue() {
    return configDefaultValue;
  }

  public static ConfigKey fromConfigStringKey(String configStringKey) {
    for (ConfigKey keys : ConfigKey.values()) {
      if (keys.getConfigKey().equalsIgnoreCase(configStringKey)) {
        return keys;
      }
    }
    return null;
  }
}
