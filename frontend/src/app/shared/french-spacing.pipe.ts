import { Pipe, PipeTransform } from '@angular/core';

const NBSP = '\u00a0';

/**
 * Typographie française : l'espace avant ? ! : ; et », et après «, devient insécable,
 * pour que le signe ne parte jamais seul à la ligne.
 *
 * Appliqué à l'affichage seulement : les textes en base ne changent pas. La clé de dédoublonnage
 * des questions (calculée en Java et en SQL) ne traite pas l'espace insécable comme une espace :
 * l'enregistrer en base désaccorderait ces deux calculs.
 */
export function frenchSpacing(text: string): string {
  // Deux passes simples plutôt qu'une regex « / +([?!:;»])/ » : celle-ci revient en arrière (backtracking)
  // sur une longue suite d'espaces sans ponctuation derrière, avec un temps de calcul quadratique.
  // 1. Les suites d'espaces deviennent une seule espace (sans effet visible : le HTML les fusionne déjà).
  // 2. Une seule espace devant le signe : plus aucun quantificateur, le traitement reste linéaire.
  return text
    .replace(/ +/g, ' ')
    .replace(/ ([?!:;»])/g, `${NBSP}$1`)
    .replace(/« /g, `«${NBSP}`);
}

/** Version pipe pour les templates : {{ question.text | frenchSpacing }}. Pure, donc recalculée seulement si le texte change. */
@Pipe({ name: 'frenchSpacing' })
export class FrenchSpacingPipe implements PipeTransform {
  transform(text: string): string {
    return frenchSpacing(text);
  }
}
