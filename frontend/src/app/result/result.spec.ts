import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { QuizOutcome } from '../quiz/quiz-outcome';
import { hasQuizOutcome, Result } from './result';

const NBSP = '\u00a0'; // espace insécable ajoutée à l'affichage (pipe frenchSpacing)

@Component({ template: 'accueil' })
class Home {}

const OUTCOME: QuizOutcome = {
  category: 'phobies',
  questions: [
    {
      id: 6,
      category: 'phobies',
      text: 'Peur des araignées ?',
      options: ['Entomophobie', 'Arachnophobie'],
    },
    {
      id: 7,
      category: 'phobies',
      text: 'Peur des nombres ?',
      options: ['Arithmophobie', 'Chronophobie', 'Logophobie'],
    },
  ],
  answers: { 6: 1, 7: 2 },
  result: {
    score: 1,
    total: 2,
    corrections: [
      { questionId: 6, givenOptionIndex: 1, correctOptionIndex: 1, correct: true },
      { questionId: 7, givenOptionIndex: 2, correctOptionIndex: 0, correct: false },
    ],
  },
};

// On passe par le vrai routeur (RouterTestingHarness) pour tester l'écran ET son guard,
// comme quand l'écran de quiz navigue vers /result avec l'état de navigation.
describe('Result', () => {
  let harness: RouterTestingHarness;
  let router: Router;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: '', component: Home },
          { path: 'result', component: Result, canActivate: [hasQuizOutcome] },
        ]),
      ],
    });
    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  });

  async function open(state?: unknown): Promise<HTMLElement> {
    await router.navigateByUrl('/result', { state: state as Record<string, unknown> });
    harness.detectChanges();
    return harness.routeNativeElement!;
  }

  it('shows the score in the gauge, with a verdict and the percentage', async () => {
    const page = await open(OUTCOME);

    expect(page.querySelector('.gauge')?.getAttribute('aria-label')).toBe('Score : 1 sur 2');
    expect(page.querySelector('.gauge')?.textContent?.replace(/\s/g, '')).toBe('1/2');
    expect(page.querySelector('h2')?.textContent).toContain('Curiosité en éveil'); // 50 %
    expect(page.querySelector('.verdict')?.textContent).toContain('1 bonne réponse sur 2 (50');
  });

  it('fills the gauge up to the percentage of correct answers', async () => {
    const page = await open(OUTCOME);

    expect(page.querySelector<HTMLElement>('.gauge')?.style.getPropertyValue('--p')).toBe('50');
  });

  it('shows one line per question, with the texts instead of indexes', async () => {
    const page = await open(OUTCOME);

    const items = Array.from(page.querySelectorAll('li'));
    expect(items).toHaveLength(2);
    expect(items[0].textContent).toContain(`Bonne réponse${NBSP}: Peur des araignées${NBSP}?`);
    expect(items[0].textContent).toContain('Arachnophobie');
    expect(items[0].classList).toContain('correct');
    expect(items[1].classList).toContain('wrong');
    expect(items[1].textContent).toContain(`Mauvaise réponse${NBSP}: Peur des nombres${NBSP}?`);
    expect(items[1].textContent).toContain(`Ta réponse${NBSP}: Logophobie`);
    expect(items[1].querySelector('strong')?.textContent).toBe('Arithmophobie');
  });

  it('says "aucune" for a question left without answer', async () => {
    const unanswered: QuizOutcome = {
      ...OUTCOME,
      result: {
        ...OUTCOME.result,
        corrections: [
          { questionId: 7, givenOptionIndex: null, correctOptionIndex: 0, correct: false },
        ],
      },
    };
    const page = await open(unanswered);

    expect(page.querySelector('li')?.textContent).toContain(`Ta réponse${NBSP}: aucune`);
  });

  it('offers to replay the same category or pick another one', async () => {
    const page = await open(OUTCOME);

    const links = Array.from(page.querySelectorAll('nav a')).map((a) => a.getAttribute('href'));
    expect(links).toEqual(['/quiz/phobies', '/']);
  });

  it('redirects to the home page when opened without a result', async () => {
    await open(); // ex. page rafraîchie ou URL tapée à la main

    expect(router.url).toBe('/');
    expect(harness.routeNativeElement?.textContent).toContain('accueil');
  });
});
