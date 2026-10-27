import java.nio.file.Files
import java.nio.file.Path


// CRUD
fun listarVideojuegos(lista: List<Videojuego>) {
    if (lista.isEmpty()) {
        println("El catálogo está vacío.")
        return
    }

    println("%-4s %-42s %-12s %-6s %-5s".format("ID", "Título", "Género", "Año", "Nota"))
    println("-".repeat(73))
    for (v in lista) {
        println("%-4s %-42s %-12s %-6s %-5s".format(v.id, v.titulo, v.genero, v.anio, v.nota))
    }
}

fun buscarPorId(lista: List<Videojuego>, id: Int) {
    val encontrado = lista.find { it.id == id }

    if (encontrado == null) {
        println("No existe ningún videojuego con ID $id.")
    } else {
        println("ID: ${encontrado.id}")
        println("Título: ${encontrado.titulo}")
        println("Género: ${encontrado.genero}")
        println("Año: ${encontrado.anio}")
        println("Nota: ${encontrado.nota}")
    }
}

fun añadirVideojuego(lista: List<Videojuego>): List<Videojuego> {
    val titulo = pedirTexto("Título: ")
    if (titulo == null) {
        println("Error: el título no puede estar vacío.")
        return lista
    }

    val genero = pedirTexto("Género: ")
    if (genero == null) {
        println("Error: el género no puede estar vacío.")
        return lista
    }

    val anio = pedirEntero("Año: ")
    if (anio == null || anio <= 0) {
        println("Error: el año debe ser un número entero positivo.")
        return lista
    }

    val nota = pedirDecimal("Nota (0.0 - 10.0): ")
    if (nota == null || nota !in 0.0..10.0) {
        println("Error: la nota debe ser un número entre 0.0 y 10.0.")
        return lista
    }

    val nuevoId = (lista.maxOfOrNull { it.id } ?: 0) + 1
    val nuevo = Videojuego(nuevoId, titulo, genero, anio, nota)

    println("Videojuego añadido con ID $nuevoId.")
    return lista + nuevo
}

fun modificarNota(lista: List<Videojuego>, id: Int, nuevaNota: Double): List<Videojuego> {
    if (nuevaNota !in 0.0..10.0) {
        println("Error: la nota debe estar entre 0.0 y 10.0.")
        return lista
    }

    if (lista.none { it.id == id }) {
        println("No existe ningún videojuego con ID $id.")
        return lista
    }

    println("Nota del videojuego $id actualizada a $nuevaNota.")
    return lista.map { if (it.id == id) it.copy(nota = nuevaNota) else it }
}

fun eliminarVideojuego(lista: List<Videojuego>, id: Int): List<Videojuego> {
    val videojuego = lista.find { it.id == id }
    if (videojuego == null) {
        println("No existe ningún videojuego con ID $id.")
        return lista
    }

    val respuesta = pedirTexto("¿Seguro que quieres eliminar \"${videojuego.titulo}\"? (s/n): ")
    if (respuesta?.lowercase() != "s") {
        println("Eliminación cancelada.")
        return lista
    }

    println("Videojuego \"${videojuego.titulo}\" eliminado.")
    return lista.filter { it.id != id }
}

fun integrarCatalogo(
    actual: List<Videojuego>,
    nuevos: List<Videojuego>
): Pair<List<Videojuego>, Int> {
    val idsExistentes = actual.map { it.id }.toSet()
    val aIntegrar = nuevos
        .filter { it.id !in idsExistentes }
        .distinctBy { it.id }

    return Pair(actual + aIntegrar, aIntegrar.size)
}

fun convertir(
    origen: Path,
    destino: Path,
    leer: (Path) -> List<Videojuego>,
    escribir: (Path, List<Videojuego>) -> Unit
) {
    if (!Files.exists(origen)) {
        println("Error: el fichero origen no existe: $origen")
        return
    }

    val datos = leer(origen)
    if (datos.isEmpty()) {
        println("No se ha generado ningún fichero: el origen no contiene registros válidos.")
        return
    }

    try {
        escribir(destino, datos)
        println("Fichero generado: $destino")
        println("Registros convertidos: ${datos.size}")
    } catch (e: Exception) {
        println("Error al escribir el fichero $destino: ${e.message}")
    }
}
