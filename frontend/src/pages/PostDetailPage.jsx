import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  getPost, updatePost, deletePost,
  getComments, createComment, deleteComment,
  vote, reportPost, repost, uploadImages, resolveImageUrl,
} from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

const AVAIL_COLORS = { SAFE: '#34d399', NEEDS_HELP: '#f87171', AVAILABLE_TO_HELP: '#38bdf8' };

// Pass 't' down to AuthorStatus
function AuthorStatus({ profile, t }) {
  const s = profile?.availability_status;
  if (!s || !AVAIL_COLORS[s]) return null;
  const c = AVAIL_COLORS[s];

  const AVAIL_LABELS = {
    SAFE: t('post_detail.availability.safe'),
    NEEDS_HELP: t('post_detail.availability.needs_help'),
    AVAILABLE_TO_HELP: t('post_detail.availability.available')
  };

  return <span className="badge" style={{ color: c, borderColor: c + '44', background: c + '11', fontSize: '0.7rem', padding: '1px 6px' }}>● {AVAIL_LABELS[s]}</span>;
}

// Pass 't' down to timeAgo
function timeAgo(dateStr, t) {
  const seconds = Math.floor((Date.now() - new Date(dateStr)) / 1000);
  if (seconds < 60) return t('post_detail.time.just_now');
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return t('post_detail.time.m_ago', { count: minutes });
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return t('post_detail.time.h_ago', { count: hours });
  const days = Math.floor(hours / 24);
  return t('post_detail.time.d_ago', { count: days });
}

export default function PostDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const selectedHub = user?.hub;
  const { t } = useTranslation(); // Initialize hook

  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);

  const [commentText, setCommentText] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);

  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [editImages, setEditImages] = useState([]);
  const [editUploading, setEditUploading] = useState(false);
  const [editSaving, setEditSaving] = useState(false);
  const editFileRef = useRef(null);

  const [showReport, setShowReport] = useState(false);
  const [reportReason, setReportReason] = useState('SPAM');
  const [reportMsg, setReportMsg] = useState('');

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const [copied, setCopied] = useState(false);
  const [reposted, setReposted] = useState(false);
  const [repostError, setRepostError] = useState('');

  const [lightboxIndex, setLightboxIndex] = useState(null);

  useEffect(() => {
    Promise.all([getPost(id), getComments(id)]).then(([postRes, commRes]) => {
      if (postRes.ok) setPost(postRes.data);
      if (commRes.ok) setComments(commRes.data);
      setLoading(false);
    });
  }, [id]);

  const isAuthor = user && post && user.id === post.author.id;

  const handleVote = async (type) => {
    if (!isAuthenticated) return;
    const prev = post;
    let newUp = post.upvote_count;
    let newDown = post.downvote_count;
    let newUserVote = type;

    if (post.user_vote === type) {
      if (type === 'UP') newUp--;
      else newDown--;
      newUserVote = null;
    } else if (post.user_vote) {
      if (post.user_vote === 'UP') { newUp--; newDown++; }
      else { newDown--; newUp++; }
    } else {
      if (type === 'UP') newUp++;
      else newDown++;
    }

    setPost({ ...post, upvote_count: newUp, downvote_count: newDown, user_vote: newUserVote });
    const { ok } = await vote(id, type);
    if (!ok) setPost(prev);
  };

  const handleCommentSubmit = async (e) => {
    e.preventDefault();
    if (!commentText.trim()) return;
    setCommentSubmitting(true);
    const { ok, data } = await createComment(id, commentText.trim());
    if (ok) {
      setComments((prev) => [...prev, data]);
      setCommentText('');
      setPost((p) => p && { ...p, comment_count: p.comment_count + 1 });
    }
    setCommentSubmitting(false);
  };

  const handleDeleteComment = async (cid) => {
    const { ok } = await deleteComment(cid);
    if (ok) {
      setComments((prev) => prev.filter((c) => c.id !== cid));
      setPost((p) => p && { ...p, comment_count: Math.max(0, p.comment_count - 1) });
    }
  };

  const startEdit = () => {
    setEditTitle(post.title);
    setEditContent(post.content);
    setEditImages(post.image_urls || []);
    setEditing(true);
  };

  const handleEditFileSelect = async (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;
    setEditUploading(true);
    const { ok, data } = await uploadImages(files);
    setEditUploading(false);
    if (ok) setEditImages((prev) => [...prev, ...data.urls]);
    if (editFileRef.current) editFileRef.current.value = '';
  };

  const handleEditSave = async () => {
    setEditSaving(true);
    const { ok, data } = await updatePost(id, {
      title: editTitle,
      content: editContent,
      image_urls: editImages,
    });
    if (ok) {
      setPost(data);
      setEditing(false);
    }
    setEditSaving(false);
  };

  const handleDelete = async () => {
    setShowDeleteConfirm(false);
    const { ok } = await deletePost(id);
    if (ok) {
      navigate(-1);
    }
  };

  const handleShare = () => {
    const url = `${window.location.origin}/forum/posts/${id}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  };

  const handleRepost = async () => {
    const { ok, data } = await repost(id, selectedHub?.id);
    if (ok) {
      setReposted(true);
      setPost((p) => p && { ...p, user_has_reposted: true, repost_count: p.repost_count + 1 });
    } else {
      setRepostError(data?.detail || t('post_detail.actions.repost_error'));
      setTimeout(() => setRepostError(''), 2000);
    }
  };

  const handleReport = async () => {
    const { ok, data } = await reportPost(id, reportReason);
    if (ok) {
      setReportMsg(t('post_detail.report_modal.success'));
    } else {
      setReportMsg(data?.detail || t('post_detail.report_modal.error'));
    }
    setTimeout(() => { setShowReport(false); setReportMsg(''); }, 1800);
  };

  if (loading) {
    return <div className="page"><p className="forum-empty">{t('post_detail.states.loading')}</p></div>;
  }

  if (!post) {
    return <div className="page"><p className="forum-empty">{t('post_detail.states.not_found')}</p></div>;
  }

  return (
      <div className="page post-detail-page">
        <button onClick={() => navigate(-1)} className="link post-back-link" style={{ background: 'none', border: 'none', cursor: 'pointer' }}>&larr; {t('post_detail.actions.back')}</button>

        <article className="post-article">
          {editing ? (
              <div className="post-edit-form">
                <input
                    className="post-edit-title"
                    value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                    placeholder={t('post_detail.labels.title')}
                />
                <textarea
                    className="post-edit-content"
                    value={editContent}
                    onChange={(e) => setEditContent(e.target.value)}
                    rows={8}
                    placeholder={t('post_detail.labels.content')}
                />

                <div className="form-group" style={{ marginTop: '0.75rem' }}>
                  <label>{t('post_detail.labels.images')}</label>
                  <div className="image-upload-area">
                    <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => editFileRef.current?.click()}
                        disabled={editUploading}
                    >
                      {editUploading ? t('post_detail.actions.uploading') : t('post_detail.actions.upload')}
                    </button>
                    <input
                        ref={editFileRef}
                        type="file"
                        accept="image/jpeg,image/png,image/gif,image/webp"
                        multiple
                        onChange={handleEditFileSelect}
                        style={{ display: 'none' }}
                    />
                  </div>
                  {editImages.length > 0 && (
                      <div className="image-preview-list">
                        {editImages.map((url, i) => (
                            <div className="image-preview-item" key={i}>
                              <img src={resolveImageUrl(url)} alt={`Image ${i + 1}`} className="image-preview-thumb" />
                              <button
                                  type="button"
                                  className="image-preview-remove"
                                  onClick={() => setEditImages((prev) => prev.filter((_, idx) => idx !== i))}
                                  title={t('post_detail.actions.remove')}
                              >&times;</button>
                            </div>
                        ))}
                      </div>
                  )}
                </div>

                <div className="post-edit-actions">
                  <button className="btn btn-primary btn-sm" onClick={handleEditSave} disabled={editSaving || editUploading}>
                    {editSaving ? t('post_detail.actions.saving') : t('post_detail.actions.save')}
                  </button>
                  <button className="btn btn-secondary btn-sm" onClick={() => setEditing(false)}>
                    {t('post_detail.actions.cancel')}
                  </button>
                </div>
              </div>
          ) : (
              <>
                {post.reposted_from && (
                    <div className="repost-label">
                      {t('post_detail.labels.reposted_from')} <Link to={`/forum/posts/${post.reposted_from.id}`} className="link">{post.reposted_from.author.full_name}</Link>
                    </div>
                )}
                <div className="post-header-row">
                  <div>
                <span className={`badge ${post.forum_type === 'URGENT' ? 'badge-urgent' : post.forum_type === 'GLOBAL' ? 'badge-global' : ''}`}>
                  {/* Keep the DB enum string if you didn't translate it globally, otherwise wrap in t() */}
                  {post.forum_type}
                </span>
                    <span className="badge badge-muted">{post.hub_name}</span>
                  </div>
                  {isAuthor && (
                      <div className="post-owner-actions">
                        <button className="btn btn-secondary btn-sm" onClick={startEdit}>{t('post_detail.actions.edit')}</button>
                        <button className="btn btn-danger btn-sm" onClick={() => setShowDeleteConfirm(true)}>{t('post_detail.actions.delete')}</button>
                      </div>
                  )}
                </div>

                <h1 className="post-title">{post.title}</h1>

                <div className="post-meta">
                  <Link to={`/users/${post.author.id}`} className="author-link">{post.author.full_name}</Link>
                  {post.author.role === 'EXPERT' && <span className="badge badge-expert-responding">{t('post_detail.labels.expert')}</span>}
                  <AuthorStatus profile={post.author.profile} t={t} />
                  <span className="post-card-dot">&middot;</span>
                  <span>{timeAgo(post.created_at, t)}</span>
                  {Math.abs(new Date(post.updated_at) - new Date(post.created_at)) > 1000 && (
                      <span className="post-edited">{t('post_detail.states.edited')}</span>
                  )}
                </div>

                <div className="post-content">{post.content}</div>

                {post.image_urls && post.image_urls.length > 0 && (
                    <div className="post-images">
                      {post.image_urls.map((url, i) => (
                          <img
                              key={i}
                              src={resolveImageUrl(url)}
                              alt={`Attachment ${i + 1}`}
                              className="post-image"
                              onClick={() => setLightboxIndex(i)}
                          />
                      ))}
                    </div>
                )}
              </>
          )}

          <div className="vote-bar">
            <button
                className={`vote-btn vote-up ${post.user_vote === 'UP' ? 'vote-active' : ''}`}
                onClick={() => handleVote('UP')}
                disabled={!isAuthenticated}
                title={t('post_detail.labels.upvote')}
            >
              &#9650; {post.upvote_count}
            </button>
            <button
                className={`vote-btn vote-down ${post.user_vote === 'DOWN' ? 'vote-active' : ''}`}
                onClick={() => handleVote('DOWN')}
                disabled={!isAuthenticated}
                title={t('post_detail.labels.downvote')}
            >
              &#9660; {post.downvote_count}
            </button>
            {!post.reposted_from && post.repost_count > 0 && (
                <span className="post-stat post-stat-repost">
              {post.repost_count === 1
                  ? t('post_detail.counts.repost_single', { count: post.repost_count })
                  : t('post_detail.counts.repost_plural', { count: post.repost_count })}
            </span>
            )}
            <button className="share-btn" onClick={handleShare}>
              {copied ? t('post_detail.actions.copied') : t('post_detail.actions.share')}
            </button>
            {isAuthenticated && !isAuthor && !post.user_has_reposted && !reposted && (
                <button
                    className="share-btn"
                    onClick={handleRepost}
                >
                  {repostError || t('post_detail.actions.repost')}
                </button>
            )}
            {(post.user_has_reposted || reposted) && !isAuthor && (
                <span className="post-stat post-stat-repost">{t('post_detail.states.reposted')}</span>
            )}
            {isAuthenticated && !isAuthor && (
                <button className="btn btn-sm btn-report" onClick={() => setShowReport(true)}>
                  {t('post_detail.actions.report')}
                </button>
            )}
          </div>
        </article>

        {showDeleteConfirm && (
            <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}>
              <div className="modal-card" onClick={(e) => e.stopPropagation()}>
                <h3>{t('post_detail.delete_modal.title')}</h3>
                <p className="modal-body-text">{t('post_detail.delete_modal.desc')}</p>
                <div className="modal-actions">
                  <button className="btn btn-danger btn-sm" onClick={handleDelete}>{t('post_detail.actions.delete')}</button>
                  <button className="btn btn-secondary btn-sm" onClick={() => setShowDeleteConfirm(false)}>{t('post_detail.actions.cancel')}</button>
                </div>
              </div>
            </div>
        )}

        {showReport && (
            <div className="modal-overlay" onClick={() => setShowReport(false)}>
              <div className="modal-card" onClick={(e) => e.stopPropagation()}>
                <h3>{t('post_detail.report_modal.title')}</h3>
                {reportMsg ? (
                    <p className="report-msg">{reportMsg}</p>
                ) : (
                    <>
                      <div className="form-group">
                        <label htmlFor="report-reason">{t('post_detail.report_modal.reason')}</label>
                        <select
                            id="report-reason"
                            value={reportReason}
                            onChange={(e) => setReportReason(e.target.value)}
                        >
                          <option value="SPAM">{t('post_detail.report_modal.options.spam')}</option>
                          <option value="MISINFORMATION">{t('post_detail.report_modal.options.misinformation')}</option>
                          <option value="ABUSE">{t('post_detail.report_modal.options.abuse')}</option>
                          <option value="IRRELEVANT">{t('post_detail.report_modal.options.irrelevant')}</option>
                        </select>
                      </div>
                      <div className="modal-actions">
                        <button className="btn btn-primary btn-sm" onClick={handleReport}>{t('post_detail.report_modal.submit')}</button>
                        <button className="btn btn-secondary btn-sm" onClick={() => setShowReport(false)}>{t('post_detail.actions.cancel')}</button>
                      </div>
                    </>
                )}
              </div>
            </div>
        )}

        <section className="comments-section">
          <h2 className="comments-heading">{t('post_detail.counts.comments', { count: post.comment_count })}</h2>

          {isAuthenticated && (
              <form className="comment-form" onSubmit={handleCommentSubmit}>
            <textarea
                className="comment-input"
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                placeholder={t('post_detail.comments.placeholder')}
                rows={3}
            />
                <button
                    type="submit"
                    className="btn btn-primary btn-sm"
                    disabled={commentSubmitting || !commentText.trim()}
                >
                  {commentSubmitting ? t('post_detail.comments.posting') : t('post_detail.comments.post')}
                </button>
              </form>
          )}

          {comments.length === 0 ? (
              <p className="forum-empty">{t('post_detail.comments.empty')}</p>
          ) : (
              <div className="comment-list">
                {comments.map((c) => (
                    <div className="comment-card" key={c.id}>
                      <div className="comment-header">
                        <Link to={`/users/${c.author.id}`} className="comment-author author-link">{c.author.full_name}</Link>
                        {c.author.role === 'EXPERT' && <span className="badge badge-expert-responding">{t('post_detail.labels.expert')}</span>}
                        <AuthorStatus profile={c.author.profile} t={t} />
                        <span className="post-card-dot">&middot;</span>
                        <span className="comment-time">{timeAgo(c.created_at, t)}</span>
                        {user && user.id === c.author.id && (
                            <button
                                className="comment-delete"
                                onClick={() => handleDeleteComment(c.id)}
                                title={t('post_detail.comments.delete_title')}
                            >
                              &times;
                            </button>
                        )}
                      </div>
                      <p className="comment-body">{c.content}</p>
                    </div>
                ))}
              </div>
          )}
        </section>

        {lightboxIndex !== null && post.image_urls && (
            <div className="lightbox-overlay" onClick={() => setLightboxIndex(null)}>
              <button className="lightbox-close" onClick={() => setLightboxIndex(null)}>&times;</button>
              {post.image_urls.length > 1 && (
                  <button
                      className="lightbox-nav lightbox-nav--prev"
                      onClick={(e) => { e.stopPropagation(); setLightboxIndex((lightboxIndex - 1 + post.image_urls.length) % post.image_urls.length); }}
                  >&#8249;</button>
              )}
              <img
                  src={resolveImageUrl(post.image_urls[lightboxIndex])}
                  alt={`Image ${lightboxIndex + 1}`}
                  className="lightbox-img"
                  onClick={(e) => e.stopPropagation()}
              />
              {post.image_urls.length > 1 && (
                  <button
                      className="lightbox-nav lightbox-nav--next"
                      onClick={(e) => { e.stopPropagation(); setLightboxIndex((lightboxIndex + 1) % post.image_urls.length); }}
                  >&#8250;</button>
              )}
              {post.image_urls.length > 1 && (
                  <span className="lightbox-counter">{lightboxIndex + 1} / {post.image_urls.length}</span>
              )}
            </div>
        )}
      </div>
  );
}