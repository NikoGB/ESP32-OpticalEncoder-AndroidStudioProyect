/* 1.- LIBRERIAS Y VARIABLES*/
// Librarias para RIC DS1307
#include "RTClib.h"

// Librarias para Bluetooth Serial (256 bytes de buffer)
#include "BluetoothSerial.h"
// Librerias para tarjeta SD
#include "FS.h"
#include "SD.h"
#include <SPI.h>

#define SD_CS 5 // Se define el CS en el pin 5 y se nombra como SD_CS

BluetoothSerial SerialBT;

RTC_DS1307 rtc;

char DiaDeLaSemana[7][12] = {"Domingo", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado"};

// Variables para el codificador rotativo
int counter = 0; // Esta variable aunentará o disminuirá dependiendo de la rotación del codificador
int temp;
const float pi = 3.14159;
const float R = 1.905;
const int N = 1200;
float distance = 0;

const int BUFFER_SIZE = 256; // Tamaño del buffer para la comunicacion Bluetooth

/*
    el formato para las configuraciones es:
    "CONFIG;[nombre de la configuracion];[valor de la configuracion]"
*/
// Varibles del experimiento
int varTiempoMuestreo = 100; // Tiempo de muestreo en milisegundos (TIEMPOMUESTREO)

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

/* 2.- SETUP*/
void setup()
{
    // asignando valores iniciales a las variables de agendamiento y fecha para evitar errores
    nextSchedule = DateTime("9999:12:30", "10:10:10");
    endNextSchedule = DateTime("9999:12:30", "10:10:10");
    Serial.begin(115200);
    SerialBT.begin("ESP32Prueba"); // Mombre del dispositivo Bluetooth
    Serial.println("El dispositivo inicio, ¡ahora puedes emparejarlo con bluetooth!");

    if (!rtc.begin())
    {
        Serial.println("No se pudo encontrar el RTC");
        Serial.flush();
        while (1)
            delay(10);
    }
    if (!rtc.isrunning())
    {
        Serial.println("RIC NO se está ejecutando, ¡configurenos la hora!");
        // Cuando es necesario configurar la hora en un muevo dispositivo, o despues de una perdida de energia,
        // la siguiente linea establece el RIC en la fecha y hora que se compilo este codigo.
        // TODO: check how to set the time to the current time (bug in the library? time was off by a minute or so)
        rtc.adjust(DateTime(F(__DATE__), F(__TIME__)));
        // Esta línea establece el RIC con una fecha y hora explícitas, por ejemplo, para establecer
        // 21 de Junio de 2022 a las 3Pm.
    }

    // Inicializar tarjeta SD
    SD.begin(SD_CS);
    if (!SD.begin(SD_CS))
    {
        Serial.println("Falló el montaje de la tarjeta");
        return;
    }

    // almacena el tipo de sd
    uint8_t cardType = SD.cardType();

    if (cardType == CARD_NONE)
    {
        Serial.println("Sin tarjeta SD adjunta");
        return;
    }

    Serial.println("Inicializando tarjeta SD...");

    if (!SD.begin(SD_CS))
    {
        Serial.println("ERROR - ¡Falló la inicialización de la tarjeta 5D!");
        return; // init failed
    }

    // Si el archivo data.txt no existe
    // Cree un archivo en la tarjeta SD y escriba las etiquetas de datos
    File file = SD.open("/data.txt");
    if (!file)
    {
        Serial.println("El archivo no existe");
        Serial.println("Creando archivo. ..");
        writeFile(SD, "/data.txt", "Lectura de fecha, hora y contador");
    }
    else
    {
        Serial.println("El archivo ya existe");
    }

    file.close();

    file = SD.open("/schedules.txt");
    if (!file)
    {
        Serial.println("El archivo no existe");
        Serial.println("Creando archivo. ..");
        writeFile(SD, "/schedules.txt", "");
    }
    else
    {
        Serial.println("El archivo ya existe");
    }

    file.close();

    pinMode(27, INPUT_PULLUP); // Entrada PullUp interna pin 27
    pinMode(14, INPUT_PULLUP); // Entrada PullUp interna pin 14

    // Configuración de interrupción (atrachInterrupt)
    // Balso ascendente de la fase A codificado activa a10()
    attachInterrupt(14, ai0, RISING);

    // Ba1so ascendente de la fase B codificado activa ail()
    attachInterrupt(27, ai1, RISING);

    // Comprobacion de interrupciones si hay error en los archivos de datos y agendamientos
    // checkData() retorna 0 si no hay error y si hay error retorna la cantidad de errores
    File file = SD.open("/data.txt");
    if (file)
    {
        if (checkData("data.txt") > 0)
        {
            Serial.println("Error al leer los archivos");
        }
    }
}

/* 3.- FUNCIONES DEL SETUP */
void ai0()
{
    if (digitalRead(27) == LOW)
    {
        counter++;
    }
    else
    {
        counter--;
    }
}

void ai1()
{
    if (digitalRead(14) == LOW)
    {
        counter--;
    }
    else
    {
        counter++;
    }
}

// funcion para comprobar archivos en caso de interrupcion
int checkData(fileName)
{
    File file = SD.open("/" + fileName);
    if (!file)
    {
        Serial.println("El archivo no existe");
        return -1;
    }
    // almacena el contenido del archivo en una variable
    String fileContent = "";
    while (file.available())
    {
        fileContent += (char)file.read();
    }
    file.close();
    int searchPos = 0;
    int errors = 0;
    // busca los pares de START; y STOP; en el archivo
    while (fileContent.indexOf("START;", searchPos) != -1)
    {
        int start = fileContent.indexOf("START;", searchPos);
        int end = fileContent.indexOf("STOP;", searchPos);
        // si no hay END; despues de START; hay un error
        if (end == -1)
        {
            // si hay un error escribir el stop en el archivo
            saveToSDCard(fileName, "STOP;" + +";" + dateNow() + ";#");
            errors++;
        }
    }

    return 1;
}

/* 4.- FUNCIONES DEL SD CARD */

// funcion para guardar datos en el archivo especificado en la tarjeta SD
void saveToSDCard(String file, String toSave)
{
    appendFile(SD, ("/" + file).c_str(), toSave.c_str());
}

// Escribir en la tarjeta SD
void writeFile(fs::FS &fs, const char *path, const char *message)
{
    Serial.printf("Escribiendo archivo: %s\n", path);

    File file = fs.open(path, FILE_WRITE);

    if (!file)
    {
        Serial.println("No se pudo abrir el archivo para escribir");
        return;
    }

    if (file.print(message))
    {
        Serial.println("Archivo escrito");
    }
    else
    {
        Serial.println("Escritura fallida");
    }

    file.close();
}
// Anexar datos a la tarjeta SD
void appendFile(fs::FS &fs, const char *path, const char *message)
{
    Serial.printf("Appending to file: %s\n", path);

    File file = fs.open(path, FILE_APPEND);
    if (!file)
    {
        Serial.println("Error al abrir el archivo para adjuntar");
        return;
    }
    if (file.print(message))
    {
        Serial.println("Mensaje adjunto");
    }
    else
    {
        Serial.println("Adjuntar fallo");
    }
    file.close();
}

// // funcion para leer el archivo especificado
// int read(String nFile)
// {
//     // TODO: probar con SD en vez de fs
//     File file = fs.open("/" + nFile, FILE_READ);
//     if (!file)
//     {
//         Serial.println("El archivo no existe");
//         return -1;
//     }
//     else
//     {
//         // se manda un mensaje por bluetooth para indicar que el archivo existe y se va a enviar mediante bluetooth (SerialBT)
//         String toSend = "";
//         while (file.available())
//         {
//             toSend += (char)file.read();
//         }
//         Serial.println(toSend);
//         SerialBT.println(toSend + "*");
//         file.close();
//         return 1;
//     }
// }

// funcion de read pero considerando el limite de 256 byte del BT (SerialBT)
// Cambiar nombre a read() y eliminar la de arriba en caso de que no se necesite
int readBT(String nFile)
{
    File file = SD.open("/" + nFile);
    if (!file)
    {
        Serial.println("El archivo no existe");
        return -1;
    }
    while (file.available())
    {
        String chunk = "";
        int byteRead = 0;
        while (byteRead < BUFFER_SIZE && file.available())
        {
            chunk += (char)file.read();
            byteRead++;
        }
        SerialBT.println(chunk);
        Serial.println(chunk);
    }
    file.close();
    SerialBT.println("*");
    // delay para la funcion de check
    delay(100);
    return 1;
}

/* 5.- FUNCIONES DE LA LOGICA DEL ESP32 */

// funcion para comprobar el resultado una funcion
// si el resultado es -1 se manda un mensaje por bluetooth para indicar que hubo un error
// si el resultado es 1 se manda un mensaje por bluetooth para indicar que no hubo error
// si el resultado es 0 se manda un mensaje por bluetooth para indicar que no surgieron efectos
void check(int code)
{
    // esta seccion es para el debug por el puerto serie
    if (code == -1)
    {
        Serial.println("Error");
    }
    else if (code == 1)
    {
        Serial.println("OK");
    }
    else if (code == 0)
    {
        Serial.println("No se realizo ninguna accion");
    }
    // siempre se manda un mensaje por bluetooth para indicar el resultado de la funcion
    // la applicacion de android se encarga de interpretar el mensaje (formato: <codigo>*)
    SerialBT.println(String(code) + "*");
}

// funcion para fortmatear la fecha y hora en un formato legible por el DateTime de la libreria RTClib
String dateNow()
{
    DateTime now = rtc.now();
    return String(now.year(), DEC) + "/" + String(now.month(), DEC) + "/" + String(now.day(), DEC) + "T" + String(now.hour(), DEC) + ":" + String(now.minute(), DEC) + ":" + String(now.second(), DEC) + ":" + "0";
}

// // funcion para obtener la fecha y hora de un DateTime
// String getDateString(DateTime dat)
// {
//     return String(dat.year(), DEC) + "/" + String(dat.month(), DEC) + "/" + String(dat.day(), DEC) + "T" + String(dat.hour(), DEC) + ":" + String(dat.minute(), DEC) + ":" + String(dat.second(), DEC) + ":" + "0";
// }

// funcion para cambiar los valores de las variables de configuracion (formato: "NOMBRE;VALOR")
int config(char *message)
{
    // se lee la configuracion a realizar y se almacena en configName
    char *configName = strtok(message, ";");
    // se lee el valor de la configuracion y se almacena en configValue
    char *configValue = strtok(NULL, ";");
    // se lee el valor de la configuracion y se asigna a la variable correspondiente
    if (strcmp(configName, "TIEMPOMUESTREO") == 0)
    {
        if (atoi(configValue) >= 100)
        {
            varTiempoMuestreo = atoi(configValue);
            return 1;
        }
        else
        {
            // mensaje de error por tiempo de muestreo muy corto
            return -1;
        }
    }
}

// funcion que reinicia los valores de las variables de configuracion a los valores por defecto
int reset()
{
    varTiempoMuestreo = 100;
    return 1;
}

// funcion para crear agendamientos
int createAgendamiento(char *message)
{
    // el formato de llegada del mensaje es: "Nombre,FechaInicio,FechaFin"
    // con las fechas con formato: "YYYY/MM/DDTHH:MM:SS"
    // se asigna el mensaje recibido a una variable String para poder usar la funcion substring
    String sc = message;
    // se crean variables para almacenar los valores de la fecha y hora de inicio y fin del agendamiento y el nombre del agendamiento
    String scStart = sc;
    String scEnd = sc;
    String nSchedule = sc;
    // se reemplazan los caracteres "/" por "-" para poder crear un objeto DateTime por ejemplo: // Prueba1,2023/1/18T18:10:0,2023/1/20T19:0:0
    scStart.replace("/", "-"); // Prueba1,2023-1-18T18:10:0,2023-1-20T19:0:0
    scEnd.replace("/", "-");
    // se obtienen los valores de la fecha y hora de inicio y fin del agendamiento y el nombre del agendamiento (formato: "Nombre,FechaInicio,FechaFin")
    scStart = scStart.substring(sc.indexOf(",") + 1, sc.length()); // Prueba1,2023-1-18T18:10:0,2023-1-20T19:0:0
    scEnd = scEnd.substring(sc.indexOf(",") + 1, sc.length());
    // se obtienen los valores de la fecha y hora de inicio y fin del agendamiento (formato: "FechaInicio,FechaFin" -> "YYYY-MM-DDTHH:MM:SS,YYYY-MM-DDTHH:MM:SS")
    scStart = scStart.substring(0, scStart.indexOf(",") - 2); // 2023-1-18T18:10:0
    Serial.println("string start: " + scStart);
    // se obtienen los valores de la fecha y hora de fin del agendamiento (formato: "FechaFin" -> "YYYY-MM-DDTHH:MM:SS")
    scEnd = scEnd.substring(scEnd.indexOf(",") + 1, scEnd.length() - 2); // 2023-1-20T19:0:0
    Serial.println("string end: " + scEnd);
    nSchedule = nSchedule.substring(0, sc.indexOf(",")); // Prueba1
    // se crea un objeto DateTime con los valores de la fecha y hora de inicio y fin del agendamiento
    DateTime newSchedule(scStart.c_str());
    DateTime newEndSchedule(scEnd.c_str());
    // compara la fecha y hora de inicio del agendamiento con la fecha y hora de inicio del siguiente agendamiento
    if (nextSchedule > newSchedule)
    {
        // si la fecha y hora de inicio del agendamiento es menor a la fecha y hora de inicio del siguiente agendamiento
        // se asigna la fecha y hora de inicio del agendamiento a la fecha y hora de inicio del siguiente agendamiento
        nextSchedule = newSchedule;
        // se asigna la fecha y hora de fin del agendamiento a la fecha y hora de fin del siguiente agendamiento
        endNextSchedule = newEndSchedule;
    }
    // se crea un string con el formato "Nombre,FechaInicio,FechaFin-"
    sc = sc + "-";
    // se guarda el string en el archivo "schedules.txt"
    saveToSDCard("schedules.txt", sc);
    return 1;
}

// funcion para cargar agendamientos del archivo "schedules.txt"
int loadSchedule()
{
    File file = SD.open("schedules.txt");
    if (!file)
    {
        return -1;
    }
    // se crea una variable String para almacenar el contenido del archivo "schedules.txt"
    String schedule = "";
    // se lee el archivo "schedules.txt" y se almacena en la variable String
    while (file.available())
    {
        schedule += (char)file.read();
    }
    file.close();
    // se busca el agendamiento que tenga la fecha y hora de inicio mas cercana a la fecha y hora actual
    while (schedule.indexOf("-") != -1)
    {
        // se obtiene el nombre del agendamiento
        String nSchedule = schedule.substring(0, schedule.indexOf(","));
        // se obtiene la fecha y hora de inicio del agendamiento
        String scStart = schedule.substring(schedule.indexOf(",") + 1, schedule.length());
        scStart = scStart.substring(0, scStart.indexOf(",") - 2);
        // se obtiene la fecha y hora de fin del agendamiento
        String scEnd = schedule.substring(schedule.indexOf(",") + 1, schedule.length());
        scEnd = scEnd.substring(scEnd.indexOf(",") + 1, scEnd.length() - 2);
        // se crea un objeto DateTime con los valores de la fecha y hora de inicio y fin del agendamiento
        DateTime newSchedule(scStart.c_str());
        DateTime newEndSchedule(scEnd.c_str());
        // compara la fecha y hora de inicio del agendamiento con la fecha y hora de inicio del siguiente agendamiento
        if (nextSchedule > newSchedule)
        {
            // si la fecha y hora de inicio del agendamiento es menor a la fecha y hora de inicio del siguiente agendamiento
            // se asigna la fecha y hora de inicio del agendamiento a la fecha y hora de inicio del siguiente agendamiento
            nextSchedule = newSchedule;
            // se asigna la fecha y hora de fin del agendamiento a la fecha y hora de fin del siguiente agendamiento
            endNextSchedule = newEndSchedule;
            // se asigna el nombre del agendamiento a la variable nameSchedule
            nameSchedule = nSchedule;
        }
        // se elimina el agendamiento del string
        schedule = schedule.substring(schedule.indexOf("-") + 1, schedule.length());
    }
    return 1;
}

// funcion para encender el muestreo
int on()
{
    if (counter != temp)
    {
        distance = ((2 * pi * R) / N) * counter;
        Serial.println(); // creo que no es necessario por el println de abajo
        temp = counter;
    }
    // Se envia el valor de la distancia al dispositivo conectado por bluetooth con el formato de "!distancia"
    String d = "!" + String(distance, DEC);
    SerialBT.println(d + "*"); // ! agrege el asterisco para que sepa que termino de enviar el dato (no se si es necesario)
    Serial.println(d);
    // se guarda el valor de la distancia en el archivo "data.txt"
    saveToSDCard("data.txt", d);
}

// funcion para manejar el temporizador
int timer(char *message)
{
    // iterar el mensaje para obtener el valor del tiempo formato: "TIMER;TIEMPO;"
    char *p = strtok(message, ";");
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
    if (timer == 0)
    {
        return -1;
    }
    // se activa el bool isTimer
    isTimer = true;

    // si es necesario guardar los datos del temp en el archivo "data.txt"
    // se debe crear un string con el formato "TIMER;START;FECHAINICIO;TIEMPO;!DISTANCIA;FECHAFIN;STOP;"
    // y se guarda en el archivo "data.txt"
    String dat = "START;;" + Date.now() + ";" + String(varTiempoMuestreo, DEC) + ";" + "cm" + ";";
    saveToSDCard("data.txt", dat);

    return 1;
}

// funcion para elimianr un agendamiento
int deleteSchedule(char *message)
{
    // se obtiene el nombre del agendamiento
    char *n = strtok(NULL, ";");
    // se crea un objeto File con el archivo "schedules.txt"
    File file = SD.open("schedules.txt");
    // si el agendamiento a eliminar es el actual
    if (n == nSchedule)
    {
        // isOn se asigna el valor de false para que no se ejecute el muestreo
        isOn = false;
        // enAgendamiento se asigna el valor de false para que no se termine el muestreo si no se ha iniciado un agendamiento
        enAgendamiento = false;
        // se crea un string con el formato "STOP;FechaHoraDeTermino"
        String dat = "STOP;" + dateNow() + ";";
        // se guarda el string en el archivo "data.txt"
        saveToSDCard("data.txt", dat);
        // se manda el data.txt por bluetooth al terminar el agendamiento
        readBT("data.txt");
        delay(100);
        return 1;
    }
    // se crea un string para almacenar el contenido del archivo "schedules.txt"
    String schedule = "";
    // se lee el archivo "schedules.txt" y se almacena en la variable String
    while (file.available())
    {
        schedule += (char)file.read();
    }
    file.close();
    // si el agendamiento no existe o schedule esta vacio
    if (schedule.indexOf(n) == -1 || schedule == "")
    {
        return -1;
    }
    // se crea una variable para almacenar la posicion del agendamiento a eliminar
    int searchPosition = 0;
    // se busca el agendamiento que tenga el nombre igual al nombre del agendamiento a eliminar
    while (schedule.indexOf("-") != -1)
    {
        // se crea un string para almacenar el nombre del agendamiento
        String nScheduleAux = schedule.substring(searchPosition, schedule.indexOf(","));
        if (nScheduleAux == n)
        {
            // schedule.substring(0, searchPosition) => se obtiene el string desde el inicio hasta la posicion del agendamiento a eliminar
            // schedule.substring(schedule.indexOf(nScheduleAux) + 1, schedule.length()) => se obtiene el string desde la posicion del agendamiento a eliminar hasta el final
            schedule = schedule.substring(0, searchPosition) + schedule.substring(schedule.indexOf(nScheduleAux) + 1, schedule.length());
            break;
        }
        else
        {
            searchPosition = schedule.indexOf("-") + 1;
        }
    }
    // se elimina el archivo "schedules.txt"
    SD.remove("schedules.txt");
    // se crea un objeto File con el archivo "schedules.txt"
    File file2 = SD.open("schedules.txt", FILE_WRITE);
    // se escribe el string con el contenido del archivo "schedules.txt" sin el agendamiento a eliminar
    file2.println(schedule); // TODO: revisar si es println o print porque no se si es necesario el salto de linea
    file2.close();
    // no se manda el archivo "schedules.txt" por bluetooth porque la app modifica su propio archivo "schedules.txt"
    return 1;
}

/* 6.- LOOP */
// funcion que admistra el funcionamiento del dispositivo
void loop()
{
    // si estamos en un temporizador se ejecuta el siguiente código
    if (isTimer)
    {
        // se llama al on() para encender el muestreo
        on();
        // se espera el tiempo de muestreo
        delay(varTiempoMuestreo);
        // se resta el tiempo de muestreo al tiempo restante del temporizador
        timer = timer - varTiempoMuestreo;
        // si el tiempo restante del temporizador es menor o igual a 0 se ejecuta el siguiente código
        if (timer <= 0)
        {
            // se asigna el valor de la variable isTimer a false para que no se ejecute el temporizador
            isTimer = false;
            // ! no hace falta guardar el stop porque la app manda un OFF; para terminar el muestreo
            // se crea un string con el formato "STOP;FechaHoraDeTermino"
            // String dat = "STOP;" + dateNow() + ";"; 
            // se guarda el string en el archivo "data.txt"
            // saveToSDCard("data.txt", dat);
            // se manda el data.txt por bluetooth al terminar el temporizador
            // readBT("data.txt");
            delay(100); // TODO: revisar si es necesario el delay
        }
    }
    // se comprueba si el siguiente agendamiento es menor o igual a la fecha y hora actual (se inicia el agendamiento)
    if (nextSchedule <= rtc.now())
    {
        // se asigna la fecha y hora de inicio del siguiente agendamiento un valor muy alto para que no se cumpla la condicion
        nextSchedule = DateTime("9999:12:30", "10:10:10");
        // se crea un string con el formato "STOP;FechaHoraDeTermino"
        String dat = "STOP;" + dateNow() + ";";
        // se guarda el string en el archivo "data.txt"
        saveToSDCard("data.txt", dat);
        // se manda el data.txt por bluetooth al terminar el agendamiento
        readBT("data.txt");
        delay(100);
        // se crea un string con el formato "START;Nombre;FechaHoraDeInicio;TiempoDeMuestreo;UnidadDeMedida;"
        dat = "START;" + nSchedule + ";" + dateNow() + ";" + String(varTiempoMuestreo, DEC) + ";" + "cm" + ";";
        // se guarda el string en el archivo "data.txt" y se inicia el muestreo
        saveToSDCard("data.txt", dat);
        // se asigna el valor de la variable isOn a true para que se inicie el muestreo
        isOn = true;
        // guarda el inicio del agendamiento en data y empieza a tomar la data
        enAgendamiento = true;
        // se manda un mensaje por bluetooth para indicar que se inicio el agendamiento (no estoy seguro sobre el c_str())
        // TODO: cambiar el mensaje por uno mas claro para la app
        SerialBT.println(dat + '*');
    }

    // si la variable isOn es true se ejecuta el siguiente código
    if (isOn)
    {
        // se llama a la funcion on() para encender el muestreo
        on();
        // se espera el tiempo de muestreo
        delay(varTiempoMuestreo);
        // se comprueba si la fecha y hora de fin del agendamiento es menor o igual a la fecha y hora actual
        // y si la variable enAgendamiento es true (para que no se termine el muestreo si no se ha iniciado un agendamiento)
        if (enAgendamiento && endNextSchedule <= rtc.now())
        {
            // isOn se asigna el valor de false para que no se ejecute el muestreo
            isOn = false;
            // enAgendamiento se asigna el valor de false para que no se termine el muestreo si no se ha iniciado un agendamiento
            enAgendamiento = false;
            // se crea un string con el formato "STOP;FechaHoraDeTermino"
            String dat = "STOP;" + dateNow() + ";";
            // se guarda el string en el archivo "data.txt"
            saveToSDCard("data.txt", dat);
            // se manda el data.txt por bluetooth al terminar el agendamiento
            readBT("data.txt");
            delay(100);
        }
    }
    else
    {
        delay(100);
    }
    // Si se recibe un mensaje por Bluetooth (SerialBT) se ejecuta el siguiente código
    if (SerialBT.available())
    {
        // Se lee el mensaje y se almacena en la variable message
        message = SerialBT.readString();
        // Se imprime el mensaje en el monitor serial
        // Se divide el mensaje en un token
        token = strtok(&message[0], ";");
        // Se compara el primer token con las palabras reservadas
        if (strcmp(token, "CONFIG") == 0)
        {
            check(config(&message[0]));
        }
        else if (strcmp(token, "SCAN") == 0)
        {
            token = strtok(NULL, ";");
            if (strcmp(token, "GET") == 0)
            { // MANDAR DATA.txt

                delay(100);
                readBT("data.txt");
            }
        }
        else if (strcmp(token, "SCHEDULE") == 0)
        {
            token = strtok(NULL, ";");

            if (strcmp(token, "ADD") == 0)
            {
                token = strtok(NULL, ";");
                check(createAgendamiento(token));
                // TODO: revisar si no da error con 100 ms menos
                delay(100);
            }
            else if (strcmp(token, "DELETE") == 0)
            {
                token = strtok(NULL, ";");
                check(deleteSchedule(token));
                delay(100);
            }
            else
            { // MANDAR SCHEDULES.txt
                readBT("schedules.txt");
                delay(100);
            }
        }
        else if (strcmp(token, "AGENDAR") == 0)
        {
            check(createAgendamiento(&message[0]));
        }
        else if (strcmp(token, "ON") == 0)
        {
            // el formato de encendido en el archivo es START;;TiempoDeMuestreo;UnidadDeMedida;
            isOn = true;
            String dat = "START;;" + dateNow() + ";" + String(varTiempoMuestreo, DEC) + ";" + "cm" + ";";
            saveToSDCard("data.txt", dat);

            delay(100);
        }
        else if (strcmp(token, "TIMER") == 0)
        {
            // el formato de encendido en el archivo es START;;TiempoDeMuestreo;UnidadDeMedida;
            isTimer = true;
            String dat = "START;;" + dateNow() + ";" + String(varTiempoMuestreo, DEC) + ";" + "cm" + ";";
            saveToSDCard("data.txt", dat);
            // manda el archivo cuando se acaba el temporizador
            delay(100);
        }
        else if (strcmp(token, "OFF") == 0)
        {
            isOn = false;
            isTimer = false;
            String dat = "STOP;" + dateNow() + ";";
            // saveToSDCard("data.txt", dat);
            // esto deberia mandar el archivo data.txt cuando se termine el muestreo
            check(readBT("data.txt"));
            // SerialBT.println("1*");
            delay(100);
        }
        else if (strcmp(token, "READ") == 0)
        {
            // el formato de lectura es READ; y lee el archivo temporal data.txt
            check(readBT("data.txt"));
        }
        else if (strcmp(token, "RESET") == 0)
        {
            // el formato de RESET es RESET;
            check(reset());
        }
        else
        {
            Serial.println("Error en el mensaje");
            SerialBT.write("-1*");
        }
        // Se limpia la variable message
        message = "";
    }
}
