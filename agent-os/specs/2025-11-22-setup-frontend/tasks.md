# Task Breakdown: Varlor Frontend Technical Stack Setup

## Overview
Total Task Groups: 8
Total Tasks: ~40 sub-tasks

This setup creates a minimal, production-ready Next.js 16.0.3 foundation with TypeScript, Tailwind CSS 4.1, Shadcn/ui, and Apache ECharts. **No application features, layouts, or pages will be implemented** - only the technical infrastructure.

---

## Task List

### Infrastructure & Prerequisites

#### Task Group 1: Project Initialization
**Dependencies:** None

- [x] 1.0 Initialize frontend project foundation
  - [x] 1.1 Verify environment prerequisites
    - Check Node.js version compatibility (v18.17.0 or higher recommended for Next.js 16)
    - Verify npm or pnpm is installed and up to date
    - Confirm port 3000 is available for dev server
    - Document Node.js version requirement
  - [x] 1.2 Create project directory structure
    - Create `/Users/hugo/Perso/Projets/varlor/client` directory if it doesn't exist
    - Create `/Users/hugo/Perso/Projets/varlor/client/web` directory
    - Set up as git submodule (this will be the frontend app)
    - Verify parent directory permissions
    - Initialize directory as project root
  - [x] 1.3 Initialize Next.js 16.0.3 project
    - Run `npx create-next-app@16.0.3` with App Router enabled
    - Select TypeScript option during initialization
    - Accept default ESLint configuration
    - Use npm as package manager
    - **DO NOT** select Tailwind during initialization (will be configured manually with v4.1)
  - [x] 1.4 Verify base project structure
    - Confirm `/app` directory created (App Router)
    - Confirm `package.json` created with Next.js 16.0.3
    - Confirm `.gitignore` includes `node_modules/`, `.next/`, etc.
    - Verify initial TypeScript files present (`layout.tsx`, `page.tsx`)

**Acceptance Criteria:**
- `/Users/hugo/Perso/Projets/varlor/client/web` directory exists as a git submodule
- Next.js 16.0.3 installed with App Router
- Base project files present
- No errors during initialization

---

### TypeScript Configuration

#### Task Group 2: TypeScript Setup
**Dependencies:** Task Group 1

- [x] 2.0 Configure TypeScript for strict type safety
  - [x] 2.1 Review default `tsconfig.json`
    - Check Next.js-generated TypeScript configuration
    - Identify settings that need modification
  - [x] 2.2 Enable strict mode configuration
    - Set `"strict": true`
    - Enable `"noUncheckedIndexedAccess": true`
    - Enable `"noUnusedLocals": true`
    - Enable `"noUnusedParameters": true`
    - Set `"forceConsistentCasingInFileNames": true`
  - [x] 2.3 Configure path aliases
    - Set up `@/*` path mapping to `./` or `./*`
    - Ensure alias works with App Router structure
    - Configure `baseUrl` if necessary
    - Add common aliases: `@/components`, `@/lib`, `@/app`
  - [x] 2.4 Verify TypeScript compilation
    - Run `npx tsc --noEmit` to check for errors
    - Ensure zero compilation errors
    - Test path alias imports in a test file
    - Verify IntelliSense works in IDE

**Acceptance Criteria:**
- Strict mode enabled in `tsconfig.json`
- Path aliases configured and functional
- TypeScript compilation passes with zero errors
- IDE IntelliSense working for imports

---

### Styling Infrastructure

#### Task Group 3: Tailwind CSS 4.1 Setup
**Dependencies:** Task Groups 1-2

- [x] 3.0 Install and configure Tailwind CSS 4.1
  - [x] 3.1 Install Tailwind CSS 4.1 and dependencies
    - Run `npm install -D tailwindcss@4.1 postcss autoprefixer`
    - Verify correct versions in `package.json`
    - Check for any peer dependency warnings
  - [x] 3.2 Initialize Tailwind configuration
    - Run `npx tailwindcss init -p` to generate config files
    - Verify `tailwind.config.js` created
    - Verify `postcss.config.js` created (if applicable for v4.1)
  - [x] 3.3 Configure Tailwind content paths
    - Add `./app/**/*.{js,ts,jsx,tsx,mdx}` to content array
    - Add `./components/**/*.{js,ts,jsx,tsx,mdx}` to content array
    - Add `./lib/**/*.{js,ts,jsx,tsx,mdx}` if needed
    - Ensure paths align with Next.js App Router structure
  - [x] 3.4 Enable dark mode structure
    - Set `darkMode: 'class'` in tailwind.config.js
    - Prepare for class-based dark mode (implementation comes later)
    - DO NOT implement actual dark mode toggle or themes
  - [x] 3.5 Create global styles with Tailwind directives
    - Create or update `/app/globals.css`
    - Add `@tailwind base;`
    - Add `@tailwind components;`
    - Add `@tailwind utilities;`
    - Remove any unnecessary default styles
  - [x] 3.6 Import globals.css in root layout
    - Import `./globals.css` in `/app/layout.tsx`
    - Ensure CSS is loaded before any components render
  - [x] 3.7 Verify Tailwind is working
    - Add test Tailwind classes to root page (`bg-blue-500`, `text-white`, etc.)
    - Start dev server with `npm run dev`
    - Confirm styles apply in browser at localhost:3000
    - Test utility classes, spacing, colors work as expected
  - [x] 3.8 Clean up test styles
    - Remove test classes from root page
    - Keep only minimal content for verification
    - Ensure no styling leaks into production setup

**Acceptance Criteria:**
- Tailwind CSS 4.1 installed and configured
- Content paths correctly set for App Router
- Dark mode structure prepared (class-based)
- Tailwind utilities work in development
- Clean console output with no CSS warnings

---

### Component Library Setup

#### Task Group 4: Shadcn/ui Foundation
**Dependencies:** Task Groups 1-3

- [x] 4.0 Initialize Shadcn/ui library (no components)
  - [x] 4.1 Install Shadcn/ui CLI and dependencies
    - Run `npx shadcn-ui@latest init` (or appropriate v4.1-compatible command)
    - Follow initialization prompts
    - Accept or configure style variant (default recommended)
    - Accept TypeScript option
  - [x] 4.2 Configure components.json
    - Set `style` (e.g., "default" or "new-york")
    - Set `rsc: true` for React Server Components compatibility
    - Configure `tsx: true` for TypeScript
    - Set `tailwind.config` path correctly
    - Set `aliases.components` to `@/components`
    - Set `aliases.utils` to `@/lib/utils`
  - [x] 4.3 Create component directory structure
    - Ensure `/components/ui/` directory exists
    - Create `/lib/` directory if not present
    - Verify utils file created (e.g., `/lib/utils.ts` with `cn` helper)
  - [x] 4.4 Verify Shadcn/ui setup
    - Check that `components.json` is valid JSON
    - Ensure path aliases align with tsconfig.json
    - Confirm `lib/utils.ts` contains `cn()` function for className merging
    - **DO NOT install any components yet** (e.g., no Button, Card, etc.)
  - [x] 4.5 Test component installation capability (optional verification)
    - Optionally test by installing one test component (e.g., `npx shadcn-ui add button`)
    - Verify it installs correctly to `/components/ui/`
    - If tested, immediately remove test component
    - Confirm installation process works for future use

**Acceptance Criteria:**
- Shadcn/ui CLI initialized successfully
- `components.json` created and valid
- Directory structure prepared (`/components/ui/`, `/lib/`)
- `cn()` utility function available in `/lib/utils.ts`
- Library ready for component installation (but no components installed)

---

### Data Visualization Library

#### Task Group 5: Apache ECharts Installation
**Dependencies:** Task Groups 1-2

- [x] 5.0 Install Apache ECharts core library
  - [x] 5.1 Install ECharts npm package
    - Run `npm install echarts`
    - Verify package added to `dependencies` in package.json
    - Check for any peer dependency warnings
  - [x] 5.2 Verify ECharts is importable
    - Create a test import in a TypeScript file: `import * as echarts from 'echarts';`
    - Ensure no TypeScript errors on import
    - Confirm types are available (ECharts includes built-in types)
  - [x] 5.3 Confirm no configuration needed
    - **DO NOT** create chart examples or configurations
    - **DO NOT** install advanced ECharts modules (GL, maps, etc.)
    - **DO NOT** create wrapper components
    - Simply ensure library is available for future use
  - [x] 5.4 Remove test imports
    - Clean up any test import statements
    - Ensure no ECharts code in production files

**Acceptance Criteria:**
- ECharts package installed
- Library importable without errors
- TypeScript types available
- No example code or configurations present

---

### Project Structure & Configuration

#### Task Group 6: Directory Structure & Configuration Files
**Dependencies:** Task Groups 1-5

- [x] 6.0 Finalize project structure and configurations
  - [x] 6.1 Create remaining directory structure
    - Create `/components/` directory (if not already present)
    - Create `/lib/` directory (if not already present)
    - Create `/public/` directory for static assets (if not already present)
    - **DO NOT** create feature directories (no `/features`, `/modules`, etc.)
  - [x] 6.2 Configure next.config.js
    - Create or update `next.config.js` with TypeScript support
    - Use minimal configuration appropriate for Next.js 16.0.3
    - Enable `reactStrictMode: true`
    - **DO NOT** add experimental features or optimizations
    - Keep configuration extensible for future needs
  - [x] 6.3 Update .gitignore
    - Ensure `.next/` excluded
    - Ensure `node_modules/` excluded
    - Ensure `.env*.local` excluded
    - Add any other Next.js-specific ignore patterns
  - [x] 6.4 Review and lock package versions
    - Verify `package.json` contains exact versions (not ranges)
    - Confirm Next.js is locked to 16.0.3
    - Confirm Tailwind is locked to 4.1.x
    - Review all dependencies for security warnings
  - [x] 6.5 Create minimal root page
    - Update `/app/page.tsx` with simple content
    - Add text like "Varlor Frontend - Setup Complete"
    - Include one Tailwind class to verify styling works
    - **DO NOT** create any application UI or layout
  - [x] 6.6 Create minimal root layout
    - Update `/app/layout.tsx` with basic HTML structure
    - Import global styles
    - Add minimal metadata (title, description)
    - Ensure children are rendered
    - **DO NOT** add headers, footers, or navigation
  - [x] 6.7 Create README.md
    - Document Node.js version requirement (v18.17.0+)
    - Add installation instructions: `npm install`
    - Add dev server start: `npm run dev`
    - List installed technologies (Next.js 16.0.3, TypeScript, Tailwind 4.1, Shadcn/ui, ECharts)
    - Add brief directory structure overview
    - Add "Next Steps" placeholder section
  - [x] 6.8 Final verification of project structure
    - Verify directory tree matches expected structure:
      ```
      client/web/
      ├── app/
      │   ├── layout.tsx
      │   ├── page.tsx
      │   └── globals.css
      ├── components/
      │   └── ui/
      ├── lib/
      │   └── utils.ts
      ├── public/
      ├── node_modules/
      ├── package.json
      ├── package-lock.json
      ├── tsconfig.json
      ├── next.config.js
      ├── tailwind.config.js
      ├── postcss.config.js
      ├── components.json
      ├── .gitignore
      └── README.md
      ```

**Acceptance Criteria:**
- All directories created as specified
- Configuration files complete and valid
- Root page and layout contain minimal content only
- README.md provides clear setup instructions
- Project structure follows Next.js 16 best practices

---

### Validation & Testing

#### Task Group 7: Development Environment Validation
**Dependencies:** Task Groups 1-6 (All previous tasks must be complete)

- [x] 7.0 Validate complete development environment
  - [x] 7.1 Clean install test
    - Delete `node_modules/` and `package-lock.json`
    - Run fresh `npm install`
    - Verify installation completes without errors
    - Check for any security vulnerabilities with `npm audit`
  - [x] 7.2 TypeScript compilation test
    - Run `npx tsc --noEmit`
    - Verify zero TypeScript errors
    - Ensure strict mode catches potential issues
  - [x] 7.3 Development server test
    - Run `npm run dev`
    - Verify server starts in under 10 seconds
    - Check console output is clean (no errors or warnings)
    - Confirm startup message shows correct port (3000)
  - [x] 7.4 Browser rendering test
    - Open browser to `http://localhost:3000`
    - Verify page loads without errors
    - Check browser console for JavaScript errors
    - Confirm simple page content renders correctly
  - [x] 7.5 Hot module replacement test
    - With dev server running, edit `/app/page.tsx`
    - Make a visible text change
    - Save file and verify browser updates without refresh
    - Confirm HMR works within 2-3 seconds
  - [x] 7.6 Tailwind CSS validation test
    - Add a test Tailwind utility class (e.g., `bg-red-500`)
    - Save and verify style applies in browser
    - Test multiple utilities (spacing, typography, colors)
    - Remove test classes after verification
  - [x] 7.7 TypeScript IntelliSense test
    - Open IDE and verify IntelliSense works for React imports
    - Test path alias autocomplete (e.g., `@/components`)
    - Verify type checking in editor matches CLI
  - [x] 7.8 Build test (optional but recommended)
    - Run `npm run build`
    - Verify production build completes successfully
    - Check build output for errors or warnings
    - Confirm build generates optimized bundles
    - **DO NOT** commit build artifacts to git

**Acceptance Criteria:**
- Fresh install completes without errors
- TypeScript compilation passes with zero errors
- Dev server starts cleanly and quickly
- Browser loads page without errors
- Hot reload works correctly
- Tailwind utilities apply as expected
- IDE IntelliSense functional
- Production build succeeds (if tested)

---

### Documentation & Handoff

#### Task Group 8: Final Documentation
**Dependencies:** Task Group 7 (COMPLETED)

- [x] 8.0 Complete setup documentation
  - [x] 8.1 Verify README.md completeness
    - Ensure all setup steps documented clearly
    - Add troubleshooting section for common issues
    - Include link to Next.js 16 documentation
    - Add section on installed libraries and their versions
  - [x] 8.2 Document configuration decisions
    - Add inline comments to `tsconfig.json` explaining strict settings
    - Add inline comments to `app/globals.css` explaining Tailwind v4 configuration
    - Add inline comments to `next.config.ts` explaining choices
    - Add inline comments to `components.json` explaining options
  - [x] 8.3 Create architecture notes
    - Document why specific versions were chosen
    - Note any deviations from default configurations
    - List any known limitations or considerations
    - Created comprehensive `ARCHITECTURE.md` document
  - [x] 8.4 Prepare handoff checklist
    - Confirm all task groups completed
    - Verify all acceptance criteria met
    - Document any issues encountered and resolutions
    - Note any recommendations for next phase
    - Created comprehensive `HANDOFF.md` document

**Acceptance Criteria:**
- README.md is comprehensive and clear
- Configuration files have helpful comments
- Documentation supports next development phase
- Setup is ready for feature development

---

## Execution Order

**Recommended implementation sequence:**

1. **Infrastructure & Prerequisites** (Task Group 1) - Initialize project foundation ✅
2. **TypeScript Configuration** (Task Group 2) - Configure type safety and aliases ✅
3. **Styling Infrastructure** (Task Group 3) - Set up Tailwind CSS 4.1 ✅
4. **Component Library Setup** (Task Group 4) - Initialize Shadcn/ui ✅
5. **Data Visualization Library** (Task Group 5) - Install Apache ECharts ✅
6. **Project Structure & Configuration** (Task Group 6) - Finalize structure and configs ✅
7. **Validation & Testing** (Task Group 7) - Comprehensive environment validation ✅
8. **Documentation & Handoff** (Task Group 8) - Complete documentation ✅

---

## Implementation Notes

### Critical Success Factors
- **Version specificity**: Must use exact versions (Next.js 16.0.3, Tailwind 4.1)
- **Minimal scope**: Resist urge to add "just one more feature"
- **Sequential execution**: Follow task order to avoid dependency conflicts
- **Validation at each step**: Test before moving to next task group

### Common Pitfalls to Avoid
- Installing Tailwind during Next.js init (need v4.1 specifically)
- Adding application features or layouts (strictly out of scope)
- Installing Shadcn/ui components (library only at this stage)
- Configuring advanced ECharts features (core library only)
- Setting up state management, auth, or testing (future phases)

### Testing Philosophy
- **No formal test suite required** for this scaffolding phase
- Validation through manual testing only
- Focus on "does it run?" not "does it work correctly?"
- Comprehensive testing comes in future feature development

### Git Workflow
- Consider committing after each major task group
- Use clear commit messages (e.g., "Setup Next.js 16.0.3 foundation")
- Ensure `.gitignore` excludes build artifacts and dependencies

---

## Success Metrics

Upon completion, a developer should be able to:

1. Clone the repository
2. Run `cd /Users/hugo/Perso/Projets/varlor/client/web && npm install`
3. Run `npm run dev`
4. Open browser to localhost:3000 and see a working page
5. Make a change and see hot reload work
6. Start building features immediately with TypeScript, Tailwind, and all libraries available

**Total estimated time: 2-4 hours** (depending on experience level and system performance)

---

## Out of Scope Reminders

The following are explicitly **NOT** included and should be ignored:

- Application layouts (header, sidebar, navigation)
- Application pages (dashboard, ontology, data catalog)
- API routes or backend integration
- Design system (colors, typography, spacing)
- Dark mode implementation
- State management (Zustand, Redux, Context)
- Authentication (Keycloak, NextAuth)
- Testing frameworks (Jest, Vitest, Playwright)
- CI/CD, Docker, deployment configuration
- Business logic or domain-specific code

These will be addressed in subsequent specification phases.

---

## Final Status

**ALL TASK GROUPS COMPLETE** ✅

The Varlor frontend foundation is production-ready and fully documented. All 8 task groups have been completed successfully with all acceptance criteria met. The setup is ready for feature development.

### Documentation Delivered:
- ✅ README.md - Comprehensive setup guide with troubleshooting
- ✅ ARCHITECTURE.md - Architecture decisions and rationale
- ✅ HANDOFF.md - Complete handoff report and checklist
- ✅ Inline comments in all configuration files

### Next Phase:
Ready for application structure and feature development.
