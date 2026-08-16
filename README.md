# Bank Batch — Migración de Procesos Legacy con Spring Batch

Exp1_S1 — Desarrollo Backend III (PBY2203)
Alumno: Lucas Silva A. - Grupo 7
Docente: Alonso Castillo P.

---

# 1. Descripción del proyecto

Este proyecto moderniza tres procesos batch legacy del Banco XYZ, utilizando Spring Batch,
reemplazando el sistema antiguo por Jobs y Steps que leen archivos CSV, aplican
validaciones/transformaciones y persisten los datos en una base de datos relacional MySQL.

Se implementan tres procesos independientes, ejecutados secuencialmente dentro de un único Job (`bankJob`):

| Step                | Origen (CSV)          | Descripción                                                  |
|---------------------|-----------------------|--------------------------------------------------------------|
| `cuentasStep`       | `cuentas_anuales.csv` | Generación de estados de cuenta anuales por cuenta           |
| `interesesStep`     | `intereses.csv`       | Cálculo de intereses mensuales sobre saldos (tasa 5%)        |
| `transaccionesStep` | `transacciones.csv`   | Reporte de transacciones diarias, con detección de anomalías |

Los datos de origen provienen de: https://github.com/KariVillagran/bank_legacy_data

---

# 2. Estructura del código

```
bank-batch/
├── src/main/java/com/duoc/bank_batch/
│   ├── BankBatchApplication.java        → Clase principal Spring Boot
│   ├── config/
│   │   └── BatchConfig.java             → Definición de Job, Steps, Readers, Processors, Writers
│   ├── entity/
│   │   ├── Cuenta.java                  → Entidad JPA → tabla 'cuentas'
│   │   ├── Interes.java                 → Entidad JPA → tabla 'intereses'
│   │   └── Transaccion.java             → Entidad JPA → tabla 'transacciones'
│   └── processor/
│       └── TransaccionProcessor.java    → ItemProcessor + listener: valida, detecta
│                                        → anomalías y genera el resumen del step
├── src/main/resources/
│   ├── application.properties           → Configuración de datasource y batch
│   └── data/
│       ├── cuentas_anuales.csv
│       ├── intereses.csv
│       └── transacciones.csv
└── pom.xml
```

---

# 3. Reglas de negocio y manejo de errores

Implementadas en `TransaccionProcessor` (step `transaccionesStep`):

- Normalización de `tipo`: se recorta espacios y se convierte a minúsculas antes de validar.
- Tipo inválido: si `tipo` no es `debito` ni `credito` tras normalizar, el registro se descarta (dato mal clasificado, no se puede insertar con confianza) y queda registrado en el log.
- Monto en cero: se detecta como anomalía y se reporta, pero se inserta igual (no se descarta información financiera real sin certeza de que sea un error).
- Posibles duplicados (misma `fecha`, `monto` y `tipo`): se detectan y reportan, pero se insertan igual, ya que dos transacciones legítimas pueden coincidir en esos tres campos.
- Al finalizar el step se imprime un resumen con el conteo de cada categoría (total leídos, corregidos, descartados, anomalías, insertados).

---

# 4. Requisitos previos

- Java 21+ (probado con Java 26)
- Maven 3.9+
- MySQL 8.x en ejecución local
- Una base de datos vacía, por ejemplo `bank_batch`

---

# 5. Configuración

Editar `src/main/resources/application.properties` con las credenciales propias:

-application.properties:

spring.application.name=bank-batch

spring.datasource.url=jdbc:mysql://localhost:3306/bank_batch?useSSL=false&serverTimezone=America/Santiago&allowPublicKeyRetrieval=true
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.batch.jdbc.initialize-schema=always
spring.batch.jdbc.platform=mysql
spring.batch.job.enabled=true

logging.level.org.springframework.batch=INFO
logging.level.com.duoc.bank_batch=INFO
```

Nota: `ddl-auto=create-drop` recrea las tablas de negocio (`cuentas`, `intereses`, `transacciones`) en cada arranque —
ideal para desarrollo/pruebas. Para un entorno con datos persistentes, cambiar a `validate` o `update` 
y gestionar el esquema con una herramienta de migración (Flyway/Liquibase).

---

# 6. Cómo clonar y ejecutar

```bash
# 1. Clonar el repositorio
git clone <https://github.com/LucasS-7/EXP1.S1-BANK-BATCH.git>
cd bank-batch

# 2. Crear la base de datos (si no existe)
mysql -u root -p -e "CREATE DATABASE bank_batch;"

# 3. Compilar el proyecto
mvn clean install

# 4. Ejecutar el Job
mvn spring-boot:run
```

Al ejecutar, el `bankJob` corre automáticamente al levantar la aplicación (`spring.batch.job.enabled=true`), procesando los tres Steps en orden: `cuentasStep → interesesStep → transaccionesStep`.

Cada ejecución genera una nueva `JobInstance` (gracias a `RunIdIncrementer`), por lo que el Job puede volver a correrse tantas veces como sea necesario sin quedar marcado como "ya completado".

---

# 7. Evidencia de ejecución

El log de consola de una corrida exitosa muestra:

- Creación de las 3 tablas (`cuentas`, `intereses`, `transacciones`)
- Ejecución de los 3 Steps con `status=COMPLETED`
- Los `INSERT` generados por Hibernate para cada tabla
- El resumen de anomalías del `transaccionesStep`:

```
=========== RESUMEN - REPORTE DE TRANSACCIONES DIARIAS ===========
Total de registros leídos:              10
Registros con tipo normalizado (trim/lower): 0
Registros descartados por tipo inválido: 0
Anomalías - monto en cero:               1
Anomalías - posibles duplicados:         1
Registros efectivamente insertados:      10
====================================================================
```

# 8. Tecnologías utilizadas

- Spring Boot 3.5.6
- Spring Batch 5.2.3
- Hibernate / Spring Data JPA 6.6.29
- MySQL 8.0 (mysql-connector-j 9.4.0)
- HikariCP 6.3.3
- Maven