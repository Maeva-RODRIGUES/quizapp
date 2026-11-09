import { Component, inject } from '@angular/core';
import { CanActivateFn, Router, RouterLink } from '@angular/router';
import { QuizOutcome } from '../quiz/quiz-outcome';
import { percentOf, verdictOf } from './verdict';

/** Lit le résultat transmis par l'écran de quiz dans l'état de la navigation en cours. */
function outcomeOf(router: Router): QuizOutcome | undefined {
  const state = router.currentNavigation()?.extras.state as Partial<QuizOutcome> | undefined;
  return state?.result && state.questions ? (state as QuizOutcome) : undefined;
}

/**
 * Guard de la route /result : l'écran n'a de sens qu'après une soumission. Sans résultat
 * (page rafraîchie, URL tapée à la main), on renvoie vers l'accueil au lieu d'afficher une page vide.
 */
export const hasQuizOutcome: CanActivateFn = () => {
  const router = inject(Router);
  return outcomeOf(router) ? true : router.createUrlTree(['/']);
};

/** Une ligne de la correction, prête à afficher. */
interface CorrectionRow {
  questionId: number;
  text: string;
  given: string | null;
  expected: string;
  correct: boolean;
}

/** Écran de résultat : le score (jauge + message) et la correction question par question. N'appelle pas l'API. */
@Component({
  selector: 'app-result',
  imports: [RouterLink],
  templateUrl: './result.html',
  styleUrl: './result.css',
})
export class Result {
  // Lu une seule fois, à la création : le guard garantit qu'il est présent.
  protected readonly outcome = outcomeOf(inject(Router))!;

  /**
   * La correction ne contient que des id et des index : on y joint les textes des questions.
   * Simple champ (pas de computed) : le résultat ne change plus une fois l'écran affiché.
   */
  protected readonly rows: CorrectionRow[] = this.toRows(this.outcome);

  protected readonly percent = percentOf(this.outcome.result.score, this.outcome.result.total);
  protected readonly verdict = verdictOf(this.outcome.result.score, this.outcome.result.total);

  private toRows({ questions, result }: QuizOutcome): CorrectionRow[] {
    const questionsById = new Map(questions.map((q) => [q.id, q]));
    return result.corrections.map((c) => {
      const question = questionsById.get(c.questionId);
      const option = (index: number | null) =>
        index === null ? null : (question?.options[index] ?? `option ${index + 1}`);
      return {
        questionId: c.questionId,
        text: question?.text ?? `Question ${c.questionId}`,
        given: option(c.givenOptionIndex),
        expected: option(c.correctOptionIndex)!,
        correct: c.correct,
      };
    });
  }
}
