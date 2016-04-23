package fi.bitrite.android.ws.util;

import fi.bitrite.android.ws.model.Host;

/**
 * Provide basic info that may not be accessible any other way.
 */
public class GlobalInfo {
    private static Host mMemberInfo = null;
    public static final String warmshowersCookieDomain = ".warmshowers.org";
    public static final String warmshowersBaseUrl = "https://www.warmshowers.org";
    
    // For testing with staging site
//    public static final String warmshowersCookieDomain = ".wsupg.net";
//    public static final String warmshowersBaseUrl = "http://www.wsupg.net";

}
