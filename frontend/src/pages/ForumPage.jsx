import { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { useNavigate, useLocation, useSearchParams, Link } from 'react-router-dom';
import { getPosts, vote, repost, resolveImageUrl } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

const STATUS_COLORS = { SAFE: '#34d399', NEEDS_HELP: '#f87171', AVAILABLE_TO_HELP: '#38bdf8' };

// Pass 't' into the helper component
function AuthorStatus({ profile, t }) {
  const s = profile?.availability_status;
  if (!s || !STATUS_COLORS[s]) return null;
  const c = STATUS_COLORS[s];

  // Map the status to the translation JSON
  const STATUS_LABELS = {
    SAFE: t('forum.status.safe'),
    NEEDS_HELP: t('forum.status.needs_help'),
    AVAILABLE_TO_HELP: t('forum.status.available_to_help')
  };

  return <span className="badge" style={{ color: c, borderColor: c + '44', background: c + '11', fontSize: '0.7rem', padding: '1px 6px' }}>● {STATUS_LABELS[s]}</span>;
}

// Pass 't' into the time function
function timeAgo(dateStr, t) {
  const seconds = Math.floor((Date.now() - new Date(dateStr)) / 1000);
  if (seconds < 60) return t('forum.time.just_now');
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return t('forum.time.m_ago', { count: minutes });
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return t('forum.time.h_ago', { count: hours });
  const days = Math.floor(hours / 24);
  return t('forum.time.d_ago', { count: days });
}

function hotScore(post) {
  const ageMs = Date.now() - new Date(post.created_at).getTime();
  const TWO_DAYS = 2 * 24 * 60 * 60 * 1000;
  if (ageMs > TWO_DAYS) return -1;
  const activity = post.upvote_count + post.downvote_count + post.comment_count;
  return activity / (1 + ageMs / 3600000);
}

const FORUM_TABS = ['GLOBAL', 'STANDARD', 'URGENT'];

export default function ForumPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user, isAuthenticated } = useAuth();
  const { t } = useTranslation(); // 4. Initialize Hook
  const selectedHub = user?.hub;

  const tabParam = searchParams.get('tab')?.toUpperCase();
  const tab = FORUM_TABS.includes(tabParam) ? tabParam
      : FORUM_TABS.includes(location.state?.forumTab) ? location.state.forumTab
          : 'GLOBAL';
  const setTab = (t) => setSearchParams({ tab: t }, { replace: true });
  const [sortBy, setSortBy] = useState('newest');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [copiedId, setCopiedId] = useState(null);
  const scrollRef = useRef(null);
  const lastScrollTop = useRef(0);
  const [showScrollTop, setShowScrollTop] = useState(false);

  const saveScroll = useCallback(() => {
    if (scrollRef.current) {
      sessionStorage.setItem('forum_scroll', String(scrollRef.current.scrollTop));
    }
  }, []);

  const handleScroll = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    const top = el.scrollTop;
    const scrollingUp = top < lastScrollTop.current;
    setShowScrollTop(scrollingUp && top > 200);
    lastScrollTop.current = top;
  }, []);

  const scrollToTop = () => {
    scrollRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
  };

  useEffect(() => {
    setLoading(true);
    const roleFilterParam = roleFilter !== 'ALL' ? roleFilter : null;
    if (tab === 'GLOBAL') {
      getPosts({ forumType: 'GLOBAL', authorRole: roleFilterParam }).then(({ ok, data }) => {
        if (ok) setPosts(data);
        setLoading(false);
      });
      return;
    }
    if (!selectedHub) {
      setPosts([]);
      setLoading(false);
      return;
    }
    getPosts({ hub: selectedHub.id, forumType: tab, authorRole: roleFilterParam }).then(({ ok, data }) => {
      if (ok) setPosts(data);
      setLoading(false);
    });
  }, [user, tab, roleFilter]);

  useEffect(() => {
    if (!loading && scrollRef.current) {
      const saved = sessionStorage.getItem('forum_scroll');
      if (saved) {
        scrollRef.current.scrollTop = Number(saved);
        sessionStorage.removeItem('forum_scroll');
      }
    }
  }, [loading]);

  const sortedPosts = useMemo(() => {
    const copy = [...posts];
    if (sortBy === 'newest') {
      copy.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
    } else if (sortBy === 'most_liked') {
      copy.sort((a, b) => b.upvote_count - a.upvote_count);
    } else if (sortBy === 'hot') {
      copy.sort((a, b) => hotScore(b) - hotScore(a));
    }
    return copy;
  }, [posts, sortBy]);

  const handleVote = async (postId, type) => {
    // ... logic remains identical
    const idx = posts.findIndex((p) => p.id === postId);
    if (idx === -1) return;
    const post = posts[idx];
    const prev = [...posts];

    let newUp = post.upvote_count;
    let newDown = post.downvote_count;
    let newUserVote = type;

    if (post.user_vote === type) {
      if (type === 'UP') newUp--; else newDown--;
      newUserVote = null;
    } else if (post.user_vote) {
      if (post.user_vote === 'UP') { newUp--; newDown++; }
      else { newDown--; newUp++; }
    } else {
      if (type === 'UP') newUp++; else newDown++;
    }

    const updated = [...posts];
    updated[idx] = { ...post, upvote_count: newUp, downvote_count: newDown, user_vote: newUserVote };
    setPosts(updated);

    const { ok } = await vote(postId, type);
    if (!ok) setPosts(prev);
  };

  const handleShare = (postId) => {
    const url = `${window.location.origin}/forum/posts/${postId}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopiedId(postId);
      setTimeout(() => setCopiedId(null), 1500);
    });
  };

  const handleRepost = async (postId) => {
    const { ok, data } = await repost(postId, tab === 'GLOBAL' ? null : selectedHub?.id);
    if (ok) {
      setPosts((prev) => {
        const updated = prev.map((p) =>
            p.id === postId
                ? { ...p, user_has_reposted: true, repost_count: p.repost_count + 1 }
                : p,
        );
        return [data, ...updated];
      });
    }
  };

  return (
      <div className="forum-layout">
        <div className="forum-top">
          <header className="forum-header">
            <div>
              <h1 className="gradient-text">{t('forum.header.title')}</h1>
              <p className="forum-hub-label">
                {tab === 'GLOBAL' ? t('forum.header.global_hub_label') : (selectedHub?.name || t('forum.header.select_hub_label'))}
              </p>
            </div>
            {isAuthenticated && (
                <button className="btn btn-primary btn-sm" onClick={() => { saveScroll(); navigate('/forum/new', { state: { forumType: tab } }); }}>
                  {t('forum.header.btn_new_post')}
                </button>
            )}
          </header>

          <div className="forum-tabs">
            <button
                className={`forum-tab ${tab === 'GLOBAL' ? 'forum-tab--active forum-tab--global' : ''}`}
                onClick={() => setTab('GLOBAL')}
            >
              {t('forum.tabs.global')}
            </button>
            <button
                className={`forum-tab ${tab === 'STANDARD' ? 'forum-tab--active' : ''}`}
                onClick={() => setTab('STANDARD')}
            >
              {t('forum.tabs.standard')}
            </button>
            <button
                className={`forum-tab ${tab === 'URGENT' ? 'forum-tab--active forum-tab--urgent' : ''}`}
                onClick={() => setTab('URGENT')}
            >
              {t('forum.tabs.urgent')}
            </button>
          </div>

          <div className="forum-sort-bar">
            {['newest', 'most_liked', 'hot'].map((s) => (
                <button
                    key={s}
                    className={`forum-sort-btn ${sortBy === s ? 'forum-sort-btn--active' : ''}`}
                    onClick={() => setSortBy(s)}
                >
                  {s === 'newest' ? t('forum.sort.newest') : s === 'most_liked' ? t('forum.sort.most_liked') : t('forum.sort.hot')}
                </button>
            ))}
          </div>

          <div className="forum-role-filter">
            {['All', 'Experts', 'Standard Users'].map((role) => (
                <button
                    key={role}
                    className={`forum-role-btn ${roleFilter === role ? 'forum-role-btn--active' : ''}`}
                    onClick={() => setRoleFilter(role)}
                >
                  {role === 'All' ? t('forum.filter.all') : role === 'Experts' ? t('forum.filter.experts') : t('forum.filter.standard_users')}
                </button>
            ))}
          </div>
        </div>

        <div className="forum-scroll" ref={scrollRef} onScroll={handleScroll}>
          {loading ? (
              <p className="forum-empty">{t('forum.empty_states.loading')}</p>
          ) : tab !== 'GLOBAL' && !selectedHub ? (
              <p className="forum-empty">{t('forum.empty_states.select_hub', { tab: tab.toLowerCase() })}</p>
          ) : sortedPosts.length === 0 ? (
              <p className="forum-empty">
                {tab === 'GLOBAL'
                    ? t('forum.empty_states.no_global')
                    : t('forum.empty_states.no_hub_posts', { tab: tab.toLowerCase(), hub: selectedHub?.name })}
              </p>
          ) : (
              <div className="post-list">
                {sortedPosts.map((post) => (
                    <div className="post-card" key={post.id}>
                      <div className="post-card-votes">
                        <button
                            className={`vote-btn-mini vote-up ${post.user_vote === 'UP' ? 'vote-active' : ''}`}
                            disabled={!isAuthenticated}
                            onClick={(e) => { e.stopPropagation(); handleVote(post.id, 'UP'); }}
                            title={t('forum.post.upvote')}
                        >&#9650; {post.upvote_count}</button>
                        <button
                            className={`vote-btn-mini vote-down ${post.user_vote === 'DOWN' ? 'vote-active' : ''}`}
                            disabled={!isAuthenticated}
                            onClick={(e) => { e.stopPropagation(); handleVote(post.id, 'DOWN'); }}
                            title={t('forum.post.downvote')}
                        >&#9660; {post.downvote_count}</button>
                      </div>
                      <Link to={`/forum/posts/${post.id}`} className="post-card-body" onClick={saveScroll}>
                        {post.reposted_from && (
                            <span className="repost-label">{t('forum.post.reposted_from', { name: post.reposted_from.author.full_name })}</span>
                        )}
                        <h3 className="post-card-title">{post.title}</h3>
                        {post.image_urls && post.image_urls.length > 0 && (
                            <div className={`post-card-images ${post.image_urls.length === 1 ? 'post-card-images--single' : 'post-card-images--multi'}`}>
                              {post.image_urls.slice(0, 3).map((url, i) => (
                                  <img key={i} src={resolveImageUrl(url)} alt="" className="post-card-thumb" />
                              ))}
                              {post.image_urls.length > 3 && (
                                  <span className="post-card-more-images">+{post.image_urls.length - 3}</span>
                              )}
                            </div>
                        )}
                        <div className="post-card-meta">
                          <Link to={`/users/${post.author.id}`} className="post-card-author author-link" onClick={(e) => e.stopPropagation()}>{post.author.full_name}</Link>
                          {post.author.role === 'EXPERT' && <span className="badge badge-expert-responding">{t('forum.post.expert')}</span>}
                          <AuthorStatus profile={post.author.profile} t={t} />
                          <span className="post-card-dot">&middot;</span>
                          <span className="post-card-time">{timeAgo(post.created_at, t)}</span>
                        </div>
                      </Link>
                      <div className="post-card-stats">
                <span className="post-stat" title="Comments">
                  {post.comment_count === 1 ? t('forum.post.comment_single', { count: post.comment_count }) : t('forum.post.comment_plural', { count: post.comment_count })}
                </span>
                        {!post.reposted_from && post.repost_count > 0 && (
                            <span className="post-stat post-stat-repost" title="Reposts">
                    {post.repost_count === 1 ? t('forum.post.repost_single', { count: post.repost_count }) : t('forum.post.repost_plural', { count: post.repost_count })}
                  </span>
                        )}
                        <button
                            className="share-btn"
                            onClick={(e) => { e.stopPropagation(); handleShare(post.id); }}
                            title={t('forum.post.copy_link')}
                        >
                          {copiedId === post.id ? t('forum.post.copied') : t('forum.post.share')}
                        </button>
                        {isAuthenticated && (!user || user.id !== post.author.id) && !post.user_has_reposted && (
                            <button
                                className="share-btn"
                                onClick={(e) => { e.stopPropagation(); handleRepost(post.id); }}
                                title={tab === 'GLOBAL' ? t('forum.post.repost_global') : t('forum.post.repost_hub')}
                            >
                              {t('forum.post.repost')}
                            </button>
                        )}
                      </div>
                    </div>
                ))}
              </div>
          )}
        </div>

        {showScrollTop && (
            <button className="scroll-to-top" onClick={scrollToTop}>
              &#9650; {t('forum.actions.back_to_top')}
            </button>
        )}

        <div className="forum-bottom">
          <Link to="/dashboard" className="link">&larr; {t('forum.actions.back_to_dashboard')}</Link>
        </div>
      </div>
  );
}