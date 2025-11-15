import React from 'react';
import { Container } from '../../components/Container/Container';
import styles from './Footer.module.scss';

export const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer id="contact" className={styles.footer}>
      <Container>
        <div className={styles.content}>
          <div className={styles.brand}>
            <h3 className={styles.brandName}>Varlor</h3>
            <p className={styles.tagline}>
              Plateforme d&rsquo;analyse de données
            </p>
          </div>
          <div className={styles.links}>
            <a href="#features" className={styles.link}>
              Fonctionnalités
            </a>
            <a href="#how-it-works" className={styles.link}>
              Comment ça marche
            </a>
            <a href="/api/docs" className={styles.link}>
              Documentation API
            </a>
          </div>
        </div>
        <div className={styles.bottom}>
          <p className={styles.copyright}>
            © {currentYear} Varlor. Tous droits réservés.
          </p>
        </div>
      </Container>
    </footer>
  );
};

