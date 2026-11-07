import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Coquille de l'application : un en-tête commun, puis l'écran choisi par le routeur. */
@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
