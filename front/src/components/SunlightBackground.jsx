import { useRef, useMemo, useState, useEffect } from 'react';
import { Canvas, useFrame, useThree } from '@react-three/fiber';
import * as THREE from 'three';

// Volumetric Sunrays Shader
const SunrayShader = {
  uniforms: {
    uTime: { value: 0 },
    uColorStart: { value: new THREE.Color('#F59E0B') },
    uColorEnd: { value: new THREE.Color('#FCD34D') },
    uOpacity: { value: 0.15 },
  },
  vertexShader: `
    varying vec2 vUv;
    varying float vY;
    void main() {
      vUv = uv;
      vY = position.y;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
  `,
  fragmentShader: `
    uniform float uTime;
    uniform vec3 uColorStart;
    uniform vec3 uColorEnd;
    uniform float uOpacity;
    varying vec2 vUv;
    varying float vY;
    
    float noise(vec2 p) {
      return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
    }
    
    void main() {
      float ray = smoothstep(0.0, 0.3, vUv.x) * smoothstep(1.0, 0.7, vUv.x);
      ray *= smoothstep(0.0, 0.2, vUv.y);
      
      float wave = sin(vUv.y * 8.0 + uTime * 0.5) * 0.1;
      ray *= (1.0 + wave);
      
      float dust = noise(vUv * 100.0 + uTime) * 0.1;
      ray += dust * 0.2;
      
      vec3 color = mix(uColorStart, uColorEnd, vUv.y);
      float alpha = ray * uOpacity * (1.0 - vUv.y * 0.5);
      
      gl_FragColor = vec4(color, alpha);
    }
  `,
  transparent: true,
  side: THREE.DoubleSide,
  depthWrite: false,
  blending: THREE.AdditiveBlending,
};

// Sunray Component
function Sunray({ angle, delay = 0 }) {
  const meshRef = useRef();
  const materialRef = useRef();
  
  useFrame((state) => {
    if (materialRef.current) {
      materialRef.current.uniforms.uTime.value = state.clock.elapsedTime + delay;
    }
  });
  
  const geometry = useMemo(() => {
    const geo = new THREE.PlaneGeometry(3, 12, 32, 32);
    return geo;
  }, []);
  
  return (
    <mesh
      ref={meshRef}
      geometry={geometry}
      position={[Math.cos(angle) * 2, 3, Math.sin(angle) * 2]}
      rotation={[0.2, -angle + Math.PI / 2, 0.1]}
    >
      <shaderMaterial
        ref={materialRef}
        uniforms={SunrayShader.uniforms}
        vertexShader={SunrayShader.vertexShader}
        fragmentShader={SunrayShader.fragmentShader}
        transparent={SunrayShader.transparent}
        side={SunrayShader.side}
        depthWrite={SunrayShader.depthWrite}
        blending={SunrayShader.blending}
      />
    </mesh>
  );
}

// Floating Dust Motes - Performance optimized to 60 particles
function DustMotes({ count = 60 }) {
  const meshRef = useRef();
  const { viewport } = useThree();
  
  const [positions, velocities] = useMemo(() => {
    const pos = new Float32Array(count * 3);
    const vel = [];
    for (let i = 0; i < count; i++) {
      pos[i * 3] = (Math.random() - 0.5) * 15;
      pos[i * 3 + 1] = (Math.random() - 0.5) * 10 + 2;
      pos[i * 3 + 2] = (Math.random() - 0.5) * 10;
      vel.push({
        x: (Math.random() - 0.5) * 0.005,
        y: (Math.random() - 0.5) * 0.005 + 0.002,
        z: (Math.random() - 0.5) * 0.003,
      });
    }
    return [pos, vel];
  }, [count]);
  
  useFrame(() => {
    if (!meshRef.current) return;
    const pos = meshRef.current.geometry.attributes.position.array;
    for (let i = 0; i < count; i++) {
      pos[i * 3] += velocities[i].x;
      pos[i * 3 + 1] += velocities[i].y;
      pos[i * 3 + 2] += velocities[i].z;
      
      // Reset if out of bounds
      if (pos[i * 3 + 1] > 6) pos[i * 3 + 1] = -4;
      if (pos[i * 3] > 8) pos[i * 3] = -8;
      if (pos[i * 3] < -8) pos[i * 3] = 8;
    }
    meshRef.current.geometry.attributes.position.needsUpdate = true;
  });
  
  return (
    <points ref={meshRef}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          count={count}
          array={positions}
          itemSize={3}
        />
      </bufferGeometry>
      <pointsMaterial
        size={0.03}
        color="#FCD34D"
        transparent
        opacity={0.6}
        sizeAttenuation
        blending={THREE.AdditiveBlending}
      />
    </points>
  );
}

// God Ray System
function GodRaySystem() {
  const groupRef = useRef();
  
  useFrame((state) => {
    if (groupRef.current) {
      groupRef.current.rotation.y = Math.sin(state.clock.elapsedTime * 0.1) * 0.05;
    }
  });
  
  const rays = useMemo(() => {
    return Array.from({ length: 7 }, (_, i) => ({
      angle: (i / 6) * Math.PI * 0.4 - Math.PI * 0.2,
      delay: i * 0.5,
    }));
  }, []);
  
  return (
    <group ref={groupRef} position={[-4, 3, -2]}>
      {rays.map((ray, i) => (
        <Sunray key={i} angle={ray.angle} delay={ray.delay} />
      ))}
    </group>
  );
}

// Main Scene - Using 60 dust motes for performance
function Scene() {
  return (
    <>
      <color attach="background" args={['#1a1814']} />
      <fog attach="fog" args={['#1a1814', 8, 20]} />
      <ambientLight intensity={0.3} color="#F59E0B" />
      <pointLight position={[-4, 4, 2]} intensity={2} color="#FCD34D" distance={20} />
      <GodRaySystem />
      <DustMotes count={60} />
    </>
  );
}

// CSS Fallback Background with animated dust particles
function CSSFallbackBackground() {
  return (
    <div 
      className="fixed inset-0 w-full h-full"
      style={{
        background: `
          radial-gradient(ellipse at 0% 0%, rgba(251, 191, 36, 0.15) 0%, transparent 50%),
          radial-gradient(ellipse at 20% 10%, rgba(245, 158, 11, 0.1) 0%, transparent 40%),
          linear-gradient(135deg, #2C1810 0%, #1a1814 50%, #0f0e0c 100%)
        `,
      }}
    >
      {/* Animated sun rays using CSS */}
      <div className="absolute inset-0 overflow-hidden">
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className="absolute origin-top-left"
            style={{
              left: '0%',
              top: '0%',
              width: '2px',
              height: '120%',
              background: `linear-gradient(180deg, rgba(251, 191, 36, ${0.3 - i * 0.05}) 0%, transparent 100%)`,
              transform: `rotate(${25 + i * 8}deg)`,
              animation: `sunray-pulse ${3 + i * 0.5}s ease-in-out infinite`,
              animationDelay: `${i * 0.3}s`,
            }}
          />
        ))}
      </div>
      
      {/* CSS Dust particles */}
      <div className="absolute inset-0">
        {[...Array(30)].map((_, i) => (
          <div
            key={`dust-${i}`}
            className="absolute w-1 h-1 bg-amber-400/40 rounded-full"
            style={{
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
              animation: `float ${5 + Math.random() * 5}s ease-in-out infinite`,
              animationDelay: `${Math.random() * 5}s`,
            }}
          />
        ))}
      </div>
      
      <style>{`
        @keyframes sunray-pulse {
          0%, 100% { opacity: 0.3; }
          50% { opacity: 0.6; }
        }
        @keyframes float {
          0%, 100% { transform: translateY(0) translateX(0); opacity: 0.3; }
          25% { transform: translateY(-20px) translateX(10px); opacity: 0.6; }
          50% { transform: translateY(-10px) translateX(-5px); opacity: 0.4; }
          75% { transform: translateY(-30px) translateX(5px); opacity: 0.5; }
        }
      `}</style>
    </div>
  );
}

export default function SunlightBackground() {
  const [webglFailed, setWebglFailed] = useState(false);
  
  // Check for WebGL support
  useEffect(() => {
    try {
      const canvas = document.createElement('canvas');
      const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
      if (!gl) {
        setWebglFailed(true);
      }
    } catch (e) {
      setWebglFailed(true);
    }
  }, []);
  
  // If WebGL failed, show CSS fallback
  if (webglFailed) {
    return (
      <div style={{ zIndex: -1, position: 'fixed', inset: 0 }}>
        <CSSFallbackBackground />
      </div>
    );
  }
  
  return (
    <div 
      className="fixed inset-0"
      style={{ zIndex: -1 }}
    >
      <Canvas
        camera={{ position: [0, 0, 8], fov: 60 }}
        dpr={1.5}
        gl={{ 
          antialias: false,
          alpha: true,
          powerPreference: 'high-performance',
        }}
        style={{ 
          background: 'linear-gradient(135deg, #2C1810 0%, #1a1814 50%, #0f0e0c 100%)',
          width: '100vw',
          height: '100vh',
          position: 'fixed',
          top: 0,
          left: 0,
        }}
        onError={() => setWebglFailed(true)}
      >
        <Scene />
      </Canvas>
    </div>
  );
}
