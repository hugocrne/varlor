import React from 'react';
import { Container } from '../../components/Container/Container';
import { Card } from '../../components/Card/Card';
import styles from './Features.module.scss';

const features = [
  {
    title: 'Prétraitement de données intelligent',
    description:
      'Traitement automatique et nettoyage de vos données pour une analyse optimale.',
  },
  {
    title: 'Indicateurs personnalisables',
    description:
      'Créez et configurez vos propres indicateurs selon vos besoins métier.',
  },
  {
    title: 'API REST sécurisée',
    description:
      'Intégration facile via une API REST complète et bien documentée.',
  },
  {
    title: 'Authentification JWT',
    description:
      'Sécurité renforcée avec authentification par tokens JWT pour tous vos accès.',
  },
];

export const Features: React.FC = () => {
  return (
    <section id="features" className={styles.features}>
      <Container>
        <div className={styles.header}>
          <h2 className={styles.title}>Fonctionnalités</h2>
          <p className={styles.subtitle}>
            Tout ce dont vous avez besoin pour analyser vos données efficacement
          </p>
        </div>
        <div className={styles.grid}>
          {features.map((feature, index) => (
            <Card
              key={index}
              title={feature.title}
              description={feature.description}
            />
          ))}
        </div>
      </Container>
    </section>
  );
};

