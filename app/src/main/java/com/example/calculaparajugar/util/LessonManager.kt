package com.example.calculaparajugar.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Sistema de aprendizaje con repetición espaciada (Leitner simplificado).
 *
 * Cada "Lesson" tiene:
 * - Contenido educativo (lo que se enseña)
 * - Pregunta vinculada (para evaluar)
 *
 * Flujo:
 * 1. Se muestra el contenido educativo (sin pregunta)
 * 2. En el siguiente ciclo, se hace la pregunta
 * 3. Si acierta → avanza a la siguiente lección
 * 4. Si falla 2+ veces → se re-muestra el contenido
 *
 * Estado por lección:
 * - UNSEEN: nunca mostrada
 * - TAUGHT: contenido mostrado, pendiente de pregunta
 * - MASTERED: respondida correctamente
 * - REVIEW: falló 2+ veces, necesita re-enseñar
 */
object LessonManager {

    private const val PREFS = "brainwall_lessons"
    private const val KEY_CURRENT_INDEX = "current_lesson_index_"
    private const val KEY_STATE = "lesson_state_"
    private const val KEY_FAIL_COUNT = "lesson_fails_"
    private const val KEY_SHOW_MODE = "next_show_mode_"

    enum class LessonState { UNSEEN, TAUGHT, MASTERED, REVIEW }
    enum class ShowMode { LESSON, QUESTION }

    data class LessonContent(
        val id: String,
        val title: String,
        val explanation: String,
        val example: String,
        val questionText: String,
        val options: List<String>,
        val correctAnswer: String,
        val category: QuestionGenerator.Category
    )

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /**
     * Determina qué mostrar: lección o pregunta.
     */
    fun getNextShowMode(context: Context, category: QuestionGenerator.Category): ShowMode {
        val key = KEY_SHOW_MODE + category.name
        val mode = prefs(context).getString(key, ShowMode.LESSON.name)
        return try { ShowMode.valueOf(mode!!) } catch (e: Exception) { ShowMode.LESSON }
    }

    /**
     * Obtiene la lección actual para una categoría.
     */
    fun getCurrentLesson(context: Context, category: QuestionGenerator.Category): LessonContent {
        val lessons = getLessonsForCategory(category)
        val index = getCurrentIndex(context, category)
        return lessons[index.coerceIn(0, lessons.size - 1)]
    }

    /**
     * Marca que se mostró la lección (contenido educativo).
     * La próxima vez se mostrará la pregunta.
     */
    fun onLessonShown(context: Context, category: QuestionGenerator.Category) {
        val key = KEY_SHOW_MODE + category.name
        val lesson = getCurrentLesson(context, category)
        prefs(context).edit()
            .putString(key, ShowMode.QUESTION.name)
            .putString(KEY_STATE + lesson.id, LessonState.TAUGHT.name)
            .apply()
    }

    /**
     * Registra respuesta correcta a la pregunta.
     * Avanza a la siguiente lección.
     */
    fun onCorrectAnswer(context: Context, category: QuestionGenerator.Category) {
        val lesson = getCurrentLesson(context, category)
        val lessons = getLessonsForCategory(category)
        val index = getCurrentIndex(context, category)
        val nextIndex = (index + 1).coerceAtMost(lessons.size - 1)

        prefs(context).edit()
            .putString(KEY_STATE + lesson.id, LessonState.MASTERED.name)
            .putInt(KEY_CURRENT_INDEX + category.name, nextIndex)
            .putString(KEY_SHOW_MODE + category.name, ShowMode.LESSON.name)
            .putInt(KEY_FAIL_COUNT + lesson.id, 0)
            .apply()
    }

    /**
     * Registra respuesta incorrecta.
     * Si falla 2+ veces → la próxima vez se re-muestra la lección.
     */
    fun onIncorrectAnswer(context: Context, category: QuestionGenerator.Category) {
        val lesson = getCurrentLesson(context, category)
        val fails = prefs(context).getInt(KEY_FAIL_COUNT + lesson.id, 0) + 1

        val nextMode = if (fails >= 2) ShowMode.LESSON else ShowMode.QUESTION

        prefs(context).edit()
            .putInt(KEY_FAIL_COUNT + lesson.id, fails)
            .putString(KEY_SHOW_MODE + category.name, nextMode.name)
            .putString(KEY_STATE + lesson.id, if (fails >= 2) LessonState.REVIEW.name else LessonState.TAUGHT.name)
            .apply()
    }

    private fun getCurrentIndex(context: Context, category: QuestionGenerator.Category): Int {
        return prefs(context).getInt(KEY_CURRENT_INDEX + category.name, 0)
    }

    fun getProgressInfo(context: Context, category: QuestionGenerator.Category): Pair<Int, Int> {
        val index = getCurrentIndex(context, category)
        val total = getLessonsForCategory(category).size
        return index to total
    }

    /**
     * Establece el índice de lección actual (usado por evaluación de nivel).
     * Niveles bajos → primeras lecciones, niveles altos → últimas.
     */
    fun setLessonIndexForLevel(context: Context, level: Int) {
        val categories = QuestionGenerator.Category.entries
        for (cat in categories) {
            val lessons = getLessonsForCategory(cat)
            // Mapear nivel a índice de lección proporcional
            val index = when {
                level <= 3 -> 0
                level <= 5 -> (lessons.size * 0.2).toInt()
                level <= 8 -> (lessons.size * 0.4).toInt()
                level <= 12 -> (lessons.size * 0.6).toInt()
                level <= 16 -> (lessons.size * 0.8).toInt()
                else -> (lessons.size - 1)
            }.coerceIn(0, lessons.size - 1)

            prefs(context).edit()
                .putInt(KEY_CURRENT_INDEX + cat.name, index)
                .putString(KEY_SHOW_MODE + cat.name, ShowMode.LESSON.name)
                .apply()
        }
    }

    private fun getLessonsForCategory(category: QuestionGenerator.Category): List<LessonContent> {
        return when (category) {
            QuestionGenerator.Category.MATH -> mathLessons
            QuestionGenerator.Category.ENGLISH -> englishLessons
            QuestionGenerator.Category.PYTHON -> pythonLessons
            QuestionGenerator.Category.CURIOSIDADES -> curiosidadesLessons
        }
    }

    // ══════════════════════════════════════
    // LECCIONES DE CURIOSIDADES (fallback)
    // ══════════════════════════════════════
    private val curiosidadesLessons = listOf(
        LessonContent("cur_01", "El espacio",
            "La luz del Sol tarda ~8 minutos en llegar a la Tierra. La estrella más cercana (Proxima Centauri) esta a 4.24 años luz.",
            "Distancia Tierra-Sol: ~150 millones de km\nVelocidad de la luz: ~300.000 km/s",
            "¿Cuantos minutos tarda la luz del Sol en llegar a la Tierra?",
            listOf("8", "1", "60", "30"), "8", QuestionGenerator.Category.CURIOSIDADES),
        LessonContent("cur_02", "El cuerpo humano",
            "Un adulto tiene 206 huesos. Un bebe nace con ~270 que se fusionan al crecer.",
            "Hueso mas largo: femur\nHueso mas pequeño: estribo (oido)",
            "¿Cuantos huesos tiene un adulto?",
            listOf("206", "300", "150", "106"), "206", QuestionGenerator.Category.CURIOSIDADES),
        LessonContent("cur_03", "Los oceanos",
            "Los oceanos cubren ~71% de la superficie terrestre. El punto mas profundo es la Fosa de las Marianas.",
            "Oceano mas grande: Pacifico\nOceano mas pequeño: Artico",
            "¿Que porcentaje de la Tierra cubren los oceanos?",
            listOf("71%", "50%", "90%", "30%"), "71%", QuestionGenerator.Category.CURIOSIDADES)
    )

    // ══════════════════════════════════════
    // LECCIONES DE MATEMÁTICAS
    // ══════════════════════════════════════
    private val mathLessons = listOf(
        LessonContent("math_01", "Suma y Resta",
            "SUMAR es juntar cantidades (+). RESTAR es quitar una de otra (−).\nSon operaciones inversas: si 3+5=8, entonces 8−5=3.",
            "7 + 8 = 15\n15 − 8 = 7\n23 + 14 = 37\n37 − 14 = 23",
            "25 − 9 = ?", emptyList(), "16", QuestionGenerator.Category.MATH),
        LessonContent("math_02", "Multiplicación y División",
            "MULTIPLICAR es sumar un número varias veces: 4×3 = 4+4+4 = 12.\nDIVIDIR es repartir en partes iguales: 12÷4 = 3.\nSon inversas: si 4×3=12, entonces 12÷3=4.",
            "6 × 7 = 42\n42 ÷ 7 = 6\n8 × 9 = 72\n72 ÷ 8 = 9",
            "63 ÷ 9 = ?", emptyList(), "7", QuestionGenerator.Category.MATH),
        LessonContent("math_03", "Orden de operaciones",
            "Se resuelve primero × y ÷, luego + y −. Los paréntesis van primero que todo.",
            "2 + 3 × 4 = 14 (no 20)\n(2 + 3) × 4 = 20",
            "5 + 2 × 6 = ?", emptyList(), "17", QuestionGenerator.Category.MATH),
        LessonContent("math_04", "Potencias",
            "Una potencia es multiplicar un número por sí mismo. 3² = 3×3 = 9.",
            "2² = 4\n5² = 25\n3³ = 27",
            "4² = ?", emptyList(), "16", QuestionGenerator.Category.MATH),
        LessonContent("math_05", "Porcentajes",
            "Porcentaje significa 'por cada 100'. El 10% de algo es dividirlo entre 10.",
            "10% de 200 = 20\n50% de 80 = 40\n25% de 60 = 15",
            "20% de 150 = ?", emptyList(), "30", QuestionGenerator.Category.MATH),
        LessonContent("math_06", "Raíz cuadrada",
            "√x es el número que multiplicado por sí mismo da x. √9=3 porque 3×3=9.",
            "√4 = 2\n√16 = 4\n√25 = 5\n√100 = 10",
            "√49 = ?", emptyList(), "7", QuestionGenerator.Category.MATH),
        LessonContent("math_07", "Ecuaciones simples",
            "Una ecuación tiene una incógnita (x). Para resolverla, despeja x haciendo la operación inversa.",
            "x + 5 = 12 → x = 12 − 5 = 7\n3x = 15 → x = 15 ÷ 3 = 5",
            "x + 8 = 20, x = ?", emptyList(), "12", QuestionGenerator.Category.MATH),
        LessonContent("math_08", "MCM (Mínimo Común Múltiplo)",
            "El MCM es el menor número que es múltiplo de dos números a la vez.",
            "MCM(4,6): múltiplos de 4: 4,8,12... múltiplos de 6: 6,12... → MCM=12",
            "MCM de 3 y 5 = ?", emptyList(), "15", QuestionGenerator.Category.MATH)
    )

    // ══════════════════════════════════════
    // LECCIONES DE INGLÉS
    // ══════════════════════════════════════
    private val englishLessons = listOf(
        LessonContent("eng_01", "Verbo TO BE (presente)",
            "TO BE = ser/estar. Se conjuga así:\nI am, You are, He/She is, We are, They are.",
            "I am happy. She is tall. They are friends.",
            "Completa: 'She ___ a teacher'",
            listOf("is", "am", "are", "be"), "is", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_02", "Artículos: A / AN",
            "Usamos 'A' antes de consonante y 'AN' antes de vocal (por sonido).",
            "A car, A house, AN apple, AN hour (la h es muda)",
            "'___ orange' se completa con:",
            listOf("An", "A", "The", "---"), "An", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_03", "Present Simple",
            "Se usa para hábitos y verdades. Con he/she/it se agrega -s al verbo.",
            "I work. She works. They play. He plays.",
            "'He ___ coffee every morning'",
            listOf("drinks", "drink", "drinking", "drank"), "drinks", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_04", "Present Continuous",
            "Se usa para acciones que pasan AHORA. Estructura: am/is/are + verbo-ing.",
            "I am eating. She is reading. They are playing.",
            "'They ___ right now'",
            listOf("are studying", "studies", "study", "studied"), "are studying", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_05", "Past Simple (regular)",
            "Para hablar del pasado. Verbos regulares agregan -ed: work→worked, play→played.",
            "I worked yesterday. She played tennis. We walked home.",
            "Pasado de 'walk':",
            listOf("walked", "walken", "walking", "walks"), "walked", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_06", "Past Simple (irregular)",
            "Muchos verbos comunes son irregulares: go→went, eat→ate, see→saw, buy→bought.",
            "I went home. She ate lunch. We saw a movie.",
            "Pasado de 'go':",
            listOf("went", "goed", "gone", "going"), "went", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_07", "Preposiciones de tiempo",
            "IN: meses/años (in January). ON: días (on Monday). AT: horas (at 5pm).",
            "In 2024. On Friday. At 3 o'clock. In the morning.",
            "'The meeting is ___ Monday'",
            listOf("on", "in", "at", "by"), "on", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_08", "Comparativos",
            "Para comparar: adjetivo corto + -er (taller), adjetivo largo: more + adj (more beautiful).",
            "Big → bigger. Fast → faster. Expensive → more expensive.",
            "Comparativo de 'cheap':",
            listOf("cheaper", "more cheap", "cheapest", "cheapper"), "cheaper", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_09", "Present Perfect",
            "Have/has + participio. Para experiencias o acciones que aún importan hoy.",
            "I have visited Paris. She has eaten sushi. They have finished.",
            "'I have ___ that movie'",
            listOf("seen", "saw", "see", "seeing"), "seen", QuestionGenerator.Category.ENGLISH),
        LessonContent("eng_10", "Primer condicional",
            "If + present simple → will + infinitivo. Para situaciones posibles en el futuro.",
            "If it rains, I will stay home. If you study, you will pass.",
            "'If she calls, I ___ answer'",
            listOf("will", "would", "can", "am"), "will", QuestionGenerator.Category.ENGLISH)
    )

    // ══════════════════════════════════════
    // LECCIONES DE PYTHON
    // ══════════════════════════════════════
    private val pythonLessons = listOf(
        LessonContent("py_01", "Variables",
            "Una variable guarda un valor con un nombre. Se crea con = (no hace falta declarar tipo).",
            "nombre = 'Ana'\nedad = 25\npi = 3.14",
            "x = 10, luego x + 5 = ?", emptyList(), "15", QuestionGenerator.Category.PYTHON),
        LessonContent("py_02", "Tipos de datos",
            "Python tiene tipos básicos:\n• int: números enteros (5)\n• float: decimales (3.14)\n• str: texto ('hola')\n• bool: True/False",
            "type(42) → int\ntype(3.14) → float\ntype('hi') → str",
            "type(True) retorna:",
            listOf("bool", "int", "str", "boolean"), "bool", QuestionGenerator.Category.PYTHON),
        LessonContent("py_03", "Print y f-strings",
            "print() muestra texto en pantalla. Con f-strings puedes incluir variables dentro del texto.",
            "nombre = 'Ana'\nprint(f'Hola {nombre}')  # Hola Ana\nprint(f'2+3 = {2+3}')    # 2+3 = 5",
            "f'{4*3}' produce:",
            listOf("'12'", "'4*3'", "12", "error"), "'12'", QuestionGenerator.Category.PYTHON),
        LessonContent("py_04", "Condicionales (if/elif/else)",
            "Permiten ejecutar código según una condición:\n• if: si se cumple\n• elif: si no, pero se cumple otra\n• else: si ninguna se cumplió",
            "edad = 18\nif edad >= 18:\n    print('Mayor')\nelse:\n    print('Menor')",
            "if 5 > 3: resultado es:",
            listOf("True (entra al if)", "False", "Error", "None"), "True (entra al if)", QuestionGenerator.Category.PYTHON),
        LessonContent("py_05", "Listas",
            "Una lista guarda varios elementos ordenados. Se crean con []. Son mutables (se pueden cambiar).",
            "frutas = ['manzana', 'pera', 'uva']\nfrutas[0]  # 'manzana'\nfrutas.append('kiwi')\nlen(frutas)  # 4",
            "[1,2,3][1] = ?",
            listOf("2", "1", "3", "error"), "2", QuestionGenerator.Category.PYTHON),
        LessonContent("py_06", "Loops (for)",
            "El loop for recorre cada elemento de una secuencia, uno por uno.",
            "for fruta in ['manzana', 'pera']:\n    print(fruta)\n# manzana\n# pera\n\nfor i in range(3):\n    print(i)  # 0, 1, 2",
            "range(1, 4) genera:",
            listOf("1, 2, 3", "1, 2, 3, 4", "0, 1, 2, 3", "0, 1, 2"), "1, 2, 3", QuestionGenerator.Category.PYTHON),
        LessonContent("py_07", "Diccionarios",
            "Un dict guarda pares clave:valor. Se crean con {}. Accedes por clave, no por posición.",
            "persona = {'nombre': 'Ana', 'edad': 25}\npersona['nombre']  # 'Ana'\npersona['edad']    # 25",
            "{'a':1, 'b':2}['b'] = ?",
            listOf("2", "1", "'b'", "error"), "2", QuestionGenerator.Category.PYTHON),
        LessonContent("py_08", "Funciones (def)",
            "Una función agrupa código reutilizable. Se define con def y puede recibir parámetros y retornar valores.",
            "def saludar(nombre):\n    return f'Hola {nombre}'\n\nresultado = saludar('Ana')  # 'Hola Ana'",
            "def doble(x): return x*2\ndoble(7) = ?",
            listOf("14", "72", "error", "None"), "14", QuestionGenerator.Category.PYTHON),
        LessonContent("py_09", "Métodos de string",
            "Los strings tienen métodos útiles:\n• .upper() → mayúsculas\n• .lower() → minúsculas\n• .split() → divide en lista\n• .replace(a,b) → reemplaza",
            "'hola'.upper()  # 'HOLA'\n'A,B,C'.split(',')  # ['A','B','C']",
            "'python'.upper() = ?",
            listOf("'PYTHON'", "'Python'", "'python'", "error"), "'PYTHON'", QuestionGenerator.Category.PYTHON),
        LessonContent("py_10", "Try / Except",
            "Maneja errores sin que el programa se detenga. try intenta ejecutar, except captura el error.",
            "try:\n    resultado = 10 / 0\nexcept ZeroDivisionError:\n    print('No se puede dividir por 0')",
            "int('abc') causa:",
            listOf("ValueError", "TypeError", "SyntaxError", "None"), "ValueError", QuestionGenerator.Category.PYTHON)
    )
}
