import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import useTutorialGuide from '../components/TutorialGuide';
import {
  addTutorialHelpComment,
  deleteTutorialHelpRequest,
  getTutorialHelpComments,
  updateTutorialHelpRequestStatus,
} from '../utils/tutorialStorage';
import { getTutorialHelpRequestById } from '../utils/tutorialHelpData';

const HELP_DETAIL_TOUR_STEPS = [
  {
    target: 'summary',
    title: 'Review the request',
    text: 'Start with the category, urgency, description, and who posted it.',
  },
  {
    target: 'location',
    title: 'Check the location',
    text: 'The location note helps helpers understand where support is needed.',
  },
  {
    target: 'comments',
    title: 'Coordinate in comments',
    text: 'Comments are useful for asking follow-up questions or offering help.',
  },
  {
    target: 'actions',
    title: 'Manage your request',
    text: 'You can only manage requests you created. When the need is handled, mark it as resolved or remove it if it is no longer needed.',
  },
];

const URGENCY_CLASSES = {
  LOW: 'badge-muted',
  MEDIUM: 'badge-accent',
  HIGH: 'badge-urgency-high',
};

const CATEGORY_LABELS = {
  MEDICAL: 'Medical',
  FOOD: 'Food / water',
  SHELTER: 'Shelter',
  TRANSPORT: 'Transport',
  OTHER: 'Other',
};

export default function HelpRequestDetailPageTutorial() {
  const { id } = useParams();
  const navigate = useNavigate();
  const request = useMemo(() => getTutorialHelpRequestById(id), [id]);
  const [currentRequest, setCurrentRequest] = useState(request);
  const [commentText, setCommentText] = useState('');
  const [commentOverrides, setCommentOverrides] = useState(() => getTutorialHelpComments());
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const { activeStep, GuidePanel, RestartButton } = useTutorialGuide({
    storageKey: 'emergencyHubHelpDetailTutorialSeen',
    steps: HELP_DETAIL_TOUR_STEPS,
  });

  if (!currentRequest) {
    return (
      <div className="page help-detail-page tutorial-page">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/tutorial/help-requests')}>
          &larr; Help Requests
        </button>
        <p className="forum-empty">Request not found.</p>
      </div>
    );
  }

  const requestKey = String(currentRequest.id);
  const comments = [
    ...(commentOverrides[requestKey] || []),
    ...(currentRequest.comments || []).map((content, index) => ({
      id: `help-comment-${currentRequest.id}-${index}`,
      author: index % 2 === 0 ? 'Community Helper' : 'Aylin Neighbor',
      content,
      createdLabel: 'recent update',
    })),
  ];

  const handleCommentSubmit = (e) => {
    e.preventDefault();
    const trimmed = commentText.trim();
    if (!trimmed) return;
    const nextComments = addTutorialHelpComment(currentRequest.id, trimmed);
    setCommentOverrides((current) => ({ ...current, [requestKey]: nextComments }));
    setCommentText('');
  };

  const handleResolve = () => {
    const updated = updateTutorialHelpRequestStatus(currentRequest.id, 'Resolved');
    setCurrentRequest(updated || { ...currentRequest, status: 'Resolved' });
  };

  const handleDelete = () => {
    deleteTutorialHelpRequest(currentRequest.id);
    navigate('/tutorial/help-requests');
  };

  const isOwnRequest = Boolean(currentRequest.local);

  return (
    <div className="page help-detail-page tutorial-page">
      <header className="help-requests-header">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/tutorial/help-requests')}>
          &larr; Help Requests
        </button>
        <h2 className="gradient-text">Help Request</h2>
        <div className="tutorial-header-actions">
          {RestartButton}
        </div>
      </header>

      {GuidePanel}

      <div className={`help-detail-card ${activeStep?.target === 'summary' ? 'tutorial-tour-highlight' : ''}`}>
        <h1 className="help-detail-title">{currentRequest.title}</h1>

        <div className="help-detail-badges">
          {currentRequest.local && <span className="badge badge-accent">Your request</span>}
          <span className="badge">{CATEGORY_LABELS[currentRequest.category] || currentRequest.category}</span>
          <span className={`badge ${URGENCY_CLASSES[currentRequest.urgency] || 'badge-muted'}`}>
            {currentRequest.urgency.charAt(0) + currentRequest.urgency.slice(1).toLowerCase()}
          </span>
          <span className={`badge ${currentRequest.status === 'Resolved' ? 'badge-resolved' : 'badge-muted'}`}>
            {currentRequest.status}
          </span>
        </div>

        <p className="help-detail-description">{currentRequest.description}</p>

        <div className="help-detail-meta">
          <span>By <strong>{currentRequest.author}</strong></span>
          <span>{currentRequest.createdLabel}</span>
        </div>

        {isOwnRequest && (
          <div className={`post-owner-actions ${activeStep?.target === 'actions' ? 'tutorial-tour-highlight' : ''}`}>
            {currentRequest.status !== 'Resolved' && (
              <button className="btn btn-primary btn-sm help-detail-resolve-btn" onClick={handleResolve}>
                Mark as resolved
              </button>
            )}
            <button className="btn btn-danger btn-sm" onClick={() => setShowDeleteConfirm(true)}>
              Delete
            </button>
          </div>
        )}
      </div>

      {showDeleteConfirm && (
        <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Delete request?</h3>
            <p className="modal-body-text">This removes the request from the list.</p>
            <div className="modal-actions">
              <button className="btn btn-danger btn-sm" onClick={handleDelete}>Delete</button>
              <button className="btn btn-secondary btn-sm" onClick={() => setShowDeleteConfirm(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      <div className={`help-detail-card ${activeStep?.target === 'location' ? 'tutorial-tour-highlight' : ''}`}>
        <h3 className="help-detail-section-title">Location</h3>
        <p className="help-detail-location-text">{currentRequest.location_text}</p>
      </div>

      <div className={`help-detail-card ${activeStep?.target === 'comments' ? 'tutorial-tour-highlight' : ''}`}>
        <h3 className="help-detail-section-title">Comments ({comments.length})</h3>

        {comments.length === 0 && (
          <p className="help-detail-no-comments">No comments yet.</p>
        )}

        {comments.length > 0 && (
          <div className="help-detail-comments">
            {comments.map((comment) => (
              <div className="help-detail-comment" key={comment.id}>
                <div className="help-detail-comment-header">
                  <strong>{comment.author}</strong>
                  {comment.local && <span className="badge badge-accent">Your comment</span>}
                  <div className="comment-right-group">
                    <span className="help-detail-comment-date">{comment.createdLabel}</span>
                  </div>
                </div>
                <p>{comment.content}</p>
              </div>
            ))}
          </div>
        )}

        <form className="help-detail-comment-form" onSubmit={handleCommentSubmit}>
          <textarea
            className="help-detail-textarea"
            placeholder="Write a comment"
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            rows={3}
          />
          <button className="btn btn-primary btn-sm" type="submit" disabled={!commentText.trim()}>
            Post comment
          </button>
        </form>
      </div>
    </div>
  );
}
