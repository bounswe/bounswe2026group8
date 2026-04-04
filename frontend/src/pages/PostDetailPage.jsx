import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  getPost, updatePost, deletePost,
  getComments, createComment, deleteComment,
  vote, reportPost, repost, uploadImages, resolveImageUrl,
} from '../services/api';
import { useAuth } from '../context/AuthContext';

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

export default function PostDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const selectedHub = user?.hub;

  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);

  // Comment input
  const [commentText, setCommentText] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);

  // Edit mode
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [editImages, setEditImages] = useState([]);
  const [editUploading, setEditUploading] = useState(false);
  const [editSaving, setEditSaving] = useState(false);
  const editFileRef = useRef(null);

  // Report modal
  const [showReport, setShowReport] = useState(false);
  const [reportReason, setReportReason] = useState('SPAM');
  const [reportMsg, setReportMsg] = useState('');

  // Delete confirm
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Share & repost
  const [copied, setCopied] = useState(false);
  const [reposted, setReposted] = useState(false);
  const [repostError, setRepostError] = useState('');

  // Lightbox
  const [lightboxIndex, setLightboxIndex] = useState(null);

  useEffect(() => {
    Promise.all([getPost(id), getComments(id)]).then(([postRes, commRes]) => {
      if (postRes.ok) setPost(postRes.data);
      if (commRes.ok) setComments(commRes.data);
      setLoading(false);
    });
  }, [id]);

  const isAuthor = user && post && user.id === post.author.id;

  // ── Voting ──────────────────────────────────────────────────────────────────
  const handleVote = async (type) => {
    if (!isAuthenticated) return;
    const prev = post;
    // Optimistic update
    let newUp = post.upvote_count;
    let newDown = post.downvote_count;
    let newUserVote = type;

    if (post.user_vote === type) {
      // Toggle off
      if (type === 'UP') newUp--;
      else newDown--;
      newUserVote = null;
    } else if (post.user_vote) {
      // Switch
      if (post.user_vote === 'UP') { newUp--; newDown++; }
      else { newDown--; newUp++; }
    } else {
      // New
      if (type === 'UP') newUp++;
      else newDown++;
    }

    setPost({ ...post, upvote_count: newUp, downvote_count: newDown, user_vote: newUserVote });
    const { ok } = await vote(id, type);
    if (!ok) setPost(prev);
  };

  // ── Comments ────────────────────────────────────────────────────────────────
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

  // ── Edit ────────────────────────────────────────────────────────────────────
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

  // ── Delete ──────────────────────────────────────────────────────────────────
  const handleDelete = async () => {
    setShowDeleteConfirm(false);
    const { ok } = await deletePost(id);
    if (ok) {
      navigate('/forum', { replace: true, state: { forumTab: post?.forum_type } });
    }
  };

  // ── Share ────────────────────────────────────────────────────────────────────
  const handleShare = () => {
    const url = `${window.location.origin}/forum/posts/${id}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  };

  // ── Repost ──────────────────────────────────────────────────────────────────
  const handleRepost = async () => {
    const { ok, data } = await repost(id, selectedHub?.id);
    if (ok) {
      setReposted(true);
      setPost((p) => p && { ...p, user_has_reposted: true, repost_count: p.repost_count + 1 });
    } else {
      setRepostError(data?.detail || 'Could not repost.');
      setTimeout(() => setRepostError(''), 2000);
    }
  };

  // ── Report ──────────────────────────────────────────────────────────────────
  const handleReport = async () => {
    const { ok, data } = await reportPost(id, reportReason);
    if (ok) {
      setReportMsg('Report submitted. Thank you.');
    } else {
      setReportMsg(data?.detail || 'Could not submit report.');
    }
    setTimeout(() => { setShowReport(false); setReportMsg(''); }, 1800);
  };

  if (loading) {
    return <div className="page"><p className="forum-empty">Loading...</p></div>;
  }

  if (!post) {
    return <div className="page"><p className="forum-empty">Post not found.</p></div>;
  }

  return (
    <div className="page post-detail-page">
      <Link to="/forum" state={{ forumTab: post?.forum_type }} className="link post-back-link">&larr; Back to Forum</Link>

      {/* Post content */}
      <article className="post-article">
        {editing ? (
          <div className="post-edit-form">
            <input
              className="post-edit-title"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              placeholder="Title"
            />
            <textarea
              className="post-edit-content"
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
              rows={8}
              placeholder="Content"
            />

            <div className="form-group" style={{ marginTop: '0.75rem' }}>
              <label>Images</label>
              <div className="image-upload-area">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => editFileRef.current?.click()}
                  disabled={editUploading}
                >
                  {editUploading ? 'Uploading...' : 'Upload from Device'}
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
                        title="Remove"
                      >&times;</button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="post-edit-actions">
              <button className="btn btn-primary btn-sm" onClick={handleEditSave} disabled={editSaving || editUploading}>
                {editSaving ? 'Saving...' : 'Save'}
              </button>
              <button className="btn btn-secondary btn-sm" onClick={() => setEditing(false)}>
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <>
            {post.reposted_from && (
              <div className="repost-label">
                Reposted from <Link to={`/forum/posts/${post.reposted_from.id}`} className="link">{post.reposted_from.author.full_name}</Link>
              </div>
            )}
            <div className="post-header-row">
              <div>
                <span className={`badge ${post.forum_type === 'URGENT' ? 'badge-urgent' : post.forum_type === 'GLOBAL' ? 'badge-global' : ''}`}>
                  {post.forum_type}
                </span>
                <span className="badge badge-muted">{post.hub_name}</span>
              </div>
              {isAuthor && (
                <div className="post-owner-actions">
                  <button className="btn btn-secondary btn-sm" onClick={startEdit}>Edit</button>
                  <button className="btn btn-danger btn-sm" onClick={() => setShowDeleteConfirm(true)}>Delete</button>
                </div>
              )}
            </div>

            <h1 className="post-title">{post.title}</h1>

            <div className="post-meta">
              <span>{post.author.full_name}</span>
              <span className="post-card-dot">&middot;</span>
              <span>{timeAgo(post.created_at)}</span>
              {Math.abs(new Date(post.updated_at) - new Date(post.created_at)) > 1000 && (
                <span className="post-edited">(edited)</span>
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

        {/* Vote bar */}
        <div className="vote-bar">
          <button
            className={`vote-btn vote-up ${post.user_vote === 'UP' ? 'vote-active' : ''}`}
            onClick={() => handleVote('UP')}
            disabled={!isAuthenticated}
            title="Upvote"
          >
            &#9650; {post.upvote_count}
          </button>
          <button
            className={`vote-btn vote-down ${post.user_vote === 'DOWN' ? 'vote-active' : ''}`}
            onClick={() => handleVote('DOWN')}
            disabled={!isAuthenticated}
            title="Downvote"
          >
            &#9660; {post.downvote_count}
          </button>
          {!post.reposted_from && post.repost_count > 0 && (
            <span className="post-stat post-stat-repost">{post.repost_count} {post.repost_count === 1 ? 'repost' : 'reposts'}</span>
          )}
          <button className="share-btn" onClick={handleShare}>
            {copied ? 'Copied!' : 'Share'}
          </button>
          {isAuthenticated && !isAuthor && !post.user_has_reposted && !reposted && (
            <button
              className="share-btn"
              onClick={handleRepost}
            >
              {repostError || 'Repost'}
            </button>
          )}
          {(post.user_has_reposted || reposted) && !isAuthor && (
            <span className="post-stat post-stat-repost">Reposted</span>
          )}
          {isAuthenticated && !isAuthor && (
            <button className="btn btn-sm btn-report" onClick={() => setShowReport(true)}>
              Report
            </button>
          )}
        </div>
      </article>

      {/* Delete confirmation modal */}
      {showDeleteConfirm && (
        <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Delete Post</h3>
            <p className="modal-body-text">Are you sure you want to delete this post? This cannot be undone.</p>
            <div className="modal-actions">
              <button className="btn btn-danger btn-sm" onClick={handleDelete}>Delete</button>
              <button className="btn btn-secondary btn-sm" onClick={() => setShowDeleteConfirm(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* Report modal */}
      {showReport && (
        <div className="modal-overlay" onClick={() => setShowReport(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Report this post</h3>
            {reportMsg ? (
              <p className="report-msg">{reportMsg}</p>
            ) : (
              <>
                <div className="form-group">
                  <label htmlFor="report-reason">Reason</label>
                  <select
                    id="report-reason"
                    value={reportReason}
                    onChange={(e) => setReportReason(e.target.value)}
                  >
                    <option value="SPAM">Spam</option>
                    <option value="MISINFORMATION">Misinformation</option>
                    <option value="ABUSE">Abuse</option>
                    <option value="IRRELEVANT">Irrelevant</option>
                  </select>
                </div>
                <div className="modal-actions">
                  <button className="btn btn-primary btn-sm" onClick={handleReport}>Submit</button>
                  <button className="btn btn-secondary btn-sm" onClick={() => setShowReport(false)}>Cancel</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Comments */}
      <section className="comments-section">
        <h2 className="comments-heading">Comments ({post.comment_count})</h2>

        {isAuthenticated && (
          <form className="comment-form" onSubmit={handleCommentSubmit}>
            <textarea
              className="comment-input"
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="Write a comment..."
              rows={3}
            />
            <button
              type="submit"
              className="btn btn-primary btn-sm"
              disabled={commentSubmitting || !commentText.trim()}
            >
              {commentSubmitting ? 'Posting...' : 'Post Comment'}
            </button>
          </form>
        )}

        {comments.length === 0 ? (
          <p className="forum-empty">No comments yet. Be the first!</p>
        ) : (
          <div className="comment-list">
            {comments.map((c) => (
              <div className="comment-card" key={c.id}>
                <div className="comment-header">
                  <span className="comment-author">{c.author.full_name}</span>
                  <span className="post-card-dot">&middot;</span>
                  <span className="comment-time">{timeAgo(c.created_at)}</span>
                  {user && user.id === c.author.id && (
                    <button
                      className="comment-delete"
                      onClick={() => handleDeleteComment(c.id)}
                      title="Delete comment"
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

      {/* Image lightbox */}
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
