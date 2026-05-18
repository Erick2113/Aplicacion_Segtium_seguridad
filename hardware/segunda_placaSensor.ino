#include <ESP8266WiFi.h> 
#include <Firebase_ESP_Client.h>
#include <DHT.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include <time.h> 


//CREDENCIALES

#define WIFI_SSID "Redmi"
#define WIFI_PASSWORD "1252725w7dgs"
#define API_KEY "AIzaSyCAU-bvySintlM9gNwTlJN7ttw9S0UdcGg" 
#define DATABASE_URL "wearable-cloud-9bc89-default-rtdb.firebaseio.com" 


// CONFIGURACIÓN DE PINES 

#define DHTPIN D1        
#define DHTTYPE DHT11   
#define TILT_PIN D2      
#define BUTTON_PIN D5    
#define BUZZER_PIN D6    
#define LED_R D7        
#define LED_G D8        
#define LED_B D0        

DHT dht(DHTPIN, DHTTYPE);
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

unsigned long sendDataPrevMillis = 0;
unsigned long checkSettingsPrevMillis = 0; 
bool signupOK = false;


// VARIABLES DINÁMICAS

String nombreUsuario = "EMP-002"; // Se actualizara desde base

bool estadoAnteriorCaidaConfirmada = false;
bool estadoAnteriorPanico = false;
bool estadoAnteriorTempAlta = false; 

unsigned long tiempoInicioInclinacion = 0; 
bool posibleCaida = false;                 

const unsigned long TIEMPO_CONFIRMACION = 5000; 
bool bloqueoCaidaActivo = false;
unsigned long tiempoInicioBloqueo = 0;
const unsigned long TIEMPO_BLOQUEO = 30000; // Cooldown

bool simulacroLocal = false; 
float limiteTempFirebase = 38.0; 

─
// FUNCIÓN NTP para SSL de la base
void sincronizarHora() {
  configTime(-6 * 3600, 0, "pool.ntp.org", "time.google.com");
  Serial.print(F("Sincronizando hora"));
  time_t ahora = time(nullptr);
  int intentos = 0;
  while (ahora < 1700000000UL && intentos < 40) {
    delay(500); Serial.print(F("."));
    ahora = time(nullptr);
    intentos++;
  }
  Serial.println(F("\nHora Sincronizada"));
}

void setup() {
  Serial.begin(115200);
  dht.begin();
  
  pinMode(TILT_PIN, INPUT_PULLUP); 
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(LED_R, OUTPUT); pinMode(LED_G, OUTPUT); pinMode(LED_B, OUTPUT);

  digitalWrite(BUZZER_PIN, LOW); 

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) { delay(300); Serial.print(F(".")); }
  Serial.println(F("\n¡WiFi OK!"));

  sincronizarHora();

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  config.timeout.serverResponse = 15 * 1000;  

  fbdo.setBSSLBufferSize(4096, 1024);
  fbdo.setResponseSize(2048);

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  if (Firebase.ready() || Firebase.signUp(&config, &auth, "", "")) {
    signupOK = true;
    Serial.println(F("Firebase Listo"));
  }

  // Carga inicial del nombre del trabajador 002
  delay(2000);
  if (Firebase.ready()) {
    if (Firebase.RTDB.getString(&fbdo, "trabajadores/EMP-002/nombre")) {
      nombreUsuario = fbdo.stringData();
    }
  }
}

void loop() {
  float t = dht.readTemperature();
  float h = dht.readHumidity();
  bool inclinacionFisica = digitalRead(TILT_PIN) == LOW; 
  bool panicoPresionado  = digitalRead(BUTTON_PIN) == LOW; 

  // Lectura de ajustes cada 5 seg para ser reactivo al botón de Simulacro de Android
  if (Firebase.ready() && (millis() - checkSettingsPrevMillis > 5000)) {
    checkSettingsPrevMillis = millis();
    if (Firebase.RTDB.getBool(&fbdo, "trabajadores/EMP-002/simulacro")) simulacroLocal = fbdo.boolData();
    if (Firebase.RTDB.getFloat(&fbdo, "configuracion/limite_temp")) limiteTempFirebase = fbdo.floatData();
    // Refrescar nombre por si hubo cambios en la App
    if (Firebase.RTDB.getString(&fbdo, "trabajadores/EMP-002/nombre")) nombreUsuario = fbdo.stringData();
  }

  if (bloqueoCaidaActivo && millis() - tiempoInicioBloqueo >= TIEMPO_BLOQUEO) {
    bloqueoCaidaActivo = false;
  }

  // Lógica de Caída
  bool caidaConfirmada = false;
  if (!bloqueoCaidaActivo) {
    if (inclinacionFisica) {
      if (!posibleCaida) {
        posibleCaida = true;
        tiempoInicioInclinacion = millis();
      } else if (millis() - tiempoInicioInclinacion >= TIEMPO_CONFIRMACION) {
        caidaConfirmada = true; 
      }
    } else { posibleCaida = false; }
  }

  bool tempAltaActiva = (!isnan(t) && t > limiteTempFirebase);
  bool enviarAlertaInmediata = false;

  if (caidaConfirmada && !estadoAnteriorCaidaConfirmada) {
    enviarAlertaInmediata = true;
    bloqueoCaidaActivo = true;
    tiempoInicioBloqueo = millis();
  }
  if (panicoPresionado && !estadoAnteriorPanico)  enviarAlertaInmediata = true;
  if (tempAltaActiva   && !estadoAnteriorTempAlta) enviarAlertaInmediata = true;

  // Actuadores Locales
  if (caidaConfirmada || panicoPresionado || tempAltaActiva || simulacroLocal) {
    digitalWrite(BUZZER_PIN, HIGH); digitalWrite(LED_R, HIGH); digitalWrite(LED_G, LOW);
  } else {
    digitalWrite(BUZZER_PIN, LOW); digitalWrite(LED_R, LOW); digitalWrite(LED_G, HIGH);
  }

  // ENVÍO SINCRONIZADO 
  if (Firebase.ready() && signupOK && (millis() - sendDataPrevMillis > 10000 || enviarAlertaInmediata)) {
    sendDataPrevMillis = millis();
    
    FirebaseJson jsonLive;
    jsonLive.set("temperatura", String(t, 1)); 
    jsonLive.set("humedad", String(h, 1));     
    jsonLive.set("alerta_caida", caidaConfirmada); 
    jsonLive.set("boton_panico", panicoPresionado);
    jsonLive.set("estado_conexion", true);  
    jsonLive.set("bateria", "82%");

  
    FirebaseJson ts;
    ts.set(".sv", "timestamp"); 
    jsonLive.set("ultimo_latido", ts); 

    Firebase.RTDB.updateNode(&fbdo, "trabajadores/EMP-002", &jsonLive);

    if (enviarAlertaInmediata) {
      enviarHistorial(t, caidaConfirmada, panicoPresionado, tempAltaActiva);
    }
  }

  estadoAnteriorCaidaConfirmada = caidaConfirmada;
  estadoAnteriorPanico = panicoPresionado;
  estadoAnteriorTempAlta = tempAltaActiva;
}

void enviarHistorial(float t, bool c, bool p, bool ta) {
  FirebaseJson jh;
  jh.set("id_trabajador", "EMP-002");
  jh.set("nombre", nombreUsuario); // Nombre dinámico 
  jh.set("estado", "pendiente");
  
  String msg = "";
  if (c) msg += "Caída ";
  if (p) msg += "Botón Pánico ";
  if (ta) msg += "Temp Alta ";
  jh.set("tipo_emergencia", msg);

  FirebaseJson ts; ts.set(".sv", "timestamp");
  jh.set("timestamp", ts);

  Firebase.RTDB.pushJSON(&fbdo, "historial_emergencias", &jh);
  Serial.println(F("Historial EMP-002 Enviado"));
}