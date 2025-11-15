import React from 'react';
import { Container } from '../../components/Container/Container';
import styles from './HowItWorks.module.scss';

const steps = [
  {
    number: '1',
    title: 'Inscription / Connexion',
    description:
      'Créez votre compte ou connectez-vous pour accéder à la plateforme.',
  },
  {
    number: '2',
    title: 'Import de données',
    description:
      'Importez vos données via notre API REST ou l&rsquo;interface web.',
  },
  {
    number: '3',
    title: 'Analyse et indicateurs',
    description:
      'Visualisez vos données traitées et configurez vos indicateurs personnalisés.',
  },
];

export const HowItWorks: React.FC = () => {
  return (
    <section id="how-it-works" className={styles.howItWorks}>
      <Container>
        <div className={styles.header}>
          <h2 className={styles.title}>Comment ça marche</h2>
          <p className={styles.subtitle}>
            Trois étapes simples pour commencer à analyser vos données
          </p>
        </div>
        <div className={styles.steps}>
          {steps.map((step, index) => (
            <div key={index} className={styles.step}>
              <div className={styles.stepNumber}>{step.number}</div>
              <h3 className={styles.stepTitle}>{step.title}</h3>
              <p className={styles.stepDescription}>{step.description}</p>
            </div>
          ))}
        </div>
      </Container>
    </section>
  );
};

