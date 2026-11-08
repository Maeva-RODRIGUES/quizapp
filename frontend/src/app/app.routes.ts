import { Routes } from '@angular/router';
import { Categories } from './categories/categories';
import { Quiz } from './quiz/quiz';
import { hasQuizOutcome, Result } from './result/result';

// Chaque URL est associée à un composant, affiché dans le <router-outlet /> de App.
export const routes: Routes = [
  { path: '', component: Categories, title: 'Quiz des bizarreries' },
  // :category est transmis à l'input category du composant (withComponentInputBinding).
  { path: 'quiz/:category', component: Quiz, title: 'Quiz en cours' },
  // Accessible seulement juste après une soumission (le guard vérifie l'état de navigation).
  { path: 'result', component: Result, canActivate: [hasQuizOutcome], title: 'Résultat' },
  // Toute URL inconnue ramène à l'accueil.
  { path: '**', redirectTo: '' },
];
