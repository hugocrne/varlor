'use client';

import React, { useEffect, useState, useRef } from 'react';
import { Container } from '../../components/Container/Container';
import { HeroSphere } from '../../components/HeroSphere/HeroSphere';
import styles from './Hero.module.scss';

export const Hero: React.FC = () => {
  // États
  const [scrollY, setScrollY] = useState(0);
  const [isLoaded] = useState(true);
  const [isVisible, setIsVisible] = useState(false);

  // Refs
  const illustrationRef = useRef<HTMLDivElement>(null);
  const buttonPrimaryRef = useRef<HTMLAnchorElement>(null);
  const heroContentRef = useRef<HTMLDivElement>(null);

  // Gestion du scroll
  useEffect(() => {
    const handleScroll = () => {
      setScrollY(window.scrollY);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  // Afficher le contenu après un court délai
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsVisible(true);
    }, 100);
    
    return () => {
      clearTimeout(timer);
    };
  }, []);

  // Calcul du parallax pour l'illustration
  const parallaxOffset = scrollY * 0.25;
  const parallaxOpacity = Math.max(1, 1 - scrollY * 0.0008);

  return (
    <section className={styles.hero}>
      <HeroSphere 
        className={styles.heroSphere}
        pointCount={400}
      />
      <Container>
        <div className={styles.heroGrid}>
          <div 
            ref={heroContentRef}
            className={`${styles.heroContent} ${isVisible ? styles.visible : ''}`}
            data-hero-content
          >
            <h1 className={styles.heroTitle}>
            Donnez du sens à vos données
            </h1>
            <p className={styles.heroSubtitle}>
            Prétraitement intelligent, indicateurs personnalisés et analyses instantanées pour faire émerger ce qui compte vraiment.
            </p>
            <div className={styles.heroCTA}>
              <a
                ref={buttonPrimaryRef}
                href="#features"
                className={styles.buttonPrimary}
              >
                Commencer
                <svg
                  className={styles.buttonIcon}
                  viewBox="0 0 16 16"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M6 12L10 8L6 4"
                    stroke="currentColor"
                    strokeWidth="1.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </a>
              <a href="#how-it-works" className={styles.buttonSecondary}>
                En savoir plus
              </a>
            </div>
          </div>
          <div className={styles.heroVisual}>
            <div
              ref={illustrationRef}
              className={styles.abstractIllustration}
              style={{
                transform: `translateY(${parallaxOffset}px)`,
                opacity: parallaxOpacity,
              }}
            >
              {/* Illustration content */}
            </div>
          </div>
        </div>
      </Container>
    </section>
  );
};
