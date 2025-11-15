import type { Metadata } from "next";
import './globals.scss';
import { Navbar } from '@/components/Navbar/Navbar';

export const metadata: Metadata = {
  title: "Varlor - Plateforme d&rsquo;analyse de données",
  description: "Prétraitement intelligent et indicateurs personnalisés pour vos données",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr">
      <body>
        <Navbar />
        <main>{children}</main>
      </body>
    </html>
  );
}
