package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.example.R

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Light),
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_bold, FontWeight.ExtraBold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val MonoTechnicalStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.4.sp,
    textDirection = TextDirection.Ltr
)

val MonoDialpadStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = 1.sp,
    textDirection = TextDirection.Ltr
)

private val PersianLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

@Suppress("DEPRECATION")
private val PersianPlatformStyle = PlatformTextStyle(includeFontPadding = true)

/**
 * Persian-tuned Material 3 Typography:
 * - Strict 0.sp letterSpacing so cursive Persian/Arabic glyph joins never break.
 * - Generous lineHeight (+3sp..+5sp) and includeFontPadding = true so tall ascenders
 *   and stacked Persian dots (پ، چ، ژ، گ، ی) never clip or collide across lines.
 * - Explicit TextDirection.Rtl so mixed Persian + Latin/numeric strings anchor cleanly right-to-left.
 */
private val PersianTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 54.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    displayMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    displaySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    headlineLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    headlineMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    headlineSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.5.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    titleLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.5.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    titleSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    bodyLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    bodySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    labelLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    labelMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    ),
    labelSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        lineHeightStyle = PersianLineHeightStyle,
        platformStyle = PersianPlatformStyle
    )
)

private val LatinTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 52.sp,
        textDirection = TextDirection.Ltr
    ),
    displayMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 44.sp,
        textDirection = TextDirection.Ltr
    ),
    displaySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 38.sp,
        textDirection = TextDirection.Ltr
    ),
    headlineLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
        textDirection = TextDirection.Ltr
    ),
    headlineMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        textDirection = TextDirection.Ltr
    ),
    headlineSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp,
        textDirection = TextDirection.Ltr
    ),
    titleLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 27.sp,
        textDirection = TextDirection.Ltr
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        textDirection = TextDirection.Ltr
    ),
    titleSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        textDirection = TextDirection.Ltr
    ),
    bodyLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        textDirection = TextDirection.Ltr
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 22.sp,
        textDirection = TextDirection.Ltr
    ),
    bodySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        textDirection = TextDirection.Ltr
    ),
    labelLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        textDirection = TextDirection.Ltr
    ),
    labelMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        textDirection = TextDirection.Ltr
    ),
    labelSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        textDirection = TextDirection.Ltr
    )
)

fun typographyForLanguage(isPersian: Boolean): Typography =
    if (isPersian) PersianTypography else LatinTypography

val ITellTypography: Typography = PersianTypography
