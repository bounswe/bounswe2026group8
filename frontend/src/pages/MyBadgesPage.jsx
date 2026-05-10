import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getMyBadges } from '../services/api'; // Adjust path if your api.js is somewhere else

export default function MyBadgesPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  
  const [badges, setBadges] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchBadges = async () => {
      try {
        const response = await getMyBadges();
        // Since your api.js wrapper returns { ok, status, data }
        if (response.ok) {
          setBadges(response.data);
        } else {
          console.error("Failed to fetch badges");
        }
      } catch (error) {
        console.error("Error fetching badges:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchBadges();
  }, []);

  return (
    <div className="page my-badges-page">
      {/* Header Section */}
      <header className="dashboard-header page-main-header">
        <div className="page-title-block">
          <h2>{t('my_badges.header.title', 'My Badges')}</h2>
        </div>
        
        <button 
          className="btn btn-secondary btn-sm" 
          onClick={() => navigate('/profile')}
          style={{ marginLeft: 'auto' }}
        >
          &larr; {t('profile.header.back', 'Back')}
        </button>
      </header>

      {/* Badges Content Section */}
      <div className="page-content" style={{ padding: '20px' }}>
        {isLoading ? (
          <p style={{ color: 'var(--text-secondary)' }}>
            {t('my_badges.loading', 'Loading your achievements...')}
          </p>
        ) : badges.length === 0 ? (
          <p style={{ color: 'var(--text-secondary)' }}>
            {t('my_badges.empty', "You haven't earned any badges yet. Start participating in the community!")}
          </p>
        ) : (
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', 
            gap: '15px' 
          }}>
            {badges.map((badge) => {
              // Create a safe, lowercase key from the database name. 
              // E.g., "Commenter" -> "Commenter"
              // E.g., "Helping Hand" -> "helping_hand"
              const badgeKey = badge.badge_name.toLowerCase().replace(/\s+/g, '_');

              // 1. Dynamic Title (Looks in JSON first, falls back to DB name)
              const baseTitle = t(`badges.${badgeKey}.title`, badge.badge_name);
              const displayTitle = badge.current_level > 0 
                ? `${baseTitle} ${badge.current_level}` 
                : baseTitle;
              
              // 2. Safe Progress Percentage
              const progressPercent = badge.is_max_level 
                ? 100 
                : Math.min((badge.current_progress / badge.next_level_goal) * 100, 100);

              return (
                <div key={badge.id} className="profile-section-card" style={{ display: 'flex', flexDirection: 'column' }}>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                    <div style={{ fontSize: '2.5rem' }}>
                      {badge.badge_icon}
                    </div>
                    
                    <div style={{ flex: 1 }}>
                      <h4 style={{ margin: 0 }}>{displayTitle}</h4>
                      <p style={{ 
                        color: 'var(--text-secondary)', 
                        fontSize: '0.85rem', 
                        margin: '0.25rem 0 0 0' 
                      }}>
                        {/* Looks in JSON first, falls back to DB description */}
                        {t(`badges.${badgeKey}.description`, badge.badge_description)}
                      </p>
                    </div>
                  </div>
                  
                  {/* ... (The progress bar code stays exactly the same as before) ... */}
                  <div style={{ background: 'rgba(150, 150, 150, 0.2)', borderRadius: '4px', height: '8px', width: '100%', marginTop: '15px', overflow: 'hidden' }}>
                    <div style={{ background: badge.is_max_level ? '#FFD700' : 'var(--accent)', height: '100%', width: `${progressPercent}%`, transition: 'width 0.5s ease-in-out' }} />
                  </div>
                  
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
                    <span style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', fontWeight: 'bold' }}>
                      {badge.is_max_level 
                        ? t('my_badges.max_level', 'MAX LEVEL REACHED') 
                        : `${badge.current_progress} / ${badge.next_level_goal}`
                      }
                    </span>
                  </div>

                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}