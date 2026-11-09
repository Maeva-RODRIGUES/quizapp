/**
 * Habillage d'une catégorie à l'écran : un emoji et une phrase d'accroche.
 * L'API ne fournit que le code et le libellé ; le reste est de la présentation, donc défini côté front.
 */
export interface CategoryTheme {
  readonly emoji: string;
  readonly teaser: string;
}

// Une Map plutôt qu'un objet littéral : avec un objet, un code comme "constructor" (valide pour l'API)
// renverrait une propriété héritée de Object.prototype au lieu du thème par défaut.
const THEMES = new Map<string, CategoryTheme>([
  [
    'superstitions',
    {
      emoji: '🐈‍⬛',
      teaser: "Chats noirs, miroirs brisés, sel renversé\u00a0: d'où viennent ces croyances\u00a0?",
    },
  ],
  [
    'phobies',
    {
      emoji: '🕷️',
      teaser: 'Araignées, clowns, petits trous… sauras-tu nommer ces peurs étranges\u00a0?',
    },
  ],
]);

/** Thème de repli, pour une catégorie ajoutée plus tard (par exemple par un import CSV). */
export const DEFAULT_THEME: CategoryTheme = {
  emoji: '🔮',
  teaser: 'Une nouvelle collection de curiosités à explorer.',
};

export function themeOf(code: string): CategoryTheme {
  return THEMES.get(code) ?? DEFAULT_THEME;
}
