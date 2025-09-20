import { useEffect, useState } from 'react'
import './index.scss'

const API_BASE = import.meta.env.VITE_API_BASE || ''

export default function Highlights() {
  const [emerging, setEmerging] = useState([])
  const [topSynergy, setTopSynergy] = useState([])
  const [popular, setPopular] = useState([])
  const [heroesById, setHeroesById] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        const [heroesRes, emergingRes, topSynRes, popularRes] = await Promise.all([
          fetch(`${API_BASE}/heroes`),
          fetch(`${API_BASE}/highlights/pairs?view=emerging-synergy&weekOffset=0&limit=6`),
          fetch(`${API_BASE}/highlights/pairs?view=synergy&weekOffset=0&limit=6`),
          fetch(`${API_BASE}/highlights/pairs?view=trending-popularity&weekOffset=0&limit=6`),
        ])
        if (!heroesRes.ok) throw new Error('Failed to load heroes')
        if (!emergingRes.ok) throw new Error('Failed to load emerging pairs')
        if (!topSynRes.ok) throw new Error('Failed to load top synergy pairs')
        if (!popularRes.ok) throw new Error('Failed to load popular pairs')
        const contentTypeH = heroesRes.headers.get('content-type') || ''
        const contentTypeE = emergingRes.headers.get('content-type') || ''
        const contentTypeTS = topSynRes.headers.get('content-type') || ''
        const contentTypeP = popularRes.headers.get('content-type') || ''
        if (!contentTypeH.includes('application/json')) throw new Error('Heroes response is not JSON')
        if (!contentTypeE.includes('application/json')) throw new Error('Emerging response is not JSON')
        if (!contentTypeTS.includes('application/json')) throw new Error('Top synergy response is not JSON')
        if (!contentTypeP.includes('application/json')) throw new Error('Popular response is not JSON')
        const heroes = await heroesRes.json()
        const emergingJson = await emergingRes.json()
        const topSynJson = await topSynRes.json()
        const popularJson = await popularRes.json()
        if (import.meta.env.DEV) {
          // Log shapes to debug field names
          console.log('highlights:heroes', heroes.slice(0,2))
          console.log('highlights:emerging', emergingJson)
          console.log('highlights:popular', popularJson)
        }
        const map = {}
        heroes.forEach(h => {
          const id = h.heroId ?? h.hero_id
          if (id != null) {
            map[id] = h
          }
        })
        setHeroesById(map)
        setEmerging(emergingJson.pairs || [])
        setTopSynergy(topSynJson.pairs || [])
        setPopular(popularJson.pairs || [])
      } catch (e) {
        setError(e.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const stripName = (raw) => {
    if (!raw) return null
    return raw.replace('npc_dota_hero_', '').replaceAll('_', ' ')
  }

  const renderPair = (p, idx) => {
    const heroIdA = p.heroIdA ?? p.hero_id_a
    const heroIdB = p.heroIdB ?? p.hero_id_b
    const a = heroesById[heroIdA]
    const b = heroesById[heroIdB]
    const cdnName = (h) => {
      if (!h) return null
      if (h.heroCdnName) return h.heroCdnName
      if (h.name) return h.name.replace('npc_dota_hero_', '').toLowerCase()
      return null
    }
    const heroAImgUrl = p.heroAImgUrl || p.hero_a_img_url || p.hero_aimg_url
    const heroBImgUrl = p.heroBImgUrl || p.hero_b_img_url || p.hero_bimg_url
    const heroACdnName = p.heroACdnName || p.hero_a_cdn_name || p.hero_acdn_name
    const heroBCdnName = p.heroBCdnName || p.hero_b_cdn_name || p.hero_bcdn_name
    const heroAName = (a?.localizedName || a?.localized_name) || p.heroALocalizedName || p.hero_a_localized_name || stripName(p.heroAName || p.hero_aname) || heroIdA
    const heroBName = (b?.localizedName || b?.localized_name) || p.heroBLocalizedName || p.hero_b_localized_name || stripName(p.heroBName || p.hero_bname) || heroIdB
    const cdnA = heroACdnName || cdnName(a)
    const cdnB = heroBCdnName || cdnName(b)
    const srcA = heroAImgUrl || (cdnA ? `https://cdn.steamstatic.com/apps/dota2/images/dota_react/heroes/${cdnA}.png` : null)
    const srcB = heroBImgUrl || (cdnB ? `https://cdn.steamstatic.com/apps/dota2/images/dota_react/heroes/${cdnB}.png` : null)
    const onImgError = (e, cdn) => {
      if (!cdn) return
      const cf = `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/${cdn}.png`
      if (e.currentTarget.src !== cf) {
        e.currentTarget.src = cf
      }
    }
    return (
      <>
        <div className="pair-images">
          {srcA && (
            <div className="half-box">
              <img className="heroes-image" src={srcA} alt={heroAName} onError={(e)=>onImgError(e, cdnA)} />
            </div>
          )}
          {srcB && (
            <div className="half-box">
              <img className="heroes-image" src={srcB} alt={heroBName} onError={(e)=>onImgError(e, cdnB)} />
            </div>
          )}
        </div>
        <div className="content">
          <p className="title">{heroAName} + {heroBName}</p>
        </div>
      </>
    )
  }

  if (loading) return <div style={{ padding: 16 }}>Loading…</div>
  if (error) return <div style={{ padding: 16, color: 'red' }}>{error}</div>

  return (
    <div className="container heroes-page">
      <h1 className="page-title">Emerging Synergy</h1>
      <div className="images-container">
        {emerging.map((p, i) => {
          const a = p.heroIdA ?? p.hero_id_a
          const b = p.heroIdB ?? p.hero_id_b
          return (
            <div key={`e-${a}-${b}`} className="pair-box">
              {renderPair(p, i)}
            </div>
          )
        })}
      </div>
      <h1 className="page-title" style={{ marginTop: 40 }}>Top Synergy</h1>
      <div className="images-container">
        {topSynergy.map((p, i) => {
          const a = p.heroIdA ?? p.hero_id_a
          const b = p.heroIdB ?? p.hero_id_b
          return (
            <div key={`ts-${a}-${b}`} className="pair-box">
              {renderPair(p, i)}
            </div>
          )
        })}
      </div>
      <h1 className="page-title" style={{ marginTop: 40 }}>Trending Popularity</h1>
      <div className="images-container">
        {popular.map((p, i) => {
          const a = p.heroIdA ?? p.hero_id_a
          const b = p.heroIdB ?? p.hero_id_b
          return (
            <div key={`p-${a}-${b}`} className="pair-box">
              {renderPair(p, i)}
            </div>
          )
        })}
      </div>
    </div>
  )
}


