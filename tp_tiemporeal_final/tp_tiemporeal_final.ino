#include <TimerOne.h>

// Canal ADC
int adcCh = 0;

long timer_period = 1000000 / 10000;

// Led
int ledPin = 13;
int ledPinVal = 0;

#ifndef BV
#define BV(bit) (1 << (bit))
#endif

void setup_adc() {

#if defined(__AVR_ATmega1280__)
  // Desactivar el ahorro de energia del ADC
  PRR0 &= ~BV(PRADC);
#else
  // Desactivar el ahorro de energia del ADC
  PRR &= ~BV(PRADC);
#endif

  // Eleccion del voltaje de referencia ADC (tabla 23-3) y del canal.
  // AVcc en este caso.
  // ADLAR: Ajuste a izquierda de la lectura del ADC. Solo me interesan los 8 bits m'as significativos.
  ADMUX = BV(REFS0) | BV(ADLAR) | adcCh;

  // ADEN: Activar el ADC.
  // ADSC: Iniciar la conversi'on.
  // ADATE: Auto trigger, inicia la conversi'on del origen seleccionado.
  // ADIE: Activar la interrupcion de notificaci'on de fin de conversi'on.
  // ADPS2: Prescaler/16.
  ADCSRA = BV(ADEN) /* | BV(ADSC) */ /* | BV(ADATE) */ | BV(ADIE) | BV(ADPS2);
  // ADCSRA = 0;

  // Timer1/Counter1.
  ADCSRB = 0; /* BV(ADTS2) | BV(ADTS1) | BV(ADTS0); */  
}

void setup_timer1() {
  Timer1.initialize(timer_period);
  Timer1.attachInterrupt(callback, timer_period);
}

void setup() {
  // Led
  pinMode(ledPin, OUTPUT);
  
  // Serial
  Serial.begin(115200);

  // ADC
  setup_adc();

  // Timer1
  setup_timer1();
}

void loop() {
  
}

void callback() {
  // digitalWrite(ledPin, ledPinVal ? HIGH : LOW);
  // ledPinVal = 1 - ledPinVal;
  ADCSRA |= BV(ADSC);
}

ISR(ADC_vect) 
{
  // LED Scrambler para verificar actividad
  digitalWrite(ledPin, ledPinVal ? HIGH : LOW);
  ledPinVal = 1 - ledPinVal;
  
  // ADC
  int valorADC = ADCH;

  Serial.write(valorADC);
}

