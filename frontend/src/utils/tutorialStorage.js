const POSTS_KEY = 'emergencyHubTutorialPosts';
const HELP_REQUESTS_KEY = 'emergencyHubTutorialHelpRequests';
const POST_COMMENTS_KEY = 'emergencyHubTutorialPostComments';
const POST_VOTES_KEY = 'emergencyHubTutorialPostVotes';
const HELP_COMMENTS_KEY = 'emergencyHubTutorialHelpComments';

function readList(key) {
  try {
    const value = window.sessionStorage.getItem(key);
    return value ? JSON.parse(value) : [];
  } catch {
    return [];
  }
}

function writeList(key, items) {
  window.sessionStorage.setItem(key, JSON.stringify(items));
}

function readObject(key) {
  try {
    const value = window.sessionStorage.getItem(key);
    return value ? JSON.parse(value) : {};
  } catch {
    return {};
  }
}

function writeObject(key, value) {
  window.sessionStorage.setItem(key, JSON.stringify(value));
}

export function getTutorialPosts() {
  return readList(POSTS_KEY);
}

export function saveTutorialPost(post) {
  const items = getTutorialPosts();
  const next = [{ ...post, id: `local-post-${Date.now()}` }, ...items];
  writeList(POSTS_KEY, next);
  return next[0];
}

export function getTutorialHelpRequests() {
  return readList(HELP_REQUESTS_KEY);
}

export function saveTutorialHelpRequest(request) {
  const items = getTutorialHelpRequests();
  const next = [{ ...request, id: `local-help-${Date.now()}` }, ...items];
  writeList(HELP_REQUESTS_KEY, next);
  return next[0];
}

export function updateTutorialHelpRequestStatus(requestId, status) {
  const items = getTutorialHelpRequests();
  const next = items.map((request) => (
    String(request.id) === String(requestId) ? { ...request, status } : request
  ));
  writeList(HELP_REQUESTS_KEY, next);
  return next.find((request) => String(request.id) === String(requestId));
}

export function deleteTutorialHelpRequest(requestId) {
  const items = getTutorialHelpRequests();
  const next = items.filter((request) => String(request.id) !== String(requestId));
  writeList(HELP_REQUESTS_KEY, next);
}

export function getTutorialPostComments() {
  return readObject(POST_COMMENTS_KEY);
}

export function addTutorialPostComment(postId, content) {
  const comments = getTutorialPostComments();
  const key = String(postId);
  const nextComment = {
    id: `local-comment-${Date.now()}`,
    author: 'You',
    content,
    createdLabel: 'just now',
    local: true,
  };
  const next = {
    ...comments,
    [key]: [nextComment, ...(comments[key] || [])],
  };
  writeObject(POST_COMMENTS_KEY, next);
  return next[key];
}

export function getTutorialPostVotes() {
  return readObject(POST_VOTES_KEY);
}

export function incrementTutorialPostVote(postId, type) {
  const votes = getTutorialPostVotes();
  const key = String(postId);
  const current = votes[key] || { upvotes: 0, downvotes: 0 };
  const next = {
    ...votes,
    [key]: {
      ...current,
      [type]: (current[type] || 0) + 1,
    },
  };
  writeObject(POST_VOTES_KEY, next);
  return next[key];
}

export function getTutorialHelpComments() {
  return readObject(HELP_COMMENTS_KEY);
}

export function addTutorialHelpComment(requestId, content) {
  const comments = getTutorialHelpComments();
  const key = String(requestId);
  const nextComment = {
    id: `local-help-comment-${Date.now()}`,
    author: 'You',
    content,
    createdLabel: 'just now',
    local: true,
  };
  const next = {
    ...comments,
    [key]: [nextComment, ...(comments[key] || [])],
  };
  writeObject(HELP_COMMENTS_KEY, next);
  return next[key];
}
