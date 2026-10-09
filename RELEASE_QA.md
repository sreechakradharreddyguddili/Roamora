# Roamora Release QA — 2026-09-22

## Checked in this pass

- Local frontend import references: no missing local imports found.
- Frontend JSX/JS parse check: all `src/**/*.jsx` and `src/**/*.js` files passed the TypeScript parser with type libraries disabled.
- CSS brace-balance check: all frontend CSS files balanced.
- JSX brace/parenthesis structural scan: all frontend JSX files balanced.
- Java brace-balance check: all backend Java files balanced.
- Invalid one-line Java text-block openings: the previously broken `PlaceService` pattern (`"""nwr...`) is removed.
- `PlaceResponse` and all `new PlaceResponse(...)` call sites were updated consistently.
- Explore card links now explicitly remove browser-style underlines.
- Stays and Explore both support typed destinations when autocomplete is unavailable.
- Nearby place retrieval now has multiple Overpass endpoint fallbacks.
- Stays exposes mapped hotel star classes from actual map data.

## Build limitation of this environment

A full dependency-backed build could not be certified inside this execution environment because the uploaded frontend did not contain a usable installed dependency tree and network dependency installation timed out. The backend Maven wrapper also requires Maven distribution/dependency downloads that are not available in this isolated environment. The source-level checks above were completed; run the two commands below on the development machine to perform the final dependency-backed build.

```powershell
cd frontend
npm ci
npm run build
npm run lint
```

```powershell
cd ..\backend
.\mvnw.cmd clean compile
```
