import React, { useEffect, useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import Loader from 'react-loaders';
import './index.scss';

const API_BASE = import.meta.env.VITE_API_BASE || '';

const BUCKET_LABELS = {
  start_game: 'Start Game (0-10m)',
  early_game: 'Early Game (10-20m)',
  mid_game: 'Mid Game (20-30m)',
  late_game: 'Late Game (30m+)',
};

export default function HeroItems() {
  const { heroId } = useParams();
  const [searchParams] = useSearchParams();
  const heroName = searchParams.get('heroName');
  const heroCdnName = searchParams.get('heroCdnName');
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const title = useMemo(() => heroName ? `Popular Items - ${heroName}` : 'Popular Items', [heroName]);

  useEffect(() => {
    document.title = title;
  }, [title]);

  useEffect(() => {
    async function load() {
      try {
        setIsLoading(true);
        const res = await fetch(`${API_BASE}/pro/heroes/${heroId}/popular-items?limit=12`);
        if (!res.ok) throw new Error('Failed to load items');
        const contentType = res.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) throw new Error('Items response is not JSON');
        const json = await res.json();
        setData(json);
      } catch (e) {
        setError(e.message);
      } finally {
        setIsLoading(false);
      }
    }
    if (heroId) load();
  }, [heroId]);

  const renderGrid = (bucketKey) => {
    const items = (data && data[bucketKey]) || [];
    return (
      <div className="items-grid">
        {items.map((row) => {
          const key = row.itemKey || row.item_key;
          const img = `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/items/${key}.png`;
          return (
            <div key={key} className="item-card">
              <img src={img} alt={key} onError={(e) => { e.currentTarget.style.visibility = 'hidden'; }} />
              <div className="item-name">{key.replaceAll('_', ' ')}</div>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="container hero-items-page">
      <div className="hero-header">
        {heroCdnName && (
          <img
            className="hero-portrait"
            src={`https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/${heroCdnName}.png`}
            alt={heroName || heroCdnName}
          />
        )}
        <h1 className="page-title">{title}</h1>
      </div>
      {error && <div style={{ color: 'red', padding: 16 }}>{error}</div>}
      {isLoading ? (
        <Loader type="pacman" />
      ) : (
        <>
          <section>
            <h2>{BUCKET_LABELS.start_game}</h2>
            {renderGrid('start_game')}
          </section>
          <section>
            <h2>{BUCKET_LABELS.early_game}</h2>
            {renderGrid('early_game')}
          </section>
          <section>
            <h2>{BUCKET_LABELS.mid_game}</h2>
            {renderGrid('mid_game')}
          </section>
          <section>
            <h2>{BUCKET_LABELS.late_game}</h2>
            {renderGrid('late_game')}
          </section>
          <section>
            <h2>Top Players</h2>
            <div className="players-grid">
              {(data?.top_players || []).map((p) => (
                <div key={p.accountId || p.account_id} className="player-card">
                  <img
                    src={(p.avatarFull || p.avatar_full) || ''}
                    alt={(p.personName || p.person_name) || 'Player'}
                    onError={(e)=>{ e.currentTarget.style.visibility='hidden' }}
                  />
                  <div className="player-name">{(p.personName || p.person_name) || p.accountId}</div>
                  {p.score != null && <div className="player-score">Score: {p.score.toFixed(2)}</div>}
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}


