import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { QuizApi } from '../api/quiz-api';
import { themeOf } from './category-theme';

/** Écran d'accueil : la liste des catégories, chacune menant à son quiz. */
@Component({
  selector: 'app-categories',
  imports: [RouterLink],
  templateUrl: './categories.html',
  styleUrl: './categories.css',
})
export class Categories {
  private readonly api = inject(QuizApi);

  // rxResource s'abonne à l'Observable et expose son état sous forme de signals :
  // isLoading(), error() et value(). Le template se met à jour quand l'un d'eux change.
  protected readonly categories = rxResource({
    stream: () => this.api.categories(),
    defaultValue: [],
  });

  protected readonly themeOf = themeOf;
}
