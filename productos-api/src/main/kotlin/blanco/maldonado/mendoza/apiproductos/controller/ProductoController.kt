package blanco.maldonado.mendoza.apiproductos.controller

import blanco.maldonado.mendoza.apiproductos.config.APIConfig
import blanco.maldonado.mendoza.apiproductos.dto.ProductoCreateDto
import blanco.maldonado.mendoza.apiproductos.dto.ProductoDto
import blanco.maldonado.mendoza.apiproductos.exceptions.ProductoBadRequestException
import blanco.maldonado.mendoza.apiproductos.exceptions.ProductoConflictIntegrityException
import blanco.maldonado.mendoza.apiproductos.exceptions.ProductoNotFoundException
import blanco.maldonado.mendoza.apiproductos.mapper.toDto
import blanco.maldonado.mendoza.apiproductos.mapper.toModel
import blanco.maldonado.mendoza.apiproductos.model.Producto
import blanco.maldonado.mendoza.apiproductos.model.TestDto
import blanco.maldonado.mendoza.apiproductos.repositories.ProductoCachedRepositoryImpl
import blanco.maldonado.mendoza.apiproductos.validator.validate
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

private val logger = KotlinLogging.logger { }

/**
 * @author Azahara Blanco, Sebastian Mendoza y Alfredo Maldonado
 * @since 20/02/2023
 *
 * Producto controller: Controlador de los endpoints de la api.
 *
 * @property repository
 * @constructor Create empty Producto controller.
 */
@RestController
@RequestMapping(APIConfig.API_PATH + "/productos")
class ProductoController
@Autowired constructor(
    private val repository: ProductoCachedRepositoryImpl
) {
    /**
     * Find all productos: Encuentra todos los productos.
     *
     * @return Devuelve todos los productos.
     */
    @Operation(summary = "Get all productos", description = "Obtiene una lista de productos", tags = ["Productos"])
    @ApiResponse(responseCode = "200", description = "Lista de Producto")
    @GetMapping("")
    suspend fun findAllProductos(): ResponseEntity<Flow<ProductoDto>> = withContext(Dispatchers.IO) {
        logger.info { "Get productos" }
        val res = repository.findAll().map { it.toDto() }
        return@withContext ResponseEntity.ok(res)
    }

    /**
     * Find product by id: Encuentra un producto si el id del producto existe.
     *
     * @param id
     * @return Devuelve un producto.
     */
    @Operation(summary = "Get Producto by ID", description = "Obtiene un objeto Test por su ID", tags = ["Test"])
    @Parameter(name = "id", description = "ID del Producto", required = true, example = "1")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado por su id")
    @ApiResponse(responseCode = "403", description = "No tienes permisos con ese id")
    @ApiResponse(responseCode = "401", description = "No autorizado con ese id")
    @ApiResponse(responseCode = "500", description = "Error interno con ese id")
    @ApiResponse(responseCode = "400", description = "Petición incorrecta con ese id")
    @GetMapping("/{id}")
    suspend fun findProductById(@PathVariable id: String): ResponseEntity<ProductoDto> =
        withContext(Dispatchers.IO) {
            logger.info { "Obteniendo producto por id" }
            try {
                val res = repository.findById(id)!!.toDto()
                return@withContext ResponseEntity.ok(res)
            } catch (e: ProductoNotFoundException) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
            }
        }

    /**
     * Find product by categoria: Encuentra un producto por su categoría si el id de la categoría existe.
     *
     * @param categoria
     * @return Devuelve un producto.
     */
    @Operation(summary = "Get Producto by Category", description = "Modifica un objeto Producto", tags = ["Producto"])
    @Parameter(name = "categoria", description = "ID de la categoría", required = true, example = "1")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado por su id")
    @ApiResponse(responseCode = "403", description = "No tienes permisos con ese id")
    @ApiResponse(responseCode = "401", description = "No autorizado con ese id")
    @ApiResponse(responseCode = "500", description = "Error interno con ese id")
    @ApiResponse(responseCode = "400", description = "Petición incorrecta con ese id")
    @GetMapping("/categoria/{categoria}")
    suspend fun findProductByCategoria(@PathVariable categoria: String): ResponseEntity<Flow<ProductoDto>> =
        withContext(Dispatchers.IO) {
            logger.info { "Get productos by categoria" }
            try {
                if (!categoria.trim().isEmpty()) {
                    try {
                        Producto.Categoria.valueOf(categoria.trim())
                        val res = repository.findByCategoria(categoria.trim()).map { it.toDto() }
                        if (!res.toList().isEmpty()) {
                            return@withContext ResponseEntity.ok(res)
                        } else {
                            throw ProductoNotFoundException("La categoria $categoria es correcta pero no tiene productos asociados.")
                        }
                    } catch (e: IllegalArgumentException) {
                        throw ProductoNotFoundException("La categoria $categoria no es correcta.")
                    }
                } else {
                    throw ProductoNotFoundException("La categoria esta vacia.")
                }
            } catch (e: ProductoNotFoundException) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
            }
        }


    /**
     * Find product by nombre: Encuentra un producto por su nombre si el id del nombre existe en la base de datos.
     *
     * @param nombre
     * @return Devuelve un producto.
     */
    @Operation(
        summary = "Get Producto by Nombre",
        description = "Obtiene un objeto Producto por el nombre",
        tags = ["Producto"]
    )
    @Parameter(name = "nombre", description = "ID del nombre", required = true, example = "1")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado por su id")
    @ApiResponse(responseCode = "403", description = "No tienes permisos con ese id")
    @ApiResponse(responseCode = "401", description = "No autorizado con ese id")
    @ApiResponse(responseCode = "500", description = "Error interno con ese id")
    @ApiResponse(responseCode = "400", description = "Petición incorrecta con ese id")
    @GetMapping("/nombre/{nombre}")
    suspend fun findProductByNombre(@PathVariable nombre: String): ResponseEntity<Flow<ProductoDto>> =
        withContext(Dispatchers.IO) {
            logger.info { "Obteniendo producto por nombre" }
            try {
                val res = repository.findByNombre(nombre.trim()).map { it.toDto() }
                if (res.toList().isEmpty() || nombre.trim().isEmpty()) {
                    throw ProductoNotFoundException("No se ha encontrado ningún producto con el nombre $nombre")
                }
                return@withContext ResponseEntity.ok(res)
            } catch (e: ProductoNotFoundException) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
            }
        }


    /**
     * Result nombre nulo: Función que lanza una excepción 404 cuando no se introduce ningún nombre.
     *
     * @return Devuelve una excepción 404 porque el producto introducido no existe.
     */
    @Operation(
        summary = "Name is null",
        description = "Obtiene una excepción porque el nombre del producto introducido no existe",
        tags = ["Producto"]
    )
    @ApiResponse(responseCode = "404", description = "Producto no encontrado por nombre nulo")
    @GetMapping("/nombre/")
    suspend fun resultNombreNulo(): ResponseEntity<Flow<ProductoDto>> =
        withContext(Dispatchers.IO) {
            logger.info { "Obteniendo producto por nombre nulo" }
            try {
                throw ProductoNotFoundException("El nombre que ha introducido es nulo.")
            } catch (e: ProductoNotFoundException) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
            }
        }

    /**
     * Result categoria nula: Función que lanza una excepción 404 cuando no se introduce ninguna categoría.
     *
     * @return Devuelve una excepción 404 porque la categoría introducida no existe.
     */
    @Operation(
        summary = "Categoria is null",
        description = "Obtiene una excepción porque la categoría introducida del producto no existe",
        tags = ["Producto"]
    )
    @ApiResponse(responseCode = "404", description = "Producto no encontrado por categoria nula")
    @GetMapping("/categoria/")
    suspend fun resultCategoriaNula(): ResponseEntity<Flow<ProductoDto>> =
        withContext(Dispatchers.IO) {
            logger.info { "Obteniendo producto por categoria nula" }
            try {
                throw ProductoNotFoundException("La categoria que ha introducido es nula.")
            } catch (e: ProductoNotFoundException) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
            }
        }

    /**
     * Create product: Crea un producto si el producto no existe en la base de datos con código 201.
     *
     * @param productoDto
     * @return Devuelve el producto añadido con código 201.
     */
    @Operation(summary = "Create Product", description = "Crea un objeto Producto", tags = ["Producto"])
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @PostMapping("")
    suspend fun createProduct(@Valid @RequestBody productoDto: ProductoCreateDto): ResponseEntity<ProductoDto> {
        logger.info { "Creando un producto" }
        productoDto.activo.lowercase()
        checkProducto(productoDto)
        try {
            val p = productoDto.validate().toModel()
            val res = repository.save(p).toDto()
            return ResponseEntity.status(HttpStatus.CREATED).body(res)
        } catch (e: ProductoBadRequestException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
    }

    /**
     * Update product: Modifica un producto si el id del producto existe en la base de datos, con código 200.
     *
     * @param id
     * @param productoDto
     * @return Devuelve el producto modificado.
     */
    @Operation(summary = "Update Product", description = "Modifica un objeto Product", tags = ["Product"])
    @Parameter(name = "id", description = "ID del Producto", required = true, example = "1")
    @ApiResponse(responseCode = "200", description = "Producto modificado")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado con ese id")
    @PutMapping("/{id}")
    suspend fun updateProduct(
        @PathVariable id: String, @Valid @RequestBody productoDto: ProductoCreateDto
    ): ResponseEntity<ProductoDto> = withContext(Dispatchers.IO) {
        logger.info { "Modificando producto con id $id" }
        try {
            val p = productoDto.validate().toModel()
            val res = repository.update(id, p)!!.toDto()
            return@withContext ResponseEntity.status(HttpStatus.OK).body(res)
        } catch (e: ProductoNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
        } catch (e: ProductoBadRequestException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
    }

    /**
     * Delete product: Borra el producto si el id existe en la base de datos.
     *
     * @param id
     * @return Devuelve el producto borrado.
     */
    @Operation(summary = "Delete Product", description = "Elimina un objeto Producto", tags = ["Product"])
    @Parameter(name = "id", description = "ID del Producto", required = true, example = "1")
    @ApiResponse(responseCode = "204", description = "Producto eliminado")
    @ApiResponse(responseCode = "404", description = "Error al eliminar el producto con ese id")
    @DeleteMapping("/{id}")
    suspend fun deleteProduct(@PathVariable id: String): ResponseEntity<ProductoDto> = withContext(Dispatchers.IO) {
        logger.info { "Borrando producto" }
        try {
            repository.delete(id)
            return@withContext ResponseEntity.noContent().build()
        } catch (e: ProductoNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
        } catch (e: ProductoConflictIntegrityException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, e.message)
        }

    }

    /**
     * Check producto: Función que comprueba si el nombre del producto ya existe en la base de datos.
     *
     * @param producto
     */
    private suspend fun checkProducto(producto: ProductoCreateDto) {
        val existe = repository.findByNombre(producto.nombre)
        if (existe.toList().isNotEmpty()) {
            throw ProductoBadRequestException("El producto con nombre ${producto.nombre} ya existe")
        }
    }

    /**
     * Get all:
     *
     * @param texto
     * @return
     */
    @GetMapping("/test")
    fun getAll(@RequestParam texto: String?): ResponseEntity<List<TestDto>> {
        logger.info { "GET ALL Test" }
        return ResponseEntity.ok(listOf(TestDto("Hola : Query: $texto"), TestDto("Mundo : Query: $texto")))
    }


}