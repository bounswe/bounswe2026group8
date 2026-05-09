import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function MyBadgesPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();

  return (
    <div className="page my-badges-page">
      {/* Header Section matching the style in your screenshot */}
      <header className="dashboard-header page-main-header">
        <div className="page-title-block">
          <h2>{t('my_badges.header.title', 'My Badges')}</h2>
        </div>
        
        {/* Back Button positioned on the right like in the "My Posts" image */}
        <button 
          className="btn btn-secondary btn-sm" 
          onClick={() => navigate('/profile')}
          style={{ marginLeft: 'auto' }}
        >
          &larr; {t('profile.header.back', 'Back')}
        </button>
      </header>
    </div>
  );
}
