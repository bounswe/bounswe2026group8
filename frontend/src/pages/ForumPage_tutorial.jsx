import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useTutorialGuide from '../components/TutorialGuide';
import { getAllTutorialPosts } from '../utils/tutorialForumData';
import { getTutorialPostComments, getTutorialPostVotes, incrementTutorialPostVote } from '../utils/tutorialStorage';

const FORUM_TOUR_STEPS = [
  {
    target: 'tabs',
    title: 'Choose the feed',
    text: 'Forum tabs separate broad public updates from hub-specific and urgent conversations.',
  },
  {
    target: 'sort',
    title: 'Sort the updates',
    text: 'Use Newest, Most liked, and Hot to scan recent updates, popular posts, and active conversations.',
  },
  {
    target: 'posts',
    title: 'Open a post',
    text: 'Click a post to inspect the full update and join the conversation.',
  },
  {
    target: 'create',
    title: 'Create a post',
    text: 'Use the new post button to write a community update.',
  },
  {
    target: 'next',
    title: 'Know when to request help',
    text: 'Forum posts are public updates. If someone needs water, shelter, transport, or medical support, use a help request.',
  },
];

function hotScore(post) {
  return (post.upvotes || 0) + (post.downvotes || 0) + (post.comments || 0);
}

export default function ForumPageTutorial() {
  const navigate = useNavigate();
  const [sortBy, setSortBy] = useState('newest');
  const [allPosts] = useState(() => getAllTutorialPosts());
  const [voteOverrides, setVoteOverrides] = useState(() => getTutorialPostVotes());
  const [commentOverrides] = useState(() => getTutorialPostComments());
  const { activeStep, GuidePanel, RestartButton } = useTutorialGuide({
    storageKey: 'emergencyHubForumTutorialSeen',
    steps: FORUM_TOUR_STEPS,
  });

  const posts = useMemo(() => {
    const copy = allPosts.map((post) => ({
      ...post,
      upvotes: (post.upvotes || 0) + (voteOverrides[String(post.id)]?.upvotes || 0),
      downvotes: (post.downvotes || 0) + (voteOverrides[String(post.id)]?.downvotes || 0),
      comments: (post.comments || 0) + (commentOverrides[String(post.id)]?.length || 0),
    }));
    if (sortBy === 'most_liked') copy.sort((a, b) => b.upvotes - a.upvotes);
    if (sortBy === 'hot') copy.sort((a, b) => hotScore(b) - hotScore(a));
    return copy;
  }, [allPosts, commentOverrides, sortBy, voteOverrides]);

  const handleVote = (postId, type) => {
    const nextVotes = incrementTutorialPostVote(postId, type);
    setVoteOverrides((current) => ({ ...current, [String(postId)]: nextVotes }));
  };

  return (
    <div className="forum-layout tutorial-page">
      <div className="forum-top">
        <header className="forum-header page-main-header">
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/tutorial')}>
            &larr; Dashboard
          </button>
          <div className="forum-header-title">
            <h1 className="gradient-text">Community Forum</h1>
            <p className="forum-hub-label">Community updates from neighbors in your area.</p>
          </div>
          <div className="tutorial-header-actions">
            {RestartButton}
            <button
              className={`btn btn-primary btn-sm ${activeStep?.target === 'create' ? 'tutorial-tour-highlight' : ''}`}
              onClick={() => navigate('/tutorial/forum/new')}
            >
              New post
            </button>
          </div>
        </header>

        {GuidePanel}

        <div className={`forum-tabs ${activeStep?.target === 'tabs' ? 'tutorial-tour-highlight' : ''}`}>
          <button className="forum-tab forum-tab--active forum-tab--global">Global</button>
          <button className="forum-tab" disabled>Standard hub</button>
          <button className="forum-tab" disabled>Urgent hub</button>
        </div>

        <div className={`forum-sort-bar ${activeStep?.target === 'sort' ? 'tutorial-tour-highlight' : ''}`}>
          {['newest', 'most_liked', 'hot'].map((s) => (
            <button
              key={s}
              className={`forum-sort-btn ${sortBy === s ? 'forum-sort-btn--active' : ''}`}
              onClick={() => setSortBy(s)}
            >
              {s === 'newest' ? 'Newest' : s === 'most_liked' ? 'Most liked' : 'Hot'}
            </button>
          ))}
        </div>
      </div>

      <div className="forum-scroll">
        <div className={`post-list ${activeStep?.target === 'posts' ? 'tutorial-tour-highlight' : ''}`}>
          {posts.map((post) => (
            <article
              className="post-card tutorial-post-card"
              key={post.id}
            >
              <div className="post-card-votes">
                <button
                  className="vote-btn-mini vote-up"
                  onClick={() => handleVote(post.id, 'upvotes')}
                >
                  &#9650; {post.upvotes}
                </button>
                <button
                  className="vote-btn-mini vote-down"
                  onClick={() => handleVote(post.id, 'downvotes')}
                >
                  &#9660; {post.downvotes}
                </button>
              </div>

              <div className="post-card-body" onClick={() => navigate(`/tutorial/forum/posts/${post.id}`)}>
                <h3 className="post-card-title">{post.title}</h3>
                <p className="tutorial-post-body">{post.body}</p>
                <div className="post-card-meta">
                  <span className="post-card-author">{post.author}</span>
                  {post.local && <span className="badge badge-accent">Your post</span>}
                  {post.role === 'EXPERT' && <span className="badge badge-expert-responding">Expert</span>}
                  <span className="badge" style={{ color: '#38bdf8', borderColor: '#38bdf844', background: '#38bdf811' }}>
                    {post.status}
                  </span>
                  <span className="post-card-dot">&middot;</span>
                  <span className="post-card-time">{post.createdLabel || 'recent update'}</span>
                </div>
              </div>

              <div className="post-card-stats">
                <span className="post-stat">{post.comments} comments</span>
                <button className="share-btn" onClick={() => navigate(`/tutorial/forum/posts/${post.id}`)}>
                  Open post
                </button>
                <button className="share-btn" disabled>View profile after sign in</button>
              </div>
            </article>
          ))}
        </div>

        <div className={`tutorial-next-panel ${activeStep?.target === 'next' ? 'tutorial-tour-highlight' : ''}`}>
          <h2>Next step</h2>
          <p>
            In a real emergency, you can use forum posts for public updates. When you personally
            need supplies, transport, shelter, or medical help, create a help request instead.
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/tutorial/help-requests/new')}>
            Create a help request
          </button>
        </div>
      </div>
    </div>
  );
}
