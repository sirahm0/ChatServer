import java.awt.datatransfer.Transferable;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    private final Socket incoming;
    private Scanner input;
    public PrintWriter output;
    public String userName;
    private String currentRoom;
    private Boolean isExiting = false;

    public ClientHandler(Socket incomingSocket) {
        this.incoming = incomingSocket;
    }

    @Override
    public void run() {
        try (
                InputStream inStream = incoming.getInputStream();
                OutputStream outStream = incoming.getOutputStream()
        ) {
            input = new Scanner(inStream, StandardCharsets.UTF_8);
            output = new PrintWriter(outStream, true, StandardCharsets.UTF_8);

            setAccount();

            while (!isExiting) {
                chooseRoom();
                startChatting();
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            try {
                incoming.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void setAccount() {
        while (true) {
            String command = ask_client("Welcome! Type 'login' to log in or 'signup' to create a new account or 'exit' to exit.").toLowerCase();
            switch (command.toLowerCase()) {
                case "login":
                    login();
                    return;
                case "signup":
                    signup();
                    return;
                case "exit":
                    isExiting = true;
                    return;
                default:
                    output.println("Unknown command. try again or exit...");
            }
        }
    }
    private void login() {//must add a way to esc if user have an account
        while(true) {

            String userName = ask_client("Enter your username:");
            String password = ask_client("Enter your password:");

            if (userName.equalsIgnoreCase("exit") || password.equalsIgnoreCase("exit")) {
                isExiting = true;
                return;
            }

            if (Database.authenticate(userName, password)) {
                output.println("Login successful!");
                this.userName = userName;
                return;
            }
            else
                output.println("Invalid username or password. Try again.");

        }
    }


    private void signup() {
        String userName = ask_client("Enter your username:");
        String password = ask_client("Enter your password:");


        if (userName.equalsIgnoreCase("exit") || password.equalsIgnoreCase("exit")) {
            isExiting = true;
            return;
        }

        Database.signup(userName, password);
        this.userName = userName;
    }

    private void chooseRoom() {
        //must store it in database, and retrieve from it
        while (!isExiting) {
            String command = ask_client("Available rooms: " + RoomManager.getAllRooms() + ". Type a room name to join or 'create' to create a new room:");


            switch (command.toLowerCase()) {
                case "create":
                    String roomName = ask_client(("Enter the new room name:"));
                    if (createRoom(roomName)) {
                        currentRoom =  roomName;
                        return;
                    }
                    break;
                case "exit":
                    isExiting = true;
                    currentRoom = null;
                    return;
                default:
                    if (RoomManager.joinRoom(command, this)) {
                        output.println("You joined the room: " + command);
                        currentRoom =  command;
                        return;
                    } else {
                        output.println("Room not found. Try again or create a new room.");
                    }
            }
        }

    }

    private Boolean createRoom(String roomName) {
        if (RoomManager.createRoom(roomName, this)) {
            output.println("Room created successfully. You joined: " + roomName);
            return true;
        } else {
            output.println("Room already exists. Try again.");
        }
        return false;
    }

    private void startChatting() {
        if (currentRoom == null)
            return;

        output.println("You are now in room: " + currentRoom);
        output.println("previous history:");
        RoomManager.getRoomHistory(currentRoom, this);
        output.println("Type your messages. Type 'leave' to leave the room.");

        while (true) {
            if (!input.hasNextLine()) break; //Handle client disconnection

            String message = input.nextLine();

            switch (message.toLowerCase()) {
                case "exit":
                    RoomManager.leaveRoom(currentRoom, this);
                    isExiting = true;
                    output.println("You have left the server.");
                    return;
                case "leave":
                    RoomManager.leaveRoom(currentRoom, this);
                    output.println("You have left the room: " + currentRoom);
                    currentRoom = null;
                    return;
            }
            broadcast(message);
        }
    }
    private void broadcast(String message) {
        RoomManager.broadcast(currentRoom, message, this);
    }

    private String ask_client(String line) {
        output.println(line);
        if (input.hasNextLine()) {
            String result = cleanBackspaces(input.nextLine()).trim();
            System.out.println("server asks: " + line + "\nclient answers: " + result + "\n");
            return result;
        }
        else
            System.exit(-1);
        return "";
    }

    private String cleanBackspaces(String input) {
        StringBuilder cleaned = new StringBuilder();

        for (char ch : input.toCharArray()) {
            if (ch == '\b' || ch == 127) { // Handle backspace
                if (!cleaned.isEmpty()) {
                    cleaned.deleteCharAt(cleaned.length() - 1);
                }
            } else {
                cleaned.append(ch);
            }
        }

        return cleaned.toString();
    }

}
