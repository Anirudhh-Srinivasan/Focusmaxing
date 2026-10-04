package com.topdawg.focusmaxxing.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.topdawg.focusmaxxing.R
import androidx.compose.ui.unit.sp

val PixelHeadingFont = FontFamily(Font(R.font.press_start_2p))
@OptIn(ExperimentalTextApi::class)
val InterFont = FontFamily(
    Font(R.font.inter, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 36.sp),
    displayMedium = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 32.sp),
    displaySmall = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp),
    headlineLarge = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    headlineMedium = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    headlineSmall = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 20.sp),
    titleLarge = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 18.sp),
    titleMedium = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 16.sp),
    titleSmall = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 9.sp, lineHeight = 14.sp),
    bodyLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = PixelHeadingFont, fontWeight = FontWeight.Normal, fontSize = 9.sp, lineHeight = 14.sp),
    labelMedium = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)
