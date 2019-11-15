package fi.bitrite.android.ws.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MapsForgeTheme implements Serializable {
    private String name;
    private String id;
    private Map<String, String> localizedNames;
    private String filePath;

    public MapsForgeTheme(String name, String id, String filePath) {
        this.name = name;
        this.id = id;
        this.filePath = filePath;
        localizedNames = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getLocalizedDisplayName(String lang) {
        if (!localizedNames.containsKey(lang)) {
            lang = "en";
        }
        if (localizedNames.get(lang) != null) {
            return localizedNames.get(lang) + " (" + name + ")";
        } else {
            return name;
        }
    }

    public void addLocalizedName(String lang, String name) {
        localizedNames.put(lang, name);
    }
}
