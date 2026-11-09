/** Message affiché sous le score, selon la part de bonnes réponses. */
export interface Verdict {
  readonly emoji: string;
  readonly title: string;
  readonly message: string;
}

/** Pourcentage de bonnes réponses, arrondi. 0 s'il n'y a aucune question (pas de division par zéro). */
export function percentOf(score: number, total: number): number {
  return total > 0 ? Math.round((score / total) * 100) : 0;
}

// Paliers du plus exigeant au moins exigeant : le premier atteint l'emporte.
const VERDICTS: readonly { readonly from: number; readonly verdict: Verdict }[] = [
  {
    from: 100,
    verdict: {
      emoji: '🔮',
      title: 'Clairvoyance totale',
      message: "Rien ne t'échappe : l'étrange n'a plus de secret pour toi.",
    },
  },
  {
    from: 70,
    verdict: {
      emoji: '✨',
      title: 'Esprit initié',
      message: "Tu connais bien les recoins de l'insolite. Encore un effort pour un sans-faute !",
    },
  },
  {
    from: 40,
    verdict: {
      emoji: '🕯️',
      title: 'Curiosité en éveil',
      message: 'Quelques mystères te résistent encore : rejoue pour les percer.',
    },
  },
  {
    from: 0,
    verdict: {
      emoji: '🌑',
      title: "Premiers pas dans l'ombre",
      message: "L'étrange t'a joué des tours… mais chaque erreur est une nouvelle curiosité.",
    },
  },
];

export function verdictOf(score: number, total: number): Verdict {
  const percent = percentOf(score, total);
  // Le dernier palier commence à 0 : find() trouve toujours une entrée.
  return VERDICTS.find((v) => percent >= v.from)!.verdict;
}
