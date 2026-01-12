# Unified Coach-Client Connection Interface

## Overview

This document defines the unified connection interface for all 5 Prometheus apps:
- 2 Android Apps (Coach + Client)
- 2 iOS/Swift Apps (Coach + Client)
- 1 React Web App

All apps call the same PostgreSQL RPC functions - ensuring consistent behavior.

---

## API Functions

### 1. `connect_by_invite_code(p_invite_code)`
**Used by:** Client Apps (Android, iOS)

```json
// Request
{ "p_invite_code": "ABC123" }

// Response (Success)
{
  "success": true,
  "connection_id": "uuid",
  "coach_name": "John Smith",
  "message": "Connection request sent to coach"
}

// Response (Error)
{
  "success": false,
  "error": "INVALID_CODE | ALREADY_CONNECTED | REQUEST_PENDING | SELF_CONNECTION",
  "message": "Human readable message"
}
```

### 2. `respond_to_connection(p_connection_id, p_accept)`
**Used by:** Coach Apps (Android, iOS, Web)

```json
// Request
{ "p_connection_id": "uuid", "p_accept": true }

// Response (Success)
{
  "success": true,
  "status": "accepted",
  "message": "Client connected successfully"
}
```

### 3. `get_my_connections()`
**Used by:** All Apps

```json
// Response for Coach
{
  "success": true,
  "role": "coach",
  "connections": [
    {
      "connection_id": "uuid",
      "user_id": "client-uuid",
      "user_name": "Client Name",
      "user_avatar": "https://...",
      "status": "pending|accepted|declined",
      "requested_at": "2024-01-01T00:00:00Z",
      "responded_at": null,
      "role": "client"
    }
  ]
}

// Response for Client
{
  "success": true,
  "role": "client",
  "connections": [
    {
      "connection_id": "uuid",
      "user_id": "coach-uuid",
      "user_name": "Coach Name",
      "user_avatar": "https://...",
      "status": "accepted",
      "role": "coach"
    }
  ]
}
```

### 4. `disconnect_connection(p_connection_id)`
**Used by:** All Apps

```json
// Response
{ "success": true, "message": "Disconnected successfully" }
```

### 5. `get_coach_by_invite_code(p_invite_code)`
**Used by:** Client Apps (preview before connecting)

```json
// Response
{
  "success": true,
  "coach": {
    "id": "uuid",
    "name": "Coach Name",
    "avatar_url": "https://...",
    "bio": "Coach bio..."
  }
}
```

---

## Platform Implementations

### Kotlin (Android)

```kotlin
// ConnectionRepository.kt
@Singleton
class ConnectionRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun connectByInviteCode(code: String): ConnectionResult {
        return try {
            val response = supabaseClient.postgrest.rpc(
                function = "connect_by_invite_code",
                parameters = mapOf("p_invite_code" to code)
            ).decodeAs<JsonObject>()

            if (response["success"]?.jsonPrimitive?.boolean == true) {
                ConnectionResult.Success(
                    connectionId = response["connection_id"]?.jsonPrimitive?.content,
                    coachName = response["coach_name"]?.jsonPrimitive?.content
                )
            } else {
                ConnectionResult.Error(
                    code = response["error"]?.jsonPrimitive?.content ?: "UNKNOWN",
                    message = response["message"]?.jsonPrimitive?.content ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            ConnectionResult.Error("NETWORK_ERROR", e.message ?: "Network error")
        }
    }

    suspend fun respondToConnection(connectionId: String, accept: Boolean): Result<Unit> {
        return try {
            val response = supabaseClient.postgrest.rpc(
                function = "respond_to_connection",
                parameters = mapOf(
                    "p_connection_id" to connectionId,
                    "p_accept" to accept
                )
            ).decodeAs<JsonObject>()

            if (response["success"]?.jsonPrimitive?.boolean == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response["message"]?.jsonPrimitive?.content))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyConnections(): Result<List<Connection>> {
        return try {
            val response = supabaseClient.postgrest.rpc("get_my_connections")
                .decodeAs<JsonObject>()

            if (response["success"]?.jsonPrimitive?.boolean == true) {
                val connections = response["connections"]?.jsonArray?.map { element ->
                    Connection(
                        connectionId = element.jsonObject["connection_id"]?.jsonPrimitive?.content ?: "",
                        userId = element.jsonObject["user_id"]?.jsonPrimitive?.content ?: "",
                        userName = element.jsonObject["user_name"]?.jsonPrimitive?.content ?: "",
                        userAvatar = element.jsonObject["user_avatar"]?.jsonPrimitive?.contentOrNull,
                        status = element.jsonObject["status"]?.jsonPrimitive?.content ?: "",
                        role = element.jsonObject["role"]?.jsonPrimitive?.content ?: ""
                    )
                } ?: emptyList()
                Result.success(connections)
            } else {
                Result.failure(Exception("Failed to get connections"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

sealed class ConnectionResult {
    data class Success(val connectionId: String?, val coachName: String?) : ConnectionResult()
    data class Error(val code: String, val message: String) : ConnectionResult()
}

data class Connection(
    val connectionId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val status: String,
    val role: String
)
```

### Swift (iOS)

```swift
// ConnectionRepository.swift
class ConnectionRepository {
    private let supabase: SupabaseClient

    init(supabase: SupabaseClient) {
        self.supabase = supabase
    }

    func connectByInviteCode(_ code: String) async throws -> ConnectionResult {
        let response: ConnectionResponse = try await supabase
            .rpc("connect_by_invite_code", params: ["p_invite_code": code])
            .execute()
            .value

        if response.success {
            return .success(connectionId: response.connectionId, coachName: response.coachName)
        } else {
            return .error(code: response.error ?? "UNKNOWN", message: response.message ?? "Unknown error")
        }
    }

    func respondToConnection(_ connectionId: String, accept: Bool) async throws {
        let response: BaseResponse = try await supabase
            .rpc("respond_to_connection", params: [
                "p_connection_id": connectionId,
                "p_accept": accept
            ])
            .execute()
            .value

        if !response.success {
            throw ConnectionError.failed(response.message ?? "Failed")
        }
    }

    func getMyConnections() async throws -> [Connection] {
        let response: ConnectionsResponse = try await supabase
            .rpc("get_my_connections")
            .execute()
            .value

        return response.connections
    }
}

enum ConnectionResult {
    case success(connectionId: String?, coachName: String?)
    case error(code: String, message: String)
}

struct Connection: Codable {
    let connectionId: String
    let userId: String
    let userName: String
    let userAvatar: String?
    let status: String
    let role: String

    enum CodingKeys: String, CodingKey {
        case connectionId = "connection_id"
        case userId = "user_id"
        case userName = "user_name"
        case userAvatar = "user_avatar"
        case status
        case role
    }
}
```

### TypeScript (React)

```typescript
// connectionService.ts
interface ConnectionResult {
  success: boolean;
  connection_id?: string;
  coach_name?: string;
  error?: string;
  message?: string;
}

interface Connection {
  connection_id: string;
  user_id: string;
  user_name: string;
  user_avatar: string | null;
  status: 'pending' | 'accepted' | 'declined';
  role: 'coach' | 'client';
  requested_at: string;
  responded_at: string | null;
}

export const connectionService = {
  async connectByInviteCode(code: string): Promise<ConnectionResult> {
    const { data, error } = await supabase.rpc('connect_by_invite_code', {
      p_invite_code: code
    });

    if (error) throw error;
    return data;
  },

  async respondToConnection(connectionId: string, accept: boolean): Promise<void> {
    const { data, error } = await supabase.rpc('respond_to_connection', {
      p_connection_id: connectionId,
      p_accept: accept
    });

    if (error) throw error;
    if (!data.success) throw new Error(data.message);
  },

  async getMyConnections(): Promise<Connection[]> {
    const { data, error } = await supabase.rpc('get_my_connections');

    if (error) throw error;
    return data.connections || [];
  },

  async disconnect(connectionId: string): Promise<void> {
    const { data, error } = await supabase.rpc('disconnect_connection', {
      p_connection_id: connectionId
    });

    if (error) throw error;
    if (!data.success) throw new Error(data.message);
  }
};
```

---

## Realtime Subscription with Fallback

### Pattern: Subscribe + Poll Fallback

```typescript
// All platforms should implement this pattern
class ConnectionManager {
  private pollInterval: Timer | null = null;
  private realtimeChannel: RealtimeChannel | null = null;

  async startListening(userId: string, onUpdate: (connections: Connection[]) => void) {
    // 1. Initial fetch
    const connections = await this.fetchConnections();
    onUpdate(connections);

    // 2. Try Realtime subscription
    try {
      this.realtimeChannel = supabase
        .channel(`connections:${userId}`)
        .on('postgres_changes', {
          event: '*',
          schema: 'public',
          table: 'coach_client_connections',
          filter: `coach_id=eq.${userId}`  // or client_id for client apps
        }, async () => {
          const updated = await this.fetchConnections();
          onUpdate(updated);
        })
        .subscribe((status) => {
          if (status === 'SUBSCRIBED') {
            console.log('Realtime connected');
            this.stopPolling();
          } else if (status === 'CHANNEL_ERROR') {
            console.log('Realtime failed, starting polling fallback');
            this.startPolling(onUpdate);
          }
        });
    } catch (e) {
      // 3. Fallback to polling if Realtime fails
      this.startPolling(onUpdate);
    }
  }

  private startPolling(onUpdate: (connections: Connection[]) => void) {
    if (this.pollInterval) return;

    this.pollInterval = setInterval(async () => {
      const connections = await this.fetchConnections();
      onUpdate(connections);
    }, 30000); // Poll every 30 seconds
  }

  private stopPolling() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  stop() {
    this.stopPolling();
    this.realtimeChannel?.unsubscribe();
  }
}
```

---

## Error Codes Reference

| Code | Description | User Action |
|------|-------------|-------------|
| `NOT_AUTHENTICATED` | User not logged in | Redirect to login |
| `INVALID_CODE` | Invite code not found | Show error, let user retry |
| `ALREADY_CONNECTED` | Already connected to this coach | Show info |
| `REQUEST_PENDING` | Request already sent | Show pending status |
| `SELF_CONNECTION` | Can't connect to self | Show error |
| `NOT_FOUND` | Connection not found | Refresh list |
| `ALREADY_RESPONDED` | Request already handled | Refresh list |
| `INTERNAL_ERROR` | Server error | Retry later |

---

## Migration Notes

To migrate from direct table access to RPC functions:

1. Run `unified_connection_functions.sql` in Supabase SQL Editor
2. Update app code to use `supabase.rpc()` instead of `supabase.from()`
3. Remove direct table queries for connections
4. Test all flows: invite, accept, decline, disconnect

Benefits:
- Single source of truth for business logic
- Consistent behavior across all 5 apps
- Easier to maintain and update
- Server-side validation = more secure
