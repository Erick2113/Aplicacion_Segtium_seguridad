#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <DHT.h>
#include "soc/soc.h"
#include "soc/rtc_cntl_reg.h"
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"


// CREDENCIALES

#define WIFI_SSID "Redmi"
#define WIFI_PASSWORD "1252725w7dgs"
#define API_KEY "AIzaSyCAU-bvySintlM9gNwTlJN7ttw9S0UdcGg" 
#define DATABASE_URL "https://wearable-cloud-9bc89-default-rtdb.firebaseio.com" 


// 2. CONFIGURACIÓN DE PINES
#define DHTPIN 4        
#define DHTTYPE DHT11   
#define TILT_PIN 5      
#define BUTTON_PIN 16   
#define BUZZER_PIN 13   
#define LED_R 12        
#define LED_G 14        
#define LED_B 27        

DHT dht(DHTPIN, DHTTYPE);
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;




unsigned long sendDataPrevMillis = 0;
unsigned long checkSettingsPrevMillis = 0;
bool signupOK = false;


// VARIABLES DINÁMICAS

String miNombreActual = "EMP-001"; // Se actualizará desde Firebase
bool estadoAnteriorPanico = false;
bool estadoAnteriorCaida = false;
bool bloqueoCaidaActivo = false;
unsigned long tiempoInicioInclinacion = 0; 
bool posibleCaida = false; 


const unsigned long TIEMPO_CONFIRMACION_CAIDA = 3000; 
const unsigned long TIEMPO_BLOQUEO_CAIDA = 60000;    
const unsigned long INTERVALO_HEARTBEAT = 10000;    

float limiteTempFirebase = 38.0;
bool simulacroLocal = false;

void setup() {
  WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0); 
  Serial.begin(115200);
  
  dht.begin();
  pinMode(TILT_PIN, INPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(LED_R, OUTPUT); pinMode(LED_G, OUTPUT); pinMode(LED_B, OUTPUT);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) { delay(200); Serial.print("."); }
  Serial.println("\nWiFi OK");

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  config.timeout.serverResponse = 5 * 1000;

  if (Firebase.signUp(&config, &auth, "", "")) signupOK = true;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Carga inicial del nombre asignado en Firebase
  delay(2000);
  if (Firebase.ready()) {
    if (Firebase.RTDB.getString(&fbdo, "trabajadores/EMP-001/nombre")) {
      miNombreActual = fbdo.stringData();
    }
  }
}

void loop() {
  unsigned long currentMillis = millis();

  // LECTURA DE SENSORES
  float t = dht.readTemperature();
  float h = dht.readHumidity(); // Dato de humedad recuperado
  bool inclinacionFisica = digitalRead(TILT_PIN) == HIGH; 
  bool panicoPresionado = digitalRead(BUTTON_PIN) == LOW; 

  // LÓGICA DE CAÍDA
  bool caidaConfirmada = false;
  if (!bloqueoCaidaActivo) {
    if (inclinacionFisica) {
      if (!posibleCaida) {
        posibleCaida = true;
        tiempoInicioInclinacion = currentMillis;
      } else if (currentMillis - tiempoInicioInclinacion >= TIEMPO_CONFIRMACION_CAIDA) {
        caidaConfirmada = true; 
      }
    } else { posibleCaida = false; }
  }

  // DETECCIÓN DE ALERTAS
  bool tempAltaActiva = (!isnan(t) && t > limiteTempFirebase);
  bool hayEmergenciaActual = (caidaConfirmada || panicoPresionado || tempAltaActiva);
  
  bool triggerEnvio = false;
  if (caidaConfirmada && !estadoAnteriorCaida) triggerEnvio = true;
  if (panicoPresionado && !estadoAnteriorPanico) triggerEnvio = true;

  // ACTUADORES
  if (hayEmergenciaActual || simulacroLocal) {
    digitalWrite(BUZZER_PIN, HIGH);
    digitalWrite(LED_R, HIGH); digitalWrite(LED_G, LOW);
  } else {
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(LED_R, LOW); digitalWrite(LED_G, HIGH);
  }

  //  ENVÍO A FIREBASE
  if (Firebase.ready() && signupOK) {
    if (triggerEnvio || (currentMillis - sendDataPrevMillis >= INTERVALO_HEARTBEAT)) {
      sendDataPrevMillis = currentMillis;

      FirebaseJson json;
      json.set("temperatura", String(t, 1)); 
      json.set("humedad", String(h, 1)); // <--- Humedad enviada
      json.set("alerta_caida", caidaConfirmada);
      json.set("boton_panico", panicoPresionado);
      json.set("estado_conexion", true); 
      json.set("bateria", "92%");

      FirebaseJson ts; ts.set(".sv", "timestamp");
      json.set("ultimo_latido", ts); 

      Firebase.RTDB.updateNode(&fbdo, "trabajadores/EMP-001", &json);

      if (triggerEnvio) {
        enviarHistorial(t, caidaConfirmada, panicoPresionado, tempAltaActiva);
      }
    }
  }

  // SINCRONIZACIÓN DE AJUSTES 
  if (currentMillis - checkSettingsPrevMillis > 5000) {
    checkSettingsPrevMillis = currentMillis;
    if (Firebase.RTDB.getBool(&fbdo, "trabajadores/EMP-001/simulacro")) simulacroLocal = fbdo.boolData();
    if (Firebase.RTDB.getFloat(&fbdo, "configuracion/limite_temp")) limiteTempFirebase = fbdo.floatData();
    // Actualizar nombre dinámico por si cambió en el registro de Android
    if (Firebase.RTDB.getString(&fbdo, "trabajadores/EMP-001/nombre")) miNombreActual = fbdo.stringData();
  }

  estadoAnteriorPanico = panicoPresionado;
  estadoAnteriorCaida = caidaConfirmada;
}

void enviarHistorial(float t, bool c, bool p, bool ta) {
  FirebaseJson hist;
  hist.set("id_trabajador", "EMP-001");
  hist.set("nombre", miNombreActual); // Nombre dinámico
  hist.set("estado", "pendiente");
  
  String msg = "";
  if (c) msg += "Caída ";
  if (p) msg += "Botón Pánico ";
  if (ta) msg += "Temp Alta ";
  hist.set("tipo_emergencia", msg);

  FirebaseJson ts; ts.set(".sv", "timestamp");
  hist.set("timestamp", ts);

  Firebase.RTDB.pushJSON(&fbdo, "historial_emergencias", &hist);
  Serial.println("¡HISTORIAL ACTUALIZADO ENVIADO!");
}