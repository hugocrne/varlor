# Story 1.3: Cloud Storage Integration

Status: Ready for Development

## Story

As a data engineer,
I want to connect to cloud storage providers,
So that I can process files directly from S3, GCS, or Azure Blob.

## Acceptance Criteria

1. **Given** I am on the dataset creation page
   **When** I click "Connect Cloud Storage"
   **Then** I see a modal with cloud provider options (AWS S3, Google Cloud Storage, Azure Blob Storage)

2. **Given** I selected AWS S3 as the provider
   **When** I configure access credentials
   **Then** I can choose between access key/secret, IAM role, or temporary credentials
   **And** the system validates credentials and lists available buckets
   **And** I can navigate through bucket folders and select files

3. **Given** I selected Google Cloud Storage
   **When** I configure service account authentication
   **Then** I can upload a service account JSON key or use workload identity
   **And** the system validates permissions and lists available buckets
   **And** I can browse bucket contents and select specific folders or files

4. **Given** I selected Azure Blob Storage
   **When** I configure Azure AD authentication
   **Then** I can use connection string or Azure AD credentials
   **And** the system validates access and lists containers
   **And** I can select specific containers and folders

5. **Given** I have selected files from cloud storage
   **When** I click "Process Files"
   **Then** the system streams files directly from cloud storage
   **And** processes them using the existing file upload pipeline
   **And** supports large files (>500MB) with streaming processing
   **And** shows real-time progress for file download and processing

6. **Given** I have configured a cloud storage connector
   **When** I view my connections
   **Then** I see all cloud storage connections with their status and last sync
   **And** I can test connection, refresh credentials, or delete connectors
   **And** all credentials are stored encrypted with tenant-specific keys

## Tasks / Subtasks

- [ ] **Backend: Cloud Storage Services** (AC: 1, 2, 3, 4, 5, 6)
  - [ ] Extend `FileStorageService` to support multiple cloud providers
  - [ ] Create `S3StorageService` using AWS SDK v3 with streaming support
  - [ ] Create `GCSStorageService` using Google Cloud Storage SDK
  - [ ] Create `AzureBlobStorageService` using Azure SDK with AD authentication
  - [ ] Implement credential validation for each provider
  - [ ] Add support for IAM roles and temporary credentials

- [ ] **Backend: Cloud Storage Models** (AC: 2, 3, 4, 6)
  - [ ] Extend `database_connections` table for cloud storage connectors
  - [ ] Create credential encryption for cloud-specific fields
  - [ ] Add provider-specific configuration fields
  - [ ] Implement tenant isolation for cloud connectors

- [ ] **Backend: API Endpoints** (AC: 1, 2, 3, 4, 5, 6)
  - [ ] Extend `ConnectorsController` with cloud storage endpoints:
    - POST `/api/v2/connectors/test-cloud` - Test cloud storage connection
    - GET `/api/v2/connectors/:provider/buckets` - List buckets/containers
    - GET `/api/v2/connectors/:provider/browse` - Browse cloud storage
    - POST `/api/v2/connectors/:provider/process` - Process cloud files
  - [ ] Implement request validation with VineJS

- [ ] **Backend: File Processing Integration** (AC: 5)
  - [ ] Create streaming file processor for large cloud files
  - [ ] Integrate with existing dataset parser service
  - [ ] Add parallel download support for multiple files
  - [ ] Implement bandwidth management and throttling
  - [ ] Add progress tracking for file operations

- [ ] **Backend: Security and Authentication** (AC: 2, 3, 4, 6)
  - [ ] Extend EncryptionService for cloud credential storage
  - [ ] Implement AWS IAM role authentication
  - [ ] Add Google Cloud service account authentication
  - [ ] Implement Azure AD authentication for Blob Storage
  - [ ] Add temporary credential support and rotation

- [ ] **Frontend: Cloud Storage Components** (AC: 1, 2, 3, 4, 5, 6)
  - [ ] Create CloudStorageModal component
  - [ ] Create ProviderSelector for S3/GCS/Azure
  - [ ] Create CloudCredentialForm with provider-specific fields
  - [ ] Create CloudBrowser component for file selection
  - [ ] Create CloudConnectionTestButton
  - [ ] Create FileProcessingProgress component

- [ ] **Frontend: State Management** (AC: 2, 3, 4, 5, 6)
  - [ ] Extend connectors store with cloud storage state
  - [ ] Implement real-time file processing progress
  - [ ] Add cloud file preview functionality
  - [ ] Create credential validation feedback

- [ ] **Testing** (All ACs)
  - [ ] Unit tests for cloud storage services
  - [ ] Integration tests with mock cloud providers
  - [ ] Frontend component tests for authentication flows
  - [ ] End-to-end test for complete cloud storage workflow
  - [ ] Performance tests for large file streaming

## Dev Notes

### Technical Requirements

#### Backend Implementation Details

**1. Cloud Storage Service Pattern**
```typescript
// app/services/cloud_storage_service.ts
export default class CloudStorageService {
  static create(provider: 's3' | 'gcs' | 'azure'): ICloudStorage {
    switch (provider) {
      case 's3': return new S3StorageService()
      case 'gcs': return new GCSStorageService()
      case 'azure': return new AzureBlobStorageService()
      default: throw new Error('Unsupported provider')
    }
  }
}

interface ICloudStorage {
  validateCredentials(config: CloudCredentials): Promise<boolean>
  listBuckets(config: CloudCredentials): Promise<CloudBucket[]>
  browseFiles(config: CloudCredentials, path: string): Promise<CloudFile[]>
  streamFile(config: CloudCredentials, path: string): Readable
  getFileMetadata(config: CloudCredentials, path: string): Promise<FileMetadata>
}
```

**2. AWS S3 Service with SDK v3**
```typescript
// app/services/s3_storage_service.ts
import { S3Client, ListBucketsCommand, GetObjectCommand } from '@aws-sdk/client-s3'
import { Upload } from '@aws-sdk/lib-storage'

export default class S3StorageService implements ICloudStorage {
  private client: S3Client

  constructor() {
    this.client = new S3Client({
      region: process.env.AWS_REGION || 'us-east-1'
    })
  }

  async validateCredentials(config: S3Credentials): Promise<boolean> {
    try {
      const command = new ListBucketsCommand({})
      await this.client.send(command)
      return true
    } catch (error) {
      return false
    }
  }

  async streamFile(config: S3Credentials, bucket: string, key: string): Promise<Readable> {
    const command = new GetObjectCommand({
      Bucket: bucket,
      Key: key
    })

    const response = await this.client.send(command)
    return response.Body as Readable
  }

  async uploadFile(config: S3Credentials, bucket: string, key: string, stream: Readable): Promise<void> {
    const upload = new Upload({
      client: this.client,
      params: {
        Bucket: bucket,
        Key: key,
        Body: stream
      },
      partSize: 1024 * 1024 * 5, // 5MB chunks
      queueSize: 4
    })

    await upload.done()
  }
}
```

**3. Google Cloud Storage Service**
```typescript
// app/services/gcs_storage_service.ts
import { Storage } from '@google-cloud/storage'

export default class GCSStorageService implements ICloudStorage {
  private storage: Storage

  constructor(config: GCSCredentials) {
    this.storage = new Storage({
      projectId: config.projectId,
      keyFilename: config.keyFile ? config.keyFile : undefined,
      credentials: config.serviceAccount
    })
  }

  async validateCredentials(): Promise<boolean> {
    try {
      const [buckets] = await this.storage.getBuckets()
      return true
    } catch (error) {
      return false
    }
  }

  async streamFile(bucket: string, filename: string): Promise<Readable> {
    const file = this.storage.bucket(bucket).file(filename)
    return file.createReadStream()
  }

  async browseFiles(bucket: string, prefix: string = ''): Promise<CloudFile[]> {
    const [files] = await this.storage.bucket(bucket).getFiles({
      prefix,
      autoPaginate: false
    })

    return files.map(file => ({
      name: file.name,
      size: file.metadata.size,
      modified: file.metadata.updated,
      type: file.metadata.contentType
    }))
  }
}
```

**4. Azure Blob Storage Service**
```typescript
// app/services/azure_blob_storage_service.ts
import { BlobServiceClient } from '@azure/storage-blob'
import { DefaultAzureCredential } from '@azure/identity'

export default class AzureBlobStorageService implements ICloudStorage {
  private client: BlobServiceClient

  constructor(config: AzureCredentials) {
    if (config.connectionString) {
      this.client = BlobServiceClient.fromConnectionString(config.connectionString)
    } else if (config.useAzureAD) {
      const credential = new DefaultAzureCredential()
      this.client = new BlobServiceClient(
        `https://${config.accountName}.blob.core.windows.net`,
        credential
      )
    } else {
      throw new Error('Invalid Azure configuration')
    }
  }

  async validateCredentials(): Promise<boolean> {
    try {
      const iterator = this.client.listContainers()
      await iterator.next()
      return true
    } catch (error) {
      return false
    }
  }

  async streamFile(container: string, blob: string): Promise<Readable> {
    const blockBlobClient = this.client.getContainerClient(container).getBlockBlobClient(blob)
    const downloadResponse = await blockBlobClient.download()
    return downloadResponse.readableStreamBody as Readable
  }
}
```

**5. Database Model Extension**
```typescript
// app/models/database_connection.ts (extended)
export default class DatabaseConnection extends BaseModel {
  // ... existing fields

  @column() // New for cloud storage
  public connectorType: 'database' | 'api' | 'storage' | 'cloud'

  @column() // Cloud-specific fields
  public cloudProvider: 's3' | 'gcs' | 'azure'

  @column() // S3 configuration
  public s3Config?: {
    accessKeyId?: string
    secretAccessKey?: string
    region?: string
    roleArn?: string
    externalId?: string
  }

  @column() // GCS configuration
  public gcsConfig?: {
    projectId?: string
    keyFile?: string // Encrypted file content
    serviceAccount?: object // Encrypted credentials
  }

  @column() // Azure configuration
  public azureConfig?: {
    connectionString?: string
    accountName?: string
    tenantId?: string
    clientId?: string
    useAzureAD?: boolean
  }

  @column() // Cache settings
  public cacheSettings?: {
    listCacheTTL: number // seconds
    metadataCacheTTL: number
  }
}
```

**6. Streaming File Processor**
```typescript
// app/services/streaming_file_processor.ts
export default class StreamingFileProcessor {
  async processCloudFile(
    connector: CloudConnector,
    filePath: string,
    datasetId: number
  ): Promise<void> {
    const storageService = CloudStorageService.create(connector.provider)
    const stream = await storageService.streamFile(connector.config, filePath)

    // Create processing job
    const job = await this.createProcessingJob(datasetId, filePath)

    try {
      // Stream process the file
      await this.processStream(stream, datasetId, (progress) => {
        this.updateJobProgress(job.id, progress)
      })

      // Mark job as completed
      await this.completeJob(job.id)
    } catch (error) {
      await this.failJob(job.id, error)
      throw error
    }
  }

  private async processStream(
    stream: Readable,
    datasetId: number,
    onProgress: (progress: number) => void
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      let bytesProcessed = 0
      const totalSize = stream.readableLength || 0

      stream
        .on('data', (chunk) => {
          bytesProcessed += chunk.length
          onProgress(totalSize > 0 ? bytesProcessed / totalSize : 0)
        })
        .on('end', () => resolve())
        .on('error', (error) => reject(error))
    })
  }
}
```

#### Frontend Implementation Details

**1. Cloud Storage Modal Component**
```typescript
// app/components/cloud/CloudStorageModal.tsx
export function CloudStorageModal() {
  const [selectedProvider, setSelectedProvider] = useState<'s3' | 'gcs' | 'azure' | null>(null)
  const [credentials, setCredentials] = useState<CloudCredentials>({})
  const [isConnected, setIsConnected] = useState(false)

  const handleConnect = async () => {
    try {
      await api.testCloudConnection(selectedProvider, credentials)
      setIsConnected(true)
    } catch (error) {
      // Handle error
    }
  }

  return (
    <Modal>
      {!selectedProvider ? (
        <ProviderSelector onSelect={setSelectedProvider} />
      ) : (
        <CloudCredentialForm
          provider={selectedProvider}
          credentials={credentials}
          onChange={setCredentials}
          onConnect={handleConnect}
        />
      )}
    </Modal>
  )
}
```

**2. Cloud Browser Component**
```typescript
// app/components/cloud/CloudBrowser.tsx
export function CloudBrowser({ connector, onFilesSelected }: CloudBrowserProps) {
  const [currentPath, setCurrentPath] = useState('')
  const [files, setFiles] = useState<CloudFile[]>([])
  const [selectedFiles, setSelectedFiles] = useState<string[]>([])

  const loadFiles = async (path: string) => {
    const files = await api.browseCloudFiles(connector.id, path)
    setFiles(files)
    setCurrentPath(path)
  }

  return (
    <div>
      <Breadcrumb path={currentPath} onNavigate={loadFiles} />
      <FileList
        files={files}
        selectedFiles={selectedFiles}
        onSelectionChange={setSelectedFiles}
        onFileClick={handleFileClick}
      />
    </div>
  )
}
```

### Architecture Compliance

1. **Service Layer Pattern**: Extend existing service structure in `app/services/`
2. **File Storage Abstraction**: Build on existing `FileStorageService` pattern
3. **Controller Pattern**: Extend existing `ConnectorsController` with cloud endpoints
4. **Model Layer**: Extend existing `DatabaseConnection` model with cloud-specific fields
5. **API Versioning**: Use `/api/v2` prefix as defined in integration architecture
6. **Multi-tenancy**: Ensure all cloud storage data is tenant-scoped

### File Structure Requirements

```
app/
├── controllers/
│   └── connectors_controller.ts          # EXTENDED (Cloud endpoints)
├── models/
│   └── database_connection.ts            # EXTENDED (Cloud fields)
├── services/
│   ├── file_storage_service.ts           # EXTENDED (Cloud providers)
│   ├── cloud_storage_service.ts          # NEW (Factory pattern)
│   ├── s3_storage_service.ts             # NEW (AWS implementation)
│   ├── gcs_storage_service.ts            # NEW (GCP implementation)
│   ├── azure_blob_storage_service.ts     # NEW (Azure implementation)
│   ├── streaming_file_processor.ts       # NEW (Large file handling)
│   └── encryption_service.ts             # EXTENDED (Cloud credentials)
├── validators/
│   └── connectors_validator.ts           # EXTENDED (Cloud validation)
└── jobs/
    └── cloud_file_processor_job.ts       # NEW (Background processing)

database/migrations/
└── xxx_extend_database_connections_for_cloud.ts  # NEW

frontend/
├── components/
│   └── cloud/                            # NEW
│       ├── CloudStorageModal.tsx
│       ├── ProviderSelector.tsx
│       ├── CloudCredentialForm.tsx
│       ├── CloudBrowser.tsx
│       ├── FilePreview.tsx
│       └── ProcessingProgress.tsx
├── lib/
│   ├── api/
│   │   └── connectors.ts                 # EXTENDED (Cloud endpoints)
│   └── hooks/
│       └── useConnectors.ts              # EXTENDED (Cloud state)
└── stores/
    └── connectors.store.ts               # EXTENDED (Cloud connectors)
```

### Library/Framework Requirements

#### Backend Dependencies
```json
{
  "@aws-sdk/client-s3": "^3.600.0",      // AWS S3 SDK v3
  "@aws-sdk/lib-storage": "^3.600.0",    // AWS multipart uploads
  "@google-cloud/storage": "^7.10.0",     // Google Cloud Storage
  "@azure/storage-blob": "^12.20.0",     // Azure Blob Storage
  "@azure/identity": "^4.5.0",           // Azure AD authentication
  "dotenv": "^16.4.0"                    // Environment variables
}
```

#### Frontend Dependencies (none new, using existing)
- React Hook Form for form validation
- TanStack Query for API calls
- Zustand for state management

### Security Considerations

1. **Credential Storage**: Use existing EncryptionService with tenant-specific keys
2. **IAM Roles**: Support AWS IAM roles, Google service accounts, Azure AD
3. **Temporary Credentials**: Support STS, workload identity federation
4. **Access Validation**: Validate bucket/container permissions before processing
5. **Audit Logging**: Log all cloud storage access and operations
6. **Network Security**: Support VPC endpoints and private connectivity

### Performance Requirements

1. **Streaming Support**: Process files >500MB without loading into memory
2. **Parallel Downloads**: Support concurrent file processing
3. **Caching Strategy**: Cache bucket listings and metadata in Redis (TTL: 1 hour)
4. **Bandwidth Management**: Throttle cloud API calls to respect provider limits
5. **Progress Tracking**: Real-time progress for file download and processing

### Error Handling

1. **Cloud-Specific Errors**: Handle provider-specific error codes
2. **Authentication Failures**: Clear error messages for invalid credentials
3. **Permission Errors**: Guide users to proper IAM configurations
4. **Network Issues**: Implement retry logic with exponential backoff
5. **Rate Limiting**: Handle API throttling from cloud providers

## Context Reference

### Epic Context
From Epic 1: Universal Data Connectors
- Story 1.3 extends connector framework to support cloud storage providers
- Enables processing files directly from cloud storage without local upload
- Critical for handling large datasets stored in cloud infrastructure

### Previous Work
Story 1.1 and 1.2 established:
- Database connector patterns and service structure
- API connector framework with OAuth2 flows
- Encryption service for credential storage
- Multi-tenant connector isolation
- File processing pipeline integration

### Project Context
- Current system only supports local file uploads
- Need to support cloud-based data sources for enterprise customers
- Must integrate with existing dataset processing pipeline
- Maintain security and multi-tenant isolation

## Git Intelligence

Based on recent commits, the system has:
- Stable connector infrastructure from Stories 1.1 and 1.2
- Existing `FileStorageService` with S3 and local support
- Encryption service with tenant-specific key derivation
- Background job processing with Bull queues
- Rate limiting with Redis

## Latest Tech Information

### AWS SDK v3 (2025 Best Practices)
- Use streaming uploads/downloads with multipart support
- Implement automatic retry with exponential backoff
- Support IAM roles and STS temporary credentials
- Use proper error handling for AWS-specific errors

### Google Cloud Storage (2025 Updates)
- Official client: @google-cloud/storage v7.10.0
- Support for service account authentication and workload identity
- Enhanced security features and audit logging
- Improved access controls and compliance features

### Azure Blob Storage (2025 Best Practices)
- Primary recommendation: Azure AD authentication
- Use @azure/storage-blob with DefaultAzureCredential
- SAS tokens for temporary, delegated access scenarios
- Passwordless authentication for production applications

### File Streaming Patterns
- Process large files without loading into memory
- Implement progress tracking for user feedback
- Support concurrent processing for multiple files
- Handle network interruptions and resume capabilities

## Dev Agent Record

### Agent Model Used
Claude-4 (anthropic-claude-4-20241101)

### Implementation Notes
This story extends the connector framework to support major cloud storage providers (AWS S3, Google Cloud Storage, Azure Blob Storage) with a focus on streaming large files, secure credential management, and seamless integration with existing file processing pipeline.

### Key Architectural Decisions
1. **Factory Pattern**: Common interface for all cloud storage providers
2. **Streaming First**: Process files directly from cloud without intermediate storage
3. **Credential Security**: Encrypt all cloud credentials with tenant-specific keys
4. **Multi-Provider Support**: Support S3, GCS, and Azure with their authentication methods
5. **Background Processing**: Use existing Bull queues for large file processing

### Security Implementation
- Support for IAM roles and temporary credentials
- Service account authentication for Google Cloud
- Azure AD integration for Blob Storage
- Encrypted credential storage with audit logging
- Permission validation before file processing