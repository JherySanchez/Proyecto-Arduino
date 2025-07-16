import com.fazecast.jSerialComm.SerialPort;
import org.json.JSONObject;

import java.io.InputStream;
import java.sql.*;

public class SensorDataLogger {

    private static final String SERIAL_PORT_NAME = "COM4"; // Asegúrate que sea el puerto correcto
    private static final int BAUD_RATE = 9600;

    private static final String DB_URL = "jdbc:mysql://localhost:3307/arduino";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static final String TABLE_NAME = "sensores";

    public static void main(String[] args) {
        initDatabase();
        new SensorDataLogger().startReading();
    }

    public void startReading() {
        SerialPort port = SerialPort.getCommPort(SERIAL_PORT_NAME);
        port.setBaudRate(BAUD_RATE);
        port.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

        if (port.openPort()) {
            System.out.println("Conectado a " + SERIAL_PORT_NAME + ". Esperando datos...");
            InputStream in = port.getInputStream();

            StringBuilder buffer = new StringBuilder();
            try {
                while (true) {
                    int data = in.read();
                    if (data == -1) continue;

                    char c = (char) data;
                    if (c == '\n') {
                        String line = buffer.toString().trim();
                        buffer.setLength(0);
                        if (!line.isEmpty()) {
                            processLine(line);
                        }
                    } else {
                        buffer.append(c);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al leer del puerto: " + e.getMessage());
            } finally {
                port.closePort();
            }
        } else {
            System.err.println("No se pudo abrir el puerto " + SERIAL_PORT_NAME);
        }
    }

    private void processLine(String line) {
        System.out.println("Recibido: " + line);
        try {
            JSONObject json = new JSONObject(line);
            Integer gas = json.has("gas") && !json.isNull("gas") ? json.getInt("gas") : null;
            Integer luz = json.has("luz") && !json.isNull("luz") ? json.getInt("luz") : null;
            Double temperatura = json.has("temperatura") && !json.isNull("temperatura") ? json.getDouble("temperatura") : null;
            Double humedad = json.has("humedad") && !json.isNull("humedad") ? json.getDouble("humedad") : null;
            Boolean movimiento = json.has("movimiento") && !json.isNull("movimiento") ? json.getBoolean("movimiento") : null;
            Integer distancia = json.has("distancia") && !json.isNull("distancia") ? json.getInt("distancia") : null;

            insertData(gas, luz, temperatura, humedad, movimiento, distancia);
        } catch (Exception e) {
            System.err.println("Error al parsear JSON: " + e.getMessage() + " - Línea: " + line);
        }
    }

    private static void initDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                        + "    id INTEGER PRIMARY KEY AUTO_INCREMENT,\n"
                        + "    gas INTEGER,\n"
                        + "    luz INTEGER,\n"
                        + "    temperatura DOUBLE,\n"
                        + "    humedad DOUBLE,\n"
                        + "    movimiento BOOLEAN,\n"
                        + "    distancia INTEGER,\n"
                        + "    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP\n"
                        + ");";
                conn.createStatement().execute(createTableSQL);
                System.out.println("Base de datos y tabla inicializadas.");
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    private static void insertData(Integer gas, Integer luz, Double temperatura, Double humedad, Boolean movimiento, Integer distancia) {
        String sql = "INSERT INTO " + TABLE_NAME + "(gas, luz, temperatura, humedad, movimiento, distancia) VALUES(?, ?, ?, ?, ?, ?)";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                if (gas != null) pstmt.setInt(1, gas); else pstmt.setNull(1, Types.INTEGER);
                if (luz != null) pstmt.setInt(2, luz); else pstmt.setNull(2, Types.INTEGER);
                if (temperatura != null) pstmt.setDouble(3, temperatura); else pstmt.setNull(3, Types.DOUBLE);
                if (humedad != null) pstmt.setDouble(4, humedad); else pstmt.setNull(4, Types.DOUBLE);
                if (movimiento != null) pstmt.setBoolean(5, movimiento); else pstmt.setNull(5, Types.BOOLEAN);
                if (distancia != null) pstmt.setInt(6, distancia); else pstmt.setNull(6, Types.INTEGER);
                
                System.out.println("INSERTANDO: Gas=" + gas + ", Luz=" + luz + ", Temp=" + temperatura + ", Hum=" + humedad + ", Mov=" + movimiento + ", Dist=" + distancia);
                pstmt.executeUpdate();
                System.out.println("Datos guardados en DB: Gas=" + gas + ", Luz=" + luz + ", Temp=" + temperatura + ", Hum=" + humedad + ", Mov=" + movimiento + ", Dist=" + distancia);
            }
        } catch (Exception e) {
            System.err.println("Error al insertar datos: " + e.getMessage());
        }
    }
}
