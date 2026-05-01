import { useState, useEffect } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getPosts, getHelpRequests, getHelpOffers, resolveImageUrl } from '../services/api';
import { useTranslation } from 'react-i18next';

const TABS = ['posts', 'requests', 'offers'];

function timeAgo(dateStr, t) {
  const seconds = Math.floor((Date.now() - new Date(dateStr)) / 1000);
  if (seconds < 60) return t('my_posts.time.just_now');
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return t('my_posts.time.m_ago', { count: minutes });
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return t('my_posts.time.h_ago', { count: hours });
  const days = Math.floor(hours / 24);
  return t('my_posts.time.d_ago', { count: days });
}

export default function MyPostsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { t } = useTranslation();

  const FORUM_TYPE_STYLE = {
    GLOBAL: { label: t('my_posts.forum_types.global'), color: '#a78bfa', bg: '#a78bfa18' },
    STANDARD: { label: t('my_posts.forum_types.standard'), color: '#38bdf8', bg: '#38bdf818' },
    URGENT: { label: t('my_posts.forum_types.urgent'), color: '#f87171', bg: '#f8717118' },
  };

  const URGENCY_STYLE = {
    LOW: { label: t('my_posts.urgency.low'), color: '#34d399' },
    MEDIUM: { label: t('my_posts.urgency.medium'), color: '#fbbf24' },
    HIGH: { label: t('my_posts.urgency.high'), color: '#f87171' },
  };

  const STATUS_LABELS = {
    OPEN: t('my_posts.status.open'),
    EXPERT_RESPONDING: t('my_posts.status.expert_responding'),
    RESOLVED: t('my_posts.status.resolved'),
  };

  const tab = TABS.includes(searchParams.get('tab')) ? searchParams.get('tab') : 'posts';
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedOffer, setSelectedOffer] = useState(null);

  const setTab = (t_val) => {
    setSearchParams({ tab: t_val }, { replace: true });
  };

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    const authorParam = { author: user.id };

    if (tab === 'posts') {
      getPosts(authorParam).then(({ ok, data }) => {
        if (ok) setItems(data);
        setLoading(false);
      });
    } else if (tab === 'requests') {
      getHelpRequests(authorParam).then(({ ok, data }) => {
        if (ok) setItems(data);
        setLoading(false);
      });
    } else {
      getHelpOffers(authorParam).then(({ ok, data }) => {
        if (ok) setItems(data);
        setLoading(false);
      });
    }
  }, [user, tab]);

  return (
      <div className="page my-posts-page">
        <header className="dashboard-header">
          <h2>{t('my_posts.header.title')}</h2>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/profile')}>
            ← {t('my_posts.header.back')}
          </button>
        </header>

        <div className="my-posts-tabs">
          {TABS.map((t_key) => (
              <button
                  key={t_key}
                  className={`my-posts-tab ${tab === t_key ? 'my-posts-tab-active' : ''}`}
                  onClick={() => setTab(t_key)}
              >
                {t_key === 'posts' ? t('my_posts.tabs.posts') : t_key === 'requests' ? t('my_posts.tabs.requests') : t('my_posts.tabs.offers')}
              </button>
          ))}
        </div>

        {loading ? (
            <p className="my-posts-empty">{t('my_posts.states.loading')}</p>
        ) : items.length === 0 ? (
            <p className="my-posts-empty">
              {t('my_posts.states.empty', { tab: t(`my_posts.tab_names.${tab}`) })}
            </p>
        ) : (
            <div className="my-posts-list">
              {tab === 'posts' && items.map((post) => (
                  <Link to={`/forum/posts/${post.id}`} key={post.id} className="my-posts-card dashboard-card-link">
                    <div className="my-posts-card-top">
                      <h3 className="my-posts-card-title">{post.title}</h3>
                      <div className="my-posts-badges">
                        {FORUM_TYPE_STYLE[post.forum_type] && (
                            <span className="badge" style={{ color: FORUM_TYPE_STYLE[post.forum_type].color, background: FORUM_TYPE_STYLE[post.forum_type].bg }}>
                      {FORUM_TYPE_STYLE[post.forum_type].label}
                    </span>
                        )}
                        {post.hub_name && <span className="badge badge-muted">{post.hub_name}</span>}
                      </div>
                    </div>
                    {post.image_urls && post.image_urls.length > 0 && (
                        <div className="my-posts-card-images">
                          {post.image_urls.slice(0, 2).map((url, i) => (
                              <img key={i} src={resolveImageUrl(url)} alt="" className="my-posts-card-thumb" />
                          ))}
                        </div>
                    )}
                    <div className="my-posts-card-meta">
                      <span>{timeAgo(post.created_at, t)}</span>
                      <span>{t('my_posts.meta.upvotes', { count: post.upvote_count })}</span>
                      <span>{t('my_posts.meta.comments', { count: post.comment_count })}</span>
                      {post.repost_count > 0 && <span>{t('my_posts.meta.reposts', { count: post.repost_count })}</span>}
                    </div>
                  </Link>
              ))}

              {tab === 'requests' && items.map((req) => (
                  <Link to={`/help-requests/${req.id}`} key={req.id} className="my-posts-card dashboard-card-link">
                    <div className="my-posts-card-top">
                      <h3 className="my-posts-card-title">{req.title}</h3>
                      <div className="my-posts-badges">
                        {/* SAFE CATEGORY TRANSLATION HERE */}
                        <span className="badge">{t(`my_posts.categories.${req.category?.toLowerCase()}`, { defaultValue: req.category })}</span>

                        {URGENCY_STYLE[req.urgency] && (
                            <span className="badge" style={{ color: URGENCY_STYLE[req.urgency].color }}>
                      {URGENCY_STYLE[req.urgency].label}
                    </span>
                        )}
                        <span className={`badge ${req.status === 'RESOLVED' ? 'badge-resolved' : 'badge-muted'}`}>
                    {STATUS_LABELS[req.status] || req.status}
                  </span>
                        {req.hub_name && <span className="badge badge-muted">{req.hub_name}</span>}
                      </div>
                    </div>
                    <div className="my-posts-card-meta">
                      <span>{timeAgo(req.created_at, t)}</span>
                      {req.comment_count > 0 && <span>{t('my_posts.meta.comments', { count: req.comment_count })}</span>}
                    </div>
                  </Link>
              ))}

              {tab === 'offers' && items.map((offer) => (
                  <div
                      key={offer.id}
                      className="help-offer-card dashboard-card-link"
                      onClick={() => setSelectedOffer(offer)}
                  >
                    <div className="help-request-card-top">
                      <h3 className="help-request-card-title">{offer.skill_or_resource}</h3>
                      {/* SAFE CATEGORY TRANSLATION HERE */}
                      <span className="badge">{t(`my_posts.categories.${offer.category?.toLowerCase()}`, { defaultValue: offer.category })}</span>
                      {offer.hub_name && <span className="badge badge-muted">{offer.hub_name}</span>}
                    </div>
                    <p className="help-offer-card-desc">{offer.description}</p>
                    <div className="help-offer-card-footer">
                      <span className="help-offer-card-avail">{offer.availability}</span>
                      <span>{timeAgo(offer.created_at, t)}</span>
                    </div>
                  </div>
              ))}
            </div>
        )}

        {selectedOffer && (
            <div className="offer-modal-overlay" onClick={() => setSelectedOffer(null)}>
              <div className="offer-modal" onClick={(e) => e.stopPropagation()}>
                <button className="offer-modal-close" onClick={() => setSelectedOffer(null)}>&times;</button>
                <h2 className="offer-modal-title">{selectedOffer.skill_or_resource}</h2>
                {/* SAFE CATEGORY TRANSLATION HERE */}
                <span className="badge">{t(`my_posts.categories.${selectedOffer.category?.toLowerCase()}`, { defaultValue: selectedOffer.category })}</span>
                {selectedOffer.hub_name && <span className="badge badge-muted" style={{ marginLeft: '0.4rem' }}>{selectedOffer.hub_name}</span>}
                <p className="offer-modal-description">{selectedOffer.description}</p>
                <div className="offer-modal-details">
                  <div className="offer-modal-row">
                    <span className="offer-modal-label">{t('my_posts.modal.availability')}</span>
                    <span className="help-offer-card-avail">{selectedOffer.availability}</span>
                  </div>
                  <div className="offer-modal-row">
                    <span className="offer-modal-label">{t('my_posts.modal.posted')}</span>
                    <span>{timeAgo(selectedOffer.created_at, t)}</span>
                  </div>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}