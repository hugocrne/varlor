'use client';

import React, { useEffect, useState } from 'react';
import styles from './Navbar.module.scss';

const navLinks = [
  { label: 'Fonctionnalités', href: '#features' },
  { label: 'Tarifs', href: '#pricing' },
  { label: 'Documentation', href: '#documentation' },
  { label: 'Contact', href: '#contact' },
];

export const Navbar: React.FC = () => {
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 10);
    };

    handleScroll();
    window.addEventListener('scroll', handleScroll, { passive: true });

    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  return (
    <header className={styles.wrapper}>
      <div className={`${styles.navbar} ${isScrolled ? styles.scrolled : ''}`}>
        <div className={styles.brand}>Varlor</div>

        <nav className={styles.nav} aria-label="Navigation principale">
          {navLinks.map((link) => (
            <a key={link.href} href={link.href} className={styles.navLink}>
              {link.label}
            </a>
          ))}
        </nav>

        <div className={styles.actions}>
          <a href="#contact" className={styles.cta}>
            Commencer
          </a>
        </div>
      </div>
    </header>
  );
};


