import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final Map<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();
    public static void initialise() {
        Map<String, String> saved_rooms = Database.getRooms();
        for (String roomName : saved_rooms.keySet()) {
            if (!rooms.containsKey(roomName)) {
                rooms.put(roomName, ConcurrentHashMap.newKeySet());
            }
        }
    }
    public static boolean createRoom(String roomName, ClientHandler client) {
        Database.createRoom(roomName, client.userName);
        if (!rooms.containsKey(roomName)) {
            rooms.put(roomName, ConcurrentHashMap.newKeySet());
            joinRoom(roomName, client);
            return true;
        }
        return false;
    }

    public static boolean joinRoom(String roomName, ClientHandler client) {
        Set<ClientHandler> roomUsers = rooms.get(roomName);
        if (roomUsers != null) {
            roomUsers.add(client);
            return true;
        }
        return false;
    }

    public static void leaveRoom(String roomName, ClientHandler client) {
        Set<ClientHandler> roomUsers = rooms.get(roomName);
        if (roomUsers != null) {
            roomUsers.remove(client);
        }
    }

    public static void broadcast(String roomName, String message, ClientHandler sender) {
        Set<ClientHandler> roomUsers = rooms.getOrDefault(roomName, Collections.emptySet());
        for (ClientHandler writer : roomUsers) {
            if (writer != sender) {
                writer.output.println(sender.userName + ": " + message);
            }
        }
        Database.recordMessage(roomName, sender.userName, message);
    }

    public static Set<String> getAllRooms() {
        return rooms.keySet();
    }

    public static void getRoomHistory(String roomName, ClientHandler client) {
        ArrayList<String> messages = Database.getRoomHistory(roomName);
        for (String message: messages) {
            client.output.println(message.trim());
        }
    }
}
