import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getUserProfile, getPosts, getHelpRequests, getHelpOffers, resolveImageUrl } from '../services/api';

const AVAILABILITY_LABELS = {
  SAFE: { label: 'Safe', color: '#34d399' },
  NEEDS_HELP: { label: 'Needs Help', color: '#f87171' },
  AVAILABLE_TO_HELP: { label: 'Available to Help', color: '#38bdf8' },
};

const FORUM_TYPE_STYLE = {
  GLOBAL: { label: 'Global', color: '#a78bfa', bg: '#a78bfa18' },
  STANDARD: { label: 'Standard', color: '#38bdf8', bg: '#38bdf818' },
  URGENT: { label: 'Urgent', color: '#f87171', bg: '#f8717118' },
};

function timeAgo(dateStr) {
  const seconds = Math.floor((Date.now() - new Date(dateStr)) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

export default function UserProfilePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState('posts');
  const [items, setItems] = useState([]);
  const [itemsLoading, setItemsLoading] = useState(false);

  // Redirect to own profile if viewing self
  useEffect(() => {
    if (currentUser && String(currentUser.id) === String(id)) {
      navigate('/profile', { replace: true });
    }
  }, [currentUser, id, navigate]);

  // Fetch user profile
  useEffect(() => {
    setLoading(true);
    getUserProfile(id).then(({ ok, data }) => {
      if (ok) setProfile(data);
      setLoading(false);
    });
  }, [id]);

  // Fetch user's posts/requests/offers
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

  if (loading) return <div className="page"><p>Loading...</p></div>;
  if (!profile) return <div className="page"><p>User not found.</p></div>;

  const avail = AVAILABILITY_LABELS[profile.profile?.availability_status] || AVAILABILITY_LABELS.SAFE;
  const roleLabel = profile.role === 'EXPERT' ? 'Expert' : 'Standard';

  return (
    <div className="page user-profile-page">
      <header className="dashboard-header">
        <h2>User Profile</h2>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate(-1)}>← Back</button>
      </header>

      {/* Identity Card */}
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

      {/* Info Section */}
      <div className="profile-section-card">
        <h4 className="profile-section-title">Personal Information</h4>
        <div className="profile-fields-grid">
          {profile.profile?.phone_number && (
            <div className="profile-field">
              <span className="profile-field-label"><span className="profile-field-icon">📞</span>Phone</span>
              <span className="profile-field-value">{profile.profile.phone_number}</span>
            </div>
          )}
          {profile.profile?.blood_type && (
            <div className="profile-field">
              <span className="profile-field-label"><span className="profile-field-icon">🩸</span>Blood Type</span>
              <span className="profile-field-value">{profile.profile.blood_type}</span>
            </div>
          )}
          {profile.profile?.emergency_contact && (
            <div className="profile-field">
              <span className="profile-field-label"><span className="profile-field-icon">👤</span>Emergency Contact</span>
              <span className="profile-field-value">{profile.profile.emergency_contact}</span>
            </div>
          )}
          {profile.profile?.emergency_contact_phone && (
            <div className="profile-field">
              <span className="profile-field-label"><span className="profile-field-icon">🚨</span>Emergency Contact Phone</span>
              <span className="profile-field-value">{profile.profile.emergency_contact_phone}</span>
            </div>
          )}
          {profile.profile?.preferred_language && (
            <div className="profile-field">
              <span className="profile-field-label"><span className="profile-field-icon">🌐</span>Preferred Language</span>
              <span className="profile-field-value">{profile.profile.preferred_language}</span>
            </div>
          )}
          {profile.neighborhood_address && (
            <div className="profile-field">
              <span className="profile-field-label"><span className="profile-field-icon">📍</span>Location</span>
              <span className="profile-field-value">{profile.neighborhood_address}</span>
            </div>
          )}
          {profile.profile?.has_disability && (
            <div className="profile-field">
              <span className="profile-field-label"><span className="profile-field-icon">♿</span>Has Disability</span>
              <span className="profile-field-value">Yes</span>
            </div>
          )}
          <div className="profile-field">
            <span className="profile-field-label"><span className="profile-field-icon">📡</span>Availability Status</span>
            <span className="profile-field-value" style={{ color: avail.color }}>● {avail.label}</span>
          </div>
          {profile.profile?.special_needs && (
            <div className="profile-field profile-field-full">
              <span className="profile-field-label"><span className="profile-field-icon">📋</span>Special Needs</span>
              <span className="profile-field-value">{profile.profile.special_needs}</span>
            </div>
          )}
          {profile.profile?.bio && (
            <div className="profile-field profile-field-full">
              <span className="profile-field-label"><span className="profile-field-icon">✏️</span>Bio</span>
              <span className="profile-field-value">{profile.profile.bio}</span>
            </div>
          )}
        </div>
      </div>

      {/* Expertise (if expert) */}
      {profile.role === 'EXPERT' && profile.expertise_fields?.length > 0 && (
        <div className="profile-section-card">
          <h4 className="profile-section-title">Expertise</h4>
          <ul className="item-card-list">
            {profile.expertise_fields.map((ef) => (
              <li key={ef.id} className="item-card">
                <div className="item-card-icon">🎓</div>
                <div className="item-card-body">
                  <span className="item-card-name">{ef.field}</span>
                  <span className="item-card-meta">{ef.certification_level === 'ADVANCED' ? '★ Advanced' : '◎ Beginner'}</span>
                </div>
                {ef.certification_document_url && (
                  <a href={ef.certification_document_url} target="_blank" rel="noreferrer" className="cert-link">Certificate ↗</a>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Resources */}
      {profile.resources?.length > 0 && (
        <div className="profile-section-card">
          <h4 className="profile-section-title">Resources</h4>
          <ul className="item-card-list">
            {profile.resources.map((r) => (
              <li key={r.id} className="item-card">
                <div className="item-card-icon">📦</div>
                <div className="item-card-body">
                  <span className="item-card-name">{r.name}</span>
                  <span className="item-card-meta">{r.category} · qty {r.quantity}</span>
                </div>
                <span className={`condition-badge ${r.condition ? 'condition-ok' : 'condition-bad'}`}>
                  {r.condition ? '✓ Functional' : '✗ Not functional'}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Posts Tabs */}
      <div className="profile-section-card">
        <h4 className="profile-section-title">Activity</h4>
        <div className="my-posts-tabs">
          {['posts', 'requests', 'offers'].map((t) => (
            <button
              key={t}
              className={`my-posts-tab ${tab === t ? 'my-posts-tab-active' : ''}`}
              onClick={() => setTab(t)}
            >
              {t === 'posts' ? 'Posts' : t === 'requests' ? 'Requests' : 'Offers'}
            </button>
          ))}
        </div>

        {itemsLoading ? (
          <p className="my-posts-empty">Loading...</p>
        ) : items.length === 0 ? (
          <p className="my-posts-empty">No {tab} yet.</p>
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
                  <span>{timeAgo(post.created_at)}</span>
                  <span>{post.upvote_count} upvotes</span>
                  <span>{post.comment_count} comments</span>
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
                  <span>{timeAgo(req.created_at)}</span>
                  {req.comment_count > 0 && <span>{req.comment_count} comments</span>}
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
                  <span>{timeAgo(offer.created_at)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
