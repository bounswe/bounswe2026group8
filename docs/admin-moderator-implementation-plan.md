# Admin, Moderator, and Verification Coordinator Implementation Plan

This plan adds application-level staff roles to the Neighborhood Emergency Preparedness Hub. It is written against the current project structure:

- Backend: Django 4.2, Django REST Framework, SimpleJWT, APIView-based endpoints.
- Frontend: React 19, Vite, React Router, `AuthContext`, `frontend/src/services/api.js`.
- Mobile: Android Kotlin, Retrofit/OkHttp, models under `mobile/app/src/main/java/com/bounswe2026group8/emergencyhub/api`.

## 1. Scope and Decisions

### In scope

- Add application staff roles: `NONE`, `MODERATOR`, `VERIFICATION_COORDINATOR`, `ADMIN`.
- Keep these roles independent from the existing community role field `User.role`, which remains `STANDARD` or `EXPERT`.
- Add admin APIs for staff-role management, account suspension/reactivation, and hub management.
- Add moderator APIs for forum and help-request/help-offer moderation.
- Add verification APIs for expert certification review.
- Add immutable audit logging for staff actions.
- Expose staff role and verification state to web/mobile clients as read-only data.
- Add focused backend, frontend, and Android tests.

### Out of scope for this iteration

- Forum post pinning.
- Forum thread locking.
- Hub-scoped moderators. Moderators are global.
- Replacing Django admin permissions. `is_staff`, `is_superuser`, groups, and Django admin access remain separate from application staff roles.

## 2. Role Model

The system will have two independent role axes:

- Community role: `User.role` with current values `STANDARD`, `EXPERT`.
- Staff role: new `User.staff_role` with values `NONE`, `MODERATOR`, `VERIFICATION_COORDINATOR`, `ADMIN`.

All existing and newly registered users default to `staff_role=NONE`.

Only users with `staff_role=ADMIN` can assign or revoke application staff roles. Staff-role changes must never be accepted from registration, login, profile update, `/me`, or public user-profile request payloads.

## 3. Permission Model

### Moderator

Moderators handle content safety across all hubs.

Allowed:

- View moderation queues for reported or hidden forum posts.
- Hide, restore, and permanently remove forum posts.
- Delete any forum comment.
- Delete any help request, help offer, and help comment.
- See moderation metadata that is hidden from normal users, such as report counts and prior moderation decisions.

Not allowed:

- Assign or revoke staff roles.
- Suspend or reactivate users.
- Manage hubs.
- Approve or reject expertise certifications.
- Change platform-level configuration.

### Verification Coordinator

Verification Coordinators handle expert certification assessment.

Allowed:

- View expertise records pending verification.
- View submitted certification document URLs.
- Approve expertise records.
- Reject expertise records with a required decision note.
- Reopen a decided expertise record when reassessment is needed.
- View verification history.

Not allowed:

- Moderate forum or help content.
- Assign or revoke staff roles.
- Suspend or reactivate users.
- Manage hubs.
- Change platform-level configuration.

### Admin

Admins govern application access and platform data.

Allowed:

- Perform all moderator actions.
- Perform all verification coordinator actions.
- Assign and revoke `MODERATOR`, `VERIFICATION_COORDINATOR`, and `ADMIN`.
- Suspend and reactivate user accounts.
- Create, update, and archive or delete hubs.
- View audit logs.

Admin safeguards:

- A user must not be able to remove their own `ADMIN` role through the API.
- The API must reject changes that would leave the system with zero active admins.
- Account suspension must reject suspending the last active admin.
- Application `ADMIN` does not automatically imply Django `is_staff` or `is_superuser`.

## 4. Backend Implementation

### 4.1 User model changes

Update `backend/accounts/models.py`.

Add a nested `StaffRole` enum to `User`:

```python
class StaffRole(models.TextChoices):
    NONE = 'NONE', 'None'
    MODERATOR = 'MODERATOR', 'Moderator'
    VERIFICATION_COORDINATOR = 'VERIFICATION_COORDINATOR', 'Verification Coordinator'
    ADMIN = 'ADMIN', 'Admin'
```

Add the field:

```python
staff_role = models.CharField(
    max_length=32,
    choices=StaffRole.choices,
    default=StaffRole.NONE,
)
```

Keep `is_active` as the account suspension flag. Suspended users already fail login in `LoginSerializer`; add tests to lock this behavior.

Update `User.__str__`, Django admin list display/filter, and sample data only if useful for local development.

### 4.2 Expertise verification model changes

Update `ExpertiseField` in `backend/accounts/models.py`.

Add:

- `verification_status`: `PENDING`, `APPROVED`, `REJECTED`.
- `reviewed_by`: nullable FK to `settings.AUTH_USER_MODEL`, `on_delete=models.SET_NULL`, `related_name='reviewed_expertise_fields'`.
- `reviewed_at`: nullable datetime.
- `verification_note`: nullable/blank text.

Default `verification_status=PENDING` for every new expertise record.

When a user creates or edits `field`, `certification_level`, or `certification_document_url`, reset verification state to:

- `verification_status=PENDING`
- `reviewed_by=None`
- `reviewed_at=None`
- `verification_note=''`

This prevents stale approvals from surviving a changed certification.

### 4.3 Audit logging

Create an audit model in a new file or directly in `accounts.models` if keeping the app small. Suggested name: `StaffAuditLog`.

Fields:

- `actor`: FK to user, nullable with `SET_NULL`.
- `target_user`: FK to user, nullable with `SET_NULL`, for user/staff/suspension actions.
- `target_type`: short string, for example `USER`, `POST`, `COMMENT`, `HELP_REQUEST`, `HELP_OFFER`, `HELP_COMMENT`, `EXPERTISE_FIELD`, `HUB`.
- `target_id`: string or positive integer stored as string for flexibility.
- `action`: enum/string, for example `STAFF_ROLE_CHANGED`, `USER_SUSPENDED`, `FORUM_POST_HIDDEN`, `EXPERTISE_APPROVED`.
- `previous_state`: JSONField default dict.
- `new_state`: JSONField default dict.
- `reason`: blank text.
- `created_at`: auto timestamp.

Rules:

- Audit records are append-only. Do not expose update/delete endpoints.
- Every staff action endpoint writes an audit row in the same transaction as the state change.
- Rejection and destructive moderation actions require a non-empty reason.

### 4.4 Permission classes

Create `backend/accounts/permissions.py`.

Implement:

- `IsAdminStaffRole`
- `IsModeratorOrAdmin`
- `IsVerificationCoordinatorOrAdmin`

Each class should require authenticated users and check `request.user.staff_role`.

Suggested helpers:

- `user_is_admin(user)`
- `user_is_moderator_or_admin(user)`
- `user_is_verification_coordinator_or_admin(user)`

Use these helpers in view-level logic where a request can be authorized either by ownership or staff role, such as deleting a comment.

### 4.5 Serializer changes

Update `backend/accounts/serializers.py`.

`UserSerializer`:

- Add `staff_role` as a read-only field.
- Keep all existing fields read-only.
- Keep public user profile behavior acceptable for now, but avoid leaking sensitive fields in future user-list/admin APIs by using dedicated serializers.

`RegisterSerializer`:

- Do not add `staff_role`.
- Explicitly ignore or reject unexpected `staff_role`, `is_active`, `is_staff`, and `is_superuser` payload keys if adding stricter validation.

`ExpertiseFieldSerializer`:

- Add read-only fields for `verification_status`, `reviewed_by`, `reviewed_at`, and `verification_note`.
- Prefer nesting a minimal reviewer object or exposing `reviewed_by_id` plus reviewer display name.

Add dedicated serializers:

- `StaffUserListSerializer` for admin user management.
- `StaffRoleUpdateSerializer` with `staff_role` and optional `reason`.
- `AccountStatusUpdateSerializer` with `is_active` and required `reason`.
- `HubWriteSerializer` for admin hub create/update.
- `ExpertiseVerificationDecisionSerializer` with `status` and `note`.

### 4.6 Admin and account-management endpoints

Add APIView classes in `backend/accounts/views.py` or a new `staff_views.py`.

Use `/staff/...` for application staff APIs because `/admin/` is already reserved by Django admin in `backend/backend/urls.py`.

Routes to add in `backend/accounts/urls.py`:

- `GET /staff/users/`
- `PATCH /staff/users/<id>/staff-role/`
- `PATCH /staff/users/<id>/status/`
- `GET /staff/audit-logs/`
- `POST /staff/hubs/`
- `PATCH /staff/hubs/<id>/`
- `DELETE /staff/hubs/<id>/`

Authorization:

- All routes require `IsAdminStaffRole`.

Behavior:

- User list supports search by email/full name and filters by `role`, `staff_role`, `is_active`, and `hub`.
- Staff role update rejects invalid roles, self-demotion, and removing the last active admin.
- Account status update toggles `is_active` and rejects suspending the last active admin.
- Hub delete should be considered carefully because `Post`, `HelpRequest`, and `HelpOffer` currently use `on_delete=CASCADE`. Prefer hub archive/deactivation if the product should preserve existing content. If deleting is kept, the UI must clearly warn about cascading content deletion.

### 4.7 Forum moderation endpoints

Update `backend/forum/views.py` and `backend/forum/urls.py`.

Routes to add:

- `GET /forum/moderation/posts/`
- `PATCH /forum/posts/<id>/moderation/`
- `DELETE /forum/moderation/comments/<id>/`

Authorization:

- All moderation routes require `IsModeratorOrAdmin`.

Behavior:

- Moderation list returns posts with `status != ACTIVE` or `report_count > 0`; support filters for `status`, `forum_type`, `hub`, and minimum report count.
- Post moderation accepts actions `HIDE`, `RESTORE`, `REMOVE`.
- `HIDE` sets `Post.status=HIDDEN`.
- `RESTORE` sets `Post.status=ACTIVE`.
- `REMOVE` sets `Post.status=REMOVED`.
- Normal post list already filters `ACTIVE`; ensure post detail and comments do not expose `HIDDEN` or `REMOVED` posts to non-staff users.
- Moderator comment delete uses the same counter update pattern as existing author delete and writes audit.

Important existing behavior to preserve:

- Authors can still edit/delete their own active content where currently allowed.
- Normal users cannot change `Post.status` through `PostUpdateSerializer`.
- Reports can still auto-hide posts using the existing threshold.

### 4.8 Help moderation endpoints

Update `backend/help_requests/views.py` and `backend/help_requests/urls.py`.

Routes to add:

- `GET /help-requests/moderation/`
- `DELETE /help-requests/moderation/<id>/`
- `DELETE /help-requests/moderation/comments/<id>/`
- `GET /help-offers/moderation/`
- `DELETE /help-offers/moderation/<id>/`

Authorization:

- All routes require `IsModeratorOrAdmin`.

Behavior:

- Moderator deletes reuse existing model deletion behavior and counter maintenance.
- Help-comment delete must decrement `comment_count` atomically.
- If soft deletion is preferred later, add status fields to `HelpRequest`, `HelpOffer`, and `HelpComment`; do not overload existing request lifecycle status for moderation state in this iteration unless the UI needs restore.

### 4.9 Expertise verification endpoints

Update `backend/accounts/views.py` and `backend/accounts/urls.py`.

Routes to add:

- `GET /staff/expertise-verifications/`
- `PATCH /staff/expertise-verifications/<id>/decision/`

Authorization:

- `IsVerificationCoordinatorOrAdmin`.

Behavior:

- Queue endpoint defaults to `verification_status=PENDING`; support filters for status, field, certification level, and user.
- Decision endpoint accepts:
  - `APPROVED`
  - `REJECTED`
  - `PENDING` for reopening
- Rejection requires a non-empty note.
- Approval may allow an optional note.
- Decision sets `reviewed_by=request.user` and `reviewed_at=timezone.now()` for approved/rejected records.
- Reopen clears reviewer/timestamp or keeps them in audit only; choose one behavior and document it in tests. Prefer clearing current reviewer fields and relying on audit for history.

### 4.10 Authentication payload and JWT considerations

Current login and `/me` responses return serialized user data. Add `staff_role` to those responses through `UserSerializer`.

Do not put authorization-critical staff role data only in JWT custom claims. The backend should authorize from the database user record on every request, so staff-role revocation takes effect without waiting for token expiry.

Clients may cache `staff_role` for UI display, but the API remains the source of truth.

## 5. Frontend Implementation

### 5.1 Auth state and route guards

Update `frontend/src/context/AuthContext.jsx` to keep `user.staff_role` from login and `/me`.

Update or extend `frontend/src/components/ProtectedRoute.jsx`:

- Existing `ProtectedRoute` remains for authenticated routes.
- Add `StaffRoute` or enhance `ProtectedRoute` with `allowedStaffRoles`.
- Unauthorized staff pages should redirect to dashboard or render a clear access-denied page.

Suggested helpers:

- `isAdmin(user)`
- `isModerator(user)`
- `isVerificationCoordinator(user)`
- `canModerate(user)`
- `canVerifyExpertise(user)`

### 5.2 API client additions

Update `frontend/src/services/api.js`.

Add methods for:

- Admin user list.
- Staff-role update.
- Account suspension/reactivation.
- Audit-log list.
- Admin hub create/update/delete.
- Forum moderation queue and decisions.
- Help moderation deletes.
- Expertise verification queue and decisions.

Keep request/response handling consistent with the current `request()` wrapper.

### 5.3 Web pages

Add pages only after backend authorization is complete.

Admin pages:

- Staff user management page.
- User detail/admin drawer or modal.
- Audit log page.
- Hub management page.

Moderator pages:

- Reported forum posts queue.
- Hidden/removed forum posts queue.
- Help content moderation queue or moderation actions embedded in help detail pages.

Verification pages:

- Pending expertise queue.
- Expertise decision page/modal with certification document link and decision note.
- Verification history view.

### 5.4 UI behavior

- Hide staff navigation for `staff_role=NONE`.
- Hide moderator controls unless `staff_role` is `MODERATOR` or `ADMIN`.
- Hide verification controls unless `staff_role` is `VERIFICATION_COORDINATOR` or `ADMIN`.
- Hide admin controls unless `staff_role` is `ADMIN`.
- Always handle `403` from the API, because UI gating is not authorization.
- Require a reason/note in destructive or rejecting workflows before submitting.

## 6. Android Implementation

Update Kotlin API models:

- Add `staffRole` to `UserData` in `AuthModels.kt` with `@SerializedName("staff_role")`.
- Add verification fields to `ExpertiseFieldData`.
- Add request/response models for staff APIs only if mobile will expose staff workflows.

Update Retrofit `ApiService.kt` if mobile staff screens are included:

- Admin user endpoints.
- Forum/help moderation endpoints.
- Expertise verification endpoints.

If mobile staff workflows are not in this milestone, still add `staff_role` and verification fields to response models so parsing remains compatible and the mobile app can display staff-aware UI later.

## 7. Migration and Rollout

### 7.1 Migrations

Create migrations for:

- `User.staff_role`.
- `ExpertiseField` verification fields.
- `StaffAuditLog`.

Backfill:

- Existing users get `staff_role=NONE`.
- Existing expertise fields get `verification_status=PENDING`.

### 7.2 Initial admin bootstrap

Add a controlled management command:

```bash
python manage.py promote_app_admin user@example.com
```

Rules:

- The command sets `staff_role=ADMIN`.
- It should print a clear success message.
- It should fail if the user does not exist.
- It should not change `is_staff` or `is_superuser`.

Use this command for the first production app admin after deployment.

### 7.3 Deployment order

1. Deploy backend model fields, serializers, permissions, and admin bootstrap command.
2. Run migrations.
3. Promote the first app admin.
4. Deploy backend staff endpoints.
5. Add frontend route guards and staff API client functions.
6. Add staff pages behind route guards.
7. Add Android model compatibility changes.
8. Enable staff workflows in production only after backend authorization tests pass.

## 8. Test Plan

### Backend tests

Add or extend tests in:

- `backend/accounts/tests.py`
- `backend/forum/tests.py`
- `backend/help_requests/tests.py`

Coverage:

- Registration always creates `staff_role=NONE`.
- Login and `/me` include `staff_role`.
- Request payloads cannot mutate `staff_role`, `is_staff`, `is_superuser`, or `is_active` through user-facing endpoints.
- `MODERATOR` can moderate forum/help content and cannot verify expertise or manage users.
- `VERIFICATION_COORDINATOR` can decide expertise verification and cannot moderate content or manage users.
- `ADMIN` can access all staff APIs.
- `STANDARD`, `EXPERT`, anonymous, and suspended users are denied from all staff APIs.
- Admin cannot demote or suspend the last active admin.
- Staff actions create audit logs in the same transaction.
- Non-staff users cannot view hidden/removed forum posts.
- Rejected expertise decisions require notes.
- Editing an expertise field resets verification status to `PENDING`.

### Frontend tests

Add tests near existing React tests.

Coverage:

- Staff route guards allow only the expected staff roles.
- Staff navigation is hidden for normal users.
- API client sends the correct methods and paths.
- `403` responses show an access-denied/error state.
- Verification rejection cannot be submitted without a note.

### Android tests

Add or update model parsing tests.

Coverage:

- `UserData` parses `staff_role`.
- Existing login and `/me` parsing remains backward compatible with the updated payload.
- Staff API models parse if mobile staff workflows are added.

## 9. Permission Matrix

| Capability | Standard/Expert | Moderator | Verification Coordinator | Admin |
|---|:---:|:---:|:---:|:---:|
| Create normal forum/help content | Yes | Yes | Yes | Yes |
| Edit/delete own forum/help content | Yes | Yes | Yes | Yes |
| View moderation queues | No | Yes | No | Yes |
| Hide/restore/remove forum posts | No | Yes | No | Yes |
| Delete any forum comment | No | Yes | No | Yes |
| Delete any help request/comment/offer | No | Yes | No | Yes |
| View expertise verification queue | No | No | Yes | Yes |
| Approve/reject/reopen expertise records | No | No | Yes | Yes |
| Assign/revoke staff roles | No | No | No | Yes |
| Suspend/reactivate users | No | No | No | Yes |
| Manage hubs | No | No | No | Yes |
| View audit logs | No | No | No | Yes |

## 10. Implementation Checklist

1. Add backend model fields and migrations.
2. Add permission helpers and DRF permission classes.
3. Update serializers and authenticated user payloads.
4. Add audit logging utility/model.
5. Add admin user/staff/hub/audit endpoints.
6. Add forum moderation endpoints and non-staff visibility restrictions.
7. Add help moderation endpoints.
8. Add expertise verification endpoints and reset-on-edit behavior.
9. Add backend tests for authorization and audit behavior.
10. Add frontend API client functions and staff route guards.
11. Add frontend staff pages and tests.
12. Add Android model compatibility updates and tests.
13. Add initial admin bootstrap command and document production usage.
