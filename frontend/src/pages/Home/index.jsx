import { useEffect, useState } from 'react';
import Loader from 'react-loaders';
import { Link } from 'react-router-dom';
import LogoGG from '../../assets/GG.webp';
import AnimatedLetters from '../../AnimatedLetters';
import './index.scss';

const Home = () => {
    const [letterClass, setLetterClass] = useState('text-animate')
    const nameArray = "Welcome to".split("");
    const jobArray = "GG Stats!".split("");

    useEffect(() => {
        const timerId = setTimeout(() => {
          setLetterClass('text-animate-hover');
        }, 4000);

        return () => {
          clearTimeout(timerId);
        };
      }, []);

    return(
      <>
        <div className = "container home-page">
            <div className="text-zone">
                <h1>
                <img src={LogoGG} alt = "GG Stats" />
                <br />
                <AnimatedLetters letterClass={letterClass} strArray={nameArray} idx={12} />
                <br />
                <AnimatedLetters letterClass={letterClass} strArray={jobArray} idx={16} />
                </h1>
                <h2>Your home for weekly Dota2 highlights</h2>
                <Link to="/highlights" className="flat-button">GET STARTED</Link>
            </div>
        </div>
        <Loader type="pacman" />
      </>
    )
}

export default Home

