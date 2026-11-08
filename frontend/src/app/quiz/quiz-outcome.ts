import { Question, QuizResult } from '../api/models';

/**
 * Ce que l'écran de quiz transmet à l'écran de résultat, via l'état de navigation du routeur.
 * Les questions voyagent avec le résultat : la correction ne contient que des id et des index,
 * il faut les textes pour afficher « ta réponse » et « la bonne réponse ».
 */
export interface QuizOutcome {
  category: string;
  questions: Question[];
  answers: Record<number, number>;
  result: QuizResult;
}
