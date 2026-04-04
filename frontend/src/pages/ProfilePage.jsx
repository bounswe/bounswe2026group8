import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  getProfile, updateProfile,
  getResources, createResource, updateResource, deleteResource,
  getExpertiseFields, createExpertiseField, updateExpertiseField, deleteExpertiseField,
} from '../services/api';

/* ── Chevron icon ───────────────────────────────────────────────────────────── */
function Chevron({ open }) {
  return (
    <svg
      width="16" height="16" viewBox="0 0 16 16" fill="none"
      style={{ transform: open ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s ease' }}
    >
      <path d="M3 6l5 5 5-5" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

/* ── Accordion section ──────────────────────────────────────────────────────── */
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

/* ── Inline editable field ──────────────────────────────────────────────────── */
function InlineEditable({ value, name, onSave, multiline = false, placeholder = 'Click to add' }) {
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
    };
    return multiline ? <textarea {...props} className="inline-edit-input" rows={3} /> : <input {...props} className="inline-edit-input" />;
  }

  return (
    <span className={`inline-edit-value ${!value ? 'inline-edit-empty' : ''}`} style={{ opacity: saving ? 0.5 : 1 }} onClick={() => setIsEditing(true)}>
      {value || placeholder}
    </span>
  );
}

/* ── Phone editable ────────────────────────────────────────────────────────── */
// Accepts E.164 and common local formats: +CountryCode digits, spaces, dashes, parens
const GSM_RE = /^\+?[\d][\d\s\-().]{5,18}\d$/;

function PhoneEditable({ value, name, onSave }) {
  const [isEditing, setIsEditing] = useState(false);
  const [tempValue, setTempValue] = useState(value || '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => { setTempValue(value || ''); }, [value]);

  const validate = (v) => {
    if (!v) return ''; // blank is allowed (cleared)
    if (!GSM_RE.test(v)) return 'Enter a valid phone number (e.g. +90 555 123 4567)';
    const digits = v.replace(/\D/g, '');
    if (digits.length < 6 || digits.length > 15) return 'Must be 6–15 digits';
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
      {value || 'Click to add'}
    </span>
  );
}

/* ── Blood type dropdown ────────────────────────────────────────────────────── */
const BLOOD_TYPES = ['A+', 'A−', 'B+', 'B−', 'AB+', 'AB−', 'O+', 'O−'];

function BloodTypeSelect({ value, name, onSave }) {
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
      <option value="">— select —</option>
      {BLOOD_TYPES.map((bt) => (
        <option key={bt} value={bt}>{bt}</option>
      ))}
    </select>
  );
}

/* ── Constants ──────────────────────────────────────────────────────────────── */
const EMPTY_RESOURCE = { name: '', category: '', quantity: 1, condition: true };
const EMPTY_EXPERTISE = { field: '', certification_level: 'BEGINNER', certification_document_url: '' };

const AVAILABILITY_LABELS = { SAFE: { label: 'Safe', color: '#34d399' }, NEEDS_HELP: { label: 'Needs Help', color: '#f87171' }, AVAILABLE_TO_HELP: { label: 'Available to Help', color: '#38bdf8' } };

/* ═══════════════════════════════════════════════════════════════════════════════
   ProfilePage
   ═══════════════════════════════════════════════════════════════════════════════ */
export default function ProfilePage() {
  const { user } = useAuth();
  const navigate = useNavigate();

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
    ok ? notify('Saved') : notify(data?.message || 'Update failed', 'error');
  };

  const onToggleField = async (field) => {
    const newVal = !profile[field];
    setProfile((p) => ({ ...p, [field]: newVal }));
    const { ok, data } = await updateProfile({ [field]: newVal });
    ok ? notify('Saved') : notify(data?.message || 'Update failed', 'error');
  };

  const onSelectField = async (field, value) => {
    setProfile((p) => ({ ...p, [field]: value }));
    const { ok, data } = await updateProfile({ [field]: value });
    ok ? notify('Saved') : notify(data?.message || 'Update failed', 'error');
  };

  /* Resources */
  const handleAddResource = async (e) => {
    e.preventDefault();
    const { ok, data } = await createResource({ ...newResource, quantity: Number(newResource.quantity) });
    if (ok) { setResources((p) => [...p, data]); setNewResource(EMPTY_RESOURCE); setShowResourceForm(false); notify('Resource added'); }
    else notify(data?.message || 'Failed to add resource', 'error');
  };

  const handleDeleteResource = async (id) => {
    const { ok } = await deleteResource(id);
    if (ok) { setResources((p) => p.filter((r) => r.id !== id)); notify('Resource removed'); }
  };

  const handleToggleCondition = async (resource) => {
    const { ok, data } = await updateResource(resource.id, { condition: !resource.condition });
    if (ok) setResources((p) => p.map((r) => (r.id === resource.id ? data : r)));
  };

  /* Expertise */
  const handleAddExpertise = async (e) => {
    e.preventDefault();
    const { ok, data } = await createExpertiseField({ ...newExpertise, certification_document_url: newExpertise.certification_document_url || null });
    if (ok) { setExpertiseFields((p) => [...p, data]); setNewExpertise(EMPTY_EXPERTISE); setShowExpertiseForm(false); notify('Expertise added'); }
    else notify(data?.message || 'Failed to add expertise', 'error');
  };

  const handleDeleteExpertise = async (id) => {
    const { ok } = await deleteExpertiseField(id);
    if (ok) { setExpertiseFields((p) => p.filter((ef) => ef.id !== id)); notify('Expertise removed'); }
  };

  const statusMeta = AVAILABILITY_LABELS[profile.availability_status] || AVAILABILITY_LABELS.SAFE;

  return (
    <div className="page profile-page">

      {/* ── Toast ── */}
      {toast && <div className={`profile-toast ${toast.type === 'error' ? 'profile-toast-error' : ''}`}>{toast.msg}</div>}

      {/* ── Header ── */}
      <header className="dashboard-header">
        <h2>Your Profile</h2>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/dashboard')}>← Back</button>
      </header>

      {/* ── Identity Card ── */}
      <div className="profile-identity-card">
        <div className="profile-avatar">{user?.full_name?.[0]?.toUpperCase() ?? '?'}</div>
        <div className="profile-identity-info">
          <h3 className="profile-name">{user?.full_name}</h3>
          <p className="profile-email">{user?.email}</p>
          <div className="profile-badges">
            <span className="badge badge-accent">{user?.role}</span>
            <span className="badge" style={{ color: statusMeta.color, borderColor: statusMeta.color + '44', background: statusMeta.color + '11' }}>
              ● {statusMeta.label}
            </span>
          </div>
        </div>
      </div>

      {/* ── Personal Info ── */}
      <div className="profile-section-card">
        <h4 className="profile-section-title">Personal Information</h4>
        <div className="profile-fields-grid">
          <ProfileField label="Phone" icon="📞"><PhoneEditable name="phone_number" value={profile.phone_number} onSave={onSaveField} /></ProfileField>
          <ProfileField label="Blood Type" icon="🩸"><BloodTypeSelect name="blood_type" value={profile.blood_type} onSave={onSaveField} /></ProfileField>
          <ProfileField label="Emergency Contact" icon="👤"><InlineEditable name="emergency_contact" value={profile.emergency_contact} onSave={onSaveField} /></ProfileField>
          <ProfileField label="Emergency Contact Phone" icon="🚨"><PhoneEditable name="emergency_contact_phone" value={profile.emergency_contact_phone} onSave={onSaveField} /></ProfileField>
          <ProfileField label="Preferred Language" icon="🌐"><InlineEditable name="preferred_language" value={profile.preferred_language} onSave={onSaveField} /></ProfileField>
          <ProfileField label="Has Disability" icon="♿">
            <label className="toggle-label">
              <input type="checkbox" className="toggle-cb" checked={profile.has_disability} onChange={() => onToggleField('has_disability')} />
              <span className="toggle-track"><span className="toggle-thumb" /></span>
              <span className="toggle-text">{profile.has_disability ? 'Yes' : 'No'}</span>
            </label>
          </ProfileField>
          <ProfileField label="Availability Status" icon="📡" fullWidth>
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
          <ProfileField label="Special Needs" icon="📋" fullWidth>
            <InlineEditable name="special_needs" value={profile.special_needs} onSave={onSaveField} multiline placeholder="Describe any special needs…" />
          </ProfileField>
          <ProfileField label="Bio" icon="✏️" fullWidth>
            <InlineEditable name="bio" value={profile.bio} onSave={onSaveField} multiline placeholder="Tell others about yourself…" />
          </ProfileField>
        </div>
      </div>

      {/* ── Resources (accordion) ── */}
      <AccordionSection title="Resources" icon="📦" count={resources.length}>
        <div className="accordion-action-row">
          <button className="btn btn-secondary btn-sm" onClick={() => setShowResourceForm((v) => !v)}>
            {showResourceForm ? 'Cancel' : '+ Add Resource'}
          </button>
        </div>

        {showResourceForm && (
          <form onSubmit={handleAddResource} className="profile-inline-form">
            <div className="form-row">
              <div className="form-group">
                <label>Name</label>
                <input placeholder="e.g. Generator" value={newResource.name} onChange={(e) => setNewResource((p) => ({ ...p, name: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>Category</label>
                <input placeholder="e.g. Power" value={newResource.category} onChange={(e) => setNewResource((p) => ({ ...p, category: e.target.value }))} required />
              </div>
              <div className="form-group form-group-sm">
                <label>Quantity</label>
                <input type="number" min="1" value={newResource.quantity} onChange={(e) => setNewResource((p) => ({ ...p, quantity: e.target.value }))} required />
              </div>
            </div>
            <div className="form-check-row">
              <label className="toggle-label">
                <input type="checkbox" className="toggle-cb" checked={newResource.condition} onChange={(e) => setNewResource((p) => ({ ...p, condition: e.target.checked }))} />
                <span className="toggle-track"><span className="toggle-thumb" /></span>
                <span className="toggle-text">Functional</span>
              </label>
              <button className="btn btn-primary btn-sm" type="submit">Save</button>
            </div>
          </form>
        )}

        {resources.length === 0 ? (
          <p className="accordion-empty">No resources added yet.</p>
        ) : (
          <ul className="item-card-list">
            {resources.map((r) => (
              <li key={r.id} className="item-card">
                <div className="item-card-icon">📦</div>
                <div className="item-card-body">
                  <span className="item-card-name">{r.name}</span>
                  <span className="item-card-meta">{r.category} · qty {r.quantity}</span>
                </div>
                <div className="item-card-actions">
                  <button
                    className={`condition-badge ${r.condition ? 'condition-ok' : 'condition-bad'}`}
                    onClick={() => handleToggleCondition(r)}
                    title="Click to toggle"
                    type="button"
                  >
                    {r.condition ? '✓ Functional' : '✗ Not functional'}
                  </button>
                  <button className="icon-btn icon-btn-danger" onClick={() => handleDeleteResource(r.id)} title="Delete" type="button">✕</button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </AccordionSection>

      {/* ── Expertise Fields (accordion, EXPERT only) ── */}
      {isExpert && (
        <AccordionSection title="Expertise Fields" icon="🎓" count={expertiseFields.length}>
          <div className="accordion-action-row">
            <button className="btn btn-secondary btn-sm" onClick={() => setShowExpertiseForm((v) => !v)}>
              {showExpertiseForm ? 'Cancel' : '+ Add Expertise'}
            </button>
          </div>

          {showExpertiseForm && (
            <form onSubmit={handleAddExpertise} className="profile-inline-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Field</label>
                  <input placeholder="e.g. First Aid" value={newExpertise.field} onChange={(e) => setNewExpertise((p) => ({ ...p, field: e.target.value }))} required />
                </div>
                <div className="form-group form-group-sm">
                  <label>Level</label>
                  <select value={newExpertise.certification_level} onChange={(e) => setNewExpertise((p) => ({ ...p, certification_level: e.target.value }))}>
                    <option value="BEGINNER">Beginner</option>
                    <option value="ADVANCED">Advanced</option>
                  </select>
                </div>
              </div>
              <div className="form-group">
                <label>Certificate URL <span className="optional-tag">(optional)</span></label>
                <input placeholder="https://…" value={newExpertise.certification_document_url} onChange={(e) => setNewExpertise((p) => ({ ...p, certification_document_url: e.target.value }))} />
              </div>
              <div className="form-check-row">
                <button className="btn btn-primary btn-sm" type="submit">Save</button>
              </div>
            </form>
          )}

          {expertiseFields.length === 0 ? (
            <p className="accordion-empty">No expertise fields added yet.</p>
          ) : (
            <ul className="item-card-list">
              {expertiseFields.map((ef) => (
                <li key={ef.id} className="item-card">
                  <div className="item-card-icon">🎓</div>
                  <div className="item-card-body">
                    <span className="item-card-name">{ef.field}</span>
                    <span className="item-card-meta">{ef.certification_level === 'ADVANCED' ? '★ Advanced' : '◎ Beginner'}</span>
                  </div>
                  <div className="item-card-actions">
                    {ef.certification_document_url && (
                      <a href={ef.certification_document_url} target="_blank" rel="noreferrer" className="cert-link">Certificate ↗</a>
                    )}
                    <button className="icon-btn icon-btn-danger" onClick={() => handleDeleteExpertise(ef.id)} title="Delete" type="button">✕</button>
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

/* ── Helper: single profile field row ────────────────────────────────────────── */
function ProfileField({ label, icon, children, fullWidth = false }) {
  return (
    <div className={`profile-field ${fullWidth ? 'profile-field-full' : ''}`}>
      <span className="profile-field-label"><span className="profile-field-icon">{icon}</span>{label}</span>
      <span className="profile-field-value">{children}</span>
    </div>
  );
}
