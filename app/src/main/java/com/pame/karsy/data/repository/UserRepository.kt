package com.pame.karsy.data.repository

import com.pame.karsy.core.session.UserMode
import com.pame.karsy.data.model.User

/**
 * Perfiles estáticos según el tipo de usuario elegido en el login.
 * TODO: leer la cuenta en sesión desde public.cuentas (y perfiles_lote si aplica).
 */
object UserRepository {

    private val particular = User(
        id = "u-particular",
        displayName = "Pamela Rodríguez",
        email = "pamela@correo.com",
        phone = "+52 55 1234 5678",
        location = "Ciudad de México, CDMX",
        bio = "Amante de los autos. Vendo y compro vehículos en excelente estado.",
        avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&auto=format",
        memberSince = "Marzo 2024",
        accountType = "particular",
    )

    private val lote = User(
        id = "u-lote",
        displayName = "Auto Premium",
        email = "contacto@autopremium.mx",
        phone = "+52 33 9876 5432",
        location = "Guadalajara, Jalisco",
        bio = "Lote de seminuevos certificados. Financiamiento y garantía en todos nuestros vehículos.",
        avatarUrl = null,
        memberSince = "Enero 2023",
        accountType = "lote",
    )

    private val admin = User(
        id = "u-admin",
        displayName = "Administrador Karsy",
        email = "admin@karsy.mx",
        phone = "+52 55 0000 0000",
        location = "Ciudad de México, CDMX",
        bio = "Cuenta de administración de la plataforma.",
        avatarUrl = null,
        memberSince = "Enero 2023",
        accountType = "particular",
    )

    fun currentUser(mode: UserMode): User? = when (mode) {
        UserMode.VISITANTE -> null
        UserMode.PARTICULAR -> particular
        UserMode.LOTE -> lote
        UserMode.ADMIN -> admin
    }
}
