package fi.bitrite.android.ws.activity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.ArrayList;

import java.util.concurrent.ConcurrentHashMap;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.impl.RestMapSearch;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.WSNonHierarchicalDistanceBasedAlgorithm;
import fi.bitrite.android.ws.util.http.HttpException;


public class Maps2Activity extends FragmentActivity implements
        ClusterManager.OnClusterClickListener<HostBriefInfo>,
        ClusterManager.OnClusterInfoWindowClickListener<HostBriefInfo>,
        ClusterManager.OnClusterItemClickListener<HostBriefInfo>,
        ClusterManager.OnClusterItemInfoWindowClickListener<HostBriefInfo>,
        GoogleMap.OnCameraChangeListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private MapSearchTask searchTask;
    private DialogHandler dialogHandler;
    private ConcurrentHashMap<Integer, HostBriefInfo> mHosts = new ConcurrentHashMap<Integer, HostBriefInfo>();
    private ClusterManager<HostBriefInfo> mClusterManager;
    private Cluster<HostBriefInfo> mLastClickedCluster;

    /**
     * Add the title and snippet to the marker so that infoWindow can be rendered.
     */
    private class HostRenderer extends DefaultClusterRenderer<HostBriefInfo> {

        public HostRenderer() {
            super(getApplicationContext(), mMap, mClusterManager);
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<HostBriefInfo> cluster, MarkerOptions markerOptions) {
            super.onBeforeClusterRendered(cluster, markerOptions);
        }

        @Override
        protected void onBeforeClusterItemRendered(HostBriefInfo host, MarkerOptions markerOptions) {
            String street = host.getStreet();
            String snippet = host.getCity() + ", " + host.getProvince().toUpperCase();
            if (street != null && street.length() > 0) {
                snippet = street + "<br/>" + snippet;
            }
            markerOptions.title(host.getFullname()).snippet(snippet);
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            return cluster.getSize() > 1;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        mMap.setOnCameraChangeListener(this);
        mClusterManager = new ClusterManager<HostBriefInfo>(this, mMap);
        mClusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<HostBriefInfo>(new WSNonHierarchicalDistanceBasedAlgorithm<HostBriefInfo>()));
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
        mClusterManager.setRenderer(new HostRenderer());
        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        mClusterManager.getClusterMarkerCollection().setOnInfoWindowAdapter(new ClusterInfoWindowAdapter(getLayoutInflater()));
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new SingleHostInfoWindowAdapter(getLayoutInflater()));
//        mMap.setInfoWindowAdapter(new Maps2Activity.SingleHostInfoWindowAdapter(getLayoutInflater()));
    }

    class ClusterInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private View mPopup=null;
        private LayoutInflater mInflater=null;

        ClusterInfoWindowAdapter(LayoutInflater inflater) {
            this.mInflater = inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {

            String hostList = "";
            ArrayList<HostBriefInfo> hosts = new ArrayList<HostBriefInfo>();
            if (mPopup == null) {
                // TODO: Should not be passing null as second param
                mPopup = mInflater.inflate(R.layout.info_window, null);
            }
            TextView tv = (TextView)mPopup.findViewById(R.id.title);

            if (mLastClickedCluster != null) {
                hosts = (ArrayList<HostBriefInfo>) mLastClickedCluster.getItems();
                if (mLastClickedCluster != null) {
                    for (HostBriefInfo host : hosts) {
                        hostList += host.getFullname() + "<br/>";
                    }

                    hostList += getString(R.string.click_to_view_all);
                }
                tv.setText(getString(R.string.hosts_at_location, hosts.size(), hosts.get(0).getLocation()));
                tv=(TextView)mPopup.findViewById(R.id.snippet);
                tv.setText(Html.fromHtml(hostList));
            }

            return(mPopup);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(android.os.Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onCameraChange(CameraPosition position) {

        LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;

        sendMessage(getResources().getString(R.string.loading_hosts), false);

        Search search = new RestMapSearch(curScreen.northeast, curScreen.southwest);
        doMapSearch(search);
    }

    public void doMapSearch(Search search) {
        searchTask = new MapSearchTask();
        searchTask.execute(search);
    }

    @Override
    /**
     * - Capture the clicked cluster so we can use it in custom infoWindow
     * - Check overall bounds of items in cluster
     * - If the bounds are empty (all hosts at same place) then let it pop the info window
     * - Otherwise, move the camera to show the bounds of the map
     */
    public boolean onClusterClick(Cluster<HostBriefInfo> cluster) {
        mLastClickedCluster = cluster; // remember for use later in the Adapter

        // Find out the bounds of the hosts currently in cluster
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(HostBriefInfo host : cluster.getItems()){
            builder.include(host.getLatLng());
        }
        LatLngBounds bounds = builder.build();

        // If the hosts are not all at the same location, then change bounds of map.
        if (!bounds.southwest.equals(bounds.northeast)) {
            // Offset from edge of map in pixels when exploding cluster
            int padding = getResources().getInteger(R.integer.cluster_explode_padding);
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
            return true; // No more processing needed for this click.
        }
        // If there was nothing in the bounds, normal handling with info window.
        return false;
    }

    @Override
    /**
     * Start the Search tab with the members we have at this exact location.
     */
    public void onClusterInfoWindowClick(Cluster<HostBriefInfo> hostBriefInfoCluster) {
        Intent intent = new Intent(this, ListSearchTabActivity.class);
        intent.putParcelableArrayListExtra("search_results", (ArrayList<HostBriefInfo>)hostBriefInfoCluster.getItems());
        startActivity(intent);
    }

    @Override
    public boolean onClusterItemClick(HostBriefInfo hostBriefInfo) {
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(HostBriefInfo host) {
        Intent i = new Intent(this, HostInformationActivity.class);
        i.putExtra("host", Host.createFromBriefInfo(host));
        i.putExtra("id", host.getId());
        startActivityForResult(i, 0);
    }

    private class MapSearchTask extends AsyncTask<Search, Void, Object> {

        @Override
        protected Object doInBackground(Search... params) {
            Search search = params[0];
            Object retObj = null;

            try {
                retObj = search.doSearch();
            }
            catch (Exception e) {
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                retObj = e;
            }

            return retObj;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Exception) {
                // TODO: Test offline to see if this works
                if (result instanceof HttpException) {
                    Log.e(WSAndroidApplication.TAG, ((HttpException)(result)).getMessage());
                    sendMessage(getResources().getString(R.string.error_loading_hosts), true);
                }

                // TODO: Improve error reporting with more specifics
                sendMessage(getResources().getString(R.string.error_retrieving_host_information), true);
                return;
            }
            ArrayList<HostBriefInfo> hosts = (ArrayList<HostBriefInfo>) result;
            if (hosts.isEmpty()) {
                sendMessage((String)getResources().getText(R.string.no_results), false);
            }

            for (HostBriefInfo host: hosts) {
                HostBriefInfo v = mHosts.putIfAbsent(host.getId(), host);
                // Only add to the cluster if it wasn't in mHosts before.
                if (v == null) {
                    mClusterManager.addItem(host);
                }
            }
            mClusterManager.cluster();
        }

    }


    /**
     * InfoWindowAdapter to present info about a single host marker.
     * Implemented here so we can have multiple lines, which the maps-provided one prevents.
     */
    class SingleHostInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private View mPopup = null;
        private LayoutInflater mInflater = null;

        SingleHostInfoWindowAdapter(LayoutInflater inflater) {
            this.mInflater = inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return (null);
        }

        @SuppressLint("InflateParams")
        @Override
        public View getInfoContents(Marker marker) {
            if (mPopup == null){
                mPopup = mInflater.inflate(R.layout.single_host_infowindow, null);
            }
            TextView titleView = (TextView) mPopup.findViewById(R.id.title);
            titleView.setText(marker.getTitle());
            TextView snippetView = (TextView) mPopup.findViewById(R.id.snippet);
            snippetView.setText(Html.fromHtml(marker.getSnippet()));
            return (mPopup);
        }
    }

    private Toast lastToast = null;

    private void sendMessage(final String message, final boolean error) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        if (lastToast != null) {
            lastToast.cancel();
        }
        toast.show();
        lastToast = toast;
    }

}
