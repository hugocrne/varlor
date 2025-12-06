# Story 1.4: Sync Scheduling Engine

Status: Ready for Development

## Story

As a business user,
I want to schedule automatic data syncs,
So that my datasets are always up-to-date without manual intervention.

## Acceptance Criteria

1. **Given** I have configured a connector (database, API, or cloud storage)
   **When** I navigate to the sync scheduling page
   **Then** I can create a new sync schedule for that connector
   **And** I can choose between realtime, hourly, daily, or weekly intervals
   **And** I can configure specific days and times for scheduled syncs

2. **Given** I am configuring a daily sync schedule
   **When** I set the schedule to run at 2:00 AM
   **Then** the sync automatically runs every day at 2:00 AM
   **And** I receive an email notification when the sync completes
   **And** the sync history shows each execution with success/failure status

3. **Given** I have selected hourly sync for an API connector
   **When** the sync runs
   **Then** the system respects API rate limits
   **And** processes new or modified data since the last sync
   **And** updates the dataset with the latest information

4. **Given** a sync fails due to connection issues
   **When** the failure occurs
   **Then** the system automatically retries with exponential backoff
   **And** I receive a failure notification with error details
   **And** the sync is marked as failed in the sync history
   **And** I can manually trigger a retry from the dashboard

5. **Given** I have multiple sync schedules configured
   **When** I view the sync dashboard
   **Then** I see all active schedules with their next run times
   **And** I can pause, resume, or delete individual schedules
   **And** I can view detailed execution history for each schedule
   **And** I can manually trigger syncs on demand

6. **Given** I want to receive notifications about sync operations
   **When** I configure notification settings
   **Then** I can choose email, in-app, or webhook notifications
   **And** I receive notifications for success, failure, or warnings
   **And** I can configure daily/weekly digest summaries of sync activity

## Tasks / Subtasks

- [ ] **Backend: Sync Scheduling Models** (AC: 1, 2, 5)
  - [ ] Create `sync_schedules` table with tenant isolation
  - [ ] Create `sync_executions` table for execution history
  - [ ] Create `sync_notifications` table for notification settings
  - [ ] Add proper indexes for query performance
  - [ ] Implement foreign key relationships with connectors

- [ ] **Backend: Sync Scheduler Service** (AC: 1, 2, 3, 4)
  - [ ] Create `SyncSchedulerService` using node-cron
  - [ ] Implement cron expression parsing and validation
  - [ ] Add support for realtime triggers via webhooks
  - [ ] Implement job priority and rate limiting
  - [ ] Add scheduler health monitoring and metrics

- [ ] **Backend: Sync Execution Engine** (AC: 2, 3, 4)
  - [ ] Create `SyncExecutionService` for running sync jobs
  - [ ] Integrate with existing connector services (Stories 1.1-1.3)
  - [ ] Implement incremental sync with watermark tracking
  - [ ] Add retry logic with exponential backoff
  - [ ] Create sync job classes for Bull queue processing

- [ ] **Backend: Notification System** (AC: 2, 4, 6)
  - [ ] Extend email service for sync notifications
  - [ ] Create webhook notification service
  - [ ] Implement in-app notification system
  - [ ] Add digest generation for daily/weekly summaries
  - [ ] Create notification preference management

- [ ] **Backend: API Endpoints** (AC: 1, 2, 5, 6)
  - [ ] Create `SyncSchedulesController` with endpoints:
    - GET `/api/v2/sync-schedules` - List schedules
    - POST `/api/v2/sync-schedules` - Create schedule
    - PUT `/api/v2/sync-schedules/:id` - Update schedule
    - DELETE `/api/v2/sync-schedules/:id` - Delete schedule
    - POST `/api/v2/sync-schedules/:id/trigger` - Manual trigger
    - GET `/api/v2/sync-schedules/:id/history` - Execution history
  - [ ] Implement request validation with VineJS

- [ ] **Backend: Monitoring and Logging** (AC: 2, 4, 5, 6)
  - [ ] Implement comprehensive sync execution logging
  - [ ] Add performance metrics collection
  - [ ] Create sync health check endpoints
  - [ ] Add audit logging for schedule modifications
  - [ ] Implement alerting for failed syncs

- [ ] **Frontend: Sync Management Components** (AC: 1, 2, 5, 6)
  - [ ] Create SyncScheduleModal component
  - [ ] Create ScheduleForm with cron expression builder
  - [ ] Create SyncDashboard component
  - [ ] Create ScheduleList component
  - [ ] Create ExecutionHistory component
  - [ ] Create NotificationSettings component

- [ ] **Frontend: State Management** (AC: 2, 3, 5, 6)
  - [ ] Create sync schedules store with Zustand
  - [ ] Implement real-time sync status updates
  - [ ] Add optimistic updates for schedule changes
  - [ ] Create sync history pagination
  - [ ] Add notification management state

- [ ] **Testing** (All ACs)
  - [ ] Unit tests for sync scheduler service
  - [ ] Integration tests for sync execution engine
  - [ ] Frontend component tests for schedule management
  - [ ] End-to-end test for complete sync workflow
  - [ ] Performance tests for concurrent sync execution

## Dev Notes

### Technical Requirements

#### Backend Implementation Details

**1. Database Schema Design**
```sql
-- Sync schedules configuration
CREATE TABLE sync_schedules (
  id SERIAL PRIMARY KEY,
  tenant_id VARCHAR(255) NOT NULL,
  connector_id INTEGER NOT NULL REFERENCES database_connections(id),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  schedule_type ENUM('realtime', 'hourly', 'daily', 'weekly', 'custom') NOT NULL,
  cron_expression VARCHAR(100), -- For custom schedules
  timezone VARCHAR(50) DEFAULT 'UTC',
  is_active BOOLEAN DEFAULT true,
  config JSONB, -- Additional schedule config
  notification_settings JSONB,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  INDEX idx_sync_schedules_tenant (tenant_id),
  INDEX idx_sync_schedules_connector (connector_id),
  INDEX idx_sync_schedules_active (is_active, next_run)
);

-- Sync execution history
CREATE TABLE sync_executions (
  id SERIAL PRIMARY KEY,
  schedule_id INTEGER NOT NULL REFERENCES sync_schedules(id),
  status ENUM('pending', 'running', 'completed', 'failed', 'cancelled') NOT NULL,
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  duration_ms INTEGER,
  records_processed INTEGER,
  records_created INTEGER,
  records_updated INTEGER,
  error_message TEXT,
  error_details JSONB,
  execution_metadata JSONB,
  created_at TIMESTAMP DEFAULT NOW(),
  INDEX idx_sync_executions_schedule (schedule_id),
  INDEX idx_sync_executions_status (status),
  INDEX idx_sync_executions_created (created_at)
);

-- Sync notifications
CREATE TABLE sync_notifications (
  id SERIAL PRIMARY KEY,
  schedule_id INTEGER NOT NULL REFERENCES sync_schedules(id),
  notification_type ENUM('email', 'webhook', 'in_app') NOT NULL,
  events JSONB NOT NULL, -- ['success', 'failure', 'warning']
  config JSONB NOT NULL, -- Email, webhook URL, etc.
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

**2. Sync Scheduler Service**
```typescript
// app/services/sync_scheduler_service.ts
import cron from 'node-cron'
import Bull from 'bull'
import SyncExecutionService from './sync_execution_service'

export default class SyncSchedulerService {
  private static instance: SyncSchedulerService
  private jobs: Map<number, cron.ScheduledTask> = new Map()
  private realtimeQueue: Bull.Queue
  private scheduledQueue: Bull.Queue

  constructor() {
    // Initialize Bull queues
    this.realtimeQueue = new Bull('realtime syncs', {
      redis: { port: 6379, host: 'localhost' }
    })

    this.scheduledQueue = new Bull('scheduled syncs', {
      redis: { port: 6379, host: 'localhost' }
    })

    this.setupQueueProcessors()
  }

  static getInstance(): SyncSchedulerService {
    if (!this.instance) {
      this.instance = new SyncSchedulerService()
    }
    return this.instance
  }

  async scheduleJob(schedule: SyncSchedule): Promise<void> {
    // Remove existing job if any
    if (this.jobs.has(schedule.id)) {
      this.jobs.get(schedule.id)?.stop()
      this.jobs.delete(schedule.id)
    }

    // Schedule based on type
    switch (schedule.scheduleType) {
      case 'realtime':
        await this.scheduleRealtimeJob(schedule)
        break
      case 'hourly':
        await this.scheduleCronJob(schedule, '0 * * * *') // Every hour
        break
      case 'daily':
        await this.scheduleCronJob(schedule, schedule.cronExpression || '0 2 * * *') // 2 AM daily
        break
      case 'weekly':
        await this.scheduleCronJob(schedule, schedule.cronExpression || '0 2 * * 0') // Sunday 2 AM
        break
      case 'custom':
        await this.scheduleCronJob(schedule, schedule.cronExpression!)
        break
    }
  }

  private async scheduleCronJob(schedule: SyncSchedule, cronExpression: string): Promise<void> {
    const task = cron.schedule(cronExpression, async () => {
      await this.enqueueSyncJob(schedule.id, 'scheduled')
    }, {
      scheduled: true,
      timezone: schedule.timezone || 'UTC'
    })

    this.jobs.set(schedule.id, task)
  }

  private async scheduleRealtimeJob(schedule: SyncSchedule): Promise<void> {
    // For realtime, we don't use cron but rather trigger via webhooks
    // The actual scheduling is handled by webhook endpoints
    console.log(`Realtime sync configured for schedule ${schedule.id}`)
  }

  async enqueueSyncJob(scheduleId: number, triggerType: 'scheduled' | 'manual' | 'realtime'): Promise<void> {
    const jobData = {
      scheduleId,
      triggerType,
      enqueuedAt: new Date()
    }

    const queue = triggerType === 'realtime' ? this.realtimeQueue : this.scheduledQueue

    await queue.add('sync-execution', jobData, {
      attempts: 3,
      backoff: {
        type: 'exponential',
        delay: 2000
      },
      removeOnComplete: 100,
      removeOnFail: 50
    })
  }

  private setupQueueProcessors(): void {
    this.scheduledQueue.process('sync-execution', 5, async (job) => {
      await SyncExecutionService.executeSync(job.data)
    })

    this.realtimeQueue.process('sync-execution', 10, async (job) => {
      await SyncExecutionService.executeSync(job.data)
    })
  }

  async pauseSchedule(scheduleId: number): Promise<void> {
    const task = this.jobs.get(scheduleId)
    if (task) {
      task.stop()
    }
  }

  async resumeSchedule(scheduleId: number): Promise<void> {
    const schedule = await SyncSchedule.find(scheduleId)
    if (schedule && schedule.isActive) {
      await this.scheduleJob(schedule)
    }
  }

  async removeSchedule(scheduleId: number): Promise<void> {
    const task = this.jobs.get(scheduleId)
    if (task) {
      task.stop()
      this.jobs.delete(scheduleId)
    }
  }
}
```

**3. Sync Execution Service**
```typescript
// app/services/sync_execution_service.ts
export default class SyncExecutionService {
  static async executeSync(jobData: SyncJobData): Promise<void> {
    const { scheduleId, triggerType } = jobData

    const schedule = await SyncSchedule.query()
      .where('id', scheduleId)
      .where('is_active', true)
      .preload('connector')
      .first()

    if (!schedule) {
      throw new Error('Schedule not found or inactive')
    }

    // Create execution record
    const execution = await SyncExecution.create({
      scheduleId: schedule.id,
      status: 'running',
      startedAt: new Date()
    })

    try {
      // Get the appropriate connector service
      const connectorService = this.getConnectorService(schedule.connector.type)

      // Perform the sync
      const result = await connectorService.syncData({
        connector: schedule.connector,
        config: schedule.config,
        watermark: await this.getLastWatermark(scheduleId),
        onProgress: (progress) => this.updateProgress(execution.id, progress)
      })

      // Update execution with success
      await execution.merge({
        status: 'completed',
        completedAt: new Date(),
        durationMs: Date.now() - execution.startedAt.getTime(),
        recordsProcessed: result.recordsProcessed,
        recordsCreated: result.recordsCreated,
        recordsUpdated: result.recordsUpdated,
        executionMetadata: result.metadata
      }).save()

      // Update watermark for next sync
      await this.updateWatermark(scheduleId, result.watermark)

      // Send notifications
      await this.sendNotifications(schedule, execution, 'success')

    } catch (error) {
      // Update execution with failure
      await execution.merge({
        status: 'failed',
        completedAt: new Date(),
        durationMs: Date.now() - execution.startedAt.getTime(),
        errorMessage: error.message,
        errorDetails: {
          stack: error.stack,
          name: error.name
        }
      }).save()

      // Send failure notifications
      await this.sendNotifications(schedule, execution, 'failure')

      throw error // Re-throw for Bull retry logic
    }
  }

  private static getConnectorService(connectorType: string) {
    switch (connectorType) {
      case 'database':
        return DatabaseConnectorService
      case 'api':
        return APIConnectorService
      case 'cloud':
        return CloudStorageService
      default:
        throw new Error(`Unsupported connector type: ${connectorType}`)
    }
  }

  private static async getLastWatermark(scheduleId: number): Promise<any> {
    const lastExecution = await SyncExecution.query()
      .where('schedule_id', scheduleId)
      .where('status', 'completed')
      .orderBy('created_at', 'desc')
      .first()

    return lastExecution?.executionMetadata?.watermark
  }

  private static async updateWatermark(scheduleId: number, watermark: any): Promise<void> {
    // Store watermark in Redis or database for next sync
    await Redis.setex(`sync:watermark:${scheduleId}`, 86400 * 30, JSON.stringify(watermark))
  }

  private static async sendNotifications(
    schedule: SyncSchedule,
    execution: SyncExecution,
    status: 'success' | 'failure'
  ): Promise<void> {
    const notifications = await SyncNotification.query()
      .where('schedule_id', schedule.id)
      .where('is_active', true)

    for (const notification of notifications) {
      if (notification.events.includes(status)) {
        switch (notification.notificationType) {
          case 'email':
            await this.sendEmailNotification(schedule, execution, notification.config, status)
            break
          case 'webhook':
            await this.sendWebhookNotification(schedule, execution, notification.config, status)
            break
          case 'in_app':
            await this.createInAppNotification(schedule, execution, status)
            break
        }
      }
    }
  }
}
```

**4. Cron Expression Builder**
```typescript
// app/services/cron_builder_service.ts
export default class CronBuilderService {
  static buildExpression(type: ScheduleType, config: ScheduleConfig): string {
    switch (type) {
      case 'hourly':
        return `${config.minute || 0} * * * *`

      case 'daily':
        return `${config.minute || 0} ${config.hour || 2} * * *`

      case 'weekly':
        return `${config.minute || 0} ${config.hour || 2} * * ${config.dayOfWeek || 0}`

      case 'monthly':
        return `${config.minute || 0} ${config.hour || 2} ${config.dayOfMonth || 1} * *`

      case 'custom':
        return this.buildCustomExpression(config)

      default:
        throw new Error(`Unsupported schedule type: ${type}`)
    }
  }

  static parseExpression(expression: string): ParsedCron {
    // Parse cron expression into human readable format
    const parts = expression.split(' ')

    return {
      minute: this.parseCronPart(parts[0], 0, 59),
      hour: this.parseCronPart(parts[1], 0, 23),
      dayOfMonth: this.parseCronPart(parts[2], 1, 31),
      month: this.parseCronPart(parts[3], 1, 12),
      dayOfWeek: this.parseCronPart(parts[4], 0, 6)
    }
  }

  static getNextRuns(expression: string, count: number = 5): Date[] {
    const parser = require('cron-parser')
    const interval = parser.parseExpression(expression)

    const runs: Date[] = []
    for (let i = 0; i < count; i++) {
      runs.push(interval.next().toDate())
    }

    return runs
  }

  static validateExpression(expression: string): boolean {
    try {
      const parser = require('cron-parser')
      parser.parseExpression(expression)
      return true
    } catch (error) {
      return false
    }
  }
}
```

#### Frontend Implementation Details

**1. Sync Schedule Form Component**
```typescript
// app/components/sync/SyncScheduleForm.tsx
export function SyncScheduleForm({ connector, onSave }: SyncScheduleFormProps) {
  const [scheduleType, setScheduleType] = useState<ScheduleType>('daily')
  const [cronExpression, setCronExpression] = useState('')
  const [config, setConfig] = useState<ScheduleConfig>({})
  const [notificationSettings, setNotificationSettings] = useState<NotificationSettings>({})

  const handleScheduleTypeChange = (type: ScheduleType) => {
    setScheduleType(type)
    const expression = CronBuilderService.buildExpression(type, config)
    setCronExpression(expression)
  }

  const handleConfigChange = (key: string, value: any) => {
    const newConfig = { ...config, [key]: value }
    setConfig(newConfig)

    if (scheduleType !== 'custom') {
      const expression = CronBuilderService.buildExpression(scheduleType, newConfig)
      setCronExpression(expression)
    }
  }

  return (
    <Form onSubmit={handleSubmit}>
      <FormField>
        <FormLabel>Sync Frequency</FormLabel>
        <Select value={scheduleType} onValueChange={handleScheduleTypeChange}>
          <Option value="realtime">Realtime</Option>
          <Option value="hourly">Hourly</Option>
          <Option value="daily">Daily</Option>
          <Option value="weekly">Weekly</Option>
          <Option value="custom">Custom</Option>
        </Select>
      </FormField>

      {scheduleType === 'daily' && (
        <FormField>
          <FormLabel>Time</FormLabel>
          <TimeInput
            value={config.time || '02:00'}
            onChange={(time) => handleConfigChange('time', time)}
          />
        </FormField>
      )}

      {scheduleType === 'weekly' && (
        <FormField>
          <FormLabel>Day of Week</FormLabel>
          <Select
            value={config.dayOfWeek || 0}
            onValueChange={(day) => handleConfigChange('dayOfWeek', day)}
          >
            <Option value={0}>Sunday</Option>
            <Option value={1}>Monday</Option>
            {/* ... other days */}
          </Select>
        </FormField>
      )}

      {scheduleType === 'custom' && (
        <FormField>
          <FormLabel>Cron Expression</FormLabel>
          <Input
            value={cronExpression}
            onChange={(e) => setCronExpression(e.target.value)}
            placeholder="0 2 * * *"
          />
          <CronPreview expression={cronExpression} />
        </FormField>
      )}

      <NotificationSettingsSection
        settings={notificationSettings}
        onChange={setNotificationSettings}
      />

      <Button type="submit">Create Schedule</Button>
    </Form>
  )
}
```

**2. Sync Dashboard Component**
```typescript
// app/components/sync/SyncDashboard.tsx
export function SyncDashboard() {
  const { schedules, loading, refresh } = useSyncSchedules()
  const [selectedSchedule, setSelectedSchedule] = useState<SyncSchedule | null>(null)

  const handleTriggerSync = async (scheduleId: number) => {
    try {
      await api.triggerSync(scheduleId)
      refresh()
    } catch (error) {
      // Handle error
    }
  }

  const handlePauseResume = async (scheduleId: number, isActive: boolean) => {
    try {
      if (isActive) {
        await api.pauseSchedule(scheduleId)
      } else {
        await api.resumeSchedule(scheduleId)
      }
      refresh()
    } catch (error) {
      // Handle error
    }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Sync Schedules</h1>
        <Button onClick={() => setSelectedSchedule(null)}>
          Create New Schedule
        </Button>
      </div>

      <div className="grid gap-4">
        {schedules.map((schedule) => (
          <ScheduleCard
            key={schedule.id}
            schedule={schedule}
            onTrigger={() => handleTriggerSync(schedule.id)}
            onPauseResume={() => handlePauseResume(schedule.id, schedule.isActive)}
            onViewHistory={() => viewHistory(schedule.id)}
          />
        ))}
      </div>

      {selectedSchedule !== null && (
        <SyncScheduleModal
          schedule={selectedSchedule}
          onClose={() => setSelectedSchedule(null)}
          onSave={refresh}
        />
      )}
    </div>
  )
}
```

### Architecture Compliance

1. **Service Layer Pattern**: Create new services following existing structure in `app/services/`
2. **Background Jobs**: Use existing Bull queue system for sync job processing
3. **Database Patterns**: Follow existing migration patterns for new tables
4. **API Versioning**: Use `/api/v2` prefix for new sync endpoints
5. **Multi-tenancy**: Ensure all sync data is tenant-scoped
6. **Notification Integration**: Build on existing email infrastructure

### File Structure Requirements

```
app/
├── controllers/
│   └── sync_schedules_controller.ts      # NEW
├── models/
│   ├── sync_schedule.ts                  # NEW
│   ├── sync_execution.ts                 # NEW
│   └── sync_notification.ts              # NEW
├── services/
│   ├── sync_scheduler_service.ts         # NEW
│   ├── sync_execution_service.ts         # NEW
│   ├── cron_builder_service.ts           # NEW
│   └── notification_service.ts           # EXTENDED
├── validators/
│   └── sync_schedules_validator.ts       # NEW
├── jobs/
│   ├── sync_execution_job.ts             # NEW
│   └── notification_job.ts               # NEW
└── start/
    └── routes.ts                         # EXTENDED (Sync routes)

database/migrations/
├── xxx_create_sync_schedules_table.ts    # NEW
├── xxx_create_sync_executions_table.ts   # NEW
└── xxx_create_sync_notifications_table.ts # NEW

frontend/
├── components/
│   └── sync/                             # NEW
│       ├── SyncDashboard.tsx
│       ├── SyncScheduleModal.tsx
│       ├── SyncScheduleForm.tsx
│       ├── ScheduleCard.tsx
│       ├── ExecutionHistory.tsx
│       ├── CronPreview.tsx
│       └── NotificationSettings.tsx
├── lib/
│   ├── api/
│   │   └── sync.ts                       # NEW
│   └── hooks/
│       └── useSyncSchedules.ts            # NEW
└── stores/
    └── sync.store.ts                     # NEW
```

### Library/Framework Requirements

#### Backend Dependencies
```json
{
  "node-cron": "^3.0.3",           // Cron job scheduling
  "bull": "^4.12.0",                // Job queue system
  "cron-parser": "^4.9.0",         // Cron expression parsing
  "@types/node-cron": "^3.0.8"     // TypeScript definitions
}
```

#### Frontend Dependencies (none new, using existing)
- React Hook Form for form validation
- TanStack Query for API calls
- Zustand for state management

### Security Considerations

1. **Schedule Isolation**: Ensure schedules can only access tenant's own connectors
2. **Rate Limiting**: Apply rate limiting to manual sync triggers
3. **Notification Security**: Validate webhook URLs and email addresses
4. **Audit Logging**: Log all schedule modifications and executions
5. **Resource Limits**: Implement limits on concurrent sync executions per tenant

### Performance Requirements

1. **Scalable Scheduling**: Support 1000+ concurrent schedules
2. **Efficient Queuing**: Use Bull queues with Redis for job distribution
3. **Watermark Tracking**: Efficient incremental sync using watermarks
4. **Batch Processing**: Process records in batches to avoid memory issues
5. **Monitoring**: Track sync performance and resource usage

### Monitoring and Observability

1. **Sync Metrics**: Track success rates, duration, and record counts
2. **Health Checks**: Monitor scheduler service health
3. **Error Tracking**: Comprehensive error logging and alerting
4. **Performance Monitoring**: Track queue depth and processing times
5. **Audit Logs**: Maintain detailed execution history

## Context Reference

### Epic Context
From Epic 1: Universal Data Connectors
- Story 1.4 adds automated sync capabilities to connectors from Stories 1.1-1.3
- Enables scheduled data updates without manual intervention
- Critical for maintaining up-to-date datasets in production environments

### Previous Work
Stories 1.1-1.3 established:
- Database, API, and cloud storage connectors
- Encryption service for credential storage
- Background job processing with Bull queues
- Multi-tenant connector isolation
- File processing and analysis pipeline

### Project Context
- Current system requires manual file uploads and data refresh
- Need automated sync capabilities for production use cases
- Must integrate with existing connector framework
- Maintain security and multi-tenant isolation

## Git Intelligence

Based on recent commits, the system has:
- Stable connector infrastructure from Stories 1.1-1.3
- Background job processing with Bull queues
- Existing email notification system
- Comprehensive audit logging framework
- Multi-tenant data isolation patterns

## Latest Tech Information

### Node.js Scheduling (2025 Best Practices)
- Use BullMQ (successor to Bull) for production job queues
- Implement proper error handling with exponential backoff
- Use dead letter queues for failed jobs
- Set up comprehensive monitoring and alerting

### Database-Driven Scheduling
- Store schedules in PostgreSQL with proper indexing
- Use cron expressions for flexible scheduling
- Track execution history for audit and monitoring
- Implement watermark tracking for incremental syncs

### Cron Expression Patterns
- Use cron-parser library for expression validation
- Provide human-readable preview of schedules
- Support timezone-aware scheduling
- Validate next run times before saving schedules

### Background Job Processing
- Separate queues for different sync types (realtime vs scheduled)
- Implement job priorities and rate limiting
- Use Redis clustering for high availability
- Track job metrics and performance

## Dev Agent Record

### Agent Model Used
Claude-4 (anthropic-claude-4-20241101)

### Implementation Notes
This story implements a comprehensive sync scheduling engine that builds on the connector framework from Stories 1.1-1.3, providing automated data synchronization with flexible scheduling options, robust error handling, and comprehensive notification system.

### Key Architectural Decisions
1. **Hybrid Scheduling**: Combine node-cron for time-based triggers with Bull queues for job processing
2. **Database-Driven**: Store schedules and execution history in PostgreSQL for audit and monitoring
3. **Incremental Sync**: Use watermark tracking to avoid processing unchanged data
4. **Multi-Channel Notifications**: Support email, webhook, and in-app notifications
5. **Realtime Support**: Handle both scheduled and webhook-triggered syncs

### Security Implementation
- Tenant isolation for all sync schedules and executions
- Rate limiting on manual sync triggers
- Audit logging for all schedule modifications
- Secure webhook validation and notification management
- Resource limits per tenant to prevent abuse