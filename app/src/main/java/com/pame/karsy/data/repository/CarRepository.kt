package com.pame.karsy.data.repository

import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.CarDetail

/**
 * Datos estáticos copiados de los mockups.
 * TODO: reemplazar por consultas a Supabase:
 *  - featuredCars  -> publicaciones con solicitudes_destacado aprobadas y vigentes
 *  - allCars       -> publicaciones activas y habilitadas (paginadas, con filtros)
 *  - favoriteCars  -> favoritos del usuario en sesión
 *  - getDetail     -> publicación + fotos_publicacion + datos de contacto del propietario
 */
object CarRepository {

    private fun img(id: String, w: Int, h: Int) =
        "https://images.unsplash.com/photo-$id?w=$w&h=$h&fit=crop&auto=format"

    val featuredCars: List<Car> = listOf(
        Car(1, "BMW", "Serie 3 320i", 2023, "$685,000",
            "Sedán premium con tecnología deportiva, pantalla 12.3'' y asistente de manejo.",
            img("1783740486439-b4487fad649f", 700, 440), badge = "Destacado", rating = 4.8),
        Car(2, "Porsche", "Cayenne S", 2022, "$1,450,000",
            "SUV de alto rendimiento, 440 CV, interior en piel Nappa y techo panorámico.",
            img("1763165524637-9067debdc80b", 700, 440), badge = "Premium", condition = "Usado", rating = 4.9),
        Car(3, "Mercedes-Benz", "GLA 200", 2023, "$798,000",
            "Crossover compacto con MBUX, cámara 360° y acabados de lujo de serie.",
            img("1788178243401-854e12eeb8b1", 700, 440), badge = "Seminuevo", rating = 4.7),
        Car(4, "Audi", "A4 2.0 TFSI", 2022, "$730,000",
            "Sedán ejecutivo con Virtual Cockpit, tracción quattro y faros Matrix LED.",
            img("1764013290175-2b76e9a00b2e", 700, 440), badge = "Destacado", rating = 4.8),
    )

    val allCars: List<Car> = listOf(
        Car(5, "Toyota", "Corolla LE", 2022, "$325,000",
            "Sedán confiable, eficiente en combustible con seguridad Toyota Safety Sense.",
            img("1610809589386-9ea41901eb54", 500, 320), condition = "Usado", rating = 4.5),
        Car(6, "Honda", "Civic Sport", 2021, "$298,000",
            "Compacto deportivo con pantalla táctil de 7'', carplay y frenos ABS.",
            img("1629538745524-5b748fddac9f", 500, 320), condition = "Usado", rating = 4.6),
        Car(7, "Mazda", "3 Sedán i Sport", 2023, "$365,000",
            "Diseño KODO premiado, motor Skyactiv-G y sistema de sonido Bose.",
            img("1522770450359-3de04ff5c9e2", 500, 320)),
        Car(8, "Nissan", "Versa Advance", 2022, "$245,000",
            "El sedán más vendido de México, económico y con excelente maniobrabilidad.",
            img("1602791036370-b00a495d8a58", 500, 320), condition = "Usado", rating = 4.4),
        Car(9, "Volkswagen", "Jetta Trendline", 2020, "$280,000",
            "Sedán alemán de clase media con acabados de primera y gran espacio interior.",
            img("1647588854348-f3b37a0f0ef7", 500, 320), condition = "Usado", rating = 4.5),
        Car(10, "Kia", "Forte EX", 2023, "$340,000",
            "Diseño dinámico, garantía 7 años, climatizador automático y control crucero.",
            img("1770936044591-979681c051cf", 500, 320), rating = 4.6),
    )

    /** Favoritos de ejemplo (mismos autos que en el mockup de favoritos). */
    val favoriteCars: List<Car>
        get() = listOf(1, 2, 3, 5, 6, 4).mapNotNull { getCar(it) }

    /** Fotos cuadradas que aparecen en la cuadrícula del perfil. */
    val profilePostImages: List<String> = listOf(
        "1610809589386-9ea41901eb54", "1629538745524-5b748fddac9f", "1580273916550-e323be2ae537",
        "1767749995450-7b63ab7cd4fd", "1758411898152-5fde4b5eef56", "1602791036370-b00a495d8a58",
        "1571987502227-9231b837d92a", "1522770450359-3de04ff5c9e2", "1647588854348-f3b37a0f0ef7",
        "1770936044591-979681c051cf", "1783740486439-b4487fad649f", "1788178243401-854e12eeb8b1",
    ).map { img(it, 220, 220) }

    /** Imágenes del carrusel de la pantalla de bienvenida. */
    val welcomeCarousel: List<String> = listOf(
        "1767749995450-7b63ab7cd4fd", "1580273916550-e323be2ae537",
        "1571987502227-9231b837d92a", "1758411898152-5fde4b5eef56",
    ).map { img(it, 600, 900) }

    fun getCar(id: Int): Car? = (featuredCars + allCars).firstOrNull { it.id == id }

    /** Equivalente a enrichCar() del mockup: completa la ficha con datos de ejemplo. */
    fun getDetail(id: Int): CarDetail? {
        val car = getCar(id) ?: return null
        return CarDetail(
            car = car,
            gallery = listOf(
                car.imageUrl,
                img("1580273916550-e323be2ae537", 700, 440),
                img("1571987502227-9231b837d92a", 700, 440),
                img("1758411898152-5fde4b5eef56", 700, 440),
            ),
            transmision = "Automática",
            kilometraje = "45,000 km",
            cilindros = "4",
            caballos = "158 HP",
            tipoCarro = "Sedán",
            color = "Blanco perla",
            cantDuenos = "1",
            contactoNombre = "Pamela Rodríguez",
            contactoTelefono = "+52 55 1234 5678",
            contactoCorreo = "pamela@correo.com",
            detalles = "Sin golpes ni rayones visibles. Pintura original. Mantenimiento al día en agencia. Llantas nuevas, factura original.",
            descripcionLarga = car.description,
        )
    }
}
