package com.example.calculaparajugar.ui

import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

internal object UiMotion {
    val easeOut = PathInterpolator(0.23f, 1f, 0.32f, 1f)
    val easeInOut = PathInterpolator(0.77f, 0f, 0.18f, 1f)
}

internal fun View.applySystemBarInsets() {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.setPadding(
            initialLeft + insets.left,
            initialTop + insets.top,
            initialRight + insets.right,
            initialBottom + insets.bottom
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}

internal fun View.applyPressFeedback(pressedScale: Float = 0.97f) {
    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> view.animate()
                .scaleX(pressedScale)
                .scaleY(pressedScale)
                .setDuration(110)
                .setInterpolator(UiMotion.easeOut)
                .start()

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .setInterpolator(UiMotion.easeOut)
                .start()
        }
        false
    }
}

internal fun View.playSheetEnter(distance: Float = 40f) {
    alpha = 0f
    translationY = distance
    animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(240)
        .setInterpolator(UiMotion.easeOut)
        .start()
}

internal fun View.playSoftPopEnter() {
    alpha = 0f
    scaleX = 0.96f
    scaleY = 0.96f
    animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(220)
        .setInterpolator(UiMotion.easeOut)
        .start()
}
