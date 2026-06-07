import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { staffApi } from '../../api/staff.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { 
  Search, Mail, Loader2, ChevronRight
} from 'lucide-react';

const StaffMembersPage = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: membersData, isLoading } = useQuery({
    queryKey: ['staffMembers'],
    queryFn: staffApi.getMembers
  });

  const members = membersData?.members || [];
  const filteredMembers = members.filter((member) =>
    member.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    member.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    member.memberId?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div>
          <h1 className="text-3xl font-display font-bold text-gray-900">Member Directory</h1>
          <p className="text-gray-500 text-sm mt-1">Verify eligibility and manage existing member accounts.</p>
        </div>

        <div className="relative max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Search by name, email, or ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-12 pr-4 py-3 bg-white border border-gray-200 focus:outline-none focus:border-teal-500 font-medium"
          />
        </div>

        <div className="bg-white border border-gray-200 overflow-hidden">
          <div className="grid grid-cols-12 gap-4 px-6 py-4 bg-gray-50 border-b border-gray-200 text-[10px] font-mono uppercase tracking-widest text-gray-500">
            <div className="col-span-4">Member Info</div>
            <div className="col-span-3">Contact</div>
            <div className="col-span-2 text-center">Active Loans</div>
            <div className="col-span-2">Membership</div>
            <div className="col-span-1"></div>
          </div>

          <div className="divide-y divide-gray-100">
            {isLoading ? (
              <div className="py-20 text-center">
                <Loader2 className="animate-spin text-teal-500 mx-auto" size={32} />
              </div>
            ) : filteredMembers.length === 0 ? (
              <div className="py-20 text-center text-gray-400">
                No members found matching your query.
              </div>
            ) : (
              filteredMembers.map((member) => (
                <div key={member.memberId} className="grid grid-cols-12 gap-4 px-6 py-6 items-center hover:bg-gray-50/50 transition-colors group">
                  <div className="col-span-4 flex items-center space-x-4">
                    <div className="w-10 h-10 bg-teal-50 text-teal-600 flex items-center justify-center font-bold text-lg">
                      {member.name[0]}
                    </div>
                    <div>
                      <p className="font-bold text-gray-900 leading-none">{member.name}</p>
                      <p className="text-[10px] uppercase font-mono text-gray-400 mt-2">ID: {member.memberId}</p>
                    </div>
                  </div>
                  
                  <div className="col-span-3">
                    <div className="flex items-center text-xs text-gray-600">
                      <Mail size={12} className="mr-2 opacity-50" /> {member.email}
                    </div>
                  </div>

                  <div className="col-span-2 text-center font-mono font-bold text-gray-900">
                    {member.activeLoanCount || 0}
                  </div>

                  <div className="col-span-2">
                    <Badge variant={member.isActive ? 'teal' : 'red'}>
                      {member.membershipType || (member.isActive ? 'Active' : 'Inactive')}
                    </Badge>
                  </div>

                  <div className="col-span-1 text-right">
                    <button className="p-2 text-gray-400 hover:text-teal-600 hover:bg-teal-50 transition-all opacity-0 group-hover:opacity-100">
                      <ChevronRight size={20} />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default StaffMembersPage;
