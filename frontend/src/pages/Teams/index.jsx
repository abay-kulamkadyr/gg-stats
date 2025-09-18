import { useEffect, useState } from 'react'
import './index.scss'

const API_BASE = import.meta.env.VITE_API_BASE || ''

export default function Teams() {
  const [teams, setTeams] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        // Request one page from backend; backend can page DB if large
        const res = await fetch(`${API_BASE}/pro/teams/paged?page=0&size=52`)
        if (!res.ok) throw new Error('Failed to load teams')
        const ct = res.headers.get('content-type') || ''
        if (!ct.includes('application/json')) throw new Error('Teams response is not JSON')
        const json = await res.json()
        const content = Array.isArray(json) ? json : (json.content || [])
        setTeams(content)
      } catch (e) {
        setError(e.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) return <div style={{ padding: 16 }}>Loading…</div>
  if (error) return <div style={{ padding: 16, color: 'red' }}>{error}</div>

  return (
    <div className="container teams-page">
      <h1 className="page-title">Top Teams</h1>
      <div className="images-container">
        {teams.map((t) => (
          <div key={t.teamId} className="team-box">
            <div className="logo-wrap">
              {(() => {
                const teamId = t.teamId ?? t.team_id ?? t.id;
                const directCdn = teamId ? `https://steamcdn-a.akamaihd.net/apps/dota2/images/team_logos/${teamId}.png` : null;
                const logo = t.logoUrl || directCdn;
                if (!logo) {
                  return <div className="placeholder">{t.tag || t.name?.slice(0,2) || 'T'}</div>;
                }
                return (
                  <img
                    src={`${API_BASE}/img?url=${encodeURIComponent(logo)}`}
                    alt={t.name}
                    onError={(e)=>{
                      if (directCdn && e.currentTarget.dataset.fallback !== '1') {
                        e.currentTarget.dataset.fallback = '1';
                        e.currentTarget.src = directCdn; // try direct CDN if proxy fails
                      } else {
                        e.currentTarget.style.display='none';
                      }
                    }}
                  />
                );
              })()}
            </div>
            <div className="content">
              <p className="title">{t.name}</p>
              {t.rating != null && <p className="subtitle">Rating: {t.rating}</p>}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}


