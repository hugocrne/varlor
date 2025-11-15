'use client';

import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';
import styles from './HeroSphere.module.scss';

interface HeroSphereProps {
  className?: string;
  pointCount?: number;
}

// Fonction d'easing ease-out
function easeOut(t: number): number {
  return 1 - Math.pow(1 - t, 3);
}

// Couleurs de la palette premium noir/blanc/gris
const COLOR_WHITE = new THREE.Color(0xEEEEEE); // #EEE - 70%
const COLOR_GRAY_LIGHT = new THREE.Color(0xAAAAAA); // #AAA - 20%
const COLOR_GRAY_DARK = new THREE.Color(0x555555); // #555 - 10%

export const HeroSphere: React.FC<HeroSphereProps> = ({
  className,
  pointCount = 400,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const sceneRef = useRef<THREE.Scene | null>(null);
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const pointsRef = useRef<THREE.Points | null>(null);
  const animationFrameRef = useRef<number | null>(null);
  
  const pointsDataRef = useRef<Array<{
    delay: number;
    startTime: number;
    isVisible: boolean;
  }>>([]);
  
  const startTimeRef = useRef<number>(0);
  const isAnimatingRef = useRef<boolean>(true);

  // Initialisation Three.js
  useEffect(() => {
    if (!containerRef.current) return;

    const initThree = async () => {
      const container = containerRef.current;
      if (!container) return;

      const width = container.clientWidth;
      const height = container.clientHeight;

      // Scene (pas d'éclairage)
      const scene = new THREE.Scene();
      sceneRef.current = scene;

      // Camera centrée
      const camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000);
      camera.position.set(0, 0, 5);
      camera.lookAt(0, 0, 0);
      cameraRef.current = camera;

      // Renderer
      const renderer = new THREE.WebGLRenderer({ 
        antialias: true,
        alpha: true,
        powerPreference: 'high-performance'
      });
      renderer.setSize(width, height);
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
      renderer.setClearColor(0x000000, 0); // Fond transparent
      container.appendChild(renderer.domElement);
      rendererRef.current = renderer;

      // Génération des points sur la sphère (Fibonacci Sphere)
      const positions = new Float32Array(pointCount * 3);
      const opacities = new Float32Array(pointCount);
      const colors = new Float32Array(pointCount * 3);
      
      const pointsData: typeof pointsDataRef.current = [];
      const sphereRadius = 2.5;

      // Distribution des couleurs : 70% blanc, 20% gris clair, 10% gris foncé
      const whiteCount = Math.floor(pointCount * 0.7);
      const grayLightCount = Math.floor(pointCount * 0.2);
      // Le reste sera gris foncé

      // Créer un tableau de couleurs mélangées
      const colorArray: THREE.Color[] = [];
      for (let i = 0; i < whiteCount; i++) colorArray.push(COLOR_WHITE);
      for (let i = 0; i < grayLightCount; i++) colorArray.push(COLOR_GRAY_LIGHT);
      for (let i = colorArray.length; i < pointCount; i++) colorArray.push(COLOR_GRAY_DARK);
      
      // Mélanger aléatoirement
      for (let i = colorArray.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [colorArray[i], colorArray[j]] = [colorArray[j], colorArray[i]];
      }

      // Distribution uniforme sur la sphère
      for (let i = 0; i < pointCount; i++) {
        const i3 = i * 3;
        
        // Fibonacci Sphere distribution
        const theta = Math.acos(1 - 2 * (i + 0.5) / pointCount); // Latitude
        const phi = Math.PI * (1 + Math.sqrt(5)) * i; // Longitude (golden angle)
        
        // Sphère centrée à l'origine (sera décalée via le groupe)
        const x = sphereRadius * Math.sin(theta) * Math.cos(phi);
        const y = sphereRadius * Math.sin(theta) * Math.sin(phi);
        const z = sphereRadius * Math.cos(theta);
        
        positions[i3] = x;
        positions[i3 + 1] = y;
        positions[i3 + 2] = z;

        // Opacité initiale à 0
        opacities[i] = 0;

        // Couleur assignée selon la répartition
        const color = colorArray[i];
        colors[i3] = color.r;
        colors[i3 + 1] = color.g;
        colors[i3 + 2] = color.b;

        // Délai aléatoire entre 0 et 1200ms (1.2 secondes)
        const delay = Math.random() * 1.2;
        pointsData.push({
          delay,
          startTime: 0,
          isVisible: false,
        });
      }

      pointsDataRef.current = pointsData;

      // Geometry
      const geometry = new THREE.BufferGeometry();
      geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
      geometry.setAttribute('opacity', new THREE.BufferAttribute(opacities, 1));
      geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

      // Shader minimaliste pour points avec opacité par vertex et vertexColors
      // Note: 'color' est déjà fourni par Three.js quand vertexColors: true
      const vertexShader = `
        attribute float opacity;
        varying float vOpacity;
        varying vec3 vColor;
        
        void main() {
          vOpacity = opacity;
          vColor = color;
          vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
          gl_PointSize = 0.3 * (300.0 / -mvPosition.z);
          gl_Position = projectionMatrix * mvPosition;
        }
      `;
      
      const fragmentShader = `
        varying float vOpacity;
        varying vec3 vColor;
        
        void main() {
          float distanceToCenter = distance(gl_PointCoord, vec2(0.5));
          
          // Point rond net sans artefacts
          float alpha = step(distanceToCenter, 0.5);
          
          gl_FragColor = vec4(vColor, alpha * vOpacity);
        }
      `;

      // Material avec shader minimaliste
      const material = new THREE.ShaderMaterial({
        vertexShader,
        fragmentShader,
        transparent: true,
        vertexColors: true,
        blending: THREE.NormalBlending,
        depthWrite: false,
      });

      // Points
      const points = new THREE.Points(geometry, material);
      
      // Groupe pour décaler et faire tourner la sphère sur elle-même
      const sphereGroup = new THREE.Group();
      sphereGroup.position.x = 2.5; // Décaler vers la droite
      sphereGroup.add(points);
      scene.add(sphereGroup);
      
      pointsRef.current = points;

      // Animation loop
      const startTime = performance.now() / 1000; // en secondes
      startTimeRef.current = startTime;
      let lastTime = startTime;
      
      const animate = () => {
        animationFrameRef.current = requestAnimationFrame(animate);
        
        const currentTime = performance.now() / 1000;
        const elapsedTime = currentTime - startTime;
        const deltaTime = currentTime - lastTime;
        lastTime = currentTime;
        
        // Mise à jour de l'opacité pour chaque point
        if (pointsRef.current && pointsDataRef.current.length > 0) {
          const geometry = pointsRef.current.geometry;
          const opacityAttribute = geometry.getAttribute('opacity') as THREE.BufferAttribute;
          let allVisible = true;
          
          pointsDataRef.current.forEach((pointData, i) => {
            const animationStartTime = pointData.delay;
            const animationDuration = 0.8; // 800ms
            
            if (elapsedTime >= animationStartTime) {
              const localTime = elapsedTime - animationStartTime;
              const progress = Math.min(localTime / animationDuration, 1);
              const eased = easeOut(progress);
              
              opacityAttribute.setX(i, eased);
              pointData.isVisible = true;
            } else {
              opacityAttribute.setX(i, 0);
              allVisible = false;
            }
          });
          
          opacityAttribute.needsUpdate = true;
          
          // Arrêter l'animation de fade-in une fois que tous les points sont visibles
          if (allVisible && isAnimatingRef.current) {
            isAnimatingRef.current = false;
          }
        }

        // Rotation lente uniquement sur l'axe Y (0.15 rad/s)
        if (pointsRef.current && pointsRef.current.parent) {
          pointsRef.current.parent.rotation.y += 0.15 * deltaTime;
        }

        if (renderer && scene && camera) {
          renderer.render(scene, camera);
        }
      };

      animate();

      // Gestion du resize
      const handleResize = () => {
        if (!container || !camera || !renderer) return;
        
        const newWidth = container.clientWidth;
        const newHeight = container.clientHeight;
        
        camera.aspect = newWidth / newHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(newWidth, newHeight);
        
        // Recalculer la position de la caméra avec la nouvelle taille
        camera.position.set(0, 0, 5);
        camera.lookAt(0, 0, 0);
      };

      const resizeObserver = new ResizeObserver(handleResize);
      resizeObserver.observe(container);

      // Cleanup
      return () => {
        resizeObserver.disconnect();
        if (animationFrameRef.current !== null) {
          cancelAnimationFrame(animationFrameRef.current);
          animationFrameRef.current = null;
        }
        if (renderer && container && renderer.domElement && container.contains(renderer.domElement)) {
          container.removeChild(renderer.domElement);
        }
        if (geometry) geometry.dispose();
        if (material) material.dispose();
        sceneRef.current = null;
        rendererRef.current = null;
        cameraRef.current = null;
        pointsRef.current = null;
      };
    };

    initThree();
  }, [pointCount]);


  return (
    <div 
      ref={containerRef} 
      className={`${styles.container} ${className || ''}`}
    />
  );
};

