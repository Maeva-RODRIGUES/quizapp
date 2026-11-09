import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  async function render(): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders the application title', async () => {
    const page = await render();

    expect(page.querySelector('h1')?.textContent).toContain('Quiz des bizarreries');
  });

  it('title links back to the home page', async () => {
    const page = await render();

    expect(page.querySelector('h1 a')?.getAttribute('href')).toBe('/');
  });
});
