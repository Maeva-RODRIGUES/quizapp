import { percentOf, verdictOf } from './verdict';

describe('percentOf', () => {
  it('rounds the share of correct answers', () => {
    expect(percentOf(3, 7)).toBe(43);
    expect(percentOf(7, 7)).toBe(100);
  });

  it('is 0 when there is no question, instead of dividing by zero', () => {
    expect(percentOf(0, 0)).toBe(0);
  });
});

describe('verdictOf', () => {
  it('rewards a perfect score', () => {
    expect(verdictOf(7, 7).title).toBe('Clairvoyance totale');
  });

  it('picks the tier reached, boundaries included', () => {
    expect(verdictOf(7, 10).title).toBe('Esprit initié'); // 70 % pile
    expect(verdictOf(6, 10).title).toBe('Curiosité en éveil');
    expect(verdictOf(4, 10).title).toBe('Curiosité en éveil'); // 40 % pile
    expect(verdictOf(3, 10).title).toBe("Premiers pas dans l'ombre");
  });

  it('has an encouraging message even for 0 or for an empty quiz', () => {
    expect(verdictOf(0, 7).title).toBe("Premiers pas dans l'ombre");
    expect(verdictOf(0, 0).title).toBe("Premiers pas dans l'ombre");
  });
});
