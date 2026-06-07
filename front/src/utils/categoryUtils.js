/**
 * Category utility functions for consistent category handling across the application
 */

// Comprehensive category mapping with all seeded categories from DataSeeder.java
export const CATEGORY_MAP = {
  'CAT001': 'Fiction',
  'CAT002': 'Self-help',
  'CAT003': 'History',
  'CAT004': 'Sci-Fi',
  'CAT005': 'Memoir',
  'CAT006': 'Philosophy',
  'CAT007': 'Psychology',
  'CAT008': 'Biography',
  'CAT009': 'Mystery',
  'CAT010': 'Technology',
  'CAT011': 'Fantasy',
  'CAT012': 'Romance',
  'CAT013': 'Horror',
  'CAT014': 'Poetry',
  'CAT015': 'Religious'
};

/**
 * Get category name from ID with comprehensive fallbacks
 * @param {string} category - Category ID or name
 * @param {Object} apiCategories - Categories fetched from API (optional)
 * @returns {string} Category name
 */
export const getCategoryName = (category, apiCategories = []) => {
  if (!category) return 'Unknown';
  
  // If it's already a name (not an ID), return as-is
  if (!/^[A-Z]{3}\d{3}$/.test(category)) {
    return category;
  }
  
  // Create dynamic map from API categories
  const apiCategoryMap = apiCategories.reduce((map, cat) => {
    map[cat.id] = cat.name;
    return map;
  }, {});
  
  // Priority: API categories > static mapping > fallbacks
  const categoryName = apiCategoryMap[category] || CATEGORY_MAP[category];
  
  if (categoryName) {
    return categoryName;
  }
  
  // Enhanced fallback for unknown categories
  console.warn(`Unknown category ID: ${category}`, {
    availableCategories: Object.keys({ ...CATEGORY_MAP, ...apiCategoryMap }),
    requestedCategory: category
  });
  
  // Try to extract meaningful info from ID
  const categoryNumber = category.match(/\d+/)?.[0];
  if (categoryNumber) {
    return `Category ${categoryNumber}`;
  }
  
  return 'Unknown Category';
};

/**
 * Create category map with API data and static fallbacks
 * @param {Array} apiCategories - Categories fetched from API
 * @returns {Object} Complete category map
 */
export const createCategoryMap = (apiCategories = []) => {
  return {
    // API-fetched categories take priority
    ...apiCategories.reduce((map, cat) => {
      map[cat.id] = cat.name;
      return map;
    }, {}),
    // Static categories as fallback
    ...CATEGORY_MAP
  };
};

/**
 * Check if a value is a category ID
 * @param {string} value - Value to check
 * @returns {boolean} True if it's a category ID
 */
export const isCategoryId = (value) => {
  return /^[A-Z]{3}\d{3}$/.test(value);
};

/**
 * Get all available category names
 * @param {Array} apiCategories - Categories fetched from API
 * @returns {Array} Array of category names
 */
export const getAllCategoryNames = (apiCategories = []) => {
  const categoryMap = createCategoryMap(apiCategories);
  return Object.values(categoryMap);
};
