import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { QuizApi } from '../api/quiz-api';
import { QuizOutcome } from './quiz-outcome';

/** Écran de jeu : les questions d'une catégorie, une réponse par question, puis la soumission. */
@Component({
  selector: 'app-quiz',
  imports: [RouterLink],
  templateUrl: './quiz.html',
  styleUrl: './quiz.css',
})
export class Quiz {
  private readonly api = inject(QuizApi);
  private readonly router = inject(Router);

  /** Code de la catégorie, lu dans l'URL /quiz/:category (withComponentInputBinding, cf. app.config.ts). */
  readonly category = input.required<string>();

  // Les questions sont rechargées automatiquement si le signal category() change (params → stream).
  protected readonly questions = rxResource({
    params: () => this.category(),
    stream: ({ params }) => this.api.questions(params),
    defaultValue: [],
  });

  /** id de question → index de l'option choisie. */
  protected readonly answers = signal<Record<number, number>>({});
  protected readonly answeredCount = computed(() => Object.keys(this.answers()).length);
  protected readonly allAnswered = computed(() => {
    const questions = this.questions.value();
    return questions.length > 0 && questions.every((q) => q.id in this.answers());
  });

  protected readonly submitting = signal(false);
  protected readonly submitError = signal(false);

  protected choose(questionId: number, optionIndex: number): void {
    // Nouvel objet plutôt que modification en place : un signal ne détecte un changement que si la référence change.
    this.answers.update((answers) => ({ ...answers, [questionId]: optionIndex }));
  }

  protected submit(): void {
    if (!this.allAnswered() || this.submitting()) {
      return;
    }
    const category = this.category();
    const answers = this.answers();
    this.submitting.set(true);
    this.submitError.set(false);
    this.api.submit({ category, answers }).subscribe({
      next: (result) => {
        const outcome: QuizOutcome = { category, questions: this.questions.value(), answers, result };
        void this.router.navigate(['/result'], { state: outcome });
      },
      error: () => {
        this.submitting.set(false);
        this.submitError.set(true);
      },
    });
  }
}
