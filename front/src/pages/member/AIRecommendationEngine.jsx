import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import { 
  Sparkles, Zap, Target, BookOpen, 
  ChevronRight, Info, Loader2, GitBranch
} from 'lucide-react';
import { motion } from 'framer-motion';

const AIRecommendationEngine = () => {
  const { data: pathData, isLoading } = useQuery({
    queryKey: ['readingPath'],
    queryFn: memberApi.getReadingPath
  });

  const pathSteps = pathData?.data || [
    { id: 1, title: 'Foundations of Thought', status: 'Completed', detail: 'The starting point of your philosophical query.' },
    { id: 2, title: 'Existential Crisis', status: 'Active', detail: 'Deepening into the themes of isolation.' },
    { id: 3, title: 'The Absurdist Loop', status: 'Suggested', detail: 'Connecting your current reading to Camus.' },
    { id: 4, title: 'Transcendence', status: 'Future', detail: 'The synthesis of your journey.' }
  ];

  return (
    <PageTransition>
      <div className="p-8 space-y-12">
        <div className="max-w-3xl space-y-4">
          <div className="inline-flex items-center text-indigo-600 bg-indigo-50 px-4 py-1 rounded-sm text-[10px] font-bold uppercase tracking-[0.2em] border border-indigo-100">
            <Zap size={14} className="mr-2" /> Neural Engine Active
          </div>
          <h1 className="text-5xl font-display font-bold text-gray-900 leading-tight">Your Personalized Reading Path</h1>
          <p className="text-gray-500 text-lg leading-relaxed">
            Our NVIDIA NIM-powered inference engine analyzes your library interactions to project a multi-dimensional literary journey. Each node represents a synthesis of your interests.
          </p>
        </div>

        {/* Path Visualization (Vertical Timeline Style) */}
        <div className="relative pl-12 space-y-12 before:absolute before:left-[1.35rem] before:top-2 before:bottom-2 before:w-1 before:bg-gradient-to-b before:from-indigo-600 before:to-transparent">
          {isLoading ? (
            <div className="py-24 text-center">
              <Loader2 className="animate-spin text-indigo-500 mx-auto" size={48} />
            </div>
          ) : (
            pathSteps.map((step, index) => (
              <motion.div 
                key={step.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.15 }}
                className="relative group"
              >
                {/* Connector Node */}
                <div className={`absolute -left-[3.15rem] top-1 w-11 h-11 rounded-full border-4 border-white flex items-center justify-center shadow-lg transition-all z-10 ${
                  step.status === 'Completed' ? 'bg-teal-500' : 
                  step.status === 'Active' ? 'bg-indigo-600 scale-110' : 
                  'bg-gray-200'
                }`}>
                  {step.status === 'Completed' ? <Target size={18} className="text-white" /> : <GitBranch size={18} className={step.status === 'Active' ? 'text-white' : 'text-gray-400'} />}
                </div>

                {/* Content Card */}
                <div className={`p-8 rounded-sm shadow-2xl border transition-all hover:translate-x-4 max-w-2xl ${
                  step.status === 'Active' ? 'bg-white border-indigo-200 ring-2 ring-indigo-50' : 'bg-white border-gray-100'
                }`}>
                  <div className="flex justify-between items-start mb-4">
                    <span className={`text-[10px] font-mono uppercase font-bold tracking-widest ${
                      step.status === 'Active' ? 'text-indigo-600' : 'text-gray-400'
                    }`}>
                      {step.status} Node
                    </span>
                    <Sparkles size={16} className={`${step.status === 'Active' ? 'text-indigo-500 animate-pulse' : 'text-gray-200'}`} />
                  </div>
                  <h3 className="text-2xl font-display font-bold text-gray-900 mb-2">{step.title}</h3>
                  <p className="text-gray-500 text-sm leading-relaxed mb-6">{step.detail}</p>
                  
                  {step.status === 'Active' && (
                    <button className="flex items-center text-indigo-600 font-bold uppercase tracking-widest text-xs hover:translate-x-2 transition-transform">
                      Synchronize with this title <ChevronRight size={16} className="ml-2" />
                    </button>
                  )}
                </div>
              </motion.div>
            ))
          )}
        </div>

        {/* Global Stats Overlay */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 pt-12">
           <div className="bg-gray-900 rounded-sm p-8 text-white relative overflow-hidden group">
              <div className="relative z-10 space-y-4">
                <span className="text-[10px] font-bold text-indigo-400 uppercase tracking-widest">Inference Confidence</span>
                <p className="text-4xl font-display font-bold">94%</p>
                <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                   <div className="w-[94%] h-full bg-indigo-500" />
                </div>
              </div>
              <Sparkles className="absolute top-2 right-2 opacity-10 group-hover:scale-150 transition-transform" size={100} />
           </div>
           
           <div className="bg-white border border-gray-100 rounded-sm p-8 flex flex-col justify-between">
              <div className="space-y-4">
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Reading Velocity</span>
                <p className="text-4xl font-display font-bold text-gray-900">1.2x</p>
                <p className="text-xs text-gray-500">Accelerating above community average.</p>
              </div>
           </div>

           <div className="bg-white border border-gray-100 rounded-sm p-8 flex flex-col justify-between">
              <div className="space-y-4">
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Category Diversity</span>
                <p className="text-4xl font-display font-bold text-gray-900">High</p>
                <p className="text-xs text-gray-500">Exploring 8 distinct literary domains.</p>
              </div>
           </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default AIRecommendationEngine;
