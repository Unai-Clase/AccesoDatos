# Catálogo de Videojuegos

Programa de consola en Kotlin para gestionar un catálogo de videojuegos.
Permite gestionar el catálogo en un ficheros: CSV, XML, JSON y tambien gestionar Binarios,
convertir entre los tres formatos y mantener una copia en un fichero binario de acceso aleatorio con su propio CRUD.

## 1. Estructura de datos

### Data class

```kotlin
@Serializable
data class Videojuego(
    val id: Int,
    val titulo: String,
    val genero: String,
    val anio: Int,
    val nota: Double
)
```

### Estructura del registro binario (`videojuegos.dat`)

| Campo    | Tipo   | Bytes | Notas                                        |
|----------|--------|-------|----------------------------------------------|
| `id`     | Int    | 4     |                                              |
| `titulo` | String | 40    | Longitud fija, UTF-8, rellenado con espacios |
| `genero` | String | 20    | Longitud fija, UTF-8, rellenado con espacios |
| `anio`   | Int    | 4     |                                              |
| `nota`   | Double | 8     |                                              |

**Tamaño total del registro: 4 + 40 + 20 + 4 + 8 = 76 bytes.**

El registro número *n* (empezando en 0) comienza en el byte `n * 76`.

### Ficheros del proyecto

```
proyecto/datos/
├── datos_ini/          Ficheros de origen
│   ├── videojuegos.csv
│   ├── videojuegos.xml
│   └── videojuegos.json
├── datos_fin/          Resultado de las conversiones (videojuegos.csv / .xml / .json)
└── videojuegos.dat     Fichero binario de acceso aleatorio

src/main/kotlin/
├── Videojuego.kt              Data class y clase contenedora del XML
├── VideojuegoRepository.kt    Lectura/escritura de CSV, XML y JSON
├── VideojuegoBinRepository.kt Lectura/escritura del fichero binario
├── VideojuegoService.kt       Lógica del CRUD, integración y conversión
└── Main.kt                    Menús y lectura de consola
```

### Formatos de intercambio y librerías

| Formato | Librería                         |
|---------|----------------------------------|
| CSV     | Kotlin-CSV (delimitador `;`)     |
| XML     | Jackson XML (`XmlMapper`)        |
| JSON    | kotlinx.serialization            |
| BIN     | `java.io.RandomAccessFile`       |

## 2. Instrucciones de ejecución

- **Requisitos previos:** JDK 21 o superior instalado e IntelliJ IDEA.
- **Compilación:** abre el proyecto en IntelliJ IDEA y deja que Gradle sincronice las dependencias
  (botón del elefante, *Load Gradle Changes*).
- **Ficheros de datos:** los tres ficheros de `proyecto/datos/datos_ini/` deben existir antes de importar o convertir.
  Las carpetas se crean solas al arrancar si faltan.
- **Ejecución:** ejecuta `Main.kt` (función `main`). Las rutas son relativas, por lo que el directorio de trabajo
  debe ser la raíz del proyecto (es lo que usa IntelliJ por defecto).
- **Menú principal:**

```
1. Gestión CSV            Listar, buscar, añadir, modificar nota y eliminar
2. Leer catálogo desde XML    Importa sin duplicar IDs y guarda en el CSV
3. Leer catálogo desde JSON   Importa sin duplicar IDs y guarda en el CSV
4. Conversiones entre formatos  Genera ficheros en datos_fin/
5. Gestión fichero BIN    Importar, leer, añadir, modificar y eliminar
0. Salir
```

## 3. Decisiones de diseño

- **Registro de 76 bytes de tamaño fijo:** al ser todos los registros iguales, se puede saltar directamente al
  registro *n* con `seek(n * 76)` sin recorrer el fichero, que es lo que hace útil un acceso aleatorio.
- **40 bytes para el título:** el título más largo de los datos, "The Legend of Zelda: Breath of the Wild",
  tiene 39 caracteres. Si un texto es más largo se recorta sin partir ningún carácter de varios bytes.
- **20 bytes para el género:** los géneros usados ("Plataformas", "Roguelike", "Aventura"...) caben de sobra.
- **UTF-8 con relleno de espacios:** las tildes ("Acción") ocupan 2 bytes, por eso la longitud se mide en bytes
  y no en caracteres. Al leer se quitan los espacios finales.
- **Fichero `videojuegos.dat`:** la extensión `.dat` indica que es un fichero binario de datos, no legible
  con un editor de texto. Está en `proyecto/datos/` porque no es ni un origen ni un resultado de conversión.
- **Eliminar sin dejar huecos:** al borrar un registro, los siguientes se desplazan una posición hacia atrás
  y el fichero se acorta 76 bytes. Así se mantiene el orden y los registros siguen siendo contiguos.
- **Modificar solo escribe lo necesario:** cambiar la nota sobrescribe únicamente sus 8 bytes, sin reescribir el registro.
- **Listas inmutables en el catálogo en memoria:** las operaciones del CRUD devuelven una lista nueva y el CSV
  solo se reescribe si el catálogo ha cambiado.
- **Datos como paso intermedio:** las conversiones siguen el patrón *formato origen → objetos Kotlin → formato destino*,
  con una única función genérica `convertir(origen, destino, leer, escribir)`.
- **Validación de la entrada:** toda la lectura de consola usa `readlnOrNull()`, `toIntOrNull()` y `toDoubleOrNull()`,
  y las operaciones de la opción 5 repiten la pregunta hasta que el dato es válido.
