#!/usr/bin/env python3
import json
import re
import unicodedata
from pathlib import Path


QUESTIONS_DIR = Path(__file__).parents[1] / "app/src/main/assets/questions"
REQUIRED = {"id", "subject", "lesson", "difficulty", "question", "options", "answer",
            "topic", "explanation", "hint", "whyCorrect", "commonError", "remedialAction"}


def normalized(text: str) -> str:
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode().lower()
    return re.sub(r"\W+", " ", text).strip()


def main() -> None:
    seen_ids: set[str] = set()
    seen_questions: set[tuple[str, str]] = set()
    total = 0

    for path in sorted(QUESTIONS_DIR.glob("questions_*.json")):
        questions = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(questions, list) or not questions:
            raise ValueError(f"{path.name}: el banco debe ser una lista no vacía")

        for index, question in enumerate(questions):
            missing = REQUIRED - question.keys()
            if missing:
                raise ValueError(f"{path.name}[{index}]: faltan {sorted(missing)}")

            question_id = question["id"]
            if question_id in seen_ids:
                raise ValueError(f"ID duplicado: {question_id}")
            seen_ids.add(question_id)

            options = question["options"]
            if len(options) != 4 or len(set(options)) != 4:
                raise ValueError(f"{question_id}: debe tener cuatro opciones distintas")
            if question["answer"] not in options:
                raise ValueError(f"{question_id}: la respuesta no aparece en las opciones")
            if not 1 <= int(question["difficulty"]) <= 5:
                raise ValueError(f"{question_id}: dificultad fuera de rango")
            duplicate_key = (question["subject"], normalized(question["question"]))
            if duplicate_key in seen_questions:
                raise ValueError(f"{question_id}: pregunta repetida en {question['subject']}")
            seen_questions.add(duplicate_key)
            for field in ("topic", "explanation", "hint", "whyCorrect", "remedialAction"):
                minimum = 3 if field == "topic" else 8
                if len(str(question[field]).strip()) < minimum:
                    raise ValueError(f"{question_id}: campo pedagógico insuficiente: {field}")
            if question.get("answerIndex") != options.index(question["answer"]):
                raise ValueError(f"{question_id}: answerIndex no coincide con la respuesta")
            total += 1

    print(f"Bancos válidos: {total} preguntas, {len(seen_ids)} IDs únicos")


if __name__ == "__main__":
    main()
