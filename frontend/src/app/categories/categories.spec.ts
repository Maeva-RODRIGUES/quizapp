import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { Category } from '../api/models';
import { QuizApi } from '../api/quiz-api';
import { Categories } from './categories';
import { DEFAULT_THEME } from './category-theme';

// Le composant est testé avec un faux QuizApi : on choisit ce que "l'API" renvoie,
// sans HTTP. On vérifie seulement ce que l'utilisateur voit dans la page.
describe('Categories', () => {
  async function render(
    categories: () => Observable<Category[]>,
    waitForData = true,
  ): Promise<HTMLElement> {
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

  it('lists the categories as cards, each linking to its quiz', async () => {
    const page = await render(() =>
      of([
        { code: 'superstitions', label: 'Superstitions étranges' },
        { code: 'phobies', label: 'Phobies étranges' },
      ]),
    );

    const cards = Array.from(page.querySelectorAll('a.card'));
    expect(cards.map((a) => a.querySelector('h3')?.textContent?.trim())).toEqual([
      'Superstitions étranges',
      'Phobies étranges',
    ]);
    expect(cards.map((a) => a.getAttribute('href'))).toEqual([
      '/quiz/superstitions',
      '/quiz/phobies',
    ]);
    expect(cards[0].querySelector('.teaser')?.textContent).toContain('Chats noirs');
  });

  it('dresses an unknown category with the default theme', async () => {
    const page = await render(() => of([{ code: 'legendes', label: 'Légendes urbaines' }]));

    expect(page.querySelector('a.card .emoji')?.textContent).toBe(DEFAULT_THEME.emoji);
    expect(page.querySelector('a.card .teaser')?.textContent).toBe(DEFAULT_THEME.teaser);
  });

  it('shows a loading message while the API has not answered', async () => {
    const page = await render(() => NEVER, false); // un Observable qui n'émet jamais : la requête reste "en cours"

    expect(page.querySelector('[role="status"]')?.textContent).toContain(
      'Chargement des catégories',
    );
    expect(page.querySelectorAll('.skeleton').length).toBeGreaterThan(0);
    expect(page.querySelector('a')).toBeNull();
  });

  it('shows an error with a retry button when the API fails', async () => {
    const page = await render(() => throwError(() => new Error('503')));

    expect(page.querySelector('[role="alert"]')?.textContent).toContain(
      'Impossible de charger les catégories',
    );
    expect(page.querySelector('button')?.textContent).toContain('Réessayer');
  });

  it('retry calls the API again and shows the categories', async () => {
    let calls = 0;
    const categories = () =>
      ++calls === 1
        ? throwError(() => new Error('503'))
        : of([{ code: 'phobies', label: 'Phobies étranges' }]);
    TestBed.configureTestingModule({
      imports: [Categories],
      providers: [provideRouter([]), { provide: QuizApi, useValue: { categories } }],
    });
    const fixture = TestBed.createComponent(Categories);
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement).querySelector('button')!.click();
    await fixture.whenStable();

    expect(calls).toBe(2);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('a.card h3')?.textContent,
    ).toContain('Phobies étranges');
  });

  it('says so when there is no category', async () => {
    const page = await render(() => of([]));

    expect(page.textContent).toContain('Aucune catégorie');
  });
});
