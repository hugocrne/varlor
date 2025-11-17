'use client';

import React, { useEffect, useRef, useState } from 'react';
import { motion, useScroll, useTransform, MotionValue } from 'framer-motion';
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

const LiquidGlassNumber: React.FC<{ number: number; rotation: MotionValue<number> }> = ({ number, rotation }) => {
  return (
    <span className={styles.stepNumber}>
      <motion.span
        className={styles.liquidBorder}
        style={{
          rotate: rotation,
        }}
      />
      <span className={styles.numberContent}>{number}</span>
    </span>
  );
};

export const HowItWorks: React.FC = () => {
  const [visibleSteps, setVisibleSteps] = useState<boolean[]>(
    steps.map(() => false)
  );
  const stepRefs = useRef<(HTMLDivElement | null)[]>([]);
  const sectionRef = useRef<HTMLElement>(null);

  const { scrollYProgress } = useScroll({
    target: sectionRef,
    offset: ["start 0.8", "end 0.2"],
  });

  const rotation = useTransform(scrollYProgress, [0, 1], [0, 360]);

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
    <section id="how-it-works" className={styles.howItWorks} ref={sectionRef}>
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
                  <LiquidGlassNumber number={index + 1} rotation={rotation} />
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

