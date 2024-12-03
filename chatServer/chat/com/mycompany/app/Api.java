import static spark.Spark.*;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;


public class Api {
    static final int PORT = 8188; // !! PORT !! //
    public static void enableCORS(String origin, String methods, String headers) {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Allow-Methods", methods);
            response.header("Access-Control-Allow-Headers", headers);
        });
    }

    static final String SECRET_KEY = "mZ8s9dMm2s9B2Splm0al2lf9a35f00A42dp4S6ldd2q72z2mdAD2590FdaD9252EddA51";

    public static void run() {
        System.out.println("Starting server using port " + PORT);

        port(PORT);
        Gson gson = new Gson();

        post("/signup", (req, res) -> {
            // Set the response content type
            res.type("application/json");

            String body = req.body();
            User user = gson.fromJson(body, User.class); // Deserialize JSON into User object

            if (user == null || user.username == null || user.password == null || user.username.isEmpty() || user.password.isEmpty()) {
                res.status(400); // Bad request
                return gson.toJson(new ResponseMessage("Username and password are required"));
            }

            //Database.signup(user.username, user.password);

            return gson.toJson(new ResponseMessage("Signup successful"));
        });

        post("/login", (req, res) -> {
            res.type("application/json");
            String body = req.body();
            System.out.println(body);
            User user = gson.fromJson(body, User.class); // Deserialize JSON into User object

            // Check if the username and password are provided
            if (user.username == null || user.password == null || user.username.isEmpty() || user.password.isEmpty()) {
                res.status(400); // Bad request
                return gson.toJson(new ResponseMessage("Username and password are required"));
            }

            // Authenticate the user
            boolean authenticated = Database.authenticate(user.username, user.password);

            // If authentication is successful, generate a JWT token
            if (authenticated) {
                String token = generateToken(user.username, "admin");
                return gson.toJson(new Token(token));
            } else {
                res.status(400); // Bad request
                return gson.toJson(new ResponseMessage("Username or password is incorrect"));
            }
        });

        post("/deleteRoom", (req, res) -> {
            // Set the response content type
            res.type("application/json");

            String token = extractToken(req);
            if (token == null) {
                res.status(401);  // Unauthorized
                return gson.toJson(new ResponseMessage("Token missing or invalid"));
            }

            // Verify the token and check if the user has the necessary permissions
            Claims claims = verifyToken(token, res);
            if (claims == null) {
                return gson.toJson(new ResponseMessage("Invalid or expired token"));
            }

            // Example: Check if the user has the required permission to delete the room
            String role = claims.get("role", String.class);
            if (role == null || !role.equals("admin")) {
                res.status(403);  // Forbidden
                return gson.toJson(new ResponseMessage("Insufficient permissions"));
            }


            String body = req.body();
            RoomRequest roomRequest = gson.fromJson(body, RoomRequest.class); // Deserialize JSON into RoomRequest object

            // Call the deleteRoom function
            boolean success = Database.deleteRoom(roomRequest.roomId);

            // Return response based on the result
            if (success) {
                return gson.toJson(new ResponseMessage("Room deleted successfully"));
            } else {
                res.status(400); // Bad request
                return gson.toJson(new ResponseMessage("Failed to delete room or room not found"));
            }
        });

        post("/createRoom", (req, res) -> {
            res.type("application/json");

            String token = extractToken(req);
            if (token == null) {
                res.status(401);  // Unauthorized
                return gson.toJson(new ResponseMessage("Token missing or invalid"));
            }

            // Verify the token and check if the user has the necessary permissions
            Claims claims = verifyToken(token, res);
            if (claims == null) {
                return gson.toJson(new ResponseMessage("Invalid or expired token"));
            }

            // Example: Check if the user has the required permission to delete the room
            String role = claims.get("role", String.class);
            if (role == null || !role.equals("admin")) {
                res.status(403);  // Forbidden
                return gson.toJson(new ResponseMessage("Insufficient permissions"));
            }

            String body = req.body();
            RoomRequest roomRequest = gson.fromJson(body, RoomRequest.class); // Deserialize JSON into RoomRequest object

            // Call the deleteRoom function
            String name = claims.get("username", String.class);
            boolean success = Database.createRoom(roomRequest.roomId, name);

            // Return response based on the result
            if (success) {
                return gson.toJson(new ResponseMessage("Room deleted successfully"));
            } else {
                res.status(400); // Bad request
                return gson.toJson(new ResponseMessage("Failed to delete room or room not found"));
            }
        });

        post("/getRooms", (req, res) -> Database.getRooms());

        post("/getRoomHistory", (reg, res) -> {
            return "";
        });
        exception(Exception.class, (exception, req, res) -> {
            res.status(500);
            res.body(gson.toJson(new ResponseMessage(exception.getMessage())));; // Send exception message in response
        });
    }


    static class User {
        String username;
        String password;
    }

    static class RoomRequest {
        String roomId;
    }

    static class Token {
        String token;
        Token(String token) {
            this.token = token;
        }
    }
    static class ResponseMessage {
        String message;

        public ResponseMessage(String message) {
            this.message = message;
        }
    }

    static class Message {
        String author;
        String content;
        Message(String author, String content) {
            this.author = author;
            this.content = content;
        }
    }

    // Method to generate a JWT token
    private static String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        Date expirationDate = new Date(now + 1000 * 60 * 60); // 1 hour expiration

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(expirationDate)
                .claim("username", username)  // Add username as a claim
                .claim("role", role)          // Add role as a claim
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }


    // Method to extract the token from the Authorization header
    private static String extractToken(spark.Request req) {
        String token = req.headers("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);  // Remove "Bearer " prefix
        }
        return null;
    }

    // Method to verify the token and return the claims
    private static Claims verifyToken(String token, spark.Response res) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException e) {
            res.status(401);  // Unauthorized
            return null;
        }
    }

}

