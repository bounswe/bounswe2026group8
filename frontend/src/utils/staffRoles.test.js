/**
 * Unit tests for staff role helpers.
 *
 * These helpers gate UI elements; the backend still enforces real authorization.
 */

import {
  STAFF_ROLE,
  canModerate,
  canVerifyExpertise,
  hasAnyStaffRole,
  isAdmin,
  isModerator,
  isVerificationCoordinator,
  staffRoleLabel,
} from './staffRoles';

describe('staffRoles helpers', () => {
  it('admin satisfies all elevated checks', () => {
    const u = { staff_role: STAFF_ROLE.ADMIN };
    expect(isAdmin(u)).toBe(true);
    expect(canModerate(u)).toBe(true);
    expect(canVerifyExpertise(u)).toBe(true);
    expect(hasAnyStaffRole(u)).toBe(true);
  });

  it('moderator can moderate but not verify', () => {
    const u = { staff_role: STAFF_ROLE.MODERATOR };
    expect(isModerator(u)).toBe(true);
    expect(canModerate(u)).toBe(true);
    expect(canVerifyExpertise(u)).toBe(false);
    expect(isAdmin(u)).toBe(false);
  });

  it('verification coordinator can verify but not moderate', () => {
    const u = { staff_role: STAFF_ROLE.VERIFICATION_COORDINATOR };
    expect(isVerificationCoordinator(u)).toBe(true);
    expect(canVerifyExpertise(u)).toBe(true);
    expect(canModerate(u)).toBe(false);
    expect(isAdmin(u)).toBe(false);
  });

  it('NONE / undefined is denied everywhere', () => {
    const u = { staff_role: STAFF_ROLE.NONE };
    expect(canModerate(u)).toBe(false);
    expect(canVerifyExpertise(u)).toBe(false);
    expect(hasAnyStaffRole(u)).toBe(false);
    expect(canModerate(undefined)).toBe(false);
    expect(hasAnyStaffRole(null)).toBe(false);
  });

  it('staffRoleLabel returns user-facing labels', () => {
    expect(staffRoleLabel(STAFF_ROLE.ADMIN)).toBe('Admin');
    expect(staffRoleLabel(STAFF_ROLE.MODERATOR)).toBe('Moderator');
    expect(staffRoleLabel(STAFF_ROLE.VERIFICATION_COORDINATOR)).toBe('Verification Coordinator');
    expect(staffRoleLabel(STAFF_ROLE.NONE)).toBeNull();
  });
});
