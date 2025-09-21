import React, { useEffect, useState } from 'react'
import Loader from 'react-loaders'
import { Link } from 'react-router-dom'
import './index.scss'
import AnimatedLetters from '../../AnimatedLetters'

const API_BASE = import.meta.env.VITE_API_BASE || ''

const Heroes = () => {
  const [letterClass, setLetterClass] = useState('text-animate')
  const [heroes, setHeroes] = useState([])
  const [searchQuery, setSearchQuery] = useState('')
  const [filteredHeroes, setFilteredHeroes] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    const timer = setTimeout(() => {
      setLetterClass('text-animate-hover')
    }, 3000)
    return () => clearTimeout(timer)
  }, [])

  useEffect(() => {
    async function loadHeroes() {
      try {
        const res = await fetch(`${API_BASE}/heroes`)
        if (!res.ok) throw new Error('Failed to load heroes')

        const contentType = res.headers.get('content-type') || ''
        console.log('contentType=' + contentType)
        if (!contentType.includes('application/json'))
          throw new Error('Heroes response is not JSON')
        const data = await res.json()
        setHeroes(data)
        setFilteredHeroes(data) // initialize with full list
      } catch (e) {
        setError(e.message)
      }
    }
    loadHeroes()
  }, [])

  useEffect(() => {
    const filtered = heroes.filter(
      (h) =>
        (h.localizedName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        (h.name || '').toLowerCase().includes(searchQuery.toLowerCase())
    )
    setFilteredHeroes(filtered)
  }, [heroes, searchQuery])

  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value)
  }

  const renderHeroImages = (heroes) => {
    return (
      <div className="images-container">
        {heroes.map((h, idx) => {
          const cdn = h.heroCdnName
            ? h.heroCdnName
            : h.name
              ? h.name.replace('npc_dota_hero_', '').toLowerCase()
              : null

          if (!cdn) return null

          const localized = h.localizedName || h.localized_name || h.name

          return (
            <div key={h.heroId || idx} className="image-box">
              <img
                src={`https://cdn.steamstatic.com/apps/dota2/images/dota_react/heroes/${cdn}.png`}
                alt={localized}
                style={{ width: '100%', height: '100%' }}
                onError={(e) => {
                  const cf = `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/${cdn}.png`
                  if (e.currentTarget.src !== cf) e.currentTarget.src = cf
                }}
              />
              <div className="content">
                <p className="title">{localized}</p>
                <Link
                  className="btn"
                  to={`/heroes/${encodeURIComponent(h.heroId || h.id || h.hero_id || '')}/items?heroName=${encodeURIComponent(localized)}&heroCdnName=${encodeURIComponent(cdn)}`}
                >
                  View
                </Link>
              </div>
            </div>
          )
        })}
      </div>
    )
  }

  return (
    <>
      <div className="container heroes-page">
        <h1 className="page-title">
          <br />
          <AnimatedLetters letterClass={letterClass} strArray={'Heroes'.split('')} idx={15} />
        </h1>

        <div className="search-bar">
          <input
            type="text"
            placeholder="Search for heroes"
            value={searchQuery}
            onChange={handleSearchChange}
          />
        </div>

        {error ? (
          <div style={{ padding: 16, color: 'red' }}>{error}</div>
        ) : (
          <div>{renderHeroImages(filteredHeroes)}</div>
        )}
      </div>

      <Loader type="pacman" />
    </>
  )
}

export default Heroes
