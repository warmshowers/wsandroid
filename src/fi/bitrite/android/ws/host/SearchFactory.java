package fi.bitrite.android.ws.host;
import com.google.android.gms.maps.model.LatLng;

public interface SearchFactory {
    public Search createTextSearch(String keyword);
    public Search createMapSearch(LatLng northEast, LatLng southWest, int numHostsCutoff);
}
