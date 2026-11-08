import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { Question, QuizResult, SubmitQuizRequest } from '../api/models';
import { QuizApi } from '../api/quiz-api';
import { Quiz } from './quiz';

const QUESTIONS: Question[] = [
  { id: 6, category: 'phobies', text: 'Peur des araignées ?', options: ['Entomophobie', 'Arachnophobie'] },
  { id: 7, category: 'phobies', text: 'Peur des nombres ?', options: ['Arithmophobie', 'Chronophobie', 'Logophobie'] },
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

  async function render(questions: () => Observable<Question[]> = () => of(QUESTIONS)): Promise<HTMLElement> {
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

  async function pick(page: HTMLElement, questionId: number, optionIndex: number): Promise<void> {
    page.querySelectorAll<HTMLInputElement>(`input[name="question-${questionId}"]`)[optionIndex].click();
    await fixture.whenStable();
  }

  const submitButton = (page: HTMLElement) => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;

  it('shows each question with its options, without any "a) b)" prefix', async () => {
    const page = await render();

    const legends = Array.from(page.querySelectorAll('legend')).map((l) => l.textContent?.trim());
    expect(legends).toEqual(['1. Peur des araignées ?', '2. Peur des nombres ?']);
    expect(page.querySelectorAll('input[name="question-7"]')).toHaveLength(3);
    expect(page.querySelector('label')?.textContent?.trim()).toBe('Entomophobie');
  });

  it('asks the API for the questions of the category taken from the URL', async () => {
    const questions = vi.fn(() => of(QUESTIONS));
    await render(questions);

    expect(questions).toHaveBeenCalledWith('phobies');
  });

  it('keeps the submit button disabled until every question has an answer', async () => {
    const page = await render();
    expect(submitButton(page).disabled).toBe(true);

    await pick(page, 6, 1);
    expect(submitButton(page).disabled).toBe(true);
    expect(page.textContent).toContain('1 / 2 réponses');

    await pick(page, 7, 2);
    expect(submitButton(page).disabled).toBe(false);
  });

  it('changing an answer keeps only the last choice', async () => {
    const page = await render();
    await pick(page, 6, 0);
    await pick(page, 6, 1);
    await pick(page, 7, 0);

    submitButton(page).click();

    expect(submit).toHaveBeenCalledWith({ category: 'phobies', answers: { 6: 1, 7: 0 } });
  });

  it('submits the answers, then goes to the result screen with everything it needs', async () => {
    const page = await render();
    await pick(page, 6, 1);
    await pick(page, 7, 2);

    submitButton(page).click();

    expect(submit).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith(['/result'], {
      state: { category: 'phobies', questions: QUESTIONS, answers: { 6: 1, 7: 2 }, result: RESULT },
    });
  });

  it('shows an error and lets the user retry when the submission fails', async () => {
    const page = await render();
    submit.mockReturnValueOnce(throwError(() => new Error('500')));
    await pick(page, 6, 1);
    await pick(page, 7, 2);

    submitButton(page).click();
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain("L'envoi a échoué");
    expect(navigate).not.toHaveBeenCalled();
    expect(submitButton(page).disabled).toBe(false);
  });

  it('shows an error with a retry button when the questions cannot be loaded', async () => {
    const page = await render(() => throwError(() => new Error('503')));

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Impossible de charger les questions');
    expect(page.querySelector('button')?.textContent).toContain('Réessayer');
  });

  it('says so when the category has no question', async () => {
    const page = await render(() => of([]));

    expect(page.textContent).toContain('Aucune question dans cette catégorie');
    expect(page.querySelector('form')).toBeNull();
  });
});
