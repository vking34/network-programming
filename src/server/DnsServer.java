package server;//import org.xbill.DNS.*;


import com.mysql.cj.jdbc.Driver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DnsServer {

    private final static int SERVER_PORT = 53;
    private static byte[] BUFFER = new byte[4096];
    private static String URL = "jdbc:mysql://localhost:3306/dns?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static Connection connection;

    public static void main (String[] args) throws SQLException{
        DatagramSocket ds = null;
        String answer;
        connectDB();

        try {
            System.out.println("Binding to port " + SERVER_PORT + ", please wait  ...");
            ds = new DatagramSocket(SERVER_PORT); // Create Socket with port 53
            System.out.println("Server started ");
            System.out.println("Waiting for messages from Client ... ");

            while (true) {
                // Create receiving packet
                DatagramPacket incoming = new DatagramPacket(BUFFER, BUFFER.length);
                ds.receive(incoming); // waiting

                // Lấy dữ liệu khỏi gói tin nhận
                String message = new String(incoming.getData(), 0, incoming.getLength());
                System.out.println("-------------------------------------------");
                System.out.println("Received message: " + message);

                answer = AnswerCreator.createAnswer(connection, message);
                System.out.println("Answer message: " + answer);
                System.out.println("-------------------------------------------");
                byte[] data = answer.getBytes();

                // Create the answer packet then send back to client
                DatagramPacket outsending = new DatagramPacket(data, data.length,
                        incoming.getAddress(), incoming.getPort());
                ds.send(outsending);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
    }

    static void connectDB(){
        Properties properties = new Properties();

        properties.put("user", "vking34");
        properties.put("password", "vking34");

        try {
            Driver driver = new Driver();

            connection = driver.connect(URL, properties);

            if (connection != null)
                System.out.println("Connected to DB");
            else
                System.out.println("Failed to connect to DB");

        } catch (SQLException e){
            e.printStackTrace();
        }
    }
}
