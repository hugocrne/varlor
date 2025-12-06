# Story 1.5: Incremental Sync Support

Status: Ready for Development

## Story

As a system administrator,
I want Varlor to sync only changed data,
So that sync operations are faster and use fewer resources.

## Acceptance Criteria

1. **Given** I have configured a connector that supports incremental sync
   **When** I am setting up the sync schedule
   **Then** I can choose between full sync or incremental sync modes
   **And** the system displays the detected incremental sync capabilities
   **And** I can configure incremental sync settings specific to the connector type

2. **Given** I have enabled incremental sync for a PostgreSQL database connector
   **When** the sync runs
   **Then** the system uses Change Data Capture (CDC) or timestamp-based tracking
   **And** only new, updated, or deleted records since the last sync are processed
   **And** the sync completes significantly faster than a full sync
   **And** the watermarks are updated with the latest sync position

3. **Given** I am using incremental sync with an API connector
   **When** the API supports cursor-based pagination or modified timestamps
   **Then** the system detects and uses the appropriate incremental sync strategy
   **And** processes only new or modified records since the last sync
   **And** handles API rate limits and pagination automatically

4. **Given** I have configured incremental sync for cloud storage
   **When** files are added, modified, or deleted
   **Then** the system detects file changes using modification timestamps and checksums
   **And** only processes changed files during sync operations
   **And** maintains file version history for audit purposes

5. **Given** an incremental sync fails due to connector issues or data inconsistencies
   **When** the failure occurs
   **Then** the system logs the error with detailed context
   **And** automatically falls back to full sync if incremental sync is not possible
   **And** I can manually trigger a full resync from the dashboard
   **And** the system tracks sync performance metrics to detect efficiency degradation

6. **Given** I want to monitor incremental sync performance
   **When** I view the sync dashboard
   **Then** I see detailed metrics comparing incremental vs full sync performance
   **And** I can view watermark history and sync efficiency trends
   **And** I receive alerts when incremental sync efficiency drops below thresholds
   **And** I can analyze sync patterns to optimize connector configurations

## Tasks / Subtasks

- [ ] **Backend: Incremental Sync Models** (AC: 1, 2, 5, 6)
  - [ ] Create `sync_watermarks` table to track last sync state
  - [ ] Create `sync_performance_metrics` table for performance tracking
  - [ ] Add incremental sync configuration to sync schedules
  - [ ] Implement proper indexing for watermark queries
  - [ ] Add data consistency validation mechanisms

- [ ] **Backend: Database CDC Implementation** (AC: 2, 5)
  - [ ] Implement PostgreSQL logical decoding CDC service
  - [ ] Add timestamp-based incremental sync for databases without CDC
  - [ ] Create change event processors for database operations
  - [ ] Implement schema change detection and handling
  - [ ] Add support for database-specific incremental sync strategies

- [ ] **Backend: API Incremental Sync** (AC: 3, 5)
  - [ ] Implement cursor-based pagination tracking
  - [ ] Add last-modified timestamp detection for APIs
  - [ ] Create ETag and header-based change detection
  - [ ] Implement API-specific incremental sync strategies
  - [ ] Add rate limiting awareness for incremental API calls

- [ ] **Backend: Cloud Storage Incremental Sync** (AC: 4, 5)
  - [ ] Implement file checksum comparison (MD5/SHA256)
  - [ ] Add file modification timestamp tracking
  - [ ] Create incremental file processing logic
  - [ ] Implement file deletion detection and handling
  - [ ] Add support for cloud provider-specific change APIs

- [ ] **Backend: Incremental Sync Engine** (AC: 1, 2, 3, 4, 5, 6)
  - [ ] Create `IncrementalSyncService` as core orchestrator
  - [ ] Implement watermark management and storage
  - [ ] Add change detection strategies per connector type
  - [ ] Create fallback to full sync mechanisms
  - [ ] Implement sync efficiency monitoring and alerting

- [ ] **Backend: Performance Optimization** (AC: 2, 5, 6)
  - [ ] Implement batch processing for incremental changes
  - [ ] Add memory-efficient change record processing
  - [ ] Create sync performance benchmarking
  - [ ] Implement adaptive sync strategies based on data volume
  - [ ] Add sync time estimation and progress tracking

- [ ] **Frontend: Incremental Sync Configuration** (AC: 1, 3, 4, 5, 6)
  - [ ] Create IncrementalSyncConfig component
  - [ ] Add connector capability detection UI
  - [ ] Create incremental sync settings forms
  - [ ] Add sync strategy preview and validation
  - [ ] Create full resync trigger functionality

- [ ] **Frontend: Performance Monitoring** (AC: 5, 6)
  - [ ] Create IncrementalSyncMetrics component
  - [ ] Add sync efficiency visualization
  - [ ] Create watermark history viewer
  - [ ] Add sync performance comparison charts
  - [ ] Create sync optimization recommendations

- [ ] **Testing** (All ACs)
  - [ ] Unit tests for incremental sync algorithms
  - [ ] Integration tests with mock CDC data streams
  - [ ] Performance tests comparing incremental vs full sync
  - [ ] Failure scenario testing and recovery validation
  - [ ] End-to-end test for complete incremental sync workflow

## Dev Notes

### Technical Requirements

#### Backend Implementation Details

**1. Database Schema for Incremental Sync**
```sql
-- Sync watermarks for tracking last sync position
CREATE TABLE sync_watermarks (
  id SERIAL PRIMARY KEY,
  schedule_id INTEGER NOT NULL REFERENCES sync_schedules(id),
  connector_id INTEGER NOT NULL REFERENCES database_connections(id),
  sync_type VARCHAR(20) NOT NULL, -- 'incremental' or 'full'
  watermark_data JSONB NOT NULL, -- Connector-specific watermark data
  records_processed INTEGER DEFAULT 0,
  records_created INTEGER DEFAULT 0,
  records_updated INTEGER DEFAULT 0,
  records_deleted INTEGER DEFAULT 0,
  sync_started_at TIMESTAMP,
  sync_completed_at TIMESTAMP,
  sync_duration_ms INTEGER,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(schedule_id, connector_id),
  INDEX idx_sync_watermarks_schedule (schedule_id),
  INDEX idx_sync_watermarks_connector (connector_id),
  INDEX idx_sync_watermarks_updated (updated_at)
);

-- Sync performance metrics for monitoring
CREATE TABLE sync_performance_metrics (
  id SERIAL PRIMARY KEY,
  schedule_id INTEGER NOT NULL REFERENCES sync_schedules(id),
  sync_type VARCHAR(20) NOT NULL,
  records_processed INTEGER,
  data_size_bytes BIGINT,
  sync_duration_ms INTEGER,
  throughput_records_per_second DECIMAL(10,2),
  throughput_mb_per_second DECIMAL(10,2),
  efficiency_score DECIMAL(5,2), -- 0-100, incremental vs full comparison
  created_at TIMESTAMP DEFAULT NOW(),
  INDEX idx_sync_perf_schedule (schedule_id),
  INDEX idx_sync_perf_created (created_at)
);
```

**2. Incremental Sync Service**
```typescript
// app/services/incremental_sync_service.ts
export default class IncrementalSyncService {
  static async syncIncremental(schedule: SyncSchedule): Promise<SyncResult> {
    const connector = schedule.connector
    const lastWatermark = await this.getLastWatermark(schedule.id, connector.id)

    // Get appropriate sync strategy
    const strategy = this.getSyncStrategy(connector.type, connector.config)

    try {
      // Perform incremental sync
      const result = await strategy.syncIncremental(connector, lastWatermark)

      // Update watermarks
      await this.updateWatermark(schedule.id, connector.id, {
        watermarkData: result.watermark,
        recordsProcessed: result.recordsProcessed,
        recordsCreated: result.recordsCreated,
        recordsUpdated: result.recordsUpdated,
        recordsDeleted: result.recordsDeleted,
        syncType: 'incremental',
        syncStartedAt: new Date(),
        syncCompletedAt: new Date()
      })

      // Record performance metrics
      await this.recordPerformanceMetrics(schedule.id, result)

      return result

    } catch (error) {
      // Handle incremental sync failure
      console.error(`Incremental sync failed for schedule ${schedule.id}:`, error)

      // Check if we should fallback to full sync
      if (await this.shouldFallbackToFullSync(error, schedule)) {
        return this.performFullSyncFallback(schedule)
      }

      throw error
    }
  }

  private static getSyncStrategy(connectorType: string, config: any): IncrementalSyncStrategy {
    switch (connectorType) {
      case 'database':
        return this.getDatabaseSyncStrategy(config)
      case 'api':
        return this.getAPISyncStrategy(config)
      case 'cloud':
        return this.getCloudSyncStrategy(config)
      default:
        throw new Error(`Unsupported connector type: ${connectorType}`)
    }
  }

  private static getDatabaseSyncStrategy(config: DatabaseConfig): DatabaseIncrementalSyncStrategy {
    if (config.type === 'postgresql') {
      // Check if CDC is available
      if (config.cdcEnabled) {
        return new PostgreSQLCDCStrategy(config)
      } else {
        return new TimestampStrategy(config)
      }
    }

    // Default to timestamp strategy for other databases
    return new TimestampStrategy(config)
  }
}
```

**3. Database CDC Strategy Implementation**
```typescript
// app/services/strategies/postgresql_cdc_strategy.ts
export default class PostgreSQLCDCStrategy implements DatabaseIncrementalSyncStrategy {
  private client: Client
  private logicalReplicationSlot: string

  constructor(private config: PostgreSQLConfig) {
    this.client = new Client(config.connectionString)
    this.logicalReplicationSlot = `varlor_cdc_${config.connectorId}`
  }

  async initialize(): Promise<void> {
    await this.client.connect()

    // Create logical replication slot if it doesn't exist
    try {
      await this.client.query(
        `SELECT pg_create_logical_replication_slot($1, 'wal2json')`,
        [this.logicalReplicationSlot]
      )
    } catch (error) {
      // Slot might already exist
      if (error.code !== 'P0004') {
        throw error
      }
    }
  }

  async syncIncremental(connector: DatabaseConnector, watermark: Watermark): Promise<SyncResult> {
    const lsn = watermark?.lsn || '0/16BAD000' // Start from beginning if no watermark

    // Get changes since last watermark
    const changesQuery = `
      SELECT * FROM pg_logical_slot_get_changes(
        $1::text,
        $2::pg_lsn,
        $3::bigint,
        'include-timestamp, include-xids'
      )
    `

    const result = await this.client.query(changesQuery, [
      this.logicalReplicationSlot,
      lsn,
      '1000000' // Max changes
    ])

    const changes = result.rows.map(this.parseCDCChange)
    const processedChanges = await this.processChanges(changes)

    return {
      recordsProcessed: processedChanges.count,
      recordsCreated: processedChanges.created,
      recordsUpdated: processedChanges.updated,
      recordsDeleted: processedChanges.deleted,
      watermark: { lsn: processedChanges.lastLSN },
      metadata: {
        changeCount: changes.length,
        processedAt: new Date().toISOString()
      }
    }
  }

  private parseCDCChange(row: any): CDCChange {
    const data = JSON.parse(row.data)

    return {
      operation: data.operation, // INSERT, UPDATE, DELETE
      table: data.table,
      schema: data.schema,
      data: data.record,
      oldData: data.old_record,
      timestamp: new Date(data.timestamp),
      lsn: row.lsn
    }
  }

  private async processChanges(changes: CDCChange[]): Promise<ProcessedChanges> {
    const result: ProcessedChanges = {
      count: 0,
      created: 0,
      updated: 0,
      deleted: 0,
      lastLSN: '0/0'
    }

    for (const change of changes) {
      switch (change.operation) {
        case 'INSERT':
          await this.handleInsert(change)
          result.created++
          break
        case 'UPDATE':
          await this.handleUpdate(change)
          result.updated++
          break
        case 'DELETE':
          await this.handleDelete(change)
          result.deleted++
          break
      }

      result.count++
      result.lastLSN = change.lsn
    }

    return result
  }

  async cleanup(): Promise<void> {
    await this.client.end()
  }
}
```

**4. Timestamp-Based Strategy**
```typescript
// app/services/strategies/timestamp_strategy.ts
export default class TimestampStrategy implements DatabaseIncrementalSyncStrategy {
  async syncIncremental(connector: DatabaseConnector, watermark: Watermark): Promise<SyncResult> {
    const lastTimestamp = watermark?.lastTimestamp || new Date('1970-01-01')

    // Build incremental query
    const query = this.buildIncrementalQuery(connector, lastTimestamp)

    // Execute query and process results
    const changes = await this.executeQuery(connector, query)
    const processedChanges = await this.processDatabaseChanges(changes)

    return {
      recordsProcessed: processedChanges.count,
      recordsCreated: processedChanges.created,
      recordsUpdated: processedChanges.updated,
      recordsDeleted: processedChanges.deleted,
      watermark: {
        lastTimestamp: processedChanges.maxTimestamp,
        lastId: processedChanges.maxId
      },
      metadata: {
        queryType: 'incremental',
        timeRange: {
          from: lastTimestamp.toISOString(),
          to: processedChanges.maxTimestamp.toISOString()
        }
      }
    }
  }

  private buildIncrementalQuery(connector: DatabaseConnector, lastTimestamp: Date): string {
    const tableName = connector.config.table
    const timestampColumn = connector.config.timestampColumn || 'updated_at'

    // Check for soft delete support
    const softDeleteColumn = connector.config.softDeleteColumn

    if (softDeleteColumn) {
      return `
        SELECT *, '${timestampColumn}' as sync_operation
        FROM ${tableName}
        WHERE ${timestampColumn} > $1
          AND ${softDeleteColumn} = false
        ORDER BY ${timestampColumn}, id
      `
    } else {
      // For tables without soft delete, we need separate handling
      return `
        SELECT *, '${timestampColumn}' as sync_operation
        FROM ${tableName}
        WHERE ${timestampColumn} > $1
        ORDER BY ${timestampColumn}, id
      `
    }
  }
}
```

**5. API Incremental Sync Strategy**
```typescript
// app/services/strategies/api_incremental_strategy.ts
export default class APIIncrementalSyncStrategy implements IncrementalSyncStrategy {
  async syncIncremental(connector: APIConnector, watermark: Watermark): Promise<SyncResult> {
    const strategy = this.detectAPIIncrementalStrategy(connector)

    switch (strategy.type) {
      case 'cursor':
        return this.syncWithCursor(connector, watermark, strategy)
      case 'timestamp':
        return this.syncWithTimestamp(connector, watermark, strategy)
      case 'etag':
        return this.syncWithETag(connector, watermark, strategy)
      default:
        throw new Error('API does not support incremental sync')
    }
  }

  private detectAPIIncrementalStrategy(connector: APIConnector): APISyncStrategy {
    const config = connector.config

    // Check for common API patterns
    if (config.supportsCursors) {
      return { type: 'cursor', cursorParam: config.cursorParam || 'cursor' }
    }

    if (config.supportsModifiedSince) {
      return {
        type: 'timestamp',
        timestampParam: config.modifiedSinceParam || 'modified_since',
        timestampFormat: config.timestampFormat || 'iso'
      }
    }

    if (config.supportsETags) {
      return { type: 'etag', etagHeader: config.etagHeader || 'ETag' }
    }

    // Try to auto-detect from API responses
    return this.autoDetectAPIStrategy(connector)
  }

  private async syncWithCursor(connector: APIConnector, watermark: Watermark, strategy: APISyncStrategy): Promise<SyncResult> {
    const results: any[] = []
    let cursor = watermark?.cursor || null
    let totalProcessed = 0

    do {
      const response = await this.makeAPICall(connector, {
        [strategy.cursorParam]: cursor,
        limit: 100 // Process in batches
      })

      results.push(...response.data)
      totalProcessed += response.data.length
      cursor = response.nextCursor

      // Store checkpoint for recovery
      if (totalProcessed % 1000 === 0) {
        await this.storeCheckpoint(connector.id, { cursor, processed: totalProcessed })
      }

    } while (cursor && totalProcessed < connector.config.maxRecords)

    return {
      recordsProcessed: totalProcessed,
      recordsCreated: results.length,
      recordsUpdated: 0, // APIs typically only return current state
      recordsDeleted: 0,
      watermark: { cursor },
      metadata: {
        apiCalls: Math.ceil(totalProcessed / 100),
        strategy: 'cursor'
      }
    }
  }
}
```

**6. Cloud Storage Incremental Sync**
```typescript
// app/services/strategies/cloud_incremental_strategy.ts
export default class CloudIncrementalSyncStrategy implements IncrementalSyncStrategy {
  async syncIncremental(connector: CloudConnector, watermark: Watermark): Promise<SyncResult> {
    const storageService = CloudStorageService.create(connector.provider)
    const lastSync = watermark?.lastSync || new Date('1970-01-01')

    // List files modified since last sync
    const files = await storageService.listFilesModifiedSince(
      connector.config,
      lastSync
    )

    const processedFiles: ProcessedFile[] = []

    for (const file of files) {
      const processedFile = await this.processCloudFile(connector, file)
      processedFiles.push(processedFile)
    }

    // Check for deleted files
    const deletedFiles = await this.detectDeletedFiles(connector, processedFiles)

    const maxTimestamp = files.length > 0
      ? Math.max(...files.map(f => f.modifiedAt.getTime()))
      : lastSync.getTime()

    return {
      recordsProcessed: processedFiles.length + deletedFiles.length,
      recordsCreated: processedFiles.filter(f => f.isNew).length,
      recordsUpdated: processedFiles.filter(f => f.isUpdated).length,
      recordsDeleted: deletedFiles.length,
      watermark: {
        lastSync: new Date(maxTimestamp),
        fileChecksums: processedFiles.map(f => ({ path: f.path, checksum: f.checksum }))
      },
      metadata: {
        totalSize: processedFiles.reduce((sum, f) => sum + f.size, 0),
        provider: connector.provider
      }
    }
  }

  private async processCloudFile(connector: CloudConnector, file: CloudFile): Promise<ProcessedFile> {
    // Compare checksum with last known checksum
    const lastChecksum = await this.getLastFileChecksum(connector.id, file.path)
    const currentChecksum = await this.calculateFileChecksum(file)

    if (lastChecksum === currentChecksum) {
      return { path: file.path, checksum: currentChecksum, isUpdated: false, isNew: false }
    }

    // Process the file through the existing pipeline
    const processedData = await this.processFileThroughPipeline(file)

    return {
      path: file.path,
      checksum: currentChecksum,
      isUpdated: lastChecksum !== null,
      isNew: lastChecksum === null,
      size: file.size,
      processedData
    }
  }
}
```

#### Frontend Implementation Details

**1. Incremental Sync Configuration Component**
```typescript
// app/components/sync/IncrementalSyncConfig.tsx
export function IncrementalSyncConfig({ connector, schedule, onChange }: IncrementalSyncConfigProps) {
  const [incrementalMode, setIncrementalMode] = useState<'none' | 'available' | 'enabled'>('none')
  const [strategy, setStrategy] = useState<IncrementalStrategy | null>(null)
  const [config, setConfig] = useState<IncrementalConfig>({})

  useEffect(() => {
    // Detect incremental sync capabilities
    const capabilities = detectIncrementalCapabilities(connector)
    if (capabilities.supported) {
      setIncrementalMode(capabilities.available ? 'available' : 'enabled')
      setStrategy(capabilities.recommendedStrategy)
    }
  }, [connector])

  const handleStrategyChange = (newStrategy: IncrementalStrategy) => {
    setStrategy(newStrategy)
    onChange({ incrementalStrategy: newStrategy, config })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Incremental Sync Configuration</CardTitle>
      </CardHeader>
      <CardContent>
        {incrementalMode === 'none' && (
          <Alert>
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              This connector does not support incremental sync. Full sync will be used.
            </AlertDescription>
          </Alert>
        )}

        {incrementalMode === 'available' && (
          <div className="space-y-4">
            <div>
              <Label>Enable Incremental Sync</Label>
              <Switch
                checked={incrementalMode === 'enabled'}
                onCheckedChange={(checked) =>
                  setIncrementalMode(checked ? 'enabled' : 'available')
                }
              />
              <p className="text-sm text-gray-600">
                Sync only changed data for better performance
              </p>
            </div>

            {incrementalMode === 'enabled' && strategy && (
              <StrategyConfiguration
                strategy={strategy}
                connector={connector}
                config={config}
                onChange={setConfig}
              />
            )}
          </div>
        )}

        {incrementalMode === 'enabled' && (
          <IncrementalSyncPreview
            connector={connector}
            strategy={strategy}
            config={config}
          />
        )}
      </CardContent>
    </Card>
  )
}
```

**2. Sync Performance Metrics Component**
```typescript
// app/components/sync/SyncPerformanceMetrics.tsx
export function SyncPerformanceMetrics({ scheduleId }: SyncPerformanceMetricsProps) {
  const { metrics, loading } = useSyncPerformanceMetrics(scheduleId)

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <MetricCard
          title="Incremental Sync Efficiency"
          value={metrics.efficiencyScore}
          unit="%"
          trend={metrics.efficiencyTrend}
          format="percentage"
        />
        <MetricCard
          title="Avg Sync Duration"
          value={metrics.avgDuration}
          unit="min"
          trend={metrics.durationTrend}
        />
        <MetricCard
          title="Records/Second"
          value={metrics.throughput}
          trend={metrics.throughputTrend}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Sync Comparison</CardTitle>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={metrics.comparisonData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="incrementalDuration"
                stroke="#10b981"
                name="Incremental (min)"
              />
              <Line
                type="monotone"
                dataKey="fullDuration"
                stroke="#ef4444"
                name="Full Sync (min)"
              />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      <WatermarkHistory scheduleId={scheduleId} />
    </div>
  )
}
```

### Architecture Compliance

1. **Strategy Pattern**: Implement different incremental sync strategies per connector type
2. **Service Layer**: Create new services following existing patterns in `app/services/`
3. **Database Patterns**: Extend existing sync tables with watermark tracking
4. **Background Jobs**: Use existing Bull queue system for incremental sync jobs
5. **API Versioning**: Use `/api/v2` prefix for new incremental sync endpoints

### File Structure Requirements

```
app/
├── controllers/
│   └── sync_schedules_controller.ts      # EXTENDED (Incremental endpoints)
├── models/
│   ├── sync_watermark.ts                 # NEW
│   ├── sync_performance_metrics.ts       # NEW
│   └── sync_schedules.ts                 # EXTENDED (Incremental config)
├── services/
│   ├── incremental_sync_service.ts       # NEW
│   ├── strategies/
│   │   ├── database_incremental_strategy.ts      # NEW
│   │   ├── postgresql_cdc_strategy.ts            # NEW
│   │   ├── timestamp_strategy.ts                  # NEW
│   │   ├── api_incremental_strategy.ts           # NEW
│   │   └── cloud_incremental_strategy.ts          # NEW
│   └── performance_monitoring_service.ts         # NEW
├── validators/
│   └── incremental_sync_validator.ts       # NEW
└── jobs/
    └── incremental_sync_job.ts            # NEW

database/migrations/
├── xxx_create_sync_watermarks_table.ts   # NEW
└── xxx_create_sync_performance_metrics_table.ts # NEW

frontend/
├── components/
│   └── sync/                              # EXTENDED
│       ├── IncrementalSyncConfig.tsx
│       ├── SyncPerformanceMetrics.tsx
│       ├── WatermarkHistory.tsx
│       ├── StrategyConfiguration.tsx
│       └── IncrementalSyncPreview.tsx
├── lib/
│   ├── api/
│   │   └── incremental_sync.ts            # NEW
│   └── hooks/
│       └── useIncrementalSync.ts           # NEW
└── stores/
    └── incremental_sync.store.ts           # NEW
```

### Library/Framework Requirements

#### Backend Dependencies
```json
{
  "pg-logical": "^1.0.0",           // PostgreSQL logical replication
  "debezium-connector-postgres": "^2.6.0", // CDC connector (optional)
  "crypto": "^1.0.1",               // Built-in Node.js crypto for checksums
  "fast-crc32c": "^2.0.0",          // Fast CRC32 for file comparison
  "@types/pg": "^8.10.0"            // PostgreSQL types
}
```

#### Frontend Dependencies (none new, using existing)
- Recharts for performance visualization
- React Hook Form for configuration forms
- TanStack Query for API calls

### Security Considerations

1. **Watermark Security**: Encrypt sensitive watermark data
2. **Change Isolation**: Ensure incremental sync respects tenant boundaries
3. **Audit Trail**: Log all change detection and processing
4. **Data Consistency**: Validate incremental sync results
5. **Rate Limiting**: Apply to change detection API calls

### Performance Requirements

1. **Efficiency Gains**: Incremental sync should be 80%+ faster than full sync
2. **Memory Usage**: Process changes in batches to avoid memory issues
3. **Scalability**: Support high-volume change streams
4. **Latency**: Minimize delay between change detection and processing
5. **Resource Usage**: Monitor and optimize CPU/IO for change processing

### Error Handling and Recovery

1. **Partial Failure Recovery**: Resume from last successful checkpoint
2. **Data Consistency**: Validate no data loss during incremental sync
3. **Fallback Mechanisms**: Automatic fallback to full sync when needed
4. **Reconciliation**: Periodic full sync to validate incremental sync accuracy
5. **Manual Override**: Allow administrators to force full resync

## Context Reference

### Epic Context
From Epic 1: Universal Data Connectors
- Story 1.5 builds on connector frameworks from Stories 1.1-1.3
- Enhances sync scheduling engine from Story 1.4 with incremental capabilities
- Critical for performance optimization and resource efficiency

### Previous Work
Stories 1.1-1.4 established:
- Database, API, and cloud storage connectors
- Sync scheduling engine with background jobs
- Watermark tracking concepts
- Performance monitoring foundation

### Project Context
- Current sync approach processes all data each time (full sync)
- Need efficient incremental sync for large datasets and frequent updates
- Must support various change detection strategies per connector type
- Maintain data consistency and integrity

## Git Intelligence

Based on recent commits, the system has:
- Comprehensive connector framework from Stories 1.1-1.3
- Sync scheduling engine from Story 1.4
- Background job processing with Bull queues
- Performance monitoring and audit logging
- Multi-tenant data isolation

## Latest Tech Information

### CDC Implementation (2025 Best Practices)
- PostgreSQL logical decoding with wal2json plugin
- Debezium for production-grade CDC deployments
- Efficient change event processing with memory optimization
- Schema change handling and backward compatibility

### Incremental Sync Patterns
- Cursor-based pagination for API incremental sync
- Timestamp and watermark tracking strategies
- Checksum comparison for file change detection
- ETag and header-based change detection for APIs

### Performance Optimization
- Batch processing of change events
- Memory-efficient stream processing
- Adaptive sync strategies based on data volume
- Comprehensive performance monitoring and alerting

### Data Consistency
- Change reconciliation mechanisms
- Periodic full sync validation
- Watermark integrity validation
- Audit trail for all change processing

## Dev Agent Record

### Agent Model Used
Claude-4 (anthropic-claude-4-20241101)

### Implementation Notes
This story implements comprehensive incremental sync capabilities that build on the connector and scheduling frameworks from previous stories, providing efficient change detection through multiple strategies (CDC, timestamps, cursors, checksums) with robust error handling and performance monitoring.

### Key Architectural Decisions
1. **Strategy Pattern**: Multiple incremental sync strategies per connector type
2. **Hybrid Approach**: CDC for databases, cursors/timestamps for APIs, checksums for files
3. **Watermark Management**: Flexible watermark storage with JSONB for connector-specific data
4. **Performance Monitoring**: Comprehensive metrics comparing incremental vs full sync efficiency
5. **Fallback Mechanisms**: Automatic fallback to full sync when incremental sync fails

### Security Implementation
- Tenant isolation for all incremental sync operations
- Encrypted watermark storage for sensitive sync positions
- Audit logging for all change detection and processing
- Data consistency validation and integrity checks
- Rate limiting and resource controls per tenant