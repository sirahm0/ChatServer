import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Database {

    private static final String DB_URL = "jdbc:sqlite:chat_app.db";

    public static void createDatabase() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String createAccounts = """
                CREATE TABLE IF NOT EXISTS accounts (
                    username TEXT PRIMARY KEY,
                    password TEXT NOT NULL
                );
            """;
            String createRooms = """
                CREATE TABLE IF NOT EXISTS rooms (
                    roomId TEXT PRIMARY KEY,
                    owner TEXT NOT NULL,
                    FOREIGN KEY (owner) REFERENCES accounts(username)
                );
            """;
            String createMessages = """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    author TEXT NOT NULL,
                    roomId TEXT NOT NULL,
                    content TEXT NOT NULL,
                    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (author) REFERENCES accounts(username),
                    FOREIGN KEY (roomId) REFERENCES rooms(roomId)
                );
            """;

            stmt.execute(createAccounts);
            stmt.execute(createRooms);
            stmt.execute(createMessages);

            System.out.println("Database created successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Authenticate user
    public static boolean authenticate(String username, String password) {
        String query = "SELECT COUNT(*) FROM accounts WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Signup a new user
    public static void signup(String username, String password) {
        String query = "INSERT INTO accounts (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            pstmt.executeUpdate();
            System.out.println("User signed up successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean createRoom(String roomId, String username) {
        String query = "INSERT INTO rooms (roomId, owner) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, roomId);
            pstmt.setString(2, username);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Room created successfully!");
                return true;
            } else {
                return false;  // In case no rows are affected
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;  // Return false in case of an exception
        }
    }


    public static boolean deleteRoom(String roomId) {
        // SQL queries to delete messages associated with the room first
        String deleteMessages = "DELETE FROM messages WHERE roomId = ?";
        // SQL query to delete the room itself
        String deleteRoom = "DELETE FROM rooms WHERE roomId = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmtDeleteMessages = conn.prepareStatement(deleteMessages);
             PreparedStatement pstmtDeleteRoom = conn.prepareStatement(deleteRoom)) {

            // Start a transaction to ensure atomicity
            conn.setAutoCommit(false);

            // Delete messages related to the room
            pstmtDeleteMessages.setString(1, roomId);
            pstmtDeleteMessages.executeUpdate();

            // Delete the room
            pstmtDeleteRoom.setString(1, roomId);
            int rowsAffected = pstmtDeleteRoom.executeUpdate();

            // If no rows were affected, the room might not exist
            if (rowsAffected > 0) {
                // Commit the transaction if successful
                conn.commit();
                System.out.println("Room deleted successfully.");
                return true;
            } else {
                // Rollback in case the room doesn't exist
                conn.rollback();
                System.out.println("Room not found.");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static Map<String, String> getRooms() {
        Map<String, String> rooms = new HashMap<>();
        String query = "SELECT roomId, owner FROM rooms";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                rooms.put(rs.getString("roomId"), rs.getString("owner"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    public static ArrayList<String> getRoomHistory(String roomId) {
        ArrayList<String> messages = new ArrayList<>();
        String query = "SELECT author, content, date FROM messages WHERE roomId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, roomId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                messages.add(rs.getString("author") + ": " + rs.getString("content"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public static void recordMessage(String roomId, String author, String content) {
        String query = "INSERT INTO messages (roomId, author, content) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, roomId);
            pstmt.setString(2, author);
            pstmt.setString(3, content.trim());

            pstmt.executeUpdate();
            System.out.println("Message recorded successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
