'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Container } from '../../components/Container/Container';
import styles from './HowItWorks.module.scss';

const steps = [
  {
    title: 'Importez vos données',
    description:
      'Connectez vos sources ou déposez vos fichiers. Varlor détecte automatiquement les schémas, les types et les incohérences.',
  },
  {
    title: 'L’IA prépare et structure',
    description:
      'Les données sont nettoyées, harmonisées et enrichies automatiquement pour être prêtes à l’analyse, sans travail manuel.',
  },
  {
    title: 'Analysez et obtenez des insights instantanés',
    description:
      'Générez des indicateurs, visualisez les tendances clés et accédez immédiatement aux informations qui comptent.',
  },
];

export const HowItWorks: React.FC = () => {
  const [visibleSteps, setVisibleSteps] = useState<boolean[]>(
    steps.map(() => false)
  );
  const stepRefs = useRef<(HTMLDivElement | null)[]>([]);

  useEffect(() => {
    if (typeof window === 'undefined' || !('IntersectionObserver' in window)) {
      setVisibleSteps(steps.map(() => true));
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const index = Number(
              (entry.target as HTMLElement).dataset.stepIndex
            );
            if (!Number.isNaN(index)) {
              setVisibleSteps((prev) => {
                if (prev[index]) {
                  return prev;
                }
                const next = [...prev];
                next[index] = true;
                return next;
              });
              observer.unobserve(entry.target);
            }
          }
        });
      },
      {
        threshold: 0.3,
      }
    );

    stepRefs.current.forEach((element) => {
      if (element) {
        observer.observe(element);
      }
    });

    return () => {
      observer.disconnect();
    };
  }, []);

  return (
    <section id="how-it-works" className={styles.howItWorks}>
      <Container>
        <div className={styles.timeline}>
          <div className={styles.timelineHeader}>
            <p className={styles.label}>Comment ça marche</p>
            <h2 className={styles.title}>
              Une timeline en trois temps pour comprendre Varlor
            </h2>
          </div>

          <div className={styles.steps}>
            <span className={styles.verticalLine} aria-hidden="true" />
            {steps.map((step, index) => (
              <div
                key={step.title}
                data-step-index={index}
                ref={(element) => {
                  stepRefs.current[index] = element;
                }}
                className={`${styles.step} ${
                  visibleSteps[index] ? styles.stepVisible : ''
                }`}
              >
                <div className={styles.stepMeta}>
                  <span className={styles.stepNumber}>{index + 1}</span>
                  <span className={styles.stepDash} aria-hidden="true" />
                </div>
                <div className={styles.stepContent}>
                  <h3 className={styles.stepTitle}>{step.title}</h3>
                  <p className={styles.stepDescription}>{step.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </Container>
    </section>
  );
};

