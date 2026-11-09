import { DEFAULT_THEME, themeOf } from './category-theme';

describe('themeOf', () => {
  it('gives each known category its own emoji and teaser', () => {
    expect(themeOf('superstitions').emoji).toBe('🐈‍⬛');
    expect(themeOf('phobies').teaser).toContain('peurs');
  });

  it('falls back to the default theme for an unknown category', () => {
    expect(themeOf('legendes-urbaines')).toBe(DEFAULT_THEME);
  });

  it('is not fooled by codes that match Object.prototype properties', () => {
    expect(themeOf('constructor')).toBe(DEFAULT_THEME);
    expect(themeOf('tostring')).toBe(DEFAULT_THEME);
  });
});
