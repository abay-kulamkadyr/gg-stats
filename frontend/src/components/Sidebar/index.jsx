import './index.scss'
import { Link, NavLink } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faHome,
  faBars,
  faClose,
  faUsers,
  faFire,
  faHelmetSafety,
} from '@fortawesome/free-solid-svg-icons'
import LogoGG from '../../assets/GG.webp'
import LogoSubtitle from '../../assets/sub-logo.png'
import { useState } from 'react'

const Sidebar = () => {
  const [showNav, setShowNav] = useState(false)
  return (
    <div className="nav-bar">
      <Link className="logo" to="/">
        <img src={LogoGG} alt="logo" />
        <img className="sub-logo" src={LogoSubtitle} alt="GG stats" />
      </Link>
      <nav className={showNav ? 'mobile-show' : ''}>
        <NavLink exact="true" activeclassname="active" to="/">
          <FontAwesomeIcon icon={faHome} onClick={() => setShowNav(false)} />
        </NavLink>
        <NavLink exact="true" activeclassname="active" className="highlights-link" to="/highlights">
          <FontAwesomeIcon icon={faFire} onClick={() => setShowNav(false)} />
        </NavLink>
        <NavLink exact="true" activeclassname="active" className="heroes-link" to="/heroes">
          <FontAwesomeIcon icon={faHelmetSafety} onClick={() => setShowNav(false)} />
        </NavLink>
        <NavLink exact="true" activeclassname="active" className="teams-link" to="/teams">
          <FontAwesomeIcon icon={faUsers} onClick={() => setShowNav(false)} />
        </NavLink>
        <FontAwesomeIcon
          icon={faClose}
          size="3x"
          className="close-icon"
          onClick={() => setShowNav(false)}
        />
      </nav>
      <FontAwesomeIcon
        onClick={() => setShowNav(true)}
        icon={faBars}
        color="#ffd700"
        size="3x"
        className="hamburger-icon"
      />
    </div>
  )
}

export default Sidebar
