package com.example.calculaparajugar.ui

import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewGroup
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur

/**
 * Helper para aplicar efecto Liquid Glass real usando BlurView.
 *
 * Aplica blur al contenido detrás de una vista, creando
 * el efecto de vidrio translúcido real.
 */
object GlassBlurHelper {

    /**
     * Configura un BlurView con efecto glass.
     * @param blurView La vista BlurView del layout
     * @param activity Activity actual
     * @param blurRadius Radio del blur (6-25 recomendado)
     * @param overlayColor Color superpuesto al blur (tinte del vidrio)
     */
    fun setupGlass(
        blurView: BlurView,
        activity: Activity,
        blurRadius: Float = 12f,
        overlayColor: Int = 0x99121218.toInt()
    ) {
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffectBlur()
        } else {
            RenderScriptBlur(activity)
        }

        blurView.setupWith(rootView, blurAlgorithm)
            .setBlurRadius(blurRadius)
            .setOverlayColor(overlayColor)
            .setBlurAutoUpdate(true)
    }

    /**
     * Aplica blur nativo a una View (API 31+).
     * Fallback: no hace nada en versiones anteriores.
     */
    fun applyNativeBlur(view: View, radius: Float = 10f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            )
        }
    }
}
