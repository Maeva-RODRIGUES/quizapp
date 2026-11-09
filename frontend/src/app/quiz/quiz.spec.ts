import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { Question, QuizResult, SubmitQuizRequest } from '../api/models';
import { QuizApi } from '../api/quiz-api';
import { Quiz } from './quiz';

const NBSP = '\u00a0'; // espace insécable ajoutée à l'affichage (pipe frenchSpacing)

const QUESTIONS: Question[] = [
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
];
const RESULT: QuizResult = {
  score: 1,
  total: 2,
  corrections: [
    { questionId: 6, givenOptionIndex: 1, correctOptionIndex: 1, correct: true },
    { questionId: 7, givenOptionIndex: 2, correctOptionIndex: 0, correct: false },
  ],
};

describe('Quiz', () => {
  let fixture: ComponentFixture<Quiz>;
  let submit: ReturnType<typeof vi.fn<(request: SubmitQuizRequest) => Observable<QuizResult>>>;
  let navigate: ReturnType<typeof vi.spyOn>;

  async function render(
    questions: () => Observable<Question[]> = () => of(QUESTIONS),
  ): Promise<HTMLElement> {
    submit = vi.fn(() => of(RESULT));
    TestBed.configureTestingModule({
      imports: [Quiz],
      providers: [provideRouter([]), { provide: QuizApi, useValue: { questions, submit } }],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Quiz);
    fixture.componentRef.setInput('category', 'phobies'); // ce que fait le routeur avec /quiz/phobies
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  /** Coche l'option n° optionIndex de la question affichée. */
  async function pick(page: HTMLElement, optionIndex: number): Promise<void> {
    page.querySelectorAll<HTMLInputElement>('fieldset input[type="radio"]')[optionIndex].click();
    await fixture.whenStable();
  }

  /** Bouton principal du formulaire : « Suivant », ou « Valider mes réponses » sur la dernière question. */
  const mainButton = (page: HTMLElement) =>
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const previousButton = (page: HTMLElement) =>
    Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.includes('Précédent'))!;
  const heading = (page: HTMLElement) => page.querySelector('fieldset h2')?.textContent?.trim();

  async function clickMain(page: HTMLElement): Promise<void> {
    mainButton(page).click();
    await fixture.whenStable();
  }

  it('shows only the first question, with its options and no "a) b)" prefix', async () => {
    const page = await render();

    expect(page.querySelectorAll('fieldset')).toHaveLength(1);
    expect(heading(page)).toBe(`Peur des araignées${NBSP}?`);
    expect(page.textContent).toContain('Question 1 / 2');
    expect(Array.from(page.querySelectorAll('label')).map((l) => l.textContent?.trim())).toEqual([
      'Entomophobie',
      'Arachnophobie',
    ]);
  });

  it('asks the API for the questions of the category taken from the URL', async () => {
    const questions = vi.fn(() => of(QUESTIONS));
    await render(questions);

    expect(questions).toHaveBeenCalledWith('phobies');
  });

  it('"Next" is enabled once the question is answered, then shows the next question and focuses it', async () => {
    const page = await render();
    expect(mainButton(page).textContent).toContain('Suivant');
    expect(mainButton(page).disabled).toBe(true);

    await pick(page, 1);
    expect(mainButton(page).disabled).toBe(false);

    await clickMain(page);
    expect(heading(page)).toBe(`Peur des nombres${NBSP}?`);
    expect(page.textContent).toContain('Question 2 / 2');
    expect(document.activeElement).toBe(page.querySelector('fieldset h2'));
  });

  it('"Previous" goes back with the answer still selected, and is disabled on the first question', async () => {
    const page = await render();
    expect(previousButton(page).disabled).toBe(true);
    await pick(page, 1);
    await clickMain(page);

    previousButton(page).click();
    await fixture.whenStable();

    expect(heading(page)).toBe(`Peur des araignées${NBSP}?`);
    expect(page.querySelectorAll<HTMLInputElement>('fieldset input')[1].checked).toBe(true);
  });

  it('on the last question, the main button submits and stays disabled until every question is answered', async () => {
    const page = await render();
    await pick(page, 1);
    await clickMain(page);

    expect(mainButton(page).textContent).toContain('Valider mes réponses');
    expect(mainButton(page).disabled).toBe(true);

    await pick(page, 2);
    expect(mainButton(page).disabled).toBe(false);
  });

  it('the progress bar follows the number of answered questions', async () => {
    const page = await render();
    const bar = () => page.querySelector('[role="progressbar"]')!;
    expect(bar().getAttribute('aria-valuenow')).toBe('0');
    expect(bar().getAttribute('aria-valuemax')).toBe('2');

    await pick(page, 1);

    expect(bar().getAttribute('aria-valuenow')).toBe('1');
    expect(page.textContent).toContain('1 / 2 réponses');
  });

  it('pressing Enter (form submission) on a question moves to the next one', async () => {
    const page = await render();
    await pick(page, 0);

    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(heading(page)).toBe(`Peur des nombres${NBSP}?`);
    expect(submit).not.toHaveBeenCalled();
  });

  it('pressing Enter on an unanswered question does nothing', async () => {
    const page = await render();

    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(heading(page)).toBe(`Peur des araignées${NBSP}?`);
  });

  it('changing an answer keeps only the last choice', async () => {
    const page = await render();
    await pick(page, 0);
    await pick(page, 1);
    await clickMain(page);
    await pick(page, 0);

    await clickMain(page);

    expect(submit).toHaveBeenCalledWith({ category: 'phobies', answers: { 6: 1, 7: 0 } });
  });

  it('submits the answers, then goes to the result screen with everything it needs', async () => {
    const page = await render();
    await pick(page, 1);
    await clickMain(page);
    await pick(page, 2);

    await clickMain(page);

    expect(submit).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith(['/result'], {
      state: { category: 'phobies', questions: QUESTIONS, answers: { 6: 1, 7: 2 }, result: RESULT },
    });
  });

  it('shows an error and lets the user retry when the submission fails', async () => {
    const page = await render();
    submit.mockReturnValueOnce(throwError(() => new Error('500')));
    await pick(page, 1);
    await clickMain(page);
    await pick(page, 2);

    await clickMain(page);

    expect(page.querySelector('[role="alert"]')?.textContent).toContain("L'envoi a échoué");
    expect(navigate).not.toHaveBeenCalled();
    expect(mainButton(page).disabled).toBe(false);
  });

  it('a new category starts again from the first question, with no answer', async () => {
    const page = await render();
    await pick(page, 1);
    await clickMain(page);

    fixture.componentRef.setInput('category', 'superstitions');
    await fixture.whenStable();

    expect(heading(page)).toBe(`Peur des araignées${NBSP}?`);
    expect(page.textContent).toContain('0 / 2 réponses');
  });

  it('shows an error with a retry button when the questions cannot be loaded', async () => {
    const page = await render(() => throwError(() => new Error('503')));

    expect(page.querySelector('[role="alert"]')?.textContent).toContain(
      'Impossible de charger les questions',
    );
    expect(page.querySelector('button')?.textContent).toContain('Réessayer');
  });

  it('says so when the category has no question', async () => {
    const page = await render(() => of([]));

    expect(page.textContent).toContain('Aucune question dans cette catégorie');
    expect(page.querySelector('form')).toBeNull();
  });
});
