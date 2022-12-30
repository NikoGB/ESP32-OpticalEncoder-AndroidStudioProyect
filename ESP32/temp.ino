// TODO: 
/*
    1.entrar al TXT de agendamiento (schedule.txt) y buscar el proximo agendamiento
      guardar la fecha de inicio, final y el nombre (no entiendo pa que pero dejar listo)
    2.temporizador (milisegundos) el tiempo que llega hay que pasarlo a millisegundos 
      y ir restando hasta que llegue a 0 (ojo que puede ser negativo, restar segun varTiempoMuestreo)
    3.al conectarse la app manda un mensaje y hay que enviar el data.txt y luego manda 
      otro mensaje y hay que enviar el schedule.txt (esta listo en teoria)
    4.Al empezar un agendamiento hay que mandar un mensaje al app para que sepa que 
      se esta agendando y que nombre tiene (fecha de inicio, final y nombre)
    5. al terminar un agendamiento o activacion manual, mandar el data.txt al app
    6. Si se interrumpe un agendamiento, al volver a encenderse el ESP32, debe reconocer 
      que se interrumpio y marcar el STOP;ERROR; en el data.txt (asi cuando se mande al app
      sepa que se interrumpio y que hay que hacer) (esta listo en teoria)
    7.Leer archivo no funciona (verificar el tamaño del buffer) (256 bytes segun la IA)
*/

/*
  DONE:
  1. listo (la funcion esta creada pero no se usa) (revisar detalles del formato de schedule.txt)
  2. listo (conversion de tiempo a milisegundos pero no los guarda en data.txt)
  3. listo (la app envia el mensaje SCAN;GET; y SCHEDULE;GET; la funcion read se encarga de mandarlo)
  4. listo 
  5. listo
  6. listo
  7. listo (256 bytes segun la IA) (funcion readBT)
*/