import java.nio.file.Files
import java.nio.file.Path

// Rutas del proyecto
private val DIR_DATOS: Path = Path.of("proyecto", "datos")
private val DIR_INI: Path = DIR_DATOS.resolve("datos_ini")
private val DIR_FIN: Path = DIR_DATOS.resolve("datos_fin")
private val RUTA_CSV: Path = DIR_INI.resolve("videojuegos.csv")

// Mapas para las conversiones
private val LECTORES: Map<String, (Path) -> List<Videojuego>> = mapOf(
    "csv" to ::leerVideojuegosCSV,
    "xml" to ::leerVideojuegosXML,
    "json" to ::leerVideojuegosJSON
)

private val ESCRITORES: Map<String, (Path, List<Videojuego>) -> Unit> = mapOf(
    "csv" to ::escribirVideojuegosCSV,
    "xml" to ::escribirVideojuegosXML,
    "json" to ::escribirVideojuegosJSON
)

fun main() {
    println("CREACIÓN DE RUTAS PROYECTO")
    println("Creando rutas...")

    try {
        if (Files.notExists(DIR_INI)) {
            println("Creación de ruta para DATOS_INI")
            Files.createDirectories(DIR_INI)
        }

        if (Files.notExists(DIR_FIN)) {
            println("Creación de ruta para DATOS_FIN")
            Files.createDirectories(DIR_FIN)
        }

        println("MOSTRANDO ESTRUCTURA DE DIRECTORIOS Y FICHEROS")

        Files.walk(DIR_DATOS).use { stream ->
            stream.sorted().forEach { path ->
                val profundidad = path.nameCount - DIR_DATOS.nameCount
                val indentacion = " ".repeat(profundidad + 1)

                if (Files.isDirectory(path)) {
                    println("$indentacion[DIR] ${path.fileName}")
                } else if (Files.isRegularFile(path)) {
                    println("$indentacion[FILE] ${path.fileName}")
                }
            }
        }
    } catch (e: Exception) {
        println("\n--- Ocurrió un error al preparar las rutas del proyecto ---")
        e.printStackTrace()
    }

    // Al iniciar se lee el catálogo desde el CSV
    var catalogo: List<Videojuego> = leerVideojuegosCSV(RUTA_CSV)
    var salir = false

    while (!salir) {
        mostrarMenuPrincipal()
        when (pedirEntero("Elige una opción: ")) {
            1 -> catalogo = menuCSV(catalogo)
            2 -> catalogo = importarDesde(catalogo, DIR_INI.resolve("videojuegos.xml"), ::leerVideojuegosXML)
            3 -> catalogo = importarDesde(catalogo, DIR_INI.resolve("videojuegos.json"), ::leerVideojuegosJSON)
            4 -> menuConversiones()
            5 -> menuBIN(RUTA_CSV)
            0 -> {
                println("¡Hasta pronto!")
                salir = true
            }
            else -> println("Opción no válida. Elige una de las opciones del menú.")
        }
    }
}

private fun mostrarMenuPrincipal() {
    println()
    println("===== CATÁLOGO DE VIDEOJUEGOS =====")
    println("1. Gestión CSV")
    println("2. Leer catálogo desde XML")
    println("3. Leer catálogo desde JSON")
    println("4. Conversiones entre formatos")
    println("5. Gestión fichero BIN")
    println("0. Salir")
    println("====================================")
}

private fun menuCSV(catalogoInicial: List<Videojuego>): List<Videojuego> {
    var catalogo = catalogoInicial
    var volver = false

    while (!volver) {
        println()
        println("--- Gestión CSV ---")
        println("1. Listar todos los videojuegos")
        println("2. Buscar videojuego por ID")
        println("3. Añadir nuevo videojuego")
        println("4. Modificar nota de un videojuego")
        println("5. Eliminar videojuego")
        println("0. Volver al menú principal")

        when (pedirEntero("Elige una opción: ")) {
            1 -> listarVideojuegos(catalogo)
            2 -> {
                val id = pedirEntero("ID a buscar: ")
                if (id == null) println("Error: el ID debe ser un número entero.")
                else buscarPorId(catalogo, id)
            }
            3 -> catalogo = guardarSiCambia(catalogo, añadirVideojuego(catalogo))
            4 -> {
                val id = pedirEntero("ID del videojuego: ")
                val nota = pedirDecimal("Nueva nota (0.0 - 10.0): ")
                if (id == null || nota == null) {
                    println("Error: el ID debe ser un entero y la nota un número.")
                } else {
                    catalogo = guardarSiCambia(catalogo, modificarNota(catalogo, id, nota))
                }
            }
            5 -> {
                val id = pedirEntero("ID del videojuego a eliminar: ")
                if (id == null) println("Error: el ID debe ser un número entero.")
                else catalogo = guardarSiCambia(catalogo, eliminarVideojuego(catalogo, id))
            }
            0 -> volver = true
            else -> println("Opción no válida.")
        }
    }
    return catalogo
}

private fun menuConversiones() {
    var volver = false

    while (!volver) {
        println()
        println("--- Conversiones ---")
        println("1. CSV  → JSON")
        println("2. CSV  → XML")
        println("3. JSON → CSV")
        println("4. JSON → XML")
        println("5. XML  → CSV")
        println("6. XML  → JSON")
        println("0. Volver")

        when (pedirEntero("Elige una opción: ")) {
            1 -> convertirFormato("csv", "json")
            2 -> convertirFormato("csv", "xml")
            3 -> convertirFormato("json", "csv")
            4 -> convertirFormato("json", "xml")
            5 -> convertirFormato("xml", "csv")
            6 -> convertirFormato("xml", "json")
            0 -> volver = true
            else -> println("Opción no válida.")
        }
    }
}

private fun importarDesde(catalogo: List<Videojuego>, ruta: Path, leer: (Path) -> List<Videojuego>): List<Videojuego> {
    val leidos = leer(ruta)
    if (leidos.isEmpty()) {
        println("No hay videojuegos que importar.")
        return catalogo
    }

    println("--- Videojuegos leídos:")
    listarVideojuegos(leidos)

    val (integrado, nuevos) = integrarCatalogo(catalogo, leidos)
    println("Videojuegos nuevos integrados: $nuevos")

    return guardarSiCambia(catalogo, integrado)
}

private fun convertirFormato(de: String, a: String) {
    val origen = DIR_INI.resolve("videojuegos.$de")
    val destino = DIR_FIN.resolve("videojuegos.$a")
    convertir(origen, destino, LECTORES.getValue(de), ESCRITORES.getValue(a))
}

private fun guardarSiCambia(anterior: List<Videojuego>, nuevo: List<Videojuego>): List<Videojuego> {
    if (anterior != nuevo) {
        try {
            escribirVideojuegosCSV(RUTA_CSV, nuevo)
            println("--- Catálogo guardado en: $RUTA_CSV")
        } catch (e: Exception) {
            println("Error al guardar el CSV: ${e.message}")
        }
    }
    return nuevo
}

// Funciones auxiliares de lectura por consola
fun pedirTexto(mensaje: String): String? {
    print(mensaje)
    val texto = readlnOrNull()?.trim()
    return if (texto.isNullOrEmpty()) null else texto
}

fun pedirEntero(mensaje: String): Int? {
    print(mensaje)
    return readlnOrNull()?.trim()?.toIntOrNull()
}

fun pedirDecimal(mensaje: String): Double? {
    print(mensaje)
    return readlnOrNull()?.trim()?.replace(',', '.')?.toDoubleOrNull()
}