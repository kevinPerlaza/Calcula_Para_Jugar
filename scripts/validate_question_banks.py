#!/usr/bin/env python3
import json
from pathlib import Path


QUESTIONS_DIR = Path(__file__).parents[1] / "app/src/main/assets/questions"
REQUIRED = {"id", "subject", "lesson", "difficulty", "question", "options", "answer"}


def main() -> None:
    seen_ids: set[str] = set()
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
            total += 1

    print(f"Bancos válidos: {total} preguntas, {len(seen_ids)} IDs únicos")


if __name__ == "__main__":
    main()
