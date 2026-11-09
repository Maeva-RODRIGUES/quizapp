import { frenchSpacing, FrenchSpacingPipe } from './french-spacing.pipe';

const NBSP = ' ';

describe('frenchSpacing', () => {
  it('makes the space before ? ! : ; non-breaking', () => {
    expect(frenchSpacing('Peur des clowns ?')).toBe(`Peur des clowns${NBSP}?`);
    expect(frenchSpacing('Attention !')).toBe(`Attention${NBSP}!`);
    expect(frenchSpacing('Selon la légende : un chat noir ; un miroir')).toBe(
      `Selon la légende${NBSP}: un chat noir${NBSP}; un miroir`,
    );
  });

  it('handles French quotation marks on both sides', () => {
    expect(frenchSpacing('le « mauvais œil »')).toBe(`le «${NBSP}mauvais œil${NBSP}»`);
  });

  it('collapses several spaces into a single non-breaking one', () => {
    expect(frenchSpacing('Vraiment   ?')).toBe(`Vraiment${NBSP}?`);
  });

  it('leaves texts without a space before the sign untouched', () => {
    expect(frenchSpacing('Vraiment?')).toBe('Vraiment?');
    expect(frenchSpacing('https://example.org')).toBe('https://example.org');
    expect(frenchSpacing('Japon')).toBe('Japon');
  });

  it('is available as a template pipe', () => {
    expect(new FrenchSpacingPipe().transform('Pourquoi ?')).toBe(`Pourquoi${NBSP}?`);
  });
});
