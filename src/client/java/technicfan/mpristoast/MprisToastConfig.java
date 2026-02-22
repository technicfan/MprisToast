package technicfan.mpristoast;

public class MprisToastConfig {
    private boolean enabled = true;
    private boolean replace = false;
    private boolean onlyPreferred = false;
    private String preferred = "";

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

    protected void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected void setReplace(boolean replace) {
        this.replace = replace;
    }

    protected void setOnlyPreferred(boolean onlyPreferred) {
        this.onlyPreferred = onlyPreferred;
    }

    protected void setPreferred(String preferred) {
        this.preferred = preferred;
    }
}
