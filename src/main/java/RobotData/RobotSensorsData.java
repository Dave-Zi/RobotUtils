package RobotData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"unused", "WeakerAccess"})

public class RobotSensorsData implements Cloneable {

    private final Logger logger = Logger.getLogger("Robot Sensor Data");
    //    board name -> board index -> ports -> values
    Map<String, Map<String, Map<String, Double>>> portsMap = new HashMap<>();
    //    board name -> board index -> board nickname
    final Map<String, Map<String, String>> boardNicknamesMap = new HashMap<>();
    //    board name -> board index -> board ports -> board nicknames
    final Map<String, Map<String, Map<String, String>>> portNicknamesMap = new HashMap<>();
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

    public void buildNicknameMaps(String json) throws IllegalArgumentException {
        Gson gson = new Gson();
        Map<?, ?> element = gson.fromJson(json, Map.class); // json String to Map
        for (Object boardNameKey : element.keySet()) { // Iterate over board types
            String boardName = (String) boardNameKey;
            Map<String, String> indexNicknames = new HashMap<>();
            Map<String, Map<String, String>> indexToPortsNicknames = new HashMap<>();
            @SuppressWarnings("unchecked")
            ArrayList<Map<String, ?>> boardsDataList =
                    (ArrayList<Map<String, ?>>) element.get(boardNameKey);

            for (int i = 0; i < boardsDataList.size(); i++) {
                Map<String, ?> portDataMap = boardsDataList.get(i);
                if (portDataMap.containsKey("Name") && !((String) portDataMap.get("Name")).isBlank()) {
                    String nickName = (String) portDataMap.get("Name");
                    if (indexNicknames.containsValue(nickName)) {
                        String errorMessage = String.format("Another board of type %s was already given the name %s", boardName, nickName);
                        logger.log(Level.SEVERE, errorMessage);
                        throw new IllegalArgumentException(errorMessage);
                    }
                    checkNickname(nickName);

                    indexNicknames.put(String.valueOf(i+1), (String) portDataMap.get("Name"));
                }
                Map<String, String> portsNicknames = new HashMap<>();
                for (Map.Entry<String, ?> ports : portDataMap.entrySet()) {
                    if (ports.getValue() instanceof LinkedTreeMap) { // Check if port value is actually a map with nickname
                        @SuppressWarnings("unchecked")
                        Map<String, String> valueMap = (Map<String, String>) ports.getValue();
                        if (valueMap.containsKey("Name") && !valueMap.get("Name").isBlank()) {
                            String nickName = valueMap.get("Name");
                            String errorMessage = String.format(
                                    "Another port on board %s of type %s on was already given the name %s",
                                    indexNicknames.containsKey("_" + (i + 1)) ? indexNicknames.get("_" + (i + 1)) : "_" + (i + 1),
                                    boardName,
                                    nickName);
                            if (indexNicknames.containsValue(nickName)) {
                                logger.log(Level.SEVERE, errorMessage);
                                throw new IllegalArgumentException(errorMessage);
                            }
                            checkNickname(nickName);
                            if (nickName.equals("speed")){
                                logger.log(Level.SEVERE, "Port cannot be named speed");
                                throw new IllegalArgumentException("Port cannot be named speed");
                            }
                            portsNicknames.put(ports.getKey(), nickName);
                        }
                    }
                }
                indexToPortsNicknames.put(String.valueOf(i + 1), portsNicknames);
            }
            boardNicknamesMap.put(boardName, indexNicknames);
            portNicknamesMap.put(boardName, indexToPortsNicknames);
        }
    }

    public String replaceNicksInJson(String json) {
        Map<String, Map<String, Object>> newJson = new HashMap<>();
        Gson gson = new Gson();
        Map<?, ?> element = gson.fromJson(json, Map.class); // json String to Map

        for (Object key : element.keySet()) { // Iterate over board types
            String boardType = (String) key;
            newJson.put(boardType, new HashMap<>()); // Add board name to map
            Object value = element.get(key);

            // Check if board contains map of boards or list of ports
            // board in json might have mapping of a number of boards of its type
            // or list of ports that will be treated as if there's only one board of this type
            if (value instanceof ArrayList) { // If board has list of ports.

                @SuppressWarnings("unchecked")
                ArrayList<String> ports = (ArrayList<String>) value;
                ArrayList<String> portsArray = new ArrayList<>();
                Map<String, String> portsNicksMap = portNicknamesMap.get(boardType).get("1");
                ports.forEach(port -> portsArray.add(
                        portsNicksMap.containsValue(port) ?
                                getKeyByValue(portsNicksMap, port) : port));

                newJson.get(boardType).put("1", portsArray); // Index of the first board of this type is 1

            } else if (value instanceof LinkedTreeMap) { // If board has map boards of this type
                @SuppressWarnings("unchecked")
                LinkedTreeMap<Object, Object> valueObject = (LinkedTreeMap<Object, Object>) value; // Map of boards to ports list
                Optional<Object> firstValue = valueObject.values().stream().findFirst();
                if (firstValue.isEmpty()){
                    continue;
                }
                Object realFirstValue = firstValue.get();
                if (realFirstValue instanceof  ArrayList){
                    @SuppressWarnings("unchecked")
                    Map<String, List<String>> valueMapped = (Map<String, List<String>>) value; // Map of boards to ports list
                    for (Map.Entry<String, List<String>> intAndList : valueMapped.entrySet()) {

                        ArrayList<String> portList = new ArrayList<>(intAndList.getValue());
                        String boardIndex = boardNicknamesMap
                                .get(boardType)
                                .containsValue(intAndList.getKey()) ?
                                getKeyByValue(boardNicknamesMap.get(boardType), intAndList.getKey())
                                : intAndList.getKey();

                        ArrayList<String> portsArray = new ArrayList<>();
                        Map<String, String> portsNicksMap = portNicknamesMap.get(boardType).get(boardIndex);
                        portList.forEach(port -> portsArray.add(
                                portsNicksMap.containsValue(port) ?
                                        getKeyByValue(portsNicksMap, port) : port));
                        newJson.get(boardType).put(boardIndex, portsArray);
                    }
                } else if (realFirstValue instanceof LinkedTreeMap){
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Object>> valueMapped = (Map<String, Map<String, Object>>) value; // Map of boards to ports list
                    for (Map.Entry<String, Map<String, Object>> intAndMap : valueMapped.entrySet()) {

                        String boardIndex = boardNicknamesMap
                                .get(boardType)
                                .containsValue(intAndMap.getKey()) ?
                                getKeyByValue(boardNicknamesMap.get(boardType), intAndMap.getKey())
                                : intAndMap.getKey();

                        Map<String, Object> newPortMap = new HashMap<>();
                        Map<String, String> portsNicksMap = portNicknamesMap.get(boardType).get(boardIndex);
                        intAndMap.getValue().forEach((port, data) -> newPortMap.put(
                                portsNicksMap.containsValue(port) ?
                                        getKeyByValue(portsNicksMap, port) : port, data));
                        newJson.get(boardType).put(boardIndex, newPortMap);
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ports = (Map<String, Object>) value;
                    Map<String, Object> newPortMap = new HashMap<>();
                    Map<String, String> portsNicksMap = portNicknamesMap.get(boardType).get("1");
                    ports.forEach((port, data) -> newPortMap.put(
                            portsNicksMap.containsValue(port) ?
                                    getKeyByValue(portsNicksMap, port) : port, data));

                    newJson.get(boardType).put("1", newPortMap); // Index of the first board of this type is 1
                }
            }
        }
        return new GsonBuilder().create().toJson(newJson);
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
                                    && portNicknamesMap.get(boardName).get(boardIndex.getKey()).containsKey(portAndValue.getKey())) {
                                String nickname = portNicknamesMap.get(boardName).get(boardIndex.getKey()).get(portAndValue.getKey());
                                setPortValue(boardName, boardIndex.getKey(), nickname, portAndValue.getValue());
                            }
                            updated = true;
                        }
                    }
                    if (boardNicknamesMap.containsKey(boardName)
                            && boardNicknamesMap.get(boardName).containsKey(boardIndex.getKey())) {
                        String indexNickname = boardNicknamesMap.get(boardName).get(boardIndex.getKey());

                        if (portsMap.get(boardName).containsKey(indexNickname)) {
                            for (Map.Entry<String, Double> portAndValue : boardIndex.getValue().entrySet()) {
                                setPortValue(boardName, indexNickname, portAndValue.getKey(), portAndValue.getValue());
                                if (portNicknamesMap.containsKey(boardName)
                                        && portNicknamesMap.get(boardName).containsKey(boardIndex.getKey())
                                        && portNicknamesMap.get(boardName).get(boardIndex.getKey()).containsKey(portAndValue.getKey())) {
                                    String nickname = portNicknamesMap.get(boardName).get(boardIndex.getKey()).get(portAndValue.getKey());
                                    setPortValue(boardName, indexNickname, nickname, portAndValue.getValue());
                                }
                                updated = true;
                            }
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
                                    && portNicknamesMap.get(entry.getKey()).get(entryInBoard.getKey()).containsKey(port)) {
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
                ports.forEach(port -> {
                    portMap.put(fixName(port), null);
                    if (portNicknamesMap.containsKey((String) key)
                            && portNicknamesMap.get((String) key).containsKey("1")) {
                        if (portNicknamesMap.get((String) key).get("1").containsKey(fixName(port))) {
                            portMap.putIfAbsent(portNicknamesMap.get((String) key).get("1").get(fixName(port)), null);
                        }
                        if (portNicknamesMap.get((String) key).get("1").containsValue(fixName(port))) {
                            String originalKey = getKeyByValue(portNicknamesMap.get((String) key).get("1"), fixName(port));
                            portMap.putIfAbsent(originalKey != null? fixName(originalKey): null, null);
                        }
                    }
                });
                data.get(key).put("1", portMap); // Index of the first board of this type is 1
                if (boardNicknamesMap.containsKey((String) key)) {
                    if (boardNicknamesMap.get((String) key).containsKey("1")) {
                        data.get(key).putIfAbsent(boardNicknamesMap.get((String) key).get("1"), portMap); // Index of the first board of this type is 1
                    }
                }
            } else if (value instanceof LinkedTreeMap) { // If board has map boards of this type
                @SuppressWarnings("unchecked")
                Map<String, List<String>> valueMapped = (Map<String, List<String>>) value; // Map of boards to ports list
                for (Map.Entry<String, List<String>> intAndList : valueMapped.entrySet()) {

                    Set<String> portList = new HashSet<>(intAndList.getValue());

                    Map<String, Double> portMap = new HashMap<>();
                    portList.forEach(port -> {
                        portMap.put(fixName(port), null);
                        if (portNicknamesMap.containsKey((String) key)) {
                            String realIndex = fixName(intAndList.getKey());
                            if (boardNicknamesMap.containsKey((String) key) &&
                                    boardNicknamesMap.get((String) key).containsValue(realIndex)) {
                                realIndex = getKeyByValue(boardNicknamesMap.get((String) key), realIndex);
                            }

                            if (portNicknamesMap.get((String) key).containsKey(realIndex)) {
                                if (portNicknamesMap.get((String) key).get(realIndex).containsKey(fixName(port))) {
                                    portMap.putIfAbsent(portNicknamesMap.get((String) key).get(realIndex).get(fixName(port)), null);
                                }
                                if (portNicknamesMap.get((String) key).get(realIndex).containsValue(fixName(port))) {
                                    String originalKey = getKeyByValue(portNicknamesMap.get((String) key).get(realIndex), fixName(port));
                                    portMap.putIfAbsent(originalKey != null? fixName(originalKey): null, null);
                                }
                            }
                        }
                    });
                    data.get(key).put(fixName(intAndList.getKey()), portMap);
                    if (boardNicknamesMap.containsKey((String) key)) {
                        if (boardNicknamesMap.get((String) key).containsKey(fixName(intAndList.getKey()))) {
                            data.get(key).putIfAbsent(boardNicknamesMap.get((String) key).get(fixName(intAndList.getKey())), portMap);
                        }
                        if (boardNicknamesMap.get((String) key).containsValue(fixName(intAndList.getKey()))) {
                            String originalKey = getKeyByValue(boardNicknamesMap.get((String) key), fixName(intAndList.getKey()));
                            data.get(key).putIfAbsent(originalKey != null? fixName(originalKey): null, portMap);
                        }
                    }
                }
            }
        }
        return data;
    }

    private synchronized void addNicknamesToPortsMap() {
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
                    if (indexPortsHaveNicknames && portNicknamesMap.get(boardName).get(index).containsKey(port)) {
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

    private String getKeyByValue(Map<String, String> map, String value) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void checkNickname(String nickName){
        try {
            Integer.parseInt(nickName);
            if (nickName.charAt(0) == '_'){
                Integer.parseInt(nickName.substring(1));
            }
        } catch (NumberFormatException nfe) {
            return;
        }
        String errorMessage = String.format("Board cannot be named like an index - %s", nickName);
        logger.log(Level.SEVERE, errorMessage);
        throw new IllegalArgumentException(errorMessage);
    }

}
