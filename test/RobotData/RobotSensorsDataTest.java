package RobotData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;


public class RobotSensorsDataTest {

    private RobotSensorsData robotSensorsData = new RobotSensorsData();
    //  private Map<String, Map<String, Map<String, Double>>> mapToCompare = new HashMap<>();
    private Map<String, Double> grovePiInsideMap = new HashMap<>();
    private Map<String, Map<String, Double>> grovePiMap = new HashMap<>();
    private Map<String, Double> ev3InsideMap1 = new HashMap<>();
    private Map<String, Map<String, Double>> ev3Map = new HashMap<>();
    private Map<String, Double> ev3InsideMap2 = new HashMap<>();


    @Before
    public void setUp() {
        String dataForBoardsMap = "{\"EV3\": {\"1\": [\"B\"],\"4\": [\"3\", \"A\"]},\"GrovePi\": [\"D3\"]}";
        robotSensorsData.addToBoardsMap(dataForBoardsMap);
    }

    @After
    public void tearDown() {
        robotSensorsData.clear();
    }

    @Test
    public void deepCopyTest() {
        // check both robotSensorData has the same portsMap after deepCopy
        Map<String, Map<String, Map<String, Double>>> expected = robotSensorsData.getPortsMap();
        RobotSensorsData newRobot = robotSensorsData.deepCopy();
        Map<String, Map<String, Map<String, Double>>> actual
                = newRobot.getPortsMap();
        assertEquals(expected, actual);
    }


    @Test
    public void updateBoardMapValuesTest() {
        // value is null before we update
        assertNull(robotSensorsData.getPortsAndValues("GrovePi", "_1").get("D3"));
        assertNull(robotSensorsData.getPortsAndValues("EV3", "_1").get("B"));
        String dataForUpdate = "{\"EV3\": {\"_1\": {\"B\" : 10}},\"GrovePi\": {\"_1\": {\"D3\" : 52}}}";
        robotSensorsData.updateBoardMapValues(dataForUpdate);
        // has value after update
        assertEquals((Double) 52.0, robotSensorsData.getPortsAndValues("GrovePi", "_1").get("D3"));
        assertEquals((Double) 10.0, robotSensorsData.getPortsAndValues("EV3", "_1").get("B"));
    }


    @Test
    public void buildNicknameMaps() {
        String dataWithNicknames = "{\n" +
                "                \"EV3\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"EV3_1\",\n" +
                "                        \"Port\": \"rfcomm0\",\n" +
                "                        \"2\": {\"Name\": \"UV3\"}\n" +
                "                    }]\n" +
                "                ,\n" +
                "                \"GrovePi\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"myGrovePi\",\n" +
                "                        \"A0\": {\"Name\": \"\", \"Device\": \"\"},\n" +
                "                        \"A1\": \"\",\n" +
                "                        \"A2\": \"\",\n" +
                "                        \"D2\": {\"Name\": \"MyLed\", \"Device\": \"Led\"},\n" +
                "                        \"D4\": {\"Name\": \"UV\", \"Device\": \"Ultrasonic\"},\n" +
                "                        \"D8\": \"Led\"\n" +
                "                    }]\n" +
                "            }";
        robotSensorsData.buildNicknameMaps(dataWithNicknames);
        //    board name -> board index -> board nickname
        Map<String, Map<String, String>> boardNicknamesMap_actual = robotSensorsData.boardNicknamesMap;
        Map<String, Map<String, String>> boardNicknamesMap_expected = new HashMap<>();

        Map<String, String> expected = new HashMap<>();
        expected.put("_1", "EV3_1");
        boardNicknamesMap_expected.put("EV3", expected);
        expected = new HashMap<>();
        expected.put("_1", "myGrovePi");
        boardNicknamesMap_expected.put("GrovePi", expected);

        assertEquals(boardNicknamesMap_expected, boardNicknamesMap_actual);

        //    board name -> board index -> board ports -> board nicknames
        Map<String, Map<String, Map<String, String>>> portNicknamesMap_actual = robotSensorsData.portNicknamesMap;
        Map<String, Map<String, Map<String, String>>> portNicknamesMap_expected = new HashMap<>();

        expected = new HashMap<>();
        expected.put("D2", "MyLed");
        expected.put("D4", "UV");
        Map<String, Map<String, String>> boardPorts = new HashMap<>();
        boardPorts.put("_1", expected);
        portNicknamesMap_expected.put("GrovePi", boardPorts);

        expected = new HashMap<>();
        expected.put("_2", "UV3");
        boardPorts = new HashMap<>();
        boardPorts.put("_1", expected);
        portNicknamesMap_expected.put("EV3", boardPorts);

        assertEquals(portNicknamesMap_expected, portNicknamesMap_actual);
    }


    @Test
    public void addToBoardsMapTest() {
        // doesn't exist before
        Set<String> ports = robotSensorsData.getPorts("EV3", "_1");
        assertFalse(ports.contains("C"));
        ports = robotSensorsData.getPorts("GrovePi", "_1");
        assertFalse(ports.contains("D1"));
        String dataToAdd = "{\"EV3\": {\"_1\": [\"C\"]},\"GrovePi\": {\"_1\": [\"D1\"]}}";
        robotSensorsData.addToBoardsMap(dataToAdd);
        // exist after we add it
        ports = robotSensorsData.getPorts("EV3", "_1");
        assertTrue(ports.contains("C"));
        ports = robotSensorsData.getPorts("GrovePi", "_1");
        assertTrue(ports.contains("D1"));
    }

    @Test
    public void addToBoardsMapNicknamesTest() {
        String dataWithNicknames = "{\"EV3\": {\"ev3_1\": [\"B\"],\"4\": [\"3\", \"A\"]},\"GrovePi\": { \"myGrovePi\": [\"D3\"]}}";
        robotSensorsData.addToBoardsMap(dataWithNicknames);
        // doesn't exist before
        Set<String> ports = robotSensorsData.getPorts("EV3", "ev3_1");
        assertFalse(ports.contains("D"));
        ports = robotSensorsData.getPorts("GrovePi", "myGrovePi");
        assertFalse(ports.contains("A2"));
        String dataToAdd = "{\"EV3\": {\"ev3_1\": [\"D\"]},\"GrovePi\": {\"myGrovePi\": [\"A2\"]}}";
        robotSensorsData.addToBoardsMap(dataToAdd);
        // exist after we add it
        ports = robotSensorsData.getPorts("EV3", "ev3_1");
        assertTrue(ports.contains("D"));
        ports = robotSensorsData.getPorts("GrovePi", "myGrovePi");
        assertTrue(ports.contains("A2"));
    }

    @Test
    public void removeFromBoardsMapTest() {
        // exist in portsMap before remove
        assertTrue(robotSensorsData.getPortsAndValues("GrovePi", "_1").containsKey("D3"));
        String dataToRemove = "{\"GrovePi\": [\"D3\"]}";
        robotSensorsData.removeFromBoardsMap(dataToRemove);
        // doesn't exist after remove
        assertFalse(robotSensorsData.getPortsAndValues("GrovePi", "_1").containsKey("D3"));
    }

    @Test
    public void getBoardNamesTest() {
        assertArrayEquals(new String[]{"EV3", "GrovePi"}, robotSensorsData.getBoardNames().toArray());
    }

    @Test
    public void getBoardByNameAndIndexTest() {
        Map<String, Double> expected = new HashMap<>();
        expected.put("B", null);
        assertEquals(expected, robotSensorsData.getBoardByNameAndIndex("EV3", "_1"));

        expected = new HashMap<>();
        expected.put("_3", null);
        expected.put("A", null);
        assertEquals(expected, robotSensorsData.getBoardByNameAndIndex("EV3", "_4"));

        expected = new HashMap<>();
        expected.put("D3", null);
        assertEquals(expected, robotSensorsData.getBoardByNameAndIndex("GrovePi", "_1"));
    }

    @Test
    public void getBoardIndexesTest() {
        Set<String> actual = robotSensorsData.getBoardIndexes("EV3");
        Set<String> expected = new HashSet<>();
        expected.add("_1");
        expected.add("_4");
        assertEquals(expected, actual);

        actual = robotSensorsData.getBoardIndexes("GrovePi");
        expected = new HashSet<>();
        expected.add("_1");
        assertEquals(expected, actual);
    }

    @Test
    public void getPortsAndValuesTest() {
        Map<String, Double> actual = robotSensorsData.getPortsAndValues("EV3", "_1");
        Map<String, Double> expected = new HashMap<>();
        expected.put("B", null);
        assertEquals(expected, actual);

        actual = robotSensorsData.getPortsAndValues("EV3", "_4");
        expected = new HashMap<>();
        expected.put("_3", null);
        expected.put("A", null);
        assertEquals(expected, actual);

        actual = robotSensorsData.getPortsAndValues("GrovePi", "_1");
        expected = new HashMap<>();
        expected.put("D3", null);
        assertEquals(expected, actual);
    }

    @Test
    public void getPortsTest() {
        assertArrayEquals(new String[]{"B"}, robotSensorsData.getPorts("EV3", "_1").toArray());
        assertArrayEquals(new String[]{"A", "_3"}, robotSensorsData.getPorts("EV3", "_4").toArray());
        assertArrayEquals(new String[]{"D3"}, robotSensorsData.getPorts("GrovePi", "_1").toArray());
    }

    @Test
    public void clearTest() {
        assertNotNull(robotSensorsData.getPortsMap());
        robotSensorsData.clear();
        assertTrue(robotSensorsData.getPortsMap().isEmpty());
    }

}