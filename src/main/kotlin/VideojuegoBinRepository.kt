import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

// Tamaños en bytes del registro binario 76 bytes
const val TAMANO_ID = Int.SIZE_BYTES
const val TAMANO_TITULO = 40
const val TAMANO_GENERO = 20
const val TAMANO_ANIO = Int.SIZE_BYTES
const val TAMANO_NOTA = Double.SIZE_BYTES
const val TAMANO_REGISTRO = TAMANO_ID + TAMANO_TITULO + TAMANO_GENERO + TAMANO_ANIO + TAMANO_NOTA

val rutaBIN: Path = Path.of("proyecto", "datos", "datos_fin", "videojuegos.bin")
val charsetBIN: Charset = Charsets.ISO_8859_1

fun menuBIN(rutaImportacion: Path) {
    var volver = false

    while (!volver) {
        println()
        println("===== CRUD fichero BIN =====")
        println("1. Importar información de un fichero de intercambio.")
        println("2. Leer información del fichero binario.")
        println("3. Añadir un registro nuevo")
        println("4. Modificar un registro existente (por ID)")
        println("5. Eliminar un registro existente (por ID)")
        println("0. Volver")

        when (binPedirEntero("Elige una opción: ")) {
            1 -> binConControl { importarABin(rutaImportacion) }
            2 -> binConControl { mostrarBin() }
            3 -> binConControl { anadirABin() }
            4 -> binConControl { modificarEnBin() }
            5 -> binConControl { eliminarDeBin() }
            0 -> volver = true
            else -> println("Opción no válida.")
        }
    }
}

private fun binConControl(accion: () -> Unit) {
    try {
        accion()
    } catch (e: Exception) {
        println("Error al trabajar con el fichero binario: ${e.message}")
    }
}

fun importarABin(origen: Path) {
    if (!Files.exists(origen)) {
        println("Error: El fichero de origen no existe: $origen")
        return
    }

    val datos = leerVideojuegosCSV(origen)
    if (datos.isEmpty()) {
        println("El fichero origen está vacío.")
        return
    }

    Files.createDirectories(rutaBIN.parent)
    FileChannel.open(rutaBIN, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).close()

    FileChannel.open(rutaBIN, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND).use { canal ->
        for (v in datos) {
            val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)
            buffer.putInt(v.id)

            val tituloBytes = v.titulo.padEnd(TAMANO_TITULO, ' ').toByteArray(charsetBIN)
            buffer.put(tituloBytes, 0, TAMANO_TITULO)

            val generoBytes = v.genero.padEnd(TAMANO_GENERO, ' ').toByteArray(charsetBIN)
            buffer.put(generoBytes, 0, TAMANO_GENERO)

            buffer.putInt(v.anio)
            buffer.putDouble(v.nota)

            buffer.flip()
            while (buffer.hasRemaining()) {
                canal.write(buffer)
            }
        }
    }
    println("--- Importados ${datos.size} registros con éxito en: $rutaBIN")
}

fun mostrarBin() {
    val lista = leerBin()
    if (lista.isEmpty()) {
        println("El fichero binario está vacío o no existe.")
    } else {
        println("\n--- Contenido del Fichero Binario ---")
        for (v in lista) {
            println(" - ID: ${v.id}, Título: ${v.titulo}, Género: ${v.genero}, Año: ${v.anio}, Nota: ${v.nota}")
        }
    }
}

fun leerBin(): List<Videojuego> {
    val lista = mutableListOf<Videojuego>()
    if (!Files.isReadable(rutaBIN)) return lista

    FileChannel.open(rutaBIN, StandardOpenOption.READ).use { canal ->
        val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)
        while (canal.read(buffer) > 0) {
            buffer.flip()

            val id = buffer.getInt()

            val tituloBytes = ByteArray(TAMANO_TITULO)
            buffer.get(tituloBytes)
            val titulo = String(tituloBytes, charsetBIN).trim()

            val generoBytes = ByteArray(TAMANO_GENERO)
            buffer.get(generoBytes)
            val genero = String(generoBytes, charsetBIN).trim()

            val anio = buffer.getInt()
            val nota = buffer.getDouble()

            lista.add(Videojuego(id, titulo, genero, anio, nota))
            buffer.clear()
        }
    }
    return lista
}

fun anadirABin() {
    val id = binPedirIdNuevo()
    val titulo = binPedirTextoValido("Título: ")
    val genero = binPedirTextoValido("Género: ")
    val anio = binPedirEnteroValido("Año: ", "Error: Debe ser un entero positivo.") { it > 0 }
    val nota = binPedirNotaValida("Nota (0.0 - 10.0): ")

    FileChannel.open(rutaBIN, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND).use { canal ->
        val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)
        buffer.putInt(id)

        val tituloBytes = titulo.padEnd(TAMANO_TITULO, ' ').toByteArray(charsetBIN)
        buffer.put(tituloBytes, 0, TAMANO_TITULO)

        val generoBytes = genero.padEnd(TAMANO_GENERO, ' ').toByteArray(charsetBIN)
        buffer.put(generoBytes, 0, TAMANO_GENERO)

        buffer.putInt(anio)
        buffer.putDouble(nota)

        buffer.flip()
        while (buffer.hasRemaining()) {
            canal.write(buffer)
        }
    }
    println("--- Videojuego con ID $id añadido correctamente.")
}

fun modificarEnBin() {
    val id = binPedirEnteroValido("ID a modificar: ", "ID no válido.")
    var encontrado = false

    FileChannel.open(rutaBIN, StandardOpenOption.READ, StandardOpenOption.WRITE).use { canal ->
        val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)
        while (canal.read(buffer) > 0 && !encontrado) {
            val posActual = canal.position()
            buffer.flip()

            val idActual = buffer.getInt()
            if (idActual == id) {
                encontrado = true

                val tituloBytes = ByteArray(TAMANO_TITULO)
                buffer.get(tituloBytes)
                val titulo = String(tituloBytes, charsetBIN).trim()

                println("Videojuego encontrado: $titulo")
                val nuevaNota = binPedirNotaValida("Nueva nota (0.0 - 10.0): ")

                val inicioRegistro = posActual - TAMANO_REGISTRO
                val posNota = inicioRegistro + TAMANO_ID + TAMANO_TITULO + TAMANO_GENERO + TAMANO_ANIO

                canal.position(posNota)
                val bufNota = ByteBuffer.allocate(TAMANO_NOTA)
                bufNota.putDouble(nuevaNota)
                bufNota.flip()
                while (bufNota.hasRemaining()) {
                    canal.write(bufNota)
                }
                println("--- Nota actualizada correctamente a $nuevaNota")
            }
            buffer.clear()
        }
    }

    if (!encontrado) {
        println("No se encontró ningún videojuego con el ID $id.")
    }
}

fun eliminarDeBin() {
    val id = binPedirEnteroValido("ID a eliminar: ", "ID no válido.")
    val tempPath = Path.of("$rutaBIN.tmp")
    var encontrado = false

    if (!Files.exists(rutaBIN)) return

    FileChannel.open(rutaBIN, StandardOpenOption.READ).use { cLectura ->
        FileChannel.open(tempPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { cEscritura ->
            val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)
            while (cLectura.read(buffer) > 0) {
                buffer.flip()
                val idActual = buffer.getInt()

                if (idActual == id) {
                    encontrado = true
                    val tituloBytes = ByteArray(TAMANO_TITULO)
                    buffer.get(tituloBytes)
                    val titulo = String(tituloBytes, charsetBIN).trim()
                    println("Encontrado: $titulo")
                } else {
                    buffer.rewind()
                    cEscritura.write(buffer)
                }
                buffer.clear()
            }
        }
    }

    if (encontrado) {
        val resp = binPedirTexto("¿Deseas eliminarlo de forma definitiva? (s/n): ")
        if (resp?.lowercase() == "s") {
            Files.move(tempPath, rutaBIN, StandardCopyOption.REPLACE_EXISTING)
            println("--- Registro eliminado con éxito.")
        } else {
            Files.deleteIfExists(tempPath)
            println("Eliminación cancelada.")
        }
    } else {
        Files.deleteIfExists(tempPath)
        println("No existe ningún registro con ID $id.")
    }
}

fun buscarBin(idBuscado: Int): Videojuego? {
    return leerBin().find { it.id == idBuscado }
}

private fun binPedirIdNuevo(): Int {
    while (true) {
        val id = binPedirEntero("ID: ")
        when {
            id == null || id <= 0 -> println("Error: El ID debe ser un número entero positivo.")
            buscarBin(id) != null -> println("Error: Ya existe un registro con ID $id.")
            else -> return id
        }
    }
}


// FUNCIONES AUXILIARES PRIVADAS.
private fun binPedirEnteroValido(mensaje: String, error: String, valido: (Int) -> Boolean = { true }): Int {
    while (true) {
        val numero = binPedirEntero(mensaje)
        if (numero != null && valido(numero)) return numero
        println(error)
    }
}

private fun binPedirTextoValido(mensaje: String): String {
    while (true) {
        val texto = binPedirTexto(mensaje)
        if (texto != null) return texto
        println("Error: este campo no puede estar vacío.")
    }
}

private fun binPedirNotaValida(mensaje: String): Double {
    while (true) {
        val nota = binPedirDecimal(mensaje)
        if (nota != null && nota in 0.0..10.0) return nota
        println("Error: la nota debe estar entre 0.0 y 10.0.")
    }
}

private fun binPedirTexto(mensaje: String): String? {
    print(mensaje)
    val texto = readlnOrNull()?.trim()
    return if (texto.isNullOrEmpty()) null else texto
}

private fun binPedirEntero(mensaje: String): Int? {
    print(mensaje)
    return readlnOrNull()?.trim()?.toIntOrNull()
}

private fun binPedirDecimal(mensaje: String): Double? {
    print(mensaje)
    return readlnOrNull()?.trim()?.replace(',', '.')?.toDoubleOrNull()
}