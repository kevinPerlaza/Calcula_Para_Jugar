package com.example.calculaparajugar

import android.app.Application
import com.example.calculaparajugar.data.QuestionBank

/**
 * Clase Application principal.
 * Carga el banco de preguntas al iniciar.
 */
class CalculaParaJugarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Cargar las 800 preguntas desde assets
        QuestionBank.load(this)
    }
}
