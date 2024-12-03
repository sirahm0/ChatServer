import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.out.println("Starting server using port 8189");

        Database.createDatabase();
        RoomManager.initialise();
        Api.run();
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            while (true) {
                // Waiting for client to connect
                Socket incomingSocket = serverSocket.accept();
                System.out.println("New client connected");

                // New thread to handle each user
                new Thread(new ClientHandler(incomingSocket)).start();
            }
        }
    }
}


