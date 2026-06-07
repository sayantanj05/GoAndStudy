import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import { 
  Target, BookOpen, Calendar, Trophy, Clock, 
  Plus, Trash2, Check, AlertCircle, ChevronRight,
  LayoutList, Flag, TrendingUp, X, Search, Loader2
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getDayOfYear } from 'date-fns';

const MemberReadingGoalPage = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('challenge'); // challenge, queue, milestones
  
  // Challenge/Goal States
  const [showGoalModal, setShowGoalModal] = useState(false);
  const [targetBooks, setTargetBooks] = useState(12);
  
  // Queue States
  const [showQueueModal, setShowQueueModal] = useState(false);
  const [queueName, setQueueName] = useState('');
  const [selectedBooks, setSelectedBooks] = useState([]);
  const [bookSearchQuery, setBookSearchQuery] = useState('');
  
  // Milestone States
  const [showMilestoneModal, setShowMilestoneModal] = useState(false);
  const [selectedBookForMilestone, setSelectedBookForMilestone] = useState(null);
  const [targetDate, setTargetDate] = useState('');
  const [milestoneNotes, setMilestoneNotes] = useState('');

  // Queries
  const { data: goalData, isLoading: goalLoading } = useQuery({
    queryKey: ['readingGoal'],
    queryFn: memberApi.getReadingGoal
  });

  const { data: queuesData, isLoading: queuesLoading } = useQuery({
    queryKey: ['bookQueues'],
    queryFn: memberApi.getQueues
  });

  const { data: milestonesData, isLoading: milestonesLoading } = useQuery({
    queryKey: ['bookMilestones'],
    queryFn: memberApi.getMilestones
  });

  const { data: searchResults, isLoading: searchLoading } = useQuery({
    queryKey: ['bookSearch', bookSearchQuery],
    queryFn: () => memberApi.searchBooks(bookSearchQuery),
    enabled: bookSearchQuery.length > 2
  });

  // Mutations
  const setGoalMutation = useMutation({
    mutationFn: () => memberApi.setReadingGoal(targetBooks, new Date().getFullYear()),
    onSuccess: () => {
      queryClient.invalidateQueries(['readingGoal']);
      setShowGoalModal(false);
      toast.success('Reading goal set successfully!');
    },
    onError: () => toast.error('Failed to set reading goal')
  });

  const deleteGoalMutation = useMutation({
    mutationFn: memberApi.deleteReadingGoal,
    onSuccess: () => {
      queryClient.invalidateQueries(['readingGoal']);
      toast.success('Reading goal deleted');
    },
    onError: () => toast.error('Failed to delete goal')
  });

  const createQueueMutation = useMutation({
    mutationFn: () => memberApi.createQueue(queueName || 'My Reading Queue', selectedBooks.map(b => b.bookId)),
    onSuccess: () => {
      queryClient.invalidateQueries(['bookQueues']);
      setShowQueueModal(false);
      setSelectedBooks([]);
      setQueueName('');
      toast.success('Reading queue created!');
    },
    onError: () => toast.error('Failed to create queue')
  });

  const deleteQueueMutation = useMutation({
    mutationFn: (queueId) => memberApi.deleteQueue(queueId),
    onSuccess: () => {
      queryClient.invalidateQueries(['bookQueues']);
      toast.success('Queue deleted');
    }
  });

  const markBookCompletedMutation = useMutation({
    mutationFn: ({ queueId, bookId }) => memberApi.markQueueBookCompleted(queueId, bookId),
    onSuccess: () => {
      queryClient.invalidateQueries(['bookQueues']);
      toast.success('Book marked as completed!');
    }
  });

  const createMilestoneMutation = useMutation({
    mutationFn: () => memberApi.createMilestone(
      selectedBookForMilestone.bookId, 
      targetDate, 
      milestoneNotes
    ),
    onSuccess: () => {
      queryClient.invalidateQueries(['bookMilestones']);
      setShowMilestoneModal(false);
      setSelectedBookForMilestone(null);
      setTargetDate('');
      setMilestoneNotes('');
      toast.success('Milestone created!');
    },
    onError: () => toast.error('Failed to create milestone')
  });

  const deleteMilestoneMutation = useMutation({
    mutationFn: (milestoneId) => memberApi.deleteMilestone(milestoneId),
    onSuccess: () => {
      queryClient.invalidateQueries(['bookMilestones']);
      toast.success('Milestone deleted');
    }
  });

  const completeMilestoneMutation = useMutation({
    mutationFn: (milestoneId) => memberApi.completeMilestone(milestoneId),
    onSuccess: () => {
      queryClient.invalidateQueries(['bookMilestones']);
      toast.success('Congratulations! Milestone completed!');
    }
  });

  const updateProgressMutation = useMutation({
    mutationFn: ({ milestoneId, progress }) => memberApi.updateMilestoneProgress(milestoneId, progress),
    onSuccess: () => queryClient.invalidateQueries(['bookMilestones'])
  });

  const goal = goalData?.data || {};
  const queues = queuesData?.data?.queues || [];
  const milestones = milestonesData?.data?.milestones || [];

  // Derived stats
  const completedMilestones = milestones.filter(m => m.status === 'COMPLETED').length;
  const atRiskMilestones = milestones.filter(m => m.status === 'AT_RISK').length;
  const overdueMilestones = milestones.filter(m => m.status === 'OVERDUE').length;

  const openGoalModal = () => {
    setTargetBooks(goal?.targetBooks || 12);
    setShowGoalModal(true);
  };

  const getStatusColor = (status) => {
    switch(status) {
      case 'COMPLETED': return 'bg-green-100 text-green-700';
      case 'ON_TRACK': return 'bg-blue-100 text-blue-700';
      case 'AT_RISK': return 'bg-amber-100 text-amber-700';
      case 'OVERDUE': return 'bg-red-100 text-red-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-display font-bold text-gray-900">Literary Milestones</h1>
            <p className="text-gray-500 mt-1">Set reading objectives and track your journey</p>
          </div>
          <div className="flex bg-white border border-gray-200 rounded-sm p-1">
            {[
              { id: 'challenge', label: 'Challenge', icon: Trophy },
              { id: 'queue', label: 'Queue', icon: LayoutList },
              { id: 'milestones', label: 'Milestones', icon: Flag },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-sm transition-colors ${
                  activeTab === tab.id 
                    ? 'bg-indigo-600 text-white' 
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                <tab.icon size={16} />
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Challenge Tab */}
        {activeTab === 'challenge' && (
          <div className="space-y-6">
            {goal.hasGoal ? (
              <div className="bg-white border border-gray-200 rounded-lg p-8">
                <div className="flex items-center justify-between mb-6">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-indigo-100 rounded-full flex items-center justify-center">
                      <Trophy className="text-indigo-600" size={24} />
                    </div>
                    <div>
                      <h2 className="text-xl font-bold">{goal.year} Reading Challenge</h2>
                      <p className="text-gray-500 text-sm">{goal.booksReadSoFar} of {goal.targetBooks} books completed</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-3xl font-bold text-indigo-600">{Math.round(goal.progressPercent)}%</div>
                    <p className="text-sm text-gray-500">{goal.isCompleted ? 'Challenge Complete!' : 'In Progress'}</p>
                  </div>
                </div>

                {/* Progress Bar */}
                <div className="w-full bg-gray-200 rounded-full h-4 mb-6">
                  <div 
                    className="bg-indigo-600 h-4 rounded-full transition-all duration-500"
                    style={{ width: `${Math.min(goal.progressPercent, 100)}%` }}
                  />
                </div>

                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 text-center">
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="text-2xl font-bold text-gray-900">{goal.booksReadSoFar}</div>
                    <p className="text-sm text-gray-500">Books Read</p>
                  </div>
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="text-2xl font-bold text-gray-900">{goal.remainingBooks ?? Math.max(0, goal.targetBooks - goal.booksReadSoFar)}</div>
                    <p className="text-sm text-gray-500">Books Remaining</p>
                  </div>
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="text-2xl font-bold text-gray-900">
                      {(goal.booksPerWeekNeeded ?? ((goal.targetBooks - goal.booksReadSoFar) / Math.max(1, Math.ceil((365 - getDayOfYear(new Date())) / 7)))).toFixed(1)}
                    </div>
                    <p className="text-sm text-gray-500">Books/Week Needed</p>
                  </div>
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="text-2xl font-bold text-gray-900">{goal.daysRemaining ?? Math.max(0, 365 - getDayOfYear(new Date()))}</div>
                    <p className="text-sm text-gray-500">Days Remaining</p>
                  </div>
                </div>

                <div className="mt-6 flex flex-wrap gap-3">
                  <button
                    onClick={openGoalModal}
                    className="px-4 py-2 bg-indigo-600 text-white rounded-sm hover:bg-indigo-700 text-sm font-medium"
                  >
                    Edit Challenge
                  </button>
                  <button
                    onClick={() => deleteGoalMutation.mutate()}
                    className="px-4 py-2 border border-red-200 text-red-600 hover:bg-red-50 text-sm font-medium"
                  >
                    Delete Challenge
                  </button>
                </div>
              </div>
            ) : (
              <div className="bg-white border border-gray-200 rounded-lg p-12 text-center">
                <Target size={48} className="mx-auto mb-4 text-indigo-400" />
                <h2 className="text-2xl font-bold mb-2">Set Your Reading Challenge</h2>
                <p className="text-gray-500 mb-6 max-w-md mx-auto">
                  How many books do you want to read this year? Set a goal and track your progress!
                </p>
                <button
                  onClick={openGoalModal}
                  className="px-6 py-3 bg-indigo-600 text-white font-bold rounded-sm hover:bg-indigo-700"
                >
                  Start Challenge
                </button>
              </div>
            )}
          </div>
        )}

        {/* Queue Tab */}
        {activeTab === 'queue' && (
          <div className="space-y-6">
            <div className="flex justify-between items-center">
              <h2 className="text-xl font-bold">Reading Queues</h2>
              <button
                onClick={() => setShowQueueModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-sm hover:bg-indigo-700"
              >
                <Plus size={18} />
                Create Queue
              </button>
            </div>

            {queues.length === 0 ? (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-12 text-center">
                <LayoutList size={48} className="mx-auto mb-4 text-gray-400" />
                <h3 className="text-lg font-bold mb-2">No Reading Queues</h3>
                <p className="text-gray-500 mb-4">Create a sequential reading list to organize your books</p>
                <button
                  onClick={() => setShowQueueModal(true)}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-sm hover:bg-indigo-700"
                >
                  Create Your First Queue
                </button>
              </div>
            ) : (
              <div className="grid gap-4">
                {queues.map(queue => (
                  <div key={queue.id} className="bg-white border border-gray-200 rounded-lg p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <h3 className="text-lg font-bold">{queue.queueName}</h3>
                        <p className="text-sm text-gray-500">
                          {queue.books?.length || 0} books • {queue.books?.filter(b => b.status === 'COMPLETED').length || 0} completed
                        </p>
                      </div>
                      <div className="flex gap-2">
                        {queue.isActive && (
                          <span className="px-2 py-1 bg-green-100 text-green-700 text-xs rounded font-medium">
                            Active
                          </span>
                        )}
                        <button
                          onClick={() => deleteQueueMutation.mutate(queue.id)}
                          className="p-1 text-gray-400 hover:text-red-600"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </div>

                    <div className="space-y-2">
                      {queue.books?.map((book, index) => (
                        <div 
                          key={book.bookId}
                          className={`flex items-center gap-3 p-3 rounded-lg ${
                            book.status === 'READING' ? 'bg-indigo-50 border border-indigo-200' :
                            book.status === 'COMPLETED' ? 'bg-green-50' : 'bg-gray-50'
                          }`}
                        >
                          <div className="w-8 h-8 rounded-full bg-white border-2 border-gray-300 flex items-center justify-center text-sm font-bold text-gray-500">
                            {index + 1}
                          </div>
                          <div className="flex-1">
                            <p className={`font-medium ${book.status === 'COMPLETED' ? 'line-through text-gray-400' : ''}`}>
                              {book.title}
                            </p>
                            <p className="text-sm text-gray-500">{book.author}</p>
                          </div>
                          {book.status === 'READING' && (
                            <button
                              onClick={() => markBookCompletedMutation.mutate({ queueId: queue.id, bookId: book.bookId })}
                              className="px-3 py-1 bg-indigo-600 text-white text-sm rounded hover:bg-indigo-700"
                            >
                              Complete
                            </button>
                          )}
                          {book.status === 'COMPLETED' && (
                            <Check size={20} className="text-green-600" />
                          )}
                          {book.status === 'PENDING' && (
                            <Clock size={20} className="text-gray-400" />
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Milestones Tab */}
        {activeTab === 'milestones' && (
          <div className="space-y-6">
            <div className="flex justify-between items-center">
              <h2 className="text-xl font-bold">Book Milestones</h2>
              <button
                onClick={() => setShowMilestoneModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-sm hover:bg-indigo-700"
              >
                <Plus size={18} />
                Add Milestone
              </button>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-4 gap-4">
              <div className="bg-white border border-gray-200 rounded-lg p-4 text-center">
                <div className="text-2xl font-bold text-gray-900">{milestones.length}</div>
                <p className="text-sm text-gray-500">Total</p>
              </div>
              <div className="bg-white border border-gray-200 rounded-lg p-4 text-center">
                <div className="text-2xl font-bold text-green-600">{completedMilestones}</div>
                <p className="text-sm text-gray-500">Completed</p>
              </div>
              <div className="bg-white border border-gray-200 rounded-lg p-4 text-center">
                <div className="text-2xl font-bold text-amber-600">{atRiskMilestones}</div>
                <p className="text-sm text-gray-500">At Risk</p>
              </div>
              <div className="bg-white border border-gray-200 rounded-lg p-4 text-center">
                <div className="text-2xl font-bold text-red-600">{overdueMilestones}</div>
                <p className="text-sm text-gray-500">Overdue</p>
              </div>
            </div>

            {milestones.length === 0 ? (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-12 text-center">
                <Flag size={48} className="mx-auto mb-4 text-gray-400" />
                <h3 className="text-lg font-bold mb-2">No Milestones Set</h3>
                <p className="text-gray-500 mb-4">Set deadlines for books you want to finish</p>
                <button
                  onClick={() => setShowMilestoneModal(true)}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-sm hover:bg-indigo-700"
                >
                  Create First Milestone
                </button>
              </div>
            ) : (
              <div className="grid gap-4">
                {milestones.map(milestone => (
                  <div key={milestone.id} className="bg-white border border-gray-200 rounded-lg p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div className="flex items-start gap-3">
                        <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${getStatusColor(milestone.status)}`}>
                          {milestone.status === 'COMPLETED' ? <Check size={20} /> : <Flag size={20} />}
                        </div>
                        <div>
                          <h3 className="font-bold">{milestone.bookTitle}</h3>
                          <p className="text-sm text-gray-500">{milestone.author}</p>
                          <div className="flex items-center gap-2 mt-1">
                            <span className={`px-2 py-0.5 text-xs rounded font-medium ${getStatusColor(milestone.status)}`}>
                              {milestone.status.replace('_', ' ')}
                            </span>
                            {milestone.daysRemaining !== null && milestone.status !== 'COMPLETED' && (
                              <span className="text-xs text-gray-500">
                                {milestone.daysRemaining > 0 
                                  ? `${milestone.daysRemaining} days remaining` 
                                  : `${Math.abs(milestone.daysRemaining)} days overdue`}
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        {milestone.status !== 'COMPLETED' && (
                          <button
                            onClick={() => completeMilestoneMutation.mutate(milestone.id)}
                            className="px-3 py-1 bg-green-600 text-white text-sm rounded hover:bg-green-700"
                          >
                            Complete
                          </button>
                        )}
                        <button
                          onClick={() => deleteMilestoneMutation.mutate(milestone.id)}
                          className="p-1 text-gray-400 hover:text-red-600"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </div>

                    {/* Progress Bar */}
                    {milestone.status !== 'COMPLETED' && (
                      <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                          <span className="text-gray-500">Progress</span>
                          <span className="font-medium">{milestone.progressPercent}%</span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div 
                            className="bg-indigo-600 h-2 rounded-full transition-all"
                            style={{ width: `${milestone.progressPercent}%` }}
                          />
                        </div>
                        <input
                          type="range"
                          min="0"
                          max="100"
                          value={milestone.progressPercent}
                          onChange={(e) => updateProgressMutation.mutate({ 
                            milestoneId: milestone.id, 
                            progress: parseInt(e.target.value) 
                          })}
                          className="w-full"
                        />
                      </div>
                    )}

                    {milestone.notes && (
                      <p className="mt-3 text-sm text-gray-500 bg-gray-50 p-2 rounded">
                        {milestone.notes}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Goal Setting Modal */}
        {showGoalModal && (
          <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
            <div className="bg-white rounded-lg p-6 max-w-md w-full">
              <h2 className="text-xl font-bold mb-4">{goal.hasGoal ? 'Update Reading Challenge' : 'Set Reading Challenge'}</h2>
              <p className="text-gray-500 mb-4">How many books do you want to read this year?</p>
              <input
                type="number"
                value={targetBooks}
                onChange={(e) => setTargetBooks(Math.max(1, parseInt(e.target.value) || 1))}
                className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                min="1"
              />
              <div className="flex gap-3">
                <button
                  onClick={() => setShowGoalModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={() => setGoalMutation.mutate()}
                  disabled={setGoalMutation.isPending}
                  className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 disabled:opacity-50"
                >
                  {setGoalMutation.isPending ? 'Saving...' : (goal.hasGoal ? 'Update Goal' : 'Set Goal')}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Queue Creation Modal */}
        {showQueueModal && (
          <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
            <div className="bg-white rounded-lg p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold">Create Reading Queue</h2>
                <button onClick={() => setShowQueueModal(false)}><X size={20} /></button>
              </div>
              
              <input
                type="text"
                placeholder="Queue name (e.g., Summer Reading)"
                value={queueName}
                onChange={(e) => setQueueName(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
              />

              <div className="mb-4">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                  <input
                    type="text"
                    placeholder="Search books to add..."
                    value={bookSearchQuery}
                    onChange={(e) => setBookSearchQuery(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded"
                  />
                </div>
                
                {searchResults?.data?.length > 0 && (
                  <div className="mt-2 max-h-40 overflow-y-auto border border-gray-200 rounded">
                    {searchResults.data.map(book => (
                      <button
                        key={book.bookId}
                        onClick={() => {
                          if (!selectedBooks.find(b => b.bookId === book.bookId)) {
                            setSelectedBooks([...selectedBooks, book]);
                          }
                          setBookSearchQuery('');
                        }}
                        className="w-full text-left px-4 py-2 hover:bg-gray-100 border-b border-gray-100 last:border-0"
                      >
                        <p className="font-medium">{book.title}</p>
                        <p className="text-sm text-gray-500">{book.author}</p>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {selectedBooks.length > 0 && (
                <div className="mb-4">
                  <h3 className="font-medium mb-2">Selected Books ({selectedBooks.length})</h3>
                  <div className="space-y-2">
                    {selectedBooks.map((book, index) => (
                      <div key={book.bookId} className="flex items-center gap-2 bg-gray-50 p-2 rounded">
                        <span className="w-6 h-6 bg-indigo-100 text-indigo-600 rounded-full flex items-center justify-center text-sm font-bold">
                          {index + 1}
                        </span>
                        <span className="flex-1">{book.title}</span>
                        <button
                          onClick={() => setSelectedBooks(selectedBooks.filter((_, i) => i !== index))}
                          className="text-red-500 hover:text-red-700"
                        >
                          <X size={16} />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="flex gap-3">
                <button
                  onClick={() => setShowQueueModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={() => createQueueMutation.mutate()}
                  disabled={createQueueMutation.isPending || selectedBooks.length === 0}
                  className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 disabled:opacity-50"
                >
                  {createQueueMutation.isPending ? 'Creating...' : 'Create Queue'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Milestone Creation Modal */}
        {showMilestoneModal && (
          <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
            <div className="bg-white rounded-lg p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold">Create Book Milestone</h2>
                <button onClick={() => setShowMilestoneModal(false)}><X size={20} /></button>
              </div>
              
              {!selectedBookForMilestone ? (
                <div>
                  <div className="relative mb-4">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                    <input
                      type="text"
                      placeholder="Search for a book..."
                      value={bookSearchQuery}
                      onChange={(e) => setBookSearchQuery(e.target.value)}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded"
                    />
                  </div>
                  
                  {searchLoading && (
                    <div className="flex justify-center py-4">
                      <Loader2 className="animate-spin text-indigo-600" />
                    </div>
                  )}
                  
                  {searchResults?.data?.length > 0 && (
                    <div className="max-h-60 overflow-y-auto border border-gray-200 rounded">
                      {searchResults.data.map(book => (
                        <button
                          key={book.bookId}
                          onClick={() => {
                            setSelectedBookForMilestone(book);
                            setBookSearchQuery('');
                          }}
                          className="w-full text-left px-4 py-3 hover:bg-gray-100 border-b border-gray-100 last:border-0"
                        >
                          <p className="font-medium">{book.title}</p>
                          <p className="text-sm text-gray-500">{book.author}</p>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <div>
                  <div className="bg-indigo-50 p-4 rounded-lg mb-4">
                    <p className="font-medium">{selectedBookForMilestone.title}</p>
                    <p className="text-sm text-gray-600">{selectedBookForMilestone.author}</p>
                    <button
                      onClick={() => setSelectedBookForMilestone(null)}
                      className="text-sm text-indigo-600 hover:underline mt-2"
                    >
                      Change book
                    </button>
                  </div>

                  <div className="mb-4">
                    <label className="block text-sm font-medium mb-1">Target Completion Date</label>
                    <input
                      type="date"
                      value={targetDate}
                      onChange={(e) => setTargetDate(e.target.value)}
                      min={new Date().toISOString().split('T')[0]}
                      className="w-full px-4 py-2 border border-gray-300 rounded"
                    />
                  </div>

                  <div className="mb-4">
                    <label className="block text-sm font-medium mb-1">Notes (optional)</label>
                    <textarea
                      value={milestoneNotes}
                      onChange={(e) => setMilestoneNotes(e.target.value)}
                      placeholder="Why do you want to read this? Any specific goals?"
                      className="w-full px-4 py-2 border border-gray-300 rounded h-24 resize-none"
                    />
                  </div>

                  <div className="flex gap-3">
                    <button
                      onClick={() => setShowMilestoneModal(false)}
                      className="flex-1 px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={() => createMilestoneMutation.mutate()}
                      disabled={createMilestoneMutation.isPending || !targetDate}
                      className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 disabled:opacity-50"
                    >
                      {createMilestoneMutation.isPending ? 'Creating...' : 'Create Milestone'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default MemberReadingGoalPage;
