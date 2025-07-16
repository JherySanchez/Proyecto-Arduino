#include <DHT.h>
#include <ArduinoJson.h>

// Pines para el sensor de gas MQ2
const int mq2Pin = A1;
const int ledGas = 10;
const int umbralGas = 100;

// Pines para la LDR (fotoresistor)
const int ldrPin = A0;
const int ledLuz = 9;

// Pines para el sensor DHT11
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
  Serial.begin(9600);

  pinMode(ledGas, OUTPUT);
  pinMode(ledLuz, OUTPUT);
  pinMode(ledTemp, OUTPUT);
  pinMode(ledPIR, OUTPUT);

  pinMode(pirPin, INPUT);
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  dht.begin();
}

void loop() {
  // Lectura de sensores
  int valorGas = analogRead(mq2Pin);
  int valorLuz = analogRead(ldrPin);
  float humedad = dht.readHumidity();
  float temperatura = dht.readTemperature();
  bool movimiento = digitalRead(pirPin);

  // Sensor ultrasónico
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  duration = pulseIn(echoPin, HIGH);
  distance = duration * 0.034 / 2;

  // Encender LEDs
  digitalWrite(ledGas, valorGas > umbralGas ? HIGH : LOW);
  digitalWrite(ledLuz, valorLuz > 500 ? HIGH : LOW);
  if (!isnan(temperatura)) {
    digitalWrite(ledTemp, temperatura > 24.0 ? HIGH : LOW);
  }
  digitalWrite(ledPIR, movimiento ? HIGH : LOW);

  // Crear objeto JSON
  StaticJsonDocument<200> doc;
  doc["gas"] = valorGas;
  doc["luz"] = valorLuz;
  if (!isnan(temperatura)) doc["temperatura"] = temperatura;
  if (!isnan(humedad)) doc["humedad"] = humedad;
  doc["movimiento"] = movimiento;
  doc["distancia"] = distance;

  // Enviar por Serial
  serializeJson(doc, Serial);
  Serial.println();  // Salto de línea para que Java lo lea correctamente

  delay(5000);  // Esperar 5 segundos
}
