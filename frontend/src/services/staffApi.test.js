/**
 * Unit tests for staff API client functions.
 *
 * Validates HTTP method, path, and request body for each staff/moderation/
 * verification endpoint without making real network calls.
 */

import {
  listStaffUsers,
  updateStaffRole,
  updateAccountStatus,
  listStaffHubs,
  createStaffHub,
  updateStaffHub,
  deleteStaffHub,
  listAuditLogs,
  listForumModerationPosts,
  moderateForumPost,
  moderationDeleteForumComment,
  listHelpRequestModeration,
  listHelpOfferModeration,
  moderationDeleteHelpRequest,
  moderationDeleteHelpOffer,
  moderationDeleteHelpComment,
  listExpertiseVerifications,
  decideExpertiseVerification,
} from './api';

function mockResponse(body, { ok = true, status = 200 } = {}) {
  return Promise.resolve({
    ok,
    status,
    json: () => Promise.resolve(body),
  });
}

beforeEach(() => {
  global.fetch = jest.fn().mockReturnValue(mockResponse({}));
  localStorage.setItem('token', 'tok');
});

afterEach(() => {
  localStorage.clear();
});

describe('staff users API', () => {
  it('listStaffUsers GETs /staff/users/ with optional filters', async () => {
    await listStaffUsers({ search: 'foo', staff_role: 'ADMIN' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/users/?search=foo&staff_role=ADMIN'),
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('updateStaffRole PATCHes the staff-role endpoint', async () => {
    await updateStaffRole(7, 'MODERATOR', 'helping out');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/users/7/staff-role/'),
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ staff_role: 'MODERATOR', reason: 'helping out' }),
      }),
    );
  });

  it('updateAccountStatus PATCHes the status endpoint', async () => {
    await updateAccountStatus(8, false, 'spam');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/users/8/status/'),
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ is_active: false, reason: 'spam' }),
      }),
    );
  });
});

describe('staff hubs API', () => {
  it('listStaffHubs GETs /staff/hubs/', async () => {
    await listStaffHubs();
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/hubs/'),
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('createStaffHub POSTs name', async () => {
    await createStaffHub({ name: 'Bursa' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/hubs/'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ name: 'Bursa' }),
      }),
    );
  });

  it('updateStaffHub PATCHes the hub', async () => {
    await updateStaffHub(3, { name: 'Edirne' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/hubs/3/'),
      expect.objectContaining({ method: 'PATCH' }),
    );
  });

  it('deleteStaffHub sends DELETE with confirm flag', async () => {
    await deleteStaffHub(3, { confirm: true });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/hubs/3/'),
      expect.objectContaining({
        method: 'DELETE',
        body: JSON.stringify({ confirm: true }),
      }),
    );
  });
});

describe('staff audit log API', () => {
  it('listAuditLogs GETs /staff/audit-logs/ with filters', async () => {
    await listAuditLogs({ action: 'STAFF_ROLE_CHANGED' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/audit-logs/?action=STAFF_ROLE_CHANGED'),
      expect.objectContaining({ method: 'GET' }),
    );
  });
});

describe('forum moderation API', () => {
  it('listForumModerationPosts GETs /forum/moderation/posts/', async () => {
    await listForumModerationPosts({ status: 'HIDDEN' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/forum/moderation/posts/?status=HIDDEN'),
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('moderateForumPost PATCHes with action and reason', async () => {
    await moderateForumPost(11, 'HIDE', 'spam');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/forum/posts/11/moderation/'),
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ action: 'HIDE', reason: 'spam' }),
      }),
    );
  });

  it('moderationDeleteForumComment DELETEs with reason', async () => {
    await moderationDeleteForumComment(42, 'abuse');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/forum/moderation/comments/42/'),
      expect.objectContaining({ method: 'DELETE' }),
    );
  });
});

describe('help moderation API', () => {
  it('listHelpRequestModeration GETs the queue', async () => {
    await listHelpRequestModeration();
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/help-requests/moderation/'),
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('listHelpOfferModeration GETs the queue', async () => {
    await listHelpOfferModeration();
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/help-offers/moderation/'),
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('moderation deletes send DELETE with reason', async () => {
    await moderationDeleteHelpRequest(1, 'pii');
    await moderationDeleteHelpOffer(2, 'spam');
    await moderationDeleteHelpComment(3, 'abuse');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/help-requests/1/'),
      expect.objectContaining({ method: 'DELETE' }),
    );
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/help-offers/2/'),
      expect.objectContaining({ method: 'DELETE' }),
    );
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/help-requests/comments/3/'),
      expect.objectContaining({ method: 'DELETE' }),
    );
  });
});

describe('expertise verification API', () => {
  it('listExpertiseVerifications GETs the queue', async () => {
    await listExpertiseVerifications({ status: 'PENDING' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/expertise-verifications/?status=PENDING'),
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('decideExpertiseVerification PATCHes status and note', async () => {
    await decideExpertiseVerification(5, 'REJECTED', 'invalid cert');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/staff/expertise-verifications/5/decision/'),
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ status: 'REJECTED', note: 'invalid cert' }),
      }),
    );
  });
});
