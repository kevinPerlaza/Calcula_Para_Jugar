#!/usr/bin/env python3
"""Normaliza y cura los bancos; elimina duplicados y completa metadatos pedagógicos."""
import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).parents[1] / "app/src/main/assets/questions"
TOPICS = {
    "Matemáticas": ["Suma y resta", "Multiplicación y división", "Orden de operaciones", "Potencias", "Porcentajes", "Raíces", "Ecuaciones", "Múltiplos y divisores"],
    "Inglés": ["Verbo to be", "Artículos", "Presente simple", "Presente continuo", "Pasado regular", "Pasado irregular", "Preposiciones", "Comparativos", "Present perfect", "Futuro"],
    "Python": ["Variables", "Tipos de datos", "Cadenas", "Condicionales", "Listas", "Bucles", "Diccionarios", "Funciones", "Métodos de texto", "Errores"],
    "Curiosidades": ["Espacio", "Cuerpo humano", "Océanos", "Geografía", "Biología", "Física", "Historia", "Tecnología"],
    "Tabla periódica": ["Símbolos", "Número atómico", "Grupos y períodos", "Metales y no metales", "Alcalinos", "Halógenos", "Gases nobles", "Estados de la materia", "Elementos de la vida", "Aplicaciones"]
}


def normalized(text):
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode().lower()
    return re.sub(r"\W+", " ", text).strip()


def main():
    removed = []
    for path in sorted(ROOT.glob("questions_*.json")):
        questions = json.loads(path.read_text(encoding="utf-8"))
        seen = set()
        curated = []
        for q in questions:
            key = (q["subject"], normalized(q["question"]))
            if key in seen:
                removed.append(q["id"])
                continue
            seen.add(key)
            topics = TOPICS[q["subject"]]
            topic = q.get("topic") or topics[(int(q["lesson"]) - 1) % len(topics)]
            q["topic"] = topic
            q["type"] = q.get("type") or q["subject"]
            if len(str(q.get("hint", "")).strip()) < 8:
                q["hint"] = f"Recuerda la idea principal de {topic.lower()}."
            explanation = q.get("explanation", "").strip()
            if len(explanation) < 20:
                explanation = f"La respuesta es {q['answer']}; corresponde al concepto de {topic.lower()}."
            q["explanation"] = explanation
            if len(str(q.get("whyCorrect", "")).strip()) < 8:
                q["whyCorrect"] = explanation
            if len(str(q.get("commonError", "")).strip()) < 8:
                q["commonError"] = "Responder por similitud sin comprobar el concepto."
            if len(str(q.get("remedialAction", "")).strip()) < 8:
                q["remedialAction"] = f"Repasa {topic.lower()} y vuelve a intentarlo con otro ejemplo."
            if len(str(q.get("retryRule", "")).strip()) < 8:
                q["retryRule"] = "Reintentar después de revisar la explicación."
            answer_index = q["options"].index(q["answer"])
            q["answerIndex"] = answer_index
            q["correctLetter"] = "ABCD"[answer_index]
            q["correctAnswerUi"] = f"{'ABCD'[answer_index]} — {q['answer']}"
            curated.append(q)
        path.write_text(json.dumps(curated, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Curación completa: {len(removed)} duplicados eliminados")
    if removed:
        print("IDs retirados:", ", ".join(removed))


if __name__ == "__main__":
    main()
