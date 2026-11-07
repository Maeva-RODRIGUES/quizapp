import { Routes } from '@angular/router';
import { Categories } from './categories/categories';

// Chaque URL est associée à un composant, affiché dans le <router-outlet /> de App.
export const routes: Routes = [
  { path: '', component: Categories, title: 'Quiz des bizarreries' },
  // Toute URL inconnue ramène à l'accueil (l'écran /quiz/:category arrive à l'étape suivante).
  { path: '**', redirectTo: '' },
];
