import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { Category } from '../api/models';
import { QuizApi } from '../api/quiz-api';
import { Categories } from './categories';

// Le composant est testé avec un faux QuizApi : on choisit ce que "l'API" renvoie,
// sans HTTP. On vérifie seulement ce que l'utilisateur voit dans la page.
describe('Categories', () => {
  async function render(categories: () => Observable<Category[]>, waitForData = true): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [Categories],
      providers: [provideRouter([]), { provide: QuizApi, useValue: { categories } }],
    });
    const fixture = TestBed.createComponent(Categories);
    if (waitForData) {
      await fixture.whenStable(); // attend que le chargement soit terminé (données ou erreur)
    } else {
      fixture.detectChanges(); // un seul rendu, sans attendre : on observe l'état "en cours"
    }
    return fixture.nativeElement as HTMLElement;
  }

  it('lists the categories, each linking to its quiz', async () => {
    const page = await render(() =>
      of([
        { code: 'superstitions', label: 'Superstitions étranges' },
        { code: 'phobies', label: 'Phobies étranges' },
      ]),
    );

    const links = Array.from(page.querySelectorAll('a'));
    expect(links.map((a) => a.textContent?.trim())).toEqual(['Superstitions étranges', 'Phobies étranges']);
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/quiz/superstitions', '/quiz/phobies']);
  });

  it('shows a loading message while the API has not answered', async () => {
    const page = await render(() => NEVER, false); // un Observable qui n'émet jamais : la requête reste "en cours"

    expect(page.textContent).toContain('Chargement des catégories');
    expect(page.querySelector('a')).toBeNull();
  });

  it('shows an error with a retry button when the API fails', async () => {
    const page = await render(() => throwError(() => new Error('503')));

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Impossible de charger les catégories');
    expect(page.querySelector('button')?.textContent).toContain('Réessayer');
  });

  it('retry calls the API again and shows the categories', async () => {
    let calls = 0;
    const categories = () =>
      ++calls === 1 ? throwError(() => new Error('503')) : of([{ code: 'phobies', label: 'Phobies étranges' }]);
    TestBed.configureTestingModule({
      imports: [Categories],
      providers: [provideRouter([]), { provide: QuizApi, useValue: { categories } }],
    });
    const fixture = TestBed.createComponent(Categories);
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement).querySelector('button')!.click();
    await fixture.whenStable();

    expect(calls).toBe(2);
    expect((fixture.nativeElement as HTMLElement).querySelector('a')?.textContent).toContain('Phobies étranges');
  });

  it('says so when there is no category', async () => {
    const page = await render(() => of([]));

    expect(page.textContent).toContain('Aucune catégorie');
  });
});
