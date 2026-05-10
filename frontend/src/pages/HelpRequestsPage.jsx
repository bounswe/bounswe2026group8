/**
 * HelpRequestsPage — tabbed page showing help requests and help offers.
 *
 * Two tabs:
 * "Requests" — fetches GET /help-requests/ and shows clickable cards.
 * "Offers"   — fetches GET /help-offers/, shows offer cards with delete,
 * and includes an inline creation form.
 *
 * Both tabs support category filtering via a shared filter bar.
 */

import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  getHelpRequests,
  getHelpOffers,
  createHelpOffer,
  deleteHelpOffer,
} from '../services/api';
import { useTranslation } from 'react-i18next';

/** Maps urgency values to badge CSS classes for visual distinction. */
const URGENCY_CLASSES = {
  LOW: 'badge-muted',
  MEDIUM: 'badge-accent',
  HIGH: 'badge-urgency-high',
};

// Pass 't' into the date formatter
function formatDate(isoString, t) {
  const date = new Date(isoString);
  const diffMs = Date.now() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return t('help_requests.time.just_now');
  if (diffMins < 60) return t('help_requests.time.m_ago', { count: diffMins });
  if (diffHours < 24) return t('help_requests.time.h_ago', { count: diffHours });
  if (diffDays < 7) return t('help_requests.time.d_ago', { count: diffDays });
  return date.toLocaleDateString();
}

const STATUS_COLORS = { SAFE: '#34d399', NEEDS_HELP: '#f87171', AVAILABLE_TO_HELP: '#38bdf8' };

// Pass 't' into AuthorStatus
function AuthorStatus({ profile, t }) {
  const s = profile?.availability_status;
  if (!s || !STATUS_COLORS[s]) return null;
  const c = STATUS_COLORS[s];

  const STATUS_LABELS_MAP = {
    SAFE: t('help_requests.availability_status.safe'),
    NEEDS_HELP: t('help_requests.availability_status.needs_help'),
    AVAILABLE_TO_HELP: t('help_requests.availability_status.available')
  };

  return <span className="badge" style={{ color: c, borderColor: c + '44', background: c + '11', fontSize: '0.7rem', padding: '1px 6px' }}>● {STATUS_LABELS_MAP[s]}</span>;
}

export default function HelpRequestsPage() {
  const { user } = useAuth();
  const selectedHub = user?.hub;
  const navigate = useNavigate();
  const isExpert = user?.role === 'EXPERT';
  const { t } = useTranslation();

  const CATEGORIES = [
    { value: '', label: t('help_requests.categories.all') },
    { value: 'MEDICAL', label: t('help_requests.categories.medical') },
    { value: 'FOOD', label: t('help_requests.categories.food') },
    { value: 'SHELTER', label: t('help_requests.categories.shelter') },
    { value: 'TRANSPORT', label: t('help_requests.categories.transport') },
    { value: 'OTHER', label: t('help_requests.categories.other') },
  ];

  const FORM_CATEGORIES = CATEGORIES.filter((c) => c.value !== '');

  const URGENCY_LABELS = {
    LOW: t('help_requests.urgency.low'),
    MEDIUM: t('help_requests.urgency.medium'),
    HIGH: t('help_requests.urgency.high'),
  };

  const STATUS_LABELS = {
    OPEN: t('help_requests.status.open'),
    EXPERT_RESPONDING: t('help_requests.status.expert_responding'),
    RESOLVED: t('help_requests.status.resolved'),
  };

  const [activeTab, setActiveTab] = useState('requests');
  const [category, setCategory] = useState('');

  const [expertiseMatch, setExpertiseMatch] = useState(true);

  const [requests, setRequests] = useState([]);
  const [reqLoading, setReqLoading] = useState(true);
  const [reqError, setReqError] = useState('');

  const [offers, setOffers] = useState([]);
  const [offLoading, setOffLoading] = useState(true);
  const [offError, setOffError] = useState('');
  const [deleting, setDeleting] = useState(null);
  const [selectedOffer, setSelectedOffer] = useState(null);

  const [showOfferForm, setShowOfferForm] = useState(false);
  const [offerForm, setOfferForm] = useState({
    category: 'MEDICAL',
    skill_or_resource: '',
    description: '',
    availability: '',
  });
  const [offerFormErrors, setOfferFormErrors] = useState({});
  const [offerFormGlobal, setOfferFormGlobal] = useState('');
  const [offerSubmitting, setOfferSubmitting] = useState(false);

  useEffect(() => {
    if (!user || activeTab !== 'requests') return;

    setReqLoading(true);
    setReqError('');

    const params = {};
    if (selectedHub?.id) params.hub_id = selectedHub.id;
    if (category) params.category = category;
    if (isExpert && expertiseMatch) params.expertise_match = true;

    getHelpRequests(params)
      .then(({ ok, data }) => {
        if (ok) setRequests(data);
        else setReqError(data.detail || t('help_requests.requests_tab.error_fetch'));
      })
      .catch(() => setReqError(t('help_requests.requests_tab.error_network')))
      .finally(() => setReqLoading(false));
  }, [user, category, activeTab, expertiseMatch, isExpert, selectedHub, t]);

  useEffect(() => {
    if (!user || activeTab !== 'offers') return;

    setOffLoading(true);
    setOffError('');

    const params = {};
    if (selectedHub?.id) params.hub_id = selectedHub.id;
    if (category) params.category = category;

    getHelpOffers(params)
        .then(({ ok, data }) => {
          if (ok) setOffers(data);
          else setOffError(data.detail || t('help_requests.offers_tab.error_fetch'));
        })
        .catch(() => setOffError(t('help_requests.offers_tab.error_network')))
        .finally(() => setOffLoading(false));
  }, [user, category, activeTab, selectedHub, t]);

  const handleOfferChange = (e) => {
    const { name, value } = e.target;
    setOfferForm((prev) => ({ ...prev, [name]: value }));
    if (offerFormErrors[name]) {
      setOfferFormErrors((prev) => {
        const copy = { ...prev };
        delete copy[name];
        return copy;
      });
    }
  };

  const validateOffer = () => {
    const errs = {};
    if (!offerForm.skill_or_resource.trim()) errs.skill_or_resource = t('help_requests.offers_tab.form.error_skill');
    if (!offerForm.description.trim()) errs.description = t('help_requests.offers_tab.form.error_desc');
    if (!offerForm.availability.trim()) errs.availability = t('help_requests.offers_tab.form.error_avail');
    return errs;
  };

  const handleOfferSubmit = async (e) => {
    e.preventDefault();
    setOfferFormGlobal('');
    setOfferFormErrors({});

    const clientErrors = validateOffer();
    if (Object.keys(clientErrors).length > 0) {
      setOfferFormErrors(clientErrors);
      return;
    }

    setOfferSubmitting(true);

    const payload = {
      category: offerForm.category,
      skill_or_resource: offerForm.skill_or_resource.trim(),
      description: offerForm.description.trim(),
      availability: offerForm.availability.trim(),
    };

    const { ok, data } = await createHelpOffer(payload);
    setOfferSubmitting(false);

    if (ok) {
      setOffers((prev) => [data, ...prev]);
      setOfferForm({ category: 'MEDICAL', skill_or_resource: '', description: '', availability: '' });
      setShowOfferForm(false);
    } else {
      if (typeof data === 'object' && data !== null) {
        const mapped = {};
        for (const [field, msgs] of Object.entries(data)) {
          if (field === 'detail' || field === 'non_field_errors') continue;
          mapped[field] = Array.isArray(msgs) ? msgs.join(' ') : msgs;
        }
        if (Object.keys(mapped).length > 0) setOfferFormErrors(mapped);
      }
      setOfferFormGlobal(
          data.detail || data.non_field_errors?.[0] || t('help_requests.offers_tab.form.error_submit')
      );
    }
  };

  const handleDeleteOffer = async (offerId) => {
    setDeleting(offerId);
    const { ok } = await deleteHelpOffer(offerId);
    if (ok) setOffers((prev) => prev.filter((o) => o.id !== offerId));
    setDeleting(null);
  };

  if (!user) return null;

  return (
      <div className="page help-requests-page">
        <header className="help-requests-header page-main-header">
          <button
              className="btn btn-secondary btn-sm"
              onClick={() => navigate('/dashboard')}
          >
            &larr; {t('help_requests.header.back')}
          </button>
          <h2 className="gradient-text">{t('help_requests.header.title')}</h2>

          {activeTab === 'requests' ? (
              <button
                  className="btn btn-primary btn-sm"
                  onClick={() => navigate('/help-requests/new')}
              >
                {t('help_requests.header.new_request')}
              </button>
          ) : (
              <button
                  className="btn btn-primary btn-sm"
                  onClick={() => setShowOfferForm(!showOfferForm)}
              >
                {showOfferForm ? t('help_requests.header.cancel') : t('help_requests.header.new_offer')}
              </button>
          )}
        </header>

        <div className="help-tabs">
          <button
              className={`help-tab ${activeTab === 'requests' ? 'help-tab-active' : ''}`}
              onClick={() => setActiveTab('requests')}
          >
            {t('help_requests.tabs.requests')}
          </button>
          <button
              className={`help-tab ${activeTab === 'offers' ? 'help-tab-active' : ''}`}
              onClick={() => setActiveTab('offers')}
          >
            {t('help_requests.tabs.offers')}
          </button>
        </div>

        {selectedHub && (
            <p className="help-requests-hub">
              {t('help_requests.hub_context', { tab: activeTab === 'requests' ? t('help_requests.tabs.requests') : t('help_requests.tabs.offers') })} <strong>{selectedHub.name}</strong>
            </p>
        )}

        <div className="help-requests-filters">
          {CATEGORIES.map((cat) => (
              <button
                  key={cat.value}
                  className={`btn btn-sm ${!expertiseMatch && category === cat.value ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => { setCategory(cat.value); setExpertiseMatch(false); }}
              >
                {cat.label}
              </button>
          ))}
          {isExpert && (
              <button
                  className={`btn btn-sm ${expertiseMatch ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => { setExpertiseMatch(true); setCategory(''); }}
              >
                {t('help_requests.my_expertise')}
              </button>
          )}
        </div>

        {/* ══════════════════════════ REQUESTS TAB ══════════════════════════════ */}
        {activeTab === 'requests' && (
            <>
              {reqError && <div className="alert alert-error">{reqError}</div>}

              {reqLoading && (
                  <div className="help-requests-loading"><p>{t('help_requests.requests_tab.loading')}</p></div>
              )}

              {!reqLoading && !reqError && requests.length === 0 && (
                  <div className="help-requests-empty">
                    <span className="help-requests-empty-icon">🔍</span>
                    <h3>{t('help_requests.requests_tab.empty_title')}</h3>
                    <p>
                      {category
                          ? t('help_requests.requests_tab.empty_filter')
                          : t('help_requests.requests_tab.empty_hub')}
                    </p>
                  </div>
              )}

              {!reqLoading && !reqError && requests.length > 0 && (
                  <div className="help-requests-list">
                    {requests.map((req) => (
                        <div
                            className="help-request-card dashboard-card-link"
                            key={req.id}
                            onClick={() => navigate(`/help-requests/${req.id}`)}
                        >
                          <div className="help-request-card-top">
                            <h3 className="help-request-card-title">{req.title}</h3>
                            <span className={`badge ${URGENCY_CLASSES[req.urgency] || 'badge-muted'}`}>
                      {URGENCY_LABELS[req.urgency] || req.urgency}
                    </span>
                          </div>

                          <div className="help-request-card-meta">
                            <span className="badge">{t(`help_requests.categories.${req.category.toLowerCase()}`)}</span>
                            <span className={`badge ${req.status === 'RESOLVED' ? 'badge-resolved' : 'badge-muted'}`}>
                      {STATUS_LABELS[req.status] || req.status}
                    </span>
                          </div>

                          <div className="help-request-card-footer">
                            <Link to={`/users/${req.author.id}`} className="help-request-card-author author-link" onClick={(e) => e.stopPropagation()}>{req.author.full_name}</Link>
                            {req.author.role === 'EXPERT' && <span className="badge badge-expert-responding">{t('help_requests.labels.expert')}</span>}
                            <AuthorStatus profile={req.author.profile} t={t} />
                            <span>{formatDate(req.created_at, t)}</span>
                            {req.comment_count > 0 && (
                                <span className="help-request-card-comments">
                        💬 {req.comment_count}
                      </span>
                            )}
                          </div>
                        </div>
                    ))}
                  </div>
              )}
            </>
        )}

        {/* ══════════════════════════ OFFERS TAB ════════════════════════════════ */}
        {activeTab === 'offers' && (
            <>
              {showOfferForm && (
                  <div className="help-offer-form-card">
                    <h3 className="help-detail-section-title">{t('help_requests.offers_tab.form.title')}</h3>

                    {offerFormGlobal && <div className="alert alert-error">{offerFormGlobal}</div>}

                    <form onSubmit={handleOfferSubmit} noValidate>
                      <div className="form-group">
                        <label htmlFor="offer-category">{t('help_requests.offers_tab.form.category')}</label>
                        <select
                            id="offer-category"
                            name="category"
                            value={offerForm.category}
                            onChange={handleOfferChange}
                        >
                          {FORM_CATEGORIES.map((c) => (
                              <option key={c.value} value={c.value}>{c.label}</option>
                          ))}
                        </select>
                      </div>

                      <div className="form-group">
                        <label htmlFor="offer-skill">{t('help_requests.offers_tab.form.skill')}</label>
                        <input
                            id="offer-skill"
                            name="skill_or_resource"
                            type="text"
                            placeholder={t('help_requests.offers_tab.form.skill_placeholder')}
                            value={offerForm.skill_or_resource}
                            onChange={handleOfferChange}
                            className={offerFormErrors.skill_or_resource ? 'input-error' : ''}
                        />
                        {offerFormErrors.skill_or_resource && (
                            <span className="field-error">{offerFormErrors.skill_or_resource}</span>
                        )}
                      </div>

                      <div className="form-group">
                        <label htmlFor="offer-description">{t('help_requests.offers_tab.form.desc')}</label>
                        <textarea
                            id="offer-description"
                            name="description"
                            className={`help-create-textarea${offerFormErrors.description ? ' input-error' : ''}`}
                            placeholder={t('help_requests.offers_tab.form.desc_placeholder')}
                            value={offerForm.description}
                            onChange={handleOfferChange}
                            rows={3}
                        />
                        {offerFormErrors.description && (
                            <span className="field-error">{offerFormErrors.description}</span>
                        )}
                      </div>

                      <div className="form-group">
                        <label htmlFor="offer-availability">{t('help_requests.offers_tab.form.availability')}</label>
                        <select
                            id="offer-availability"
                            name="availability"
                            value={offerForm.availability}
                            onChange={handleOfferChange}
                            className={offerFormErrors.availability ? 'input-error' : ''}
                        >
                          <option value="">{t('help_requests.offers_tab.form.avail_placeholder')}</option>
                          <option value="24/7">{t('help_requests.offers_tab.avail_options.24_7')}</option>
                          <option value="Weekdays">{t('help_requests.offers_tab.avail_options.weekdays')}</option>
                          <option value="Weekends">{t('help_requests.offers_tab.avail_options.weekends')}</option>
                          <option value="Mornings">{t('help_requests.offers_tab.avail_options.mornings')}</option>
                          <option value="Evenings">{t('help_requests.offers_tab.avail_options.evenings')}</option>
                          <option value="On-call">{t('help_requests.offers_tab.avail_options.on_call')}</option>
                        </select>
                        {offerFormErrors.availability && (
                            <span className="field-error">{offerFormErrors.availability}</span>
                        )}
                      </div>

                      <button
                          type="submit"
                          className="btn btn-primary btn-sm"
                          disabled={offerSubmitting}
                      >
                        {offerSubmitting ? t('help_requests.offers_tab.form.posting') : t('help_requests.offers_tab.form.post')}
                      </button>
                    </form>
                  </div>
              )}

              {offError && <div className="alert alert-error">{offError}</div>}

              {offLoading && (
                  <div className="help-requests-loading"><p>{t('help_requests.offers_tab.loading')}</p></div>
              )}

              {!offLoading && !offError && offers.length === 0 && (
                  <div className="help-requests-empty">
                    <span className="help-requests-empty-icon">🤝</span>
                    <h3>{t('help_requests.offers_tab.empty_title')}</h3>
                    <p>
                      {category
                          ? t('help_requests.offers_tab.empty_filter')
                          : t('help_requests.offers_tab.empty_hub')}
                    </p>
                  </div>
              )}

              {!offLoading && !offError && offers.length > 0 && (
                  <div className="help-requests-list">
                    {offers.map((offer) => (
                        <div
                            className="help-offer-card dashboard-card-link"
                            key={offer.id}
                            onClick={() => setSelectedOffer(offer)}
                        >
                          <div className="help-request-card-top">
                            <h3 className="help-request-card-title">{offer.skill_or_resource}</h3>
                            <span className="badge">{t(`help_requests.categories.${offer.category.toLowerCase()}`)}</span>
                          </div>

                          <p className="help-offer-card-desc">{offer.description}</p>

                          <div className="help-offer-card-footer">
                            <Link to={`/users/${offer.author.id}`} className="help-request-card-author author-link" onClick={(e) => e.stopPropagation()}>{offer.author.full_name}</Link>
                            {offer.author.role === 'EXPERT' && <span className="badge badge-expert-responding">{t('help_requests.labels.expert')}</span>}
                            <AuthorStatus profile={offer.author.profile} t={t} />

                            {/* The availability string comes from the user input, but we can attempt to translate it if it matches our standard options */}
                            <span className="help-offer-card-avail">
                       {t(`help_requests.offers_tab.avail_options.${offer.availability.toLowerCase().replace(/[\/\s-]/g, '_')}`, { defaultValue: offer.availability })}
                    </span>

                            <span>{formatDate(offer.created_at, t)}</span>

                            {offer.author.id === user.id && (
                                <button
                                    className="btn btn-secondary btn-sm help-offer-delete-btn"
                                    onClick={(e) => { e.stopPropagation(); handleDeleteOffer(offer.id); }}
                                    disabled={deleting === offer.id}
                                >
                                  {deleting === offer.id ? t('help_requests.labels.deleting') : t('help_requests.labels.delete')}
                                </button>
                            )}
                          </div>
                        </div>
                    ))}
                  </div>
              )}
            </>
        )}

        {/* ══════════════════════════ OFFER DETAIL MODAL ════════════════════════ */}
        {selectedOffer && (
            <div className="offer-modal-overlay" onClick={() => setSelectedOffer(null)}>
              <div className="offer-modal" onClick={(e) => e.stopPropagation()}>
                <button
                    className="offer-modal-close"
                    onClick={() => setSelectedOffer(null)}
                >
                  &times;
                </button>

                <h2 className="offer-modal-title">{selectedOffer.skill_or_resource}</h2>

                <span className="badge">{t(`help_requests.categories.${selectedOffer.category.toLowerCase()}`)}</span>

                <p className="offer-modal-description">{selectedOffer.description}</p>

                <div className="offer-modal-details">
                  <div className="offer-modal-row">
                    <span className="offer-modal-label">{t('help_requests.offers_tab.modal.availability')}</span>
                    <span className="help-offer-card-avail">
                   {t(`help_requests.offers_tab.avail_options.${selectedOffer.availability.toLowerCase().replace(/[\/\s-]/g, '_')}`, { defaultValue: selectedOffer.availability })}
                </span>
                  </div>
                  <div className="offer-modal-row">
                    <span className="offer-modal-label">{t('help_requests.offers_tab.modal.offered_by')}</span>
                    <strong>{selectedOffer.author.full_name}</strong>
                  </div>
                  <div className="offer-modal-row">
                    <span className="offer-modal-label">{t('help_requests.offers_tab.modal.posted')}</span>
                    <span>{formatDate(selectedOffer.created_at, t)}</span>
                  </div>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}
