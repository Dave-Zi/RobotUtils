package RobotData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

import java.util.*;

public class RobotSensorsData {
    private Map<String, Map<String, Map<String, Double>>> portsMap = new HashMap<>();
    private boolean updated;

    public synchronized boolean isUpdated() {
        return updated;
    }

    public String toJson(){
        updated = false;
        return new GsonBuilder().create().toJson(portsMap);
    }

    public void updateBoardMapValues(String json){
        Gson gson = new Gson();
        Map element = gson.fromJson(json, Map.class); // json String to Map

        for (Object boardNameKey: element.keySet()) { // Iterate over board types
            String boardName = (String)boardNameKey;
            if (portsMap.containsKey(boardName)){ // We want only boards that exist on our map
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Double>> indexesToPorts = (Map<String, Map<String, Double>>)element.get(boardNameKey);
                for (Map.Entry<String, Map<String, Double>> boardIndex: indexesToPorts.entrySet()) { // Iterate over board indexes
                    if (portsMap.get(boardName).containsKey(boardIndex.getKey())){
                        for (Map.Entry<String, Double> portAndValue : boardIndex.getValue().entrySet()) {
                            setPortValue(boardName, boardIndex.getKey(), portAndValue.getKey(), portAndValue.getValue());
                            updated = true;
                        }
                    }
                }
            }
        }
    }

    // Add new sensors from json to mapping
    public void addToBoardsMap(String json){
        Map<String, Map<String, Map<String, Double>>> boards = jsonToBoardsMap(json); // Build Map of Robot Ports in json

        for (Map.Entry<String, Map<String, Map<String, Double>>> board : boards.entrySet()) { // Iterate over board types
            if (portsMap.keySet().contains(board.getKey())){ // If board type already exist in portsMap
                for (Map.Entry<String, Map<String, Double>> entryInBoard : board.getValue().entrySet()) { // Iterate over board map
                    Map<String, Map<String, Double>> boardsMap = portsMap.get(board.getKey());
                    if (boardsMap.keySet().contains(entryInBoard.getKey())){ // If  existing boards map already contain this board
                        boardsMap.get(entryInBoard.getKey()).putAll(entryInBoard.getValue()); // Add boards value to pre existing port list
                    } else {
                        boardsMap.put(entryInBoard.getKey(), entryInBoard.getValue()); // Put new board into map
                    }
                }
            } else { // If board type doesn't exist in portMap.
                portsMap.put(board.getKey(), board.getValue()); // Add board type with all its data to map
            }
        }
    }

    // Remove from mapping any sensors that exist on given json
    public void removeFromBoardsMap(String json){
        Map<String, Map<String, Map<String, Double>>> data = jsonToBoardsMap(json);

        for (Map.Entry<String, Map<String, Map<String, Double>>> entry : data.entrySet()) { // Iterate over boards
            if (portsMap.keySet().contains(entry.getKey())){ // If our board map contains this board
                for (Map.Entry<String, Map<String, Double>> entryInBoard : entry.getValue().entrySet()) { // Iterate over board indexes
                    Map<String, Map<String, Double>> boardsMap = portsMap.get(entry.getKey());
                    if (boardsMap.keySet().contains(entryInBoard.getKey())){ // If our board map contains board with this index
                        entryInBoard.getValue().forEach((port, value) -> boardsMap.get(entryInBoard.getKey()).remove(port));
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
        Map element = gson.fromJson(json, Map.class); // json String to Map

        for (Object key: element.keySet()){ // Iterate over board types
            data.put((String) key, new HashMap<>()); // Add board name to map
            Object value = element.get(key);

            // Check if board contains map of boards or list of ports
            // board in json might have mapping of a number of boards of its type
            // or list of ports that will be treated as if there's only one board of this type
            if (value instanceof ArrayList){ // If board has list of ports.

                @SuppressWarnings("unchecked")
                ArrayList<String> ports = (ArrayList<String>) value;
                Map<String, Double> portMap = new HashMap<>();
                ports.forEach(port -> portMap.put(fixName(port), null));
                data.get(key).put("_1", portMap); // Index of the first board of this type is _1

            } else if (value instanceof LinkedTreeMap){ // If board has map boards of this type
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

    public Map<String, Map<String, Double>> getBoardsByName(String name){
        return portsMap.get(name);
    }

    public Set<String> getBoardNames (){ return portsMap.keySet(); }

    public Map<String, Double> getBoardByNameAndIndex(String name, String index){ return portsMap.get(name).get(index); }

    public Set<String> getBoardIndexes (String name){ return portsMap.get(name).keySet(); }

    public Map<String, Double> getPortsAndValues(String boardName, String index){
        return getBoardsByName(boardName).get(index);
    }

    public Set<String> getPorts(String boardName, String boardIndex){
        return portsMap.get(boardName).get(boardIndex).keySet();
    }

    private void setPortValue(String boardName, String boardIndex, String portName, Double newValue) {
        Map<String, Double> ports = getPortsAndValues(boardName, boardIndex);
        ports.replace(portName, newValue);
    }


    // Prepend '_' to port and board index names that start with a number
    private String fixName(String name){
        char firstChar = name.charAt(0);
        return Character.isDigit(firstChar) ? "_"+name : name;
    }
//    {"Ev3":{"1":["2"],"2":["3"]},"GrovePi":["D3"]}

}
