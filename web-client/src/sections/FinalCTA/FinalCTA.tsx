import React from 'react';
import { Container } from '../../components/Container/Container';
import { Button } from '../../components/Button/Button';
import styles from './FinalCTA.module.scss';

export const FinalCTA: React.FC = () => {
  return (
    <section id="pricing" className={styles.finalCTA}>
      <Container>
        <div className={styles.content}>
          <h2 className={styles.title}>Prêt à commencer ?</h2>
          <p className={styles.subtitle}>
            Rejoignez Varlor dès aujourd&rsquo;hui et transformez vos données en insights
          </p>
          <Button variant="primary" as="a" href="#features">
            Commencer gratuitement
          </Button>
        </div>
      </Container>
    </section>
  );
};

