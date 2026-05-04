import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const POSTS = [
  {
    id: 1,
    title: 'Power is out near Barbaros Boulevard',
    author: 'Demo Neighbor',
    role: 'STANDARD',
    status: 'Safe',
    comments: 4,
    upvotes: 12,
    body: 'Several buildings are affected. Elevators are not working, and residents are checking on older neighbors.',
  },
  {
    id: 2,
    title: 'Volunteer list for charging phones',
    author: 'Demo Expert',
    role: 'EXPERT',
    status: 'Available to help',
    comments: 7,
    upvotes: 19,
    body: 'A small charging station is being organized at the community center. Bring your own cable if possible.',
  },
  {
    id: 3,
    title: 'Reminder: avoid downed cables',
    author: 'Safety Moderator',
    role: 'EXPERT',
    status: 'Available to help',
    comments: 2,
    upvotes: 24,
    body: 'If you see damaged electrical lines, keep distance and report the exact location to emergency services.',
  },
];

export default function ForumPageTutorial() {
  const navigate = useNavigate();
  const [sortBy, setSortBy] = useState('newest');

  const posts = useMemo(() => {
    const copy = [...POSTS];
    if (sortBy === 'most_liked') copy.sort((a, b) => b.upvotes - a.upvotes);
    return copy;
  }, [sortBy]);

  return (
    <div className="forum-layout tutorial-page">
      <div className="forum-top">
        <header className="forum-header page-main-header">
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/tutorial')}>
            &larr; Tutorial dashboard
          </button>
          <div className="forum-header-title">
            <h1 className="gradient-text">Community Forum Preview</h1>
            <p className="forum-hub-label">A short practice feed with example updates from neighbors.</p>
          </div>
          <button className="btn btn-secondary btn-sm" disabled>
            New post after sign in
          </button>
        </header>

        <div className="forum-tabs">
          <button className="forum-tab forum-tab--active forum-tab--global">Global</button>
          <button className="forum-tab" disabled>Standard hub</button>
          <button className="forum-tab" disabled>Urgent hub</button>
        </div>

        <div className="forum-sort-bar">
          {['newest', 'most_liked'].map((s) => (
            <button
              key={s}
              className={`forum-sort-btn ${sortBy === s ? 'forum-sort-btn--active' : ''}`}
              onClick={() => setSortBy(s)}
            >
              {s === 'newest' ? 'Newest' : 'Most liked'}
            </button>
          ))}
        </div>
      </div>

      <div className="forum-scroll">
        <div className="post-list">
          {posts.map((post) => (
            <article className="post-card tutorial-post-card" key={post.id}>
              <div className="post-card-votes">
                <button className="vote-btn-mini vote-up" disabled>&#9650; {post.upvotes}</button>
                <button className="vote-btn-mini vote-down" disabled>&#9660; 0</button>
              </div>

              <div className="post-card-body">
                <h3 className="post-card-title">{post.title}</h3>
                <p className="tutorial-post-body">{post.body}</p>
                <div className="post-card-meta">
                  <span className="post-card-author">{post.author}</span>
                  {post.role === 'EXPERT' && <span className="badge badge-expert-responding">Expert</span>}
                  <span className="badge" style={{ color: '#38bdf8', borderColor: '#38bdf844', background: '#38bdf811' }}>
                    {post.status}
                  </span>
                  <span className="post-card-dot">&middot;</span>
                  <span className="post-card-time">sample scenario</span>
                </div>
              </div>

              <div className="post-card-stats">
                <span className="post-stat">{post.comments} sample comments</span>
                <button className="share-btn" disabled>Comment after sign in</button>
                <button className="share-btn" disabled>View profile after sign in</button>
              </div>
            </article>
          ))}
        </div>

        <div className="tutorial-next-panel">
          <h2>Next step</h2>
          <p>
            In a real emergency, you can use forum posts for public updates. When you personally
            need supplies, transport, shelter, or medical help, create a help request instead.
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/tutorial/help-requests/new')}>
            Practice creating a help request
          </button>
        </div>
      </div>
    </div>
  );
}
