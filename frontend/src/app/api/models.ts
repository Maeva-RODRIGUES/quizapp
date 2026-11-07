// Contrat avec l'API Spring : une interface par DTO Java (adapters/in/web/dto).
// Si un DTO change côté back, c'est ici qu'on le reporte, et le compilateur signale les usages à corriger.

/** GET /api/categories → CategoryView */
export interface Category {
  code: string;
  label: string;
}

/** GET /api/questions?category=… → QuestionView (sans la bonne réponse) */
export interface Question {
  id: number;
  category: string;
  text: string;
  options: string[];
}

/** POST /api/quiz/submit ← SubmitQuizRequest */
export interface SubmitQuizRequest {
  category: string;
  /** id de question → index de l'option choisie (à partir de 0). En JSON, les clés d'objet sont des chaînes. */
  answers: Record<number, number>;
}

/** POST /api/quiz/submit → QuizResultResponse */
export interface QuizResult {
  score: number;
  total: number;
  corrections: Correction[];
}

/** QuizResultResponse.CorrectionView */
export interface Correction {
  questionId: number;
  /** null si la question est restée sans réponse */
  givenOptionIndex: number | null;
  correctOptionIndex: number;
  correct: boolean;
}
