package technicfan.mpristoast;

public class MprisToastConfig {
    private boolean enabled = true;
    private String filter = "";
    private String preferred = "";

    public boolean getEnabled() {
        return enabled;
    }

    public String getFilter() {
        return filter;
    }

    public String getPreferred() {
        return preferred;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setPreferred(String preferred) {
        this.preferred = preferred;
    }
}
