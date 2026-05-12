import {
  addTutorialHelpComment,
  addTutorialPostComment,
  deleteTutorialHelpRequest,
  getTutorialHelpComments,
  getTutorialHelpRequests,
  getTutorialPostComments,
  getTutorialPosts,
  getTutorialPostVotes,
  incrementTutorialPostVote,
  saveTutorialHelpRequest,
  saveTutorialPost,
  updateTutorialHelpRequestStatus,
} from './tutorialStorage';

describe('tutorialStorage', () => {
  beforeEach(() => {
    sessionStorage.clear();
    jest.spyOn(Date, 'now').mockReturnValue(1700000000000);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('returns empty collections when storage is empty', () => {
    expect(getTutorialPosts()).toEqual([]);
    expect(getTutorialHelpRequests()).toEqual([]);
    expect(getTutorialPostComments()).toEqual({});
    expect(getTutorialPostVotes()).toEqual({});
    expect(getTutorialHelpComments()).toEqual({});
  });

  it('falls back safely when stored tutorial data is malformed', () => {
    sessionStorage.setItem('emergencyHubTutorialPosts', '{bad-json');
    sessionStorage.setItem('emergencyHubTutorialPostVotes', '{bad-json');

    expect(getTutorialPosts()).toEqual([]);
    expect(getTutorialPostVotes()).toEqual({});
  });

  it('saves new posts before existing posts with generated local ids', () => {
    sessionStorage.setItem('emergencyHubTutorialPosts', JSON.stringify([{ id: 'older', title: 'Older post' }]));

    const saved = saveTutorialPost({ title: 'Fresh update', body: 'Useful details' });

    expect(saved).toMatchObject({
      id: 'local-post-1700000000000',
      title: 'Fresh update',
      body: 'Useful details',
    });
    expect(getTutorialPosts()).toEqual([
      saved,
      { id: 'older', title: 'Older post' },
    ]);
  });

  it('saves, updates, and deletes tutorial help requests', () => {
    const saved = saveTutorialHelpRequest({ title: 'Need water', status: 'Open' });

    expect(saved).toMatchObject({
      id: 'local-help-1700000000000',
      title: 'Need water',
      status: 'Open',
    });

    const updated = updateTutorialHelpRequestStatus(saved.id, 'Resolved');
    expect(updated).toMatchObject({ id: saved.id, status: 'Resolved' });
    expect(getTutorialHelpRequests()).toHaveLength(1);

    deleteTutorialHelpRequest(saved.id);
    expect(getTutorialHelpRequests()).toEqual([]);
  });

  it('returns undefined when updating a missing help request but preserves storage', () => {
    sessionStorage.setItem('emergencyHubTutorialHelpRequests', JSON.stringify([{ id: 'known', status: 'Open' }]));

    const updated = updateTutorialHelpRequestStatus('missing', 'Resolved');

    expect(updated).toBeUndefined();
    expect(getTutorialHelpRequests()).toEqual([{ id: 'known', status: 'Open' }]);
  });

  it('adds newest post comments first and keeps comments grouped by post id', () => {
    jest.spyOn(Date, 'now').mockReturnValueOnce(1700000000000).mockReturnValueOnce(1700000000001);

    addTutorialPostComment('post-1', 'First comment');
    const comments = addTutorialPostComment('post-1', 'Second comment');

    expect(comments.map((comment) => comment.content)).toEqual(['Second comment', 'First comment']);
    expect(getTutorialPostComments()['post-1'][0]).toMatchObject({
      id: 'local-comment-1700000000001',
      author: 'You',
      local: true,
    });
  });

  it('increments tutorial post votes independently by type', () => {
    const upvotes = incrementTutorialPostVote('post-1', 'upvotes');
    const downvotes = incrementTutorialPostVote('post-1', 'downvotes');

    expect(upvotes).toEqual({ upvotes: 1, downvotes: 0 });
    expect(downvotes).toEqual({ upvotes: 1, downvotes: 1 });
    expect(getTutorialPostVotes()).toEqual({
      'post-1': { upvotes: 1, downvotes: 1 },
    });
  });

  it('adds newest help comments first and keeps comments grouped by request id', () => {
    jest.spyOn(Date, 'now').mockReturnValueOnce(1700000000000).mockReturnValueOnce(1700000000001);

    addTutorialHelpComment('help-1', 'I can help');
    const comments = addTutorialHelpComment('help-1', 'On my way');

    expect(comments.map((comment) => comment.content)).toEqual(['On my way', 'I can help']);
    expect(getTutorialHelpComments()['help-1'][0]).toMatchObject({
      id: 'local-help-comment-1700000000001',
      author: 'You',
      local: true,
    });
  });
});
