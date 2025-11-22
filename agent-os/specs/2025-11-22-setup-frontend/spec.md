# Specification: Varlor Frontend Technical Stack Setup

## Goal

Initialize a production-ready Next.js 16.0.3 frontend foundation with TypeScript, Tailwind CSS 4.1, Shadcn/ui, and Apache ECharts in the `varlor/app` directory. This creates the minimal technical infrastructure required for subsequent feature development, without implementing any application-specific layouts, pages, or business logic.

## User Stories

- As a frontend developer, I want a fully configured Next.js environment so that I can start building Varlor features immediately
- As a developer, I want TypeScript configured properly so that I have type safety throughout the codebase
- As a UI developer, I want Tailwind CSS 4.1 and Shadcn/ui ready so that I can build consistent, modern interfaces
- As a data visualization developer, I want Apache ECharts installed so that I can create charts when needed
- As a team member, I want a working dev server so that I can verify the setup is correct

## Core Requirements

### Next.js Setup
- Install Next.js 16.0.3 with App Router configuration
- Configure TypeScript with strict mode enabled
- Set up proper directory structure (`/app` folder for routes)
- Ensure `npm run dev` starts the development server successfully
- Configure basic `next.config.js` for future extensibility

### TypeScript Configuration
- Initialize TypeScript with `tsconfig.json`
- Enable strict type checking
- Configure path aliases for clean imports (e.g., `@/components`, `@/lib`)
- Set up proper module resolution for Next.js App Router

### Tailwind CSS 4.1
- Install and configure Tailwind CSS 4.1
- Set up `tailwind.config.js` with content paths for Next.js App Router
- Configure PostCSS if required
- Create base styles file importing Tailwind directives
- Verify Tailwind utilities work in development

### Shadcn/ui Foundation
- Install Shadcn/ui CLI and core dependencies
- Initialize Shadcn/ui with proper configuration
- Set up `components.json` configuration file
- Prepare directory structure for UI components
- NO components need to be installed at this stage (library ready for future use)

### Apache ECharts
- Install core Apache ECharts package
- NO advanced modules or configurations required
- NO example charts or implementations needed
- Just base library available for import

### Development Environment
- Functional `npm run dev` command
- Clean console output with no errors
- Hot module replacement working
- TypeScript compilation successful
- Tailwind CSS processing functional

## Visual Design

No visual design implementation required for this setup phase. This is stack-only infrastructure.

## Reusable Components

### Existing Code to Leverage
This is a greenfield project with no existing frontend code. The following will be created from scratch:
- Next.js 16.0.3 project structure
- TypeScript configuration
- Tailwind CSS 4.1 setup
- Shadcn/ui integration

### New Components Required
No UI components are required at this stage. Only the technical foundation and library configurations.

## Technical Approach

### Project Location
- Create frontend project in: `/Users/hugo/Perso/Projets/varlor/client/web`
- This should be set up as a git submodule
- The `client/` parent directory should be created first if it doesn't exist

### Installation Process
1. Initialize Next.js 16.0.3 with TypeScript and App Router
2. Configure TypeScript with strict settings
3. Install and configure Tailwind CSS 4.1
4. Initialize Shadcn/ui (library only, no components)
5. Install Apache ECharts core package
6. Verify development server runs without errors

### Directory Structure
After setup, the `client/web` directory should contain:
```
client/web/
├── app/                    # Next.js App Router directory
│   ├── layout.tsx          # Root layout (minimal)
│   ├── page.tsx            # Root page (minimal)
│   └── globals.css         # Global styles with Tailwind directives
├── components/             # Prepared for future components
│   └── ui/                 # Shadcn/ui components (empty initially)
├── lib/                    # Prepared for utilities
├── public/                 # Static assets
├── node_modules/
├── package.json
├── package-lock.json
├── tsconfig.json
├── next.config.js
├── tailwind.config.js
├── postcss.config.js       # If needed for Tailwind
├── components.json         # Shadcn/ui configuration
└── .gitignore
```

### Configuration Files

#### tsconfig.json
- Enable strict mode
- Configure path aliases:
  - `@/*` → `./` or `./*`
  - Ensure aliases work with Next.js App Router
- Set up proper module resolution

#### tailwind.config.js
- Configure content paths for App Router: `./app/**/*.{js,ts,jsx,tsx,mdx}`, `./components/**/*.{js,ts,jsx,tsx,mdx}`
- Prepare theme extension structure (but don't add custom theme yet)
- Enable dark mode support structure (class-based)

#### next.config.js
- Basic configuration for Next.js 16.0.3
- Enable TypeScript
- Prepare for future optimizations (but keep minimal)

#### components.json (Shadcn/ui)
- Configure style variant (default or custom)
- Set up component installation paths
- Configure utilities path
- Set up Tailwind integration

### Package Dependencies
Core packages to install:
- `next@16.0.3`
- `react` and `react-dom` (compatible versions)
- `typescript`
- `@types/react` and `@types/react-dom`
- `@types/node`
- `tailwindcss@4.1`
- `postcss` and `autoprefixer` (if required)
- Shadcn/ui dependencies (based on init command)
- `echarts` (core package only)

### Minimal Page Content
Root layout and page should contain only:
- Basic HTML structure
- Tailwind CSS classes to verify setup
- Simple text confirming the setup works
- NO application UI, headers, sidebars, or navigation

### Success Validation
After setup completion:
1. `npm install` completes without errors
2. `npm run dev` starts successfully
3. Browser loads `http://localhost:3000` without errors
4. TypeScript compilation shows no errors
5. Tailwind utilities apply correctly (verify with a test class)
6. Console shows no warnings or errors

## Out of Scope

The following are explicitly NOT included in this setup:

### Application Structure
- No layouts (header, sidebar, navigation)
- No application pages (dashboard, ontology, data catalog, etc.)
- No routing beyond default root page
- No page templates or scaffolding

### API & Data
- No API routes or route handlers
- No data fetching utilities
- No API client setup
- No backend integration
- No mock data files

### Design System
- No color palette configuration
- No dark mode implementation
- No typography system
- No spacing system
- No custom Tailwind theme
- No design tokens

### State Management
- No Zustand or Redux setup
- No context providers
- No state management patterns

### Authentication
- No Keycloak integration
- No NextAuth.js setup
- No auth utilities or middleware
- No protected routes

### Advanced Features
- No testing setup (Jest, Vitest, Playwright, etc.)
- No CI/CD configuration
- No Docker configuration
- No environment variable management beyond Next.js defaults
- No error boundaries
- No loading states
- No SEO configuration
- No analytics integration
- No performance monitoring

### Business Logic
- No domain-specific code
- No utility functions
- No business rules
- No data transformations

### Production Optimization
- No build optimizations
- No bundle analysis
- No performance tuning
- No deployment configuration

### External Integrations
- No third-party services
- No external APIs
- No CDN configuration

## Success Criteria

### Technical Validation
- Next.js 16.0.3 successfully installed with App Router enabled
- TypeScript compilation passes with zero errors
- Tailwind CSS 4.1 processes styles correctly
- Shadcn/ui configuration file created and valid
- Apache ECharts package installed and importable
- Development server starts in under 10 seconds
- Hot reload functions correctly when files are modified

### Developer Experience
- Clear console output with no warnings or errors
- Fast build times (initial build under 30 seconds)
- TypeScript IntelliSense working in modern IDEs
- Tailwind IntelliSense available (if IDE supports it)
- Project structure follows Next.js 16 best practices

### Code Quality
- All configuration files use proper syntax
- Package.json contains only necessary dependencies
- No deprecated packages or practices
- gitignore properly excludes node_modules, .next, etc.

### Functional Test
A developer can:
1. Clone the repository
2. Run `cd client/web && npm install`
3. Run `npm run dev`
4. Open browser to localhost:3000
5. See a simple working page
6. Make a change to a file and see hot reload work
7. Add a Tailwind class and see it apply
8. Import TypeScript types without errors

## Implementation Notes

### Version Specificity
- Exact version requirements: Next.js 16.0.3, Tailwind CSS 4.1
- Use compatible React version recommended by Next.js 16.0.3 documentation
- Lock versions in package.json to prevent breaking updates

### Setup Order
The setup must follow this sequence to avoid conflicts:
1. Next.js + TypeScript initialization
2. Tailwind CSS 4.1 installation
3. Shadcn/ui initialization
4. Apache ECharts installation
5. Verification and cleanup

### Configuration Philosophy
- Keep configurations minimal and focused
- Avoid premature optimization
- Don't add features "just in case"
- Follow official documentation defaults
- Prepare structure for future additions without implementing them

### Error Prevention
- Verify Node.js version compatibility before starting
- Check for port 3000 availability
- Ensure npm or pnpm is installed
- Clear any existing conflicting directories
- Test each major step before proceeding to the next

### Documentation
Include a minimal README.md in the `client/web` directory with:
- Node.js version requirement
- Installation instructions
- How to start development server
- Directory structure overview
- Next steps pointer (to be filled in later phases)

This setup creates the foundation upon which all future Varlor frontend features will be built. It prioritizes correctness, developer experience, and alignment with modern Next.js 16 best practices while maintaining strict minimalism.
