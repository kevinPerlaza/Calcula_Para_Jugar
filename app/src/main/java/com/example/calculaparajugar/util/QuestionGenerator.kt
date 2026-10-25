package com.example.calculaparajugar.util

import kotlin.random.Random

/**
 * Generador de preguntas con progresión pedagógica real.
 *
 * MATEMÁTICAS: currículo K-12
 *   Fácil: aritmética básica, tablas de multiplicar
 *   Medio: fracciones, porcentajes, orden de operaciones
 *   Difícil: potencias, raíces, álgebra básica
 *
 * INGLÉS: niveles CEFR
 *   Fácil (A1): vocabulario cotidiano, colores, familia, números
 *   Medio (A2-B1): verbos irregulares, tiempos verbales, preposiciones
 *   Difícil (B1-B2): condicionales, phrasal verbs, vocabulario avanzado
 *
 * PYTHON: roadmap de desarrollo
 *   Fácil: tipos, variables, operadores, print
 *   Medio: listas, dicts, loops, funciones, strings
 *   Difícil: OOP, comprehensions, excepciones, módulos
 */
object QuestionGenerator {

    data class Question(
        val text: String,
        val options: List<String>,
        val correctAnswer: String,
        val category: Category
    )

    enum class Category(val displayName: String) {
        MATH("Matemáticas"),
        ENGLISH("Inglés"),
        PYTHON("Python"),
        CURIOSIDADES("Curiosidades")
    }

    enum class Difficulty { EASY, MEDIUM, HARD }

    private var difficulty = Difficulty.EASY
    fun setDifficulty(level: Difficulty) { difficulty = level }

    fun generate(category: Category): Question = when (category) {
        Category.MATH -> generateMath()
        Category.ENGLISH -> generateEnglish()
        Category.PYTHON -> generatePython()
        Category.CURIOSIDADES -> generateMath() // Fallback; real questions come from QuestionBank
    }

    // ══════════════════════════════════════
    // MATEMÁTICAS — Currículo progresivo
    // ══════════════════════════════════════

    private fun generateMath(): Question = when (difficulty) {
        Difficulty.EASY -> generateMathEasy()
        Difficulty.MEDIUM -> generateMathMedium()
        Difficulty.HARD -> generateMathHard()
    }

    /** Fácil: sumas, restas, multiplicaciones simples, divisiones exactas */
    private fun generateMathEasy(): Question {
        return when (Random.nextInt(5)) {
            0 -> { val a = Random.nextInt(1, 20); val b = Random.nextInt(1, 20)
                Question("$a + $b", emptyList(), (a + b).toString(), Category.MATH) }
            1 -> { val a = Random.nextInt(10, 30); val b = Random.nextInt(1, a)
                Question("$a − $b", emptyList(), (a - b).toString(), Category.MATH) }
            2 -> { val a = Random.nextInt(2, 10); val b = Random.nextInt(2, 10)
                Question("$a × $b", emptyList(), (a * b).toString(), Category.MATH) }
            3 -> { val b = Random.nextInt(2, 10); val r = Random.nextInt(1, 10); val a = b * r
                Question("$a ÷ $b", emptyList(), r.toString(), Category.MATH) }
            else -> { // Doble de un número
                val a = Random.nextInt(5, 50)
                Question("El doble de $a", emptyList(), (a * 2).toString(), Category.MATH) }
        }
    }

    /** Medio: fracciones, porcentajes, orden de operaciones, negativos */
    private fun generateMathMedium(): Question {
        return when (Random.nextInt(6)) {
            0 -> { // Porcentajes
                val pct = listOf(10, 20, 25, 50, 75).random()
                val n = listOf(40, 60, 80, 100, 120, 200, 500).random()
                val ans = n * pct / 100
                Question("$pct% de $n", emptyList(), ans.toString(), Category.MATH) }
            1 -> { // Orden de operaciones
                val a = Random.nextInt(2, 8); val b = Random.nextInt(2, 6); val c = Random.nextInt(1, 10)
                Question("$a × $b + $c", emptyList(), (a * b + c).toString(), Category.MATH) }
            2 -> { // Orden operaciones con paréntesis
                val a = Random.nextInt(2, 10); val b = Random.nextInt(1, 8); val c = Random.nextInt(2, 5)
                Question("($a + $b) × $c", emptyList(), ((a + b) * c).toString(), Category.MATH) }
            3 -> { // Potencia cuadrada
                val a = Random.nextInt(2, 13)
                Question("$a²", emptyList(), (a * a).toString(), Category.MATH) }
            4 -> { // Múltiplos
                val n = listOf(3, 4, 5, 6, 7, 8, 9).random()
                val mult = n * Random.nextInt(3, 9)
                val opts = listOf("Sí", "No")
                val ans = if (mult % n == 0) "Sí" else "No"
                Question("¿$mult es múltiplo de $n?", opts, ans, Category.MATH) }
            else -> { // Fracción simple
                val num = Random.nextInt(1, 5); val den = listOf(2, 4, 5, 10).random()
                val whole = listOf(10, 20, 40, 50, 100).random()
                val ans = whole * num / den
                Question("$num/$den de $whole", emptyList(), ans.toString(), Category.MATH) }
        }
    }

    /** Difícil: álgebra, raíces, potencias, ecuaciones simples */
    private fun generateMathHard(): Question {
        return when (Random.nextInt(6)) {
            0 -> { // Ecuación simple: x + a = b
                val x = Random.nextInt(3, 20); val a = Random.nextInt(5, 30)
                Question("Si x + $a = ${x + a}, ¿x = ?", emptyList(), x.toString(), Category.MATH) }
            1 -> { // Ecuación: a·x = b
                val x = Random.nextInt(2, 12); val a = Random.nextInt(3, 9)
                Question("Si ${a}x = ${a * x}, ¿x = ?", emptyList(), x.toString(), Category.MATH) }
            2 -> { // Raíz cuadrada
                val r = Random.nextInt(2, 15); val sq = r * r
                Question("√$sq", emptyList(), r.toString(), Category.MATH) }
            3 -> { // Potencia cúbica
                val a = Random.nextInt(2, 6)
                Question("$a³", emptyList(), (a * a * a).toString(), Category.MATH) }
            4 -> { // MCM de dos números
                val a = listOf(4, 6, 8, 12).random(); val b = listOf(3, 5, 6, 9, 10).random()
                val mcm = lcm(a, b)
                Question("MCM de $a y $b", emptyList(), mcm.toString(), Category.MATH) }
            else -> { // Operación combinada — loop para garantizar división exacta
                var a: Int; var b: Int; val c = Random.nextInt(2, 5)
                do {
                    a = Random.nextInt(10, 50)
                    b = Random.nextInt(5, 20)
                } while ((a - b) % c != 0 || a <= b)
                Question("($a − $b) ÷ $c", emptyList(), ((a - b) / c).toString(), Category.MATH)
            }
        }
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    private fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b

    // ══════════════════════════════════════
    // INGLÉS — Progresión CEFR A1→B2
    // ══════════════════════════════════════

    private fun generateEnglish(): Question {
        val q = when (difficulty) {
            Difficulty.EASY -> englishA1[Random.nextInt(englishA1.size)]
            Difficulty.MEDIUM -> englishA2B1[Random.nextInt(englishA2B1.size)]
            Difficulty.HARD -> englishB1B2[Random.nextInt(englishB1B2.size)]
        }
        return q.copy(category = Category.ENGLISH)
    }

    /** A1: vocabulario básico cotidiano — objetos, colores, familia, cuerpo */
    private val englishA1 = listOf(
        // Sustantivos cotidianos
        q("'Breakfast' significa:", "Desayuno", "Almuerzo", "Cena", "Merienda"),
        q("'Window' significa:", "Ventana", "Puerta", "Pared", "Techo"),
        q("'Kitchen' significa:", "Cocina", "Baño", "Sala", "Cuarto"),
        q("'Shoulder' significa:", "Hombro", "Rodilla", "Codo", "Tobillo"),
        q("'Cloud' significa:", "Nube", "Sol", "Lluvia", "Trueno"),
        q("'Bridge' significa:", "Puente", "Calle", "Edificio", "Torre"),
        q("'Knife' significa:", "Cuchillo", "Cuchara", "Tenedor", "Plato"),
        q("'Floor' significa:", "Piso/Suelo", "Techo", "Pared", "Escalera"),
        q("'Shirt' significa:", "Camisa", "Pantalón", "Zapato", "Sombrero"),
        q("'Pillow' significa:", "Almohada", "Sábana", "Colchón", "Cobija"),
        // Verbos básicos
        q("'To drink' significa:", "Beber", "Comer", "Dormir", "Correr"),
        q("'To open' significa:", "Abrir", "Cerrar", "Empujar", "Jalar"),
        q("'To write' significa:", "Escribir", "Leer", "Hablar", "Escuchar"),
        q("'To buy' significa:", "Comprar", "Vender", "Pagar", "Ahorrar"),
        q("'To listen' significa:", "Escuchar", "Hablar", "Mirar", "Tocar"),
        q("'To carry' significa:", "Llevar/Cargar", "Dejar", "Tirar", "Recoger"),
        // Adjetivos
        q("'Cheap' significa:", "Barato", "Caro", "Gratis", "Nuevo"),
        q("'Empty' significa:", "Vacío", "Lleno", "Abierto", "Cerrado"),
        q("'Wide' significa:", "Ancho", "Estrecho", "Alto", "Bajo"),
        q("'Angry' significa:", "Enojado", "Triste", "Feliz", "Asustado"),
        q("'Thirsty' significa:", "Sediento", "Hambriento", "Cansado", "Enfermo"),
        q("'Loud' significa:", "Ruidoso", "Silencioso", "Rápido", "Lento"),
        // Números y tiempo
        q("'Thirteen' es el número:", "13", "30", "14", "31"),
        q("'Wednesday' es:", "Miércoles", "Martes", "Jueves", "Viernes"),
        q("'Autumn' significa:", "Otoño", "Invierno", "Primavera", "Verano"),
        q("'Century' significa:", "Siglo", "Década", "Año", "Mes"),
        // Preposiciones lugar
        q("'Behind' significa:", "Detrás", "Delante", "Encima", "Debajo"),
        q("'Next to' significa:", "Al lado de", "Lejos de", "Encima de", "Dentro de"),
        q("'Between' significa:", "Entre", "Fuera", "Sobre", "Bajo"),
        q("'Inside' significa:", "Dentro", "Fuera", "Arriba", "Abajo")
    )

    /** A2-B1: verbos irregulares, tiempos verbales, preposiciones de tiempo */
    private val englishA2B1 = listOf(
        // Verbos irregulares (past simple)
        q("Pasado de 'go':", "went", "goed", "gone", "going"),
        q("Pasado de 'eat':", "ate", "eated", "eaten", "eating"),
        q("Pasado de 'buy':", "bought", "buyed", "buied", "buying"),
        q("Pasado de 'write':", "wrote", "writed", "written", "writing"),
        q("Pasado de 'see':", "saw", "seed", "seen", "seeing"),
        q("Pasado de 'take':", "took", "taked", "taken", "taking"),
        q("Pasado de 'give':", "gave", "gived", "given", "giving"),
        q("Pasado de 'think':", "thought", "thinked", "thunk", "thinking"),
        q("Pasado de 'know':", "knew", "knowed", "known", "knowing"),
        q("Pasado de 'speak':", "spoke", "speaked", "spoken", "speaking"),
        q("Pasado de 'drive':", "drove", "drived", "driven", "driving"),
        q("Pasado de 'break':", "broke", "breaked", "broken", "breaking"),
        // Tiempos verbales
        q("'She ___ to school every day'", "goes", "go", "going", "gone"),
        q("'They ___ playing right now'", "are", "is", "was", "be"),
        q("'I have ___ this movie before'", "seen", "saw", "see", "seeing"),
        q("'He ___ already left'", "has", "have", "had", "is"),
        q("'We ___ there yesterday'", "were", "was", "are", "is"),
        q("'She ___ cooking when I arrived'", "was", "is", "were", "has"),
        // Preposiciones de tiempo
        q("'___ Monday' (el lunes):", "On", "In", "At", "By"),
        q("'___ 8 o'clock' (a las 8):", "At", "On", "In", "By"),
        q("'___ January' (en enero):", "In", "On", "At", "By"),
        q("'___ the morning' (en la mañana):", "In", "On", "At", "By"),
        // Comparativos/superlativos
        q("Comparativo de 'good':", "better", "gooder", "more good", "best"),
        q("Superlativo de 'bad':", "worst", "baddest", "more bad", "worse"),
        q("Comparativo de 'far':", "farther", "farer", "more far", "farest"),
        q("'This is ___ than that' (grande):", "bigger", "more big", "biger", "biggest"),
        // Conectores
        q("'___ it was raining, we went out'", "Although", "Because", "So", "And"),
        q("'I'll call you ___ I arrive'", "when", "while", "during", "for"),
        q("'She studied hard, ___ she passed'", "so", "but", "although", "while"),
        q("'I like tea ___ coffee'", "and", "but", "so", "or")
    )

    /** B1-B2: condicionales, phrasal verbs, vocab avanzado, voz pasiva */
    private val englishB1B2 = listOf(
        // Condicionales
        q("'If I ___ rich, I would travel' (2nd cond.):", "were", "am", "was", "be"),
        q("'If it rains, I ___ stay home' (1st cond.):", "will", "would", "can", "could"),
        q("'If I had studied, I ___ passed' (3rd cond.):", "would have", "will have", "had", "would"),
        q("'Unless you hurry, you ___ be late':", "will", "would", "can", "should"),
        // Phrasal verbs
        q("'Give up' significa:", "Rendirse", "Dar", "Subir", "Entregar"),
        q("'Look after' significa:", "Cuidar", "Buscar", "Mirar", "Encontrar"),
        q("'Put off' significa:", "Posponer", "Poner", "Apagar", "Quitar"),
        q("'Turn down' significa:", "Rechazar", "Bajar", "Girar", "Apagar"),
        q("'Break down' significa:", "Descomponerse", "Romper", "Bajar", "Entrar"),
        q("'Come up with' significa:", "Idear/Inventar", "Venir", "Subir", "Encontrar"),
        q("'Figure out' significa:", "Descifrar/Entender", "Figurar", "Salir", "Dibujar"),
        q("'Run out of' significa:", "Quedarse sin", "Correr", "Salir", "Huir"),
        // Vocabulario B2
        q("'Reliable' significa:", "Confiable", "Relajado", "Relevante", "Reacio"),
        q("'To achieve' significa:", "Lograr", "Alcanzar", "Recibir", "Perder"),
        q("'Shortage' significa:", "Escasez", "Atajo", "Corto", "Pequeño"),
        q("'Thoroughly' significa:", "Minuciosamente", "Rápidamente", "Apenas", "Usualmente"),
        q("'Outcome' significa:", "Resultado", "Ingreso", "Salida", "Fuera"),
        q("'Deadline' significa:", "Fecha límite", "Línea muerta", "Final", "Meta"),
        // Voz pasiva
        q("Pasiva de 'They built the house':", "The house was built", "The house is built", "The house built", "The house has built"),
        q("Pasiva de 'She wrote the book':", "The book was written", "The book wrote", "The book is wrote", "The book written"),
        // Reported speech
        q("'She said she ___ tired' (reported speech):", "was", "is", "were", "be"),
        q("Direct → Reported: 'I am happy' → She said she ___ happy:", "was", "is", "were", "has been"),
        // Modal verbs
        q("'You ___ see a doctor' (consejo fuerte):", "should", "can", "might", "will"),
        q("'It ___ rain later' (posibilidad):", "might", "will", "should", "must"),
        q("'You ___ not park here' (prohibición):", "must", "should", "might", "can"),
        // Relative clauses
        q("'The man ___ lives next door' (persona):", "who", "which", "what", "where"),
        q("'The book ___ I read' (cosa):", "which/that", "who", "where", "whom"),
        q("'The place ___ we met' (lugar):", "where", "which", "who", "when")
    )

    // ══════════════════════════════════════
    // PYTHON — Roadmap real de desarrollo
    // ══════════════════════════════════════

    private fun generatePython(): Question {
        val q = when (difficulty) {
            Difficulty.EASY -> pythonFoundations[Random.nextInt(pythonFoundations.size)]
            Difficulty.MEDIUM -> pythonIntermediate[Random.nextInt(pythonIntermediate.size)]
            Difficulty.HARD -> pythonAdvanced[Random.nextInt(pythonAdvanced.size)]
        }
        return q.copy(category = Category.PYTHON)
    }

    /** Fundamentos: tipos, variables, operadores, print, input, condicionales */
    private val pythonFoundations = listOf(
        // Tipos de datos
        q("type(42) retorna:", "int", "float", "str", "number"),
        q("type(3.14) retorna:", "float", "int", "decimal", "number"),
        q("type('hello') retorna:", "str", "string", "text", "char"),
        q("type(True) retorna:", "bool", "boolean", "int", "bit"),
        q("type([1,2,3]) retorna:", "list", "array", "tuple", "set"),
        q("type((1,2)) retorna:", "tuple", "list", "pair", "set"),
        q("type({'a':1}) retorna:", "dict", "object", "map", "hash"),
        // Operadores
        q("7 // 2 = ?", "3", "3.5", "4", "2"),
        q("7 % 2 = ?", "1", "3", "0", "3.5"),
        q("2 ** 4 = ?", "16", "8", "6", "24"),
        q("10 // 3 = ?", "3", "3.33", "4", "1"),
        q("'ab' + 'cd' = ?", "'abcd'", "'ab cd'", "error", "None"),
        q("'ha' * 3 = ?", "'hahaha'", "'ha3'", "error", "'ha ha ha'"),
        q("not False = ?", "True", "False", "None", "error"),
        q("5 == 5.0 = ?", "True", "False", "error", "None"),
        q("5 is 5.0 = ?", "False", "True", "error", "None"),
        // Variables y asignación
        q("x = 5; x += 3 → x vale:", "8", "53", "15", "error"),
        q("a, b = 1, 2 → b vale:", "2", "1", "(1,2)", "error"),
        q("x = 'Python'; len(x) = ?", "6", "5", "7", "error"),
        // Condicionales
        q("if 0: 'yes' else: 'no' → ?", "'no'", "'yes'", "error", "None"),
        q("bool('') = ?", "False", "True", "None", "''"),
        q("bool(0) = ?", "False", "True", "0", "None"),
        q("bool([]) = ?", "False", "True", "[]", "None"),
        q("bool(' ') = ?", "True", "False", "None", "error"),
        // Print y formato
        q("print(f'{3+4}') muestra:", "7", "3+4", "{7}", "error"),
        q("print(type(None)) muestra:", "<class 'NoneType'>", "None", "null", "error"),
        // Conversión de tipos
        q("int('42') = ?", "42", "'42'", "error", "None"),
        q("str(100) = ?", "'100'", "100", "error", "int"),
        q("float('3.5') = ?", "3.5", "'3.5'", "error", "3"),
        q("int(7.9) = ?", "7", "8", "7.9", "error")
    )

    /** Intermedio: listas, dicts, loops, funciones, strings, manejo errores */
    private val pythonIntermediate = listOf(
        // Listas
        q("[1,2,3][−1] = ?", "3", "1", "error", "−1"),
        q("[1,2,3,4][1:3] = ?", "[2,3]", "[1,2,3]", "[2,3,4]", "[1,2]"),
        q("len([10,20,30]) = ?", "3", "30", "2", "error"),
        q("[1,2] + [3,4] = ?", "[1,2,3,4]", "[4,6]", "error", "10"),
        q("sorted([3,1,2]) = ?", "[1,2,3]", "[3,2,1]", "[3,1,2]", "error"),
        q("[x*2 for x in [1,2,3]] = ?", "[2,4,6]", "[1,2,3,1,2,3]", "error", "[2,3,4]"),
        q("'hello'.split('l') = ?", "['he','','o']", "['hel','lo']", "['hello']", "error"),
        // Diccionarios
        q("{'a':1,'b':2}['b'] = ?", "2", "1", "'b'", "error"),
        q("{'a':1}.get('x', 0) = ?", "0", "None", "error", "'x'"),
        q("len({'a':1,'b':2,'c':3}) = ?", "3", "6", "2", "error"),
        q("'a' in {'a':1,'b':2} = ?", "True", "False", "error", "1"),
        q("list({'x':1,'y':2}.keys()) = ?", "['x','y']", "[1,2]", "[('x',1),('y',2)]", "error"),
        q("list({'x':1,'y':2}.values()) = ?", "[1,2]", "['x','y']", "{'x','y'}", "error"),
        // Strings
        q("'Hello World'.lower() = ?", "'hello world'", "'HELLO WORLD'", "'Hello World'", "error"),
        q("'python'.upper() = ?", "'PYTHON'", "'Python'", "'python'", "error"),
        q("'hello'.replace('l','r') = ?", "'herro'", "'herlo'", "'hello'", "error"),
        q("'a,b,c'.split(',') = ?", "['a','b','c']", "'a b c'", "['a,b,c']", "error"),
        q("' hi '.strip() = ?", "'hi'", "' hi'", "'hi '", "error"),
        q("'Python'[0:2] = ?", "'Py'", "'Pyt'", "'P'", "'yt'"),
        // Funciones
        q("def f(x): return x*2 → f(5) = ?", "10", "25", "error", "None"),
        q("def f(): pass → f() retorna:", "None", "0", "''", "error"),
        q("def f(a, b=3): return a+b → f(2) = ?", "5", "error", "23", "2"),
        // Loops
        q("sum(range(1,4)) = ?", "6", "10", "3", "error"),
        q("list(range(0,6,2)) = ?", "[0,2,4]", "[0,2,4,6]", "[2,4,6]", "[1,3,5]"),
        q("'break' en un for:", "Sale del loop", "Salta iteración", "Pausa", "Termina programa"),
        q("'continue' en un for:", "Salta a siguiente iteración", "Sale del loop", "Reinicia", "Pausa"),
        // Sets
        q("set([1,1,2,2,3]) = ?", "{1,2,3}", "[1,2,3]", "{1,1,2,2,3}", "error"),
        q("{1,2} | {2,3} = ?", "{1,2,3}", "{2}", "{1,3}", "error"),
        q("{1,2,3} & {2,3,4} = ?", "{2,3}", "{1,2,3,4}", "{1,4}", "error"),
        // Excepciones
        q("try/except sirve para:", "Manejar errores", "Crear loops", "Definir funciones", "Importar"),
        q("int('abc') causa:", "ValueError", "TypeError", "SyntaxError", "None")
    )

    /** Avanzado: OOP, comprehensions, generators, decoradores, módulos */
    private val pythonAdvanced = listOf(
        // OOP
        q("__init__ en una clase es:", "Constructor", "Destructor", "Getter", "Static method"),
        q("'self' en un método es:", "Referencia al objeto", "Variable global", "Parámetro obligatorio", "Keyword"),
        q("class Dog(Animal) → Dog:", "Hereda de Animal", "Crea Animal", "Es igual a Animal", "Importa Animal"),
        q("@staticmethod define:", "Método sin self", "Constructor", "Propiedad", "Método privado"),
        q("@property define:", "Getter como atributo", "Método estático", "Constructor", "Decorador"),
        q("super().__init__() sirve para:", "Llamar constructor padre", "Crear clase", "Importar", "Destruir objeto"),
        q("isinstance(5, int) = ?", "True", "False", "5", "error"),
        q("Herencia múltiple:", "class A(B, C)", "class A extends B, C", "class A(B)(C)", "import B, C"),
        // Generators y iteradores
        q("yield en una función crea:", "Generator", "Return", "Error", "Thread"),
        q("next() sobre generator:", "Da siguiente valor", "Reinicia", "Termina", "Error"),
        q("list(zip([1,2],[3,4])) = ?", "[(1,3),(2,4)]", "[[1,3],[2,4]]", "[1,2,3,4]", "error"),
        q("map(str, [1,2,3]) da:", "Iterador de strings", "['1','2','3']", "[1,2,3]", "error"),
        q("filter(None, [0,1,'',2]) da:", "Iterador truthy", "[1,2]", "[0,'']", "error"),
        // Comprehensions avanzadas
        q("{x:x**2 for x in range(3)} = ?", "{0:0, 1:1, 2:4}", "[0,1,4]", "error", "{0,1,4}"),
        q("[x for x in range(10) if x%2==0] = ?", "[0,2,4,6,8]", "[1,3,5,7,9]", "[2,4,6,8,10]", "error"),
        q("(x*2 for x in [1,2,3]) es:", "Generator", "Tupla", "Lista", "Set"),
        // Decoradores
        q("@decorator es:", "Función que envuelve otra", "Comentario", "Import", "Variable"),
        q("functools.lru_cache sirve para:", "Cachear resultados", "Crear threads", "Ordenar", "Filtrar"),
        // Módulos y paquetes
        q("__name__ == '__main__' verifica:", "Si se ejecuta directamente", "Nombre de clase", "Si hay error", "Versión"),
        q("pip install sirve para:", "Instalar paquetes", "Crear archivos", "Compilar", "Testear"),
        q("virtualenv/venv sirve para:", "Entorno aislado", "Compilar", "Debugear", "Crear GUI"),
        // Manejo avanzado
        q("*args recibe:", "Argumentos posicionales extra", "Un diccionario", "Un string", "Nada"),
        q("**kwargs recibe:", "Keyword args extra", "Una lista", "Un entero", "Nada"),
        q("with open('f.txt') as f: → hace:", "Abre y cierra automáticamente", "Crea archivo", "Borra archivo", "Lee en memoria"),
        q("GIL en Python es:", "Lock global de threads", "Librería gráfica", "Gestor de memoria", "IDE"),
        q("PEP 8 es:", "Guía de estilo", "Framework", "Librería", "Compilador"),
        // Testing
        q("assert x == 5 hace:", "Falla si x ≠ 5", "Asigna 5 a x", "Imprime x", "Nada"),
        q("unittest sirve para:", "Testing", "Debugging", "Logging", "Profiling")
    )

    // ══════════════════════════════════════
    // Helper para crear preguntas de opción múltiple
    // ══════════════════════════════════════

    private fun q(text: String, correct: String, wrong1: String, wrong2: String, wrong3: String): Question {
        return Question(
            text = text,
            options = listOf(correct, wrong1, wrong2, wrong3),
            correctAnswer = correct,
            category = Category.ENGLISH // Se sobreescribe al usarse
        )
    }
}
