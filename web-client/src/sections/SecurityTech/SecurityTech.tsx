import React from 'react';
import { Container } from '../../components/Container/Container';
import { Card } from '../../components/Card/Card';
import styles from './SecurityTech.module.scss';

const securityFeatures = [
  {
    title: 'Authentification JWT',
    description:
      'Sécurité renforcée avec des tokens JWT pour une authentification sécurisée et scalable.',
  },
  {
    title: 'Rate limiting',
    description:
      'Protection contre les abus avec limitation du nombre de requêtes par IP et par utilisateur.',
  },
  {
    title: 'API documentée',
    description:
      'Documentation complète OpenAPI/Swagger pour une intégration facile et rapide.',
  },
];

export const SecurityTech: React.FC = () => {
  return (
    <section id="documentation" className={styles.securityTech}>
      <Container>
        <div className={styles.header}>
          <h2 className={styles.title}>Sécurité & technologie</h2>
          <p className={styles.subtitle}>
            Des technologies modernes et sécurisées pour vos données
          </p>
        </div>
        <div className={styles.grid}>
          {securityFeatures.map((feature, index) => (
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

