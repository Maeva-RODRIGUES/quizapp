import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { QuizApi } from '../api/quiz-api';
import { QuizOutcome } from './quiz-outcome';

/** Écran de jeu : une question à la fois, une réponse par question, puis la soumission. */
@Component({
  selector: 'app-quiz',
  imports: [RouterLink],
  templateUrl: './quiz.html',
  styleUrl: './quiz.css',
})
export class Quiz {
  private readonly api = inject(QuizApi);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);

  /** Code de la catégorie, lu dans l'URL /quiz/:category (withComponentInputBinding, cf. app.config.ts). */
  readonly category = input.required<string>();

  // Les questions sont rechargées automatiquement si le signal category() change (params → stream).
  protected readonly questions = rxResource({
    params: () => this.category(),
    stream: ({ params }) => this.api.questions(params),
    defaultValue: [],
  });

  // linkedSignal : un signal modifiable (comme signal()), mais remis à sa valeur de départ
  // chaque fois que sa source change. Nouvelle catégorie = retour à la 1re question, réponses vides.
  /** id de question → index de l'option choisie. */
  protected readonly answers = linkedSignal<string, Record<number, number>>({
    source: this.category,
    computation: () => ({}),
  });
  /** Index de la question affichée. */
  protected readonly current = linkedSignal({ source: this.category, computation: () => 0 });

  protected readonly total = computed(() => this.questions.value().length);
  protected readonly currentQuestion = computed(() => this.questions.value()[this.current()]);
  protected readonly isFirst = computed(() => this.current() === 0);
  protected readonly isLast = computed(() => this.current() === this.total() - 1);
  protected readonly currentAnswered = computed(() => {
    const question = this.currentQuestion();
    return question !== undefined && question.id in this.answers();
  });
  protected readonly answeredCount = computed(() => Object.keys(this.answers()).length);
  protected readonly allAnswered = computed(() => {
    const questions = this.questions.value();
    return questions.length > 0 && questions.every((q) => q.id in this.answers());
  });
  /** Avancement affiché par la barre de progression, en pourcentage des questions répondues. */
  protected readonly progress = computed(() =>
    this.total() === 0 ? 0 : Math.round((this.answeredCount() / this.total()) * 100),
  );

  protected readonly submitting = signal(false);
  protected readonly submitError = signal(false);

  /** Intitulé de la question affichée : reçoit le focus à chaque changement de question. */
  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');

  protected choose(questionId: number, optionIndex: number): void {
    // Nouvel objet plutôt que modification en place : un signal ne détecte un changement que si la référence change.
    this.answers.update((answers) => ({ ...answers, [questionId]: optionIndex }));
  }

  protected previous(): void {
    if (!this.isFirst()) {
      this.goTo(this.current() - 1);
    }
  }

  /** Bouton « Suivant » ou touche Entrée : question suivante, ou envoi des réponses sur la dernière. */
  protected next(): void {
    if (this.isLast()) {
      this.submit();
    } else if (this.currentAnswered()) {
      this.goTo(this.current() + 1);
    }
  }

  private goTo(index: number): void {
    this.current.set(index);
    // Après le rendu de la nouvelle question, on y place le focus : un lecteur d'écran l'annonce,
    // et la navigation au clavier reprend depuis la question au lieu du bouton qui vient de disparaître.
    afterNextRender(
      { write: () => this.heading()?.nativeElement.focus() },
      { injector: this.injector },
    );
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
        const outcome: QuizOutcome = {
          category,
          questions: this.questions.value(),
          answers,
          result,
        };
        void this.router.navigate(['/result'], { state: outcome });
      },
      error: () => {
        this.submitting.set(false);
        this.submitError.set(true);
      },
    });
  }
}
