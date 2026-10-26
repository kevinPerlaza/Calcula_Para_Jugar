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
    ids = {item["id"] for item in current}
    current.extend(item for item in additions if item["id"] not in ids)
    path.write_text(json.dumps(current, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def math_questions():
    result = []
    for n in range(1, 51):
        difficulty = (n - 1) // 10 + 1
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
    ("because", "porque"), ("although", "aunque")
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
    ("str(25) + '!'", "'25!'"), ("tuple([1, 2])", "(1, 2)")
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
    ("¿Qué aparato convierte energía eléctrica en movimiento?", "Motor eléctrico", ["Generador", "Batería", "Transformador"])
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
    ("¿Qué elemento tiene símbolo Ag?", "Plata", ["Oro", "Argón", "Aluminio"])
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
