import React from 'react';
import { useParams } from 'react-router-dom';
import PageTransition from '../../components/shared/PageTransition';
import { User } from 'lucide-react';

const MemberAuthorPage = () => {
  const { id } = useParams();
  return (
    <PageTransition>
      <div className="p-8 space-y-8 text-center pt-24">
        <div className="w-24 h-24 bg-indigo-50 text-indigo-600 rounded-full flex items-center justify-center mx-auto mb-6">
          <User size={48} />
        </div>
        <h1 className="text-4xl font-display font-bold">Author Identity: {id}</h1>
        <p className="text-gray-500 max-w-md mx-auto">Detailed categorical analysis for this author is currently being synthesized by the GoAndStudy Neural Engine.</p>
      </div>
    </PageTransition>
  );
};

export default MemberAuthorPage;
