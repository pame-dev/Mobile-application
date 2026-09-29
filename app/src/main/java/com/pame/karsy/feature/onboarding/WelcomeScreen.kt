package com.pame.karsy.feature.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.R
import com.pame.karsy.core.components.KarsyLogo
import com.pame.karsy.core.locale.Idioma
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.repository.CarRepository
import kotlinx.coroutines.delay

private val WelcomeLoginBlue = Color(0xFF56799F)

/** Pantalla de bienvenida: carrusel de fotos a pantalla completa y accesos principales. */
@Composable
fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit, onGuest: () -> Unit) {
    val images = CarRepository.welcomeCarousel
    var current by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val lang = remember { Idioma.actual(context) }

    // Avanza el carrusel cada 5 s (igual que la animación del mockup).
    LaunchedEffect(current) {
        delay(5000)
        current = (current + 1) % images.size
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyNavy)
    ) {
        // Fotos apiladas con fundido y zoom suave
        images.forEachIndexed { i, url ->
            val visible = i == current
            val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(1200), label = "slideAlpha")
            val scale by animateFloatAsState(if (visible) 1f else 1.04f, tween(1600), label = "slideScale")
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
                    .scale(scale)
            )
        }

        // Degradado navy desde abajo
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.28f to KarsyNavy.copy(alpha = 0.1f),
                        0.5f to KarsyNavy.copy(alpha = 0.75f),
                        0.72f to KarsyNavy,
                        1f to KarsyNavy,
                    )
                )
        )

        // Encabezado: logo + selector de idioma
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KarsyLogo(size = 36.dp)
                Text(
                    "Karsy",
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = KarsyWhite,
                    letterSpacing = 0.04.em
                )
            }
            // Guarda el idioma y recrea la pantalla: toda la app se vuelve a dibujar en ese idioma.
            LanguageToggle(lang = lang, onChange = { code ->
                context.findActivity()?.let { Idioma.cambiar(it, code) }
            })
        }

        // Contenido inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CarouselDots(count = images.size, current = current, onSelect = { current = it })
            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(R.string.welcome_headline),
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.01).em,
                color = KarsyWhite,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            WelcomeButton(stringResource(R.string.welcome_login), WelcomeLoginBlue, 1.5f, onLogin)
            Spacer(Modifier.height(12.dp))
            WelcomeButton(stringResource(R.string.welcome_register), KarsyNavy, 1f, onRegister)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.welcome_guest),
                fontFamily = DmSans,
                fontSize = 14.sp,
                color = KarsyMid,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onGuest)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
            val termsPrefix = stringResource(R.string.welcome_terms_prefix)
            val termsLink = stringResource(R.string.welcome_terms_link)
            Text(
                buildAnnotatedString {
                    append(termsPrefix)
                    withStyle(SpanStyle(color = KarsyTeal, textDecoration = TextDecoration.Underline)) {
                        append(termsLink)
                    }
                },
                fontFamily = DmSans,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = KarsyMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    // TODO: abrir la pantalla/enlace de Términos y Condiciones
                }
            )
        }
    }
}

@Composable
private fun WelcomeButton(text: String, container: Color, borderWidth: Float, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(borderWidth.dp, KarsyWhite),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = KarsyWhite),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(text, fontFamily = Outfit, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.01.em)
    }
}

@Composable
private fun CarouselDots(count: Int, current: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            val width by animateDpAsState(if (active) 20.dp else 8.dp, tween(300), label = "dotWidth")
            Box(
                Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) KarsyWhite else KarsyWhite.copy(alpha = 0.45f))
                    .clickable { onSelect(i) }
            )
        }
    }
}

/**
 * Selector de idioma. Tocar la píldora (o la bandera) alterna ES ↔ EN;
 * tocar "ES" o "EN" elige ese idioma directamente.
 */
@Composable
private fun LanguageToggle(lang: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(KarsyWhite.copy(alpha = 0.12f))
            .clickable { onChange(if (lang == Idioma.ES) Idioma.EN else Idioma.ES) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (lang == Idioma.EN) UkFlag() else SpainFlag()
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf(Idioma.ES, Idioma.EN).forEachIndexed { i, code ->
                if (i > 0) Text("|", color = KarsyWhite.copy(alpha = 0.3f), fontSize = 11.sp)
                val active = lang == code
                Text(
                    code.uppercase(),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) KarsyWhite else KarsyWhite.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { onChange(code) }
                        .padding(horizontal = 6.dp)
                )
            }
        }
    }
}

/** Busca la Activity detrás del contexto de Compose (para poder recrearla al cambiar de idioma). */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Bandera de España dibujada con franjas (rojo - amarillo - rojo). */
@Composable
private fun SpainFlag() {
    val red = Color(0xFFC60B1E)
    Column(
        Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        Box(Modifier.fillMaxWidth().weight(1f).background(red))
        Box(Modifier.fillMaxWidth().weight(2f).background(Color(0xFFFFC400)))
        Box(Modifier.fillMaxWidth().weight(1f).background(red))
    }
}

/** Bandera del Reino Unido (Union Jack) dibujada con Canvas. */
@Composable
private fun UkFlag() {
    val blue = Color(0xFF012169)
    val red = Color(0xFFC8102E)
    Canvas(
        Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        val w = size.width
        val h = size.height
        drawRect(blue)

        // Diagonales: blanca ancha y roja delgada encima
        drawLine(Color.White, Offset(0f, 0f), Offset(w, h), strokeWidth = h * 0.2f)
        drawLine(Color.White, Offset(w, 0f), Offset(0f, h), strokeWidth = h * 0.2f)
        drawLine(red, Offset(0f, 0f), Offset(w, h), strokeWidth = h * 0.07f)
        drawLine(red, Offset(w, 0f), Offset(0f, h), strokeWidth = h * 0.07f)

        // Cruz central: blanca ancha y roja encima
        val white = h / 3f
        drawRect(Color.White, Offset(0f, (h - white) / 2f), Size(w, white))
        drawRect(Color.White, Offset((w - white) / 2f, 0f), Size(white, h))
        val cross = h / 5f
        drawRect(red, Offset(0f, (h - cross) / 2f), Size(w, cross))
        drawRect(red, Offset((w - cross) / 2f, 0f), Size(cross, h))
    }
}
