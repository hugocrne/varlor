'use client';

import React, { useRef } from 'react';
import { motion, Variants, useScroll, useTransform } from 'framer-motion';
import { Container } from '../../components/Container/Container';
import styles from './Features.module.scss';

type Feature = {
  title: string;
  description: string;
  Icon: React.FC<React.SVGProps<SVGSVGElement>>;
};

const iconProps = {
  viewBox: '0 0 32 32',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
} as const;

const PreprocessIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg {...iconProps} {...props}>
    <path d="M8 12c0-2.21 1.79-4 4-4h8" />
    <path d="M12 8v16" />
    <path d="M6 20h12" />
    <path d="M20 12h6" />
    <path d="M22 8v16" />
    <circle cx="10" cy="12" r="2.5" />
    <circle cx="24" cy="12" r="2" />
  </svg>
);

const MetricsIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg {...iconProps} {...props}>
    <path d="M6 22l5-6 4 3 6-7 5 4" />
    <path d="M6 26h20" />
    <circle cx="11" cy="16" r="1.5" />
    <circle cx="15" cy="19" r="1.5" />
    <circle cx="21" cy="12" r="1.5" />
    <circle cx="26" cy="16" r="1.5" />
  </svg>
);

const LightningIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg {...iconProps} {...props}>
    <path d="M18 4L8 18h8l-2 10 10-14h-8l2-10z" />
  </svg>
);

const features: Feature[] = [
  {
    title: 'Prétraitement intelligent',
    description:
      'Vos données sont automatiquement nettoyées, harmonisées et enrichies pour éliminer le bruit et révéler ce qui compte vraiment.',
    Icon: PreprocessIcon,
  },
  {
    title: 'Indicateurs personnalisés',
    description:
      'Créez des métriques adaptées à votre activité, sans configuration complexe ni expertise technique.',
    Icon: MetricsIcon,
  },
  {
    title: 'Analyses instantanées',
    description:
      'Obtenez une compréhension claire et exploitable de vos données en quelques secondes, sans effort supplémentaire.',
    Icon: LightningIcon,
  },
];

const sectionVariants: Variants = {
  hidden: { opacity: 0, y: 24 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.8, ease: [0.22, 1, 0.36, 1] },
  },
};

const featureVariants: Variants = {
  hidden: { opacity: 0, y: 12 },
  visible: (index: number = 0) => ({
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.6,
      ease: [0.22, 1, 0.36, 1],
      delay: index * 0.08,
    },
  }),
};

const AnimatedBorder: React.FC = () => {
  const borderRef = useRef<HTMLDivElement>(null);
  
  const { scrollYProgress } = useScroll({
    target: borderRef,
    offset: ["start 0.8", "end 0.3"]
  });

  const pathLength = useTransform(scrollYProgress, [0, 1], [0, 1]);
  const opacity = useTransform(scrollYProgress, [0, 0.1], [0, 1]);

  return (
    <div ref={borderRef} className={styles.borderWrapper}>
      <svg
        className={styles.borderSvg}
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <motion.rect
          x="1"
          y="1"
          width="98"
          height="98"
          rx="8"
          ry="8"
          fill="none"
          stroke="currentColor"
          strokeWidth="1"
          vectorEffect="non-scaling-stroke"
          style={{
            pathLength,
            opacity,
          }}
        />
      </svg>
    </div>
  );
};

export const Features: React.FC = () => {
  return (
    <motion.section
      id="features"
      className={styles.features}
      initial="hidden"
      whileInView="visible"
      viewport={{ once: true, amount: 0.3 }}
      variants={sectionVariants}
    >
      <Container>
        <div className={styles.inner}>
          <div className={styles.header}>
            <p className={styles.label}>Fonctionnalités</p>
            <h2 className={styles.title}>
              Trois piliers pour accélérer votre analyse
            </h2>
          </div>
          <span className={styles.divider} aria-hidden="true">
            ⸻
          </span>
          <div className={styles.grid}>
            {features.map((feature, index) => {
              const Icon = feature.Icon;

              return (
                <motion.div
                  key={feature.title}
                  className={styles.feature}
                  custom={index}
                  variants={featureVariants}
                >
                  <AnimatedBorder />
                  <div className={styles.featureContent}>
                    <span className={styles.iconWrapper}>
                      <Icon className={styles.icon} />
                    </span>
                    <h3 className={styles.featureTitle}>{feature.title}</h3>
                    <p className={styles.featureDescription}>
                      {feature.description}
                    </p>
                  </div>
                </motion.div>
              );
            })}
          </div>
        </div>
      </Container>
    </motion.section>
  );
};

