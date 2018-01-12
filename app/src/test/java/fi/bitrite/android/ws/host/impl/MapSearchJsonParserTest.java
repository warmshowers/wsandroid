package fi.bitrite.android.ws.host.impl;

/**
 * User: johannes
 * Date: 08.01.2013
 */
public class MapSearchJsonParserTest {

//    @Rule
//    public ResourceFile incomplete = new ResourceFile("map_search_incomplete.json");
//
//    @Rule
//    public ResourceFile sixHosts = new ResourceFile("map_search_six_hosts.json");
//
//    @Rule
//    public ResourceFile singleHost = new ResourceFile("map_search_single_host.json");
//
//    @Rule
//    public ResourceFile unknownHost = new ResourceFile("map_search_unknown.json");
//
//    @Rule
//    public ResourceFile hostWithStreet = new ResourceFile("map_search_host_with_street.json");
//
//    @Rule
//    public ExpectedException exception = ExpectedException.none();
//
//    @Test
//    public void testIncomplete() throws Exception {
//        exception.expect(IncompleteResultsException.class);
//        MapSearchJsonParser parser = new MapSearchJsonParser(new JSONObject(incomplete.getContent()));
//        List<Host> hosts = parser.getHosts();
//    }
//
//    @Test
//    public void hostsCutoff() throws Exception {
//        // exception.expect(TooManyHostsException.class);
//        // MapSearchJsonParser parser = new MapSearchJsonParser(new JSONObject(sixHosts.getContent()));
//        // List<HostBriefInfo> hosts = parser.getHosts();
//
//        // TODO: This exception was removed in commit a46f8b20. What is the
//        //       exact testcase here?
//    }
//
//    @Test
//    public void testSingleHost() throws Exception {
//        MapSearchJsonParser parser = new MapSearchJsonParser(new JSONObject(singleHost.getContent()));
//        List<Host> hosts = parser.getHosts();
//
//        assertEquals(1, hosts.size());
//        Host host = hosts.get(0);
//        assertEquals(18496, host.getId());
//        assertEquals("Johannes Staffans", host.getFullname());
//        assertEquals("Helsinki, ES 00650, FI", host.getLocation());
//        assertEquals("60.184443", host.getLatitude());
//        assertEquals("25.006599", host.getLongitude());
//    }
//
//    @Test
//    public void testUnknownHost() throws Exception {
//        MapSearchJsonParser parser = new MapSearchJsonParser(new JSONObject(unknownHost.getContent()));
//        List<Host> hosts = parser.getHosts();
//        Host host = hosts.get(0);
//        assertEquals("", host.getFullname());
//    }
//
//
//    @Test
//    public void testStreet() throws Exception {
//        MapSearchJsonParser parser = new MapSearchJsonParser(new JSONObject(hostWithStreet.getContent()));
//        List<Host> hosts = parser.getHosts();
//        Host host = hosts.get(0);
//        assertEquals("Street 1\nHelsinki, ES 00650, FI", host.getLocation());
//    }
}
