import React from 'react';
import styles from './Card.module.scss';

interface CardProps {
  children?: React.ReactNode;
  className?: string;
  title?: string;
  description?: string;
}

export const Card: React.FC<CardProps> = ({
  children,
  className,
  title,
  description,
}) => {
  return (
    <div className={`${styles.card} ${className || ''}`}>
      {title && <h3 className={styles.title}>{title}</h3>}
      {description && <p className={styles.description}>{description}</p>}
      {children}
    </div>
  );
};

