# quizapp — front Angular

Interface web du quiz : elle appelle l'API Spring Boot du dossier parent.

```bash
npm ci                         # installe les versions exactes de package-lock.json
npm start                      # http://localhost:4200 (l'API doit tourner sur le port 8080)
npm test -- --watch=false      # tests unitaires (Vitest)
npm run build                  # build de production dans dist/
```

En développement, `proxy.conf.json` relaie les appels `/api/**` vers `http://localhost:8080` :
le navigateur ne parle qu'à `localhost:4200`, il n'y a donc pas de CORS à configurer côté Spring.
