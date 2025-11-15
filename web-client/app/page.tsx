import { Hero } from '@/sections/Hero/Hero';
import { Features } from '@/sections/Features/Features';
import { HowItWorks } from '@/sections/HowItWorks/HowItWorks';
import { ForWho } from '@/sections/ForWho/ForWho';
import { SecurityTech } from '@/sections/SecurityTech/SecurityTech';
import { FinalCTA } from '@/sections/FinalCTA/FinalCTA';
import { Footer } from '@/sections/Footer/Footer';

export default function Home() {
  return (
    <>
      <Hero />
      <Features />
      <HowItWorks />
      <ForWho />
      <SecurityTech />
      <FinalCTA />
      <Footer />
    </>
  );
}
