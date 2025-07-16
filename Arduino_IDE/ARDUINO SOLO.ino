#include <DHT.h>

// Pines para el sensor de gas MQ2
const int mq2Pin = A1;
const int ledGas = 10;
const int umbralGas = 100;

// Pines para la LDR (fotoresistor)
const int ldrPin = A0;
const int ledLuz = 9;

// Pines para el sensor DHT11 (Humedad y Temperatura)
#define DHTPIN 8
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);
const int ledTemp = 5;

// Pines para el sensor PIR
const int pirPin = 7;
const int ledPIR = 6;

// Pines para el sensor ultrasónico
const int trigPin = 2;
const int echoPin = 4;

// Variables
long duration;
int distance;

void setup() {
  // Comunicación serial
  Serial.begin(9600);

  // Configurar pines de salida para los LEDs
  pinMode(ledGas, OUTPUT);
  pinMode(ledLuz, OUTPUT);
  pinMode(ledTemp, OUTPUT);
  pinMode(ledPIR, OUTPUT);

  // Configurar sensores
  pinMode(pirPin, INPUT);
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  // Inicializar sensor DHT
  dht.begin();
}

void loop() {
  // ---------------- Sensor de Gas ----------------
  int valorGas = analogRead(mq2Pin);
  Serial.print("Gas: ");
  Serial.println(valorGas);
  digitalWrite(ledGas, valorGas > umbralGas ? HIGH : LOW);

  // ---------------- Sensor de Luz (LDR) ----------------
  int valorLuz = analogRead(ldrPin);
  Serial.print("Luz: ");
  Serial.println(valorLuz);
  digitalWrite(ledLuz, valorLuz > 500 ? HIGH : LOW);

  // ---------------- Sensor DHT11 ----------------
  float humedad = dht.readHumidity();
  float temperatura = dht.readTemperature();
  if (!isnan(humedad) && !isnan(temperatura)) {
    Serial.print("Temperatura: ");
    Serial.print(temperatura);
    Serial.print(" °C\tHumedad: ");
    Serial.print(humedad);
    Serial.println(" %");
    digitalWrite(ledTemp, temperatura > 24.0 ? HIGH : LOW);
  } else {
    Serial.println("Error leyendo del sensor DHT11");
  }

  // ---------------- Sensor PIR ----------------
  bool movimiento = digitalRead(pirPin);
  Serial.print("Movimiento: ");
  Serial.println(movimiento ? "Detectado" : "No detectado");
  digitalWrite(ledPIR, movimiento ? HIGH : LOW);

  // ---------------- Sensor Ultrasónico ----------------
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  duration = pulseIn(echoPin, HIGH);
  distance = duration * 0.034 / 2;
  Serial.print("Distancia: ");
  Serial.print(distance);
  Serial.println(" cm");
  Serial.println("------------------------------------------");

  // ---------------- Espera ----------------
  delay(1000); // Espera 1 segundo antes de repetir
}