import React, { useEffect } from 'react'
import { Route, Routes } from 'react-router-dom'
import './App.scss'
import Home from './pages/Home'
import Highlights from './pages/Highlights'
import Heroes from './pages/Heroes'
import Teams from './pages/Teams'
import Layout from './components/Layout'
import Search from './pages/Search'
import HeroItems from './pages/HeroItems'

function App() {
  useEffect(() => {
    document.title = 'GG Stats'
  }, [])

  return (
    <>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="heroes" element={<Heroes />} />
          <Route path="heroes/:heroId/items" element={<HeroItems />} />
          <Route path="teams" element={<Teams />} />
          <Route path="highlights" element={<Highlights />} />
          <Route path="search" element={<Search />} />
        </Route>
      </Routes>
    </>
  )
}

export default App
