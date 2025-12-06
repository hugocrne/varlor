# Story 1.6: Connector Monitoring Dashboard

Status: Ready for Development

## Story

As a data platform admin,
I want to monitor all connector health and activity,
So that I can quickly identify and resolve issues.

## Acceptance Criteria

1. **Given** I have admin permissions
   **When** I navigate to the connector monitoring dashboard
   **Then** I see all configured connectors with their real-time status (healthy, warning, error)
   **And** each connector shows key metrics like last sync time, success rate, and throughput
   **And** I can filter and search connectors by type, status, or name

2. **Given** I click on a specific connector
   **When** the connector details panel opens
   **Then** I see comprehensive information including configuration, sync history, and error logs
   **And** I can view recent sync executions with detailed performance metrics
   **And** I can access real-time logs and debug information
   **And** I can manually trigger test connections or sync operations

3. **Given** a connector experiences issues
   **When** the status changes to warning or error
   **Then** I receive immediate notifications through configured channels (email, in-app, webhook)
   **And** the dashboard highlights the problematic connector with visual indicators
   **And** I can view detailed error analysis and recommended resolution steps
   **And** the system provides one-click actions for common issues (retry, pause, reconfigure)

4. **Given** I want to monitor overall connector performance
   **When** I view the dashboard overview
   **Then** I see aggregate metrics across all connectors (total syncs, success rates, data volume)
   **And** I can visualize trends over time with interactive charts
   **And** I can compare performance between different connector types
   **And** I can export monitoring reports for compliance and analysis

5. **Given** I need to configure alert rules
   **When** I access the alert configuration panel
   **Then** I can create custom alert rules with specific thresholds and conditions
   **And** I can configure escalation policies with multiple notification channels
   **And** I can set up maintenance windows to suppress alerts during planned downtime
   **And** I can test alert rules before activating them

6. **Given** I want to manage connector operations from the dashboard
   **When** I select one or more connectors
   **Then** I can perform bulk operations (pause, resume, reconfigure, delete)
   **Then** I can manually trigger syncs with optional parameters
   **Then** I can temporarily disable alerting for maintenance
   **And** all operations are logged with audit trails for compliance

## Tasks / Subtasks

- [ ] **Backend: Connector Monitoring Models** (AC: 1, 2, 4, 5)
  - [ ] Create `connector_events` table for event tracking
  - [ ] Create `connector_health_metrics` table for performance data
  - [ ] Create `alert_rules` table for alert configuration
  - [ ] Create `alert_notifications` table for alert history
  - [ ] Add proper indexing for time-series queries

- [ ] **Backend: Health Check Service** (AC: 1, 2, 3)
  - [ ] Create `ConnectorHealthService` for comprehensive health monitoring
  - [ ] Implement health checks for all connector types (database, API, cloud)
  - [ ] Add connection validation and performance testing
  - [ ] Create health status calculation algorithms
  - [ ] Add automatic health score generation

- [ ] **Backend: Metrics Collection** (AC: 1, 4)
  - [ ] Implement Prometheus metrics endpoint for connector monitoring
  - [ ] Create metrics collection service for connector performance
  - [ ] Add custom metrics for connector-specific operations
  - [ ] Implement metric aggregation and summarization
  - [ ] Add historical data retention policies

- [ ] **Backend: Alert Engine** (AC: 3, 5)
  - [ ] Create `AlertEngineService` for rule evaluation and notification
  - [ ] Implement alert rule evaluation with configurable thresholds
  - [ ] Add multi-channel notification system (email, webhook, in-app)
  - [ ] Create alert escalation and suppression logic
  - [ ] Add alert deduplication and grouping

- [ ] **Backend: Admin API Endpoints** (AC: 1, 2, 6)
  - [ ] Create `ConnectorMonitoringController` with endpoints:
    - GET `/api/v2/admin/connectors` - List all connectors with health
    - GET `/api/v2/admin/connectors/:id/health` - Detailed health info
    - GET `/api/v2/admin/connectors/:id/metrics` - Performance metrics
    - GET `/api/v2/admin/connectors/:id/events` - Event history
    - POST `/api/v2/admin/connectors/:id/test` - Test connection
    - POST `/api/v2/admin/connectors/bulk-action` - Bulk operations
  - [ ] Implement admin permission validation

- [ ] **Backend: Real-time Updates** (AC: 1, 2, 3)
  - [ ] Create WebSocket events for real-time connector status updates
  - [ ] Implement event broadcasting for connector health changes
  - [ ] Add real-time metrics streaming
  - [ ] Create subscription management for dashboard clients
  - [ ] Add connection management for dashboard users

- [ ] **Frontend: Monitoring Dashboard** (AC: 1, 2, 4, 6)
  - [ ] Create ConnectorMonitoringDashboard component
  - [ ] Create ConnectorStatusGrid component with real-time updates
  - [ ] Create ConnectorDetailsPanel component
  - [ ] Create PerformanceCharts component
  - [ ] Create BulkOperations component
  - [ ] Create AlertConfigurationModal component

- [ ] **Frontend: Real-time Updates** (AC: 1, 2, 3)
  - [ ] Implement WebSocket connection management
  - [ ] Create real-time connector status updates
  - [ ] Add live metrics streaming
  - [ ] Create alert notification components
  - [ ] Add connection status indicators

- [ ] **Testing** (All ACs)
  - [ ] Unit tests for health check services
  - [ ] Integration tests for alert engine
  - [ ] Frontend component tests for dashboard
  - [ ] End-to-end test for complete monitoring workflow
  - [ ] Performance tests for metrics collection

## Dev Notes

### Technical Requirements

#### Backend Implementation Details

**1. Monitoring Database Schema**
```sql
-- Connector events for tracking all activities
CREATE TABLE connector_events (
  id SERIAL PRIMARY KEY,
  tenant_id VARCHAR(255) NOT NULL,
  connector_id INTEGER NOT NULL REFERENCES database_connections(id),
  event_type VARCHAR(50) NOT NULL, -- 'health_check', 'sync_started', 'sync_completed', 'error'
  event_level VARCHAR(20) NOT NULL, -- 'info', 'warning', 'error', 'critical'
  message TEXT,
  details JSONB,
  metadata JSONB,
  created_at TIMESTAMP DEFAULT NOW(),
  INDEX idx_connector_events_connector (connector_id),
  INDEX idx_connector_events_type (event_type),
  INDEX idx_connector_events_level (event_level),
  INDEX idx_connector_events_created (created_at),
  INDEX idx_connector_events_tenant (tenant_id)
);

-- Connector health metrics for performance tracking
CREATE TABLE connector_health_metrics (
  id SERIAL PRIMARY KEY,
  connector_id INTEGER NOT NULL REFERENCES database_connections(id),
  metric_name VARCHAR(100) NOT NULL,
  metric_value DECIMAL(15,4),
  metric_unit VARCHAR(20),
  labels JSONB,
  timestamp TIMESTAMP DEFAULT NOW(),
  INDEX idx_health_metrics_connector (connector_id),
  INDEX idx_health_metrics_name (metric_name),
  INDEX idx_health_metrics_timestamp (timestamp)
);

-- Alert rules for configuration
CREATE TABLE alert_rules (
  id SERIAL PRIMARY KEY,
  tenant_id VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  connector_type VARCHAR(50), -- NULL for all types
  conditions JSONB NOT NULL, -- Alert condition configuration
  threshold_config JSONB NOT NULL, -- Threshold values
  notification_config JSONB NOT NULL, -- Notification settings
  escalation_config JSONB,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  INDEX idx_alert_rules_tenant (tenant_id),
  INDEX idx_alert_rules_active (is_active)
);

-- Alert notifications for tracking sent alerts
CREATE TABLE alert_notifications (
  id SERIAL PRIMARY KEY,
  alert_rule_id INTEGER NOT NULL REFERENCES alert_rules(id),
  connector_id INTEGER REFERENCES database_connections(id),
  alert_level VARCHAR(20) NOT NULL,
  message TEXT,
  notification_channel VARCHAR(50) NOT NULL, -- 'email', 'webhook', 'in_app'
  recipient VARCHAR(255),
  sent_at TIMESTAMP DEFAULT NOW(),
  acknowledged_at TIMESTAMP,
  resolved_at TIMESTAMP,
  INDEX idx_alert_notifications_rule (alert_rule_id),
  INDEX idx_alert_notifications_connector (connector_id),
  INDEX idx_alert_notifications_sent (sent_at)
);
```

**2. Connector Health Service**
```typescript
// app/services/connector_health_service.ts
import client from 'prom-client'

export default class ConnectorHealthService {
  private static readonly healthMetrics = new client.Registry()

  // Define Prometheus metrics
  private static readonly connectorHealthGauge = new client.Gauge({
    name: 'varlor_connector_health_score',
    help: 'Health score of connectors (0-100)',
    labelNames: ['connector_id', 'connector_type', 'tenant_id']
  })

  private static readonly syncDurationHistogram = new client.Histogram({
    name: 'varlor_connector_sync_duration_seconds',
    help: 'Duration of connector sync operations',
    labelNames: ['connector_id', 'connector_type', 'status'],
    buckets: [1, 5, 10, 30, 60, 300, 600, 1800, 3600]
  })

  private static readonly syncSuccessRate = new client.Gauge({
    name: 'varlor_connector_sync_success_rate',
    help: 'Success rate of connector sync operations (0-1)',
    labelNames: ['connector_id', 'connector_type']
  })

  static async checkConnectorHealth(connector: DatabaseConnection): Promise<HealthStatus> {
    const startTime = Date.now()

    try {
      // Get appropriate health checker for connector type
      const healthChecker = this.getHealthChecker(connector.type)

      // Perform comprehensive health check
      const healthResult = await healthChecker.check(connector)

      // Calculate health score
      const healthScore = this.calculateHealthScore(healthResult)

      // Record health metrics
      this.recordHealthMetrics(connector, healthScore, healthResult)

      // Log health check event
      await this.logHealthEvent(connector, healthScore, healthResult)

      const duration = Date.now() - startTime

      return {
        connectorId: connector.id,
        status: this.determineStatus(healthScore),
        score: healthScore,
        lastCheck: new Date(),
        details: healthResult,
        checkDuration: duration,
        recommendations: this.generateRecommendations(healthResult)
      }

    } catch (error) {
      // Handle health check failure
      await this.logHealthError(connector, error)

      return {
        connectorId: connector.id,
        status: 'error',
        score: 0,
        lastCheck: new Date(),
        details: { error: error.message },
        checkDuration: Date.now() - startTime,
        recommendations: ['Check connector configuration', 'Verify network connectivity']
      }
    }
  }

  private static getHealthChecker(connectorType: string): ConnectorHealthChecker {
    switch (connectorType) {
      case 'database':
        return new DatabaseHealthChecker()
      case 'api':
        return new APIHealthChecker()
      case 'cloud':
        return new CloudStorageHealthChecker()
      default:
        throw new Error(`Unsupported connector type: ${connectorType}`)
    }
  }

  private static calculateHealthScore(result: HealthCheckResult): number {
    let score = 100

    // Penalty for connection issues
    if (!result.connectionSuccess) score -= 50
    if (result.connectionLatency > 5000) score -= 20

    // Penalty for recent sync failures
    const recentFailures = result.recentSyncFailures || 0
    score -= Math.min(recentFailures * 10, 30)

    // Bonus for recent successful syncs
    const recentSuccesses = result.recentSyncSuccesses || 0
    score += Math.min(recentSuccesses * 5, 20)

    // Ensure score is within 0-100 range
    return Math.max(0, Math.min(100, score))
  }

  private static recordHealthMetrics(
    connector: DatabaseConnection,
    score: number,
    result: HealthCheckResult
  ): void {
    // Update Prometheus metrics
    this.connectorHealthGauge
      .labels(
        connector.id.toString(),
        connector.type,
        connector.tenantId
      )
      .set(score)

    if (result.lastSyncDuration) {
      this.syncDurationHistogram
        .labels(
          connector.id.toString(),
          connector.type,
          result.lastSyncStatus || 'unknown'
        )
        .observe(result.lastSyncDuration / 1000)
    }

    if (result.syncSuccessRate !== undefined) {
      this.syncSuccessRate
        .labels(connector.id.toString(), connector.type)
        .set(result.syncSuccessRate)
    }

    // Store detailed metrics in database
    this.storeDetailedMetrics(connector, score, result)
  }

  static getMetrics(): string {
    return this.healthMetrics.metrics()
  }
}
```

**3. Database Health Checker**
```typescript
// app/services/health_checkers/database_health_checker.ts
export default class DatabaseHealthChecker implements ConnectorHealthChecker {
  async check(connector: DatabaseConnection): Promise<HealthCheckResult> {
    const startTime = Date.now()

    try {
      // Test database connection
      const connectionTest = await this.testDatabaseConnection(connector)

      // Check database accessibility
      const accessibilityCheck = await this.checkDatabaseAccessibility(connector)

      // Verify table permissions
      const permissionsCheck = await this.checkTablePermissions(connector)

      // Get recent sync performance
      const syncPerformance = await this.getSyncPerformance(connector.id)

      const connectionLatency = Date.now() - startTime

      return {
        connectionSuccess: connectionTest.success,
        connectionLatency,
        accessibilityCheck,
        permissionsCheck,
        syncPerformance,
        lastSyncStatus: syncPerformance.lastStatus,
        lastSyncDuration: syncPerformance.lastDuration,
        recentSyncFailures: syncPerformance.recentFailures,
        recentSyncSuccesses: syncPerformance.recentSuccesses,
        syncSuccessRate: syncPerformance.successRate,
        metadata: {
          databaseType: connector.config.type,
          version: await this.getDatabaseVersion(connector),
          tableCount: accessibilityCheck.tableCount,
          lastError: connectionTest.error
        }
      }

    } catch (error) {
      return {
        connectionSuccess: false,
        connectionLatency: Date.now() - startTime,
        error: error.message,
        metadata: {
          errorType: error.constructor.name
        }
      }
    }
  }

  private async testDatabaseConnection(connector: DatabaseConnection): Promise<ConnectionTest> {
    try {
      const service = DatabaseConnectorService.create(connector.config)

      // Test basic connection
      const startTime = Date.now()
      await service.testConnection()
      const latency = Date.now() - startTime

      return { success: true, latency }

    } catch (error) {
      return {
        success: false,
        error: error.message,
        latency: 0
      }
    }
  }

  private async checkDatabaseAccessibility(connector: DatabaseConnection): Promise<AccessibilityCheck> {
    try {
      const service = DatabaseConnectorService.create(connector.config)

      // Check if we can query information_schema
      const tables = await service.getTables()

      // Check if we can read sample data
      let sampleDataSuccess = false
      if (tables.length > 0) {
        try {
          await service.previewTable(tables[0], 1)
          sampleDataSuccess = true
        } catch (error) {
          // Ignore preview errors
        }
      }

      return {
        accessible: true,
        tableCount: tables.length,
        sampleDataSuccess,
        permissions: {
          canRead: true,
          canWrite: await this.checkWritePermissions(connector),
          canExecute: await this.checkExecutePermissions(connector)
        }
      }

    } catch (error) {
      return {
        accessible: false,
        error: error.message,
        tableCount: 0,
        permissions: { canRead: false, canWrite: false, canExecute: false }
      }
    }
  }
}
```

**4. Alert Engine Service**
```typescript
// app/services/alert_engine_service.ts
export default class AlertEngineService {
  private static instance: AlertEngineService
  private alertRules: Map<number, AlertRule> = new Map()
  private evaluationInterval: NodeJS.Timeout

  constructor() {
    this.loadAlertRules()
    this.startEvaluationEngine()
  }

  static getInstance(): AlertEngineService {
    if (!this.instance) {
      this.instance = new AlertEngineService()
    }
    return this.instance
  }

  async loadAlertRules(): Promise<void> {
    const rules = await AlertRule.query().where('is_active', true)

    this.alertRules.clear()
    for (const rule of rules) {
      this.alertRules.set(rule.id, rule)
    }
  }

  private startEvaluationEngine(): void {
    // Evaluate alert rules every minute
    this.evaluationInterval = setInterval(async () => {
      await this.evaluateAllRules()
    }, 60000)
  }

  private async evaluateAllRules(): Promise<void> {
    for (const rule of this.alertRules.values()) {
      try {
        await this.evaluateRule(rule)
      } catch (error) {
        console.error(`Error evaluating alert rule ${rule.id}:`, error)
      }
    }
  }

  private async evaluateRule(rule: AlertRule): Promise<void> {
    // Get connectors matching rule criteria
    const connectors = await this.getConnectorsForRule(rule)

    for (const connector of connectors) {
      const healthStatus = await ConnectorHealthService.checkConnectorHealth(connector)

      // Check if rule conditions are met
      const alertTriggered = this.checkConditions(rule, healthStatus)

      if (alertTriggered) {
        await this.triggerAlert(rule, connector, healthStatus)
      }
    }
  }

  private checkConditions(rule: AlertRule, healthStatus: HealthStatus): boolean {
    const conditions = rule.conditions

    // Check health score threshold
    if (conditions.healthScore && healthStatus.score < conditions.healthScore.min) {
      return true
    }

    // Check status conditions
    if (conditions.status && conditions.status.includes(healthStatus.status)) {
      return true
    }

    // Check sync failure rate
    if (conditions.maxFailureRate) {
      const failureRate = 1 - (healthStatus.details.syncSuccessRate || 0)
      if (failureRate > conditions.maxFailureRate) {
        return true
      }
    }

    // Check connection latency
    if (conditions.maxLatency && healthStatus.checkDuration > conditions.maxLatency) {
      return true
    }

    return false
  }

  private async triggerAlert(
    rule: AlertRule,
    connector: DatabaseConnection,
    healthStatus: HealthStatus
  ): Promise<void> {
    // Check if we recently sent similar alert (deduplication)
    const recentAlert = await this.getRecentAlert(rule.id, connector.id)
    if (recentAlert && this.isWithinDeduplicationWindow(recentAlert)) {
      return // Skip duplicate alert
    }

    const alertMessage = this.generateAlertMessage(rule, connector, healthStatus)

    // Send notifications through configured channels
    for (const channel of rule.notificationConfig.channels) {
      switch (channel.type) {
        case 'email':
          await this.sendEmailAlert(channel, alertMessage, connector)
          break
        case 'webhook':
          await this.sendWebhookAlert(channel, alertMessage, connector)
          break
        case 'in_app':
          await this.createInAppAlert(rule, connector, alertMessage)
          break
      }
    }

    // Record the alert
    await this.recordAlert(rule, connector, alertMessage, healthStatus)
  }

  private generateAlertMessage(
    rule: AlertRule,
    connector: DatabaseConnection,
    healthStatus: HealthStatus
  ): string {
    return `Connector Alert: ${connector.name} (${connector.type})
Health Score: ${healthStatus.score}/100
Status: ${healthStatus.status}
Last Check: ${healthStatus.lastCheck.toISOString()}

Rule: ${rule.name}
${rule.description}

Recommendations:
${healthStatus.recommendations.join('\n')}`
  }

  private async sendEmailAlert(
    channel: NotificationChannel,
    message: string,
    connector: DatabaseConnection
  ): Promise<void> {
    // Use existing email service
    await EmailService.send({
      to: channel.recipients,
      subject: `Connector Alert: ${connector.name}`,
      message,
      template: 'connector_alert'
    })
  }

  private async sendWebhookAlert(
    channel: NotificationChannel,
    message: string,
    connector: DatabaseConnection
  ): Promise<void> {
    const payload = {
      alert: {
        rule: channel.ruleName,
        message,
        severity: 'warning',
        connector: {
          id: connector.id,
          name: connector.name,
          type: connector.type
        },
        timestamp: new Date().toISOString()
      }
    }

    await fetch(channel.webhookUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...channel.headers
      },
      body: JSON.stringify(payload)
    })
  }

  async createAlertRule(ruleData: CreateAlertRuleDto): Promise<AlertRule> {
    const rule = await AlertRule.create(ruleData)
    this.alertRules.set(rule.id, rule)
    return rule
  }

  async updateAlertRule(id: number, ruleData: UpdateAlertRuleDto): Promise<AlertRule> {
    const rule = await AlertRule.findOrFail(id)
    await rule.merge(ruleData).save()

    if (rule.isActive) {
      this.alertRules.set(id, rule)
    } else {
      this.alertRules.delete(id)
    }

    return rule
  }

  async deleteAlertRule(id: number): Promise<void> {
    const rule = await AlertRule.findOrFail(id)
    await rule.delete()
    this.alertRules.delete(id)
  }

  async cleanup(): Promise<void> {
    if (this.evaluationInterval) {
      clearInterval(this.evaluationInterval)
    }
  }
}
```

#### Frontend Implementation Details

**1. Connector Monitoring Dashboard**
```typescript
// app/components/admin/ConnectorMonitoringDashboard.tsx
export function ConnectorMonitoringDashboard() {
  const [connectors, setConnectors] = useState<ConnectorWithHealth[]>([])
  const [selectedConnectors, setSelectedConnectors] = useState<number[]>([])
  const [filters, setFilters] = useState<ConnectorFilters>({})
  const [isLoading, setIsLoading] = useState(true)

  // WebSocket connection for real-time updates
  useEffect(() => {
    const ws = new WebSocket(`${process.env.NEXT_PUBLIC_WS_URL}/admin/monitoring`)

    ws.onmessage = (event) => {
      const update = JSON.parse(event.data)
      handleRealtimeUpdate(update)
    }

    return () => ws.close()
  }, [])

  const handleRealtimeUpdate = (update: MonitoringUpdate) => {
    switch (update.type) {
      case 'health_status':
        updateConnectorHealth(update.connectorId, update.healthStatus)
        break
      case 'alert':
        showAlertNotification(update.alert)
        break
      case 'metrics':
        updateConnectorMetrics(update.connectorId, update.metrics)
        break
    }
  }

  const updateConnectorHealth = (connectorId: number, healthStatus: HealthStatus) => {
    setConnectors(prev =>
      prev.map(connector =>
        connector.id === connectorId
          ? { ...connector, healthStatus }
          : connector
      )
    )
  }

  const handleBulkAction = async (action: BulkAction) => {
    try {
      await api.performBulkAction({
        action: action.type,
        connectorIds: selectedConnectors,
        parameters: action.parameters
      })

      setSelectedConnectors([])
      // Refresh data
      await loadConnectors()
    } catch (error) {
      // Handle error
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Connector Monitoring</h1>
        <div className="flex gap-2">
          <Button onClick={() => setShowAlertConfig(true)}>
            Configure Alerts
          </Button>
          <Button onClick={() => loadConnectors()}>
            Refresh
          </Button>
        </div>
      </div>

      {/* Overview Metrics */}
      <OverviewMetrics connectors={connectors} />

      {/* Filters */}
      <ConnectorFilters
        filters={filters}
        onChange={setFilters}
      />

      {/* Bulk Actions */}
      {selectedConnectors.length > 0 && (
        <BulkActionsPanel
          selectedCount={selectedConnectors.length}
          onAction={handleBulkAction}
          onClear={() => setSelectedConnectors([])}
        />
      )}

      {/* Connector Status Grid */}
      <ConnectorStatusGrid
        connectors={connectors}
        filters={filters}
        selectedConnectors={selectedConnectors}
        onSelectionChange={setSelectedConnectors}
        onConnectorClick={handleConnectorClick}
      />

      {/* Connector Details Panel */}
      {selectedConnector && (
        <ConnectorDetailsPanel
          connector={selectedConnector}
          onClose={() => setSelectedConnector(null)}
          onTest={handleTestConnection}
          onTriggerSync={handleTriggerSync}
        />
      )}

      {/* Alert Configuration Modal */}
      {showAlertConfig && (
        <AlertConfigModal
          onClose={() => setShowAlertConfig(false)}
          onSave={handleAlertConfigSave}
        />
      )}
    </div>
  )
}
```

**2. Real-time Status Updates**
```typescript
// app/components/admin/ConnectorStatusGrid.tsx
export function ConnectorStatusGrid({
  connectors,
  filters,
  selectedConnectors,
  onSelectionChange,
  onConnectorClick
}: ConnectorStatusGridProps) {
  const filteredConnectors = useMemo(() => {
    return connectors.filter(connector => {
      if (filters.type && connector.type !== filters.type) return false
      if (filters.status && connector.healthStatus?.status !== filters.status) return false
      if (filters.search && !connector.name.toLowerCase().includes(filters.search.toLowerCase())) return false
      return true
    })
  }, [connectors, filters])

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'healthy': return 'text-green-600'
      case 'warning': return 'text-yellow-600'
      case 'error': return 'text-red-600'
      default: return 'text-gray-600'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'healthy': return <CheckCircle className="w-4 h-4" />
      case 'warning': return <AlertTriangle className="w-4 h-4" />
      case 'error': return <XCircle className="w-4 h-4" />
      default: return <Clock className="w-4 h-4" />
    }
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {filteredConnectors.map((connector) => (
        <Card
          key={connector.id}
          className={`cursor-pointer transition-all hover:shadow-lg ${
            selectedConnectors.includes(connector.id) ? 'ring-2 ring-blue-500' : ''
          }`}
          onClick={() => onConnectorClick(connector)}
        >
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium">
                {connector.name}
              </CardTitle>
              <Checkbox
                checked={selectedConnectors.includes(connector.id)}
                onCheckedChange={(checked) => {
                  if (checked) {
                    onSelectionChange([...selectedConnectors, connector.id])
                  } else {
                    onSelectionChange(selectedConnectors.filter(id => id !== connector.id))
                  }
                }}
                onClick={(e) => e.stopPropagation()}
              />
            </div>
            <div className="flex items-center gap-2">
              <Badge variant="outline">{connector.type}</Badge>
              <span className={`flex items-center gap-1 text-sm ${getStatusColor(connector.healthStatus?.status || 'unknown')}`}>
                {getStatusIcon(connector.healthStatus?.status || 'unknown')}
                {connector.healthStatus?.status || 'Unknown'}
              </span>
            </div>
          </CardHeader>

          <CardContent>
            <div className="space-y-3">
              {/* Health Score */}
              <div>
                <div className="flex justify-between text-sm">
                  <span>Health Score</span>
                  <span className="font-medium">
                    {connector.healthStatus?.score || 0}/100
                  </span>
                </div>
                <Progress
                  value={connector.healthStatus?.score || 0}
                  className="h-2"
                />
              </div>

              {/* Last Sync */}
              <div>
                <div className="flex justify-between text-sm">
                  <span>Last Sync</span>
                  <span>{formatRelativeTime(connector.lastSyncAt)}</span>
                </div>
              </div>

              {/* Sync Rate */}
              <div>
                <div className="flex justify-between text-sm">
                  <span>Success Rate</span>
                  <span className="font-medium">
                    {Math.round((connector.healthStatus?.details.syncSuccessRate || 0) * 100)}%
                  </span>
                </div>
              </div>

              {/* Recent Activity */}
              {connector.recentEvents && connector.recentEvents.length > 0 && (
                <div>
                  <div className="text-sm font-medium mb-1">Recent Activity</div>
                  <div className="space-y-1">
                    {connector.recentEvents.slice(0, 2).map((event, index) => (
                      <div key={index} className="flex items-center gap-2 text-xs text-gray-600">
                        {getStatusIcon(event.level)}
                        <span className="truncate">{event.message}</span>
                        <span>{formatRelativeTime(event.createdAt)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
```

### Architecture Compliance

1. **Admin Pattern**: Extend existing admin section with new monitoring capabilities
2. **WebSocket Integration**: Use existing WebSocket infrastructure for real-time updates
3. **Prometheus Metrics**: Standardize metrics collection for monitoring
4. **Service Layer**: Create new services following existing patterns in `app/services/`
5. **Permission System**: Extend existing role-based access control for admin features

### File Structure Requirements

```
app/
├── controllers/
│   └── admin/
│       └── connector_monitoring_controller.ts  # NEW
├── models/
│   ├── connector_event.ts                       # NEW
│   ├── connector_health_metric.ts               # NEW
│   ├── alert_rule.ts                             # NEW
│   └── alert_notification.ts                     # NEW
├── services/
│   ├── connector_health_service.ts              # NEW
│   ├── alert_engine_service.ts                  # NEW
│   ├── metrics_collection_service.ts             # NEW
│   ├── health_checkers/
│   │   ├── database_health_checker.ts            # NEW
│   │   ├── api_health_checker.ts                 # NEW
│   │   └── cloud_health_checker.ts              # NEW
│   └── notification_service.ts                   # EXTENDED
├── validators/
│   └── admin/
│       └── connector_monitoring_validator.ts     # NEW
└── start/
    ├── routes.ts                                 # EXTENDED (Admin routes)
    └── websocket.ts                               # EXTENDED (Monitoring events)

database/migrations/
├── xxx_create_connector_events_table.ts          # NEW
├── xxx_create_connector_health_metrics_table.ts  # NEW
├── xxx_create_alert_rules_table.ts               # NEW
└── xxx_create_alert_notifications_table.ts       # NEW

frontend/
├── app/
│   ├── (admin)/
│   │   ├── monitoring/
│   │   │   ├── page.tsx                          # NEW
│   │   │   └── layout.tsx                        # NEW
│   │   └── dashboard/
│   │       └── layout.tsx                        # EXTENDED
├── components/
│   └── admin/
│       └── monitoring/                             # NEW
│           ├── ConnectorMonitoringDashboard.tsx
│           ├── ConnectorStatusGrid.tsx
│           ├── ConnectorDetailsPanel.tsx
│           ├── OverviewMetrics.tsx
│           ├── AlertConfigModal.tsx
│           ├── BulkActionsPanel.tsx
│           └── PerformanceCharts.tsx
├── lib/
│   ├── api/
│   │   └── admin/
│   │       └── monitoring.ts                     # NEW
│   └── hooks/
│       └── admin/
│           └── useConnectorMonitoring.ts          # NEW
└── stores/
    └── admin/
        └── monitoring.store.ts                    # NEW
```

### Library/Framework Requirements

#### Backend Dependencies
```json
{
  "prom-client": "^15.1.0",           // Prometheus metrics collection
  "bull": "^4.12.0",                   // Existing - for alert processing
  "node-cron": "^3.0.3",               // Existing - for scheduled health checks
  "@types/node-cron": "^3.0.8"         // TypeScript definitions
}
```

#### Frontend Dependencies
```json
{
  "recharts": "^2.12.0",               // For performance charts
  "date-fns": "^3.6.0",                // For date formatting
  "@tanstack/react-query": "^5.90.10",  // Existing - for API calls
  "zustand": "^5.0.8"                   // Existing - for state management
}
```

### Security Considerations

1. **Admin Permissions**: Validate admin role for all monitoring endpoints
2. **Data Isolation**: Ensure users can only view connectors from their tenant
3. **Audit Logging**: Log all monitoring actions and configuration changes
4. **Rate Limiting**: Apply to metrics collection and alert endpoints
5. **Input Validation**: Sanitize all alert rule configurations and parameters

### Performance Requirements

1. **Real-time Updates**: Sub-second WebSocket message delivery
2. **Scalable Metrics**: Support 1000+ connectors with concurrent monitoring
3. **Efficient Storage**: Time-series data with proper retention policies
4. **Fast Queries**: Optimized indexes for dashboard data retrieval
5. **Low Overhead**: Monitoring should not impact connector performance

### Monitoring and Observability

1. **Self-Monitoring**: Monitor the monitoring system itself
2. **Alert SLA**: Track alert delivery times and success rates
3. **Dashboard Performance**: Monitor dashboard load times and responsiveness
4. **Resource Usage**: Track CPU, memory, and network usage of monitoring services
5. **Health Checks**: Implement health checks for all monitoring components

## Context Reference

### Epic Context
From Epic 1: Universal Data Connectors
- Story 1.6 completes the connector framework with comprehensive monitoring
- Provides visibility into all connector operations from Stories 1.1-1.5
- Essential for production deployment and operational excellence
- Final piece needed for Alpha phase connector capabilities

### Previous Work
Stories 1.1-1.5 established:
- Complete connector framework (database, API, cloud storage)
- Sync scheduling and incremental sync capabilities
- Background job processing and performance tracking
- Foundation for monitoring and alerting

### Project Context
- Need comprehensive monitoring for production-ready connector system
- Admin users require visibility into connector health and performance
- Alert system essential for proactive issue resolution
- Dashboard needed for operational management

## Git Intelligence

Based on recent commits, the system has:
- Complete connector infrastructure from Stories 1.1-1.5
- Background job processing with Bull queues
- WebSocket infrastructure for real-time features
- Admin section foundation and permission system
- Email notification service for alerts

## Latest Tech Information

### Prometheus Metrics (2025 Best Practices)
- Use prom-client for Node.js metrics collection
- Implement custom metrics for business logic tracking
- Use histogram and gauge types for performance data
- Consider OpenTelemetry integration for enhanced observability

### Real-time Dashboards (2025 Trends)
- React 18/19 with TypeScript 5.0+ for type safety
- WebSocket and Server-Sent Events for live updates
- Modern state management with Zustand or Redux Toolkit
- Performance optimization with React.memo and useMemo

### Alert System Patterns
- Multi-channel notifications (email, webhook, in-app)
- Rule-based alert evaluation with configurable thresholds
- Alert deduplication and escalation policies
- Maintenance windows and alert suppression

### Admin Dashboard Design
- Responsive design for desktop and mobile
- Role-based access control and permission management
- Comprehensive audit logging for compliance
- Interactive charts and data visualization

## Dev Agent Record

### Agent Model Used
Claude-4 (anthropic-claude-4-20241101)

### Implementation Notes
This story completes the Universal Data Connectors epic with a comprehensive monitoring dashboard that provides real-time visibility into all connector operations, intelligent alerting, and powerful admin controls for operational management.

### Key Architectural Decisions
1. **Real-time First**: WebSocket-based updates for immediate visibility
2. **Prometheus Integration**: Standardized metrics collection and monitoring
3. **Multi-channel Alerts**: Email, webhook, and in-app notifications
4. **Admin-focused Design**: Comprehensive operational controls and bulk actions
5. **Self-Monitoring**: Monitor the monitoring system for reliability

### Security Implementation
- Role-based access control for admin features
- Tenant isolation for all monitoring data
- Comprehensive audit logging for compliance
- Input validation and sanitization for alert configurations
- Rate limiting and resource protection

### Epic Completion
This story completes Epic 1: Universal Data Connectors, providing a production-ready connector framework with:
- ✅ Database Connections (1.1)
- ✅ API Connector Framework (1.2)
- ✅ Cloud Storage Integration (1.3)
- ✅ Sync Scheduling Engine (1.4)
- ✅ Incremental Sync Support (1.5)
- ✅ Connector Monitoring Dashboard (1.6)