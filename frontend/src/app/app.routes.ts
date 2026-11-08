import { Routes } from '@angular/router';
import { Categories } from './categories/categories';
import { Quiz } from './quiz/quiz';

// Chaque URL est associée à un composant, affiché dans le <router-outlet /> de App.
export const routes: Routes = [
  { path: '', component: Categories, title: 'Quiz des bizarreries' },
  // :category est transmis à l'input category du composant (withComponentInputBinding).
  { path: 'quiz/:category', component: Quiz, title: 'Quiz en cours' },
  // Toute URL inconnue ramène à l'accueil (l'écran /result arrive à l'étape suivante).
  { path: '**', redirectTo: '' },
];
