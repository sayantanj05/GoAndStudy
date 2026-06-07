import React from 'react';
import PageTransition from '../../components/shared/PageTransition';
import { RotateCcw } from 'lucide-react';

const MemberRenewalRequestPage = () => (
  <PageTransition>
    <div className="p-8 space-y-8 text-center pt-24">
      <RotateCcw size={48} className="mx-auto mb-6 text-amber-500" />
      <h1 className="text-4xl font-display font-bold">Shelf Extension</h1>
      <p className="text-gray-500 max-w-md mx-auto">Requested renewals for your current holdings will appear here once submitted. You can renew a book once for an additional 7 days.</p>
    </div>
  </PageTransition>
);

export default MemberRenewalRequestPage;
