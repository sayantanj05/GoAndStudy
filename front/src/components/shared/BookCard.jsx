import React from 'react';
import { Link } from 'react-router-dom';
import { Heart, Star, Book } from 'lucide-react';
import { motion } from 'framer-motion';

const BookCard = ({ book, onWishlistToggle, isWishlisted }) => {
  const { bookId, title, authors, coverImageUrl, availableCopies, averageRating, genre, isbn, categoryIds, categoryNames } = book;
  const detailId = encodeURIComponent(bookId || isbn || title || '');

  // Use categoryNames if available, otherwise fall back to categoryIds or genre
  const categories = categoryNames?.length > 0
    ? categoryNames.slice(0, 2)
    : (categoryIds?.length > 0 ? categoryIds.slice(0, 2) : (genre ? [genre] : []));

  return (
    <motion.div
      whileHover={{ y: -5 }}
      transition={{ duration: 0.2 }}
      className="group relative bg-white border border-amber-100 overflow-hidden shadow-sm hover:shadow-md h-full flex flex-col"
    >
      {/* Cover Image */}
      <div className="relative aspect-[2/3] overflow-hidden bg-amber-50">
        <img
          src={coverImageUrl || `https://picsum.photos/seed/${isbn || bookId}/200/300.jpg`}
          alt={title}
          onError={(e) => {
            e.target.style.display = 'none';
            e.target.nextSibling.style.display = 'flex';
          }}
          onLoad={(e) => {
            e.target.style.display = 'block';
            e.target.nextSibling.style.display = 'none';
          }}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
        />
        <div
          style={{ display: 'none' }}
          className="w-full h-full flex items-center justify-center bg-amber-100 text-amber-800 font-display text-4xl font-bold"
        >
          {title?.substring(0, 2).toUpperCase()}
        </div>

        {/* Floating Badge */}
        <div className="absolute top-2 left-2">
          <span className={`px-2 py-1 text-[10px] font-bold uppercase tracking-tighter ${
            availableCopies > 0 ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
          }`}>
            {availableCopies > 0 ? `${availableCopies} Available` : 'Out of Stock'}
          </span>
        </div>

        {/* Wishlist Button */}
        <button
          onClick={(e) => {
            e.preventDefault();
            onWishlistToggle?.(bookId);
          }}
          className="absolute top-2 right-2 p-2 bg-white/90 backdrop-blur-sm rounded-none shadow-sm hover:bg-white transition-colors z-10"
        >
          <Heart 
            size={16} 
            className={isWishlisted ? 'fill-red-500 text-red-500' : 'text-gray-400'} 
          />
        </button>
      </div>

      {/* Content */}
      <div className="p-4 flex-1 flex flex-col">
        <div className="mb-1 flex flex-wrap gap-1">
          {categories.map((cat, idx) => (
            <span key={idx} className="uppercase text-[10px] font-mono text-amber-700 tracking-wider bg-amber-50 px-2 py-0.5 rounded">
              {cat}
            </span>
          ))}
          {(categoryNames?.length > 2 || categoryIds?.length > 2) && (
            <span className="uppercase text-[10px] font-mono text-amber-700 tracking-wider bg-amber-50 px-2 py-0.5 rounded">
              +{(categoryNames?.length || categoryIds?.length || 0) - 2}
            </span>
          )}
        </div>
        <Link to={`/member/books/${detailId}`} className="block">
          <h4 className="font-display text-lg font-bold leading-tight line-clamp-2 hover:text-amber-700 transition-colors">
            {title}
          </h4>
        </Link>
        <p className="text-xs text-gray-500 mt-1 line-clamp-1">by {authors?.join(', ')}</p>

        <div className="mt-auto pt-4 flex items-center justify-between">
          <div className="flex items-center space-x-1">
            <Star size={12} className="fill-amber-400 text-amber-400" />
            <span className="text-xs font-bold">{averageRating?.toFixed(1) || '0.0'}</span>
          </div>
          <Link 
            to={`/member/books/${detailId}`}
            className="text-[10px] font-bold uppercase tracking-widest text-amber-800 hover:underline flex items-center"
          >
            Details <Book size={10} className="ml-1" />
          </Link>
        </div>
      </div>
    </motion.div>
  );
};

export default BookCard;
