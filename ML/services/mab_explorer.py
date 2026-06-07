"""Thompson Sampling for exploration slots."""
import numpy as np
from collections import defaultdict

class GenreBandit:
    def __init__(self, genres: list):
        self.genres = genres
        # Beta priors: alpha=successes+1, beta=failures+1
        self.alpha = {g: 1.0 for g in genres}
        self.beta = {g: 1.0 for g in genres}
    
    def select_arm(self) -> str:
        """Sample from Beta distribution, return genre with highest sample."""
        samples = {g: np.random.beta(self.alpha[g], self.beta[g]) for g in self.genres}
        return max(samples, key=samples.get)
    
    def update(self, genre: str, reward: float):
        """reward: 1.0 if clicked/borrowed, 0.0 if ignored."""
        self.alpha[genre] += reward
        self.beta[genre] += (1 - reward)
    
    def get_exploration_slots(self, n_slots: int, top_genres: list) -> list:
        """Return genres for exploration slots."""
        slots = []
        for _ in range(n_slots):
            arm = self.select_arm()
            slots.append(arm)
        return slots

