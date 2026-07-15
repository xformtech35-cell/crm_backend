# CRM to Spring Boot API Migration

This document maps the current CodeIgniter CRM behavior to a Spring Boot REST backend design.

## 1) Recommended Spring Boot stack

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security (JWT)
- Flyway or Liquibase
- MySQL driver
- springdoc-openapi

## 2) Suggested package structure

- com.crm.api.config
- com.crm.api.security
- com.crm.api.common
- com.crm.api.auth
- com.crm.api.dashboard
- com.crm.api.lead
- com.crm.api.opportunity
- com.crm.api.project
- com.crm.api.task
- com.crm.api.contact
- com.crm.api.organization
- com.crm.api.role
- com.crm.api.team
- com.crm.api.teammember
- com.crm.api.createteam
- com.crm.api.report

Inside each module:

- controller
- service
- repository
- entity
- dto
- mapper

## 3) Existing database tables identified

- user
- role
- permission
- team
- team_member
- create_team
- lead
- lead_note
- lead_reminder
- opportunity
- project
- task
- contact
- organization

Also used via second DB connection (accounting):

- customer

## 4) Full API list for Spring Boot

All endpoints below are proposed REST equivalents of current PHP actions.

### 4.1 Auth

- POST /api/v1/auth/login
  - body: { usernameOrEmail, password }
  - returns JWT + user profile + permissions
- POST /api/v1/auth/logout
- POST /api/v1/auth/change-password
  - body: { oldPassword, newPassword, confirmPassword }
- POST /api/v1/auth/forgot-password
  - body: { mobile }

### 4.2 Dashboard and Calendar

- GET /api/v1/dashboard/summary
  - returns lead/opportunity/project aggregates currently shown on home page
- GET /api/v1/calendar/events
  - returns task events + lead reminders

### 4.3 Leads

- GET /api/v1/leads
  - query: status, assignedUserId, fromDate, toDate, leadType, leadSource
- GET /api/v1/leads/{id}
- POST /api/v1/leads
  - multipart form: lead fields + up to 4 documents
- PUT /api/v1/leads/{id}
  - multipart form: editable fields + optional document updates
- PATCH /api/v1/leads/{id}/status
  - body: { leadStatus }
- DELETE /api/v1/leads/{id}

Lead notes:

- GET /api/v1/leads/{id}/notes
- POST /api/v1/leads/{id}/notes
  - body: { leadNote }
- DELETE /api/v1/leads/notes/{noteId}
- GET /api/v1/leads/notes

Lead reminders:

- GET /api/v1/leads/{id}/reminders
- POST /api/v1/leads/{id}/reminders
  - body: { reminderFor, reminderDate }
- DELETE /api/v1/leads/reminders/{reminderId}
- GET /api/v1/leads/reminders

Lead conversion and import:

- POST /api/v1/leads/{id}/convert
  - body: { opportunityName }
  - action: create opportunity, update lead status=Converted, optionally sync accounting customer
- POST /api/v1/leads/import/indiamart
  - body: { fromDate, toDate }
  - action: fetch IndiaMart API and insert non-duplicate unique_query_id

Lead reports:

- GET /api/v1/reports/leads
  - query: fromDate, toDate, leadType, leadStatus, leadSource

### 4.4 Opportunities

- GET /api/v1/opportunities
  - query: status
- GET /api/v1/opportunities/{id}
- POST /api/v1/opportunities
  - multipart form: opportunity fields + optional doc
- PUT /api/v1/opportunities/{id}
  - multipart form: editable fields + optional doc
- DELETE /api/v1/opportunities/{id}

Opportunity reports:

- GET /api/v1/reports/opportunities

### 4.5 Projects

- GET /api/v1/projects
- GET /api/v1/projects/{id}
- POST /api/v1/projects
  - multipart form: project fields + optional document
  - action: generate projectCode
- PUT /api/v1/projects/{id}
  - multipart form: editable fields + optional document
- DELETE /api/v1/projects/{id}

Project reports:

- GET /api/v1/reports/projects

### 4.6 Tasks

- GET /api/v1/tasks
- GET /api/v1/tasks/{id}
- POST /api/v1/tasks
  - multipart form:
    - taskAssign=Member with assignedMemberIds[] (creates one task per member)
    - or taskAssign=Team with assignedTeamId
- PUT /api/v1/tasks/{id}
- DELETE /api/v1/tasks/{id}
- GET /api/v1/tasks/by-team

Task reports:

- GET /api/v1/reports/tasks

### 4.7 Contacts

- GET /api/v1/contacts
- GET /api/v1/contacts/{id}
- POST /api/v1/contacts
- PUT /api/v1/contacts/{id}
- DELETE /api/v1/contacts/{id}

Contact reports:

- GET /api/v1/reports/contacts

### 4.8 Organizations

- GET /api/v1/organizations
- GET /api/v1/organizations/{id}
- POST /api/v1/organizations
- PUT /api/v1/organizations/{id}
- DELETE /api/v1/organizations/{id}

Organization reports:

- GET /api/v1/reports/organizations

### 4.9 Roles and Permissions

- GET /api/v1/roles
- GET /api/v1/roles/{id}
- POST /api/v1/roles
- PUT /api/v1/roles/{id}
- DELETE /api/v1/roles/{id}

Role permissions:

- GET /api/v1/roles/{id}/permissions
- PUT /api/v1/roles/{id}/permissions
  - body: { permissions: ["lead", "opportunity", ...] }

### 4.10 Team

- GET /api/v1/teams
- GET /api/v1/teams/{id}
- POST /api/v1/teams
- PUT /api/v1/teams/{id}
- DELETE /api/v1/teams/{id}

### 4.11 Team Members

- GET /api/v1/team-members
- GET /api/v1/team-members/{id}
- POST /api/v1/team-members
  - creates both user and team_member records
- PUT /api/v1/team-members/{id}
  - updates user + team_member
- DELETE /api/v1/team-members/{id}
  - deletes user + team_member

### 4.12 Create Team assignments

- GET /api/v1/team-assignments
- GET /api/v1/team-assignments/{id}
- POST /api/v1/team-assignments
  - body: { teamId, memberIds[] }
- PUT /api/v1/team-assignments/{id}
- DELETE /api/v1/team-assignments/{id}

## 5) Core DTO templates

```java
public record ApiResponse<T>(
    boolean success,
    String message,
    T data
) {}
```

```java
public record PageResponse<T>(
    java.util.List<T> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {}
```

Lead create request example:

```java
public record LeadCreateRequest(
    String firstName,
    String lastName,
    String title,
    String mobileNo,
    String address,
    String email,
    String city,
    String state,
    String country,
    String phoneNo,
    String organizationName,
    String website,
    String industry,
    Integer noOfEmployee,
    String source,
    String type,
    String reason,
    String status,
    String designation,
    java.time.LocalDate inquiryDate,
    Long userIdFk
) {}
```

## 6) Example controller style (for all modules)

```java
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LeadDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String leadType,
            @RequestParam(required = false) String leadSource,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponseBuilder.success(
                leadService.search(status, assignedUserId, fromDate, toDate, leadType, leadSource, page, size)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadDetailDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseBuilder.success(leadService.getById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LeadDto>> create(
            @Valid @ModelAttribute LeadCreateMultipartRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseBuilder.success("Lead created", leadService.create(request)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LeadDto>> update(
            @PathVariable Long id,
            @Valid @ModelAttribute LeadUpdateMultipartRequest request
    ) {
        return ResponseEntity.ok(ApiResponseBuilder.success("Lead updated", leadService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody LeadStatusUpdateRequest request
    ) {
        leadService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponseBuilder.success("Lead status updated", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        leadService.delete(id);
        return ResponseEntity.ok(ApiResponseBuilder.success("Lead deleted", null));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<ApiResponse<OpportunityDto>> convert(
            @PathVariable Long id,
            @Valid @RequestBody LeadConvertRequest request
    ) {
        return ResponseEntity.ok(ApiResponseBuilder.success("Lead converted", leadService.convert(id, request)));
    }
}
```

## 7) Entity naming map (PHP -> Java)

- lead.lead_id -> Lead.id
- opportunity.opp_id -> Opportunity.id
- project.project_id -> Project.id
- task.task_id -> Task.id
- team_member.team_member_id -> TeamMember.id
- create_team.create_team_id -> TeamAssignment.id
- role.role_id -> Role.id
- permission.role_id_fk + grp_perm -> RolePermission

## 8) Migration risks to fix while moving

- Password handling is inconsistent (plain, sha1(md5), password_verify). Use BCrypt only in Spring Security.
- Some PHP updates return false when no row changes; in Spring, treat idempotent update as success.
- Multiple endpoints currently return HTML views; convert to JSON-only APIs.
- File upload currently allows all types. In Spring, whitelist mime types and scan size limits.
- Session-based auth should become stateless JWT for API use.

## 9) Priority implementation order

1. Auth, role, permission
2. Lead + notes + reminders + convert
3. Opportunity
4. Team and team members
5. Task and calendar
6. Project
7. Contact and organization
8. Dashboard/report endpoints

## 10) Minimum OpenAPI groups

- auth
- dashboard
- leads
- opportunities
- projects
- tasks
- contacts
- organizations
- roles
- teams
- team-members
- team-assignments
- reports
