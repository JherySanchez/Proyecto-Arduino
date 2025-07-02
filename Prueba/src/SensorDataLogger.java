import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.json.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SensorDataLogger {

    // --- Configuración del Puerto Serial ---
    // puerto arduino
    // En Windows: "COM3", "COM4"

    private static final String SERIAL_PORT_NAME = "COM3";
    private static final int BAUD_RATE = 9600;

    // conex Mysql
    private static final String DB_URL = "jdbc:mysql://localhost:3306/prueba"; // El archivo de la DB se creará aquí
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";
    private static final String TABLE_NAME = "sensor";

    private SerialPort serialPort;
    private StringBuilder serialBuffer = new StringBuilder();

    public static void main(String[] args) {
        // Inicializar la base de datos
        initDatabase();
       
        // Crear e iniciar el logger
        SensorDataLogger logger = new SensorDataLogger();
        logger.startReading();
    }

    public void startReading() {
        serialPort = new SerialPort(SERIAL_PORT_NAME);
        try {
            serialPort.openPort();
            serialPort.setParams(BAUD_RATE, 8, 1, 0); // Baud rate, data bits, stop bits, parity
            serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR); // Escuchar datos entrantes

            System.out.println("Conectado a " + SERIAL_PORT_NAME + " a " + BAUD_RATE + " bps. Esperando datos...");

            
            while (true) {
                Thread.sleep(100);//pausas
            }

        } catch (SerialPortException | InterruptedException e) {
            System.err.println("Error al configurar el puerto serial: " + e.getMessage());
            if (serialPort != null && serialPort.isOpened()) {
                try {
                    serialPort.closePort();
                } catch (SerialPortException ex) {
                    System.err.println("Error al cerrar el puerto: " + ex.getMessage());
                }
            }
        }
    }

    private class PortReader implements SerialPortEventListener {
        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR() && event.getEventValue() > 0) { // Si hay bytes recibidos
                try {
                    String receivedData = serialPort.readString(event.getEventValue());
                    serialBuffer.append(receivedData);

                    // Procesa línea por línea si hay saltos de línea
                    int newLineIndex;
                    while ((newLineIndex = serialBuffer.indexOf("\n")) != -1) {
                        String line = serialBuffer.substring(0, newLineIndex).trim();
                        serialBuffer.delete(0, newLineIndex + 1); // Elimina la línea procesada

                        if (!line.isEmpty()) {
                            processLine(line);
                        }
                    }

                } catch (SerialPortException e) {
                    System.err.println("Error al leer del puerto serial: " + e.getMessage());
                }
            }
        }
    }

    private void processLine(String line) {
        System.out.println("Recibido: " + line);
        try {
            // Analizar la cadena JSON
            JSONObject json = new JSONObject(line);

            // Extraer datos (manejo de posibles valores nulos si Arduino envía "null")
            Integer gas = json.has("gas") && !json.isNull("gas") ? json.getInt("gas") : null;
            Integer luz = json.has("luz") && !json.isNull("luz") ? json.getInt("luz") : null;
            Double temperatura = json.has("temperatura") && !json.isNull("temperatura") ? json.getDouble("temperatura") : null;
            Double humedad = json.has("humedad") && !json.isNull("humedad") ? json.getDouble("humedad") : null;
            Boolean movimiento = json.has("movimiento") && !json.isNull("movimiento") ? json.getBoolean("movimiento") : null;
            Integer distancia = json.has("distancia") && !json.isNull("distancia") ? json.getInt("distancia") : null;

            // Guardar en la base de datos
            insertData(gas, luz, temperatura, humedad, movimiento, distancia);

        } catch (org.json.JSONException e) { // Cambié a org.json.JSONException
            System.err.println("Error al parsear JSON: " + e.getMessage() + " - Línea: " + line);
        }
    }

    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                                  + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                                  + "    gas INTEGER,\n"
                                  + "    luz INTEGER,\n"
                                  + "    temperatura REAL,\n"
                                  + "    humedad REAL,\n"
                                  + "    movimiento BOOLEAN,\n"
                                  + "    distancia INTEGER,\n"
                                  + "    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP\n"
                                  + ");";
            conn.createStatement().execute(createTableSQL);
            System.out.println("Base de datos y tabla inicializadas.");
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    private static void insertData(Integer gas, Integer luz, Double temperatura, Double humedad, Boolean movimiento, Integer distancia) {
        String sql = "INSERT INTO " + TABLE_NAME + "(gas, luz, temperatura, humedad, movimiento, distancia) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
           
            // Usar setInt, setDouble, setBoolean, etc. y manejar nulls
            if (gas != null) pstmt.setInt(1, gas); else pstmt.setNull(1, java.sql.Types.INTEGER);
            if (luz != null) pstmt.setInt(2, luz); else pstmt.setNull(2, java.sql.Types.INTEGER);
            if (temperatura != null) pstmt.setDouble(3, temperatura); else pstmt.setNull(3, java.sql.Types.REAL);
            if (humedad != null) pstmt.setDouble(4, humedad); else pstmt.setNull(4, java.sql.Types.REAL);
            if (movimiento != null) pstmt.setBoolean(5, movimiento); else pstmt.setNull(5, java.sql.Types.BOOLEAN);
            if (distancia != null) pstmt.setInt(6, distancia); else pstmt.setNull(6, java.sql.Types.INTEGER);
           
            pstmt.executeUpdate();
            System.out.println("Datos guardados en DB: Gas=" + gas + ", Luz=" + luz + ", Temp=" + temperatura + ", Hum=" + humedad + ", Mov=" + movimiento + ", Dist=" + distancia);
        } catch (SQLException e) {
            System.err.println("Error al insertar datos: " + e.getMessage());
        }
    }
}
