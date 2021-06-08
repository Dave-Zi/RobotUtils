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
        // String dataForBoardsMap = "{\"EV3\": {\"1\": [\"B\"],\"4\": [\"3\", \"A\"]},\"GrovePi\": [\"D3\"]}";
        // robotSensorsData.addToBoardsMap(dataForBoardsMap);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void deepCopyTest() {
        robotSensorsData = new RobotSensorsData();
        // check both robotSensorData has the same portsMap after deepCopy
        Map<String, Map<String, Map<String, Double>>> expected = robotSensorsData.getPortsMap();
        RobotSensorsData newRobot = robotSensorsData.deepCopy();
        Map<String, Map<String, Map<String, Double>>> actual
                = newRobot.getPortsMap();
        assertEquals(expected, actual);
    }


    @Test
    public void updateBoardMapValuesTest() {
        robotSensorsData = new RobotSensorsData();

        String dataWithNicknames = "{\n" +
                "                \"EV3\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"EV3_1\",\n" +
                "                        \"Port\": \"rfcomm0\",\n" +
                "                        \"2\": {\"Name\": \"UV3\"}" +
                "                    }," +
                "                   {\n" +
                "                        \"Port\": \"rfcomm1\",\n" +
                "                        \"3\": {\"Name\": \"UV_22\"}\n" +
                "                    }]\n" +
                "                ,\n" +
                "                \"GrovePi\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"myGrovePi\",\n" +
                "                        \"A2\": \"\",\n" +
                "                        \"D2\": {\"Name\": \"MyLed\", \"Device\": \"Led\"},\n" +
                "                        \"D4\": {\"Name\": \"UV\", \"Device\": \"Ultrasonic\"},\n" +
                "                        \"D8\": \"Led\"\n" +
                "                    }," +
                "                      {\n" +
                "                        \"A2\": {\"Name\": \"UV234\", \"Device\": \"Ultrasonic\"},\n" +
                "                        \"D2\": {\"Name\": \"MyLed\", \"Device\": \"Led\"},\n" +
                "                        \"D8\": {\"Name\": \"Led2\", \"Device\": \"Led\"}\n" +
                "                    }]\n" +
                "            }";

        robotSensorsData.buildNicknameMaps(dataWithNicknames);
        String dataToAdd = "{\"EV3\": {\"EV3_1\": [\"3\",\"5\" ],\"2\" : [\"3\", \"4\"]},\"GrovePi\": {\"2\": [\"D2\"]}}";

        robotSensorsData.addToBoardsMap(dataToAdd);
        // values are null before we update
        assertNull(robotSensorsData.getPortsAndValues("EV3", "_1").get("_5"));

        assertNull(robotSensorsData.getPortsAndValues("EV3", "_2").get("_3"));

        assertNull(robotSensorsData.getPortsAndValues("GrovePi", "_2").get("D2"));

        String dataForUpdate = "{\"EV3\": {\"_1\": {\"_5\" : 10}, \"_2\" : {\"_3\" : 122}}, \"GrovePi\": {\"_2\": {\"D2\" : 57}}}";
        robotSensorsData.updateBoardMapValues(dataForUpdate);
        // has value after update
        assertEquals((Double) 10.0,robotSensorsData.getPortsAndValues("EV3", "_1").get("_5"));

        assertEquals((Double) 122.0,robotSensorsData.getPortsAndValues("EV3", "_2").get("_3"));

        assertEquals((Double) 57.0,robotSensorsData.getPortsAndValues("GrovePi", "_2").get("D2"));

        assertNull(robotSensorsData.getPortsAndValues("EV3", "_1").get("_3"));

        assertNull(robotSensorsData.getPortsAndValues("EV3", "_2").get("_4"));
    }


    @Test
    public void buildNicknameMaps() {
        robotSensorsData = new RobotSensorsData();

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
        expected.put("1", "EV3_1");
        boardNicknamesMap_expected.put("EV3", expected);
        expected = new HashMap<>();
        expected.put("1", "myGrovePi");
        boardNicknamesMap_expected.put("GrovePi", expected);

        assertEquals(boardNicknamesMap_expected, boardNicknamesMap_actual);

        //    board name -> board index -> board ports -> board nicknames
        Map<String, Map<String, Map<String, String>>> portNicknamesMap_actual = robotSensorsData.portNicknamesMap;
        Map<String, Map<String, Map<String, String>>> portNicknamesMap_expected = new HashMap<>();

        expected = new HashMap<>();
        expected.put("D2", "MyLed");
        expected.put("D4", "UV");
        Map<String, Map<String, String>> boardPorts = new HashMap<>();
        boardPorts.put("1", expected);
        portNicknamesMap_expected.put("GrovePi", boardPorts);

        expected = new HashMap<>();
        expected.put("2", "UV3");
        boardPorts = new HashMap<>();
        boardPorts.put("1", expected);
        portNicknamesMap_expected.put("EV3", boardPorts);

        assertEquals(portNicknamesMap_expected, portNicknamesMap_actual);
    }


    @Test
    public void addToBoardsMapNicknamesTest() {
        robotSensorsData = new RobotSensorsData();

        String dataWithNicknames = "{\n" +
                "                \"EV3\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"EV3_1\",\n" +
                "                        \"Port\": \"rfcomm0\",\n" +
                "                        \"2\": {\"Name\": \"UV3\"}\n" +
                "                    }," +
                "                   {\n" +
                "                        \"Port\": \"rfcomm1\",\n" +
                "                        \"3\": {\"Name\": \"UV_22\"}\n" +
                "                    }, " +
                "                        {\"Port\": \"rfcomm2\"\n" +
                "                       }]\n" +
                "                ,\n" +
                "                \"GrovePi\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"myGrovePi\",\n" +
                "                        \"A2\": \"\",\n" +
                "                        \"D2\": {\"Name\": \"MyLed\", \"Device\": \"Led\"},\n" +
                "                        \"D4\": {\"Name\": \"UV\", \"Device\": \"Ultrasonic\"},\n" +
                "                        \"D8\": \"Led\"\n" +
                "                    }," +
                "                      {\n" +
                "                        \"A2\": {\"Name\": \"UV234\", \"Device\": \"Ultrasonic\"},\n" +
                "                        \"D2\": {\"Name\": \"MyLed\", \"Device\": \"Led\"},\n" +
                "                        \"D8\": \"Led\"\n" +
                "                    }]\n" +
                "            }";

        robotSensorsData.buildNicknameMaps(dataWithNicknames);

        String dataToAdd = "{\"EV3\": {\"EV3_1\": [\"A\"],\"2\" : [\"B\"], \"3\": [\"4\"]},\"GrovePi\": {\"myGrovePi\": [\"D1\"], \"2\": [\"D1\"]}}";

        robotSensorsData.addToBoardsMap(dataToAdd);

        // exist after we added it
        Set<String> ports = robotSensorsData.getPorts("EV3", "EV3_1");
        assertTrue(ports.contains("A"));

        ports = robotSensorsData.getPorts("EV3", "_1");
        assertTrue(ports.contains("A"));

        ports = robotSensorsData.getPorts("EV3", "_2");
        assertTrue(ports.contains("B"));

        ports = robotSensorsData.getPorts("EV3", "_3");
        assertTrue(ports.contains("_4"));

        ports = robotSensorsData.getPorts("GrovePi", "_1");
        assertTrue(ports.contains("D1"));

        ports = robotSensorsData.getPorts("GrovePi", "myGrovePi");
        assertTrue(ports.contains("D1"));

        ports = robotSensorsData.getPorts("GrovePi", "_2");
        assertTrue(ports.contains("D1"));
    }

    @Test
    public void removeFromBoardsMapWithNicknamesTest() {
        robotSensorsData = new RobotSensorsData();

        String dataWithNicknames = "{\n" +
                "                \"EV3\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"EV3_1\",\n" +
                "                        \"Port\": \"rfcomm0\",\n" +
                "                        \"2\": {\"Name\": \"UV3\"}" +
                "                    }," +
                "                   {\n" +
                "                        \"Port\": \"rfcomm1\",\n" +
                "                        \"3\": {\"Name\": \"UV_22\"}\n" +
                "                    }]\n" +
                "                ,\n" +
                "                \"GrovePi\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"myGrovePi\",\n" +
                "                        \"A2\": \"\",\n" +
                "                        \"D2\": {\"Name\": \"MyLed\", \"Device\": \"Led\"},\n" +
                "                        \"D4\": {\"Name\": \"UV\", \"Device\": \"Ultrasonic\"},\n" +
                "                        \"D8\": \"Led\"\n" +
                "                    }," +
                "                      {\n" +
                "                        \"A2\": {\"Name\": \"UV234\", \"Device\": \"Ultrasonic\"},\n" +
                "                        \"D2\": {\"Name\": \"MyLed\", \"Device\": \"Led\"},\n" +
                "                        \"D8\": {\"Name\": \"Led2\", \"Device\": \"Led\"}\n" +
                "                    }]\n" +
                "            }";

        robotSensorsData.buildNicknameMaps(dataWithNicknames);
        String dataToAdd = "{\"EV3\": {\"EV3_1\": [\"UV3\",\"5\" ],\"_2\" : [\"UV_22\", \"4\"]},\"GrovePi\": {\"myGrovePi\": [\"MyLed\", \"Led2\"], \"_2\": [\"UV234\"]}}";

        robotSensorsData.addToBoardsMap(dataToAdd);

        String dataToRemove = "{\"EV3\": {\"EV3_1\": [\"UV3\"],\"_2\" : [\"UV_22\"]},\"GrovePi\": {\"myGrovePi\": [\"MyLed\"], \"_2\": [\"UV234\"]}}";


        robotSensorsData.removeFromBoardsMap(dataToRemove);

        // exist after we added it
        Set<String> ports = robotSensorsData.getPorts("EV3", "EV3_1");
        assertFalse(ports.contains("UV3"));

        ports = robotSensorsData.getPorts("EV3", "EV3_1");
        assertFalse(ports.contains("_2"));

        ports = robotSensorsData.getPorts("EV3", "_1");
        assertFalse(ports.contains("UV3"));

        ports = robotSensorsData.getPorts("EV3", "_1");
        assertFalse(ports.contains("_2"));

        ports = robotSensorsData.getPorts("EV3", "_2");
        assertFalse(ports.contains("UV_22"));

        ports = robotSensorsData.getPorts("EV3", "_2");
        assertFalse(ports.contains("_3"));

        ports = robotSensorsData.getPorts("GrovePi", "_1");
        assertFalse(ports.contains("D2"));

        ports = robotSensorsData.getPorts("GrovePi", "_1");
        assertFalse(ports.contains("MyLed"));

        ports = robotSensorsData.getPorts("GrovePi", "myGrovePi");
        assertFalse(ports.contains("D2"));

        ports = robotSensorsData.getPorts("GrovePi", "myGrovePi");
        assertFalse(ports.contains("MyLed"));

        ports = robotSensorsData.getPorts("GrovePi", "myGrovePi");
        assertFalse(ports.contains("D1"));

        ports = robotSensorsData.getPorts("GrovePi", "_2");
        assertTrue(ports.isEmpty());

        //check rest of the sensors were not removed
        ports = robotSensorsData.getPorts("EV3", "EV3_1");
        assertTrue(ports.contains("_5"));

        ports = robotSensorsData.getPorts("EV3", "_1");
        assertTrue(ports.contains("_5"));

        ports = robotSensorsData.getPorts("EV3", "_2");
        assertTrue(ports.contains("_4"));

        ports = robotSensorsData.getPorts("GrovePi", "myGrovePi");
        assertTrue(ports.contains("Led2"));
    }

    @Test
    public void getBoardNamesTest() {
        robotSensorsData = new RobotSensorsData();
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
                "                        \"A2\": \"\",\n" +
                "                        \"D2\": {\"Name\": \"MyLed\", \"Device\": \"Led\"},\n" +
                "                        \"D4\": {\"Name\": \"UV\", \"Device\": \"Ultrasonic\"},\n" +
                "                        \"D8\": \"Led\"\n" +
                "                    }]\n" +
                "            }";


        robotSensorsData.buildNicknameMaps(dataWithNicknames);
        String dataToAdd = "{\"EV3\": {\"EV3_1\": [\"A\"]},\"GrovePi\": {\"myGrovePi\": [\"D1\"]}}";
        robotSensorsData.addToBoardsMap(dataToAdd);
        assertArrayEquals(new String[]{"EV3", "GrovePi"}, robotSensorsData.getBoardNames().toArray());
    }

    @Test
    public void getBoardByNameAndIndexTest() {
        robotSensorsData = new RobotSensorsData();
        robotSensorsData.portsMap = Map.of(
                "EV3", Map.of(
                        "_1", Map.of(
                                "A", 50.0,
                                "NickA", 50.0,
                                "B", 60.0
                        ),
                        "Nick1", Map.of(
                                "A", 50.0,
                                "NickA", 50.0,
                                "B", 60.0
                        )
                ),
                "GrovePi", Map.of(
                        "_2", Map.of(
                                "D2", 50.0,
                                "D4", 60.0
                        )
                )
        );

        Map<String, Double> expected = new HashMap<>();
        expected.put("A", 50.0);
        expected.put("NickA", 50.0);
        expected.put("B", 60.0);

        assertEquals(expected, robotSensorsData.getBoardByNameAndIndex("EV3", "_1"));
        assertEquals(expected, robotSensorsData.getBoardByNameAndIndex("EV3", "Nick1"));

        expected = new HashMap<>();
        expected.put("D2", 50.0);
        expected.put("D4", 60.0);
        assertEquals(expected, robotSensorsData.getBoardByNameAndIndex("GrovePi", "_2"));
    }

    @Test
    public void getBoardIndexesTest() {
        robotSensorsData = new RobotSensorsData();

        robotSensorsData.portsMap = Map.of(
                "EV3", Map.of(
                        "_1", Map.of(
                                "A", 50.0,
                                "NickA", 50.0,
                                "B", 60.0
                        ),
                        "Nick1", Map.of(
                                "A", 50.0,
                                "NickA", 50.0,
                                "B", 60.0
                        )
                ),
                "GrovePi", Map.of(
                        "_2", Map.of(
                                "D2", 50.0,
                                "D4", 60.0
                        )
                )
        );


        Set<String> actual = robotSensorsData.getBoardIndexes("EV3");
        Set<String> expected = new HashSet<>();
        expected.add("Nick1");
        expected.add("_1");
        assertEquals(expected, actual);

        actual = robotSensorsData.getBoardIndexes("GrovePi");
        expected = new HashSet<>();
        expected.add("_2");
        assertEquals(expected, actual);
    }

    @Test
    public void getPortsAndValuesTest() {
        robotSensorsData = new RobotSensorsData();

        robotSensorsData.portsMap = Map.of(
                "EV3", Map.of(
                        "_1", Map.of(
                                "A", 50.0,
                                "NickA", 50.0,
                                "B", 60.0
                        ),
                        "Nick1", Map.of(
                                "A", 50.0,
                                "NickA", 50.0,
                                "B", 60.0
                        )
                ),
                "GrovePi", Map.of(
                        "_2", Map.of(
                                "D2", 12.6,
                                "D4", 177.0
                        )
                )
        );

        Map<String, Double> actual = robotSensorsData.getPortsAndValues("EV3", "_1");
        Map<String, Double> expected = new HashMap<>();
        expected.put("A", 50.0);
        expected.put("NickA", 50.0);
        expected.put("B", 60.0);
        assertEquals(expected, actual);


        actual = robotSensorsData.getPortsAndValues("EV3", "Nick1");
        expected = new HashMap<>();
        expected.put("A", 50.0);
        expected.put("NickA", 50.0);
        expected.put("B", 60.0);
        assertEquals(expected, actual);

        actual = robotSensorsData.getPortsAndValues("GrovePi", "_2");
        expected = new HashMap<>();
        expected.put("D2", 12.6);
        expected.put("D4", 177.0);
        assertEquals(expected, actual);
    }

    @Test
    public void getPortsTest() {
        robotSensorsData = new RobotSensorsData();
        robotSensorsData.portsMap = Map.of(
                "EV3", Map.of(
                        "_1", Map.of(
                                "A", 50.0,
                                "NickA", 50.0,
                                "B", 60.0
                        ),
                        "Nick1", Map.of(
                                "A", 50.0,
                                "NickA", 50.0,
                                "B", 60.0
                        )
                ),
                "GrovePi", Map.of(
                        "_2", Map.of(
                                "D2", 12.6,
                                "D4", 177.0
                        )
                )
        );
        Set<String> expected = new HashSet<>();
        expected.add("A");
        expected.add("NickA");
        expected.add("B");
        assertEquals(expected, robotSensorsData.getPorts("EV3", "_1"));
        assertEquals(expected, robotSensorsData.getPorts("EV3", "Nick1"));

        expected = new HashSet<>();
        expected.add("D2");
        expected.add("D4");
        assertEquals(expected, robotSensorsData.getPorts("GrovePi", "_2"));
    }

    @Test
    public void replaceNicksInJsonTest() {

        String buildDataWithNicknames = "{\n" +
                "                \"EV3\":\n" +
                "                    [{\n" +
                "                        \"Name\": \"Nick1\",\n" +
                "                        \"Port\": \"rfcomm0\",\n" +
                "                        \"A\": {\"Name\": \"NickA\"},\n" +
                "                        \"B\": {\"Name\": \"NickB\"}\n" +
                "                    }]\n" +
                "            }";
        robotSensorsData.buildNicknameMaps(buildDataWithNicknames);
        String[] testCases = testCasesForReplaceNicksInJsonTest;
        for (int i = 0; i < testCases.length; i += 2) {
            assertEquals(String.format("Input :%s\n",testCases[i]), testCases[i+1], robotSensorsData.replaceNicksInJson(testCases[i]));
        }
    }

    String[] testCasesForReplaceNicksInJsonTest = {
            "{\"EV3\":[\"NickA\"]}", "{\"EV3\":{\"1\":[\"A\"]}}",
            "{\"EV3\":[\"NickA\"]}", "{\"EV3\":{\"1\":[\"A\"]}}",
            "{\"EV3\":[\"NickB\"]}", "{\"EV3\":{\"1\":[\"B\"]}}",
            "{\"EV3\":[\"NickA\",\"NickB\"]}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":[\"NickB\",\"NickA\"]}", "{\"EV3\":{\"1\":[\"B\",\"A\"]}}",
            "{\"EV3\":{\"1\":[\"A\"]}}", "{\"EV3\":{\"1\":[\"A\"]}}",
            "{\"EV3\":{\"1\":[\"B\"]}}", "{\"EV3\":{\"1\":[\"B\"]}}",
            "{\"EV3\":{\"1\":[\"A\",\"B\"]}}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":{\"1\":[\"NickA\"]}}", "{\"EV3\":{\"1\":[\"A\"]}}",
            "{\"EV3\":{\"1\":[\"NickB\"]}}", "{\"EV3\":{\"1\":[\"B\"]}}",
            "{\"EV3\":{\"1\":[\"NickA\",\"B\"]}}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":{\"1\":[\"A\",\"NickB\"]}}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":{\"1\":[\"NickA\",\"NickB\"]}}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":{\"Nick1\":[\"A\"]}}", "{\"EV3\":{\"1\":[\"A\"]}}",
            "{\"EV3\":{\"Nick1\":[\"B\"]}}", "{\"EV3\":{\"1\":[\"B\"]}}",
            "{\"EV3\":{\"Nick1\":[\"A\",\"B\"]}}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":{\"Nick1\":[\"NickA\"]}}", "{\"EV3\":{\"1\":[\"A\"]}}",
            "{\"EV3\":{\"Nick1\":[\"NickB\"]}}", "{\"EV3\":{\"1\":[\"B\"]}}",
            "{\"EV3\":{\"Nick1\":[\"NickA\",\"B\"]}}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":{\"Nick1\":[\"A\",\"NickB\"]}}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":{\"Nick1\":[\"NickA\",\"NickB\"]}}", "{\"EV3\":{\"1\":[\"A\",\"B\"]}}",
            "{\"EV3\":{\"Nick1\":[\"A\",\"HAHAHA\"]}}", "{\"EV3\":{\"1\":[\"A\",\"HAHAHA\"]}}",
            "{\"EV3\":{\"Nick1\":[\"HAHAHA\",\"NickA\"]}}", "{\"EV3\":{\"1\":[\"HAHAHA\",\"A\"]}}",
            "{\"EV3\":{\"Nick1\":[\"NickA\",\"HAHAHA\"]}}", "{\"EV3\":{\"1\":[\"A\",\"HAHAHA\"]}}",
            "{\"EV3\":{\"Nick1\":{\"NickA\":50,\"HAHAHA\":60}}}", "{\"EV3\":{\"1\":{\"A\":50.0,\"HAHAHA\":60.0}}}",
            "{\"EV3\":{\"1\":{\"NickA\":50.1,\"HAHAHA\":60.0,\"B\":70.0}}}", "{\"EV3\":{\"1\":{\"A\":50.1,\"B\":70.0,\"HAHAHA\":60.0}}}",
            "{\"EV3\":{\"1\":{\"NickA\":50.2,\"HAHAHA\":60.0,\"NickB\":70.0}}}", "{\"EV3\":{\"1\":{\"A\":50.2,\"B\":70.0,\"HAHAHA\":60.0}}}",
            "{\"EV3\":{\"1\":{\"A\":50.0,\"NickB\":70.0,\"HAHAHA\":60.0}}}", "{\"EV3\":{\"1\":{\"A\":50.0,\"B\":70.0,\"HAHAHA\":60.0}}}",
            "{\"EV3\":{\"1\":{}}}", "{\"EV3\":{\"1\":{}}}",
            "{\"EV3\":{\"Nick1\":{}}}", "{\"EV3\":{\"1\":{}}}",
            "{\"EV3\":{\"1\":[]}}", "{\"EV3\":{\"1\":[]}}",
            "{\"EV3\":{\"Nick1\":[]}}", "{\"EV3\":{\"1\":[]}}",
            "{\"EV3\":{}}", "{\"EV3\":{}}",
            "{\"EV3\":{\"B\":70.0,\"C\":50.0,\"speed\":15.0}}","{\"EV3\":{\"1\":{\"B\":70.0,\"C\":50.0,\"speed\":15.0}}}",
            "{\"EV3\":{\"NickB\":70.0,\"C\":50.0,\"speed\":15.0}}","{\"EV3\":{\"1\":{\"B\":70.0,\"C\":50.0,\"speed\":15.0}}}",
            "{\"EV3\":{\"NickB\":70.0,\"NickA\":50.0,\"speed\":15.0}}","{\"EV3\":{\"1\":{\"A\":50.0,\"B\":70.0,\"speed\":15.0}}}",
    };

}