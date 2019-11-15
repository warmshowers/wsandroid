package fi.bitrite.android.ws.ui.util;

import android.content.Context;
import android.os.Environment;
import android.util.Xml;

import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.FragmentActivity;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.MapsForgeTheme;
import fi.bitrite.android.ws.repository.SettingsRepository;
import io.reactivex.disposables.Disposable;

public class OfflineMapHelper {

    public static final String TAG = "OfflineMapHelper";

    public static String defaultMapDataDirectory(Context context) {
        return context.getExternalFilesDir(null).toString() + "/";
    }

    public static boolean containsExistingFile(File[] files) {
        for (File f : files) {
            if (f.exists()) {
                return true;
            }
        }
        return false;
    }

    public static void searchOfflineMapData(SettingsRepository settingsRepository, boolean extendedSearchScope, FragmentActivity activity) {
        File[] startFolders;
        if (extendedSearchScope) {
            startFolders = new File[]{
                    new File(defaultMapDataDirectory(activity)),
                    Environment.getExternalStorageDirectory()};
        } else {
            startFolders = new File[]{
                    new File(defaultMapDataDirectory(activity))};
        }

        Disposable progressDisposable = ProgressDialog.create(R.string.map_search_in_progress)
                .showDelayed(activity, 100, TimeUnit.MILLISECONDS);

        List<String> mapfiles = OfflineMapHelper.findMapsforgeMapFiles(startFolders);
        settingsRepository.setAvailableOfflineMapSources(new HashSet<>(mapfiles));

        List<MapsForgeTheme> themes = new ArrayList<>();
        for (File themefile : OfflineMapHelper.findMapsforgeThemeFiles(startFolders)) {
            themes.addAll(OfflineMapHelper.getThemeStyles(themefile));
        }
        settingsRepository.setAvailableOfflineThemeSources(themes);

        progressDisposable.dispose();
    }

    private static List<String> findMapsforgeMapFiles(File[] rootFolders) {
        List<String> mapFiles = new ArrayList<>();
        for (File file : findFilesWithExtension(new ArrayList<>(), rootFolders, ".map")) {
            if (isMapsforgeBinaryOSM(file)) {
                mapFiles.add(file.getAbsolutePath());
            }
        }
        return mapFiles;
    }


    private static List<File> findMapsforgeThemeFiles(File[] rootFolders) {
        List<File> mapFiles = new ArrayList<>();
        for (File file : findFilesWithExtension(new ArrayList<>(), rootFolders, ".xml")) {
            if (isMapsforgeThemeXML(file)) {
                mapFiles.add(file);
            }
        }
        return mapFiles;
    }

    private static List<MapsForgeTheme> getThemeStyles(File theme) {
        XmlPullParser parser = Xml.newPullParser();
        try (InputStream in_s = new FileInputStream(theme)) {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in_s, null);
            return extractThemes(parser, theme.getAbsolutePath());
        } catch (XmlPullParserException|IOException ignored) {}

        return new ArrayList<>();
    }

    private static List<MapsForgeTheme> extractThemes(XmlPullParser parser, final String filePath)
            throws XmlPullParserException, IOException {

        List<MapsForgeTheme> themes = new ArrayList<>();

        int eventType = parser.getEventType();
        String elementName;
        MapsForgeTheme theme = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    elementName = parser.getName();
                    if ("layer".equalsIgnoreCase(elementName)) {
                        if ("true".equalsIgnoreCase(parser.getAttributeValue(null, "visible"))) {
                            theme = new MapsForgeTheme(new File(filePath).getName(), parser.getAttributeValue(null, "id"), filePath);
                        }
                    } else if (theme != null && "name".equalsIgnoreCase(elementName)) {
                        String lang = parser.getAttributeValue(null,"lang");
                        String localizedThemeName = parser.getAttributeValue(null,"value");
                        theme.addLocalizedName(lang, localizedThemeName);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    elementName = parser.getName();
                    if ("layer".equalsIgnoreCase(elementName) && theme != null ) {
                        themes.add(theme);
                        theme = null;
                    }
            }
            eventType = parser.next();
        }

        return themes;
    }

    public static void setThemeStyle(XmlRenderTheme theme, String id) {
        theme.setMenuCallback(themestyle -> {
            String themeId = id.isEmpty() ? themestyle.getDefaultValue() : id;
            XmlRenderThemeStyleLayer baseLayer = themestyle.getLayer(themeId);
            if (baseLayer == null) {
                return null;
            }

            // add the categories from overlays
            Set<String> result = baseLayer.getCategories();
            for (XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
                result.addAll(overlay.getCategories());
            }

            return result;
        });
    }


    private static List<File> findFilesWithExtension(List<File> foundFiles, File[] filesArray, String ext) {
        if (filesArray == null) {
            return foundFiles;
        }

        for (File file : filesArray) {
            if (file.isDirectory()) {
                findFilesWithExtension(foundFiles,file.listFiles(path ->
                        (path.isDirectory() || path.getName().toLowerCase().endsWith(ext))), ext);
            } else if (file.getName().toLowerCase().endsWith(ext)) {
                foundFiles.add(file);
            }
        }
        return foundFiles;
    }


    private static boolean isMapsforgeBinaryOSM(File file) {
        // https://github.com/mapsforge/mapsforge/blob/master/docs/Specification-Binary-Map-File.md
        int magicByteSize = 20;
        String magicByteString = "mapsforge binary OSM";

        byte[] buffer = new byte[magicByteSize];
        try (InputStream is = new FileInputStream(file.getAbsolutePath())) {
            is.read(buffer);
        } catch (IOException ignored) { }
        return new String(buffer).contentEquals(magicByteString);
    }

    private static boolean isMapsforgeThemeXML(File file) {
        String identifier = "http://mapsforge.org/renderTheme";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(identifier)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
