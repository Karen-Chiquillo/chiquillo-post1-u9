# Recordatorios Citas Service

## Descripción

Repositorio del post-contenido de la Unidad 9 de Patrones de Diseño de Software — Sexto Semestre. Contiene un único proyecto Spring Boot (`recordatorios-citas-service`) que revisa qué citas médicas ocurren dentro de las próximas 24 horas y todavía no han sido notificadas, y le envía al paciente un recordatorio por su canal preferido (correo o SMS) sobre una base de datos H2 en memoria.

El servicio central, `CitaReminderServiceImpl`, está construido desde el inicio con dos patrones de unidades anteriores y recibe sus tres colaboradores por constructor (DIP): **Strategy** (`ReminderChannel`, Unidad 4), donde cada canal de envío es una implementación intercambiable del mismo método `enviar(cita, mensaje)`, y **Factory Method** (`ReminderChannelFactory`, Unidad 2), que decide qué canal entregar según `cita.getCanalPreferido()`. El tercer colaborador es un `Clock`, que se registra como bean en `config/ClockConfig` para que la hora actual también sea una dependencia inyectada.

El proyecto usa Spring Boot 3.5.16 en lugar del 3.3.x que indica la guía, porque Spring Initializr ya no ofrece la línea 3.3.

---

## Parte 1 — Pruebas Unitarias con JUnit 5 y Mockito

`CitaReminderServiceImplTest` aísla `CitaReminderServiceImpl` de sus tres colaboradores con `@ExtendWith(MockitoExtension.class)`, sin levantar el contexto de Spring: `CitaRepository`, `ReminderChannelFactory` y `ReminderChannel` son dobles de Mockito, y el `Clock` es `Clock.fixed(...)` congelado en `2026-08-10T09:00:00Z`. La justificación de cada doble está en *Decisiones de diseño*.

Además de las tres pruebas de la guía, se agregaron pruebas para lo que la guía describe pero no prueba: la selección de canal para EMAIL **y** SMS, el contenido del mensaje, la Factory real y la estrategia SMS. Los paquetes de prueba repiten los de la aplicación (`service/` y `channel/`).

| Clase | Prueba | Qué verifica |
|-------|--------|--------------|
| `CitaReminderServiceImplTest` | `enviarRecordatoriosPendientes_citaEnVentana_usaElCanalPreferido` | La cita se envía por el canal que entrega la Factory, se guarda una vez y queda con `recordatorioEnviado = true`. |
| | `enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos` | Con el `Clock` fijo, el repositorio se consulta exactamente entre las 09:00 del 10 y las 09:00 del 11 de agosto. |
| | `enviarRecordatoriosPendientes_sinCitasPendientes_noConsultaLaFactory` | Sin citas no se pide ningún canal ni se llama a `save`. |
| | `enviarRecordatoriosPendientes_cualquierCanalPreferido_pideALaFactoryEseCanal` | `@ParameterizedTest` con `@EnumSource`: para EMAIL y para SMS se verifica `obtenerCanal(canal)`. |
| | `enviarRecordatoriosPendientes_citaEnVentana_mensajeIncluyeFechaDeLaCita` | Un `ArgumentCaptor` sobre el mensaje comprueba que incluye la fecha de la cita. |
| `ReminderChannelFactoryImplTest` | `obtenerCanal_email_retornaEmailReminderChannel` / `obtenerCanal_sms_retornaSmsReminderChannel` | La Factory real devuelve la misma instancia de cada estrategia (`assertSame`). |
| `SmsReminderChannelTest` | `enviar_citaConTelefono_delegaAlGatewayConContactoYMensaje` | `SmsReminderChannel` llama al gateway con el teléfono del paciente y el mensaje. |

`mvn test` ejecuta **9 pruebas** (8 métodos; la parametrizada corre una vez por canal) con 0 fallos.

Las pruebas corren sin advertencias. Con JDK 21 y la configuración por defecto, cada `mvn test` mostraba `Mockito is currently self-attaching...` y `WARNING: A Java agent has been loaded dynamically`; por eso en el `pom.xml` Surefire carga Mockito como `-javaagent` (con `-Xshare:off`), y en `application.properties` se definió `spring.jpa.open-in-view=false` para quitar el aviso de Spring al arrancar.

---

## Decisiones de diseño

**Por qué Strategy facilitó las pruebas.** Sin `ReminderChannel` como interfaz, el envío por correo y por SMS estaría escrito dentro de `CitaReminderServiceImpl`, y para probar el servicio habría que sustituir un cliente SMTP y un cliente HTTP. Con Strategy, un solo `@Mock ReminderChannel` reemplaza a cualquier canal: la prueba parametrizada `cualquierCanalPreferido_pideALaFactoryEseCanal` usa el mismo doble para EMAIL y para SMS sin cambiar una línea. Además, cada estrategia concreta se prueba sola; `SmsReminderChannelTest` solo sustituye el `NotificadorSmsGateway`.

**Por qué Factory facilitó las pruebas.** Sin `ReminderChannelFactory`, el servicio tendría un `switch` sobre `CanalNotificacion` y la decisión del canal solo se podría comprobar de forma indirecta, viendo qué se imprimió. Con la Factory inyectada, elegir el canal pasa a ser una llamada que se puede verificar: `verify(reminderChannelFactory).obtenerCanal(canal)`. El README de la guía cita esa línea, pero ninguna de sus pruebas la contiene; aquí está en `cualquierCanalPreferido_pideALaFactoryEseCanal`, y el `switch` de la Factory real se prueba aparte en `ReminderChannelFactoryImplTest`. Hay que aclarar que `ReminderChannelFactory` no es el Factory Method clásico del GoF, que delega la creación en subclases (Unidad 2, sección 3.2), sino una fábrica parametrizada detrás de una interfaz. Para las pruebas el beneficio es el mismo: el servicio depende de la abstracción y no de las clases concretas.

**Mock vs. Stub vs. Fake (Parte 1).** Cada uno de los tres colaboradores del constructor se reemplazó según la tabla 3.1 de la guía: un stub solo devuelve valores predefinidos, un mock además verifica cómo fue llamado y un fake es una implementación simplificada con lógica real.

- **`CitaRepository` → Mock.** En la misma prueba se necesita controlar lo que devuelve `findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(...)` y verificar cómo fue llamado: `save(cita)` exactamente una vez cuando hay una cita, `never()` cuando no hay ninguna, y con dos `ArgumentCaptor` los límites exactos de la consulta. Un stub no podría detectar que el servicio olvidó guardar la cita marcada como enviada, y un fake con H2 convertiría la prueba unitaria en una de integración que levanta JPA.
- **`ReminderChannelFactory` / `ReminderChannel` → Mock.** El comportamiento bajo prueba es la selección de la estrategia: que el servicio pida a la Factory el canal preferido de la cita y que ese canal reciba el mensaje. Eso solo se comprueba verificando interacciones (`obtenerCanal(canal)` y `enviar(eq(cita), ...)`), no con un valor de retorno; además `enviar` devuelve `void`, así que un stub del canal no tendría nada que devolver. Un matiz que la guía no separa: el `@Mock` de la Factory hace de **stub** en `citaEnVentana_usaElCanalPreferido`, donde solo se programa `obtenerCanal(EMAIL)` y lo que se verifica es el envío, y de **mock** en `cualquierCanalPreferido_pideALaFactoryEseCanal`, donde se verifica la llamada para EMAIL y SMS. Mockito no define el tipo de doble por la anotación, sino por lo que cada prueba hace con él.
- **`Clock` → Fake (no Mock).** `Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"))` no es un doble hecho a mano: es una implementación real y completa de `java.time.Clock` que trae el JDK, así que `LocalDateTime.now(clock)` funciona igual que en producción, solo que con el instante congelado. Mockearlo exigiría programar `instant()` y `getZone()` por separado, y con `Clock.systemDefaultZone()` la prueba de los límites de la ventana no se podría escribir porque el resultado cambiaría en cada ejecución. Con el fake, `consultaLosLimitesExactos` comprueba `2026-08-10T09:00` y `2026-08-11T09:00` en cualquier máquina y a cualquier hora.

---

## Ejecutar las pruebas

Requisitos: Java 21 y Maven 3.9+ (o el wrapper incluido).

```bash
mvn test                  # Pruebas unitarias
mvn spring-boot:run       # Arranca la aplicación con H2 en memoria
```

> En Windows con el wrapper: `.\mvnw.cmd test` y `.\mvnw.cmd spring-boot:run`.

---

## Herramientas utilizadas

- Java 21, Spring Boot 3.5.16, Spring Data JPA, H2, Lombok, JUnit 5, Mockito
- Apache Maven, Git, GitHub

---

## Evidencias de pruebas

### Parte 1

| # | Prueba | Resultado |
|---|--------|-----------|
| 1 | `mvn test` | 9 pruebas, 0 fallos, `BUILD SUCCESS` |
| 2 | `mvn spring-boot:run` con la base vacía | Arranca con `jdbc:h2:mem` y Tomcat en el puerto 8080, sin advertencias |

![mvn test Parte 1](docs/evidencias/p1/01-mvn-test-parte1.png)
![mvn spring-boot:run](docs/evidencias/p1/02-spring-boot-run.png)

---

## Conclusiones

La Parte 1 mostró que elegir el doble de prueba no depende de la herramienta sino de lo que cada prueba necesita comprobar: `CitaRepository` y `ReminderChannel` se reemplazaron con mocks porque lo importante era verificar las interacciones (`save` y `enviar`), mientras que el `Clock` se resolvió con `Clock.fixed`, un fake que ya trae el JDK, porque mockearlo habría sido más trabajo y menos fiel al comportamiento real. Lo que más costó fue la Factory: la guía la trata como mock, pero en una prueba solo se programa su respuesta y en otra se verifica su llamada, así que el mismo objeto de Mockito cumple el papel de stub o de mock según el caso. También quedó claro que los patrones de las unidades anteriores son los que hacen posible aislar el servicio: sin Strategy no habría una sola interfaz que reemplazar para todos los canales, y sin Factory Method la selección del canal sería un `switch` interno imposible de verificar. Por último, revisar la guía con cuidado dejó ver que su README afirmaba un `verify(...).obtenerCanal(EMAIL)` que ninguna de sus pruebas tenía, y escribir esa prueba fue la forma de que la documentación dijera la verdad.
