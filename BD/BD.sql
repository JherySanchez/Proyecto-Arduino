CREATE DATABASE arduino;
USE arduino;
CREATE TABLE sensor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    gas INT,
    luz INT,
    temperatura DOUBLE,
    humedad DOUBLE,
    movimiento BOOLEAN,
    distancia INT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
SHOW TABLES;
select * from sensor;