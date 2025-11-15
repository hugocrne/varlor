import React from 'react';
import { Container } from '../../components/Container/Container';
import { Card } from '../../components/Card/Card';
import styles from './ForWho.module.scss';

const targetAudiences = [
  {
    title: 'Entreprises',
    description:
      'Analysez vos données métier et prenez des décisions éclairées avec des indicateurs personnalisés.',
  },
  {
    title: 'Développeurs',
    description:
      'Intégrez facilement notre API REST dans vos applications pour enrichir vos fonctionnalités.',
  },
  {
    title: 'Analystes de données',
    description:
      'Gagnez du temps avec notre prétraitement intelligent et concentrez-vous sur l&rsquo;analyse.',
  },
];

export const ForWho: React.FC = () => {
  return (
    <section className={styles.forWho}>
      <Container>
        <div className={styles.header}>
          <h2 className={styles.title}>Pour qui</h2>
          <p className={styles.subtitle}>
            Varlor s&rsquo;adapte à tous vos besoins d&rsquo;analyse de données
          </p>
        </div>
        <div className={styles.grid}>
          {targetAudiences.map((audience, index) => (
            <Card
              key={index}
              title={audience.title}
              description={audience.description}
            />
          ))}
        </div>
      </Container>
    </section>
  );
};

