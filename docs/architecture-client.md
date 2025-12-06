# Architecture Frontend - Varlor

## Résumé Exécutif

Le frontend de Varlor est une application web moderne construite avec Next.js 16 et React 19, conçue pour offrir une expérience utilisateur exceptionnelle dans la gestion et l'analyse de données. L'architecture suit les meilleures pratiques actuelles avec une forte emphase sur la performance, la maintenabilité et l'évolutivité.

## Stack Technologique

### Framework Principal
- **Next.js 16.0.3**: Framework full-stack avec App Router
- **React 19.2.0**: Bibliothèque UI avec Server Components
- **TypeScript 5.7.2**: Typage strict avec enhanced type checking
- **Node.js**: Runtime environnement

### Interface Utilisateur
- **Tailwind CSS 4.1.0**: Système de design utility-first
- **Shadcn/ui**: Composants accessibles sur Radix UI
- **Lucide React**: Bibliothèque d'icônes
- **Apache ECharts 6.0.0**: Visualisation de données

### État et Données
- **Zustand 5.0.8**: Gestion d'état côté client
- **TanStack Query 5.90.10**: État serveur et cache
- **Axios 1.13.2**: Client HTTP avec intercepteurs

### Formulaires et Validation
- **React Hook Form 7.66.1**: Gestion formulaires
- **Zod 4.1.12**: Validation de schémas
- **@hookform/resolvers**: Intégration RHF + Zod

## Architecture Modèle

### 1. Architecture par Composants

#### Structure Hiérarchique
```
App Router (Next.js 16)
├── Layout Racine (providers)
├── Route Groups
│   ├── (auth)/ - Pages publiques
│   └── (dashboard)/ - Pages protégées
└── Server/Client Components
```

#### Pattern de Composition
- **Composants de UI**: Réutilisables, testables isolément
- **Composants de Page**: Orchestration des composants
- **Composants de Présentation**: Logic minimale
- **Composants de Container**: Gestion état et effets

### 2. Gestion d'État

#### Zustand Stores
```typescript
// Auth Store - État global authentification
interface AuthState {
  user: User | null
  accessToken: string | null
  isAuthenticated: boolean
  actions: {
    login: (credentials: LoginRequest) => Promise<void>
    logout: () => void
    refreshToken: () => Promise<void>
  }
}

// Cleaning Store - État opérationnel
interface CleaningState {
  status: 'idle' | 'processing' | 'completed' | 'error'
  progress: number
  error: string | null
  results: CleaningResults | null
}
```

#### TanStack Query
- **Query Keys**: Factorisées pour cohérence
- **Caching Stratégique**: TTL basé sur type de donnée
- **Background Refetch**: Automatique pour données critiques

### 3. Architecture API Client

#### Configuration Axios
```typescript
// lib/api/client.ts
const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  withCredentials: true, // Pour refresh tokens
})

// Interceptors
apiClient.interceptors.request.use(authInterceptor)
apiClient.interceptors.response.use(
  successInterceptor,
  errorInterceptor
)
```

#### Modules API
- **Auth**: `/lib/api/auth.ts` - Authentification
- **Datasets**: `/lib/api/datasets.ts` - Gestion datasets
- **Analysis**: `/lib/api/analysis.ts` - Analyse données
- **AI Insights**: `/lib/api/ai-insights.ts` - Insights IA
- **Reports**: `/lib/api/report.ts` - Génération rapports

### 4. Architecture de Routage

#### App Router Structure
```typescript
app/
├── (auth)/              # Groupe de routes auth
│   ├── layout.tsx       # Layout auth
│   ├── login/page.tsx   # Page connexion
│   └── register/page.tsx # Page inscription
├── (dashboard)/         # Groupe routes dashboard
│   ├── layout.tsx       # Layout avec sidebar
│   ├── dashboard/       # Dashboard principal
│   └── datasets/        # Gestion datasets
└── layout.tsx           # Layout racine
```

#### Protection des Routes
```typescript
// middleware/proxy.ts
export function middleware(request: NextRequest) {
  // Vérification token JWT
  // Redirection si non authentifié
  // Chargement état utilisateur
}
```

## Flux de Données

### 1. Pipeline de Upload
```
File Upload Zone → Validation → API Upload → Progress Tracking → Success → Redirect to Dataset
```

### 2. Pipeline d'Analyse
```
Dataset Selection → Start Cleaning → Poll Status → Display Results → Start Analysis → Poll Analysis → Display Visualizations
```

### 3. Flux Authentification
```
Login Form → API Login → Store Token → Redirect → Route Protection → Load User Data
```

## Patterns de Conception

### 1. Server Components Pattern
```typescript
// Page Server Component
export default async function DatasetPage({ params }: Props) {
  const dataset = await fetchDataset(params.id)

  return (
    <DatasetLayout>
      <DatasetHeader dataset={dataset} />
      <DatasetClient datasetId={params.id} />
    </DatasetLayout>
  )
}
```

### 2. Optimistic Updates
```typescript
// TanStack Query avec optimist
const mutation = useMutation({
  mutationFn: updateDataset,
  onMutate: async (newData) => {
    await queryClient.cancelQueries(['dataset'])
    const previous = queryClient.getQueryData(['dataset'])
    queryClient.setQueryData(['dataset'], newData)
    return { previous }
  },
  onError: (err, newData, context) => {
    queryClient.setQueryData(['dataset', context.previous])
  }
})
```

### 3. Error Boundary Pattern
```typescript
// Components/ErrorBoundary.tsx
export class ErrorBoundary extends Component<Props, State> {
  // Capture erreurs, afficher UI fallback
  // Logging pour débogage
}
```

## Performance

### 1. Optimisations Next.js
- **Bundle Analyzer**: Monitoring taille bundle
- **Image Optimization**: Formats AVIF/WebP
- **Code Splitting**: Par route et composant
- **Middleware Caching**: Réponses statiques

### 2. Optimisations React
- **React.memo**: Composants coûteux
- **useMemo/useCallback**: Calculs lourds
- **Virtualization**: Listes longues (future)

### 3. Stratégies de Cache
```typescript
// Configuration TanStack Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      retry: 3
    }
  }
})
```

## Sécurité

### 1. Gestion Tokens
```typescript
// httpOnly refresh token
// JWT access token court durée
// Rotation automatique
// SessionStorage pour état UI
```

### 2. Validation
```typescript
// Zod schémas pour toutes les réponses API
const datasetSchema = z.object({
  id: z.number(),
  name: z.string(),
  status: z.enum(['UPLOADING', 'PARSING', 'CLEANING', 'READY'])
})
```

### 3. CORS
- Configuration stricte
- Origines whitelistées
- Credentials support

## Tests

### 1. Tests Unitaires (Jest + RTL)
- 20 tests couvrant:
  - API client
  - Hooks personnalisés
  - Composants UI
  - Stores Zustand

### 2. Tests E2E (Playwright)
- 10 tests couvrant:
  - Flow authentification
  - Upload dataset
  - Pipeline analyse
  - Navigation

### 3. Tests d'Intégration
- Mock API endpoints
- Test interaction composants
- Validation formulaires

## Déploiement

### 1. Configuration Build
```typescript
// next.config.ts
const nextConfig: NextConfig = {
  // Optimisations production
  compiler: {
    removeConsole: process.env.NODE_ENV === 'production'
  },
  images: {
    formats: ['image/avif', 'image/webp']
  }
}
```

### 2. Variables Environnement
```bash
NEXT_PUBLIC_API_URL=http://localhost:3001
NODE_ENV=production
ANALYZE=false
```

## Évolutivité

### 1. Architecture Modulaire
- Composants indépendants
- Services découplés
- Interface claire entre modules

### 2. Internationalisation
- Structure prête pour i18n
- Composants text-externalisés
- Format supporté

### 3. Micro-frontends
- Architecture supporte futur splitting
- Shared components via design system
- API versioning

## Décisions Techniques Notables

1. **Next.js 16**: Early adoption App Router
2. **Tailwind CSS v4**: Performance Rust-based
3. **Zustand**: Simplicité vs Redux
4. **TanStack Query**: Gestion état serveur sophistiquée
5. **Shadcn/ui**: Contrôle total vs bibliothèque monolithique
6. **TypeScript Strict**: Qualité code et maintenabilité

Cette architecture frontend moderne assure une base solide pour l'évolution de Varlor tout en maintenant une excellente expérience développeur.