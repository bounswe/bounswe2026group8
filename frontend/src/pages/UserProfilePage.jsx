import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getUserProfile, getPosts, getHelpRequests, getHelpOffers, resolveImageUrl } from '../services/api';
import { useTranslation } from 'react-i18next'; // 1. Import hook

// 2. Pass 't' to timeAgo
function timeAgo(dateStr, t) {
  const seconds = Math.floor((Date.now() - new Date(dateStr)) / 1000);
  if (seconds < 60) return t('user_profile.time.just_now');
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return t('user_profile.time.m_ago', { count: minutes });
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return t('user_profile.time.h_ago', { count: hours });
  const days = Math.floor(hours / 24);
  return t('user_profile.time.d_ago', { count: days });
}

export default function UserProfilePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
  const { t } = useTranslation(); // 3. Initialize hook

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState('posts');
  const [items, setItems] = useState([]);
  const [itemsLoading, setItemsLoading] = useState(false);

  // 4. Map the static styles to the dynamic translation keys
  const AVAILABILITY_LABELS = {
    SAFE: { label: t('user_profile.status.safe'), color: '#34d399' },
    NEEDS_HELP: { label: t('user_profile.status.needs_help'), color: '#f87171' },
    AVAILABLE_TO_HELP: { label: t('user_profile.status.available_to_help'), color: '#38bdf8' },
  };

  const FORUM_TYPE_STYLE = {
    GLOBAL: { label: t('user_profile.forum_types.global'), color: '#a78bfa', bg: '#a78bfa18' },
    STANDARD: { label: t('user_profile.forum_types.standard'), color: '#38bdf8', bg: '#38bdf818' },
    URGENT: { label: t('user_profile.forum_types.urgent'), color: '#f87171', bg: '#f8717118' },
  };

  useEffect(() => {
    if (currentUser && String(currentUser.id) === String(id)) {
      navigate('/profile', { replace: true });
    }
  }, [currentUser, id, navigate]);

  useEffect(() => {
    setLoading(true);
    getUserProfile(id).then(({ ok, data }) => {
      if (ok) setProfile(data);
      setLoading(false);
    });
  }, [id]);

  useEffect(() => {
    if (!profile) return;
    setItemsLoading(true);
    const authorParam = { author: profile.id };

    if (tab === 'posts') {
      getPosts(authorParam).then(({ ok, data }) => {
        if (ok) setItems(data);
        setItemsLoading(false);
      });
    } else if (tab === 'requests') {
      getHelpRequests(authorParam).then(({ ok, data }) => {
        if (ok) setItems(data);
        setItemsLoading(false);
      });
    } else {
      getHelpOffers(authorParam).then(({ ok, data }) => {
        if (ok) setItems(data);
        setItemsLoading(false);
      });
    }
  }, [profile, tab]);

  if (loading) return <div className="page"><p>{t('user_profile.states.loading')}</p></div>;
  if (!profile) return <div className="page"><p>{t('user_profile.states.not_found')}</p></div>;

  const avail = AVAILABILITY_LABELS[profile.profile?.availability_status] || AVAILABILITY_LABELS.SAFE;
  const roleLabel = profile.role === 'EXPERT' ? t('user_profile.roles.expert') : t('user_profile.roles.standard');

  return (
      <div className="page user-profile-page">
        <header className="dashboard-header">
          <h2>{t('user_profile.header.title')}</h2>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate(-1)}>
            ← {t('user_profile.header.back')}
          </button>
        </header>

        <div className="profile-identity-card">
          <div className="profile-avatar">{profile.full_name?.[0]?.toUpperCase() ?? '?'}</div>
          <div className="profile-identity-info">
            <h3 className="profile-name">{profile.full_name}</h3>
            <div className="profile-badges">
              <span className="badge">{roleLabel}</span>
              <span className="badge" style={{ color: avail.color, borderColor: avail.color + '44', background: avail.color + '11' }}>
              ● {avail.label}
            </span>
              {profile.hub && <span className="badge badge-muted">{profile.hub.name}</span>}
            </div>
          </div>
        </div>

        <div className="profile-section-card">
          <h4 className="profile-section-title">{t('user_profile.sections.personal_info')}</h4>
          <div className="profile-fields-grid">
            {profile.profile?.phone_number && (
                <div className="profile-field">
                  <span className="profile-field-label"><span className="profile-field-icon">📞</span>{t('user_profile.fields.phone')}</span>
                  <span className="profile-field-value">{profile.profile.phone_number}</span>
                </div>
            )}
            {profile.profile?.blood_type && (
                <div className="profile-field">
                  <span className="profile-field-label"><span className="profile-field-icon">🩸</span>{t('user_profile.fields.blood_type')}</span>
                  <span className="profile-field-value">{profile.profile.blood_type}</span>
                </div>
            )}
            {profile.profile?.emergency_contact && (
                <div className="profile-field">
                  <span className="profile-field-label"><span className="profile-field-icon">👤</span>{t('user_profile.fields.emergency_contact')}</span>
                  <span className="profile-field-value">{profile.profile.emergency_contact}</span>
                </div>
            )}
            {profile.profile?.emergency_contact_phone && (
                <div className="profile-field">
                  <span className="profile-field-label"><span className="profile-field-icon">🚨</span>{t('user_profile.fields.emergency_contact_phone')}</span>
                  <span className="profile-field-value">{profile.profile.emergency_contact_phone}</span>
                </div>
            )}
            {profile.profile?.preferred_language && (
                <div className="profile-field">
                  <span className="profile-field-label"><span className="profile-field-icon">🌐</span>{t('user_profile.fields.preferred_language')}</span>
                  <span className="profile-field-value">{profile.profile.preferred_language}</span>
                </div>
            )}
            {profile.neighborhood_address && (
                <div className="profile-field">
                  <span className="profile-field-label"><span className="profile-field-icon">📍</span>{t('user_profile.fields.location')}</span>
                  <span className="profile-field-value">{profile.neighborhood_address}</span>
                </div>
            )}
            {profile.profile?.has_disability && (
                <div className="profile-field">
                  <span className="profile-field-label"><span className="profile-field-icon">♿</span>{t('user_profile.fields.has_disability')}</span>
                  <span className="profile-field-value">{t('user_profile.fields.yes')}</span>
                </div>
            )}
            <div className="profile-field">
              <span className="profile-field-label"><span className="profile-field-icon">📡</span>{t('user_profile.fields.availability')}</span>
              <span className="profile-field-value" style={{ color: avail.color }}>● {avail.label}</span>
            </div>
            {profile.profile?.special_needs && (
                <div className="profile-field profile-field-full">
                  <span className="profile-field-label"><span className="profile-field-icon">📋</span>{t('user_profile.fields.special_needs')}</span>
                  <span className="profile-field-value">{profile.profile.special_needs}</span>
                </div>
            )}
            {profile.profile?.bio && (
                <div className="profile-field profile-field-full">
                  <span className="profile-field-label"><span className="profile-field-icon">✏️</span>{t('user_profile.fields.bio')}</span>
                  <span className="profile-field-value">{profile.profile.bio}</span>
                </div>
            )}
          </div>
        </div>

        {profile.role === 'EXPERT' && profile.expertise_fields?.length > 0 && (
            <div className="profile-section-card">
              <h4 className="profile-section-title">{t('user_profile.sections.expertise')}</h4>
              <ul className="item-card-list">
                {profile.expertise_fields.map((ef) => (
                    <li key={ef.id} className="item-card">
                      <div className="item-card-icon">🎓</div>
                      <div className="item-card-body">
                        <span className="item-card-name">{ef.field}</span>
                        <span className="item-card-meta">
                    {ef.certification_level === 'ADVANCED'
                        ? `★ ${t('user_profile.expertise_item.advanced')}`
                        : `◎ ${t('user_profile.expertise_item.beginner')}`}
                  </span>
                      </div>
                      {ef.certification_document_url && (
                          <a href={ef.certification_document_url} target="_blank" rel="noreferrer" className="cert-link">
                            {t('user_profile.expertise_item.certificate')} ↗
                          </a>
                      )}
                    </li>
                ))}
              </ul>
            </div>
        )}

        {profile.resources?.length > 0 && (
            <div className="profile-section-card">
              <h4 className="profile-section-title">{t('user_profile.sections.resources')}</h4>
              <ul className="item-card-list">
                {profile.resources.map((r) => (
                    <li key={r.id} className="item-card">
                      <div className="item-card-icon">📦</div>
                      <div className="item-card-body">
                        <span className="item-card-name">{r.name}</span>
                        <span className="item-card-meta">{r.category} · {t('user_profile.resource_item.qty', { count: r.quantity })}</span>
                      </div>
                      <span className={`condition-badge ${r.condition ? 'condition-ok' : 'condition-bad'}`}>
                  {r.condition ? `✓ ${t('user_profile.resource_item.functional')}` : `✗ ${t('user_profile.resource_item.not_functional')}`}
                </span>
                    </li>
                ))}
              </ul>
            </div>
        )}

        <div className="profile-section-card">
          <h4 className="profile-section-title">{t('user_profile.sections.activity')}</h4>
          <div className="my-posts-tabs">
            {['posts', 'requests', 'offers'].map((t_key) => (
                <button
                    key={t_key}
                    className={`my-posts-tab ${tab === t_key ? 'my-posts-tab-active' : ''}`}
                    onClick={() => setTab(t_key)}
                >
                  {t_key === 'posts' ? t('user_profile.tabs.posts') : t_key === 'requests' ? t('user_profile.tabs.requests') : t('user_profile.tabs.offers')}
                </button>
            ))}
          </div>

          {itemsLoading ? (
              <p className="my-posts-empty">{t('user_profile.states.loading')}</p>
          ) : items.length === 0 ? (
              <p className="my-posts-empty">{t('user_profile.states.no_items', { tab: tab === 'posts' ? t('user_profile.tabs.posts').toLowerCase() : tab === 'requests' ? t('user_profile.tabs.requests').toLowerCase() : t('user_profile.tabs.offers').toLowerCase() })}</p>
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
                      {post.image_urls?.length > 0 && (
                          <div className="my-posts-card-images">
                            {post.image_urls.slice(0, 2).map((url, i) => (
                                <img key={i} src={resolveImageUrl(url)} alt="" className="my-posts-card-thumb" />
                            ))}
                          </div>
                      )}
                      <div className="my-posts-card-meta">
                        <span>{timeAgo(post.created_at, t)}</span>
                        <span>{t('user_profile.card.upvotes', { count: post.upvote_count })}</span>
                        <span>{t('user_profile.card.comments', { count: post.comment_count })}</span>
                      </div>
                    </Link>
                ))}

                {tab === 'requests' && items.map((req) => (
                    <Link to={`/help-requests/${req.id}`} key={req.id} className="my-posts-card dashboard-card-link">
                      <div className="my-posts-card-top">
                        <h3 className="my-posts-card-title">{req.title}</h3>
                        <div className="my-posts-badges">
                          <span className="badge">{req.category}</span>
                          {req.hub_name && <span className="badge badge-muted">{req.hub_name}</span>}
                        </div>
                      </div>
                      <div className="my-posts-card-meta">
                        <span>{timeAgo(req.created_at, t)}</span>
                        {req.comment_count > 0 && <span>{t('user_profile.card.comments', { count: req.comment_count })}</span>}
                      </div>
                    </Link>
                ))}

                {tab === 'offers' && items.map((offer) => (
                    <div key={offer.id} className="my-posts-card">
                      <div className="my-posts-card-top">
                        <h3 className="my-posts-card-title">{offer.skill_or_resource}</h3>
                        <div className="my-posts-badges">
                          <span className="badge">{offer.category}</span>
                          {offer.hub_name && <span className="badge badge-muted">{offer.hub_name}</span>}
                        </div>
                      </div>
                      <p className="my-posts-card-desc">{offer.description}</p>
                      <div className="my-posts-card-meta">
                        <span>{offer.availability}</span>
                        <span>{timeAgo(offer.created_at, t)}</span>
                      </div>
                    </div>
                ))}
              </div>
          )}
        </div>
      </div>
  );
}