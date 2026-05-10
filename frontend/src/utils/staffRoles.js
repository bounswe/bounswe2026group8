/**
 * Helpers for application staff-role checks on the frontend.
 *
 * Backend authorization is the source of truth; these helpers exist solely
 * for UI gating (showing/hiding controls and routes).
 */

export const STAFF_ROLE = Object.freeze({
  NONE: 'NONE',
  MODERATOR: 'MODERATOR',
  VERIFICATION_COORDINATOR: 'VERIFICATION_COORDINATOR',
  ADMIN: 'ADMIN',
});

export function isAdmin(user) {
  return Boolean(user) && user.staff_role === STAFF_ROLE.ADMIN;
}

export function isModerator(user) {
  return Boolean(user) && user.staff_role === STAFF_ROLE.MODERATOR;
}

export function isVerificationCoordinator(user) {
  return Boolean(user) && user.staff_role === STAFF_ROLE.VERIFICATION_COORDINATOR;
}

export function canModerate(user) {
  return isAdmin(user) || isModerator(user);
}

export function canVerifyExpertise(user) {
  return isAdmin(user) || isVerificationCoordinator(user);
}

export function hasAnyStaffRole(user) {
  return Boolean(user) && user.staff_role && user.staff_role !== STAFF_ROLE.NONE;
}

export function staffRoleLabel(role) {
  switch (role) {
    case STAFF_ROLE.ADMIN:
      return 'Admin';
    case STAFF_ROLE.MODERATOR:
      return 'Moderator';
    case STAFF_ROLE.VERIFICATION_COORDINATOR:
      return 'Verification Coordinator';
    default:
      return null;
  }
}
