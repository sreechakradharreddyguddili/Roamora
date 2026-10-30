# Roamora — Two-section visual update

Only the requested sections were changed:

- How It Works: replaced the decorative route arc/grid illustration in the “02 / THE RESULT” section with a travel landscape photo panel. The existing hero photo remains unchanged.
- About: replaced the circular compass/ring illustration in the hero with a travel landscape photo panel.

Both photos use a local placeholder fallback if the remote image cannot load. The rest of the pages and application logic were not intentionally changed.

## Verification

- Checked that old decision-graph and About circle markup/styles are no longer referenced.
- Checked JSX section/div tag counts and CSS brace/parenthesis balance.
- ZIP integrity checked after packaging.
- Full ESLint/Vite build could not be run in this environment because npm dependency installation timed out and the environment could not resolve external hosts. Run `npm ci`, `npm run lint`, and `npm run build` on a connected development machine before release.
