import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  addTutorialPostComment,
  getTutorialPostComments,
  getTutorialPostVotes,
  incrementTutorialPostVote,
} from '../utils/tutorialStorage';
import { getTutorialPostById } from '../utils/tutorialForumData';

export default function PostDetailPageTutorial() {
  const { id } = useParams();
  const navigate = useNavigate();
  const post = useMemo(() => getTutorialPostById(id), [id]);
  const [commentText, setCommentText] = useState('');
  const [commentOverrides, setCommentOverrides] = useState(() => getTutorialPostComments());
  const [voteOverrides, setVoteOverrides] = useState(() => getTutorialPostVotes());

  if (!post) {
    return (
      <div className="page post-detail-page tutorial-page">
        <button className="btn btn-secondary btn-sm post-back-link" onClick={() => navigate('/tutorial/forum')}>
          &larr; Forum tutorial
        </button>
        <p className="forum-empty">Post not found.</p>
      </div>
    );
  }

  const postKey = String(post.id);
  const extraVotes = voteOverrides[postKey] || { upvotes: 0, downvotes: 0 };
  const comments = [
    ...(commentOverrides[postKey] || []),
    ...(post.sampleComments || []).map((content, index) => ({
      id: `sample-comment-${post.id}-${index}`,
      author: index % 2 === 0 ? 'Aylin Neighbor' : 'Community Helper',
      content,
      createdLabel: 'recent update',
    })),
  ];

  const upvotes = (post.upvotes || 0) + (extraVotes.upvotes || 0);
  const downvotes = (post.downvotes || 0) + (extraVotes.downvotes || 0);

  const handleVote = (type) => {
    const nextVotes = incrementTutorialPostVote(post.id, type);
    setVoteOverrides((current) => ({ ...current, [postKey]: nextVotes }));
  };

  const handleCommentSubmit = (e) => {
    e.preventDefault();
    const trimmed = commentText.trim();
    if (!trimmed) return;
    const nextComments = addTutorialPostComment(post.id, trimmed);
    setCommentOverrides((current) => ({ ...current, [postKey]: nextComments }));
    setCommentText('');
  };

  return (
    <div className="page post-detail-page tutorial-page">
      <button className="btn btn-secondary btn-sm post-back-link" onClick={() => navigate('/tutorial/forum')}>
        &larr; Forum
      </button>

      <article className="post-article">
        <div className="post-header-row">
          <div>
            <span className="badge badge-global">{post.forumType || 'GLOBAL'}</span>
            {post.local && <span className="badge badge-accent">Your post</span>}
            <span className="badge badge-muted">Besiktas Neighborhood</span>
          </div>
        </div>

        <h1 className="post-title">{post.title}</h1>

        <div className="post-meta">
          <span className="author-link">{post.author}</span>
          {post.role === 'EXPERT' && <span className="badge badge-expert-responding">Expert</span>}
          <span className="badge" style={{ color: '#38bdf8', borderColor: '#38bdf844', background: '#38bdf811' }}>
            {post.status}
          </span>
          <span className="post-card-dot">&middot;</span>
          <span>{post.createdLabel || 'recent update'}</span>
        </div>

        <div className="post-content">{post.body}</div>

        {post.imageUrls?.length > 0 && (
          <div className="tutorial-preview-images">
            {post.imageUrls.map((url) => (
              <span key={url}>{url}</span>
            ))}
          </div>
        )}

        <div className="vote-bar">
          <button className="vote-btn vote-up" onClick={() => handleVote('upvotes')}>
            &#9650; {upvotes}
          </button>
          <button className="vote-btn vote-down" onClick={() => handleVote('downvotes')}>
            &#9660; {downvotes}
          </button>
          <span className="post-stat">{comments.length} comments</span>
        </div>
      </article>

      <section className="comments-section">
        <h2 className="comments-heading">Comments</h2>

        <form className="comment-form" onSubmit={handleCommentSubmit}>
          <textarea
            className="comment-input"
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            placeholder="Write a comment"
            rows={3}
          />
          <button type="submit" className="btn btn-primary btn-sm" disabled={!commentText.trim()}>
            Post comment
          </button>
        </form>

        <div className="comment-list">
          {comments.map((comment) => (
            <div className="comment-card" key={comment.id}>
              <div className="comment-header">
                <span className="comment-author">{comment.author}</span>
                {comment.local && <span className="badge badge-accent">Your comment</span>}
                <span className="post-card-dot">&middot;</span>
                <span className="comment-time">{comment.createdLabel}</span>
              </div>
              <p className="comment-body">{comment.content}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
