import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Category, Question, QuizResult } from './models';
import { QuizApi } from './quiz-api';

// Aucun serveur n'est lancé : HttpTestingController intercepte les requêtes, on vérifie ce qui part
// (méthode, URL, corps) puis on répond à la place de Spring avec flush().
describe('QuizApi', () => {
  let api: QuizApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(QuizApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify()); // échoue si une requête est partie sans être attendue par le test

  it('loads the categories', async () => {
    const categories: Category[] = [{ code: 'phobies', label: 'Phobies étranges' }];

    const result = firstValueFrom(api.categories());
    const request = http.expectOne('/api/categories');
    expect(request.request.method).toBe('GET');
    request.flush(categories);

    expect(await result).toEqual(categories);
  });

  it('loads the questions of one category, passed as a query parameter', async () => {
    const questions: Question[] = [
      { id: 7, category: 'phobies', text: 'Peur des clowns ?', options: ['Coulrophobie', 'Clinophobie'] },
    ];

    const result = firstValueFrom(api.questions('phobies'));
    const request = http.expectOne((req) => req.url === '/api/questions');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('category')).toBe('phobies');
    request.flush(questions);

    expect(await result).toEqual(questions);
  });

  it('submits the answers and returns the score with corrections', async () => {
    const quizResult: QuizResult = {
      score: 1,
      total: 2,
      corrections: [
        { questionId: 7, givenOptionIndex: 0, correctOptionIndex: 0, correct: true },
        { questionId: 8, givenOptionIndex: null, correctOptionIndex: 2, correct: false },
      ],
    };

    const result = firstValueFrom(api.submit({ category: 'phobies', answers: { 7: 0 } }));
    const request = http.expectOne('/api/quiz/submit');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ category: 'phobies', answers: { 7: 0 } });
    request.flush(quizResult);

    expect(await result).toEqual(quizResult);
  });

  it('propagates an HTTP error to the caller', async () => {
    const result = firstValueFrom(api.categories());
    http.expectOne('/api/categories').flush(null, { status: 503, statusText: 'Service Unavailable' });

    await expect(result).rejects.toMatchObject({ status: 503 });
  });
});
