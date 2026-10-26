#!/usr/bin/env python3
"""Añade contenido complementario reproducible a los bancos de BrainWall."""
import json
import random
from pathlib import Path

ROOT = Path(__file__).parents[1] / "app/src/main/assets/questions"


def question(qid, subject, lesson, difficulty, text, answer, distractors, explanation):
    options = [str(answer), *map(str, distractors)]
    random.Random(qid).shuffle(options)
    return {
        "id": qid, "subject": subject, "lesson": lesson, "difficulty": difficulty,
        "question": text, "options": options, "answer": str(answer),
        "explanation": explanation, "whyCorrect": explanation,
        "commonError": "Elegir una opción parecida sin comprobar el concepto.",
        "feedbackWrong": f"Incorrecto. La respuesta correcta es {answer}. {explanation}",
        "jumpEligible": "Sí", "poolJumpL1L2": "Sí", "poolJumpL2L3": "Sí",
        "poolJumpL3L4": "Sí", "poolJumpL4L5": "Sí"
    }


def append(filename, additions):
    path = ROOT / filename
    current = json.loads(path.read_text(encoding="utf-8"))
    generated = {item["id"]: item for item in additions}
    current = [generated.pop(item["id"], item) for item in current]
    current.extend(generated.values())
    path.write_text(json.dumps(current, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def math_questions():
    result = []
    for n in range(1, 101):
        difficulty = min(5, (n - 1) // 10 + 1)
        lesson = (n - 1) % 8 + 1
        a = 8 + n * difficulty
        b = 2 + (n % 9)
        if lesson in (1, 3):
            answer = a + b * difficulty
            text = f"¿Cuánto es {a} + {b} × {difficulty}?"
            explanation = "Primero se resuelve la multiplicación y después la suma."
        elif lesson in (2, 8):
            answer = b * difficulty
            text = f"Hay {difficulty} grupos con {b} elementos cada uno. ¿Cuántos elementos hay en total?"
            explanation = f"Se multiplican {b} por {difficulty}: el resultado es {answer}."
        elif lesson == 4:
            base = 2 + n % 8
            answer = base * base
            text = f"¿Cuál es el cuadrado de {base}?"
            explanation = f"{base}² = {base} × {base} = {answer}."
        elif lesson == 5:
            percent = [10, 20, 25, 50][n % 4]
            total = 40 + (n % 5) * 40
            answer = total * percent // 100
            text = f"¿Cuánto es el {percent}% de {total}?"
            explanation = f"{percent}/100 × {total} = {answer}."
        elif lesson == 6:
            root = 3 + n % 10
            answer = root
            text = f"¿Cuál es √{root * root}?"
            explanation = f"{root} × {root} = {root * root}, por eso la raíz es {root}."
        else:
            answer = b + difficulty
            text = f"Resuelve x − {b} = {difficulty}. ¿Cuánto vale x?"
            explanation = f"Se suma {b} a ambos lados: x = {answer}."
        distractors = [answer + 1, max(0, answer - 1), answer + difficulty + 2]
        result.append(question(f"MAT-X-{n:03}", "Matemáticas", lesson, difficulty, text, answer, distractors, explanation))
    return result


ENGLISH = [
    ("apple", "manzana"), ("window", "ventana"), ("river", "río"), ("cloud", "nube"),
    ("forest", "bosque"), ("kitchen", "cocina"), ("teacher", "profesor"), ("question", "pregunta"),
    ("answer", "respuesta"), ("morning", "mañana"), ("night", "noche"), ("friend", "amigo"),
    ("family", "familia"), ("city", "ciudad"), ("country", "país"), ("water", "agua"),
    ("bread", "pan"), ("school", "escuela"), ("book", "libro"), ("computer", "computadora"),
    ("learn", "aprender"), ("write", "escribir"), ("read", "leer"), ("speak", "hablar"),
    ("listen", "escuchar"), ("travel", "viajar"), ("healthy", "saludable"), ("quickly", "rápidamente"),
    ("because", "porque"), ("although", "aunque"), ("bridge", "puente"),
    ("mountain", "montaña"), ("weather", "clima"), ("quiet", "tranquilo"),
    ("strong", "fuerte"), ("careful", "cuidadoso"), ("choose", "elegir"),
    ("build", "construir"), ("remember", "recordar"), ("forget", "olvidar"),
    ("before", "antes"), ("after", "después"), ("between", "entre"),
    ("always", "siempre"), ("never", "nunca"), ("sometimes", "a veces"),
    ("enough", "suficiente"), ("different", "diferente"), ("improve", "mejorar"),
    ("knowledge", "conocimiento")
]


def english_questions():
    spanish = [v for _, v in ENGLISH]
    result = []
    for i, (word, meaning) in enumerate(ENGLISH, 1):
        distractors = [spanish[(i + shift) % len(spanish)] for shift in (3, 9, 16)]
        result.append(question(f"ENG-X-{i:03}", "Inglés", (i - 1) % 10 + 1,
            min(5, (i - 1) // 6 + 1), f"¿Qué significa ‘{word}’ en español?", meaning,
            distractors, f"‘{word}’ se traduce como ‘{meaning}’."))
    return result


PYTHON = [
    ("len([4, 8, 15])", "3"), ("2 ** 5", "32"), ("10 // 3", "3"), ("10 % 3", "1"),
    ("'hola'.capitalize()", "'Hola'"), ("[1, 2] + [3]", "[1, 2, 3]"),
    ("sum([2, 3, 4])", "9"), ("min(7, 2, 5)", "2"), ("max(7, 2, 5)", "7"),
    ("bool(0)", "False"), ("bool('texto')", "True"), ("list(range(3))", "[0, 1, 2]"),
    ("'abc'[1]", "'b'"), ("'python'[-1]", "'n'"), ("{'a': 4}.get('a')", "4"),
    ("sorted([3, 1, 2])", "[1, 2, 3]"), ("abs(-12)", "12"),
    ("round(3.6)", "4"), ("'a,b'.split(',')", "['a', 'b']"),
    ("'-'.join(['a', 'b'])", "'a-b'"), ("type(3).__name__", "'int'"),
    ("3 in [1, 2, 3]", "True"), ("not False", "True"), ("5 > 2 and 1 < 3", "True"),
    ("[x * 2 for x in range(3)]", "[0, 2, 4]"), ("len({'a': 1, 'b': 2})", "2"),
    ("'banana'.count('a')", "3"), ("int('12') + 1", "13"),
    ("str(25) + '!'", "'25!'"), ("tuple([1, 2])", "(1, 2)"),
    ("set([1, 1, 2])", "{1, 2}"), ("len(set('aba'))", "2"),
    ("any([False, True])", "True"), ("all([True, False])", "False"),
    ("'Python'.lower()", "'python'"), ("'  hola  '.strip()", "'hola'"),
    ("[1, 2, 3][::-1]", "[3, 2, 1]"), ("5 == 5.0", "True"),
    ("isinstance(4, int)", "True"), ("divmod(10, 3)", "(3, 1)"),
    ("pow(2, 4)", "16"), ("chr(65)", "'A'"), ("ord('A')", "65"),
    ("dict(a=1)['a']", "1"), ("len(range(2, 10, 2))", "4"),
    ("'abc'.startswith('a')", "True"), ("'abc'.replace('b', 'x')", "'axc'"),
    ("3 if 5 > 2 else 0", "3"), ("sum(range(5))", "10"),
    ("next(iter([7, 8]))", "7")
]


def python_questions():
    result = []
    generic = ["Error", "None", "False", "0", "1", "2", "3", "True"]
    for i, (expr, answer) in enumerate(PYTHON, 1):
        distractors = []
        for candidate in generic + [f"'{answer}'", f"[{answer}]"]:
            if candidate != answer and candidate not in distractors:
                distractors.append(candidate)
            if len(distractors) == 3:
                break
        result.append(question(f"PY-X-{i:03}", "Python", (i - 1) % 10 + 1,
            min(5, (i - 1) // 6 + 1), f"¿Qué devuelve Python al evaluar {expr}?", answer,
            distractors, f"La expresión {expr} produce {answer}."))
    return result


FACTS = [
    ("¿Cuál es el planeta más grande del sistema solar?", "Júpiter", ["Marte", "Venus", "Saturno"]),
    ("¿Qué gas absorben principalmente las plantas?", "Dióxido de carbono", ["Oxígeno", "Helio", "Hidrógeno"]),
    ("¿Cuál es el órgano más grande del cuerpo humano?", "La piel", ["El hígado", "El corazón", "El pulmón"]),
    ("¿Cuántos continentes se reconocen habitualmente en el modelo de siete continentes?", "7", ["5", "6", "8"]),
    ("¿Qué océano es el más grande?", "Pacífico", ["Atlántico", "Índico", "Ártico"]),
    ("¿Qué instrumento mide los terremotos?", "Sismógrafo", ["Barómetro", "Termómetro", "Anemómetro"]),
    ("¿Qué vitamina produce la piel con ayuda de la luz solar?", "Vitamina D", ["Vitamina A", "Vitamina C", "Vitamina K"]),
    ("¿Cuál es el satélite natural de la Tierra?", "La Luna", ["Fobos", "Europa", "Titán"]),
    ("¿Qué parte de la célula contiene principalmente el ADN?", "Núcleo", ["Membrana", "Citoplasma", "Ribosoma"]),
    ("¿Cuál es la velocidad aproximada de la luz en el vacío?", "300 000 km/s", ["30 000 km/s", "3 000 km/s", "3 000 000 km/s"]),
    ("¿Qué país tiene forma de bota en los mapas?", "Italia", ["Grecia", "Portugal", "Croacia"]),
    ("¿Cuál es la capital de Japón?", "Tokio", ["Kioto", "Osaka", "Seúl"]),
    ("¿Qué animal es el mamífero más grande?", "Ballena azul", ["Elefante africano", "Tiburón ballena", "Jirafa"]),
    ("¿Qué capa protege a la Tierra de gran parte de la radiación ultravioleta?", "Capa de ozono", ["Troposfera", "Ionosfera", "Manto"]),
    ("¿Cuál es el hueso más largo del cuerpo humano?", "Fémur", ["Tibia", "Húmero", "Radio"]),
    ("¿Qué metal es líquido a temperatura ambiente?", "Mercurio", ["Hierro", "Aluminio", "Cobre"]),
    ("¿Qué proceso convierte agua líquida en vapor?", "Evaporación", ["Condensación", "Solidificación", "Fusión"]),
    ("¿Cuál es la estrella más cercana a la Tierra?", "El Sol", ["Sirio", "Polaris", "Próxima Centauri"]),
    ("¿Qué científico formuló las leyes del movimiento clásico?", "Isaac Newton", ["Charles Darwin", "Louis Pasteur", "Gregor Mendel"]),
    ("¿Qué unidad mide la corriente eléctrica?", "Amperio", ["Voltio", "Vatio", "Ohmio"]),
    ("¿Cuántas cámaras tiene el corazón humano?", "4", ["2", "3", "5"]),
    ("¿Qué planeta es conocido como el planeta rojo?", "Marte", ["Mercurio", "Venus", "Neptuno"]),
    ("¿Qué cordillera contiene el monte Everest?", "Himalaya", ["Andes", "Alpes", "Rocosas"]),
    ("¿Cuál es el idioma con más hablantes nativos?", "Chino mandarín", ["Inglés", "Español", "Hindi"]),
    ("¿Qué órgano bombea la sangre?", "Corazón", ["Pulmón", "Riñón", "Estómago"]),
    ("¿Qué fenómeno explica un arcoíris?", "Refracción y dispersión de la luz", ["Magnetismo", "Evaporación", "Gravedad"]),
    ("¿Qué planeta tiene los anillos más visibles?", "Saturno", ["Urano", "Júpiter", "Neptuno"]),
    ("¿Qué ciencia estudia los seres vivos?", "Biología", ["Geología", "Astronomía", "Meteorología"]),
    ("¿Cuál es la capital de Chile?", "Santiago", ["Valparaíso", "Concepción", "Antofagasta"]),
    ("¿Qué aparato convierte energía eléctrica en movimiento?", "Motor eléctrico", ["Generador", "Batería", "Transformador"]),
    ("¿Cuál es el desierto cálido más grande del mundo?", "Sahara", ["Atacama", "Gobi", "Kalahari"]),
    ("¿Qué río atraviesa Egipto?", "Nilo", ["Amazonas", "Danubio", "Misisipi"]),
    ("¿Qué planeta está más cerca del Sol?", "Mercurio", ["Venus", "Tierra", "Marte"]),
    ("¿Cuál es la capital de Australia?", "Canberra", ["Sídney", "Melbourne", "Perth"]),
    ("¿Qué órgano filtra la sangre y produce orina?", "Riñón", ["Páncreas", "Bazo", "Estómago"]),
    ("¿Qué molécula transporta la información genética?", "ADN", ["ATP", "Glucosa", "Colágeno"]),
    ("¿Qué escala se usa para medir la dureza de minerales?", "Mohs", ["Richter", "Beaufort", "Kelvin"]),
    ("¿Qué fuerza mantiene los planetas en órbita?", "Gravedad", ["Fricción", "Magnetismo", "Flotación"]),
    ("¿Qué animal puede regenerar brazos perdidos?", "Estrella de mar", ["Delfín", "Águila", "Pingüino"]),
    ("¿Qué país alberga Machu Picchu?", "Perú", ["Bolivia", "Ecuador", "México"]),
    ("¿Cuál es la capital de Canadá?", "Ottawa", ["Toronto", "Vancouver", "Montreal"]),
    ("¿Qué instrumento mide la presión atmosférica?", "Barómetro", ["Higrómetro", "Sismógrafo", "Calorímetro"]),
    ("¿Cómo se llama el cambio de sólido a gas?", "Sublimación", ["Fusión", "Condensación", "Deposición"]),
    ("¿Qué parte de la planta realiza principalmente la fotosíntesis?", "Hojas", ["Raíces", "Flores", "Semillas"]),
    ("¿Cuál es la capital de Argentina?", "Buenos Aires", ["Córdoba", "Rosario", "Mendoza"]),
    ("¿Qué capa de la Tierra es líquida y rodea el núcleo interno?", "Núcleo externo", ["Manto superior", "Corteza", "Litosfera"]),
    ("¿Qué tipo de sangre se considera donante universal de glóbulos rojos?", "O negativo", ["AB positivo", "A positivo", "B negativo"]),
    ("¿Qué científico propuso la evolución por selección natural?", "Charles Darwin", ["Isaac Newton", "Nikola Tesla", "Niels Bohr"]),
    ("¿Qué país tiene a Wellington como capital?", "Nueva Zelanda", ["Australia", "Irlanda", "Sudáfrica"]),
    ("¿Qué energía almacena un objeto por su altura?", "Potencial gravitatoria", ["Cinética", "Térmica", "Sonora"])
]


def fact_questions():
    return [question(f"CUR-X-{i:03}", "Curiosidades", (i - 1) % 8 + 1,
        min(5, (i - 1) // 6 + 1), text, answer, wrong, f"La respuesta correcta es {answer}.")
        for i, (text, answer, wrong) in enumerate(FACTS, 1)]


PERIODIC = [
    ("¿En qué grupo están los gases nobles?", "Grupo 18", ["Grupo 1", "Grupo 2", "Grupo 17"]),
    ("¿En qué grupo están los halógenos?", "Grupo 17", ["Grupo 1", "Grupo 14", "Grupo 18"]),
    ("¿Cuál es el elemento más electronegativo?", "Flúor", ["Oxígeno", "Cloro", "Francio"]),
    ("¿Qué partícula determina el número atómico?", "Protón", ["Neutrón", "Electrón", "Fotón"]),
    ("¿Cómo se llaman las filas horizontales de la tabla?", "Períodos", ["Grupos", "Familias", "Bloques"]),
    ("¿Cómo se llaman las columnas verticales?", "Grupos", ["Períodos", "Series", "Capas"]),
    ("¿Qué familia incluye litio, sodio y potasio?", "Metales alcalinos", ["Halógenos", "Gases nobles", "Lantánidos"]),
    ("¿Qué elemento se usa como referencia del mol con masa aproximada 12?", "Carbono-12", ["Oxígeno-16", "Hidrógeno-1", "Nitrógeno-14"]),
    ("¿Cuál es el metal más abundante en la corteza terrestre?", "Aluminio", ["Hierro", "Cobre", "Calcio"]),
    ("¿Qué elemento es esencial en la hemoglobina?", "Hierro", ["Cobre", "Plata", "Zinc"]),
    ("¿Qué elemento forma el grafito y el diamante?", "Carbono", ["Silicio", "Boro", "Azufre"]),
    ("¿Qué gas noble se usa con frecuencia en letreros luminosos?", "Neón", ["Argón", "Helio", "Radón"]),
    ("¿Qué elemento tiene símbolo K?", "Potasio", ["Kriptón", "Calcio", "Cobalto"]),
    ("¿Qué elemento tiene símbolo Na?", "Sodio", ["Nitrógeno", "Neón", "Níquel"]),
    ("¿Qué elemento tiene símbolo W?", "Wolframio", ["Vanadio", "Renio", "Osmio"]),
    ("¿Cuál de estos es un metaloide?", "Silicio", ["Sodio", "Cloro", "Neón"]),
    ("¿Qué bloque contiene a los metales de transición?", "Bloque d", ["Bloque s", "Bloque p", "Bloque f"]),
    ("¿Qué dos elementos son líquidos cerca de temperatura ambiente?", "Bromo y mercurio", ["Cloro y sodio", "Helio y neón", "Hierro y cobre"]),
    ("¿Qué elemento se emplea ampliamente en baterías recargables modernas?", "Litio", ["Argón", "Azufre", "Calcio"]),
    ("¿Qué elemento es la base de la mayoría de los semiconductores?", "Silicio", ["Helio", "Bromo", "Plomo"]),
    ("¿Qué tendencia suele aumentar de izquierda a derecha en un período?", "Electronegatividad", ["Radio atómico", "Carácter metálico", "Número de capas"]),
    ("¿Qué tendencia suele aumentar al bajar por un grupo?", "Radio atómico", ["Electronegatividad", "Energía de ionización", "Afinidad electrónica"]),
    ("¿Qué ion forma normalmente el sodio?", "Na⁺", ["Na²⁺", "Na⁻", "Na²⁻"]),
    ("¿Qué ion forma normalmente el cloro?", "Cl⁻", ["Cl⁺", "Cl²⁺", "Cl²⁻"]),
    ("¿Qué elemento es el principal componente del Sol?", "Hidrógeno", ["Oxígeno", "Carbono", "Helio líquido"]),
    ("¿Qué elemento radiactivo tiene número atómico 92?", "Uranio", ["Radio", "Plutonio", "Torio"]),
    ("¿Qué nombre recibe el grupo 2?", "Alcalinotérreos", ["Alcalinos", "Halógenos", "Calcógenos"]),
    ("¿Dónde se ubican lantánidos y actínidos?", "Bloque f", ["Bloque s", "Bloque p", "Bloque d"]),
    ("¿Qué elemento tiene símbolo Au?", "Oro", ["Plata", "Cobre", "Aluminio"]),
    ("¿Qué elemento tiene símbolo Ag?", "Plata", ["Oro", "Argón", "Aluminio"]),
    ("¿Qué elemento tiene símbolo Pb?", "Plomo", ["Paladio", "Fósforo", "Platino"]),
    ("¿Qué elemento tiene símbolo Sn?", "Estaño", ["Antimonio", "Estroncio", "Sodio"]),
    ("¿Qué elemento se utiliza para desinfectar agua de piscinas?", "Cloro", ["Neón", "Litio", "Argón"]),
    ("¿Qué elemento da color verde a muchas esmeraldas?", "Cromo", ["Calcio", "Sodio", "Helio"]),
    ("¿Qué gas constituye la mayor parte de la atmósfera terrestre?", "Nitrógeno", ["Oxígeno", "Argón", "Dióxido de carbono"]),
    ("¿Qué elemento forma aproximadamente una quinta parte del aire?", "Oxígeno", ["Nitrógeno", "Hidrógeno", "Neón"]),
    ("¿Qué elemento tiene número atómico 26?", "Hierro", ["Cobalto", "Níquel", "Manganeso"]),
    ("¿Qué elemento tiene número atómico 79?", "Oro", ["Plata", "Platino", "Mercurio"]),
    ("¿Qué elemento se usa en filamentos por su alto punto de fusión?", "Wolframio", ["Estaño", "Sodio", "Galio"]),
    ("¿Qué elemento se combina con cloro para formar sal común?", "Sodio", ["Potasio", "Calcio", "Magnesio"]),
    ("¿Cuál es el elemento natural más pesado presente en cantidades apreciables?", "Uranio", ["Osmio", "Plomo", "Radio"]),
    ("¿Qué elemento tiene el símbolo Ca?", "Calcio", ["Carbono", "Cadmio", "Cobalto"]),
    ("¿Qué elemento es importante para huesos y dientes?", "Calcio", ["Argón", "Bromo", "Neón"]),
    ("¿Qué elemento tiene símbolo Cu?", "Cobre", ["Curio", "Calcio", "Carbono"]),
    ("¿Qué metal se usa mucho en cables eléctricos?", "Cobre", ["Plomo", "Titanio", "Uranio"]),
    ("¿Qué elemento tiene símbolo Mg?", "Magnesio", ["Manganeso", "Mercurio", "Molibdeno"]),
    ("¿Qué elemento se encuentra en la clorofila?", "Magnesio", ["Hierro", "Oro", "Plata"]),
    ("¿Qué elemento tiene símbolo P?", "Fósforo", ["Potasio", "Plomo", "Platino"]),
    ("¿Qué elemento tiene símbolo S?", "Azufre", ["Silicio", "Sodio", "Escandio"]),
    ("¿Qué propiedad comparten los elementos de un mismo grupo?", "Electrones de valencia similares", ["Igual masa atómica", "Igual número de neutrones", "El mismo estado físico"])
]


def periodic_questions():
    return [question(f"PER-X-{i:03}", "Tabla periódica", (i - 1) % 10 + 1,
        min(5, (i - 1) // 6 + 1), text, answer, wrong, f"La respuesta correcta es {answer}.")
        for i, (text, answer, wrong) in enumerate(PERIODIC, 1)]


if __name__ == "__main__":
    append("questions_math.json", math_questions())
    append("questions_english.json", english_questions())
    append("questions_python.json", python_questions())
    append("questions_curiosidades.json", fact_questions())
    append("questions_periodic_table.json", periodic_questions())
