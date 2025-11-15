import React from 'react';
import styles from './Button.module.scss';

interface ButtonProps {
  variant?: 'primary' | 'secondary';
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
}

interface ButtonAsLinkProps extends ButtonProps {
  as: 'a';
  href: string;
}

interface ButtonAsButtonProps extends ButtonProps {
  as?: 'button';
  type?: 'button' | 'submit' | 'reset';
  disabled?: boolean;
}

export const Button: React.FC<ButtonAsLinkProps | ButtonAsButtonProps> = ({
  variant = 'primary',
  children,
  className,
  ...props
}) => {
  const baseClassName = `${styles.button} ${styles[variant]} ${className || ''}`;

  if (props.as === 'a' && 'href' in props) {
    const { href, ...anchorProps } = props as ButtonAsLinkProps;
    return (
      <a href={href} className={baseClassName} {...anchorProps}>
        {children}
      </a>
    );
  }

  const { type = 'button', disabled, onClick } = props as ButtonAsButtonProps;
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={baseClassName}
    >
      {children}
    </button>
  );
};

