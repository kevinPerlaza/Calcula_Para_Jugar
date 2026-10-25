package com.example.calculaparajugar.util

import com.example.calculaparajugar.R

/**
 * "Neuri" - mascota tipo criatura cerebral que evoluciona con el nivel.
 *
 * Evoluciona visualmente: empieza como una gota dormida y termina
 * como un ser con aura, corona y alas de energía.
 */
object MascotManager {

    data class MascotStage(
        val drawableRes: Int,
        val name: String,
        val title: String,
        val description: String,
        val minLevel: Int
    )

    private val stages = listOf(
        MascotStage(R.drawable.mascot_stage_1, "Neuri",
            "Semilla", "Dormido. Respondé para despertarlo", 1),
        MascotStage(R.drawable.mascot_stage_2, "Neuri",
            "Despierto", "¡Abrió los ojos! Quiere aprender", 3),
        MascotStage(R.drawable.mascot_stage_3, "Neuri",
            "Curioso", "Le crecieron antenas. Explora todo", 6),
        MascotStage(R.drawable.mascot_stage_4, "Neuri",
            "Estudioso", "Se graduó. Cada vez más listo", 10),
        MascotStage(R.drawable.mascot_stage_5, "Neuri",
            "Sabio", "Brilla con conocimiento", 15),
        MascotStage(R.drawable.mascot_stage_6, "Neuri",
            "Maestro", "Poder completo. Alas de energía", 20)
    )

    fun getStageForLevel(level: Int): MascotStage {
        return stages.lastOrNull { level >= it.minLevel } ?: stages.first()
    }

    fun getNextStage(level: Int): MascotStage? {
        val currentIdx = stages.indexOfLast { level >= it.minLevel }
        return if (currentIdx < stages.size - 1) stages[currentIdx + 1] else null
    }

    fun getLevelsToNextEvolution(level: Int): Int {
        val next = getNextStage(level) ?: return 0
        return next.minLevel - level
    }

    fun getStageIndex(level: Int): Int {
        return stages.indexOfLast { level >= it.minLevel }.coerceAtLeast(0)
    }

    fun getAllStages(): List<MascotStage> = stages
}
