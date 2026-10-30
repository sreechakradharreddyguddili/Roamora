# Roamora stability and discovery update

## Included

- Replaced abstract/orbital hero visuals with travel photography and a calmer photo-led layout; removed the AntiGravity wrapper/component.
- Added a home-page destination search and eight city/country inspiration cards on Explore.
- Added representative restaurant, café, hotel, hostel, guest-house and resort photography with a local fallback image for failed remote image requests.
- Clearly label generic business photos as representative instead of implying a photo belongs to a particular hotel or restaurant.
- Nearby-place and accommodation map links use returned coordinates when valid; if coordinates are missing, links search by place name/address rather than silently using `0,0`.
- Unknown hotel star classifications display “Star class not listed” rather than a misleading `0★`.
- Fixed static footer links, added URL-prefill support for planner/stays/explore, and wired common Copilot requests to navigation and mapped searches.
- Workspace Copilot prompt buttons now submit their prompt to the fixed-corner assistant.
- Added `AI_SETUP.md`, backend/frontend environment templates and a local photo fallback.
- Disabled decorative transitions/animations to keep the interface stable and reduce layout movement.

## Checks performed

- ESLint passed for the frontend `src` directory with no warnings/errors.
- JavaScript/JSX parsing passed for all 36 source files.
- Static internal `Link` destinations were checked against routes declared in `App.jsx`; no missing static routes were found.
- All JSX image elements have an `onError` fallback handler.
- No AntiGravity component/import remains in the application source.

## Remaining verification before launch

- A Vite production build could not be completed in this Linux environment because the uploaded `node_modules` lacked the platform-specific Rolldown native binding. The ZIP excludes `node_modules`; run `npm install` on Windows before `npm run build` to install the correct platform package.
- Remote Unsplash image endpoints could not be live HTTP-checked from this environment. Failed image loads now use the included local placeholder instead of leaving broken-image icons.
- Backend database/provider integration, live LLM calls, mobile browser/device testing, and production deployment were not certified by source-only checks.
