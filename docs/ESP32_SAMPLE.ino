#include <WiFi.h>
#include <HTTPClient.h>

const char* ssid = "YOUR_WIFI_NAME";
const char* password = "YOUR_WIFI_PASSWORD";

const char* serverUrl = "http://YOUR_PC_IP:9090/api/sensors/ingest";
const char* deviceKey = "DEVICE-MAIN-001";

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }

  Serial.println("Connected.");
}

void loop() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(serverUrl);
    http.addHeader("Content-Type", "application/json");
    http.addHeader("X-DEVICE-KEY", deviceKey);

    int level = random(20, 95);
    float ph = random(65, 85) / 10.0;
    float temp = random(22, 36);

    String payload = "{";
    payload += "\"level\":" + String(level) + ",";
    payload += "\"ph\":" + String(ph, 1) + ",";
    payload += "\"temp\":" + String(temp, 1);
    payload += "}";

    int responseCode = http.POST(payload);
    Serial.print("POST response: ");
    Serial.println(responseCode);
    Serial.println(payload);

    http.end();
  }

  delay(15000);
}
