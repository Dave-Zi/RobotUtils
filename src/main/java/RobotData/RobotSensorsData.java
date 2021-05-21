package RobotData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

import java.util.*;

@SuppressWarnings({"unused", "WeakerAccess"})

public class RobotSensorsData implements Cloneable {

    //    board name -> board index -> ports -> values
    private Map<String, Map<String, Map<String, Double>>> portsMap = new HashMap<>();
    //    board name -> board index -> board nickname
    private final Map<String, Map<String, String>> boardNicknamesMap = new HashMap<>();
    //    board name -> board index -> board ports -> board nicknames
    private final Map<String, Map<String, Map<String, String>>> portNicknamesMap = new HashMap<>();
    private boolean updated;

    public synchronized RobotSensorsData deepCopy() {
        RobotSensorsData robotSensorsData = new RobotSensorsData();
        robotSensorsData.portsMap = new HashMap<>();
        this.portsMap.forEach((boardName, mappedValue) ->
        {
            Map<String, Map<String, Double>> mapForBoard = new HashMap<>();
            mappedValue.forEach((index, currentPortsMap) ->
            {
                Map<String, Double> mapForIndexes = new HashMap<>();
                currentPortsMap.forEach(mapForIndexes::put);
                mapForBoard.put(index, mapForIndexes);
            });
            robotSensorsData.portsMap.put(boardName, mapForBoard);
        });
        return robotSensorsData;
    }

    public synchronized boolean isUpdated() {
        return updated;
    }

    public void buildNicknameMaps(String json){
        Gson gson = new Gson();
        Map<?, ?> element = gson.fromJson(json, Map.class); // json String to Map
        for (Object boardNameKey : element.keySet()) { // Iterate over board types
            String boardName = (String) boardNameKey;
            Map<String, String> indexNicknames = new HashMap<>();
            Map<String, Map<String, String>> indexToPortsNicknames = new HashMap<>();
            @SuppressWarnings("unchecked")
            ArrayList<Map<String, ?>> boardsDataList =
                    (ArrayList<Map<String, ?>>) element.get(boardNameKey);

            for (int i = 0; i< boardsDataList.size(); i++) {
                Map<String, ?> portDataMap = boardsDataList.get(i);
                if (portDataMap.containsKey("Name") && !((String) portDataMap.get("Name")).isBlank()){
                    indexNicknames.put("_" + (i+1), (String) portDataMap.get("Name"));
                }
                Map<String, String> portsNicknames = new HashMap<>();
                for (Map.Entry<String, ?> ports: portDataMap.entrySet()){
                    if (ports.getValue() instanceof LinkedTreeMap){ // Check if port value is actually a map with nickname
                        @SuppressWarnings("unchecked")
                        Map<String, String> valueMap = (Map<String, String>) ports.getValue();
                        if (valueMap.containsKey("Name") && !valueMap.get("Name").isBlank()){
                            portsNicknames.put(fixName(ports.getKey()), valueMap.get("Name"));
                        }
                    }
                }
                indexToPortsNicknames.put("_" + (i+1), portsNicknames);
            }
            boardNicknamesMap.put(boardName, indexNicknames);
            portNicknamesMap.put(boardName, indexToPortsNicknames);
        }
    }

    public String toJson() {
        updated = false;
        return new GsonBuilder().create().toJson(portsMap);
    }

    public void updateBoardMapValues(String json) {
        Gson gson = new Gson();
        Map<?, ?> element = gson.fromJson(json, Map.class); // json String to Map

        for (Object boardNameKey : element.keySet()) { // Iterate over board types
            String boardName = (String) boardNameKey;
            if (portsMap.containsKey(boardName)) { // We want only boards that exist on our map
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Double>> indexesToPorts = (Map<String, Map<String, Double>>) element.get(boardNameKey);
                for (Map.Entry<String, Map<String, Double>> boardIndex : indexesToPorts.entrySet()) { // Iterate over board indexes
                    if (portsMap.get(boardName).containsKey(boardIndex.getKey())) {
                        for (Map.Entry<String, Double> portAndValue : boardIndex.getValue().entrySet()) {
                            setPortValue(boardName, boardIndex.getKey(), portAndValue.getKey(), portAndValue.getValue());
                            if (portNicknamesMap.containsKey(boardName)
                                    && portNicknamesMap.get(boardName).containsKey(boardIndex.getKey())
                                    && portNicknamesMap.get(boardName).get(boardIndex.getKey()).containsKey(portAndValue.getKey())){
                                String nickname = portNicknamesMap.get(boardName).get(boardIndex.getKey()).get(portAndValue.getKey());
                                setPortValue(boardName, boardIndex.getKey(), nickname, portAndValue.getValue());
                            }
                            updated = true;
                        }
                    }
                }
            }
        }
    }

    // Add new sensors from json to mapping
    public synchronized void addToBoardsMap(String json) {
        Map<String, Map<String, Map<String, Double>>> boards = jsonToBoardsMap(json); // Build Map of Robot Ports in json

        for (Map.Entry<String, Map<String, Map<String, Double>>> board : boards.entrySet()) { // Iterate over board types
            if (portsMap.containsKey(board.getKey())) { // If board type already exist in portsMap
                for (Map.Entry<String, Map<String, Double>> entryInBoard : board.getValue().entrySet()) { // Iterate over board map
                    Map<String, Map<String, Double>> boardsMap = portsMap.get(board.getKey());
                    if (boardsMap.containsKey(entryInBoard.getKey())) { // If existing boards map already contain this board
                        boardsMap.get(entryInBoard.getKey()).putAll(entryInBoard.getValue()); // Add boards value to pre existing port list
                    } else {
                        boardsMap.put(entryInBoard.getKey(), entryInBoard.getValue()); // Put new board into map
                    }
                }
            } else { // If board type doesn't exist in portMap.
                portsMap.put(board.getKey(), board.getValue()); // Add board type with all its data to map
            }
        }
        addNicknamesToPortsMap();
    }

    // Remove from mapping any sensors that exist on given json
    public synchronized void removeFromBoardsMap(String json) {
        Map<String, Map<String, Map<String, Double>>> data = jsonToBoardsMap(json);

        for (Map.Entry<String, Map<String, Map<String, Double>>> entry : data.entrySet()) { // Iterate over boards
            if (portsMap.containsKey(entry.getKey())) { // If our board map contains this board
                for (Map.Entry<String, Map<String, Double>> entryInBoard : entry.getValue().entrySet()) { // Iterate over board indexes
                    Map<String, Map<String, Double>> boardsMap = portsMap.get(entry.getKey());
                    if (boardsMap.containsKey(entryInBoard.getKey())) { // If our board map contains board with this index
                        entryInBoard.getValue().forEach((port, value) -> {
                            boardsMap.get(entryInBoard.getKey()).remove(port);
                            if (portNicknamesMap.containsKey(entry.getKey())
                                    && portNicknamesMap.get(entry.getKey()).containsKey(entryInBoard.getKey())
                                    && portNicknamesMap.get(entry.getKey()).get(entryInBoard.getKey()).containsKey(port)){
                                boardsMap.get(entryInBoard.getKey()).remove(
                                        portNicknamesMap.get(entry.getKey()).get(entryInBoard.getKey()).get(port)
                                );
                            }
                        });
                    }
                }
            }
        }
    }

    // Create new mapping of board name -> index -> ports and values
    // from given json
    private Map<String, Map<String, Map<String, Double>>> jsonToBoardsMap(String json) {
        Map<String, Map<String, Map<String, Double>>> data = new HashMap<>();
        Gson gson = new Gson();
        Map<?, ?> element = gson.fromJson(json, Map.class); // json String to Map

        for (Object key : element.keySet()) { // Iterate over board types
            data.put((String) key, new HashMap<>()); // Add board name to map
            Object value = element.get(key);

            // Check if board contains map of boards or list of ports
            // board in json might have mapping of a number of boards of its type
            // or list of ports that will be treated as if there's only one board of this type
            if (value instanceof ArrayList) { // If board has list of ports.

                @SuppressWarnings("unchecked")
                ArrayList<String> ports = (ArrayList<String>) value;
                Map<String, Double> portMap = new HashMap<>();
                ports.forEach(port -> portMap.put(fixName(port), null));
                data.get(key).put("_1", portMap); // Index of the first board of this type is _1

            } else if (value instanceof LinkedTreeMap) { // If board has map boards of this type
                @SuppressWarnings("unchecked")
                Map<String, List<String>> valueMapped = (Map<String, List<String>>) value; // Map of boards to ports list
                for (Map.Entry<String, List<String>> intAndList : valueMapped.entrySet()) {

                    Set<String> portList = new HashSet<>(intAndList.getValue());

                    Map<String, Double> portMap = new HashMap<>();
                    portList.forEach(port -> portMap.put(fixName(port), null));
                    data.get(key).put(fixName(intAndList.getKey()), portMap);
                }
            }
        }
        return data;
    }

    private synchronized void addNicknamesToPortsMap(){
        Map<String, Map<String, Map<String, Double>>> updatedPortsMap = new HashMap<>();
        portsMap.forEach((boardName, indexes) -> {
            boolean indexesHaveNicknames = boardNicknamesMap.containsKey(boardName);
            boolean portsHaveNicknames = portNicknamesMap.containsKey(boardName);
            Map<String, Map<String, Double>> indexValueMap = new HashMap<>();
            indexes.forEach((index, ports) -> {
                boolean indexHasNickname = indexesHaveNicknames && boardNicknamesMap.get(boardName).containsKey(index);
                boolean indexPortsHaveNicknames = portsHaveNicknames && portNicknamesMap.get(boardName).containsKey(index);
                Map<String, Double> portValueMap = new HashMap<>();
                ports.forEach((port, value) -> {
                    portValueMap.put(port, value);
                    if (indexPortsHaveNicknames && portNicknamesMap.get(boardName).get(index).containsKey(port)){
                        String nickname = portNicknamesMap.get(boardName).get(index).getOrDefault(port, null);
                        portValueMap.put(portNicknamesMap.get(boardName).get(index).get(port), value);
                    }
                });
                indexValueMap.put(index, portValueMap);
                if (indexHasNickname) {
                    indexValueMap.put(boardNicknamesMap.get(boardName).get(index), portValueMap);
                }
            });
            updatedPortsMap.put(boardName, indexValueMap);
        });
        portsMap = updatedPortsMap;
    }

    private Map<String, Map<String, Double>> getBoardsByName(String name) {
        return portsMap.get(name);
    }

    public Set<String> getBoardNames() {
        return portsMap.keySet();
    }

    public Map<String, Double> getBoardByNameAndIndex(String name, String index) {
        try {
            return portsMap.get(name).get(index);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Set<String> getBoardIndexes(String name) {
        try {
            return portsMap.get(name).keySet();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Map<String, Double> getPortsAndValues(String boardName, String index) {
        try {
            return getBoardsByName(boardName).get(index);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Set<String> getPorts(String boardName, String boardIndex) {
        try {
            return portsMap.get(boardName).get(boardIndex).keySet();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void clear() {
        portsMap.clear();
    }

    private void setPortValue(String boardName, String boardIndex, String portName, Double newValue) {
        try {
            Map<String, Double> ports = getPortsAndValues(boardName, boardIndex);
            ports.replace(portName, newValue);
        } catch (NullPointerException ignore) {
        }
    }


    // Prepend '_' to port and board index names that start with a number
    private String fixName(String name) {
        char firstChar = name.charAt(0);
        return Character.isDigit(firstChar) ? "_" + name : name;
    }

    public Map<String, Map<String, Map<String, Double>>> getPortsMap() {
        return portsMap;
    }
//    {"Ev3":{"1":["2"],"2":["3"]},"GrovePi":["D3"]}

}
