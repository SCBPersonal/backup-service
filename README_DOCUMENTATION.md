# Backup Orchestrator Service - Documentation Index

## 📚 Complete Documentation Suite

This repository contains comprehensive documentation for the Backup Orchestrator Service, including architecture diagrams, use cases, and quick reference guides.

---

## 📁 Documentation Files

### 1. **BACKUP_ORCHESTRATOR_DIAGRAMS.md** 🎨
**Purpose**: Visual diagrams for architecture and workflows

**Contains:**
- ✅ Full Backup Sequence Diagram (6 phases)
- ✅ Incremental Backup Sequence Diagram (with error handling)
- ✅ Three-Table Architecture Flow (18-step numbered flow)
- ✅ Backup Lifecycle State Diagram (state machine)
- ✅ Enhanced ER Diagram (entity relationships)
- ✅ Detailed Database Schema with Constraints (class diagram style)
- ✅ Database Operations Flow (full vs incremental comparison)

**Total Diagrams**: 7 comprehensive Mermaid diagrams

**Use For:**
- Understanding system architecture
- Design reviews and presentations
- Developer onboarding
- Troubleshooting workflows

---

### 2. **EXAMPLE_USE_CASES.md** 📖
**Purpose**: Real-world examples and scenarios

**Contains:**
- ✅ Full Backup Use Case (step-by-step)
- ✅ Incremental Backup Use Case (with base UUID dependency)
- ✅ Error Scenarios (missing base UUID, job failures)
- ✅ Database State Examples (multiple categories, month rollover)
- ✅ API Request/Response Examples
- ✅ Monitoring Query Examples

**Use For:**
- Learning how the system works
- Testing scenarios
- Understanding error handling
- Writing integration tests

---

### 3. **QUICK_REFERENCE_GUIDE.md** ⚡
**Purpose**: Quick lookup for common tasks and troubleshooting

**Contains:**
- ✅ API Endpoints (request/response formats)
- ✅ Database Tables (structure and purpose)
- ✅ Key Workflows (full and incremental)
- ✅ Configuration (YBA config, application properties)
- ✅ Monitoring Queries (status checks, failed backups)
- ✅ Troubleshooting (5 common issues with solutions)
- ✅ Common Commands (restart, logs, cleanup)
- ✅ Performance Tips (index optimization)
- ✅ Best Practices

**Use For:**
- Day-to-day operations
- Troubleshooting issues
- Monitoring backup status
- Quick reference during incidents

---

### 4. **document/BACKUP_ORCHESTRATOR_SERVICE_MASTER_PROMPT.md** 📋
**Purpose**: Comprehensive technical specification (Updated)

**Contains:**
- ✅ Service Overview (updated with three-table design)
- ✅ Architecture Overview (updated with BackupPollerService)
- ✅ Key Responsibilities (updated with async polling)
- ✅ Three-Table Design (detailed explanation)
- ✅ Core Components (BackupService, YbaClient, etc.)
- ✅ Database Schemas (all three tables)
- ✅ API Contracts (YBA integration)
- ✅ Error Handling (retry mechanisms)
- ✅ Configuration Management
- ✅ Deployment Guidelines

**Use For:**
- Complete system understanding
- Making modifications to the service
- Developer onboarding
- AI context for code generation

---

## 🎯 Quick Navigation

### For New Developers
1. Start with **BACKUP_ORCHESTRATOR_DIAGRAMS.md** - Visual overview
2. Read **EXAMPLE_USE_CASES.md** - Understand workflows
3. Reference **QUICK_REFERENCE_GUIDE.md** - Common tasks
4. Deep dive into **BACKUP_ORCHESTRATOR_SERVICE_MASTER_PROMPT.md** - Technical details

### For Operations/Support
1. **QUICK_REFERENCE_GUIDE.md** - Troubleshooting and monitoring
2. **EXAMPLE_USE_CASES.md** - Error scenarios
3. **BACKUP_ORCHESTRATOR_DIAGRAMS.md** - Workflow diagrams

### For Architects/Designers
1. **BACKUP_ORCHESTRATOR_DIAGRAMS.md** - All architecture diagrams
2. **BACKUP_ORCHESTRATOR_SERVICE_MASTER_PROMPT.md** - Technical specification
3. **EXAMPLE_USE_CASES.md** - Database state examples

---

## 🔑 Key Concepts

### Three-Table Design
1. **BATCH_EXECUTION_TABLE** - Managed by batch framework
2. **full_backup_tracker** - Full backup state + base UUID storage
3. **incremental_backup_tracker** - Incremental backup state

### Critical Flow
```
Full Backup → BackupPollerService → base_backup_uuid → Incremental Backup
```

### Monthly Backup Strategy
- **Day 1-5**: Full backup (stores base_backup_uuid)
- **Day 6-31**: Daily incremental backups (reference base_backup_uuid)
- **Next Month**: New full backup (new base_backup_uuid)

---

## 📊 Diagram Viewing

All diagrams use **Mermaid** syntax. View them using:

1. **GitHub/GitLab** - Native support (just view the .md files)
2. **Mermaid Live Editor** - https://mermaid.live (copy/paste diagram code)
3. **VS Code** - Install "Markdown Preview Mermaid Support" extension
4. **Confluence** - Install Mermaid plugin
5. **Draw.io** - Import Mermaid diagrams

---

## 🛠️ Common Tasks

### Check Backup Status
```sql
-- See QUICK_REFERENCE_GUIDE.md - Monitoring Queries section
```

### Trigger Full Backup
```bash
curl -X POST http://localhost:8989/backupProcess \
  -H "Content-Type: application/json" \
  -d '{"batchId":"BATCH_20260206_001","businessDate":"2026-02-06","categoryCode":"HWA_EPR_DB_BACKUP_FULL"}'
```

### Trigger Incremental Backup
```bash
curl -X POST http://localhost:8989/backupProcess \
  -H "Content-Type: application/json" \
  -d '{"batchId":"BATCH_20260207_001","businessDate":"2026-02-07","categoryCode":"HWA_EPR_DB_BACKUP_INCRE"}'
```

### Troubleshoot Failed Backup
```
See QUICK_REFERENCE_GUIDE.md - Troubleshooting section
```

---

## 📝 Document Versions

| Document | Version | Last Updated |
|----------|---------|--------------|
| BACKUP_ORCHESTRATOR_DIAGRAMS.md | 1.0 | 2026-02-06 |
| EXAMPLE_USE_CASES.md | 1.0 | 2026-02-06 |
| QUICK_REFERENCE_GUIDE.md | 1.0 | 2026-02-06 |
| BACKUP_ORCHESTRATOR_SERVICE_MASTER_PROMPT.md | Updated | 2026-02-06 |

---

## 🎨 Diagram Summary

| # | Diagram Name | Type | Purpose |
|---|--------------|------|---------|
| 1 | Full Backup Sequence | Sequence | Complete full backup flow |
| 2 | Incremental Backup Sequence | Sequence | Incremental backup with error handling |
| 3 | Three-Table Architecture Flow | Graph | Overall system architecture |
| 4 | Backup Lifecycle State | State | State machine transitions |
| 5 | Enhanced ER Diagram | ER | Entity relationships |
| 6 | Detailed Schema with Constraints | Class | Database schema details |
| 7 | Database Operations Flow | Graph | DB operations comparison |

---

## 🚀 Getting Started

### Step 1: Understand the Architecture
Read **BACKUP_ORCHESTRATOR_DIAGRAMS.md** sections 1-4

### Step 2: Learn the Workflows
Read **EXAMPLE_USE_CASES.md** - Full and Incremental backup examples

### Step 3: Set Up Monitoring
Use queries from **QUICK_REFERENCE_GUIDE.md** - Monitoring section

### Step 4: Test the System
Follow examples in **EXAMPLE_USE_CASES.md** - API Request/Response section

---

## 📞 Support

For questions or issues:
1. Check **QUICK_REFERENCE_GUIDE.md** - Troubleshooting section
2. Review **EXAMPLE_USE_CASES.md** - Error Scenarios
3. Consult **BACKUP_ORCHESTRATOR_SERVICE_MASTER_PROMPT.md** - Technical details
4. Contact Backup Orchestrator Service Team

---

**Documentation Suite Version:** 1.0  
**Last Updated:** 2026-02-06  
**Maintained By:** Backup Orchestrator Service Team

