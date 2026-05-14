# Graph Report - File-Share  (2026-05-14)

## Corpus Check
- 66 files · ~781,723 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 420 nodes · 614 edges · 46 communities (19 shown, 27 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 62 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `352b8afc`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]

## God Nodes (most connected - your core abstractions)
1. `directTransferActivity` - 29 edges
2. `get_connection()` - 20 edges
3. `UploadApis` - 17 edges
4. `MainActivity` - 14 edges
5. `UserProfileActivity` - 13 edges
6. `sendActivity` - 13 edges
7. `receiveActivity` - 12 edges
8. `UserLocalStore` - 11 edges
9. `UserSentHistoryActivity` - 9 edges
10. `ReceivedHistoryActivity` - 9 edges

## Surprising Connections (you probably didn't know these)
- `admin_view()` --calls--> `get_connection()`  [EXTRACTED]
  api/response_app/views.py → api/response_app/utils.py
- `login()` --calls--> `get_connection()`  [EXTRACTED]
  api/response_app/views.py → api/response_app/utils.py
- `signup()` --calls--> `get_connection()`  [EXTRACTED]
  api/response_app/views.py → api/response_app/utils.py
- `setUserProfilePicture()` --calls--> `get_connection()`  [EXTRACTED]
  api/response_app/views.py → api/response_app/utils.py
- `getUserProfilePicture()` --calls--> `get_connection()`  [EXTRACTED]
  api/response_app/views.py → api/response_app/utils.py

## Communities (46 total, 27 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.07
Nodes (8): CheckOtpActivity, FindAccountActivity, loginActivity, NewPasswordActivity, signupActivity, UploadApis, AppCompatActivity, RequestBody

### Community 1 - "Community 1"
Cohesion: 0.11
Nodes (7): directTransferActivity, onConnectionInfoAvailable(), onPeersAvailable(), SendTask, WiFiDirectBroadcastReceiver, BroadcastReceiver, Runnable

### Community 2 - "Community 2"
Cohesion: 0.08
Nodes (6): ReceivedHistoryActivity, UserSentHistoryActivity, AppDatabase, FileDao, userInfo, RoomDatabase

### Community 3 - "Community 3"
Cohesion: 0.11
Nodes (25): AppConfig, ResponseAppConfig, check_firebase(), get_connection(), get_connection_without_dbname(), send_fcm_notification(), send_otp_email(), admin_view() (+17 more)

### Community 4 - "Community 4"
Cohesion: 0.1
Nodes (5): sendActivity, DialogFragment, SmallFunctions, UriWorks, UserHistoryResponse

### Community 6 - "Community 6"
Cohesion: 0.13
Nodes (5): FileAdapter, FileViewHolder, ReceivedFilesActivity, SharedFile, Filterable

### Community 7 - "Community 7"
Cohesion: 0.09
Nodes (22): Android App Setup, Backend Setup, code:bash (DB_HOST=db), code:bash (docker compose up), code:python (config = {), code:python (EMAIL_HOST_USER = "your gmail"), code:bash (pip install uv), code:bash (python -m uv run manage.py runserver 0.0.0.0:8000) (+14 more)

### Community 11 - "Community 11"
Cohesion: 0.2
Nodes (3): ReceivedGroup, ReceiveHistoryAdapter, VH

### Community 12 - "Community 12"
Cohesion: 0.2
Nodes (3): HistoryAdapter, SentGroup, VH

## Knowledge Gaps
- **22 isolated node(s):** `Run administrative tasks.`, `ASGI config for file_sharing_project project.  It exposes the ASGI callable as`, `URL configuration for file_sharing_project project.  The `urlpatterns` list ro`, `WSGI config for file_sharing_project project.  It exposes the WSGI callable as`, `Django settings for file_sharing_project project.  Generated by 'django-admin` (+17 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **27 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `directTransferActivity` connect `Community 1` to `Community 0`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **Why does `sendActivity` connect `Community 4` to `Community 0`?**
  _High betweenness centrality (0.073) - this node is a cross-community bridge._
- **Why does `MainActivity` connect `Community 5` to `Community 0`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **What connects `Run administrative tasks.`, `ASGI config for file_sharing_project project.  It exposes the ASGI callable as`, `URL configuration for file_sharing_project project.  The `urlpatterns` list ro` to the rest of the system?**
  _22 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.07 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.11 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._