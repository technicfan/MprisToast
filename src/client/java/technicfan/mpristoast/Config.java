package technicfan.mpristoast;

public class Config {
    private final boolean enabled;
    private final boolean replace;
    private final boolean onlyPreferred;
    private final String preferred;
    private final String preferredDisplay;

    protected Config() {
        this.enabled = true;
        this.replace = false;
        this.onlyPreferred = false;
        this.preferred = "";
        this.preferredDisplay = "";
    }
    
    private Config(boolean enabled, boolean replace, boolean onlyPreferred, String preferred, String preferredDisplay) {
        this.enabled = enabled;
        this.replace = replace;
        this.onlyPreferred = onlyPreferred;
        this.preferred = preferred;
        this.preferredDisplay = preferredDisplay;
    }

    protected boolean getEnabled() {
        return enabled;
    }

    protected boolean getReplace() {
        return replace;
    }

    protected boolean getOnlyPreferred() {
        return onlyPreferred;
    }

    protected String getPreferred() {
        return preferred;
    }

    protected String getBusName() {
        return MediaTracker.busPrefix + preferred;
    }

    protected String getDisplayName() {
        return preferredDisplay;
    }

    protected Config setEnabled(boolean enabled) {
        return new Config(enabled, replace, onlyPreferred, preferred, preferredDisplay);
    }

    protected Config setReplace(boolean replace) {
        return new Config(enabled, replace, onlyPreferred, preferred, preferredDisplay);
    }

    protected Config setOnlyPreferred(boolean onlyPreferred) {
        return new Config(enabled, replace, onlyPreferred, preferred, preferredDisplay);
    }

    protected Config setPreferred(String preferred, String preferredDisplay) {
        return new Config(enabled, replace, onlyPreferred, preferred, preferredDisplay);
    }
}
