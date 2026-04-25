import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next'; // 1. Import hook
import {
  getProfile, updateProfile,
  getResources, createResource, updateResource, deleteResource,
  getExpertiseFields, createExpertiseField, updateExpertiseField, deleteExpertiseField,
  uploadImages, resolveImageUrl,
} from '../services/api';

function Chevron({ open }) {
  return (
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" style={{ transform: open ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s ease' }}>
        <path d="M3 6l5 5 5-5" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
  );
}

function AccordionSection({ title, icon, count, children }) {
  const [open, setOpen] = useState(false);
  return (
      <div className="profile-accordion">
        <button className="profile-accordion-header" onClick={() => setOpen((v) => !v)}>
        <span className="profile-accordion-title">
          <span className="profile-accordion-icon">{icon}</span>
          {title}
          {count > 0 && <span className="profile-count-badge">{count}</span>}
        </span>
          <Chevron open={open} />
        </button>
        {open && <div className="profile-accordion-body">{children}</div>}
      </div>
  );
}

// 2. Added 't' prop for the default placeholder
function InlineEditable({ value, name, onSave, multiline = false, placeholder, t }) {
  const [isEditing, setIsEditing] = useState(false);
  const [tempValue, setTempValue] = useState(value || '');
  const [saving, setSaving] = useState(false);

  useEffect(() => { setTempValue(value || ''); }, [value]);

  const handleSave = async () => {
    setIsEditing(false);
    if (tempValue === value) return;
    setSaving(true);
    await onSave(name, tempValue);
    setSaving(false);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !multiline) { e.preventDefault(); handleSave(); }
    if (e.key === 'Escape') { setTempValue(value || ''); setIsEditing(false); }
  };

  if (isEditing) {
    const props = {
      autoFocus: true, value: tempValue,
      onChange: (e) => setTempValue(e.target.value),
      onBlur: handleSave, onKeyDown: handleKeyDown,
      placeholder: placeholder // Added placeholder
    };
    return multiline ? <textarea {...props} className="inline-edit-input" rows={3} /> : <input {...props} className="inline-edit-input" />;
  }

  return (
      <span className={`inline-edit-value ${!value ? 'inline-edit-empty' : ''}`} style={{ opacity: saving ? 0.5 : 1 }} onClick={() => setIsEditing(true)}>
      {value || placeholder || t('profile.placeholders.click_to_add')}
    </span>
  );
}

const GSM_RE = /^\+?[\d][\d\s\-().]{5,18}\d$/;

// 3. Added 't' prop for validation errors and placeholders
function PhoneEditable({ value, name, onSave, t }) {
  const [isEditing, setIsEditing] = useState(false);
  const [tempValue, setTempValue] = useState(value || '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => { setTempValue(value || ''); }, [value]);

  const validate = (v) => {
    if (!v) return '';
    if (!GSM_RE.test(v)) return t('profile.phone_validation.invalid');
    const digits = v.replace(/\D/g, '');
    if (digits.length < 6 || digits.length > 15) return t('profile.phone_validation.length');
    return '';
  };

  const handleSave = async () => {
    const err = validate(tempValue);
    if (err) { setError(err); return; }
    setError('');
    setIsEditing(false);
    if (tempValue === value) return;
    setSaving(true);
    await onSave(name, tempValue || null);
    setSaving(false);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') { e.preventDefault(); handleSave(); }
    if (e.key === 'Escape') { setTempValue(value || ''); setError(''); setIsEditing(false); }
  };

  if (isEditing) {
    return (
        <span className="phone-edit-wrap">
        <input
            autoFocus
            type="tel"
            value={tempValue}
            placeholder="+90 555 123 4567"
            onChange={(e) => { setTempValue(e.target.value); setError(''); }}
            onBlur={handleSave}
            onKeyDown={handleKeyDown}
            className={`inline-edit-input ${error ? 'inline-edit-input-error' : ''}`}
        />
          {error && <span className="inline-field-error">{error}</span>}
      </span>
    );
  }

  return (
      <span className={`inline-edit-value ${!value ? 'inline-edit-empty' : ''}`} style={{ opacity: saving ? 0.5 : 1 }} onClick={() => setIsEditing(true)}>
      {value || t('profile.placeholders.click_to_add')}
    </span>
  );
}

const BLOOD_TYPES = ['A+', 'A−', 'B+', 'B−', 'AB+', 'AB−', 'O+', 'O−'];

// 4. Added 't' prop for the default option
function BloodTypeSelect({ value, name, onSave, t }) {
  const [saving, setSaving] = useState(false);

  const handleChange = async (e) => {
    const next = e.target.value || null;
    setSaving(true);
    await onSave(name, next);
    setSaving(false);
  };

  return (
      <select
          className="blood-type-select"
          value={value || ''}
          onChange={handleChange}
          disabled={saving}
          style={{ opacity: saving ? 0.5 : 1 }}
      >
        <option value="">{t('profile.placeholders.select_blood')}</option>
        {BLOOD_TYPES.map((bt) => (
            <option key={bt} value={bt}>{bt}</option>
        ))}
      </select>
  );
}

const EMPTY_RESOURCE = { name: '', category: '', quantity: 1, condition: true };
const EMPTY_EXPERTISE = { field: '', certification_level: 'BEGINNER', certification_document_url: '' };

export default function ProfilePage() {
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation(); // 5. Initialize hook

  // 6. Dynamic labels mapping
  const AVAILABILITY_LABELS = {
    SAFE: { label: t('user_profile.status.safe'), color: '#34d399' },
    NEEDS_HELP: { label: t('user_profile.status.needs_help'), color: '#f87171' },
    AVAILABLE_TO_HELP: { label: t('user_profile.status.available'), color: '#38bdf8' }
  };

  const [profile, setProfile] = useState({
    phone_number: '', blood_type: '', emergency_contact_phone: '',
    special_needs: '', has_disability: false, availability_status: 'SAFE',
    bio: '', preferred_language: '', emergency_contact: '',
  });

  const [resources, setResources] = useState([]);
  const [newResource, setNewResource] = useState(EMPTY_RESOURCE);
  const [showResourceForm, setShowResourceForm] = useState(false);

  const [expertiseFields, setExpertiseFields] = useState([]);
  const [newExpertise, setNewExpertise] = useState(EMPTY_EXPERTISE);
  const [showExpertiseForm, setShowExpertiseForm] = useState(false);
  const [certUploading, setCertUploading] = useState(false);
  const certFileRef = useRef(null);

  const [toast, setToast] = useState(null);
  const isExpert = user?.role === 'EXPERT';

  useEffect(() => {
    if (!user) return;
    getProfile().then(({ ok, data }) => {
      if (ok) setProfile({ phone_number: data.phone_number || '', blood_type: data.blood_type || '', emergency_contact_phone: data.emergency_contact_phone || '', special_needs: data.special_needs || '', has_disability: data.has_disability ?? false, availability_status: data.availability_status || 'SAFE', bio: data.bio || '', preferred_language: data.preferred_language || '', emergency_contact: data.emergency_contact || '' });
    });
    getResources().then(({ ok, data }) => { if (ok) setResources(data); });
    if (isExpert) getExpertiseFields().then(({ ok, data }) => { if (ok) setExpertiseFields(data); });
  }, [user, isExpert]);

  const notify = (msg, type = 'success') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  const onSaveField = async (field, value) => {
    setProfile((p) => ({ ...p, [field]: value }));
    const { ok, data } = await updateProfile({ [field]: value });
    if (ok) { notify(t('profile.toasts.saved')); refreshUser(); }
    else notify(data?.message || t('profile.toasts.update_failed'), 'error');
  };

  const onToggleField = async (field) => {
    const newVal = !profile[field];
    setProfile((p) => ({ ...p, [field]: newVal }));
    const { ok, data } = await updateProfile({ [field]: newVal });
    if (ok) { notify(t('profile.toasts.saved')); refreshUser(); }
    else notify(data?.message || t('profile.toasts.update_failed'), 'error');
  };

  const onSelectField = async (field, value) => {
    setProfile((p) => ({ ...p, [field]: value }));
    const { ok, data } = await updateProfile({ [field]: value });
    if (ok) { notify(t('profile.toasts.saved')); refreshUser(); }
    else notify(data?.message || t('profile.toasts.update_failed'), 'error');
  };

  const handleAddResource = async (e) => {
    e.preventDefault();
    const { ok, data } = await createResource({ ...newResource, quantity: Number(newResource.quantity) });
    if (ok) { setResources((p) => [...p, data]); setNewResource(EMPTY_RESOURCE); setShowResourceForm(false); notify(t('profile.toasts.resource_added')); }
    else notify(data?.message || t('profile.toasts.update_failed'), 'error');
  };

  const handleDeleteResource = async (id) => {
    const { ok } = await deleteResource(id);
    if (ok) { setResources((p) => p.filter((r) => r.id !== id)); notify(t('profile.toasts.resource_removed')); }
  };

  const handleToggleCondition = async (resource) => {
    const { ok, data } = await updateResource(resource.id, { condition: !resource.condition });
    if (ok) setResources((p) => p.map((r) => (r.id === resource.id ? data : r)));
  };

  const handleAddExpertise = async (e) => {
    e.preventDefault();
    const { ok, data } = await createExpertiseField({ ...newExpertise, certification_document_url: newExpertise.certification_document_url || null });
    if (ok) { setExpertiseFields((p) => [...p, data]); setNewExpertise(EMPTY_EXPERTISE); setShowExpertiseForm(false); notify(t('profile.toasts.expertise_added')); }
    else notify(data?.message || t('profile.toasts.update_failed'), 'error');
  };

  const handleDeleteExpertise = async (id) => {
    const { ok } = await deleteExpertiseField(id);
    if (ok) { setExpertiseFields((p) => p.filter((ef) => ef.id !== id)); notify(t('profile.toasts.expertise_removed')); }
  };

  const handleCertFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setCertUploading(true);
    const { ok, data } = await uploadImages([file]);
    if (ok && data.urls?.length) {
      setNewExpertise((p) => ({ ...p, certification_document_url: resolveImageUrl(data.urls[0]) }));
      notify(t('profile.toasts.file_uploaded'));
    } else {
      notify(t('profile.toasts.upload_failed'), 'error');
    }
    setCertUploading(false);
    if (certFileRef.current) certFileRef.current.value = '';
  };

  const statusMeta = AVAILABILITY_LABELS[profile.availability_status] || AVAILABILITY_LABELS.SAFE;
  const roleLabel = user?.role === 'EXPERT' ? t('user_profile.roles.expert') : t('user_profile.roles.standard');

  return (
      <div className="page profile-page">

        {toast && <div className={`profile-toast ${toast.type === 'error' ? 'profile-toast-error' : ''}`}>{toast.msg}</div>}

        <header className="dashboard-header">
          <h2>{t('profile.header.title')}</h2>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/dashboard')}>← {t('profile.header.back')}</button>
        </header>

        <div className="profile-identity-card">
          <div className="profile-avatar">{user?.full_name?.[0]?.toUpperCase() ?? '?'}</div>
          <div className="profile-identity-info">
            <h3 className="profile-name">{user?.full_name}</h3>
            <p className="profile-email">{user?.email}</p>
            <div className="profile-badges">
              <span className="badge badge-accent">{roleLabel}</span>
              <span className="badge" style={{ color: statusMeta.color, borderColor: statusMeta.color + '44', background: statusMeta.color + '11' }}>
              ● {statusMeta.label}
            </span>
            </div>
          </div>
        </div>

        <div className="profile-section-card profile-my-posts-card" onClick={() => navigate('/my-posts')} role="link" style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <h4 className="profile-section-title" style={{ margin: 0 }}>{t('profile.my_posts.title')}</h4>
            <span style={{ color: 'var(--accent)', fontSize: '1.2rem' }}>&rarr;</span>
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '0.25rem' }}>{t('profile.my_posts.desc')}</p>
        </div>

        <div className="profile-section-card">
          <h4 className="profile-section-title">{t('profile.sections.personal_info')}</h4>
          <div className="profile-fields-grid">
            <ProfileField label={t('profile.fields.phone')} icon="📞"><PhoneEditable name="phone_number" value={profile.phone_number} onSave={onSaveField} t={t} /></ProfileField>
            <ProfileField label={t('profile.fields.blood_type')} icon="🩸"><BloodTypeSelect name="blood_type" value={profile.blood_type} onSave={onSaveField} t={t} /></ProfileField>
            <ProfileField label={t('profile.fields.emergency_contact')} icon="👤"><InlineEditable name="emergency_contact" value={profile.emergency_contact} onSave={onSaveField} t={t} /></ProfileField>
            <ProfileField label={t('profile.fields.emergency_contact_phone')} icon="🚨"><PhoneEditable name="emergency_contact_phone" value={profile.emergency_contact_phone} onSave={onSaveField} t={t} /></ProfileField>
            <ProfileField label={t('profile.fields.preferred_language')} icon="🌐"><InlineEditable name="preferred_language" value={profile.preferred_language} onSave={onSaveField} t={t} /></ProfileField>
            <ProfileField label={t('profile.fields.has_disability')} icon="♿">
              <label className="toggle-label">
                <input type="checkbox" className="toggle-cb" checked={profile.has_disability} onChange={() => onToggleField('has_disability')} />
                <span className="toggle-track"><span className="toggle-thumb" /></span>
                <span className="toggle-text">{profile.has_disability ? t('profile.actions.yes') : t('profile.actions.no')}</span>
              </label>
            </ProfileField>
            <ProfileField label={t('profile.fields.availability')} icon="📡" fullWidth>
              <div className="status-selector">
                {Object.entries(AVAILABILITY_LABELS).map(([val, { label, color }]) => (
                    <button
                        key={val}
                        className={`status-option ${profile.availability_status === val ? 'status-option-active' : ''}`}
                        style={profile.availability_status === val ? { borderColor: color, color, background: color + '15' } : {}}
                        onClick={() => onSelectField('availability_status', val)}
                        type="button"
                    >
                      ● {label}
                    </button>
                ))}
              </div>
            </ProfileField>
            <ProfileField label={t('profile.fields.special_needs')} icon="📋" fullWidth>
              <InlineEditable name="special_needs" value={profile.special_needs} onSave={onSaveField} multiline placeholder={t('profile.placeholders.special_needs')} t={t} />
            </ProfileField>
            <ProfileField label={t('profile.fields.bio')} icon="✏️" fullWidth>
              <InlineEditable name="bio" value={profile.bio} onSave={onSaveField} multiline placeholder={t('profile.placeholders.bio')} t={t} />
            </ProfileField>
          </div>
        </div>

        <AccordionSection title={t('profile.sections.resources')} icon="📦" count={resources.length}>
          <div className="accordion-action-row">
            <button className="btn btn-secondary btn-sm" onClick={() => setShowResourceForm((v) => !v)}>
              {showResourceForm ? t('profile.actions.cancel') : t('profile.actions.add_resource')}
            </button>
          </div>

          {showResourceForm && (
              <form onSubmit={handleAddResource} className="profile-inline-form">
                <div className="form-row">
                  <div className="form-group">
                    <label>{t('profile.labels.name')}</label>
                    <input placeholder={t('profile.placeholders.resource_name')} value={newResource.name} onChange={(e) => setNewResource((p) => ({ ...p, name: e.target.value }))} required />
                  </div>
                  <div className="form-group">
                    <label>{t('profile.labels.category')}</label>
                    <input placeholder={t('profile.placeholders.resource_category')} value={newResource.category} onChange={(e) => setNewResource((p) => ({ ...p, category: e.target.value }))} required />
                  </div>
                  <div className="form-group form-group-sm">
                    <label>{t('profile.labels.quantity')}</label>
                    <input type="number" min="1" value={newResource.quantity} onChange={(e) => setNewResource((p) => ({ ...p, quantity: e.target.value }))} required />
                  </div>
                </div>
                <div className="form-check-row">
                  <label className="toggle-label">
                    <input type="checkbox" className="toggle-cb" checked={newResource.condition} onChange={(e) => setNewResource((p) => ({ ...p, condition: e.target.checked }))} />
                    <span className="toggle-track"><span className="toggle-thumb" /></span>
                    <span className="toggle-text">{t('profile.labels.functional')}</span>
                  </label>
                  <button className="btn btn-primary btn-sm" type="submit">{t('profile.actions.save')}</button>
                </div>
              </form>
          )}

          {resources.length === 0 ? (
              <p className="accordion-empty">{t('profile.empty.resources')}</p>
          ) : (
              <ul className="item-card-list">
                {resources.map((r) => (
                    <li key={r.id} className="item-card">
                      <div className="item-card-icon">📦</div>
                      <div className="item-card-body">
                        <span className="item-card-name">{r.name}</span>
                        <span className="item-card-meta">{r.category} · {t('profile.resource_item.qty', { count: r.quantity })}</span>
                      </div>
                      <div className="item-card-actions">
                        <button
                            className={`condition-badge ${r.condition ? 'condition-ok' : 'condition-bad'}`}
                            onClick={() => handleToggleCondition(r)}
                            title={t('profile.actions.click_to_toggle')}
                            type="button"
                        >
                          {r.condition ? `✓ ${t('profile.resource_item.functional')}` : `✗ ${t('profile.resource_item.not_functional')}`}
                        </button>
                        <button className="icon-btn icon-btn-danger" onClick={() => handleDeleteResource(r.id)} title={t('profile.actions.delete')} type="button">✕</button>
                      </div>
                    </li>
                ))}
              </ul>
          )}
        </AccordionSection>

        {isExpert && (
            <AccordionSection title={t('profile.sections.expertise')} icon="🎓" count={expertiseFields.length}>
              <div className="accordion-action-row">
                <button className="btn btn-secondary btn-sm" onClick={() => setShowExpertiseForm((v) => !v)}>
                  {showExpertiseForm ? t('profile.actions.cancel') : t('profile.actions.add_expertise')}
                </button>
              </div>

              {showExpertiseForm && (
                  <form onSubmit={handleAddExpertise} className="profile-inline-form">
                    <div className="form-row">
                      <div className="form-group">
                        <label>{t('profile.labels.field')}</label>
                        <input placeholder={t('profile.placeholders.expertise_field')} value={newExpertise.field} onChange={(e) => setNewExpertise((p) => ({ ...p, field: e.target.value }))} required />
                      </div>
                      <div className="form-group form-group-sm">
                        <label>{t('profile.labels.level')}</label>
                        <select value={newExpertise.certification_level} onChange={(e) => setNewExpertise((p) => ({ ...p, certification_level: e.target.value }))}>
                          <option value="BEGINNER">{t('profile.expertise_item.beginner')}</option>
                          <option value="ADVANCED">{t('profile.expertise_item.advanced')}</option>
                        </select>
                      </div>
                    </div>
                    <div className="form-group">
                      <label>{t('profile.labels.certificate')} <span className="optional-tag">{t('profile.labels.optional')}</span></label>
                      <div className="cert-upload-row">
                        <input placeholder={t('profile.placeholders.cert_url')} value={newExpertise.certification_document_url} onChange={(e) => setNewExpertise((p) => ({ ...p, certification_document_url: e.target.value }))} />
                        <input type="file" ref={certFileRef} accept="image/*,.pdf" onChange={handleCertFileUpload} style={{ display: 'none' }} />
                        <button type="button" className="btn btn-secondary btn-sm" disabled={certUploading} onClick={() => certFileRef.current?.click()}>
                          {certUploading ? t('profile.actions.uploading') : t('profile.actions.upload')}
                        </button>
                      </div>
                    </div>
                    <div className="form-check-row">
                      <button className="btn btn-primary btn-sm" type="submit">{t('profile.actions.save')}</button>
                    </div>
                  </form>
              )}

              {expertiseFields.length === 0 ? (
                  <p className="accordion-empty">{t('profile.empty.expertise')}</p>
              ) : (
                  <ul className="item-card-list">
                    {expertiseFields.map((ef) => (
                        <li key={ef.id} className="item-card">
                          <div className="item-card-icon">🎓</div>
                          <div className="item-card-body">
                            <span className="item-card-name">{ef.field}</span>
                            <span className="item-card-meta">
                      {ef.certification_level === 'ADVANCED'
                          ? `★ ${t('profile.expertise_item.advanced')}`
                          : `◎ ${t('profile.expertise_item.beginner')}`}
                    </span>
                          </div>
                          <div className="item-card-actions">
                            {ef.certification_document_url && (
                                <a href={ef.certification_document_url} target="_blank" rel="noreferrer" className="cert-link">
                                  {t('profile.expertise_item.certificate')} ↗
                                </a>
                            )}
                            <button className="icon-btn icon-btn-danger" onClick={() => handleDeleteExpertise(ef.id)} title={t('profile.actions.delete')} type="button">✕</button>
                          </div>
                        </li>
                    ))}
                  </ul>
              )}
            </AccordionSection>
        )}

      </div>
  );
}

function ProfileField({ label, icon, children, fullWidth = false }) {
  return (
      <div className={`profile-field ${fullWidth ? 'profile-field-full' : ''}`}>
        <span className="profile-field-label"><span className="profile-field-icon">{icon}</span>{label}</span>
        <span className="profile-field-value">{children}</span>
      </div>
  );
}