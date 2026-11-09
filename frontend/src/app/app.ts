import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/** Coquille de l'application : un en-tête commun, puis l'écran choisi par le routeur. */
@Component({
  imports: [RouterLink, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
