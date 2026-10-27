import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path


// Se crean una sola vez y se reutilizan
private val xmlMapper = XmlMapper().registerKotlinModule()
private val json = Json { prettyPrint = true }

// Crea la carpeta destino si no existe
private fun asegurarCarpeta(ruta: Path) {
    ruta.parent?.let { Files.createDirectories(it) }
}

// CSV
fun leerVideojuegosCSV(ruta: Path): List<Videojuego> {
    if (!Files.isReadable(ruta)) {
        println("Error: No se puede leer el fichero en la ruta: $ruta")
        return emptyList()
    }

    return try {
        val reader = csvReader {
            delimiter = ';'
        }

        // Devuelve Lista de String una lista de columnas por cada fila.
        val filas: List<List<String>> = reader.readAll(ruta.toFile())

        // Convertimos cada fila en un Videojuego.
        val videojuegos = filas.mapNotNull { columnas ->
            if (columnas.size >= 5) {
                try {
                    val id = columnas[0].toInt()
                    val titulo = columnas[1]
                    val genero = columnas[2]
                    val anio = columnas[3].toInt()
                    val nota = columnas[4].toDouble()
                    Videojuego(id, titulo, genero, anio, nota)
                } catch (e: Exception) {
                    println("Fila inválida ignorada: $columnas -> Error: ${e.message}")
                    null
                }
            } else {
                println("Fila con formato incompleto ignorada: $columnas")
                null
            }
        }
        println("--- Información leída con éxito de: $ruta")
        videojuegos
    } catch (e: Exception) {
        println("Error al leer el fichero CSV: ${e.message}")
        emptyList()
    }
}

fun escribirVideojuegosCSV(ruta: Path, videojuegos: List<Videojuego>) {
    asegurarCarpeta(ruta)

    csvWriter {
        delimiter = ';'
    }.writeAll(
        videojuegos.map { v ->
            listOf(
                v.id.toString(),
                v.titulo,
                v.genero,
                v.anio.toString(),
                v.nota.toString()
            )
        },
        ruta.toFile()
    )
}

// XML
fun leerVideojuegosXML(ruta: Path): List<Videojuego> {
    if (!Files.isReadable(ruta)) {
        println("Error: No se puede leer el fichero en la ruta: $ruta")
        return emptyList()
    }

    return try {
        val contenedor: VideojuegosXML = xmlMapper.readValue(ruta.toFile())
        println("--- Información leída con éxito de: $ruta")
        contenedor.videojuegos
    } catch (e: Exception) {
        println("Error al leer el fichero XML: ${e.message}")
        emptyList()
    }
}

fun escribirVideojuegosXML(ruta: Path, videojuegos: List<Videojuego>) {
    asegurarCarpeta(ruta)

    // XML formateado con saltos de línea e indentación para que sea legible
    val xmlString = xmlMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(VideojuegosXML(videojuegos))

    Files.writeString(ruta, xmlString)
}

// JSON
fun leerVideojuegosJSON(ruta: Path): List<Videojuego> {
    if (!Files.isReadable(ruta)) {
        println("Error: No se puede leer el fichero en la ruta: $ruta")
        return emptyList()
    }

    return try {
        val jsonString = Files.readString(ruta)
        val videojuegos = Json.decodeFromString<List<Videojuego>>(jsonString)
        println("--- Información leída con éxito de: $ruta")
        videojuegos
    } catch (e: Exception) {
        println("Error al leer el fichero JSON: ${e.message}")
        emptyList()
    }
}

fun escribirVideojuegosJSON(ruta: Path, videojuegos: List<Videojuego>) {
    asegurarCarpeta(ruta)

    val jsonString = json.encodeToString(videojuegos)
    Files.writeString(ruta, jsonString)
}
