import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useTutorialGuide from '../components/TutorialGuide';
import { getAllTutorialPosts } from '../utils/tutorialForumData';
import { getTutorialPostComments, getTutorialPostVotes, incrementTutorialPostVote } from '../utils/tutorialStorage';

function hotScore(post) {
  return (post.upvotes || 0) + (post.downvotes || 0) + (post.comments || 0);
}

export default function ForumPageTutorial() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const FORUM_TOUR_STEPS = [
    { target: 'tabs', title: t('tutorial.forum.steps.feedTitle'), text: t('tutorial.forum.steps.feedText') },
    { target: 'sort', title: t('tutorial.forum.steps.sortTitle'), text: t('tutorial.forum.steps.sortText') },
    { target: 'posts', title: t('tutorial.forum.steps.openTitle'), text: t('tutorial.forum.steps.openText') },
    { target: 'create', title: t('tutorial.forum.steps.createTitle'), text: t('tutorial.forum.steps.createText') },
    { target: 'next', title: t('tutorial.forum.steps.requestTitle'), text: t('tutorial.forum.steps.requestText') },
  ];
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
            {t('tutorial.common.backDashboard')}
          </button>
          <div className="forum-header-title">
            <h1 className="gradient-text">{t('tutorial.forum.title')}</h1>
            <p className="forum-hub-label">{t('tutorial.forum.subtitle')}</p>
          </div>
          <div className="tutorial-header-actions">
            {RestartButton}
            <button
              className={`btn btn-primary btn-sm ${activeStep?.target === 'create' ? 'tutorial-tour-highlight' : ''}`}
              onClick={() => navigate('/tutorial/forum/new')}
            >
              {t('tutorial.forum.newPost')}
            </button>
          </div>
        </header>

        {GuidePanel}

        <div className={`forum-tabs ${activeStep?.target === 'tabs' ? 'tutorial-tour-highlight' : ''}`}>
          <button className="forum-tab forum-tab--active forum-tab--global">{t('forum.tabs.global')}</button>
          <button className="forum-tab" disabled>{t('tutorial.forum.standardHub')}</button>
          <button className="forum-tab" disabled>{t('tutorial.forum.urgentHub')}</button>
        </div>

        <div className={`forum-sort-bar ${activeStep?.target === 'sort' ? 'tutorial-tour-highlight' : ''}`}>
          {['newest', 'most_liked', 'hot'].map((s) => (
            <button
              key={s}
              className={`forum-sort-btn ${sortBy === s ? 'forum-sort-btn--active' : ''}`}
              onClick={() => setSortBy(s)}
            >
              {s === 'newest' ? t('forum.sort.newest') : s === 'most_liked' ? t('tutorial.forum.mostLiked') : t('forum.sort.hot')}
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
                  {post.local && <span className="badge badge-accent">{t('tutorial.common.yourPost')}</span>}
                  {post.role === 'EXPERT' && <span className="badge badge-expert-responding">{t('tutorial.common.expert')}</span>}
                  <span className="badge" style={{ color: '#38bdf8', borderColor: '#38bdf844', background: '#38bdf811' }}>
                    {post.status}
                  </span>
                  <span className="post-card-dot">&middot;</span>
                  <span className="post-card-time">{post.createdLabel || t('tutorial.common.recentUpdate')}</span>
                </div>
              </div>

              <div className="post-card-stats">
                <span className="post-stat">{t('tutorial.common.commentsCount', { count: post.comments })}</span>
                <button className="share-btn" onClick={() => navigate(`/tutorial/forum/posts/${post.id}`)}>
                  {t('tutorial.common.openPost')}
                </button>
                <button className="share-btn" disabled>{t('tutorial.common.viewProfileAfterSignIn')}</button>
              </div>
            </article>
          ))}
        </div>

        <div className={`tutorial-next-panel ${activeStep?.target === 'next' ? 'tutorial-tour-highlight' : ''}`}>
          <h2>{t('tutorial.common.nextStep')}</h2>
          <p>{t('tutorial.forum.nextBody')}</p>
          <button className="btn btn-primary" onClick={() => navigate('/tutorial/help-requests/new')}>
            {t('tutorial.forum.createHelpRequest')}
          </button>
        </div>
      </div>
    </div>
  );
}
