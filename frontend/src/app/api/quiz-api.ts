import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Category, Question, QuizResult, SubmitQuizRequest } from './models';

/**
 * Accès à l'API REST du quiz. Les URL sont relatives (/api/…) : en développement, le serveur Angular
 * les relaie vers Spring (proxy.conf.json), le navigateur ne parle donc qu'à une seule origine.
 */
@Injectable({ providedIn: 'root' })
export class QuizApi {
  private readonly http = inject(HttpClient);

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  questions(category: string): Observable<Question[]> {
    return this.http.get<Question[]>('/api/questions', { params: { category } });
  }

  submit(request: SubmitQuizRequest): Observable<QuizResult> {
    return this.http.post<QuizResult>('/api/quiz/submit', request);
  }
}
