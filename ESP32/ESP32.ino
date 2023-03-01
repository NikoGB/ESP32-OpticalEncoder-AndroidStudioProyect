/* 1.- LIBRERIAS Y VARIABLES*/
// Librarias para RIC DS1307
#include <RTClib.h>
#include <Wire.h>

// Librarias para Bluetooth Serial (256 bytes de buffer)
#include "BluetoothSerial.h"
// Librerias para tarjeta SD
#include "FS.h"
#include "SD.h"
#include <SPI.h>

#define SD_CS 5  // Se define el CS en el pin 5 y se nombra como SD_CS

BluetoothSerial SerialBT;
RTC_DS1307 rtc;
DateTime lastValidDate;

// Variables para el codificador rotativo
int counter = 0;  // Esta variable aunentará o disminuirá dependiendo de la rotación del codificador
int temp;
const float pi = 3.14159;
const float R = 1.905;
const int N = 1200;
float distance = 0;

const int BUFFER_SIZE = 4000;  // Tamaño del buffer para la comunicacion Bluetooth

/*
    el formato para las configuraciones es:
    "CONFIG;[nombre de la configuracion];[valor de la configuracion]"
*/
// Varibles del experimiento
int varTiempoMuestreo = 100;

/*
    el formato del mensaje para crear un agendamiento es:
    "AGENDAR;stDate;edDate;stTime;edTime;"
*/
// variables para el agendamiento
String nSchedule;
DateTime nextSchedule, endNextSchedule;

bool enAgendamiento = false;

// Variables de comunicacion
String message;
char *token;

// variables para almacenar el tiempo del temporizador
int timer = 0;
bool isTimer = false;

// variables para el manejo del loop de on
bool isOn = false;

// struct Date (){
//   uint32_t unixTime;
//   int year;
//   int month;
//   int day;
//   int hour;
//   int minute;
//   int second;


// }

/* 2.- SETUP*/
void setup() {
  // asignando valores iniciales a las variables de agendamiento y fecha para evitar errores
  nextSchedule = DateTime(2098, 12, 30, 0, 0, 0);
  endNextSchedule = DateTime(2098, 12, 30, 0, 0, 0);
  Serial.begin(115200);
  Wire.begin();

  if (!rtc.begin()) {
    Serial.println("No se pudo encontrar el RTC");
    Serial.flush();
  }

  if (!rtc.isrunning()) {
  // Serial.println("RIC NO se está ejecutando, ¡configurenos la hora!");
  // Cuando es necesario configurar la hora en un muevo dispositivo, o despues de una perdida de energia,
  // la siguiente linea establece el RIC en la fecha y hora que se compilo este codigo.
  rtc.adjust(DateTime(F(__DATE__), F(__TIME__)));
  lastValidDate = DateTime(F(__DATE__), F(__TIME__));
  } else{
    lastValidDate= rtc.now();
  }

  // Inicializar tarjeta SD
  SD.begin(SD_CS);
  if (!SD.begin(SD_CS)) {
    Serial.println("Falló el montaje de la tarjeta");
    return;
  }

  // almacena el tipo de sd
  uint8_t cardType = SD.cardType();
  if (cardType == CARD_NONE) {
    Serial.println("Sin tarjeta SD adjunta");
    return;
  }

  Serial.println("Inicializando tarjeta SD...");

  if (!SD.begin(SD_CS)) {
    Serial.println("ERROR - ¡Falló la inicialización de la tarjeta 5D!");
    return;  // init failed
  }

  // Si el archivo data.txt no existe
  // Cree un archivo en la tarjeta SD y escriba las etiquetas de datos
  File file = SD.open("/data.txt");
  if (!file) {
    Serial.println("El archivo no existe");
    Serial.println("Creando archivo. ..");
    writeFile(SD, "/data.txt", "Lectura de fecha, hora y contador");
  } else {
    Serial.println("El archivo ya existe");
  }

  file.close();

  File nFile = SD.open("/schedules.txt");
  if (!nFile) {
    Serial.println("El archivo no existe");
    Serial.println("Creando archivo. ..");
    writeFile(SD, "/schedules.txt", "");
  } else {
    Serial.println("El archivo ya existe");
  }

  nFile.close();

  pinMode(27, INPUT_PULLUP);  // Entrada PullUp interna pin 27
  pinMode(14, INPUT_PULLUP);  // Entrada PullUp interna pin 14

  // Configuración de interrupción (atrachInterrupt)
  // Balso ascendente de la fase A codificado activa a10()
  attachInterrupt(14, ai0, RISING);

  // Ba1so ascendente de la fase B codificado activa ail()
  attachInterrupt(27, ai1, RISING);

  // Comprobacion de interrupciones si hay error en los archivos de datos y agendamientos
  // checkData() retorna 0 si no hay error y si hay error retorna la cantidad de errores
  file = SD.open("data.txt");
  if (checkData("data.txt") < 0) {
    Serial.print("error en datos");
  }
  Serial.println("fechas de nextSchedule y endNextScheule");
  Serial.println(getDateString(nextSchedule));
  Serial.println(getDateString(endNextSchedule));
  loadSchedule();
  SerialBT.begin("ESP32");  // Mombre del dispositivo Bluetooth
  Serial.println("Dispositivo listo para su conexion");
}

/* 3.- FUNCIONES DEL SETUP */
void ai0() {
  if (digitalRead(27) == LOW) {
    counter++;
  } else {
    counter--;
  }
}

void ai1() {
  if (digitalRead(14) == LOW) {
    counter--;
  } else {
    counter++;
  }
}

// funcion para comprobar archivos en caso de interrupcion no llamar
// @param fileName nombre del archivo a comprobar
// @return 0 si no hay error, -1 si hay error
int checkData(String fileName) {
  Serial.println("Revisando datos");
  File file = SD.open("/" + fileName);
  // se obtiene el tamaño del archivo para revisar unicamente los ultimos 25 caracteres
  int fileSize = file.size();

  if (fileSize > 21) {
    file.seek(fileSize - 26);
  }
  // almacena el contenido del archivo en una variable
  String fileContent = "";
  while (file.available()) {
    fileContent += (char)file.read();
  }
  if (fileContent.isEmpty()) {
    return 1;
  }
  Serial.println(fileContent);

  file.close();
  int searchPos = 0;
  int errors = 0;
  // busca los pares de START; y STOP; en el archivo
  int end = fileContent.indexOf("STOP;", searchPos);
  // si no hay END; despues de START; hay un error
  if (end == -1) {
    // si hay un error escribir el stop en el archivo
    saveToSDCard(fileName, "STOP;" + dateNow() + ";#");
    errors++;
    return -1;
  }

  Serial.println("Revision finalizo");
  return 1;
}

/* 4.- FUNCIONES DEL SD CARD */

// funcion para guardar datos en el archivo especificado en la tarjeta SD
// @param file nombre del archivo
// @param toSave datos a guardar
// @return void
void saveToSDCard(String file, String toSave) {
  appendFile(SD, ("/" + file).c_str(), toSave.c_str());
}

// Escribir en la tarjeta SD
// @param fs tarjeta SD
// @param path ruta del archivo
// @param message mensaje a escribir
// @return void
void writeFile(fs::FS &fs, const char *path, const char *message) {
  Serial.printf("Escribiendo archivo: %s\n", path);
  File file = fs.open(path, FILE_WRITE);
  if (!file) {
    Serial.println("No se pudo abrir el archivo para escribir");
    return;
  }
  if (file.print(message)) {
    Serial.println("Archivo escrito");
  } else {
    Serial.println("Escritura fallida");
  }
  file.close();
}
// Anexar datos a la tarjeta SD
// @param fs tarjeta SD
// @param path ruta del archivo
// @param message mensaje a escribir
// @return void
void appendFile(fs::FS &fs, const char *path, const char *message) {
  //Serial.printf("Appending to file: %s\n", path);

  File file = fs.open(path, FILE_APPEND);
  if (!file) {
    Serial.println("Error al abrir el archivo para adjuntar");
    return;
  }
  if (file.print(message)) {
    //Serial.println("Mensaje adjunto");
  } else {
    Serial.println("Adjuntar fallo");
  }
  file.close();
}

// Funcion para enviar archivos por bluetooth serial pero considerando el tamaño del buffer (BUFFER_SIZE)
// @param nFile nombre del archivo a leer
// @return 1 si no hay error, -1 si hay error
int readBT(String nFile) {
  Serial.println("Enviando archivos");
  File file = SD.open("/" + nFile);
  if (!file) {
    Serial.println("El archivo no existe");
    return -1;
  }
  int total = 0;
  char buffer[BUFFER_SIZE];
  while (file.available()) {


    int byteRead = file.readBytes(buffer, BUFFER_SIZE);
    String chunk = buffer;
    // Serial.println(byteRead);
    chunk = chunk.substring(0, byteRead);
    // Serial.println(chunk);
    if (nFile.compareTo("schedule.txt")) {
      chunk.replace(',', ';');
      SerialBT.println(chunk);
    } else {
      SerialBT.println(chunk);
    }
    total++;
  }
  if (total == 0) {
    SerialBT.println("1");
  }

  file.close();
  SerialBT.println("*");
  Serial.println("Finalizo el envio");
  // delay para la funcion de check
  delay(100);
  return 1;
}

int sendData(char *tFecha) {
  String fecha = tFecha;
  Serial.println("Enviando datos desde: " + fecha);
  if (fecha.isEmpty()) {
    return -1;
  }
  File myFile = SD.open("/data.txt");
  int idx = -1, seekIdx = 0;
  if (myFile.size() > 1000) {
    seekIdx = myFile.size() - 1000;
  }

  char buffer[1100];
  while (myFile.available()) {
    if (seekIdx < 0) {
      seekIdx = 0;
    }
    myFile.seek(seekIdx);

    int bytesRead = myFile.readBytes(buffer, 1100);
    String data = buffer;
    data = data.substring(0, bytesRead);
    idx = data.lastIndexOf(";" + fecha + ";");


    Serial.println(data);
    if (idx > -1) {
      Serial.println(idx);


      myFile.seek(myFile.position() - (bytesRead - idx));
      bytesRead = 0;
      do {

        idx = 0;
        bytesRead = myFile.readBytes(buffer, 1100);
        data = buffer;
        data = data.substring(0, bytesRead);
        Serial.println("hola");
        Serial.println(data);
      } while ((idx = data.indexOf("START", idx)) < 0 && myFile.available());



      idx = myFile.position() - (bytesRead - idx);
      Serial.println(idx);

      break;
    }
    seekIdx -= 1000;
    if (seekIdx == -1000) {
      break;
    }
  }
  if (idx < 0) {
    SerialBT.println("*");
    return -1;
  }
  char buffer2[3000];
  myFile.seek(idx);
  Serial.println(idx);
  while (myFile.available()) {
    int bytesRead = myFile.readBytes(buffer2, 3000);
    String data = buffer2;
    data = data.substring(0, bytesRead);
    SerialBT.println(data);
    Serial.println(data);
  }
  Serial.println("hola3");
  SerialBT.println("*");
}



/* 5.- FUNCIONES DE LA LOGICA DEL ESP32 */

// funcion para comprobar el resultado una funcion y mandar un mensaje por bluetooth
// para que la aplicacion de android lo reciba y sepa que hacer 1 = OK, 0 = No se realizo ninguna accion, -1 = Error
// @param code codigo que retorna la funcion
// @return void
void check(int code) {
  // esta seccion es para el debug por el puerto serie
  if (code == -1) {
    Serial.println("Error");
  } else if (code == 1) {
    Serial.println("OK");
  } else if (code == 0) {
    Serial.println("No se realizo ninguna accion");
  }
  // delay para dejar que la app reciba el primer mensaje
  delay(1000);
  // siempre se manda un mensaje por bluetooth para indicar el resultado de la funcion
  // la applicacion de android se encarga de interpretar el mensaje (formato: <codigo>*)
  SerialBT.println(String(code) + "*");
  Serial.println(String(code) + "*");
}

// funcion para fortmatear la fecha y hora en un formato legible por el DateTime de la libreria RTClib
// @param void
// @return String fecha y hora en formato legible
String dateNow() {
  DateTime now = rtc.now();
  return String(now.year(), DEC) + "/" + String(now.month(), DEC) + "/" + String(now.day(), DEC) + "T" + String(now.hour(), DEC) + ":" + String(now.minute(), DEC) + ":" + String(now.second(), DEC);
}

// funcion para obtener la fecha y hora de un DateTime
// @param dat fecha y hora en formato DateTime
// @return String fecha y hora
String getDateString(DateTime dat) {
  return String(dat.year(), DEC) + "/" + String(dat.month(), DEC) + "/" + String(dat.day(), DEC) + "T" + String(dat.hour(), DEC) + ":" + String(dat.minute(), DEC) + ":" + String(dat.second(), DEC);
}

// funcion para cambiar los valores de las variables de configuracion (formato: "NOMBRE;VALOR")
// @param message mensaje con la configuracion a realizar
// @return 1 si se realizo la configuracion, 0 si no se encontro la configuracion, -1 si hubo un error
int config(char *message) {
  String msg = message;
  // se lee el valor de la configuracion y se asigna a la variable correspondiente
  String mensaje = message;
  Serial.println(msg);
}

// funcion que reinicia los valores de las variables de configuracion a los valores por defecto
// @param void
// @return 1 si se reinicio la configuracion, -1 si hubo un error
int reset() {
  varTiempoMuestreo = 100;
  return 1;
}

// funcion para crear agendamientos
// @param message mensaje con los datos del agendamiento
// @return 1 si se creo el agendamiento, -1 si hubo un error
int createAgendamiento(char *message) {
  // el formato de llegada del mensaje es: "Nombre,FechaInicio,FechaFin"
  // con las fechas con formato: "YYYY/MM/DDTHH:MM:SS"
  // se asigna el mensaje recibido a una variable String para poder usar la funcion substring
  String sc = message;
  // se crean variables para almacenar los valores de la fecha y hora de inicio y fin del agendamiento y el nombre del agendamiento
  String scStart = sc;
  String scEnd = sc;
  // se reemplazan los caracteres "/" por "-" para poder crear un objeto DateTime por ejemplo: // Prueba1,2023/1/18T18:10:0,2023/1/20T19:0:0
  scStart.replace("/", "-");  // Prueba1,2023-1-18T18:10:0,2023-1-20T19:0:0
  scEnd.replace("/", "-");
  // se obtienen los valores de la fecha y hora de inicio y fin del agendamiento y el nombre del agendamiento (formato: "Nombre,FechaInicio,FechaFin")
  scStart = scStart.substring(sc.indexOf(",") + 1, sc.length());  // Prueba1,2023-1-18T18:10:0,2023-1-20T19:0:0
  scEnd = scEnd.substring(sc.indexOf(",") + 1, sc.length());
  // se obtienen los valores de la fecha y hora de inicio y fin del agendamiento (formato: "FechaInicio,FechaFin" -> "YYYY-MM-DDTHH:MM:SS,YYYY-MM-DDTHH:MM:SS")
  scStart = scStart.substring(0, scStart.indexOf(","));  // 2023-1-18T18:10:0
  Serial.println("string start: " + scStart);
  int year = scStart.substring(0, scStart.indexOf("-")).toInt();
  int month = scStart.substring(scStart.indexOf("-") + 1, scStart.lastIndexOf("-")).toInt();
  int day = scStart.substring(scStart.lastIndexOf("-") + 1, scStart.indexOf("T")).toInt();
  int hour = scStart.substring(scStart.indexOf("T") + 1, scStart.indexOf(":")).toInt();
  int minute = scStart.substring(scStart.indexOf(":") + 1, scStart.lastIndexOf(":")).toInt();
  int second = scStart.substring(scStart.lastIndexOf(":") + 1, scStart.length() - 1).toInt();
  // se crea un objeto DateTime con los valores de la fecha y hora de inicio y fin del agendamiento
  DateTime newSchedule;
  newSchedule = DateTime(year, month, day, hour, minute, second);
  // se obtienen los valores de la fecha y hora de fin del agendamiento (formato: "FechaFin" -> "YYYY-MM-DDTHH:MM:SS")
  scEnd = scEnd.substring(scEnd.indexOf(",") + 1, scEnd.length());  // 2023-1-20T19:0:0
  Serial.println("string end: " + scEnd);

  year = scEnd.substring(0, scEnd.indexOf("-")).toInt();
  month = scEnd.substring(scEnd.indexOf("-") + 1, scEnd.lastIndexOf("-")).toInt();
  day = scEnd.substring(scEnd.lastIndexOf("-") + 1, scEnd.indexOf("T")).toInt();
  hour = scEnd.substring(scEnd.indexOf("T") + 1, scEnd.indexOf(":")).toInt();
  minute = scEnd.substring(scEnd.indexOf(":") + 1, scEnd.lastIndexOf(":")).toInt();
  second = scEnd.substring(scEnd.lastIndexOf(":") + 1, scEnd.length() - 1).toInt();
  // se crea un objeto DateTime con los valores de la fecha y hora de inicio y fin del agendamiento
  DateTime newEndSchedule;
  newEndSchedule = DateTime(year, month, day, hour, minute, second);
  nSchedule = nSchedule.substring(0, sc.indexOf(","));  // Prueba1

  Serial.println("newSchedule: " + getDateString(newSchedule));
  Serial.println("newEndSchedule: " + getDateString(newEndSchedule));

  // compara la fecha y hora de inicio del agendamiento con la fecha y hora de inicio del siguiente agendamiento
  if (nextSchedule.unixtime() > newSchedule.unixtime()) {
    // si la fecha y hora de inicio del agendamiento es menor a la fecha y hora de inicio del siguiente agendamiento
    // se asigna la fecha y hora de inicio del agendamiento a la fecha y hora de inicio del siguiente agendamiento
    nextSchedule = newSchedule;
    Serial.println(getDateString(newSchedule));
    // se asigna la fecha y hora de fin del agendamiento a la fecha y hora de fin del siguiente agendamiento
    endNextSchedule = newEndSchedule;
    Serial.println(getDateString(endNextSchedule));
    // se asigna el nombre del agendamiento a la variable nameSchedule
    Serial.println("schedule (sc): " + sc);
    nSchedule = sc.substring(0, sc.indexOf(","));
  }
  // se crea un string con el formato "Nombre,FechaInicio,FechaFin-"
  sc = sc + "-";
  // se guarda el string en el archivo "schedules.txt"
  saveToSDCard("schedules.txt", sc);
  Serial.println("hora actual" + dateNow());
  return 1;
}

// ! esta funcion solo es valida si los agendamientos se crean en orden cronologico
// funcion para cargar agendamientos del archivo "schedules.txt"
// @param void
// @return 1 si se cargo el agendamiento, -1 si hubo un error
int loadSchedule() {
  Serial.println("Cargando Schedule");
  File file = SD.open("/schedules.txt");
  if (!file) {
    Serial.println("Archivo schedules.txt no encontrado");
    return -1;
  }
  // se crea una variable String para almacenar un schedule a la vez
  String schedule = "";
  // se lee el archivo "schedules.txt" y se almacena en la variable String
  while (file.available()) {
    schedule = file.readStringUntil('-');
    if (!schedule.isEmpty()) {
      // se obtiene la fecha y hora de inicio del agendamiento
      Serial.println("revisando schedule:" + schedule);
      String sc = schedule;
      // se crean variables para almacenar los valores de la fecha y hora de inicio y fin del agendamiento y el nombre del agendamiento
      String scStart = sc;
      String scEnd = sc;
      // se reemplazan los caracteres "/" por "-" para poder crear un objeto DateTime por ejemplo: // Prueba1,2023/1/18T18:10:0,2023/1/20T19:0:0
      scStart.replace("/", "-");  // Prueba1,2023-1-18T18:10:0,2023-1-20T19:0:0
      scEnd.replace("/", "-");
      // se obtienen los valores de la fecha y hora de inicio y fin del agendamiento y el nombre del agendamiento (formato: "Nombre,FechaInicio,FechaFin")
      scStart = scStart.substring(sc.indexOf(",") + 1, sc.length());  // Prueba1,2023-1-18T18:10:0,2023-1-20T19:0:0
      scEnd = scEnd.substring(sc.indexOf(",") + 1, sc.length());
      // se obtienen los valores de la fecha y hora de inicio y fin del agendamiento (formato: "FechaInicio,FechaFin" -> "YYYY-MM-DDTHH:MM:SS,YYYY-MM-DDTHH:MM:SS")
      scStart = scStart.substring(0, scStart.indexOf(","));  // 2023-1-18T18:10:0
      Serial.println("string start: " + scStart);
      int year = scStart.substring(0, scStart.indexOf("-")).toInt();
      int month = scStart.substring(scStart.indexOf("-") + 1, scStart.lastIndexOf("-")).toInt();
      int day = scStart.substring(scStart.lastIndexOf("-") + 1, scStart.indexOf("T")).toInt();
      int hour = scStart.substring(scStart.indexOf("T") + 1, scStart.indexOf(":")).toInt();
      int minute = scStart.substring(scStart.indexOf(":") + 1, scStart.lastIndexOf(":")).toInt();
      int second = scStart.substring(scStart.lastIndexOf(":") + 1, scStart.length() - 1).toInt();
      // se crea un objeto DateTime con los valores de la fecha y hora de inicio y fin del agendamiento
      DateTime newSchedule;
      newSchedule = DateTime(year, month, day, hour, minute, second);
      // se obtienen los valores de la fecha y hora de fin del agendamiento (formato: "FechaFin" -> "YYYY-MM-DDTHH:MM:SS")
      scEnd = scEnd.substring(scEnd.indexOf(",") + 1, scEnd.length());  // 2023-1-20T19:0:0
      Serial.println("string end: " + scEnd);

      year = scEnd.substring(0, scEnd.indexOf("-")).toInt();
      month = scEnd.substring(scEnd.indexOf("-") + 1, scEnd.lastIndexOf("-")).toInt();
      day = scEnd.substring(scEnd.lastIndexOf("-") + 1, scEnd.indexOf("T")).toInt();
      hour = scEnd.substring(scEnd.indexOf("T") + 1, scEnd.indexOf(":")).toInt();
      minute = scEnd.substring(scEnd.indexOf(":") + 1, scEnd.lastIndexOf(":")).toInt();
      second = scEnd.substring(scEnd.lastIndexOf(":") + 1, scEnd.length() - 1).toInt();
      // se crea un objeto DateTime con los valores de la fecha y hora de inicio y fin del agendamiento
      DateTime newEndSchedule;
      newEndSchedule = DateTime(year, month, day, hour, minute, second);
      nSchedule = nSchedule.substring(0, sc.indexOf(","));  // Prueba1

      Serial.println("newSchedule: " + getDateString(newSchedule));
      Serial.println("newEndSchedule: " + getDateString(newEndSchedule));

      // verifica que la fecha de inicio no haya pasado (evita que los agendamientos terminados se ejecuten)
      if (newSchedule.unixtime() >= rtc.now().unixtime()) {
        // compara la fecha y hora de inicio del agendamiento con la fecha y hora de inicio del siguiente agendamiento
        if (nextSchedule.unixtime() > newSchedule.unixtime()) {
          // si la fecha y hora de inicio del agendamiento es menor a la fecha y hora de inicio del siguiente agendamiento
          // se asigna la fecha y hora de inicio del agendamiento a la fecha y hora de inicio del siguiente agendamiento
          nextSchedule = newSchedule;
          // se asigna la fecha y hora de fin del agendamiento a la fecha y hora de fin del siguiente agendamiento
          endNextSchedule = newEndSchedule;
          // se asigna el nombre del agendamiento a la variable nameSchedule
          nSchedule = schedule.substring(0, schedule.indexOf(","));
        }
      }
    }
  }
  // si no se encuentra un shcedule valido el nombre esta vacio y se reinician las variables
  if (nSchedule.isEmpty()) {
    nextSchedule = DateTime(2098, 12, 30, 0, 0, 0);
    endNextSchedule = DateTime(2098, 12, 30, 0, 0, 0);
  }
  file.close();

  Serial.println("fecha actual: " + dateNow());
  Serial.println("LoadSchedule finalizo, schedule cargado:" + nSchedule + getDateString(nextSchedule) + " " + getDateString(endNextSchedule));
  return 1;
}

// funcion para encender el muestreo de datos, enviarlos por bluetooth y guardarlos en el archivo "data.txt"
// @param void
// @return void
int on() {
  if (counter != temp) {
    distance = ((2 * pi * R) / N) * counter;
    Serial.println();  // creo que no es necessario por el println de abajo
    temp = counter;
  }
  // Se envia el valor de la distancia al dispositivo conectado por bluetooth con el formato de "!distancia"
  String d = "!" + String(distance, DEC);
  d = d.substring(0, 5);
  SerialBT.println(d + "*");  // ! el asterisco para que la reciba el mensaje la app
  Serial.println(d);
  // se guarda el valor de la distancia en el archivo "data.txt"
  saveToSDCard("data.txt", d);
}

// funcion para manejar el temporizador
// @param message mensaje recibido por bluetooth con el formato "TIMER;TIEMPO;"
// @return 1 si se activo el temporizador, -1 si hubo un error
int funcTimer(char *message) {
  // iterar el mensaje para obtener el valor del tiempo formato: "TIMER;TIEMPO;"
  // el TIEMPO viene en formato mm:ss:ms => 00:00:000
  // se obtiene el valor de los minutos
  char *m = strtok(NULL, ":");
  // se obtiene el valor de los segundos
  char *s = strtok(NULL, ":");
  // se obtiene el valor de los milisegundos
  char *ms = strtok(NULL, ";");
  // se asigna el valor del tiempo en milisegundos al int timer
  timer = (atoi(m) * 60000) + (atoi(s) * 1000) + atoi(ms);
  // se comprueba si el tiempo no es 0
  if (timer == 0) {
    return -1;
  }
  // se activa el bool isTimer
  isTimer = true;
  // se debe crear un string con el formato "START;;FECHAINICIO;TIEMPO;!DISTANCIA;FECHAFIN;STOP;"
  // y se guarda en el archivo "data.txt"
  String dat = "START;;" + dateNow() + ";" + String(varTiempoMuestreo, DEC) + ";" + "cm" + ";";
  saveToSDCard("data.txt", dat);

  return 1;
}

// funcion para elimianr un agendamiento
// @param message mensaje recibido por bluetooth con el formato "DELETE;NOMBRE;"
// @return 1 si se elimino el agendamiento, -1 si hubo un error, 0 si no se encontro el agendamiento
int deleteSchedule(char *message) {
  // se obtiene el nombre del agendamiento
  char *n = strtok(NULL, ";");
  // si el agendamiento a eliminar es el actual
  if (strcmp(nSchedule.c_str(), n) == 0) {

    if (isOn) {
      // isOn se asigna el valor de false para que no se ejecute el muestreo
      isOn = false;
      // enAgendamiento se asigna el valor de false para que no se termine el muestreo si no se ha iniciado un agendamiento
      enAgendamiento = false;
      // se crea un string con el formato "STOP;FechaHoraDeTermino"
      String dat = "STOP;" + dateNow() + ";";
      // se guarda el string en el archivo "data.txt"
      saveToSDCard("data.txt", dat);
    }
    // // se manda el data.txt por bluetooth al terminar el agendamiento
    // // readBT("data.txt");
    // // delay(100);
    // // se carga el siguiente agendamiento
    // loadSchedule();
    // return 1;
    // eliminar el nombre para que no se ejecute y reiniciando las variables del agendamiento
    nSchedule = "";
    nextSchedule = DateTime(2098, 12, 30, 0, 0, 0);
    endNextSchedule = DateTime(2098, 12, 30, 0, 0, 0);
  }
  // se crea un objeto File con el archivo "schedules.txt"
  File file = SD.open("/schedules.txt");
  // se crea un archivo temporal para guardar los agendamientos
  File temp = SD.open("/temp.txt", FILE_WRITE);
  bool found = false;
  // se lee el archivo "schedules.txt" hasta el final
  while (file.available()) {
    // se lee una linea del archivo "schedules.txt"
    String line = file.readStringUntil('-');
    if (line.indexOf(n) == -1) {
      temp.print(line + "-");
    } else {
      found = true;
    }
  }
  // se cierra el archivo "schedules.txt"
  file.close();
  // se cierra el archivo temporal
  temp.close();
  if (!found) {
    SD.remove("/temp.txt");
    return 0;
  }
  // si exite el agendamiento se elimina el archivo "schedules.txt"
  SD.remove("/schedules.txt");
  // se renombra el archivo temporal como "schedules.txt"
  SD.rename("/temp.txt", "/schedules.txt");
  // loadSchedule para que se revisen devuelta los agendamientos
  loadSchedule();
  // delay por si las dudas
  return 1;
}

int fallos = 0;
int total = 0;
bool fallo = false;



bool endSchedule, startSchedule;
/* 6.- LOOP */
// funcion que admistra el funcionamiento del dispositivo
void loop() {

  DateTime now = rtc.now();

  uint32_t diff = now.unixtime() - lastValidDate.unixtime();
  // si la diferencia es > 0 signifca que la fecha actual es mayor y se tiene que teminar el schedule
  // uint32_t endScheduleDiff = now.unixtime() - endNextSchedule.unixtime();

  Serial.println("last valid: " + getDateString(lastValidDate) + " rtc:" + getDateString(now) + " endSchedule bool: " + String(endSchedule));
  if (diff < 60 && diff >= 0) {
    lastValidDate = now;
  } else {
    Serial.println("La fecha invalida es: " + getDateString(now));
    fallos++;
    rtc.adjust(lastValidDate);
    now = rtc.now();
    Serial.println("La fecha fue reajustada a: " + getDateString(now));
  }
  if (now.unixtime() >= endNextSchedule.unixtime()) {
    endSchedule = true;
  } else {
    endSchedule = false;
  }

  // si estamos en un temporizador se ejecuta el siguiente código
  if (isTimer) {
    // se llama al on() para encender el muestreo
    on();
    // se espera el tiempo de muestreo
    delay(varTiempoMuestreo);
    // se resta el tiempo de muestreo al tiempo restante del temporizador
    timer = timer - varTiempoMuestreo;
    // si el tiempo restante del temporizador es menor o igual a 0 se ejecuta el siguiente código
    if (timer <= 0) {
      isTimer = false;
      // ! no hace falta guardar el stop porque la app manda un OFF; para terminar el muestreo
    }
  }
  // si la variable isOn es true se ejecuta el siguiente código
  if (isOn) {
    total += 1;
    // Serial.println("fecha actual (loop):" + getDateString(now));

    // se llama a la funcion on() para encender el muestreo
    on();
    // se espera el tiempo de muestreo
    delay(varTiempoMuestreo);
    // se comprueba si la fecha y hora de fin del agendamiento es menor o igual a la fecha y hora actual
    // y si la variable enAgendamiento es true (para que no se termine el muestreo si no se ha iniciado un agendamiento)


    Serial.println("Fecha antes de comprobar: " + getDateString(now) + " en unix: " + now.unixtime());
    if (enAgendamiento && endSchedule) {
      Serial.println("fecha actual de termino (loop/off):" + getDateString(now) + "en Unix: " + now.unixtime());
      Serial.println("fecha de termino del agendamiento: " + getDateString(endNextSchedule) + "En Unix: " + endNextSchedule.unixtime());
      Serial.print("Porcentaje de fallos del rtc: ");
      Serial.print(fallos);
      Serial.print("/");
      Serial.println(total);
      if (fallo) {
        Serial.println(" se re ajusto el rtc durante el agendamiento");
        fallo = false;
      }

      // isOn se asigna el valor de false para que no se ejecute el muestreo
      isOn = false;
      // enAgendamiento se asigna el valor de false para que no se termine el muestreo si no se ha iniciado un agendamiento
      enAgendamiento = false;
      // se crea un string con el formato "STOP;FechaHoraDeTermino"
      String dat = "STOP;" + dateNow() + ";";
      // TODO: probar esta wea de aqui porque creo que se cargan 2 veces los agendamientos porque no se reinician los valores
      // se reinician los valores de schedule para cargar correctamente el siguiente
      nSchedule = "";
      nextSchedule = DateTime(2098, 12, 30, 0, 0, 0);
      endNextSchedule = DateTime(2098, 12, 30, 0, 0, 0);
      // se guarda el string en el archivo "data.txt"
      saveToSDCard("data.txt", dat);
      delay(100);
      SerialBT.println("scheduleStop*");
      delay(1000);
      SerialBT.println("1*");
      delay(1000);
      // se manda el data.txt por bluetooth al terminar el agendamiento
      readBT("data.txt");
      delay(1000);
      // se carga el siguiente agendamiento
      loadSchedule();
    }
  } else {
    delay(100);
  }

  // si la diferencia es > 0 signifca que la fecha actual es mayor y se tiene que empezar el shedule
  uint32_t nextScheduleDiff = now.unixtime() - nextSchedule.unixtime();
  if (now.unixtime() >= nextSchedule.unixtime()) {
    startSchedule = true;
  } else {
    startSchedule = false;
  }
  // se comprueba si el siguiente agendamiento es menor o igual a la fecha y hora actual (se inicia el agendamiento)
  if (startSchedule && !nSchedule.isEmpty()) {
    Serial.println("Se empezo el agendamiento: " + nSchedule);
    Serial.println("con nextSchedule: " + getDateString(nextSchedule));
    Serial.println("con endNextSchedule: " + getDateString(endNextSchedule));

    // se asigna la fecha y hora de inicio del siguiente agendamiento un valor muy alto para que no se cumpla la condicion
    nextSchedule = DateTime(2098, 12, 30, 0, 0, 0);  // esto esta de antes, en el if se comprueba si se apaga
    if (isOn) {
      // se crea un string con el formato "STOP;FechaHoraDeTermino"
      String dat = "STOP;" + dateNow() + ";";
      // se guarda el string en el archivo "data.txt"
      saveToSDCard("data.txt", dat);
      SerialBT.println("sAbort*");
      delay(3000);
      // se manda el data.txt por bluetooth al terminar el agendamiento
      readBT("data.txt");
      delay(3000);
    }
    // se crea un string con el formato "START;Nombre;FechaHoraDeInicio;TiempoDeMuestreo;UnidadDeMedida;"
    String dat = "START;" + nSchedule + ";" + dateNow() + ";" + String(varTiempoMuestreo, DEC) + ";" + "cm" + ";";
    // se guarda el string en el archivo "data.txt" y se inicia el muestreo
    saveToSDCard("data.txt", dat);
    // se asigna el valor de la variable isOn a true para que se inicie el muestreo
    isOn = true;
    // guarda el inicio del agendamiento en data y empieza a tomar la data
    enAgendamiento = true;

    // se manda un mensaje por bluetooth para indicar que se inicio el agendamiento (no estoy seguro sobre el c_str())
    SerialBT.println("scheduleStart*");
    delay(1000);
  }


  // Si se recibe un mensaje por Bluetooth (SerialBT) se ejecuta el siguiente código
  if (SerialBT.available()) {
    // Se lee el mensaje y se almacena en la variable message
    message = SerialBT.readString();
    // Se imprime el mensaje en el monitor serial
    // Se divide el mensaje en un token
    token = strtok(&message[0], ";");
    // Se compara el primer token con las palabras reservadas
    if (strcmp(token, "CONFIG") == 0) {
      token = strtok(NULL, ";");

      if (strcmp(token, "TIEMPOMUESTREO") == 0) {
        token = strtok(NULL, ";");
        String valor = token;
        if (atoi(valor.c_str()) >= 100) {
          varTiempoMuestreo = atoi(valor.c_str());
          SerialBT.println("1*");

        } else {
          // mensaje de error por tiempo de muestreo muy corto
          SerialBT.println("-1*");
        }
      } else {

        // si no se encuentra la configuracion se retorna 0
        SerialBT.println("0*");
      }
    } else if (strcmp(token, "SCAN") == 0) {
      token = strtok(NULL, ";");
      if (strcmp(token, "GET") == 0 && !isOn) {  // MANDAR DATA.txt
        token = strtok(NULL, ";");
        String t = token;
        Serial.println("token: " + t);
        delay(1000);
        if (strcmp(token, "-") == 0) {
          readBT("data.txt");
        } else {
          sendData(token);
        }

        delay(100);
      }
    } else if (strcmp(token, "SCHEDULE") == 0) {
      token = strtok(NULL, ";");

      if (strcmp(token, "ADD") == 0) {
        token = strtok(NULL, ";");
        check(createAgendamiento(token));
        delay(100);
      } else if (strcmp(token, "DELETE") == 0) {
        check(deleteSchedule(token));
        delay(100);
      } else if (!isOn) {  // MANDAR SCHEDULES.txt
        readBT("schedules.txt");
        delay(100);
      }
    } else if (strcmp(token, "AGENDAR") == 0) {
      check(createAgendamiento(&message[0]));
    } else if (strcmp(token, "ON") == 0) {
      // el formato de encendido en el archivo es START;;TiempoDeMuestreo;UnidadDeMedida;
      isOn = true;
      String dat = "START;;" + dateNow() + ";" + String(varTiempoMuestreo, DEC) + ";" + "cm" + ";";
      saveToSDCard("data.txt", dat);
      delay(500);
      SerialBT.println("1*");
      delay(500);
    } else if (strcmp(token, "TIMER") == 0) {
      // el formato de encendido en el archivo es START;;TiempoDeMuestreo;UnidadDeMedida;
      check(funcTimer(&message[0]));
      // manda el archivo cuando se acaba el temporizador
      delay(100);
    } else if (strcmp(token, "OFF") == 0) {
      isOn = false;
      isTimer = false;
      String dat = "STOP;" + dateNow() + ";";
      saveToSDCard("data.txt", dat);
      SerialBT.println("1*");
      delay(100);
      // se carga el siguiente agendamiento
      // loadSchedule();
    } else if (strcmp(token, "READ") == 0) {
      // el formato de lectura es READ; y lee el archivo data.txt
      check(readBT("data.txt"));
    } else if (strcmp(token, "RESET") == 0) {
      // el formato de RESET es RESET;
      check(reset());
    } else {
      Serial.println("Error en el mensaje");
      SerialBT.print("-1*");  // PUEDE SER PRINTLN
      // delay(100);
    }
    // Se limpia la variable message
    message = "";
  }
}
